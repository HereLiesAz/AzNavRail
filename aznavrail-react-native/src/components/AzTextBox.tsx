import React, { useState } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import { historyManager } from '../util/HistoryManager';

export const AzTextBoxDefaults = {
    setSuggestionLimit: (limit: number) => historyManager.setLimit(limit),
};

export interface AzTextBoxProps {
  value?: string;
  onValueChange?: (text: string) => void;
  hint?: string;
  outlined?: boolean;
  multiline?: boolean;
  secret?: boolean;
  outlineColor?: string;
  historyContext?: string;
  submitButtonContent?: React.ReactNode;
  onSubmit?: (text: string) => void;
  showSubmitButton?: boolean;
  style?: ViewStyle;
  containerStyle?: ViewStyle;
  backgroundColor?: string;
  backgroundOpacity?: number;
  enabled?: boolean;
}

export const AzTextBox: React.FC<AzTextBoxProps> = ({
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
}) => {
  const isControlled = controlledValue !== undefined;
  const [internalValue, setInternalValue] = useState('');
  const [isSecretVisible, setIsSecretVisible] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const currentValue = isControlled ? controlledValue : internalValue;

  // Mutual exclusivity: A field cannot be multiline and secret.
  const effectiveMultiline = secret ? false : multiline;

  const handleChange = (text: string) => {
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
    historyManager.addEntry(historyContext, currentValue);
    if (onSubmit) onSubmit(currentValue);
    setShowSuggestions(false);
    if (!isControlled) {
        setInternalValue('');
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
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

  return (
    <View style={[
        styles.container,
        containerStyle,
        {
            zIndex: showSuggestions ? 1000 : 1,
            opacity: enabled ? 1 : 0.5
        }
    ]}>
      <View style={[
          styles.inputRow,
          {
              borderColor: outlineColor,
              borderWidth: outlined ? 1 : 0,
              backgroundColor: backgroundColor,
              opacity: backgroundOpacity
          }
      ]}>
        <TextInput
          value={currentValue}
          onChangeText={handleChange}
          placeholder={hint}
          placeholderTextColor={outlineColor + '80'}
          secureTextEntry={secret && !isSecretVisible}
          multiline={effectiveMultiline}
          editable={enabled}
          style={[
              styles.input,
              {
                  color: outlineColor,
                  minHeight: effectiveMultiline ? 40 : 40,
                  height: effectiveMultiline ? undefined : 40,
                  textAlignVertical: effectiveMultiline ? 'top' : 'center'
              }
          ]}
        />

        {(currentValue.length > 0) && (
          <TouchableOpacity
              onPress={secret ? toggleSecret : clearText}
              style={styles.iconButton}
              disabled={!enabled}
          >
            <Text style={{ color: outlineColor, fontSize: 10 }}>
              {secret ? (isSecretVisible ? 'HIDE' : 'SHOW') : 'X'}
            </Text>
          </TouchableOpacity>
        )}

        {showSubmitButton && (
            <TouchableOpacity
                onPress={handleSubmit}
                disabled={!enabled}
                style={[
                    styles.submitButton,
                    {
                        backgroundColor: backgroundColor,
                        borderColor: outlineColor,
                        borderWidth: !outlined ? 1 : 0
                    }
                ]}
            >
                 {submitButtonContent || <Text style={{color: outlineColor, fontSize: 10}}>GO</Text>}
            </TouchableOpacity>
        )}
      </View>

      {showSuggestions && (
        <View style={styles.suggestionsContainer}>
          {suggestions.map((item, index) => (
            <TouchableOpacity
              key={index}
              onPress={() => handleSuggestionClick(item)}
              style={[
                styles.suggestionItem,
                { backgroundColor: index % 2 === 0 ? 'rgba(200,200,200,0.9)' : 'rgba(200,200,200,0.8)' }
              ]}
            >
              <Text style={styles.suggestionText}>{item}</Text>
            </TouchableOpacity>
          ))}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginBottom: 8,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  input: {
    flex: 1,
    padding: 8,
    fontSize: 12,
    paddingVertical: 8,
  },
  iconButton: {
    padding: 8,
  },
  submitButton: {
    padding: 8,
    justifyContent: 'center',
    alignItems: 'center',
    height: '100%',
    minWidth: 40,
  },
  suggestionsContainer: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    backgroundColor: 'white',
    maxHeight: 150,
  },
  suggestionItem: {
    padding: 8,
  },
  suggestionText: {
    fontSize: 12,
  },
});
