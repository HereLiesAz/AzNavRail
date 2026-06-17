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
/** Declarative description of a single field in an `AzForm` when using the `entries` API. */

const AzFormContext = /*#__PURE__*/(0, _react.createContext)(undefined);

/** Props for `AzForm` — a container that gathers `AzTextBox` inputs and emits a single submit payload. */

/**
 * Form container with two equivalent APIs:
 *  - Pass an `entries` array for a declarative spec mirroring the Android `AzForm` builder.
 *  - Or nest `<AzFormEntry>` children for a stack of inputs sharing the form context.
 *
 * Calls `onSubmit` with a `{ fieldName: value }` map when the user taps the inline submit button.
 */
const AzForm = ({
  formName,
  onSubmit,
  outlineColor = '#6200ee',
  outlined = true,
  submitButtonContent,
  children,
  style,
  entries,
  trailingIcon
}) => {
  const [formData, setFormData] = (0, _react.useState)(() => {
    const initial = {};
    if (entries) {
      entries.forEach(e => {
        initial[e.name] = e.initialValue || '';
      });
    }
    return initial;
  });
  const updateField = (0, _react.useCallback)((name, value) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  }, []);
  const registerField = (0, _react.useCallback)((name, initialValue) => {
    setFormData(prev => {
      if (prev[name] === undefined) {
        return {
          ...prev,
          [name]: initialValue
        };
      }
      return prev;
    });
  }, []);
  const handleSubmit = () => {
    onSubmit(formData);
  };

  // Modern Android-parity rendering
  if (entries && entries.length > 0) {
    return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      style: [styles.container, style]
    }, entries.map((entry, index) => {
      const isLast = index === entries.length - 1;
      const returnKeyType = entry.returnKeyType || (isLast ? 'send' : 'next');
      const val = formData[entry.name] !== undefined ? formData[entry.name] : entry.initialValue || '';
      const textBox = /*#__PURE__*/_react.default.createElement(_AzTextBox.AzTextBox, {
        value: val,
        onValueChange: t => updateField(entry.name, t),
        historyContext: formName,
        hint: entry.hint,
        outlined: outlined,
        outlineColor: outlineColor,
        multiline: entry.multiline,
        secret: entry.secret,
        leadingIcon: entry.leadingIcon,
        trailingIcon: trailingIcon,
        isError: entry.isError,
        enabled: entry.enabled,
        keyboardType: entry.keyboardType,
        returnKeyType: returnKeyType,
        showSubmitButton: false,
        containerStyle: isLast ? {
          flex: 1,
          marginBottom: 0
        } : undefined,
        onSubmitEditing: isLast ? handleSubmit : undefined
      });
      if (isLast) {
        return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
          key: entry.name,
          style: styles.lastRow
        }, textBox, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
          style: {
            width: 8
          }
        }), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
          onPress: handleSubmit,
          style: [styles.paritySubmitButton, {
            backgroundColor: 'transparent',
            borderColor: outlineColor,
            borderWidth: outlined ? 0 : 1
          }]
        }, submitButtonContent || /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
          style: {
            color: outlineColor,
            fontSize: 12,
            fontWeight: 'bold'
          }
        }, "GO")));
      }
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: entry.name
      }, textBox);
    }));
  }

  // Legacy rendering
  return /*#__PURE__*/_react.default.createElement(AzFormContext.Provider, {
    value: {
      updateField,
      registerField,
      formName,
      outlineColor,
      outlined,
      formData
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, style]
  }, children, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleSubmit,
    style: [styles.submitButton, {
      backgroundColor: 'transparent',
      // Match main component background?
      borderColor: outlineColor,
      borderWidth: outlined ? 0 : 1 // Inverse
    }]
  }, submitButtonContent || /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: outlineColor
    }
  }, "Submit"))));
};

/** Props for `AzFormEntry`, an `AzTextBox` registered into the surrounding `AzForm` by `name`. */
exports.AzForm = AzForm;
/** Single `AzTextBox` row that registers itself into the nearest `AzForm` context. */
const AzFormEntry = ({
  name,
  initialValue = '',
  ...props
}) => {
  const context = (0, _react.useContext)(AzFormContext);
  if (!context) {
    throw new Error('AzFormEntry must be used within an AzForm');
  }
  const {
    updateField,
    registerField,
    formName,
    outlineColor,
    outlined,
    formData
  } = context;
  (0, _react.useEffect)(() => {
    registerField(name, initialValue);
  }, [name, initialValue, registerField]);
  const handleChange = text => {
    updateField(name, text);
    if (props.onValueChange) props.onValueChange(text);
  };
  const value = formData[name] !== undefined ? formData[name] : initialValue;
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      flexDirection: 'row',
      marginBottom: 8
    }
  }, /*#__PURE__*/_react.default.createElement(_AzTextBox.AzTextBox, _extends({}, props, {
    value: value,
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
  },
  lastRow: {
    flexDirection: 'row',
    alignItems: 'center'
  },
  paritySubmitButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    height: 40
  }
});
//# sourceMappingURL=AzForm.js.map