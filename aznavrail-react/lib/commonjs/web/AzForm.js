"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _react = _interopRequireWildcard(require("react"));
var _AzTextBox = _interopRequireDefault(require("./AzTextBox"));
var _AzButton = _interopRequireDefault(require("./AzButton"));
require("./AzForm.css");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
/**
 * A form component that manages multiple entries.
 *
 * @param {object} props
 * @param {Array<object>} props.entries - The list of form entries.
 *   Each entry object can have:
 *   - name (string): Unique identifier.
 *   - hint (string): Placeholder.
 *   - secret (boolean): Is password?
 *   - multiline (boolean): Is textarea?
 *   - leadingIcon (ReactNode)
 *   - initialValue (string)
 * @param {React.ReactNode} [props.trailingIcon] - Icon applied to all entries.
 * @param {function} props.onSubmit - Callback with map of values { name: value }.
 * @param {string} [props.submitText='Submit'] - Text for the submit button.
 * @param {string} [props.color='currentColor'] - The color.
 * @param {boolean} [props.isLoading=false] - Whether the form is submitting.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzForm = ({
  entries = [],
  trailingIcon,
  onSubmit,
  submitText = 'Submit',
  color = 'currentColor',
  isLoading = false,
  className = '',
  style = {}
}) => {
  const [values, setValues] = (0, _react.useState)(() => {
    const initial = {};
    entries.forEach(e => {
      initial[e.name] = e.initialValue || '';
    });
    return initial;
  });
  const handleChange = (name, newValue) => {
    setValues(prev => ({
      ...prev,
      [name]: newValue
    }));
  };
  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit(values);
    }
  };
  return /*#__PURE__*/_react.default.createElement("div", {
    className: `az-form-container ${className}`,
    style: style
  }, entries.map(entry => /*#__PURE__*/_react.default.createElement(_AzTextBox.default, {
    key: entry.name,
    value: values[entry.name],
    onValueChange: val => handleChange(entry.name, val),
    hint: entry.hint,
    secret: entry.secret,
    multiline: entry.multiline,
    leadingIcon: entry.leadingIcon,
    trailingIcon: trailingIcon // Applied to all entries
    ,
    color: color,
    enabled: !isLoading
  })), /*#__PURE__*/_react.default.createElement(_AzButton.default, {
    text: submitText,
    onClick: handleSubmit,
    color: color,
    isLoading: isLoading,
    shape: "RECTANGLE",
    style: {
      alignSelf: 'flex-end'
    }
  }));
};
var _default = exports.default = AzForm;
//# sourceMappingURL=AzForm.js.map