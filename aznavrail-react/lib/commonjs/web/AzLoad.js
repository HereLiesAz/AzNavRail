"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
require("./AzLoad.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * A loading spinner component.
 *
 * @param {object} props - The component props.
 * @param {string} [props.color='currentColor'] - The color of the spinner.
 * @param {number} [props.size=24] - The size of the spinner in pixels.
 * @param {object} [props.style] - Additional styles.
 * @param {string} [props.className] - Additional classes.
 */
const AzLoad = ({
  color = 'currentColor',
  size = 24,
  style = {},
  className = ''
}) => {
  return /*#__PURE__*/_react.default.createElement("div", {
    className: `az-load-container ${className}`,
    style: {
      width: size,
      height: size,
      ...style
    }
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "az-load-spinner",
    style: {
      width: size,
      height: size,
      borderColor: 'rgba(0, 0, 0, 0.1)',
      borderLeftColor: color
    }
  }));
};
var _default = exports.default = AzLoad;
//# sourceMappingURL=AzLoad.js.map