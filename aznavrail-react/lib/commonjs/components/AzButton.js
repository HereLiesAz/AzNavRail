"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzButton = void 0;
var _react = _interopRequireDefault(require("react"));
var _reactNative = require("react-native");
var _types = require("../types");
var _AzLoad = require("./AzLoad");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
// Default size for circle/square
const BUTTON_SIZE = 72; // dp equivalent

const AzButton = ({
  text,
  onClick,
  color = '#6200ee',
  // Default primary color
  fillColor,
  textColor,
  shape = _types.AzButtonShape.CIRCLE,
  style,
  enabled = true,
  isLoading = false,
  testID,
  hasCustomContent = false,
  content: customContentNode
}) => {
  const isCircle = shape === _types.AzButtonShape.CIRCLE;
  const isSquare = shape === _types.AzButtonShape.SQUARE;
  const isRectangle = shape === _types.AzButtonShape.RECTANGLE;
  const isNone = shape === _types.AzButtonShape.NONE;
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
  if (hasCustomContent) {
    containerStyle.width = BUTTON_SIZE;
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
      containerStyle.paddingHorizontal = 8;
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
  const content = isLoading ? /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      transform: [{
        scale: 0.5
      }]
    }
  }, /*#__PURE__*/_react.default.createElement(_AzLoad.AzLoad, null)) : customContentNode ? customContentNode : /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: textStyle,
    adjustsFontSizeToFit: !hasNewline,
    numberOfLines: hasNewline ? undefined : 1,
    minimumFontScale: 0.1
  }, text);
  return /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: onClick,
    disabled: !enabled || isLoading,
    style: containerStyle,
    testID: testID,
    accessibilityRole: "button",
    accessibilityLabel: text,
    testID: "button",
    accessibilityState: {
      disabled: !enabled || isLoading
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [_reactNative.StyleSheet.absoluteFill, {
      backgroundColor: actualFillColor,
      zIndex: -1,
      borderRadius: containerStyle.borderRadius
    }],
    pointerEvents: "none"
  }), content);
};
exports.AzButton = AzButton;
//# sourceMappingURL=AzButton.js.map