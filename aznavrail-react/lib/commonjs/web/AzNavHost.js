"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireDefault(require("react"));
var _AzNavRail = _interopRequireDefault(require("./AzNavRail"));
require("./AzNavHost.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
/**
 * A container component that manages the layout of the Navigation Rail,
 * background content, and onscreen content with safe zones.
 *
 * @param {object} railProps - Props to be passed to the AzNavRail component.
 * @param {React.ReactNode} background - Content rendered in the background (z-index 0).
 * @param {React.ReactNode} children - Main screen content (z-index 1), confined to safe zones.
 */
const AzNavHost = ({
  railProps = {},
  background,
  children
}) => {
  const settings = railProps.settings || {};
  const {
    dockingSide = 'LEFT',
    collapsedRailWidth = '80px'
  } = settings;

  // Apply padding to content to avoid overlap with the persistent rail strip
  const paddingSide = dockingSide === 'RIGHT' ? 'paddingRight' : 'paddingLeft';
  const paddingValue = collapsedRailWidth;
  return /*#__PURE__*/_react.default.createElement("div", {
    className: "az-nav-host"
  }, /*#__PURE__*/_react.default.createElement("div", {
    className: "az-nav-host-background"
  }, background), /*#__PURE__*/_react.default.createElement("div", {
    className: "az-nav-host-content",
    style: {
      [paddingSide]: paddingValue
    }
  }, children), /*#__PURE__*/_react.default.createElement("div", {
    className: `az-nav-host-rail-wrapper ${dockingSide === 'RIGHT' ? 'right' : 'left'}`
  }, /*#__PURE__*/_react.default.createElement(_AzNavRail.default, railProps)));
};
var _default = exports.default = AzNavHost;
//# sourceMappingURL=AzNavHost.js.map