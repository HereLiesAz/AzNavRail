// Silence the warning: Animated: `useNativeDriver` is not supported because the native animated module is missing
jest.mock('react-native/Libraries/Animated/NativeAnimatedHelper');

jest.mock('react-native/Libraries/Utilities/Dimensions', () => ({
  get: jest.fn().mockReturnValue({ width: 375, height: 812 }),
  addEventListener: jest.fn(() => ({ remove: jest.fn() })),
}));

jest.mock('react-native/Libraries/Utilities/PixelRatio', () => ({
  get: () => 1,
  getFontScale: () => 1,
  getPixelSizeForLayoutSize: (x) => x,
  roundToNearestPixel: (x) => x,
}));

jest.mock('react-native/Libraries/StyleSheet/StyleSheet', () => ({
  create: (obj) => obj,
  flatten: (obj) => obj,
  hairlineWidth: 1,
}));

jest.mock('react-native/Libraries/Animated/Animated', () => {
  const React = require('react');
  const View = require('react-native/Libraries/Components/View/View');

  class Value {
    constructor(val) { this.val = val; }
    setValue = jest.fn();
    interpolate = jest.fn();
    addListener = jest.fn();
    removeListener = jest.fn();
  }

  class ValueXY {
      constructor(val) { this.x = new Value(val?.x || 0); this.y = new Value(val?.y || 0); }
      setValue = jest.fn();
      setOffset = jest.fn();
      flattenOffset = jest.fn();
      addListener = jest.fn();
      removeListener = jest.fn();
  }

  return {
    Value: Value,
    ValueXY: ValueXY,
    timing: jest.fn(() => ({ start: jest.fn() })),
    event: jest.fn(),
    View: View,
    default: {
        Value: Value,
        ValueXY: ValueXY,
        timing: jest.fn(() => ({ start: jest.fn() })),
        event: jest.fn(),
        View: View,
    }
  };
});
