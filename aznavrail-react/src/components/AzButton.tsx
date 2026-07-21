import React from 'react';
import { TouchableOpacity, Text, ViewStyle, TextStyle, View, StyleSheet, ImageSourcePropType } from 'react-native';
import { AzButtonShape } from '../types';
import { AzLoad } from './AzLoad';
import { renderFillContent } from './fillContent';

/** Props for the shared `AzButton` component used by the main rail and components subdir. */
export interface AzButtonProps {
  /** Text label rendered inside the button. */
  text: string;
  /** Called when the button is pressed. */
  onClick: () => void;
  /** Border and default text color. */
  color?: string;
  /** Background fill color drawn inside the button shape. */
  fillColor?: string;
  /** Overrides the text color independently of the border color. */
  textColor?: string;
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
  /** When true, `content` is rendered instead of the text label. */
  hasCustomContent?: boolean;
  /**
   * Custom content rendered inside the button when `hasCustomContent` is true. May be a React
   * node (including an `<Image>` or a `react-native-svg` `<Svg>`) or an image source
   * (`require()` id / `{ uri }`). Graphics fill the shape (cover) and are clipped to it.
   */
  /** Optional size in dp, defaults to 72. */
  size?: number;
  /** Badge text to display. */
  badge?: string;
  /** Whether the badge is persistent. */
  persistentBadge?: boolean;
}

/** Shared touchable button with configurable shape, fill, text-color override, and optional custom content. */
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
  size = 72,
  badge,
  persistentBadge = false,
}) => {
  const isCircle = shape === AzButtonShape.CIRCLE;
  const isSquare = shape === AzButtonShape.SQUARE;
  const isRectangle = shape === AzButtonShape.RECTANGLE;
  const isNone = shape === AzButtonShape.NONE;

  const [showBadge, setShowBadge] = React.useState(!!badge);

  React.useEffect(() => {
    if (badge) {
      setShowBadge(true);
      if (!persistentBadge) {
        const timer = setTimeout(() => setShowBadge(false), 1000);
        return () => clearTimeout(timer);
      }
    } else {
      setShowBadge(false);
    }
  }, [badge, persistentBadge]);

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

  const isCustomContent = hasCustomContent || !!customContentNode;

  if (isCustomContent) {
    containerStyle.width = size;
    containerStyle.height = isRectangle || isNone ? 40 : size;
    if (isCircle) containerStyle.borderRadius = size / 2;
    else containerStyle.borderRadius = 0;
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
    renderFillContent(customContentNode)
  ) : (
    <View style={{ padding: 8, width: '100%', height: '100%', alignItems: 'center', justifyContent: 'center' }}>
      <Text
        style={textStyle}
        adjustsFontSizeToFit={!hasNewline}
        numberOfLines={hasNewline ? undefined : 1}
        minimumFontScale={0.1}
      >
        {text}
      </Text>
    </View>
  );

  return (
    <View style={containerStyle}>
      <TouchableOpacity
        onPress={onClick}
        disabled={!enabled || isLoading}
        style={[StyleSheet.absoluteFill, { alignItems: 'center', justifyContent: 'center' }]}
        testID={testID}
        accessibilityRole="button"
        accessibilityLabel={text}
        accessibilityState={{ disabled: !enabled || isLoading }}
      >
        <View style={[StyleSheet.absoluteFill, { backgroundColor: actualFillColor, zIndex: -1, borderRadius: containerStyle.borderRadius }]} pointerEvents="none" />
        {content}
      </TouchableOpacity>
      {showBadge && (
        <View style={{
          position: 'absolute',
          top: -4,
          right: -4,
          backgroundColor: color,
          borderRadius: 12,
          paddingHorizontal: 6,
          paddingVertical: 2,
          justifyContent: 'center',
          alignItems: 'center',
          minWidth: 20,
        }} pointerEvents="none">
          <Text style={{ color: 'white', fontSize: 10, fontWeight: 'bold' }}>{badge}</Text>
        </View>
      )}
    </View>
  );
};
