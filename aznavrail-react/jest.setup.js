/* eslint-env jest */

jest.mock('react-native', () => {
  const React = require('react');

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
    Dimensions: {
        get: jest.fn().mockReturnValue({ width: 375, height: 812 }),
        addEventListener: jest.fn(() => ({ remove: jest.fn() })),
    },
    useWindowDimensions: jest.fn().mockReturnValue({ width: 375, height: 812, scale: 2, fontScale: 1 }),
    I18nManager: {
        isRTL: false,
    },
    Animated: {
        Value: Value,
        ValueXY: ValueXY,
        timing: jest.fn(() => ({ start: jest.fn() })),
        spring: jest.fn(() => ({ start: jest.fn() })),
        add: (_a, _b) => new Value(0),
        multiply: (_a, _b) => new Value(0),
        subtract: (_a, _b) => new Value(0),
        event: jest.fn(),
        View: ({children, ...props}) => React.createElement('View', props, children),
    },
    Vibration: {
        vibrate: jest.fn(),
    },
    View: ({children, ...props}) => React.createElement('View', props, children),
    Image: ({children, ...props}) => React.createElement('Image', props, children),
    Text: ({children, ...props}) => React.createElement('Text', props, children),
    TouchableOpacity: ({children, onPress, ...props}) => React.createElement('TouchableOpacity', {onPress, ...props}, children),
    Pressable: ({children, onPress, ...props}) => React.createElement('Pressable', {onPress, ...props}, children),
    Modal: ({children, ...props}) => React.createElement('Modal', props, children),
    SafeAreaView: ({children, ...props}) => React.createElement('SafeAreaView', props, children),
    ScrollView: ({children, ...props}) => React.createElement('ScrollView', props, children),
    ActivityIndicator: ({children, ...props}) => React.createElement('ActivityIndicator', props, children),
    TextInput: ({children, ...props}) => React.createElement('TextInput', props, children),
    PanResponder: {
        create: () => ({ panHandlers: {} }),
    },
    BackHandler: {
        addEventListener: jest.fn(() => ({ remove: jest.fn() })),
    },
    Easing: {
        out: jest.fn((fn) => fn),
        cubic: jest.fn(),
    },
    PixelRatio: {
        get: jest.fn(() => 2),
    },
    StyleSheet: {
        create: (obj) => obj,
        flatten: (obj) => obj,
        absoluteFill: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
        absoluteFillObject: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 },
        hairlineWidth: 1,
    },
    Platform: {
        OS: 'ios',
        select: (obj) => obj.ios,
    },
  };
});
