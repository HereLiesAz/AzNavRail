import React, { useState, createContext, useContext, useEffect, useCallback, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ViewStyle, TextInput } from 'react-native';
import { AzTextBox, AzTextBoxProps } from './AzTextBox';

export interface AzFormEntryData {
  name: string;
  hint: string;
  multiline?: boolean;
  secret?: boolean;
  leadingIcon?: React.ReactNode;
  isError?: boolean;
  enabled?: boolean;
  initialValue?: string;
  keyboardType?: any;
  returnKeyType?: any;
}

interface AzFormContextType {
  updateField: (name: string, value: string) => void;
  registerField: (name: string, value: string) => void;
  formName: string;
  outlineColor: string;
  outlined: boolean;
  formData: Record<string, string>;
}

const AzFormContext = createContext<AzFormContextType | undefined>(undefined);

export interface AzFormProps {
  formName: string;
  onSubmit: (data: Record<string, string>) => void;
  outlineColor?: string;
  outlined?: boolean;
  submitButtonContent?: React.ReactNode;
  children?: React.ReactNode;
  style?: ViewStyle;
  
  // Android parity props
  entries?: AzFormEntryData[];
  trailingIcon?: React.ReactNode;
}

export const AzForm: React.FC<AzFormProps> = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style,
  entries,
  trailingIcon,
}) => {
  const [formData, setFormData] = useState<Record<string, string>>(() => {
     const initial: Record<string, string> = {};
     if (entries) {
         entries.forEach(e => {
             initial[e.name] = e.initialValue || '';
         });
     }
     return initial;
  });

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

  // Modern Android-parity rendering
  if (entries && entries.length > 0) {
      return (
        <View style={[styles.container, style]}>
            {entries.map((entry, index) => {
                const isLast = index === entries.length - 1;
                const returnKeyType = entry.returnKeyType || (isLast ? 'send' : 'next');
                const val = formData[entry.name] !== undefined ? formData[entry.name] : (entry.initialValue || '');
                
                const textBox = (
                    <AzTextBox
                        value={val}
                        onValueChange={(t) => updateField(entry.name, t)}
                        historyContext={formName}
                        hint={entry.hint}
                        outlined={outlined}
                        outlineColor={outlineColor}
                        multiline={entry.multiline}
                        secret={entry.secret}
                        leadingIcon={entry.leadingIcon}
                        trailingIcon={trailingIcon}
                        isError={entry.isError}
                        enabled={entry.enabled}
                        keyboardType={entry.keyboardType}
                        returnKeyType={returnKeyType}
                        showSubmitButton={false}
                        containerStyle={isLast ? { flex: 1, marginBottom: 0 } : undefined}
                        onSubmitEditing={isLast ? handleSubmit : undefined}
                    />
                );

                if (isLast) {
                    return (
                        <View key={entry.name} style={styles.lastRow}>
                            {textBox}
                            <View style={{ width: 8 }} />
                            <TouchableOpacity
                                onPress={handleSubmit}
                                style={[
                                    styles.paritySubmitButton,
                                    {
                                        backgroundColor: 'transparent',
                                        borderColor: outlineColor,
                                        borderWidth: outlined ? 0 : 1,
                                    }
                                ]}
                            >
                                {submitButtonContent || <Text style={{ color: outlineColor, fontSize: 12, fontWeight: 'bold' }}>GO</Text>}
                            </TouchableOpacity>
                        </View>
                    );
                }

                return <View key={entry.name}>{textBox}</View>;
            })}
        </View>
      );
  }

  // Legacy rendering
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

interface AzFormEntryProps extends Omit<AzTextBoxProps, 'onSubmit' | 'submitButtonContent'> {
  name: string;
  initialValue?: string;
}

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
  lastRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  paritySubmitButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    height: 40,
  }
});
