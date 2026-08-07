import React from 'react';
import { Text, TextStyle, TouchableOpacity, ViewStyle } from 'react-native';

/** Sizing bounds for the auto-shrinking footer labels. */
export const AzFooterDefaults = {
  /** Nothing in the footer is drawn larger than this. */
  maxFontSize: 22,
  /** The floor, expressed as a fraction of the max — matches the Kotlin library's 9sp. */
  minimumFontScale: 0.41,
} as const;

export interface AzFooterLabelProps {
  /** The label itself. */
  text: string;
  /** Tint — the footer's resolved accent. */
  color: string;
  onPress: () => void;
  onLongPress?: () => void;
  bold?: boolean;
  style?: ViewStyle;
  textStyle?: TextStyle;
  testID?: string;
}

/**
 * One footer row, drawn with the same auto-sizing the rail's own buttons use.
 *
 * The footer is a fixed-width column at the bottom of a menu, and its longest label — `@HereLiesAz`
 * — is the one that doesn't fit. Wrapping it to a second line makes the footer taller than the strip
 * it is pinned to and reads as a typo. So the label shrinks to fit its row instead: one line, no
 * wrap, scaled down until it fits.
 */
export const AzFooterLabel: React.FC<AzFooterLabelProps> = ({
  text,
  color,
  onPress,
  onLongPress,
  bold = false,
  style,
  textStyle,
  testID,
}) => (
  <TouchableOpacity
    onPress={onPress}
    onLongPress={onLongPress}
    delayLongPress={500}
    testID={testID}
    style={[{ paddingVertical: 4, width: '100%' }, style]}
  >
    <Text
      numberOfLines={1}
      adjustsFontSizeToFit
      minimumFontScale={AzFooterDefaults.minimumFontScale}
      ellipsizeMode="clip"
      style={[
        {
          color,
          fontSize: AzFooterDefaults.maxFontSize,
          textAlign: 'center',
          fontWeight: bold ? 'bold' : 'normal',
        },
        textStyle,
      ]}
    >
      {text}
    </Text>
  </TouchableOpacity>
);

export default AzFooterLabel;
