import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  ViewStyle,
  TextInput,
} from 'react-native';

export interface AzRollerProps {
  options: string[];
  selectedOption?: string;
  onOptionSelected: (option: string) => void;
  hint?: string;
  enabled?: boolean;
  outlineColor?: string;
  backgroundColor?: string;
  backgroundOpacity?: number;
  style?: ViewStyle;
  isError?: boolean;
}

export const AzRoller: React.FC<AzRollerProps> = ({
  options,
  selectedOption,
  onOptionSelected,
  hint = '',
  enabled = true,
  outlineColor = '#6200ee',
  backgroundColor = 'transparent',
  backgroundOpacity = 1,
  style,
  isError = false,
}) => {
  const [expanded, setExpanded] = useState(false);
  const [filterText, setFilterText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // Repeat options to simulate infinite scroll for slot machine mode
  const repeatCount = 50;
  const displayOptions: string[] = [];
  if (options.length > 0) {
      for (let i = 0; i < repeatCount; i++) {
          displayOptions.push(...options);
      }
  }

  useEffect(() => {
      if (selectedOption) {
          setFilterText(selectedOption);
      }
  }, [selectedOption]);

  const handleSelect = (option: string) => {
    onOptionSelected(option);
    setFilterText(option);
    setExpanded(false);
    setIsTyping(false);
  };

  const handleTextFocus = () => {
    if (!enabled) return;
    setIsTyping(true);
    setExpanded(true);
    // If we focus, we might want to clear text if it matches selection to allow easy typing?
    // Or keep it. Android behavior: "Activates text edit mode".
  };

  const handleArrowClick = () => {
    if (!enabled) return;
    if (expanded && isTyping) {
        // If currently typing, switch to slot machine mode
        setIsTyping(false);
        // Don't close, just switch mode? Or close and reopen?
        // Android: "Clicking the dropdown arrow while typing exits Text Mode and re-opens the full list"
        setExpanded(true); // Ensure open
    } else {
        setExpanded(!expanded);
        setIsTyping(false);
    }
  };

  const getVisibleOptions = () => {
      if (isTyping) {
          if (!filterText) return options;
          return options.filter(o => o.toLowerCase().includes(filterText.toLowerCase()));
      }
      return displayOptions;
  };

  const visibleOptions = getVisibleOptions();
  const effectiveOutlineColor = isError ? 'red' : outlineColor;

  return (
    <View style={[styles.container, style, { zIndex: expanded ? 1000 : 1, opacity: enabled ? 1 : 0.5 }]}>
      <View
        style={[
          styles.header,
          {
            borderColor: effectiveOutlineColor,
            backgroundColor: backgroundColor,
            opacity: backgroundOpacity
          }
        ]}
      >
        <TextInput
            style={[styles.input, { color: effectiveOutlineColor }]}
            value={filterText}
            onChangeText={(text) => {
                setFilterText(text);
                if (!expanded) setExpanded(true);
                setIsTyping(true);
            }}
            onFocus={handleTextFocus}
            placeholder={hint}
            placeholderTextColor={effectiveOutlineColor + '80'}
            editable={enabled}
        />

        <TouchableOpacity
            onPress={handleArrowClick}
            style={styles.arrowButton}
            disabled={!enabled}
        >
             <Text style={[styles.icon, { color: effectiveOutlineColor }]}>▼</Text>
        </TouchableOpacity>
      </View>

      {expanded && (
        <View style={[styles.dropdown, { borderColor: effectiveOutlineColor }]}>
          <ScrollView
            style={styles.scrollView}
            nestedScrollEnabled={true}
            keyboardShouldPersistTaps="handled"
          >
            {visibleOptions.map((option, index) => {
              const isSelected = option === selectedOption;
              const isEven = index % 2 === 0;
              const itemBg = isSelected
                ? 'rgba(0,0,0,0.1)'
                : isEven
                ? 'rgba(0,0,0,0.05)'
                : 'transparent';

              return (
                <TouchableOpacity
                  key={index}
                  onPress={() => handleSelect(option)}
                  style={[styles.item, { backgroundColor: itemBg }]}
                >
                  <Text style={[styles.itemText, { color: '#000' }]}>{option}</Text>
                </TouchableOpacity>
              );
            })}
            {visibleOptions.length === 0 && (
                <View style={styles.item}>
                    <Text style={styles.itemText}>No options</Text>
                </View>
            )}
          </ScrollView>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    position: 'relative',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 40,
    borderWidth: 1,
  },
  input: {
    flex: 1,
    paddingHorizontal: 8,
    fontSize: 12,
    height: '100%',
    paddingVertical: 0,
  },
  arrowButton: {
      paddingHorizontal: 8,
      height: '100%',
      justifyContent: 'center',
      borderLeftWidth: 0, // Maybe separator?
  },
  icon: {
    fontSize: 12,
  },
  dropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    borderWidth: 1,
    borderTopWidth: 0,
    backgroundColor: 'white',
    maxHeight: 200,
  },
  scrollView: {
    maxHeight: 200,
  },
  item: {
    padding: 12,
  },
  itemText: {
    fontSize: 12,
  },
});
