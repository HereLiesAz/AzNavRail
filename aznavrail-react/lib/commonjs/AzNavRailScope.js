"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzRailToggle = exports.AzRailSubToggle = exports.AzRailSubItem = exports.AzRailSubCycler = exports.AzRailItem = exports.AzRailHostItem = exports.AzRailCycler = exports.AzNavRailContext = exports.AzMenuToggle = exports.AzMenuSubToggle = exports.AzMenuSubItem = exports.AzMenuSubCycler = exports.AzMenuItem = exports.AzMenuHostItem = exports.AzMenuCycler = exports.AzDivider = void 0;
var _react = _interopRequireWildcard(require("react"));
var _types = require("./types");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzNavRailContext = exports.AzNavRailContext = /*#__PURE__*/_react.default.createContext(null);
const useAzItem = item => {
  const context = (0, _react.useContext)(AzNavRailContext);
  const previousItem = (0, _react.useRef)(null);
  (0, _react.useEffect)(() => {
    if (!context) return;

    // Simple comparison to avoid spamming updates
    const prev = previousItem.current;
    const isSame = prev && prev.id === item.id && prev.text === item.text && prev.disabled === item.disabled && prev.isChecked === item.isChecked && prev.selectedOption === item.selectedOption &&
    // Compare arrays
    JSON.stringify(prev.options) === JSON.stringify(item.options) && prev.shape === item.shape && prev.color === item.color;
    if (!isSame) {
      context.register(item);
      previousItem.current = item;
    }

    // Cleanup only on unmount
    return () => {
      // We don't unregister on every update, only on unmount
      // But if ID changes (rare), we should unregister old ID.
    };
  }, [context, item]); // dependencies should capture all props

  (0, _react.useEffect)(() => {
    if (context) {
      return () => context.unregister(item.id);
    }
    return undefined;
  }, [context, item.id]);
  return null;
};

// --- Component Wrappers ---

const AzRailItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzRailItem = AzRailItem;
const AzMenuItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzMenuItem = AzMenuItem;
const AzRailToggle = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false
  });
  return null;
};
exports.AzRailToggle = AzRailToggle;
const AzMenuToggle = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false
  });
  return null;
};
exports.AzMenuToggle = AzMenuToggle;
const AzRailCycler = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzRailCycler = AzRailCycler;
const AzMenuCycler = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzMenuCycler = AzMenuCycler;
const AzDivider = () => {
  // ID needed?
  const id = (0, _react.useRef)(`divider-${Math.random()}`).current;
  useAzItem({
    id,
    text: '',
    isRailItem: false,
    // Dividers usually in menu only? Kotlin: azDivider() adds to menu.
    // Wait, "Easily add dividers to your menu".
    // Are dividers on rail? "Rectangular rail items have 2.dp of vertical padding...".
    // I'll assume dividers are menu only unless specified.
    // Kotlin azDivider() implementation creates AzNavItem with isDivider=true.
    // And it seems it's added to the list. The Rail rendering logic decides if it shows.
    // Usually dividers are horizontal lines in menu.
    isToggle: false,
    isCycler: false,
    isDivider: true,
    collapseOnClick: false,
    shape: _types.AzButtonShape.NONE,
    disabled: false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzDivider = AzDivider;
const AzRailHostItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: true,
    isSubItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    // Hosts expand/collapse, don't close rail
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    // Initial state, managed by parent usually
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzRailHostItem = AzRailHostItem;
const AzMenuHostItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: true,
    isSubItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    shape: props.shape || _types.AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzMenuHostItem = AzMenuHostItem;
const AzRailSubItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzRailSubItem = AzRailSubItem;
const AzMenuSubItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzMenuSubItem = AzMenuSubItem;
const AzRailSubToggle = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false
  });
  return null;
};
exports.AzRailSubToggle = AzRailSubToggle;
const AzMenuSubToggle = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false
  });
  return null;
};
exports.AzMenuSubToggle = AzMenuSubToggle;
const AzRailSubCycler = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzRailSubCycler = AzRailSubCycler;
const AzMenuSubCycler = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: _types.AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};
exports.AzMenuSubCycler = AzMenuSubCycler;
//# sourceMappingURL=AzNavRailScope.js.map