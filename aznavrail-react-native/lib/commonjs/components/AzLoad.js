"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzLoad = void 0;
var _react = _interopRequireDefault(require("react"));
var _reactNative = require("react-native");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const AzLoad = () => /*#__PURE__*/_react.default.createElement(_reactNative.View, {
  style: styles.container
}, /*#__PURE__*/_react.default.createElement(_reactNative.ActivityIndicator, {
  size: "large",
  color: "#6200ee"
}));
exports.AzLoad = AzLoad;
const styles = _reactNative.StyleSheet.create({
  container: {
    width: 80,
    height: 80,
    // Square container
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white',
    borderRadius: 10,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84
  }
});
//# sourceMappingURL=AzLoad.js.map