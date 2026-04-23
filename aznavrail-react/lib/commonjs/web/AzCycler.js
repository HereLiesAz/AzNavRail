"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireWildcard(require("react"));
require("./AzCycler.css");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
/**
 * A component that cycles through a list of options when clicked.
 * It waits 1 second after the last click before triggering the action.
 *
 * @param {object} props
 * @param {Array<string>} props.options - The list of options.
 * @param {string} props.value - The current selected value.
 * @param {function} props.onValueChange - Callback when the value changes (confirmed after delay).
 * @param {string} [props.label] - Optional label.
 * @param {boolean} [props.enabled=true] - Whether the cycler is enabled.
 * @param {string} [props.color='currentColor'] - The color.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzCycler = ({
  options = [],
  value,
  onValueChange,
  label,
  enabled = true,
  color = 'currentColor',
  className = '',
  style = {}
}) => {
  const [displayedValue, setDisplayedValue] = (0, _react.useState)(value);
  const timerRef = (0, _react.useRef)(null);
  (0, _react.useEffect)(() => {
    setDisplayedValue(value);
  }, [value]);
  (0, _react.useEffect)(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);
  const handleClick = () => {
    if (!enabled || options.length === 0) return;
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    const currentIndex = options.indexOf(displayedValue);
    const nextIndex = (currentIndex + 1) % options.length;
    const nextValue = options[nextIndex];
    setDisplayedValue(nextValue);
    timerRef.current = setTimeout(() => {
      onValueChange(nextValue);
      timerRef.current = null;
    }, 1000);
  };
  return /*#__PURE__*/_react.default.createElement("div", {
    className: `az-cycler-container ${!enabled ? 'disabled' : ''} ${className}`,
    onClick: handleClick,
    style: {
      borderColor: color,
      color: color,
      ...style
    }
  }, label && /*#__PURE__*/_react.default.createElement("span", {
    className: "az-cycler-label"
  }, label), /*#__PURE__*/_react.default.createElement("span", {
    className: "az-cycler-value"
  }, displayedValue));
};
var _default = exports.default = AzCycler;
//# sourceMappingURL=AzCycler.js.map