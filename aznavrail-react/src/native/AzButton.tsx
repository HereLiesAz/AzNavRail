import React from 'react';
import { TouchableOpacity, Text, ViewStyle, TextStyle, View, StyleSheet } from 'react-native';
import { AzButtonShape } from '../types';
import { AzLoad } from './AzLoad';

/** Props for the native `AzButton` primitive used internally by the rail. */
export interface AzButtonProps {
  /** Text label rendered inside the button. */
  text: string;
  /** Called when the button is pressed. */
  onClick: () => void;
  /** Border and text color. */
  color?: string;
  /** Background fill color drawn inside the button shape. */
  fillColor?: string;
  /** Shape of the button container. */
  shape?: AzButtonShape;
  /** Additional style merged into the container. */
  style?: ViewStyle;
  /** When false, the button is rendered at 50% opacity and is non-interactive. */
  enabled?: boolean;
  /** When true, replaces the button content with an activity indicator. */
  isLoading?: boolean;
  /** Test identifier forwarded to the underlying `TouchableOpacity`. */
  testID?: string;
  /** When true, `content` is rendered instead of the text label and size is computed differently. */
  hasCustomContent?: boolean;
  /** Custom React content rendered inside the button when `hasCustomContent` is true. */
  content?: React.ReactNode;
}

/** Native implementation: Touchable button with configurable shape, fill, and optional custom content. */
export const AzButton: React.FC<AzButtonProps> = ({
  text,
  onClick,
  color = '#6200ee', // Default primary color
  fillColor,
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
  const size = 48; // dp equivalent

  const containerStyle: ViewStyle = {
    borderColor: isNone ? 'transparent' : color,
    borderWidth: isNone ? 0 : 2,
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
      containerStyle.height = size;
      // Width is determined by content or parent
      containerStyle.paddingHorizontal = 8;
      containerStyle.borderRadius = 0;
    } else if (isNone) {
       // Invisible rectangle
       containerStyle.height = size;
    }
  }

  const textStyle: TextStyle = {
    color: color,
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
