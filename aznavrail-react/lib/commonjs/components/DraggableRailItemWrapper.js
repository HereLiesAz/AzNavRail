"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DraggableRailItemWrapper = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzButton = require("./AzButton");
var _AzTextBox = require("./AzTextBox");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const DraggableRailItemWrapper = ({
  item,
  index,
  onDragStart,
  onDragEnd,
  onDragMove,
  offsetY,
  style,
  translucentBackground
}) => {
  const pan = (0, _react.useRef)(new _reactNative.Animated.ValueXY()).current;
  const [isDragging, setIsDragging] = (0, _react.useState)(false);
  const [showHiddenMenu, setShowHiddenMenu] = (0, _react.useState)(false);
  const [menuPosition, setMenuPosition] = (0, _react.useState)({
    x: 0,
    y: 0
  });
  (0, _react.useEffect)(() => {
    if (item.forceHiddenMenuOpen) {
      setShowHiddenMenu(true);
    }
  }, [item.forceHiddenMenuOpen]);
  const closeMenu = () => {
    setShowHiddenMenu(false);
    if (item.onHiddenMenuDismiss) {
      item.onHiddenMenuDismiss();
    }
  };

  // Create a listener for offset updates
  (0, _react.useEffect)(() => {
    // If this item is being displaced by another item being dragged
    // offsetY prop drives the displacement animation
  }, [offsetY]);
  const panResponder = (0, _react.useRef)(_reactNative.PanResponder.create({
    onStartShouldSetPanResponder: () => false,
    onMoveShouldSetPanResponder: (_, gestureState) => {
      // Only start drag if vertical movement is significant
      return Math.abs(gestureState.dy) > 10;
    },
    onPanResponderGrant: () => {
      setIsDragging(true);
      pan.setOffset({
        x: 0,
        // Constrain horizontal
        y: pan.y.__getValue()
      });
      onDragStart(index);
    },
    onPanResponderMove: (_, gestureState) => {
      // Constrain to vertical
      pan.setValue({
        x: 0,
        y: gestureState.dy
      });
      onDragMove(gestureState.dy, index);
    },
    onPanResponderRelease: () => {
      pan.flattenOffset();
      setIsDragging(false);
      onDragEnd(index);

      // Reset position visually - the list reorder handles the actual move
      _reactNative.Animated.spring(pan, {
        toValue: {
          x: 0,
          y: 0
        },
        useNativeDriver: false
      }).start();
    }
  })).current;
  const handleLongPress = event => {
    // Show hidden menu
    const {
      pageX,
      pageY
    } = event.nativeEvent;
    setMenuPosition({
      x: pageX + 60,
      y: pageY - 20
    }); // Offset slightly
    setShowHiddenMenu(true);
  };
  const renderHiddenMenu = () => {
    if (!showHiddenMenu || !item.hiddenMenu) return null;
    return /*#__PURE__*/_react.default.createElement(_reactNative.Modal, {
      transparent: true,
      visible: showHiddenMenu,
      onRequestClose: closeMenu
    }, /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      style: styles.modalOverlay,
      activeOpacity: 1,
      onPress: closeMenu
    }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      style: [styles.hiddenMenu, {
        top: menuPosition.y,
        left: menuPosition.x
      }, translucentBackground ? {
        backgroundColor: translucentBackground
      } : {}]
    }, item.hiddenMenu.map((menuItem, i) => {
      if (menuItem.isInput) {
        return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
          key: i,
          style: styles.hiddenMenuItem
        }, /*#__PURE__*/_react.default.createElement(_AzTextBox.AzTextBox, {
          initialValue: menuItem.initialValue,
          hint: menuItem.hint,
          onValueChange: menuItem.onValueChange,
          onSubmit: val => {
            if (menuItem.onValueChange) menuItem.onValueChange(val);
            closeMenu();
          },
          showSubmitButton: true,
          outlined: true
        }));
      }
      return /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
        key: i,
        style: styles.hiddenMenuItem,
        onPress: () => {
          if (menuItem.onClick) menuItem.onClick();
          if (menuItem.route) {
            _reactNative.Linking.openURL(menuItem.route).catch(err => console.error("Couldn't open URL", err));
          }
          setShowHiddenMenu(false);
        }
      }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
        style: styles.hiddenMenuItemText
      }, menuItem.text));
    }))));
  };

  // Combine dragging transform with displacement transform
  const transform = [{
    translateY: _reactNative.Animated.add(pan.y, offsetY)
  }, {
    scale: isDragging ? 1.1 : 1
  }];
  return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: [styles.container, style, {
      zIndex: isDragging ? 100 : 1
    }]
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Animated.View, _extends({
    style: {
      transform
    }
  }, panResponder.panHandlers), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    activeOpacity: 0.8,
    onLongPress: handleLongPress,
    onPress: () => item.onClick && item.onClick(),
    delayLongPress: 500
  }, /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, {
    text: item.text,
    color: item.color,
    shape: item.shape,
    enabled: !item.disabled
    // Pass a dummy click handler since we handle clicks on the wrapper for gesture conflict resolution
    ,
    onClick: () => {}
  }))), renderHiddenMenu());
};
exports.DraggableRailItemWrapper = DraggableRailItemWrapper;
const styles = _reactNative.StyleSheet.create({
  container: {
    marginBottom: 8 // Standard spacing
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'transparent'
  },
  hiddenMenu: {
    position: 'absolute',
    backgroundColor: 'white',
    borderRadius: 4,
    borderWidth: 2,
    borderColor: '#6200ee',
    // Primary color ideally
    padding: 8,
    minWidth: 150,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84
  },
  hiddenMenuItem: {
    paddingVertical: 8,
    paddingHorizontal: 12
  },
  hiddenMenuItemText: {
    fontSize: 16,
    color: 'black'
  }
});
//# sourceMappingURL=DraggableRailItemWrapper.js.map