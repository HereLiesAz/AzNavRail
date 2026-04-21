"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzToggle = void 0;
var _react = _interopRequireDefault(require("react"));
var _AzButton = require("./AzButton");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const AzToggle = ({
  isChecked,
  onToggle,
  toggleOnText,
  toggleOffText,
  color,
  shape,
  style,
  disabled,
  testID
}) => {
  return /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, {
    text: isChecked ? toggleOnText : toggleOffText,
    onClick: onToggle,
    color: color,
    shape: shape,
    style: style,
    disabled: disabled,
    testID: testID
  });
};
exports.AzToggle = AzToggle;
//# sourceMappingURL=AzToggle.js.map