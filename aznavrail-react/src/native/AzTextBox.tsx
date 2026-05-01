import React, { useState, useEffect } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import { historyManager } from '../util/HistoryManager';

/** Global defaults for `AzTextBox` instances. */
export const AzTextBoxDefaults = {
    /** Sets the maximum number of history suggestions stored per context. */
    setSuggestionLimit: (limit: number) => historyManager.setLimit(limit),
};

/** Props for the `AzTextBox` text-input component. */
export interface AzTextBoxProps {
  /** Controlled value; when provided the component is fully controlled. */
  value?: string;
  /** Initial value for uncontrolled mode. */
  initialValue?: string;
  /** Called on every keystroke with the current text. */
  onValueChange?: (text: string) => void;
  /** Placeholder text shown when the field is empty. */
  hint?: string;
  /** When true, an outline border is drawn around the input. */
  outlined?: boolean;
  /** When true, the input grows vertically to accommodate multiple lines. Mutually exclusive with `secret`. */
  multiline?: boolean;
  /** When true, text is hidden and a SHOW/HIDE toggle is displayed. Mutually exclusive with `multiline`. */
  secret?: boolean;
  /** Accent color applied to borders, placeholder, and submit button. */
  outlineColor?: string;
  /** Key used to store and retrieve typed-history suggestions; defaults to `'global'`. */
  historyContext?: string;
  /** Custom content rendered inside the submit button. */
  submitButtonContent?: React.ReactNode;
  /** Called with the current value when the submit button is pressed; also saves to history. */
  onSubmit?: (text: string) => void;
  /** When false, the submit button is hidden. */
  showSubmitButton?: boolean;
  /** Deprecated — use `containerStyle` instead. */
  style?: ViewStyle;
  /** Additional style applied to the outer container. */
  containerStyle?: ViewStyle;
  /** Background color of the input row. */
  backgroundColor?: string;
  /** Opacity of the input row background. */
  backgroundOpacity?: number;
  /** When false, all interactions are blocked and the control is dimmed. */
  enabled?: boolean;
}

/** Native implementation: Text input with optional outline, secret mode, history suggestions, and a submit button. */
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
  initialValue = '',
}) => {
  const isControlled = controlledValue !== undefined;
  const [internalValue, setInternalValue] = useState(initialValue);
  const [isSecretVisible, setIsSecretVisible] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => {
    if (!isControlled) {
      setInternalValue(initialValue);
    }
  }, [initialValue, isControlled]);

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
    if (!secret) {
      historyManager.addEntry(historyContext, currentValue);
    }
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
          placeholder={currentValue.trim().length > 0 ? '' : hint}
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
