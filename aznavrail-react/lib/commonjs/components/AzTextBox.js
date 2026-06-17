"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzTextBoxDefaults = exports.AzTextBox = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _HistoryManager = require("../util/HistoryManager");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Module-level configuration for `AzTextBox` autocomplete behaviour. */
const AzTextBoxDefaults = exports.AzTextBoxDefaults = {
  /** Sets the maximum number of suggestion entries retained by the shared `historyManager`. */
  setSuggestionLimit: limit => _HistoryManager.historyManager.setLimit(limit)
};

/** Props for the `AzTextBox` text input. Extends `TextInputProps` minus `value`/`onChangeText` which are renamed below. */

/**
 * Text input with optional outline, inline submit button, history-backed autocomplete
 * suggestions (keyed by `historyContext`), secret-mode reveal toggle, and clear button.
 */
const AzTextBox = ({
  value: controlledValue,
  onValueChange,
  hint = '',
  outlined = true,
  multiline = false,
  secret = false,
  outlineColor = '#6200ee',
  textColor,
  fillColor,
  historyContext = 'global',
  submitButtonContent,
  onSubmit,
  showSubmitButton = true,
  containerStyle,
  backgroundColor = 'transparent',
  backgroundOpacity = 1,
  enabled = true,
  initialValue = '',
  isError = false,
  leadingIcon,
  trailingIcon,
  showClearButton = true,
  ...textInputProps
}) => {
  const isControlled = controlledValue !== undefined;
  const [internalValue, setInternalValue] = (0, _react.useState)(initialValue);
  const [isSecretVisible, setIsSecretVisible] = (0, _react.useState)(false);
  const [suggestions, setSuggestions] = (0, _react.useState)([]);
  const [showSuggestions, setShowSuggestions] = (0, _react.useState)(false);
  (0, _react.useEffect)(() => {
    if (!isControlled) {
      setInternalValue(initialValue);
    }
  }, [initialValue, isControlled]);
  const currentValue = isControlled ? controlledValue : internalValue;

  // Mutual exclusivity: A field cannot be multiline and secret.
  const effectiveMultiline = secret ? false : multiline;
  const handleChange = text => {
    if (!enabled) return;
    if (!isControlled) {
      setInternalValue(text);
    }
    if (onValueChange) {
      onValueChange(text);
    }
    if (!secret && text.length > 0) {
      const suggs = _HistoryManager.historyManager.getSuggestions(historyContext, text);
      setSuggestions(suggs);
      setShowSuggestions(suggs.length > 0);
    } else {
      setShowSuggestions(false);
    }
  };
  const handleSubmit = () => {
    if (!enabled) return;
    if (!secret) {
      _HistoryManager.historyManager.addEntry(historyContext, currentValue);
    }
    if (onSubmit) onSubmit(currentValue);
    setShowSuggestions(false);
    if (!isControlled) {
      setInternalValue('');
    }
  };
  const handleSuggestionClick = suggestion => {
    if (!enabled) return;
    if (!isControlled) {
      setInternalValue(suggestion);
    }
    if (onValueChange) {
      onValueChange(suggestion);
    }
    setShowSuggestions(false);
  };
  const toggleSecret = () => {
    if (!enabled) return;
    setIsSecretVisible(!isSecretVisible);
  };
  const clearText = () => handleChange('');
  const effectiveColor = isError ? 'red' : outlineColor;
  const effectiveTextColor = isError ? 'red' : textColor || outlineColor;
  const effectiveFillColor = fillColor || backgroundColor;
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, containerStyle, {
      zIndex: showSuggestions ? 1000 : 1,
      opacity: enabled ? 1 : 0.5
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.inputRow, {
      borderColor: effectiveColor,
      borderWidth: outlined ? 1 : 0,
      backgroundColor: effectiveFillColor,
      opacity: backgroundOpacity
    }]
  }, leadingIcon && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.iconWrapper
  }, leadingIcon), /*#__PURE__*/_react.default.createElement(_reactNative.TextInput, _extends({}, textInputProps, {
    value: currentValue,
    onChangeText: handleChange,
    placeholder: currentValue.trim().length > 0 ? '' : hint,
    placeholderTextColor: effectiveColor + '80',
    secureTextEntry: secret && !isSecretVisible,
    multiline: effectiveMultiline,
    editable: enabled,
    style: [styles.input, textInputProps.style, {
      color: effectiveTextColor,
      minHeight: effectiveMultiline ? 40 : 40,
      height: effectiveMultiline ? undefined : 40,
      textAlignVertical: effectiveMultiline ? 'top' : 'center'
    }]
  })), trailingIcon && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.iconWrapper
  }, trailingIcon), isError && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.iconWrapper
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: 'red',
      fontWeight: 'bold'
    }
  }, "!")), currentValue.length > 0 && showClearButton && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: secret ? toggleSecret : clearText,
    style: styles.iconButton,
    disabled: !enabled
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: effectiveColor,
      fontSize: 10
    }
  }, secret ? isSecretVisible ? 'HIDE' : 'SHOW' : 'X')), showSubmitButton && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleSubmit,
    disabled: !enabled,
    style: [styles.submitButton, {
      backgroundColor: effectiveFillColor,
      borderColor: effectiveColor,
      borderWidth: !outlined ? 1 : 0
    }]
  }, submitButtonContent || /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: effectiveTextColor,
      fontSize: 10
    }
  }, "GO"))), showSuggestions && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.suggestionsContainer
  }, suggestions.map((item, index) => /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    key: index,
    onPress: () => handleSuggestionClick(item),
    style: [styles.suggestionItem, {
      backgroundColor: index % 2 === 0 ? 'rgba(200,200,200,0.9)' : 'rgba(200,200,200,0.8)'
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.suggestionText
  }, item)))));
};
exports.AzTextBox = AzTextBox;
const styles = _reactNative.StyleSheet.create({
  container: {
    marginBottom: 8
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center'
  },
  input: {
    flex: 1,
    padding: 8,
    fontSize: 12,
    paddingVertical: 8
  },
  iconButton: {
    padding: 8
  },
  iconWrapper: {
    paddingHorizontal: 8,
    justifyContent: 'center',
    alignItems: 'center'
  },
  submitButton: {
    padding: 8,
    justifyContent: 'center',
    alignItems: 'center',
    height: '100%',
    minWidth: 40
  },
  suggestionsContainer: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    backgroundColor: 'white',
    maxHeight: 150
  },
  suggestionItem: {
    padding: 8
  },
  suggestionText: {
    fontSize: 12
  }
});
//# sourceMappingURL=AzTextBox.js.map