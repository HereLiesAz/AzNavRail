"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzTextBoxDefaults = exports.AzTextBox = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _HistoryManager = require("../util/HistoryManager");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzTextBoxDefaults = exports.AzTextBoxDefaults = {
  setSuggestionLimit: limit => _HistoryManager.historyManager.setLimit(limit)
};
const AzTextBox = ({
  value: controlledValue,
  onValueChange,
  hint = '',
  outlined = true,
  multiline = false,
  secret = false,
  outlineColor = '#6200ee',
  historyContext = 'global',
  submitButtonContent,
  onSubmit,
  showSubmitButton = true,
  containerStyle,
  backgroundColor = 'transparent',
  backgroundOpacity = 1,
  enabled = true,
  initialValue = ''
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
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, containerStyle, {
      zIndex: showSuggestions ? 1000 : 1,
      opacity: enabled ? 1 : 0.5
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.inputRow, {
      borderColor: outlineColor,
      borderWidth: outlined ? 1 : 0,
      backgroundColor: backgroundColor,
      opacity: backgroundOpacity
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.TextInput, {
    value: currentValue,
    onChangeText: handleChange,
    placeholder: currentValue.trim().length > 0 ? '' : hint,
    placeholderTextColor: outlineColor + '80',
    secureTextEntry: secret && !isSecretVisible,
    multiline: effectiveMultiline,
    editable: enabled,
    style: [styles.input, {
      color: outlineColor,
      minHeight: effectiveMultiline ? 40 : 40,
      height: effectiveMultiline ? undefined : 40,
      textAlignVertical: effectiveMultiline ? 'top' : 'center'
    }]
  }), currentValue.length > 0 && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: secret ? toggleSecret : clearText,
    style: styles.iconButton,
    disabled: !enabled
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: outlineColor,
      fontSize: 10
    }
  }, secret ? isSecretVisible ? 'HIDE' : 'SHOW' : 'X')), showSubmitButton && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleSubmit,
    disabled: !enabled,
    style: [styles.submitButton, {
      backgroundColor: backgroundColor,
      borderColor: outlineColor,
      borderWidth: !outlined ? 1 : 0
    }]
  }, submitButtonContent || /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: outlineColor,
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