function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useRef, useState, useEffect } from 'react';
import { View, PanResponder, Animated, StyleSheet, TouchableOpacity, Text, Modal, Linking } from 'react-native';
import { AzButton } from './AzButton';
import { AzTextBox } from './AzTextBox';
export const DraggableRailItemWrapper = ({
  item,
  index,
  onDragStart,
  onDragEnd,
  onDragMove,
  offsetY,
  style,
  translucentBackground
}) => {
  const pan = useRef(new Animated.ValueXY()).current;
  const [isDragging, setIsDragging] = useState(false);
  const [showHiddenMenu, setShowHiddenMenu] = useState(false);
  const [menuPosition, setMenuPosition] = useState({
    x: 0,
    y: 0
  });
  useEffect(() => {
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
  useEffect(() => {
    // If this item is being displaced by another item being dragged
    // offsetY prop drives the displacement animation
  }, [offsetY]);
  const panResponder = useRef(PanResponder.create({
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
      Animated.spring(pan, {
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
    return /*#__PURE__*/React.createElement(Modal, {
      transparent: true,
      visible: showHiddenMenu,
      onRequestClose: closeMenu
    }, /*#__PURE__*/React.createElement(TouchableOpacity, {
      style: styles.modalOverlay,
      activeOpacity: 1,
      onPress: closeMenu
    }, /*#__PURE__*/React.createElement(View, {
      style: [styles.hiddenMenu, {
        top: menuPosition.y,
        left: menuPosition.x
      }, translucentBackground ? {
        backgroundColor: translucentBackground
      } : {}]
    }, item.hiddenMenu.map((menuItem, i) => {
      if (menuItem.isInput) {
        return /*#__PURE__*/React.createElement(View, {
          key: i,
          style: styles.hiddenMenuItem
        }, /*#__PURE__*/React.createElement(AzTextBox, {
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
      return /*#__PURE__*/React.createElement(TouchableOpacity, {
        key: i,
        style: styles.hiddenMenuItem,
        onPress: () => {
          if (menuItem.onClick) menuItem.onClick();
          if (menuItem.route) {
            Linking.openURL(menuItem.route).catch(err => console.error("Couldn't open URL", err));
          }
          setShowHiddenMenu(false);
        }
      }, /*#__PURE__*/React.createElement(Text, {
        style: styles.hiddenMenuItemText
      }, menuItem.text));
    }))));
  };

  // Combine dragging transform with displacement transform
  const transform = [{
    translateY: Animated.add(pan.y, offsetY)
  }, {
    scale: isDragging ? 1.1 : 1
  }];
  return /*#__PURE__*/React.createElement(View, {
    style: [styles.container, style, {
      zIndex: isDragging ? 100 : 1
    }]
  }, /*#__PURE__*/React.createElement(Animated.View, _extends({
    style: {
      transform
    }
  }, panResponder.panHandlers), /*#__PURE__*/React.createElement(TouchableOpacity, {
    activeOpacity: 0.8,
    onLongPress: handleLongPress,
    onPress: () => item.onClick && item.onClick(),
    delayLongPress: 500
  }, /*#__PURE__*/React.createElement(AzButton, {
    text: item.text,
    color: item.color,
    shape: item.shape,
    enabled: !item.disabled
    // Pass a dummy click handler since we handle clicks on the wrapper for gesture conflict resolution
    ,
    onClick: () => {}
  }))), renderHiddenMenu());
};
const styles = StyleSheet.create({
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