import React, { useState, useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { AzNavItem } from '../types';

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
    textStyle
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

    return (
        <View>
            <TouchableOpacity
                onPress={handlePress}
                style={[styles.menuItem, { paddingLeft: 16 + (depth * 16), backgroundColor: (!item.isHost) ? `${item.fillColor ?? item.color ?? '#000000'}1F` : 'transparent' }]}
                accessibilityRole={item.isToggle ? "switch" : "menuitem"}
                accessibilityLabel={displayText}
                accessibilityState={accessibilityState}
            >
                <Text style={[styles.menuItemText, { fontWeight: item.isHost ? 'bold' : 'normal', color: item.textColor ?? item.color ?? '#000000' }, textStyle]}>
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
      justifyContent: 'center',
      alignItems: 'center',
  },
  menuItemText: {
      fontSize: 16,
      textAlign: 'center',
      flex: 1,
  },
});
