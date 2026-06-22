function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useState, createContext, useContext, useEffect, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { AzTextBox } from './AzTextBox';

/** Declarative description of a single field in an `AzForm` when using the `entries` API. */

const AzFormContext = /*#__PURE__*/createContext(undefined);

/** Props for `AzForm` — a container that gathers `AzTextBox` inputs and emits a single submit payload. */

/**
 * Form container with two equivalent APIs:
 *  - Pass an `entries` array for a declarative spec mirroring the Android `AzForm` builder.
 *  - Or nest `<AzFormEntry>` children for a stack of inputs sharing the form context.
 *
 * Calls `onSubmit` with a `{ fieldName: value }` map when the user taps the inline submit button.
 */
export const AzForm = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style,
  entries,
  trailingIcon
}) => {
  const [formData, setFormData] = useState(() => {
    const initial = {};
    if (entries) {
      entries.forEach(e => {
        initial[e.name] = e.initialValue || '';
      });
    }
    return initial;
  });
  const updateField = useCallback((name, value) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  }, []);
  const registerField = useCallback((name, initialValue) => {
    setFormData(prev => {
      if (prev[name] === undefined) {
        return {
          ...prev,
          [name]: initialValue
        };
      }
      return prev;
    });
  }, []);
  const handleSubmit = () => {
    onSubmit(formData);
  };

  // Modern Android-parity rendering
  if (entries && entries.length > 0) {
    return /*#__PURE__*/React.createElement(View, {
      style: [styles.container, style]
    }, entries.map((entry, index) => {
      const isLast = index === entries.length - 1;
      const returnKeyType = entry.returnKeyType || (isLast ? 'send' : 'next');
      const val = formData[entry.name] !== undefined ? formData[entry.name] : entry.initialValue || '';
      const textBox = /*#__PURE__*/React.createElement(AzTextBox, {
        value: val,
        onValueChange: t => updateField(entry.name, t),
        historyContext: formName,
        hint: entry.hint,
        outlined: outlined,
        outlineColor: outlineColor,
        multiline: entry.multiline,
        secret: entry.secret,
        leadingIcon: entry.leadingIcon,
        trailingIcon: trailingIcon,
        isError: entry.isError,
        enabled: entry.enabled,
        keyboardType: entry.keyboardType,
        returnKeyType: returnKeyType,
        showSubmitButton: false,
        containerStyle: isLast ? {
          flex: 1,
          marginBottom: 0
        } : undefined,
        onSubmitEditing: isLast ? handleSubmit : undefined
      });
      if (isLast) {
        return /*#__PURE__*/React.createElement(View, {
          key: entry.name,
          style: styles.lastRow
        }, textBox, /*#__PURE__*/React.createElement(View, {
          style: {
            width: 8
          }
        }), /*#__PURE__*/React.createElement(TouchableOpacity, {
          onPress: handleSubmit,
          style: [styles.paritySubmitButton, {
            backgroundColor: 'transparent',
            borderColor: outlineColor,
            borderWidth: outlined ? 0 : 1
          }]
        }, submitButtonContent || /*#__PURE__*/React.createElement(Text, {
          style: {
            color: outlineColor,
            fontSize: 12,
            fontWeight: 'bold'
          }
        }, "GO")));
      }
      return /*#__PURE__*/React.createElement(View, {
        key: entry.name
      }, textBox);
    }));
  }

  // Legacy rendering
  return /*#__PURE__*/React.createElement(AzFormContext.Provider, {
    value: {
      updateField,
      registerField,
      formName,
      outlineColor,
      outlined,
      formData
    }
  }, /*#__PURE__*/React.createElement(View, {
    style: [styles.container, style]
  }, children, /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleSubmit,
    style: [styles.submitButton, {
      backgroundColor: 'transparent',
      // Match main component background?
      borderColor: outlineColor,
      borderWidth: outlined ? 0 : 1 // Inverse
    }]
  }, submitButtonContent || /*#__PURE__*/React.createElement(Text, {
    style: {
      color: outlineColor
    }
  }, "Submit"))));
};

/** Props for `AzFormEntry`, an `AzTextBox` registered into the surrounding `AzForm` by `name`. */

/** Single `AzTextBox` row that registers itself into the nearest `AzForm` context. */
export const AzFormEntry = ({
  name,
  initialValue = '',
  ...props
}) => {
  const context = useContext(AzFormContext);
  if (!context) {
    throw new Error('AzFormEntry must be used within an AzForm');
  }
  const {
    updateField,
    registerField,
    formName,
    outlineColor,
    outlined,
    formData
  } = context;
  useEffect(() => {
    registerField(name, initialValue);
  }, [name, initialValue, registerField]);
  const handleChange = text => {
    updateField(name, text);
    if (props.onValueChange) props.onValueChange(text);
  };
  const value = formData[name] !== undefined ? formData[name] : initialValue;
  return /*#__PURE__*/React.createElement(View, {
    style: {
      flexDirection: 'row',
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement(AzTextBox, _extends({}, props, {
    value: value,
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
  },
  lastRow: {
    flexDirection: 'row',
    alignItems: 'center'
  },
  paritySubmitButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    height: 40
  }
});
//# sourceMappingURL=AzForm.js.map