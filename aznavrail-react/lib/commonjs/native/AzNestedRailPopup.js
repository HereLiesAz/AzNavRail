"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzNestedRailPopup = void 0;
var _react = _interopRequireDefault(require("react"));
var _reactNative = require("react-native");
var _types = require("../types");
var _AzNavRailDefaults = require("../AzNavRailDefaults");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
const AzNestedRailPopup = ({
  visible,
  onDismiss,
  items,
  alignment,
  renderItem,
  anchorPosition,
  dockingSide = 'LEFT',
  helpList = {}
}) => {
  if (!visible) return null;
  const isHorizontal = alignment === _types.AzNestedRailAlignment.HORIZONTAL;
  const window = _reactNative.Dimensions.get('window');
  const popupStyle = {
    position: 'absolute',
    backgroundColor: '#f9f9f9',
    borderRadius: 8,
    padding: 8,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84
  };
  if (anchorPosition) {
    if (isHorizontal) {
      popupStyle.top = anchorPosition.y;
      if (dockingSide === 'LEFT') {
        popupStyle.left = anchorPosition.x + anchorPosition.width + 8;
      } else {
        popupStyle.right = window.width - anchorPosition.x + 8;
      }
      popupStyle.maxWidth = window.width * 0.8;
      popupStyle.maxHeight = _AzNavRailDefaults.AzNavRailDefaults.ButtonWidth + 16;
    } else {
      // Vertical popup centered on screen as per Android spec if possible,
      // but usually anchored next to parent
      popupStyle.top = Math.max(window.height * 0.1, anchorPosition.y - items.length * 30);
      if (dockingSide === 'LEFT') {
        popupStyle.left = anchorPosition.x + anchorPosition.width + 8;
      } else {
        popupStyle.right = window.width - anchorPosition.x + 8;
      }
      popupStyle.maxHeight = window.height * 0.8;
      popupStyle.width = _AzNavRailDefaults.AzNavRailDefaults.ButtonWidth + 16;
    }
  }
  return /*#__PURE__*/_react.default.createElement(_reactNative.Modal, {
    transparent: true,
    visible: visible,
    onRequestClose: onDismiss,
    animationType: "fade"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableWithoutFeedback, {
    onPress: onDismiss
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.overlay
  }, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableWithoutFeedback, null, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: popupStyle
  }, /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    horizontal: isHorizontal,
    contentContainerStyle: isHorizontal ? styles.horizontalContent : styles.verticalContent
  }, items.map((item, index) => renderItem(item, index))))))));
};
exports.AzNestedRailPopup = AzNestedRailPopup;
const styles = _reactNative.StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.1)'
  },
  horizontalContent: {
    flexDirection: 'row',
    alignItems: 'center'
  },
  verticalContent: {
    flexDirection: 'column',
    alignItems: 'center'
  }
});
//# sourceMappingURL=AzNestedRailPopup.js.map