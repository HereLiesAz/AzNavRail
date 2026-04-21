import React, { useState } from 'react';
import { View, TextInput, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { historyManager } from '../util/HistoryManager';
export const AzTextBoxDefaults = {
  setSuggestionLimit: limit => historyManager.setLimit(limit)
};
export const AzTextBox = ({
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
  backgroundOpacity = 1
}) => {
  const isControlled = controlledValue !== undefined;
  const [internalValue, setInternalValue] = useState('');
  const [isSecretVisible, setIsSecretVisible] = useState(false);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const currentValue = isControlled ? controlledValue : internalValue;
  const handleChange = text => {
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
    historyManager.addEntry(historyContext, currentValue);
    if (onSubmit) onSubmit(currentValue);
    setShowSuggestions(false);
    if (!isControlled) {
      setInternalValue('');
    }
  };
  const handleSuggestionClick = suggestion => {
    if (!isControlled) {
      setInternalValue(suggestion);
    }
    if (onValueChange) {
      onValueChange(suggestion);
    }
    setShowSuggestions(false);
  };
  const toggleSecret = () => setIsSecretVisible(!isSecretVisible);
  const clearText = () => handleChange('');
  return /*#__PURE__*/React.createElement(View, {
    style: [styles.container, containerStyle, {
      zIndex: showSuggestions ? 1000 : 1
    }]
  }, /*#__PURE__*/React.createElement(View, {
    style: [styles.inputRow, {
      borderColor: outlineColor,
      borderWidth: outlined ? 1 : 0,
      backgroundColor: backgroundColor,
      opacity: backgroundOpacity
    }]
  }, /*#__PURE__*/React.createElement(TextInput, {
    value: currentValue,
    onChangeText: handleChange,
    placeholder: hint,
    placeholderTextColor: outlineColor + '80',
    secureTextEntry: secret && !isSecretVisible,
    multiline: multiline,
    style: [styles.input, {
      color: outlineColor,
      minHeight: multiline ? 40 : 40,
      height: multiline ? undefined : 40
    }]
  }), currentValue.length > 0 && /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: secret ? toggleSecret : clearText,
    style: styles.iconButton
  }, /*#__PURE__*/React.createElement(Text, {
    style: {
      color: outlineColor,
      fontSize: 10
    }
  }, secret ? isSecretVisible ? 'HIDE' : 'SHOW' : 'X')), showSubmitButton && /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleSubmit,
    style: [styles.submitButton, {
      backgroundColor: backgroundColor,
      borderColor: outlineColor,
      borderWidth: !outlined ? 1 : 0
    }]
  }, submitButtonContent || /*#__PURE__*/React.createElement(Text, {
    style: {
      color: outlineColor,
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