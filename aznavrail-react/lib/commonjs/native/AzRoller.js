"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzRoller = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzRoller = ({
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
  const [expanded, setExpanded] = (0, _react.useState)(false);
  const [filterText, setFilterText] = (0, _react.useState)('');
  const [isTyping, setIsTyping] = (0, _react.useState)(false);

  // Repeat options to simulate infinite scroll for slot machine mode
  const repeatCount = 50;
  const displayOptions = [];
  if (options.length > 0) {
    for (let i = 0; i < repeatCount; i++) {
      displayOptions.push(...options);
    }
  }
  (0, _react.useEffect)(() => {
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
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, style, {
      zIndex: expanded ? 1000 : 1,
      opacity: enabled ? 1 : 0.5
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.header, {
      borderColor: effectiveOutlineColor,
      backgroundColor: backgroundColor,
      opacity: backgroundOpacity
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.TextInput, {
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
  }), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleArrowClick,
    style: styles.arrowButton,
    disabled: !enabled
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: [styles.icon, {
      color: effectiveOutlineColor
    }]
  }, "\u25BC"))), expanded && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.dropdown, {
      borderColor: effectiveOutlineColor
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    style: styles.scrollView,
    nestedScrollEnabled: true,
    keyboardShouldPersistTaps: "handled"
  }, visibleOptions.map((option, index) => {
    const isSelected = option === selectedOption;
    const isEven = index % 2 === 0;
    const itemBg = isSelected ? 'rgba(0,0,0,0.1)' : isEven ? 'rgba(0,0,0,0.05)' : 'transparent';
    return /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      key: index,
      onPress: () => handleSelect(option),
      style: [styles.item, {
        backgroundColor: itemBg
      }]
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.itemText, {
        color: '#000'
      }]
    }, option));
  }), visibleOptions.length === 0 && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.item
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.itemText
  }, "No options")))));
};
exports.AzRoller = AzRoller;
const styles = _reactNative.StyleSheet.create({
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