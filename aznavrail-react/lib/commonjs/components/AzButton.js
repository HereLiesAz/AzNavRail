"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzButton = void 0;
var _react = _interopRequireDefault(require("react"));
var _reactNative = require("react-native");
var _types = require("../types");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const AzButton = ({
  text,
  onClick,
  color = '#6200ee',
  // Default primary color
  shape = _types.AzButtonShape.CIRCLE,
  style,
  disabled = false,
  testID
}) => {
  const isCircle = shape === _types.AzButtonShape.CIRCLE;
  const isSquare = shape === _types.AzButtonShape.SQUARE;
  const isRectangle = shape === _types.AzButtonShape.RECTANGLE;
  const isNone = shape === _types.AzButtonShape.NONE;

  // Default size for circle/square
  const size = 48; // dp equivalent

  const containerStyle = {
    borderColor: isNone ? 'transparent' : color,
    borderWidth: isNone ? 0 : 2,
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    opacity: disabled ? 0.5 : 1,
    ...style
  };
  if (isCircle) {
    containerStyle.width = size;
    containerStyle.height = size;
    containerStyle.borderRadius = size / 2;
  } else if (isSquare) {
    containerStyle.width = size;
    containerStyle.height = size;
    containerStyle.borderRadius = 0;
  } else if (isRectangle) {
    containerStyle.height = size;
    // Width is determined by content or parent
    containerStyle.paddingHorizontal = 8;
    containerStyle.borderRadius = 0;
  } else if (isNone) {
    // Invisible rectangle
    containerStyle.height = size;
  }
  const textStyle = {
    color: color,
    textAlign: 'center',
    fontWeight: 'bold'
  };
  const hasNewline = text.includes('\n');
  return /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: onClick,
    disabled: disabled,
    style: containerStyle,
    testID: testID,
    accessibilityRole: "button",
    accessibilityLabel: text,
    accessibilityState: {
      disabled
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: textStyle,
    adjustsFontSizeToFit: !hasNewline,
    numberOfLines: hasNewline ? undefined : 1,
    minimumFontScale: 0.1
  }, text));
};
exports.AzButton = AzButton;
//# sourceMappingURL=AzButton.js.map