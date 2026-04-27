import React, { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, ScrollView, StyleSheet, TextInput } from 'react-native';
export const AzRoller = ({
  options,
  selectedOption,
  onOptionSelected,
  hint = '',
  enabled = true,
  outlineColor = '#6200ee',
  backgroundColor = 'transparent',
  backgroundOpacity = 1,
  style,
  isError = false
}) => {
  const [expanded, setExpanded] = useState(false);
  const [filterText, setFilterText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // Repeat options to simulate infinite scroll for slot machine mode
  const repeatCount = 50;
  const displayOptions = [];
  if (options.length > 0) {
    for (let i = 0; i < repeatCount; i++) {
      displayOptions.push(...options);
    }
  }
  useEffect(() => {
    if (!expanded) {
      setFilterText(selectedOption || '');
    }
  }, [selectedOption, expanded]);
  const handleSelect = option => {
    onOptionSelected(option);
    setFilterText(option);
    setExpanded(false);
    setIsTyping(false);
  };
  const handleTextFocus = () => {
    if (!enabled) return;
    setIsTyping(true);
    setExpanded(true);
    // If we focus, we might want to clear text if it matches selection to allow easy typing?
    // Or keep it. Android behavior: "Activates text edit mode".
  };
  const handleArrowClick = () => {
    if (!enabled) return;
    if (expanded && isTyping) {
      // If currently typing, switch to slot machine mode
      setIsTyping(false);
      // Don't close, just switch mode? Or close and reopen?
      // Android: "Clicking the dropdown arrow while typing exits Text Mode and re-opens the full list"
      setExpanded(true); // Ensure open
    } else {
      setExpanded(!expanded);
      setIsTyping(false);
    }
  };
  const getVisibleOptions = () => {
    if (isTyping) {
      if (!filterText) return options;
      return options.filter(o => o.toLowerCase().includes(filterText.toLowerCase()));
    }
    return displayOptions;
  };
  const visibleOptions = getVisibleOptions();
  const effectiveOutlineColor = isError ? 'red' : outlineColor;
  return /*#__PURE__*/React.createElement(View, {
    style: [styles.container, style, {
      zIndex: expanded ? 1000 : 1,
      opacity: enabled ? 1 : 0.5
    }]
  }, /*#__PURE__*/React.createElement(View, {
    style: [styles.header, {
      borderColor: effectiveOutlineColor,
      backgroundColor: backgroundColor,
      opacity: backgroundOpacity
    }]
  }, /*#__PURE__*/React.createElement(TextInput, {
    style: [styles.input, {
      color: effectiveOutlineColor
    }],
    value: filterText,
    onChangeText: text => {
      setFilterText(text);
      if (!expanded) setExpanded(true);
      setIsTyping(true);
    },
    onFocus: handleTextFocus,
    placeholder: hint,
    placeholderTextColor: effectiveOutlineColor + '80',
    editable: enabled
  }), /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleArrowClick,
    style: styles.arrowButton,
    disabled: !enabled
  }, /*#__PURE__*/React.createElement(Text, {
    style: [styles.icon, {
      color: effectiveOutlineColor
    }]
  }, "\u25BC"))), expanded && /*#__PURE__*/React.createElement(View, {
    style: [styles.dropdown, {
      borderColor: effectiveOutlineColor
    }]
  }, /*#__PURE__*/React.createElement(ScrollView, {
    style: styles.scrollView,
    nestedScrollEnabled: true,
    keyboardShouldPersistTaps: "handled"
  }, visibleOptions.map((option, index) => {
    const isSelected = option === selectedOption;
    const isEven = index % 2 === 0;
    const itemBg = isSelected ? 'rgba(0,0,0,0.1)' : isEven ? 'rgba(0,0,0,0.05)' : 'transparent';
    return /*#__PURE__*/React.createElement(TouchableOpacity, {
      key: index,
      onPress: () => handleSelect(option),
      style: [styles.item, {
        backgroundColor: itemBg
      }]
    }, /*#__PURE__*/React.createElement(Text, {
      style: [styles.itemText, {
        color: '#000'
      }]
    }, option));
  }), visibleOptions.length === 0 && /*#__PURE__*/React.createElement(View, {
    style: styles.item
  }, /*#__PURE__*/React.createElement(Text, {
    style: styles.itemText
  }, "No options")))));
};
const styles = StyleSheet.create({
  container: {
    width: '100%',
    position: 'relative'
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 40,
    borderWidth: 1
  },
  input: {
    flex: 1,
    paddingHorizontal: 8,
    fontSize: 12,
    height: '100%',
    paddingVertical: 0
  },
  arrowButton: {
    paddingHorizontal: 8,
    height: '100%',
    justifyContent: 'center',
    borderLeftWidth: 0 // Maybe separator?
  },
  icon: {
    fontSize: 12
  },
  dropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    borderWidth: 1,
    borderTopWidth: 0,
    backgroundColor: 'white',
    maxHeight: 200
  },
  scrollView: {
    maxHeight: 200
  },
  item: {
    padding: 12
  },
  itemText: {
    fontSize: 12
  }
});
//# sourceMappingURL=AzRoller.js.map