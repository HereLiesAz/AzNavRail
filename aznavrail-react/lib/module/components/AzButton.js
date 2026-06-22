import React from 'react';
import { TouchableOpacity, Text, View, StyleSheet } from 'react-native';
import { AzButtonShape } from '../types';
import { AzLoad } from './AzLoad';
import { renderFillContent } from './fillContent';

/** Props for the shared `AzButton` component used by the main rail and components subdir. */

// Default size for circle/square
const BUTTON_SIZE = 72; // dp equivalent

/** Shared touchable button with configurable shape, fill, text-color override, and optional custom content. */
export const AzButton = ({
  text,
  onClick,
  color = '#6200ee',
  // Default primary color
  fillColor,
  textColor,
  shape = AzButtonShape.CIRCLE,
  style,
  enabled = true,
  isLoading = false,
  testID,
  hasCustomContent = false,
  content: customContentNode
}) => {
  const isCircle = shape === AzButtonShape.CIRCLE;
  const isSquare = shape === AzButtonShape.SQUARE;
  const isRectangle = shape === AzButtonShape.RECTANGLE;
  const isNone = shape === AzButtonShape.NONE;
  const containerStyle = {
    borderColor: isNone ? 'transparent' : color,
    borderWidth: isNone ? 0 : 3,
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    opacity: enabled ? 1 : 0.5,
    overflow: 'hidden',
    ...style
  };
  const lowercaseColor = color.toLowerCase();
  const defaultFillColor = lowercaseColor === 'black' || lowercaseColor === '#000000' || lowercaseColor === '#000' ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';
  const actualFillColor = fillColor || defaultFillColor;
  const isCustomContent = hasCustomContent || !!customContentNode;
  if (isCustomContent) {
    containerStyle.width = BUTTON_SIZE;
    containerStyle.height = isRectangle || isNone ? 40 : BUTTON_SIZE;
    if (isCircle) containerStyle.borderRadius = BUTTON_SIZE / 2;else containerStyle.borderRadius = 0;
  } else {
    if (isCircle) {
      containerStyle.width = BUTTON_SIZE;
      containerStyle.height = BUTTON_SIZE;
      containerStyle.borderRadius = BUTTON_SIZE / 2;
    } else if (isSquare) {
      containerStyle.width = BUTTON_SIZE;
      containerStyle.height = BUTTON_SIZE;
      containerStyle.borderRadius = 0;
    } else if (isRectangle) {
      containerStyle.width = BUTTON_SIZE;
      containerStyle.height = 40;
      containerStyle.borderRadius = 0;
    } else if (isNone) {
      // Invisible rectangle
      containerStyle.width = BUTTON_SIZE;
      containerStyle.height = 40;
    }
  }
  const textStyle = {
    color: textColor || color,
    textAlign: 'center',
    fontWeight: 'bold'
  };
  const hasNewline = text.includes('\n');
  const content = isLoading ? /*#__PURE__*/React.createElement(View, {
    style: {
      transform: [{
        scale: 0.5
      }]
    }
  }, /*#__PURE__*/React.createElement(AzLoad, null)) : customContentNode ? renderFillContent(customContentNode) : /*#__PURE__*/React.createElement(View, {
    style: {
      padding: 8,
      width: '100%',
      height: '100%',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(Text, {
    style: textStyle,
    adjustsFontSizeToFit: !hasNewline,
    numberOfLines: hasNewline ? undefined : 1,
    minimumFontScale: 0.1
  }, text));
  return /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: onClick,
    disabled: !enabled || isLoading,
    style: containerStyle,
    testID: testID,
    accessibilityRole: "button",
    accessibilityLabel: text,
    accessibilityState: {
      disabled: !enabled || isLoading
    }
  }, /*#__PURE__*/React.createElement(View, {
    style: [StyleSheet.absoluteFill, {
      backgroundColor: actualFillColor,
      zIndex: -1,
      borderRadius: containerStyle.borderRadius
    }],
    pointerEvents: "none"
  }), content);
};
//# sourceMappingURL=AzButton.js.map