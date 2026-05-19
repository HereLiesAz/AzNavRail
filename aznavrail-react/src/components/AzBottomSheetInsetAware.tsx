import React from 'react';
import { Platform, SafeAreaView, StyleSheet, View } from 'react-native';
import { AzBottomSheet, AzBottomSheetProps } from './AzBottomSheet';

/**
 * Inset-aware sibling of `<AzBottomSheet>`. On native, wraps the sheet in a `SafeAreaView`
 * so the body sits inside the system navigation-bar inset. On web, applies a CSS
 * `env(safe-area-inset-bottom)` padding (no true system overlay exists on web — see
 * `KNOWN_GAPS.md`).
 */
export function AzBottomSheetInsetAware(props: AzBottomSheetProps): React.ReactElement {
  if (Platform.OS === 'web') {
    return (
      <View style={[StyleSheet.absoluteFill, webSafeAreaStyle]}>
        <AzBottomSheet {...props} />
      </View>
    );
  }
  return (
    <SafeAreaView style={StyleSheet.absoluteFill} pointerEvents="box-none">
      <AzBottomSheet {...props} />
    </SafeAreaView>
  );
}

const webSafeAreaStyle = {
  // react-native-web preserves this as a plain CSS string at build time.
  paddingBottom: 'env(safe-area-inset-bottom)' as unknown as number,
};
