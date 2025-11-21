"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzFormEntry = exports.AzForm = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzTextBox = require("./AzTextBox");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const AzFormContext = /*#__PURE__*/(0, _react.createContext)(undefined);
const AzForm = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style
}) => {
  const [formData, setFormData] = (0, _react.useState)({});
  const updateField = (name, value) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };
  const handleSubmit = () => {
    onSubmit(formData);
  };
  return /*#__PURE__*/_react.default.createElement(AzFormContext.Provider, {
    value: {
      updateField,
      formName,
      outlineColor,
      outlined
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, style]
  }, children, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleSubmit,
    style: [styles.submitButton, {
      backgroundColor: 'transparent',
      // Match main component background?
      borderColor: outlineColor,
      borderWidth: !outlined ? 1 : 0 // Inverse
    }]
  }, submitButtonContent || /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: outlineColor
    }
  }, "Submit"))));
};
exports.AzForm = AzForm;
const AzFormEntry = ({
  name,
  ...props
}) => {
  const context = (0, _react.useContext)(AzFormContext);
  if (!context) {
    throw new Error('AzFormEntry must be used within an AzForm');
  }
  const {
    updateField,
    formName,
    outlineColor,
    outlined
  } = context;
  const handleChange = text => {
    updateField(name, text);
    if (props.onValueChange) props.onValueChange(text);
  };
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      flexDirection: 'row',
      marginBottom: 8
    }
  }, /*#__PURE__*/_react.default.createElement(_AzTextBox.AzTextBox, _extends({}, props, {
    onValueChange: handleChange,
    historyContext: formName // Use formName as history context
    ,
    outlineColor: outlineColor // Inherit
    ,
    outlined: outlined,
    containerStyle: {
      flex: 1
    },
    showSubmitButton: false
  })));
};
exports.AzFormEntry = AzFormEntry;
const styles = _reactNative.StyleSheet.create({
  container: {
    padding: 8
  },
  submitButton: {
    padding: 12,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8
  }
});
//# sourceMappingURL=AzForm.js.map