"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
require("./AzNestedRailPopup.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * A popup overlay for nested navigation rails in the web library.
 */
const AzNestedRailPopup = ({
  visible,
  onDismiss,
  items,
  alignment = 'VERTICAL',
  renderItem,
  anchorPosition,
  dockingSide = 'LEFT',
  helpList = {}
}) => {
  if (!visible) return null;
  const isHorizontal = alignment === 'HORIZONTAL';
  const popupStyle = {};
  if (anchorPosition) {
    popupStyle.top = `${anchorPosition.top}px`;
    if (dockingSide === 'LEFT') {
      popupStyle.left = `${anchorPosition.left + anchorPosition.width + 8}px`;
    } else {
      popupStyle.right = `${window.innerWidth - anchorPosition.left + 8}px`;
    }
  }
  return /*#__PURE__*/_react.default.createElement("div", {
    className: "az-nested-rail-overlay",
    onClick: onDismiss
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: `az-nested-rail-popup ${alignment.toLowerCase()}`,
    style: popupStyle,
    onClick: e => e.stopPropagation()
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: `nested-content ${isHorizontal ? 'row' : 'column'}`
  }, items.map((item, index) => renderItem(item, index)))));
};
var _default = exports.default = AzNestedRailPopup;
//# sourceMappingURL=AzNestedRailPopup.js.map