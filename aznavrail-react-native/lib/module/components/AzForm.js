function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useState, createContext, useContext } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { AzTextBox } from './AzTextBox';
const AzFormContext = /*#__PURE__*/createContext(undefined);
export const AzForm = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style
}) => {
  const [formData, setFormData] = useState({});
  const updateField = (name, value) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  const handleSubmit = () => {
    onSubmit(formData);
  };
  return /*#__PURE__*/React.createElement(AzFormContext.Provider, {
    value: {
      updateField,
      formName,
      outlineColor,
      outlined
    }
  }, /*#__PURE__*/React.createElement(View, {
    style: [styles.container, style]
  }, children, /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleSubmit,
    style: [styles.submitButton, {
      backgroundColor: 'transparent',
      // Match main component background?
      borderColor: outlineColor,
      borderWidth: !outlined ? 1 : 0 // Inverse
    }]
  }, submitButtonContent || /*#__PURE__*/React.createElement(Text, {
    style: {
      color: outlineColor
    }
  }, "Submit"))));
};
export const AzFormEntry = ({
  name,
  ...props
}) => {
  const context = useContext(AzFormContext);
  if (!context) {
    throw new Error('AzFormEntry must be used within an AzForm');
  }
  const {
    updateField,
    formName,
    outlineColor,
    outlined
  } = context;
  const handleChange = text => {
    updateField(name, text);
    if (props.onValueChange) props.onValueChange(text);
  };
  return /*#__PURE__*/React.createElement(View, {
    style: {
      flexDirection: 'row',
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement(AzTextBox, _extends({}, props, {
    onValueChange: handleChange,
    historyContext: formName // Use formName as history context
    ,
    outlineColor: outlineColor // Inherit
    ,
    outlined: outlined,
    containerStyle: {
      flex: 1
    },
    showSubmitButton: false
  })));
};
const styles = StyleSheet.create({
  container: {
    padding: 8
  },
  submitButton: {
    padding: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8
  }
});
//# sourceMappingURL=AzForm.js.map