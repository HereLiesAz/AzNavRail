import React from 'react';
import { TouchableOpacity, Text, ViewStyle, TextStyle, View, StyleSheet } from 'react-native';
import { AzButtonShape } from '../types';
import { AzLoad } from './AzLoad';

export interface AzButtonProps {
  text: string;
  onClick: () => void;
  color?: string;
  fillColor?: string;
  textColor?: string;
  shape?: AzButtonShape;
  style?: ViewStyle;
  enabled?: boolean;
  isLoading?: boolean;
  testID?: string;
  hasCustomContent?: boolean;
  content?: React.ReactNode;
}

export const AzButton: React.FC<AzButtonProps> = ({
  text,
  onClick,
  color = '#6200ee', // Default primary color
  fillColor,
  textColor,
  shape = AzButtonShape.CIRCLE,
  style,
  enabled = true,
  isLoading = false,
  testID,
  hasCustomContent = false,
  content: customContentNode,
}) => {
  const isCircle = shape === AzButtonShape.CIRCLE;
  const isSquare = shape === AzButtonShape.SQUARE;
  const isRectangle = shape === AzButtonShape.RECTANGLE;
  const isNone = shape === AzButtonShape.NONE;

  // Default size for circle/square
  const size = 72; // dp equivalent

  const containerStyle: ViewStyle = {
    borderColor: isNone ? 'transparent' : color,
    borderWidth: isNone ? 0 : 3,
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    opacity: enabled ? 1 : 0.5,
    overflow: 'hidden',
    ...style,
  };

  const lowercaseColor = color.toLowerCase();
  const defaultFillColor = (lowercaseColor === 'black' || lowercaseColor === '#000000' || lowercaseColor === '#000') ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';

  const actualFillColor = fillColor || defaultFillColor;

  if (hasCustomContent) {
    containerStyle.minWidth = size;
  } else {
    if (isCircle) {
      containerStyle.width = size;
      containerStyle.height = size;
      containerStyle.borderRadius = size / 2;
    } else if (isSquare) {
      containerStyle.width = size;
      containerStyle.height = size;
      containerStyle.borderRadius = 0;
    } else if (isRectangle) {
      containerStyle.width = size;
      containerStyle.height = 40;
      containerStyle.paddingHorizontal = 8;
      containerStyle.borderRadius = 0;
    } else if (isNone) {
       // Invisible rectangle
       containerStyle.width = size;
       containerStyle.height = 40;
    }
  }

  const textStyle: TextStyle = {
    color: textColor || color,
    textAlign: 'center',
    fontWeight: 'bold',
  };

  const hasNewline = text.includes('\n');

  const content = isLoading ? (
    <View style={{ transform: [{ scale: 0.5 }] }}>
      <AzLoad />
    </View>
  ) : customContentNode ? (
    customContentNode
  ) : (
    <Text
      style={textStyle}
      adjustsFontSizeToFit={!hasNewline}
      numberOfLines={hasNewline ? undefined : 1}
      minimumFontScale={0.1}
    >
      {text}
    </Text>
  );

  return (
    <TouchableOpacity
      onPress={onClick}
      disabled={!enabled || isLoading}
      style={containerStyle}
      testID={testID}
      accessibilityRole="button"
      accessibilityLabel={text}
      accessibilityState={{ disabled: !enabled || isLoading }}
    >
      <View style={[StyleSheet.absoluteFill, { backgroundColor: actualFillColor, zIndex: -1, borderRadius: containerStyle.borderRadius }]} pointerEvents="none" />
      {content}
    </TouchableOpacity>
  );
};
