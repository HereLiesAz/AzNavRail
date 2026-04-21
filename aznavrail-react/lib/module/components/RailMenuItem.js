import React, { useState, useEffect, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
export const RailMenuItem = ({
  item,
  depth,
  isExpandedHost,
  onToggleHost,
  onItemClick,
  renderSubItems
}) => {
  const [displayOption, setDisplayOption] = useState(item.selectedOption);
  const timerRef = useRef(null);
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
  const displayText = item.text || (item.isChecked ? item.toggleOnText : item.toggleOffText) || displayOption || '';
  return /*#__PURE__*/React.createElement(View, null, /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handlePress,
    style: [styles.menuItem, {
      paddingLeft: 16 + depth * 16
    }],
    accessibilityRole: "menuitem",
    accessibilityLabel: displayText,
    accessibilityState: {
      expanded: item.isHost ? isExpandedHost : undefined
    }
  }, /*#__PURE__*/React.createElement(Text, {
    style: [styles.menuItemText, {
      fontWeight: item.isHost ? 'bold' : 'normal'
    }]
  }, displayText), item.isHost && /*#__PURE__*/React.createElement(Text, null, isExpandedHost ? '▲' : '▼')), item.isHost && isExpandedHost && renderSubItems());
};
const styles = StyleSheet.create({
  menuItem: {
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  menuItemText: {
    fontSize: 16
  }
});
//# sourceMappingURL=RailMenuItem.js.map