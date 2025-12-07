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
    Animated: {
        Value: Value,
        ValueXY: ValueXY,
        timing: jest.fn(() => ({ start: jest.fn() })),
        event: jest.fn(),
        View: ({children, ...props}) => React.createElement('View', props, children),
    },
    View: ({children, ...props}) => React.createElement('View', props, children),
    Text: ({children, ...props}) => React.createElement('Text', props, children),
    TouchableOpacity: ({children, onPress, ...props}) => React.createElement('TouchableOpacity', {onPress, ...props}, children),
    ScrollView: ({children, ...props}) => React.createElement('ScrollView', props, children),
    PanResponder: {
        create: () => ({ panHandlers: {} }),
    },
    StyleSheet: {
        create: (obj) => obj,
        flatten: (obj) => obj,
        absoluteFillObject: {},
        hairlineWidth: 1,
    },
    Platform: {
        OS: 'ios',
        select: (obj) => obj.ios,
    },
  };
});
