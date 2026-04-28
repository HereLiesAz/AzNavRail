import React, { useState } from 'react';
import { View, StyleSheet, LayoutChangeEvent } from 'react-native';

export interface AzDividerProps {
  thickness?: number;
  color?: string;
  horizontalPadding?: number;
  verticalPadding?: number;
}

/**
 * A divider that automatically adjusts its orientation based on the available space.
 * It renders as a horizontal line in a vertical layout and a vertical line in a horizontal layout.
 *
 * This component mirrors the Android AzDivider.kt functionality using React Native's onLayout.
 */
export const AzDivider: React.FC<AzDividerProps> = ({
  thickness = 1,
  color = 'rgba(0, 0, 0, 0.12)', // Default Material outline copy(alpha = 0.5f) equivalent approx
  horizontalPadding = 16,
  verticalPadding = 8,
}) => {
  const [layout, setLayout] = useState({ width: 0, height: 0 });

  const handleLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    setLayout({ width, height });
  };

  // If we haven't measured yet, just render a flexible invisible view to take up layout space
  if (layout.width === 0 && layout.height === 0) {
    return (
      <View
        style={{ flex: 1, paddingHorizontal: horizontalPadding, paddingVertical: verticalPadding }}
        onLayout={handleLayout}
      />
    );
  }

  const isVertical = layout.height > layout.width;

  return (
    <View
      style={{
        paddingHorizontal: horizontalPadding,
        paddingVertical: verticalPadding,
        flex: 1,
      }}
      onLayout={handleLayout}
    >
      <View
        style={[
          styles.divider,
          { backgroundColor: color },
          isVertical
            ? { width: thickness, height: '100%' }
            : { height: thickness, width: '100%' },
        ]}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  divider: {
    // Styles applied dynamically
  },
});
