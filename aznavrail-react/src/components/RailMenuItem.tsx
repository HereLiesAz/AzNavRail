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

    // Alignment + kerning-justify computed per-row. We measure the label's natural width off-screen,
    // then compute an equal letter-spacing that stretches it to fill the row.
    const textAlign =
        menuItemAlignment === 'center'
            ? 'center'
            : dockingSide === AzDockingSide.RIGHT ? 'right' : 'left';
    const [availableWidth, setAvailableWidth] = useState(0);
    const [naturalWidth, setNaturalWidth] = useState(0);
    const onContainerLayout = (e: LayoutChangeEvent) => {
        const w = e.nativeEvent.layout.width;
        if (w && Math.abs(w - availableWidth) > 0.5) setAvailableWidth(w);
    };
    const onLabelLayout = (e: LayoutChangeEvent) => {
        const w = e.nativeEvent.layout.width;
        if (w && Math.abs(w - naturalWidth) > 0.5) setNaturalWidth(w);
    };
    // Hybrid justify: try kerning up to α·fontSize, then grow the font past that limit so both
    // letter-spacing and font-scale reach a stable mix that fills the row. See ../util/AzJustify.
    let letterSpacing = 0;
    let fontScale = 1;
    if (justifyMenuItems && displayText && displayText.length >= 2 && availableWidth > 0 && naturalWidth > 0) {
        const { scale, letterSpacing: ls } = solveHybridJustify(
            naturalWidth,
            availableWidth,
            displayText.length,
            BASE_FONT_PX,
        );
        fontScale = scale;
        letterSpacing = ls;
    }
    const scaledFontSize = BASE_FONT_PX * fontScale;

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
                {/* Off-screen "measurer" — same font/style as the visible label, absent from layout. */}
                <Text
                    onLayout={onLabelLayout}
                    style={[styles.measurer, { fontWeight: item.isHost ? 'bold' : 'normal' }, textStyle]}
                >
                    {displayText}
                </Text>
                <Text
                    style={[
                        styles.menuItemText,
                        {
                            fontWeight: item.isHost ? 'bold' : 'normal',
                            color: item.textColor ?? item.color ?? '#000000',
                            textAlign,
                            letterSpacing,
                            fontSize: scaledFontSize,
                        },
                        textStyle,
                    ]}
                >
                    {displayText}
                </Text>
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
  menuItemText: {
      fontSize: 16,
      flex: 1,
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
