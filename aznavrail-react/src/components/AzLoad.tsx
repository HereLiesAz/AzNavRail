import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';

/** Props for the `AzLoad` activity indicator. */
export interface AzLoadProps {
  /** Spinner diameter — accepts the platform `ActivityIndicator` size values or a numeric pixel size. */
  size?: number | 'small' | 'large';
  /** Spinner stroke color. Defaults to the library primary color. */
  color?: string;
  /** Test identifier. Defaults to [AzLoadTestID] so a loading state is findable without guessing. */
  testID?: string;
}

/** The identity an `AzLoad` answers to in tests and for assistive tech. */
export const AzLoadTestID = 'az-load';

/** Card-style activity indicator overlay used by the rail when `isLoading` is set. */
export const AzLoad: React.FC<AzLoadProps> = ({
  size = 'large',
  color = '#6200ee',
  testID = AzLoadTestID,
}) => (
  <View
    style={styles.container}
    testID={testID}
    accessibilityRole="progressbar"
    accessibilityLabel="loading"
  >
    <ActivityIndicator size={size} color={color} />
  </View>
);

const styles = StyleSheet.create({
  container: {
    width: 80,
    height: 80, // Square container
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white',
    borderRadius: 10,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
});
