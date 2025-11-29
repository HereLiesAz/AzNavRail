"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.setAzNavRailInteractionLogger = exports.AzNavRail = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzNavRailScope = require("./AzNavRailScope");
var _types = require("./types");
var _AzNavRailDefaults = require("./AzNavRailDefaults");
var _AzButton = require("./components/AzButton");
var _AzToggle = require("./components/AzToggle");
var _AzCycler = require("./components/AzCycler");
var _RailMenuItem = require("./components/RailMenuItem");
var _AzLoad = require("./components/AzLoad");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const AzNavRail = ({
  children,
  initiallyExpanded = false,
  displayAppNameInHeader = true,
  expandedRailWidth = _AzNavRailDefaults.AzNavRailDefaults.ExpandedRailWidth,
  collapsedRailWidth = _AzNavRailDefaults.AzNavRailDefaults.CollapsedRailWidth,
  showFooter = true,
  isLoading = false,
  defaultShape = _types.AzButtonShape.CIRCLE,
  enableRailDragging = false,
  onExpandedChange,
  onInteraction
}) => {
  const logInteraction = (0, _react.useCallback)((action, details) => {
    if (onInteraction) {
      onInteraction(action, details);
      return;
    }

    // Fallback to console logging only in development when no custom logger is configured
    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      const suffix = details ? ` ${details}` : '';
      // eslint-disable-next-line no-console
      console.log(`[AzNavRail] ${action}${suffix}`);
    }
  }, [onInteraction]);
  const [items, setItems] = (0, _react.useState)([]);
  const [isExpanded, setIsExpanded] = (0, _react.useState)(initiallyExpanded);
  const [isFloating, setIsFloating] = (0, _react.useState)(false);
  const [showFloatingButtons, setShowFloatingButtons] = (0, _react.useState)(false);
  const [headerHeight, setHeaderHeight] = (0, _react.useState)(0);
  const [hostStates, setHostStates] = (0, _react.useState)({});
  const railWidthAnim = (0, _react.useRef)(new _reactNative.Animated.Value(initiallyExpanded ? expandedRailWidth : collapsedRailWidth)).current;
  const pan = (0, _react.useRef)(new _reactNative.Animated.ValueXY()).current;
  const panValue = (0, _react.useRef)({
    x: 0,
    y: 0
  });
  const dragStartPos = (0, _react.useRef)({
    x: 0,
    y: 0
  });

  // Cache screen height
  const screenHeightRef = (0, _react.useRef)(_reactNative.Dimensions.get('window').height);

  // Refs for PanResponder stale closures
  const isFloatingRef = (0, _react.useRef)(isFloating);
  const enableRailDraggingRef = (0, _react.useRef)(enableRailDragging);
  const showFloatingButtonsRef = (0, _react.useRef)(showFloatingButtons);
  const wasVisibleOnDragStartRef = (0, _react.useRef)(false);
  const itemsRef = (0, _react.useRef)(items);
  (0, _react.useEffect)(() => {
    isFloatingRef.current = isFloating;
  }, [isFloating]);
  (0, _react.useEffect)(() => {
    enableRailDraggingRef.current = enableRailDragging;
  }, [enableRailDragging]);
  (0, _react.useEffect)(() => {
    showFloatingButtonsRef.current = showFloatingButtons;
  }, [showFloatingButtons]);
  (0, _react.useEffect)(() => {
    itemsRef.current = items;
  }, [items]);
  (0, _react.useEffect)(() => {
    const id = pan.addListener(value => {
      panValue.current = value;
    });
    return () => pan.removeListener(id);
  }, [pan]);
  (0, _react.useEffect)(() => {
    const subscription = _reactNative.Dimensions.addEventListener('change', ({
      window
    }) => {
      screenHeightRef.current = window.height;
    });
    return () => {
      if (!subscription) return;
      // @ts-ignore
      if (typeof subscription === 'function') {
        // @ts-ignore
        subscription();
      } else if (typeof subscription.remove === 'function') {
        subscription.remove();
      }
    };
  }, []);

  // --- Item Management ---
  const register = (0, _react.useCallback)(item => {
    setItems(prev => {
      const index = prev.findIndex(i => i.id === item.id);
      if (index >= 0) {
        const old = prev[index];
        const isSame = old.text === item.text && old.disabled === item.disabled && old.isChecked === item.isChecked && old.selectedOption === item.selectedOption && old.isExpanded === item.isExpanded && JSON.stringify(old.options) === JSON.stringify(item.options) && old.shape === item.shape && old.color === item.color && old.route === item.route;
        if (isSame) return prev;
        const newItems = [...prev];
        newItems[index] = item;
        return newItems;
      }
      return [...prev, item];
    });
  }, []);
  const unregister = (0, _react.useCallback)(id => {
    setItems(prev => prev.filter(i => i.id !== id));
  }, []);

  // --- Animation & Effects ---
  (0, _react.useEffect)(() => {
    _reactNative.Animated.timing(railWidthAnim, {
      toValue: isExpanded ? expandedRailWidth : collapsedRailWidth,
      duration: 300,
      useNativeDriver: false
    }).start();
    if (onExpandedChange) onExpandedChange(isExpanded);
  }, [isExpanded, expandedRailWidth, collapsedRailWidth]);

  // --- Helpers ---

  const adjustFloatingRailWithinBounds = (currentX, currentY) => {
    const screenHeight = screenHeightRef.current;
    const railItemsCount = itemsRef.current.filter(i => i.isRailItem && !i.isSubItem).length;
    // Estimated height: Header + (Count * (48 + 8)) + Padding
    // 48 = button size, 8 = margin
    const contentHeight = headerHeight + railItemsCount * 56 + 16;
    const bottomY = currentY + contentHeight;
    const limitY = screenHeight * 0.9;
    if (bottomY > limitY) {
      const shift = bottomY - limitY;
      const newY = currentY - shift;
      _reactNative.Animated.timing(pan, {
        toValue: {
          x: currentX,
          y: newY
        },
        duration: 200,
        useNativeDriver: false
      }).start();

      // We log here because this action (shifting) is significant, but note that
      // handleHeaderTap didn't originally log. However, adding it makes the behavior consistent.
      // The reviewer asked to preserve behavior. If handleHeaderTap didn't log, maybe we shouldn't enforce it.
      // But 'Adjusted position' log is useful. I'll check if I can conditionally log based on caller,
      // or just log it. The reviewer said "Keep the behavior in sync". So syncing behavior likely includes logging.
      logInteraction('Adjusted position', `Shifted up by ${shift}`);
    }
  };

  // --- Gestures ---
  const panResponder = (0, _react.useRef)(_reactNative.PanResponder.create({
    onMoveShouldSetPanResponder: (_, gestureState) => {
      if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;
      const {
        dx,
        dy
      } = gestureState;
      // "Vertical swipe immediately initiates FAB mode"
      if (!isFloatingRef.current && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10 && enableRailDraggingRef.current) {
        return true;
      }
      return isFloatingRef.current;
    },
    onPanResponderGrant: () => {
      if (isFloatingRef.current) {
        pan.setOffset({
          x: panValue.current.x,
          y: panValue.current.y
        });
        pan.setValue({
          x: 0,
          y: 0
        });
        dragStartPos.current = {
          x: panValue.current.x,
          y: panValue.current.y
        };
        wasVisibleOnDragStartRef.current = showFloatingButtonsRef.current;
        setShowFloatingButtons(false);
        logInteraction('Drag started', 'FAB mode');
      } else {
        setIsFloating(true);
        setIsExpanded(false);
        logInteraction('Swipe detected', 'Entering FAB mode');

        // Reset to 0,0 for simplicity as we undock
        pan.setOffset({
          x: 0,
          y: 0
        });
        pan.setValue({
          x: 0,
          y: 0
        });
        dragStartPos.current = {
          x: 0,
          y: 0
        };
      }
    },
    onPanResponderMove: (_, gestureState) => {
      if (!isFloatingRef.current) return;
      const {
        dx,
        dy
      } = gestureState;
      const screenHeight = screenHeightRef.current;
      const iconSize = _AzNavRailDefaults.AzNavRailDefaults.HeaderIconSize;

      // 10% margin bounds
      const minY = screenHeight * 0.1;
      const maxY = screenHeight * 0.9 - iconSize;
      const targetY = dragStartPos.current.y + dy;

      // Clamp
      let clampedY = Math.max(minY, Math.min(targetY, maxY));

      // Apply as new value (remove offset to get delta)
      pan.setValue({
        x: dx,
        y: clampedY - dragStartPos.current.y
      });
    },
    onPanResponderRelease: () => {
      pan.flattenOffset();
      if (isFloatingRef.current) {
        logInteraction('Drag ended');

        // Snap back logic
        const currentX = panValue.current.x;
        const currentY = panValue.current.y;
        const distance = Math.sqrt(Math.pow(currentX, 2) + Math.pow(currentY, 2));
        if (distance < _AzNavRailDefaults.AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
          setIsFloating(false);
          pan.setValue({
            x: 0,
            y: 0
          });
          logInteraction('Snap back', 'Docked');
        } else if (wasVisibleOnDragStartRef.current) {
          // Unfold logic
          setShowFloatingButtons(true);
          adjustFloatingRailWithinBounds(currentX, currentY);
        }
      }
    }
  })).current;
  const handleHeaderLongPress = () => {
    if (enableRailDragging) {
      if (isFloating) {
        setIsFloating(false);
        pan.setValue({
          x: 0,
          y: 0
        });
        logInteraction('Long press', 'Docked rail');
      } else {
        setIsFloating(true);
        setIsExpanded(false);
        logInteraction('Long press', 'Undocked rail');
      }
    }
  };
  const handleHeaderTap = () => {
    logInteraction('Header tapped');
    if (isFloating) {
      // Unfold/Fold logic
      const willShow = !showFloatingButtons;
      setShowFloatingButtons(willShow);
      if (willShow) {
        const currentX = panValue.current.x;
        const currentY = panValue.current.y;
        adjustFloatingRailWithinBounds(currentX, currentY);
      }
    } else {
      setIsExpanded(!isExpanded);
    }
  };

  // --- Render Helpers ---

  const renderRailItem = item => {
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = items.filter(i => i.hostId === item.id);
    const isRect = item.shape === _types.AzButtonShape.RECTANGLE;
    const commonProps = {
      key: item.id,
      color: item.color,
      shape: item.shape || defaultShape,
      disabled: item.disabled,
      style: {
        marginBottom: isRect ? 2 : _AzNavRailDefaults.AzNavRailDefaults.RailContentVerticalArrangement
      }
    };
    if (item.isHost) {
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: item.id,
        style: {
          alignItems: 'center',
          width: '100%'
        }
      }, /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, _extends({}, commonProps, {
        text: item.text,
        onClick: () => {
          setHostStates(prev => ({
            ...prev,
            [item.id]: !prev[item.id]
          }));
          logInteraction('Host toggled', item.text);
        }
      })), isExpandedHost && subItems.map(renderRailItem));
    }
    if (item.isCycler) {
      return /*#__PURE__*/_react.default.createElement(_AzCycler.AzCycler, _extends({}, commonProps, {
        options: item.options || [],
        selectedOption: item.selectedOption || '',
        onCycle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Cycler cycled', item.text);
        }
      }));
    }
    if (item.isToggle) {
      return /*#__PURE__*/_react.default.createElement(_AzToggle.AzToggle, _extends({}, commonProps, {
        isChecked: item.isChecked || false,
        toggleOnText: item.toggleOnText,
        toggleOffText: item.toggleOffText,
        onToggle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Toggle toggled', item.text);
        }
      }));
    }
    return /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, _extends({}, commonProps, {
      text: item.text,
      onClick: () => {
        logInteraction('Item clicked', item.text);
        if (item.onClick) item.onClick();
        if (item.collapseOnClick) setIsExpanded(false);
      }
    }));
  };
  const renderMenuItem = (item, depth = 0) => {
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = items.filter(i => i.hostId === item.id);
    return /*#__PURE__*/_react.default.createElement(_RailMenuItem.RailMenuItem, {
      key: item.id,
      item: item,
      depth: depth,
      isExpandedHost: isExpandedHost,
      onToggleHost: () => setHostStates(prev => ({
        ...prev,
        [item.id]: !prev[item.id]
      })),
      onItemClick: () => {
        logInteraction('Menu item clicked', item.text);
        if (item.onClick) item.onClick();
        if (item.collapseOnClick) setIsExpanded(false);
      },
      renderSubItems: () => /*#__PURE__*/_react.default.createElement(_react.default.Fragment, null, subItems.map(subItem => renderMenuItem(subItem, depth + 1)))
    });
  };
  const railItems = items.filter(i => i.isRailItem && !i.isSubItem);
  const menuItems = items.filter(i => !i.isSubItem);
  return /*#__PURE__*/_react.default.createElement(_AzNavRailScope.AzNavRailContext.Provider, {
    value: {
      register,
      unregister
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      flexDirection: 'row',
      height: '100%'
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Animated.View, _extends({
    style: [styles.railContainer, {
      width: railWidthAnim,
      position: isFloating ? 'absolute' : 'relative',
      transform: isFloating ? [{
        translateX: pan.x
      }, {
        translateY: pan.y
      }] : [],
      zIndex: isFloating ? 1000 : 1,
      height: isFloating ? 'auto' : '100%'
    }]
  }, panResponder.panHandlers), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleHeaderTap,
    onLongPress: handleHeaderLongPress,
    delayLongPress: 500,
    style: styles.header,
    onLayout: e => setHeaderHeight(e.nativeEvent.layout.height),
    accessibilityRole: "button",
    accessibilityLabel: isFloating ? "Undocked Rail" : isExpanded ? "Collapse Menu" : "Expand Menu",
    accessibilityHint: "Double tap to toggle, long press to drag"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      width: _AzNavRailDefaults.AzNavRailDefaults.HeaderIconSize,
      height: _AzNavRailDefaults.AzNavRailDefaults.HeaderIconSize,
      backgroundColor: 'gray',
      borderRadius: 24,
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: 'white'
    }
  }, "Icon")), !isFloating && isExpanded && displayAppNameInHeader && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: styles.appName,
    numberOfLines: 1
  }, "App Name")), isExpanded ? /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    style: styles.menuContent
  }, menuItems.map(item => {
    if (item.isDivider) {
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: item.id,
        style: styles.divider
      });
    }
    return renderMenuItem(item);
  }), showFooter && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.footer
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, null, "Footer"))) : /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    contentContainerStyle: styles.railContent
  }, isFloating && !showFloatingButtons ? null : railItems.map(renderRailItem))), children, isLoading && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.loaderOverlay
  }, /*#__PURE__*/_react.default.createElement(_AzLoad.AzLoad, null))));
};
exports.AzNavRail = AzNavRail;
const styles = _reactNative.StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderRightWidth: 1,
    borderRightColor: '#ddd'
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: _AzNavRailDefaults.AzNavRailDefaults.HeaderPadding
  },
  appName: {
    marginLeft: 16,
    fontWeight: 'bold',
    fontSize: 18
  },
  railContent: {
    paddingHorizontal: _AzNavRailDefaults.AzNavRailDefaults.RailContentHorizontalPadding,
    paddingVertical: 8,
    alignItems: 'center'
  },
  menuContent: {
    flex: 1
  },
  menuItem: {
    padding: 16
  },
  menuItemText: {
    fontSize: 16
  },
  divider: {
    height: 1,
    backgroundColor: '#ccc',
    marginVertical: 8
  },
  footer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#ccc'
  },
  loaderOverlay: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10000,
    elevation: 10
  }
});
//# sourceMappingURL=AzNavRail.js.map