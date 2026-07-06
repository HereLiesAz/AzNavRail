import React, { useState, useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, LayoutChangeEvent } from 'react-native';
import { AzDockingSide, AzNavItem } from '../types';
import { solveHybridJustify } from '../util/AzJustify';

const BASE_FONT_PX = 16;

/** Internal props for `RailMenuItem` — one row inside the expanded-menu scroll view. */
interface RailMenuItemProps {
    /** Resolved nav item to render. */
    item: AzNavItem;
    /** Nesting depth used to indent sub-items. */
    depth: number;
    /** When `item.isHost` is true, whether the host is currently expanded. */
    isExpandedHost: boolean;
    /** Called to toggle the host item's expanded state. */
    onToggleHost: () => void;
    /** Called when a non-host item is tapped. */
    onItemClick: () => void;
    /** Renders the host's sub-items beneath it when expanded. */
    renderSubItems: () => React.ReactNode;
    /** Optional style merged over the row label (big/light/wide Metro type). */
    textStyle?: object;
    /** Docking side of the rail — drives side-alignment when `menuItemAlignment === 'side'`. */
    dockingSide?: AzDockingSide;
    /** Alignment of the label within the row: `center` (legacy) or `side` (docked-side aligned). */
    menuItemAlignment?: 'center' | 'side';
    /** When true, labels are full-justified via computed letter-spacing. */
    justifyMenuItems?: boolean;
}

/**
 * Renders one logical line of a menu label with an independent measurement + hybrid-justify solve.
 * Splitting up-front on `\n` and rendering one of these per line mirrors the Compose implementation:
 * each line gets its own `charCount`, its own natural width, and its own `letterSpacing` + font
 * `scale`, so a two-line label doesn't skew the solve by counting the newline into the char count.
 */
const JustifiedRNLine: React.FC<{
    line: string;
    justify: boolean;
    containerWidth: number;
    textAlign: 'left' | 'right' | 'center';
    color: string;
    bold: boolean;
    textStyle?: object;
}> = ({ line, justify, containerWidth, textAlign, color, bold, textStyle }) => {
    const [naturalWidth, setNaturalWidth] = useState(0);
    const onLabelLayout = (e: LayoutChangeEvent) => {
        const w = e.nativeEvent.layout.width;
        if (w && Math.abs(w - naturalWidth) > 0.5) setNaturalWidth(w);
    };
    let letterSpacing = 0;
    let fontScale = 1;
    if (justify && line.length >= 1 && containerWidth > 0 && naturalWidth > 0) {
        const { scale, letterSpacing: ls } = solveHybridJustify(
            naturalWidth,
            containerWidth,
            line.length,
            BASE_FONT_PX,
        );
        fontScale = scale;
        letterSpacing = ls;
    }
    const scaledFontSize = BASE_FONT_PX * fontScale;
    return (
        <>
            {/* Off-screen measurer — same font as the visible line, one text-run so we measure the
                line's natural width. Rendered absolute so it doesn't affect layout. */}
            <Text
                onLayout={onLabelLayout}
                style={[styles.measurer, { fontWeight: bold ? 'bold' : 'normal' }, textStyle]}
            >
                {line}
            </Text>
            {/* Visible line — `numberOfLines={1}` prevents width-based wrapping now that we've
                split explicit `\n` breaks up-front. The solver's shrink branch (fontScale < 1) keeps
                oversized lines from ever overflowing in the first place. */}
            <Text
                numberOfLines={1}
                style={[
                    styles.menuItemText,
                    {
                        fontWeight: bold ? 'bold' : 'normal',
                        color,
                        textAlign,
                        letterSpacing,
                        fontSize: scaledFontSize,
                    },
                    textStyle,
                ]}
            >
                {line}
            </Text>
        </>
    );
};

/**
 * Internal renderer for a single row in the expanded menu, handling toggle, cycler, and host item modes.
 * Used by `AzNavRail` when the menu is open.
 */
export const RailMenuItem: React.FC<RailMenuItemProps> = ({
    item,
    depth,
    isExpandedHost,
    onToggleHost,
    onItemClick,
    renderSubItems,
    textStyle,
    dockingSide = AzDockingSide.LEFT,
    menuItemAlignment = 'side',
    justifyMenuItems = true,
}) => {
    const [displayOption, setDisplayOption] = useState(item.selectedOption);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    useEffect(() => {
        setDisplayOption(item.selectedOption);
    }, [item.selectedOption]);

    const handlePress = () => {
        if (item.isHost) {
            onToggleHost();
        } else if (item.isCycler && item.options) {
             const options = item.options;
             const currentIndex = options.indexOf(displayOption || '');
             const nextIndex = (currentIndex + 1) % options.length;
             const nextOption = options[nextIndex];
             setDisplayOption(nextOption);

             if (timerRef.current) clearTimeout(timerRef.current);
             timerRef.current = setTimeout(() => {
                 if (item.onClick) item.onClick();
             }, 1000);
        } else {
            onItemClick();
        }
    };


    let displayText = item.menuText ?? item.text;
    if (item.isToggle) {
        displayText = item.isChecked
            ? (item.menuToggleOnText ?? item.toggleOnText)
            : (item.menuToggleOffText ?? item.toggleOffText);
    } else if (item.isCycler) {
        if (item.options && displayOption) {
            const idx = item.options.indexOf(displayOption);
            if (idx !== -1 && item.menuOptions && idx >= 0 && idx < item.menuOptions.length) {
                displayText = item.menuOptions[idx];
            } else {
                if (item.menuOptions) {
                    console.warn(`Cycler configuration mismatch for item='${item.text}': selectedOption='${displayOption}', index=${idx}, optionsSize=${item.options.length}, menuOptionsSize=${item.menuOptions.length}`);
                }
                displayText = displayOption;
            }
        } else {
            displayText = displayOption || '';
        }
    }


    const accessibilityState: any = {};
    if (item.isToggle) accessibilityState.checked = item.isChecked;
    if (item.isHost || item.isNestedRail) accessibilityState.expanded = isExpandedHost;

    const textAlign: 'left' | 'right' | 'center' =
        menuItemAlignment === 'center'
            ? 'center'
            : dockingSide === AzDockingSide.RIGHT ? 'right' : 'left';
    const [availableWidth, setAvailableWidth] = useState(0);
    const onContainerLayout = (e: LayoutChangeEvent) => {
        const w = e.nativeEvent.layout.width;
        if (w && Math.abs(w - availableWidth) > 0.5) setAvailableWidth(w);
    };

    // Split up-front on `\n` and render each line independently — the newline count skews
    // per-line justify math otherwise. Compose's `internal/MenuItem.kt` uses the same pattern.
    const lines = (displayText ?? '').split('\n');
    const labelColor = item.textColor ?? item.color ?? '#000000';

    return (
        <View>
            <TouchableOpacity
                onPress={handlePress}
                onLayout={onContainerLayout}
                style={[styles.menuItem, { paddingLeft: 16 + (depth * 16), backgroundColor: (!item.isHost) ? `${item.fillColor ?? item.color ?? '#000000'}1F` : 'transparent' }]}
                accessibilityRole={item.isToggle ? "switch" : "menuitem"}
                accessibilityLabel={displayText}
                accessibilityState={accessibilityState}
            >
                <View style={styles.labelColumn}>
                    {lines.map((line, i) => (
                        <JustifiedRNLine
                            key={i}
                            line={line}
                            justify={!!justifyMenuItems}
                            containerWidth={availableWidth}
                            textAlign={textAlign}
                            color={labelColor}
                            bold={!!item.isHost}
                            textStyle={textStyle}
                        />
                    ))}
                </View>
                {item.isHost && (
                    <Text style={{ marginLeft: 8 }}>{isExpandedHost ? '▲' : '▼'}</Text>
                )}
            </TouchableOpacity>

            {item.isHost && isExpandedHost && renderSubItems()}
        </View>
    );
};

const styles = StyleSheet.create({
  menuItem: {
      padding: 16,
      flexDirection: 'row',
      alignItems: 'center',
  },
  labelColumn: {
      flex: 1,
      flexDirection: 'column',
  },
  menuItemText: {
      fontSize: 16,
      alignSelf: 'stretch',
  },
  // Rendered off-screen (position:absolute + opacity:0) so it doesn't affect layout, but its onLayout
  // still fires with the label's natural width — which drives the letterSpacing calculation above.
  measurer: {
      fontSize: 16,
      position: 'absolute',
      opacity: 0,
      left: -9999,
      top: -9999,
  },
});
