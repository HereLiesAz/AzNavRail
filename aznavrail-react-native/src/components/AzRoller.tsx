import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  ViewStyle,
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
}) => {
  const [expanded, setExpanded] = useState(false);

  // Repeat options to simulate infinite scroll
  const repeatCount = 50;
  const displayOptions: string[] = [];
  if (options.length > 0) {
      for (let i = 0; i < repeatCount; i++) {
          displayOptions.push(...options);
      }
  }

  const handleSelect = (option: string) => {
    onOptionSelected(option);
    setExpanded(false);
  };

  const toggleExpanded = () => {
    if (enabled) {
      setExpanded(!expanded);
    }
  };

  return (
    <View style={[styles.container, style, { zIndex: expanded ? 1000 : 1, opacity: enabled ? 1 : 0.5 }]}>
      <TouchableOpacity
        onPress={toggleExpanded}
        style={[
          styles.header,
          {
            borderColor: outlineColor,
            backgroundColor: backgroundColor,
            // We can't apply opacity directly to bg color nicely in RN without rgba string manipulation,
            // so we rely on parent opacity or exact rgba props.
            // Assuming backgroundColor handles its own alpha or is solid.
          }
        ]}
      >
        <Text style={[styles.text, { color: selectedOption ? outlineColor : outlineColor + '80' }]}>
          {selectedOption || hint}
        </Text>
        <Text style={[styles.icon, { color: outlineColor }]}>▼</Text>
      </TouchableOpacity>

      {expanded && (
        <View style={[styles.dropdown, { borderColor: outlineColor }]}>
          <ScrollView
            style={styles.scrollView}
            nestedScrollEnabled={true}
          >
            {displayOptions.map((option, index) => {
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
    paddingHorizontal: 8,
  },
  text: {
    flex: 1,
    fontSize: 12,
  },
  icon: {
    fontSize: 12,
    paddingLeft: 8,
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
