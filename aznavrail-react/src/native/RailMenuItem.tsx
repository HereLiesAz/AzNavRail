import React, { useState, useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { AzNavItem } from '../types';

/** Props for a single row in the expanded-menu list. */
interface RailMenuItemProps {
    /** The nav item data to render. */
    item: AzNavItem;
    /** Nesting depth used to indent sub-items visually. */
    depth: number;
    /** Whether this host item's sub-items are currently expanded. */
    isExpandedHost: boolean;
    /** Called when the user taps a host item to toggle its expanded state. */
    onToggleHost: () => void;
    /** Called when the user taps a non-host item. */
    onItemClick: () => void;
    /** Renders the expanded sub-items below this host row. */
    renderSubItems: () => React.ReactNode;
}

/** Native implementation: Renders a single row in the expanded rail menu, handling toggle, cycler, and host variants. */
export const RailMenuItem: React.FC<RailMenuItemProps> = ({
    item,
    depth,
    isExpandedHost,
    onToggleHost,
    onItemClick,
    renderSubItems
}) => {
    const [displayOption, setDisplayOption] = useState(item.selectedOption);
    const timerRef = useRef<NodeJS.Timeout | null>(null);

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
                <Text style={[styles.menuItemText, { fontWeight: item.isHost ? 'bold' : 'normal', color: item.textColor ?? item.color ?? '#000000' }]}>
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
