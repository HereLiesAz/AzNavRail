function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useState, useEffect } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { historyManager } from '../util/HistoryManager';

/** Module-level configuration for `AzTextBox` autocomplete behaviour. */
export const AzTextBoxDefaults = {
  /** Sets the maximum number of suggestion entries retained by the shared `historyManager`. */
  setSuggestionLimit: limit => historyManager.setLimit(limit)
};

/** Props for the `AzTextBox` text input. Extends `TextInputProps` minus `value`/`onChangeText` which are renamed below. */

/**
 * Text input with optional outline, inline submit button, history-backed autocomplete
 * suggestions (keyed by `historyContext`), secret-mode reveal toggle, and clear button.
 */
export const AzTextBox = ({
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
  const [internalValue, setInternalValue] = useState(initialValue);
  const [isSecretVisible, setIsSecretVisible] = useState(false);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  useEffect(() => {
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
      const suggs = historyManager.getSuggestions(historyContext, text);
      setSuggestions(suggs);
      setShowSuggestions(suggs.length > 0);
    } else {
      setShowSuggestions(false);
    }
  };
  const handleSubmit = () => {
    if (!enabled) return;
    if (!secret) {
      historyManager.addEntry(historyContext, currentValue);
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
  return /*#__PURE__*/React.createElement(View, {
    style: [styles.container, containerStyle, {
      zIndex: showSuggestions ? 1000 : 1,
      opacity: enabled ? 1 : 0.5
    }]
  }, /*#__PURE__*/React.createElement(View, {
    style: [styles.inputRow, {
      borderColor: effectiveColor,
      borderWidth: outlined ? 1 : 0,
      backgroundColor: effectiveFillColor,
      opacity: backgroundOpacity
    }]
  }, leadingIcon && /*#__PURE__*/React.createElement(View, {
    style: styles.iconWrapper
  }, leadingIcon), /*#__PURE__*/React.createElement(TextInput, _extends({}, textInputProps, {
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
  })), trailingIcon && /*#__PURE__*/React.createElement(View, {
    style: styles.iconWrapper
  }, trailingIcon), isError && /*#__PURE__*/React.createElement(View, {
    style: styles.iconWrapper
  }, /*#__PURE__*/React.createElement(Text, {
    style: {
      color: 'red',
      fontWeight: 'bold'
    }
  }, "!")), currentValue.length > 0 && showClearButton && /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: secret ? toggleSecret : clearText,
    style: styles.iconButton,
    disabled: !enabled
  }, /*#__PURE__*/React.createElement(Text, {
    style: {
      color: effectiveColor,
      fontSize: 10
    }
  }, secret ? isSecretVisible ? 'HIDE' : 'SHOW' : 'X')), showSubmitButton && /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleSubmit,
    disabled: !enabled,
    style: [styles.submitButton, {
      backgroundColor: effectiveFillColor,
      borderColor: effectiveColor,
      borderWidth: !outlined ? 1 : 0
    }]
  }, submitButtonContent || /*#__PURE__*/React.createElement(Text, {
    style: {
      color: effectiveTextColor,
      fontSize: 10
    }
  }, "GO"))), showSuggestions && /*#__PURE__*/React.createElement(View, {
    style: styles.suggestionsContainer
  }, suggestions.map((item, index) => /*#__PURE__*/React.createElement(TouchableOpacity, {
    key: index,
    onPress: () => handleSuggestionClick(item),
    style: [styles.suggestionItem, {
      backgroundColor: index % 2 === 0 ? 'rgba(200,200,200,0.9)' : 'rgba(200,200,200,0.8)'
    }]
  }, /*#__PURE__*/React.createElement(Text, {
    style: styles.suggestionText
  }, item)))));
};
const styles = StyleSheet.create({
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