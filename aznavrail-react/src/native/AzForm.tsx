import React, { useState, createContext, useContext, useEffect, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ViewStyle } from 'react-native';
import { AzTextBox, AzTextBoxProps } from './AzTextBox';

interface AzFormContextType {
  updateField: (name: string, value: string) => void;
  registerField: (name: string, value: string) => void;
  formName: string;
  outlineColor: string;
  outlined: boolean;
  formData: Record<string, string>;
}

const AzFormContext = createContext<AzFormContextType | undefined>(undefined);

/** Props for the `AzForm` form container. */
export interface AzFormProps {
  /** Unique form name used as the history context key for each `AzFormEntry` field. */
  formName: string;
  /** Called with the collected field values when the submit button is pressed. */
  onSubmit: (data: Record<string, string>) => void;
  /** Accent color applied to input borders and the submit button. */
  outlineColor?: string;
  /** When true, input fields are rendered with an outline border. */
  outlined?: boolean;
  /** Custom content rendered inside the submit button; defaults to a "Submit" label. */
  submitButtonContent?: React.ReactNode;
  /** `AzFormEntry` children that declare each field in the form. */
  children: React.ReactNode;
  /** Additional style applied to the form container. */
  style?: ViewStyle;
}

/** Native implementation: Form container that collects `AzFormEntry` field values and calls `onSubmit`. */
export const AzForm: React.FC<AzFormProps> = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style,
}) => {
  const [formData, setFormData] = useState<Record<string, string>>({});

  const updateField = useCallback((name: string, value: string) => {
    setFormData(prev => ({ ...prev, [name]: value }));
  }, []);

  const registerField = useCallback((name: string, initialValue: string) => {
    setFormData(prev => {
      if (prev[name] === undefined) {
        return { ...prev, [name]: initialValue };
      }
      return prev;
    });
  }, []);

  const handleSubmit = () => {
    onSubmit(formData);
  };

  return (
    <AzFormContext.Provider value={{ updateField, registerField, formName, outlineColor, outlined, formData }}>
      <View style={[styles.container, style]}>
        {children}
        <TouchableOpacity
          onPress={handleSubmit}
          style={[
            styles.submitButton,
            {
              backgroundColor: 'transparent', // Match main component background?
              borderColor: outlineColor,
              borderWidth: outlined ? 0 : 1, // Inverse
            }
          ]}
        >
          {submitButtonContent || <Text style={{ color: outlineColor }}>Submit</Text>}
        </TouchableOpacity>
      </View>
    </AzFormContext.Provider>
  );
};

/** Props for an individual field inside an `AzForm`. */
interface AzFormEntryProps extends Omit<AzTextBoxProps, 'onSubmit' | 'submitButtonContent'> {
  /** Field name key used to store this field's value in the form data map. */
  name: string;
  /** Initial text pre-filled in the field. */
  initialValue?: string;
}

/** A single text-input field that registers itself with the enclosing `AzForm`. */
export const AzFormEntry: React.FC<AzFormEntryProps> = ({ name, initialValue = '', ...props }) => {
  const context = useContext(AzFormContext);
  if (!context) {
    throw new Error('AzFormEntry must be used within an AzForm');
  }

  const { updateField, registerField, formName, outlineColor, outlined, formData } = context;

  useEffect(() => {
    registerField(name, initialValue);
  }, [name, initialValue, registerField]);

  const handleChange = (text: string) => {
    updateField(name, text);
    if (props.onValueChange) props.onValueChange(text);
  };

  const value = formData[name] !== undefined ? formData[name] : initialValue;

  return (
    <View style={{ flexDirection: 'row', marginBottom: 8 }}>
      <AzTextBox
        {...props}
        value={value}
        onValueChange={handleChange}
        historyContext={formName} // Use formName as history context
        outlineColor={outlineColor} // Inherit
        outlined={outlined}
        containerStyle={{ flex: 1 }}
        showSubmitButton={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 8,
  },
  submitButton: {
    padding: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
});
