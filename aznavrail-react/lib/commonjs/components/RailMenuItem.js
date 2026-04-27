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
  let displayText = item.menuText ?? item.text;
  if (item.isToggle) {
    displayText = item.isChecked ? item.menuToggleOnText ?? item.toggleOnText : item.menuToggleOffText ?? item.toggleOffText;
  } else if (item.isCycler) {
    if (item.options && displayOption) {
      const idx = item.options.indexOf(displayOption);
      if (idx !== -1 && item.menuOptions && idx >= 0 && idx < item.menuOptions.length) {
        displayText = item.menuOptions[idx];
      } else {
        if (item.menuOptions) {
          console.warn(`Cycler configuration mismatch for item='${item.text}': selectedOption='${displayOption}', index=${idx}, optionsSize=${item.options.length}, menuOptionsSize=${item.menuOptions.length}`);
        }
        displayText = displayOption;
      }
    } else {
      displayText = displayOption || '';
    }
  }
  const accessibilityState = {};
  if (item.isToggle) accessibilityState.checked = item.isChecked;
  if (item.isHost || item.isNestedRail) accessibilityState.expanded = isExpandedHost;
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, null, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handlePress,
    style: [styles.menuItem, {
      paddingLeft: 16 + depth * 16,
      backgroundColor: !item.isHost ? `${item.fillColor ?? item.color ?? '#000000'}1F` : 'transparent'
    }],
    accessibilityRole: item.isToggle ? "switch" : "menuitem",
    accessibilityLabel: displayText,
    accessibilityState: accessibilityState
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: [styles.menuItemText, {
      fontWeight: item.isHost ? 'bold' : 'normal',
      color: item.textColor ?? item.color ?? '#000000'
    }]
  }, displayText), item.isHost && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      marginLeft: 8
    }
  }, isExpandedHost ? '▲' : '▼')), item.isHost && isExpandedHost && renderSubItems());
};
exports.RailMenuItem = RailMenuItem;
const styles = _reactNative.StyleSheet.create({
  menuItem: {
    padding: 16,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center'
  },
  menuItemText: {
    fontSize: 16,
    textAlign: 'center',
    flex: 1
  }
});
//# sourceMappingURL=RailMenuItem.js.map