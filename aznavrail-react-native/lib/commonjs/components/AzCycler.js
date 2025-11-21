"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzCycler = void 0;
var _react = _interopRequireWildcard(require("react"));
var _AzButton = require("./AzButton");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
const AzCycler = ({
  options,
  selectedOption,
  onCycle,
  color,
  shape,
  style,
  disabled,
  disabledOptions = [],
  testID
}) => {
  // Local state to show the currently "previewed" option
  const [displayOption, setDisplayOption] = (0, _react.useState)(selectedOption);
  const timerRef = (0, _react.useRef)(null);

  // Sync local state if external selection changes (e.g. initial load or external update)
  (0, _react.useEffect)(() => {
    setDisplayOption(selectedOption);
  }, [selectedOption]);
  const handlePress = () => {
    if (disabled) return;
    const currentIndex = options.indexOf(displayOption);
    let nextIndex = (currentIndex + 1) % options.length;
    let nextOption = options[nextIndex];

    // Skip disabled options if any
    // Guard against infinite loop if all are disabled
    let loopCount = 0;
    while (disabledOptions.includes(nextOption) && loopCount < options.length) {
      nextIndex = (nextIndex + 1) % options.length;
      nextOption = options[nextIndex];
      loopCount++;
    }
    setDisplayOption(nextOption);

    // Reset timer
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      onCycle(nextOption);
    }, 1000);
  };

  // Cleanup timer on unmount
  (0, _react.useEffect)(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);
  return /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, {
    text: displayOption,
    onClick: handlePress,
    color: color,
    shape: shape,
    style: style,
    disabled: disabled,
    testID: testID
  });
};
exports.AzCycler = AzCycler;
//# sourceMappingURL=AzCycler.js.map