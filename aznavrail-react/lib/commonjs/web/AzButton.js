"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
var _useFitText = _interopRequireDefault(require("../hooks/useFitText"));
var _AzLoad = _interopRequireDefault(require("./AzLoad"));
require("./AzButton.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * A standalone button component that matches the AzNavRail style.
 *
 * @param {object} props - The component props.
 * @param {string} props.text - The button text.
 * @param {function} props.onClick - The click handler.
 * @param {string} [props.shape='RECTANGLE'] - The shape of the button (CIRCLE, SQUARE, RECTANGLE, ROUNDED, NONE).
 * @param {string} [props.color='currentColor'] - The color of the button (border and text).
 * @param {string} [props.fillColor] - The custom background fill color of the button.
 * @param {boolean} [props.isLoading=false] - Whether the button is in a loading state.
 * @param {boolean} [props.enabled=true] - Whether the button is enabled.
 * @param {object} [props.contentPadding] - Custom padding.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzButton = ({
  text,
  onClick,
  shape = 'RECTANGLE',
  color = 'currentColor',
  fillColor,
  isLoading = false,
  enabled = true,
  contentPadding,
  className = '',
  style = {}
}) => {
  const isFixedSize = shape === 'CIRCLE' || shape === 'SQUARE';
  const fitTextRef = (0, _useFitText.default)();
  const textRef = isFixedSize ? fitTextRef : null;
  const shapeClass = `az-button-shape-${shape.toLowerCase()}`;
  const lowerColor = color.toLowerCase();
  const computedFillColor = lowerColor === 'black' || lowerColor === '#000000' || lowerColor === '#000' ? 'rgba(255, 255, 255, 0.25)' : 'rgba(0, 0, 0, 0.25)';
  const finalFillColor = fillColor || computedFillColor;
  const customStyle = {
    borderColor: color,
    backgroundColor: finalFillColor,
    color: color,
    ...style,
    ...(contentPadding ? {
      padding: contentPadding
    } : {})
  };
  return /*#__PURE__*/_react.default.createElement("button", {
    className: `az-button ${shapeClass} ${className}`,
    onClick: enabled && !isLoading ? onClick : undefined,
    disabled: !enabled,
    style: customStyle
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "az-button-content",
    style: {
      opacity: isLoading ? 0 : 1
    }
  }, /*#__PURE__*/_react.default.createElement("span", {
    ref: textRef,
    className: "az-button-text",
    style: !isFixedSize ? {
      fontSize: '14px'
    } : {}
  }, text)), isLoading && /*#__PURE__*/_react.default.createElement(_AzLoad.default, {
    color: color,
    size: 20
  }));
};
var _default = exports.default = AzButton;
//# sourceMappingURL=AzButton.js.map