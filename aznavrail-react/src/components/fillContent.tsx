import React from 'react';
import { Image, ImageSourcePropType, StyleSheet, View } from 'react-native';

/**
 * Renders custom button `content` so it fills the item's shape (the React equivalent of Android's
 * `ContentScale.Crop` + shape clip). Clipping is acceptable — graphics are scaled to cover the
 * button bounds without changing the item's dimensions.
 *
 * Accepts:
 * - an image source (`number` from `require()`, or an `{ uri }` / array `ImageSourcePropType`) —
 *   rendered as a filling `<Image resizeMode="cover">`;
 * - a React element (a user `<Image>`, or an `<Svg>` from the consumer's `react-native-svg`) —
 *   cloned to fill (merged `width: '100%', height: '100%'` style, plus `resizeMode="cover"` when it
 *   is an RN `Image`);
 * - any other React node — rendered as-is inside the fill+clip container.
 */
export function renderFillContent(content: React.ReactNode | ImageSourcePropType): React.ReactNode {
  return (
    <View style={[StyleSheet.absoluteFill, styles.fill]} pointerEvents="box-none">
      {renderInner(content)}
    </View>
  );
}

const FILL_STYLE = { width: '100%', height: '100%' } as const;

function isImageSource(value: unknown): value is ImageSourcePropType {
  if (typeof value === 'number') return true; // require('./x.png')
  if (Array.isArray(value)) return true; // array of sources
  return (
    typeof value === 'object' &&
    value !== null &&
    !React.isValidElement(value) &&
    typeof (value as { uri?: unknown }).uri === 'string'
  );
}

function renderInner(content: React.ReactNode | ImageSourcePropType): React.ReactNode {
  if (isImageSource(content)) {
    return <Image source={content} resizeMode="cover" style={FILL_STYLE} />;
  }

  if (React.isValidElement(content)) {
    const element = content as React.ReactElement<any>;
    const mergedStyle = [FILL_STYLE, element.props?.style];
    const extraProps: Record<string, unknown> = { style: mergedStyle };
    // RN Image fills its box via resizeMode rather than CSS object-fit.
    if (element.type === Image) {
      extraProps.resizeMode = element.props?.resizeMode ?? 'cover';
    }
    return React.cloneElement(element, extraProps);
  }

  return content;
}

const styles = StyleSheet.create({
  fill: { overflow: 'hidden', alignItems: 'stretch', justifyContent: 'center' },
});
