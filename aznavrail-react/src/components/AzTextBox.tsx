import React, { useState, useEffect } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
  TextInputProps,
} from 'react-native';
import { historyManager } from '../util/HistoryManager';

/** Module-level configuration for `AzTextBox` autocomplete behaviour. */
export const AzTextBoxDefaults = {
    /** Sets the maximum number of suggestion entries retained by the shared `historyManager`. */
    setSuggestionLimit: (limit: number) => historyManager.setLimit(limit),
};

/** Props for the `AzTextBox` text input. Extends `TextInputProps` minus `value`/`onChangeText` which are renamed below. */
export interface AzTextBoxProps extends Omit<TextInputProps, 'value' | 'onChangeText'> {
  /** Controlled text value. When set, the input is fully controlled. */
  value?: string;
  /** Initial text value used when the input is uncontrolled. */
  initialValue?: string;
  /** Called with the new text on every keystroke. */
  onValueChange?: (text: string) => void;
  /** Placeholder text shown when the input is empty. */
  hint?: string;
  /** When true, draws an outline border; when false, uses a background fill. */
  outlined?: boolean;
  /** When true, the input expands to multiple lines. Mutually exclusive with `secret`. */
  multiline?: boolean;
  /** When true, the input masks characters and shows a SHOW/HIDE reveal toggle. */
  secret?: boolean;
  /** Outline color when `outlined` is true; also the placeholder/text color when no override is set. */
  outlineColor?: string;
  /** Independent text color override. */
  textColor?: string;
  /** Background fill color inside the input row. */
  fillColor?: string;
  /** Key used to scope autocomplete history; entries are shared across inputs with the same context. */
  historyContext?: string;
  /** Custom content for the inline submit button. */
  submitButtonContent?: React.ReactNode;
  /** Called with the current value when the user submits (taps the submit button or hits Return). */
  onSubmit?: (text: string) => void;
  /** When true, shows the inline submit button to the right of the input. */
  showSubmitButton?: boolean;
  /** Style merged into the underlying `TextInput`. */
  style?: ViewStyle;
  /** Style merged into the outer container `View`. */
  containerStyle?: ViewStyle;
  /** Background color of the input row. */
  backgroundColor?: string;
  /** Opacity of the input row background, 0–1. */
  backgroundOpacity?: number;
  /** When false, the input is rendered at 50% opacity and is non-editable. */
  enabled?: boolean;
  /** When true, the outline is forced to red and an error indicator is shown. */
  isError?: boolean;
  /** React node rendered before the input text. */
  leadingIcon?: React.ReactNode;
  /** React node rendered after the input text. */
  trailingIcon?: React.ReactNode;
  /** When true, displays an X button to clear the input while it has content. */
  showClearButton?: boolean;
}

/**
 * Text input with optional outline, inline submit button, history-backed autocomplete
 * suggestions (keyed by `historyContext`), secret-mode reveal toggle, and clear button.
 */
export const AzTextBox: React.FC<AzTextBoxProps> = ({
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

  const effectiveColor = isError ? 'red' : outlineColor;
  const effectiveTextColor = isError ? 'red' : (textColor || outlineColor);
  const effectiveFillColor = fillColor || backgroundColor;

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
              borderColor: effectiveColor,
              borderWidth: outlined ? 1 : 0,
              backgroundColor: effectiveFillColor,
              opacity: backgroundOpacity
          }
      ]}>
        {leadingIcon && (
           <View style={styles.iconWrapper}>{leadingIcon}</View>
        )}

        <TextInput
          {...textInputProps}
          value={currentValue}
          onChangeText={handleChange}
          placeholder={currentValue.trim().length > 0 ? '' : hint}
          placeholderTextColor={effectiveColor + '80'}
          secureTextEntry={secret && !isSecretVisible}
          multiline={effectiveMultiline}
          editable={enabled}
          style={[
              styles.input,
              textInputProps.style,
              {
                  color: effectiveTextColor,
                  minHeight: effectiveMultiline ? 40 : 40,
                  height: effectiveMultiline ? undefined : 40,
                  textAlignVertical: effectiveMultiline ? 'top' : 'center'
              }
          ]}
        />

        {trailingIcon && (
           <View style={styles.iconWrapper}>{trailingIcon}</View>
        )}

        {isError && (
           <View style={styles.iconWrapper}><Text style={{ color: 'red', fontWeight: 'bold' }}>!</Text></View>
        )}

        {(currentValue.length > 0 && showClearButton) && (
          <TouchableOpacity
              onPress={secret ? toggleSecret : clearText}
              style={styles.iconButton}
              disabled={!enabled}
          >
            <Text style={{ color: effectiveColor, fontSize: 10 }}>
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
                        backgroundColor: effectiveFillColor,
                        borderColor: effectiveColor,
                        borderWidth: !outlined ? 1 : 0
                    }
                ]}
            >
                 {submitButtonContent || <Text style={{color: effectiveTextColor, fontSize: 10}}>GO</Text>}
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
  iconWrapper: {
    paddingHorizontal: 8,
    justifyContent: 'center',
    alignItems: 'center',
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
