import React from 'react';
import {
  TouchableOpacity,
  Text,
  ViewStyle,
  TextStyle,
  View,
  StyleSheet,
  ImageSourcePropType,
  GestureResponderEvent,
} from 'react-native';
import { AzButtonShape, baseShapeOf, isBorderlessShape } from '../types';
import { AzLoad } from './AzLoad';
import { renderFillContent } from './fillContent';

/** Props for the shared `AzButton` component used by the main rail and components subdir. */
export interface AzButtonProps {
  /** Text label rendered inside the button. */
  text: string;
  /** Called when the button is pressed. */
  onClick: () => void;
  /**
   * Called on a long-press (500ms), same idiom as the header icon and reloc items elsewhere in
   * this library. Omit for no long-press behavior (today's default). Receives the native gesture
   * event so callers (eg. a hidden-menu opener) can read the touch position off `nativeEvent`.
   */
  onLongPress?: (event: GestureResponderEvent) => void;
  /** Border and default text color. */
  color?: string;
  /** Background fill color drawn inside the button shape. */
  fillColor?: string;
  /**
   * Exact translucent fill color (alpha included), used verbatim in place of `fillColor` and the
   * hardcoded default-fill alpha computation. Distinct from the rail-level
   * `AzNavRailSettings.translucentBackground`, which styles panels/overlays, not button fills.
   */
  translucentBackgroundColor?: string;
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
  content?: React.ReactNode | ImageSourcePropType;
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
  onLongPress,
  color = '#6200ee', // Default primary color
  fillColor,
  translucentBackgroundColor,
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
  // A borderless shape keeps the footprint of the base shape it names, so dropping the border never
  // reflows the rail around the item.
  const base = baseShapeOf(shape);
  const isCircle = base === AzButtonShape.CIRCLE;
  const isSquare = base === AzButtonShape.SQUARE;
  const isRectangle = base === AzButtonShape.RECTANGLE;
  const isNone = isBorderlessShape(shape);

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
    return undefined;
  }, [badge, persistentBadge]);

  // Feature 2: the stroke must read as drawn OUTSIDE the fill, never overlapping it. RN's border
  // is drawn just inside the declared box (border-box-equivalent), so instead of inflating the
  // box outward (which would grow the rail's layout footprint), the FILL is inset by the stroke
  // width on every side below and the box itself is left exactly as before — the stroke ring now
  // sits in the space that used to be "inside the stroke," touching the fill but never covering it.
  const strokeWidth = isNone ? 0 : 3;

  const containerStyle: ViewStyle = {
    borderColor: isNone ? 'transparent' : color,
    borderWidth: strokeWidth,
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    opacity: enabled ? 1 : 0.5,
    overflow: 'hidden',
    ...style,
  };

  const lowercaseColor = color.toLowerCase();
  const defaultFillColor =
    lowercaseColor === 'black' ||
    lowercaseColor === '#000000' ||
    lowercaseColor === '#000'
      ? 'rgba(255, 255, 255, 0.25)'
      : 'rgba(0, 0, 0, 0.25)';

  // `translucentBackgroundColor` is an exact, alpha-included override — it wins over `fillColor`
  // and the hardcoded default-fill alpha alike, since it exists specifically to bypass both.
  const actualFillColor =
    translucentBackgroundColor || fillColor || defaultFillColor;

  const isCustomContent = hasCustomContent || !!customContentNode;

  if (isCustomContent) {
    containerStyle.width = size;
    containerStyle.height = isRectangle ? 40 : size;
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
    <View
      style={{
        padding: 8,
        width: '100%',
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
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
        onLongPress={onLongPress}
        delayLongPress={500}
        disabled={!enabled || isLoading}
        style={[
          StyleSheet.absoluteFill,
          { alignItems: 'center', justifyContent: 'center' },
        ]}
        testID={testID}
        accessibilityRole="button"
        accessibilityLabel={text}
        accessibilityState={{ disabled: !enabled || isLoading }}
      >
        <View
          style={[
            StyleSheet.absoluteFill,
            {
              top: strokeWidth,
              left: strokeWidth,
              right: strokeWidth,
              bottom: strokeWidth,
              backgroundColor: actualFillColor,
              zIndex: -1,
              borderRadius: Math.max(
                0,
                ((containerStyle.borderRadius as number) || 0) - strokeWidth
              ),
            },
          ]}
          pointerEvents="none"
        />
        {content}
      </TouchableOpacity>
      {showBadge && (
        <View
          style={{
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
          }}
          pointerEvents="none"
        >
          <Text style={{ color: 'white', fontSize: 10, fontWeight: 'bold' }}>
            {badge}
          </Text>
        </View>
      )}
    </View>
  );
};
