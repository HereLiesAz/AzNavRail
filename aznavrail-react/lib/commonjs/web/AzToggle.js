"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
require("./AzToggle.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * A toggle switch component.
 *
 * @param {object} props
 * @param {boolean} props.value - The current state (true/false).
 * @param {function} props.onValueChange - Callback for state change.
 * @param {string} [props.label] - Optional label text.
 * @param {boolean} [props.enabled=true] - Whether the toggle is enabled.
 * @param {string} [props.color='currentColor'] - The color of the toggle.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzToggle = ({
  value,
  onValueChange,
  label,
  enabled = true,
  color = 'currentColor',
  className = '',
  style = {}
}) => {
  const handleChange = e => {
    if (enabled) {
      onValueChange(e.target.checked);
    }
  };
  return /*#__PURE__*/_react.default.createElement("label", {
    className: `az-toggle-container ${!enabled ? 'disabled' : ''} ${className}`,
    style: {
      color: color,
      ...style
    }
  }, /*#__PURE__*/_react.default.createElement("input", {
    type: "checkbox",
    className: "az-toggle-input",
    checked: !!value,
    onChange: handleChange,
    disabled: !enabled
  }), /*#__PURE__*/_react.default.createElement("div", {
    className: "az-toggle-track",
    style: {
      borderColor: color
    }
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "az-toggle-thumb",
    style: {
      backgroundColor: color
    }
  })), label && /*#__PURE__*/_react.default.createElement("span", {
    className: "az-toggle-label"
  }, label));
};
var _default = exports.default = AzToggle;
//# sourceMappingURL=AzToggle.js.map