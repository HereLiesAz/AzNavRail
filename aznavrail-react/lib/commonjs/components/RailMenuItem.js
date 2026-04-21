"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.RailMenuItem = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const RailMenuItem = ({
  item,
  depth,
  isExpandedHost,
  onToggleHost,
  onItemClick,
  renderSubItems
}) => {
  const [displayOption, setDisplayOption] = (0, _react.useState)(item.selectedOption);
  const timerRef = (0, _react.useRef)(null);
  (0, _react.useEffect)(() => {
    setDisplayOption(item.selectedOption);
  }, [item.selectedOption]);
  const handlePress = () => {
    if (item.isHost) {
      onToggleHost();
    } else if (item.isCycler && item.options) {
      const options = item.options;
      const currentIndex = options.indexOf(displayOption || '');
      const nextIndex = (currentIndex + 1) % options.length;
      const nextOption = options[nextIndex];
      setDisplayOption(nextOption);
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        if (item.onClick) item.onClick();
      }, 1000);
    } else {
      onItemClick();
    }
  };
  const displayText = item.text || (item.isChecked ? item.toggleOnText : item.toggleOffText) || displayOption || '';
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, null, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handlePress,
    style: [styles.menuItem, {
      paddingLeft: 16 + depth * 16
    }],
    accessibilityRole: "menuitem",
    accessibilityLabel: displayText,
    accessibilityState: {
      expanded: item.isHost ? isExpandedHost : undefined
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: [styles.menuItemText, {
      fontWeight: item.isHost ? 'bold' : 'normal'
    }]
  }, displayText), item.isHost && /*#__PURE__*/_react.default.createElement(_reactNative.Text, null, isExpandedHost ? '▲' : '▼')), item.isHost && isExpandedHost && renderSubItems());
};
exports.RailMenuItem = RailMenuItem;
const styles = _reactNative.StyleSheet.create({
  menuItem: {
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  menuItemText: {
    fontSize: 16
  }
});
//# sourceMappingURL=RailMenuItem.js.map