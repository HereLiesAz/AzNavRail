"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzNavRail = void 0;
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
var _DraggableRailItemWrapper = require("./components/DraggableRailItemWrapper");
var _RelocItemHandler = require("./util/RelocItemHandler");
var _AzNestedRailPopup = require("./components/AzNestedRailPopup");
var _HelpOverlay = require("./components/HelpOverlay");
var _AzTutorialController = require("./tutorial/AzTutorialController");
var _AzTutorialOverlay = require("./components/AzTutorialOverlay");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const AzNavRailInner = props => {
  const {
    children,
    navController: _navController,
    currentDestination,
    initiallyExpanded = false,
    displayAppNameInHeader = true,
    expandedRailWidth = _AzNavRailDefaults.AzNavRailDefaults.ExpandedRailWidth,
    collapsedRailWidth = _AzNavRailDefaults.AzNavRailDefaults.CollapsedRailWidth,
    showFooter = true,
    isLoading = false,
    defaultShape = _types.AzButtonShape.CIRCLE,
    enableRailDragging = false,
    dockingSide = _types.AzDockingSide.LEFT,
    noMenu = false,
    infoScreen = false,
    onDismissInfoScreen,
    activeColor,
    headerIconShape = _types.AzHeaderIconShape.CIRCLE,
    translucentBackground,
    vibrate = false,
    onExpandedChange,
    onInteraction,
    helpList = {}
  } = props;
  const logInteraction = (0, _react.useCallback)((action, details) => {
    if (onInteraction) {
      onInteraction(action, details);
      return;
    }
    if (typeof __DEV__ !== 'undefined' && __DEV__) {
      const suffix = details ? ` ${details}` : '';
      // eslint-disable-next-line no-console
      console.log(`[AzNavRail] ${action}${suffix}`);
    }
  }, [onInteraction]);
  const [items, setItems] = (0, _react.useState)([]);
  const [dslOverrides, setDslOverrides] = (0, _react.useState)({});
  const config = {
    displayAppNameInHeader: dslOverrides.displayAppNameInHeader ?? displayAppNameInHeader,
    packRailButtons: dslOverrides.packRailButtons ?? props.packRailButtons,
    expandedRailWidth: dslOverrides.expandedRailWidth ?? expandedRailWidth,
    collapsedRailWidth: dslOverrides.collapsedRailWidth ?? collapsedRailWidth,
    showFooter: dslOverrides.showFooter ?? showFooter,
    isLoading: dslOverrides.isLoading ?? isLoading,
    defaultShape: dslOverrides.defaultShape ?? defaultShape,
    enableRailDragging: dslOverrides.enableRailDragging ?? enableRailDragging,
    dockingSide: dslOverrides.dockingSide ?? dockingSide,
    noMenu: dslOverrides.noMenu ?? noMenu,
    infoScreen: dslOverrides.infoScreen ?? infoScreen,
    activeColor: dslOverrides.activeColor ?? activeColor,
    headerIconShape: dslOverrides.headerIconShape ?? headerIconShape,
    translucentBackground: dslOverrides.translucentBackground ?? translucentBackground,
    vibrate: dslOverrides.vibrate ?? vibrate,
    onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
    helpList: dslOverrides.helpList ?? helpList
  };
  const [isExpanded, setIsExpanded] = (0, _react.useState)(initiallyExpanded && !config.noMenu);
  const [isFloating, setIsFloating] = (0, _react.useState)(false);
  const [showFloatingButtons, setShowFloatingButtons] = (0, _react.useState)(false);
  const [headerHeight, setHeaderHeight] = (0, _react.useState)(0);
  const [hostStates, setHostStates] = (0, _react.useState)({});
  const [nestedRailVisible, setNestedRailVisible] = (0, _react.useState)(null);
  const [anchorPosition, setAnchorPosition] = (0, _react.useState)(undefined);
  const [itemBounds, setItemBounds] = (0, _react.useState)({});
  const [secLocClicks, setSecLocClicks] = (0, _react.useState)(0);
  const [secLocVisible, setSecLocVisible] = (0, _react.useState)(false);
  const handleItemLayout = (0, _react.useCallback)((id, e) => {
    const target = e.target;
    if (target) {
      _reactNative.UIManager.measureInWindow(target, (x, y, width, height) => {
        if (x !== undefined && y !== undefined && width !== undefined && height !== undefined) {
          const bounds = {
            x,
            y,
            width,
            height
          };
          setItemBounds(prev => ({
            ...prev,
            [id]: bounds
          }));
          if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
        } else {
          const bounds = {
            ...e.nativeEvent.layout
          };
          setItemBounds(prev => ({
            ...prev,
            [id]: bounds
          }));
          if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
        }
      });
    } else {
      const bounds = {
        ...e.nativeEvent.layout
      };
      setItemBounds(prev => ({
        ...prev,
        [id]: bounds
      }));
      if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
    }
  }, [config]);
  const subItemsMap = (0, _react.useMemo)(() => {
    const map = {};
    items.forEach(item => {
      if (item.hostId) {
        if (!map[item.hostId]) {
          map[item.hostId] = [];
        }
        map[item.hostId].push(item);
      }
    });
    return map;
  }, [items]);
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
  const itemOffsets = (0, _react.useRef)({});
  const screenHeightRef = (0, _react.useRef)(_reactNative.Dimensions.get('window').height);
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
  const updateSettings = (0, _react.useCallback)(newSettings => {
    setDslOverrides(prev => ({
      ...prev,
      ...newSettings
    }));
  }, []);
  const dividerCounter = (0, _react.useRef)(0);
  const getDividerId = (0, _react.useCallback)(() => {
    const currentItems = itemsRef.current || [];
    return `divider_${currentItems.length + dividerCounter.current++}`;
  }, []);
  const hasItem = (0, _react.useCallback)(id => {
    return (itemsRef.current || []).some(i => i.id === id);
  }, []);
  const register = (0, _react.useCallback)(item => {
    setItems(prev => {
      const index = prev.findIndex(i => i.id === item.id);
      if (index >= 0) {
        const old = prev[index];
        const isSame = old.text === item.text && old.disabled === item.disabled && old.isChecked === item.isChecked && old.selectedOption === item.selectedOption && old.isExpanded === item.isExpanded && JSON.stringify(old.options) === JSON.stringify(item.options) && old.shape === item.shape && old.color === item.color && old.route === item.route && old.isRelocItem === item.isRelocItem && old.hiddenMenu === item.hiddenMenu && old.info === item.info;
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
    if (itemOffsets.current[id]) delete itemOffsets.current[id];
  }, []);
  (0, _react.useEffect)(() => {
    _reactNative.Animated.timing(railWidthAnim, {
      toValue: isExpanded ? config.expandedRailWidth : config.collapsedRailWidth,
      duration: 300,
      useNativeDriver: false
    }).start();
    if (onExpandedChange) onExpandedChange(isExpanded);
  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth, onExpandedChange]);
  (0, _react.useEffect)(() => {
    items.forEach(item => {
      if (!itemOffsets.current[item.id]) {
        itemOffsets.current[item.id] = new _reactNative.Animated.Value(0);
      }
    });
  }, [items]);

  // Reloc Item Logic
  const handleRelocDragStart = draggedItemIndex => {
    if (vibrate) _reactNative.Vibration.vibrate(50);
    logInteraction('Reloc drag started', items[draggedItemIndex].text);
  };
  const handleRelocDragEnd = _draggedItemIndex => {
    Object.values(itemOffsets.current).forEach(anim => anim.setValue(0));
    logInteraction('Reloc drag ended');
  };
  const handleRelocDragMove = (dy, draggedItemIndex) => {
    const draggedItem = items[draggedItemIndex];
    if (!draggedItem.isRelocItem || !draggedItem.hostId) return;
    const cluster = _RelocItemHandler.RelocItemHandler.getCluster(items, draggedItem.hostId);
    const itemHeight = 48 + 8;
    const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
    const targetClusterIndex = _RelocItemHandler.RelocItemHandler.calculateTargetIndex(dy, draggedClusterIndex, cluster.length, itemHeight);

    // Animation logic omitted for brevity, but same as before
    cluster.forEach((item, index) => {
      if (item.id === draggedItem.id) return;
      let offset = 0;
      if (draggedClusterIndex < targetClusterIndex) {
        if (index > draggedClusterIndex && index <= targetClusterIndex) offset = -itemHeight;
      } else if (draggedClusterIndex > targetClusterIndex) {
        if (index >= targetClusterIndex && index < draggedClusterIndex) offset = itemHeight;
      }
      _reactNative.Animated.spring(itemOffsets.current[item.id], {
        toValue: offset,
        useNativeDriver: false
      }).start();
    });
    panValue.targetIndex = targetClusterIndex;
  };
  const handleRelocDrop = draggedItemIndex => {
    const draggedItem = items[draggedItemIndex];
    const cluster = _RelocItemHandler.RelocItemHandler.getCluster(items, draggedItem.hostId);
    const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
    const targetClusterIndex = panValue.targetIndex ?? draggedClusterIndex;
    if (draggedClusterIndex !== targetClusterIndex) {
      const newItems = _RelocItemHandler.RelocItemHandler.reorderItems(items, draggedItem.id, draggedItem.hostId, targetClusterIndex);
      setItems(newItems);
      const newCluster = _RelocItemHandler.RelocItemHandler.getCluster(newItems, draggedItem.hostId);
      const newOrder = newCluster.map(i => i.id);
      if (draggedItem.onRelocate) {
        draggedItem.onRelocate(draggedClusterIndex, targetClusterIndex, newOrder);
      }
    }
    delete panValue.targetIndex;
    handleRelocDragEnd(draggedItemIndex);
  };
  const adjustFloatingRailWithinBounds = (currentX, currentY) => {
    const screenHeight = screenHeightRef.current;
    const railItemsCountForDrag = itemsRef.current.filter(i => i.isRailItem && !i.isSubItem).length;
    const contentHeight = headerHeight + railItemsCountForDrag * 56 + 16;
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
    }
  };
  const panResponder = (0, _react.useRef)(_reactNative.PanResponder.create({
    onMoveShouldSetPanResponder: (_, gestureState) => {
      if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;
      const {
        dx,
        dy
      } = gestureState;
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
        if (vibrate) _reactNative.Vibration.vibrate(50);
        setIsFloating(true);
        setIsExpanded(false);
        logInteraction('Swipe detected', 'Entering FAB mode');
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
      const minY = screenHeight * 0.1;
      const maxY = screenHeight * 0.9 - iconSize;
      const targetY = dragStartPos.current.y + dy;
      let clampedY = Math.max(minY, Math.min(targetY, maxY));
      pan.setValue({
        x: dx,
        y: clampedY - dragStartPos.current.y
      });
    },
    onPanResponderRelease: () => {
      pan.flattenOffset();
      if (isFloatingRef.current) {
        logInteraction('Drag ended');
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
          setShowFloatingButtons(true);
          adjustFloatingRailWithinBounds(currentX, currentY);
        }
      }
    }
  })).current;
  const handleHeaderLongPress = () => {
    if (config.enableRailDragging) {
      if (vibrate) _reactNative.Vibration.vibrate(50);
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
      const willShow = !showFloatingButtons;
      setShowFloatingButtons(willShow);
      if (willShow) {
        const currentX = panValue.current.x;
        const currentY = panValue.current.y;
        adjustFloatingRailWithinBounds(currentX, currentY);
      }
    } else {
      if (config.noMenu) return; // Prevent expansion if menu is disabled
      setIsExpanded(!isExpanded);
    }
  };
  const renderRailItem = (item, _index, overrideConfig = config) => {
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = subItemsMap[item.id] || [];
    const isRect = item.shape === _types.AzButtonShape.RECTANGLE;
    const commonProps = {
      key: item.id,
      color: overrideConfig.activeColor && (item.isChecked || item.id === currentDestination) ? overrideConfig.activeColor : item.color,
      shape: item.shape || overrideConfig.defaultShape,
      enabled: !item.disabled,
      style: {
        marginBottom: isRect ? 2 : _AzNavRailDefaults.AzNavRailDefaults.RailContentVerticalArrangement
      }
    };
    if (item.isRelocItem) {
      return /*#__PURE__*/_react.default.createElement(_DraggableRailItemWrapper.DraggableRailItemWrapper, {
        key: item.id,
        item: item,
        index: items.indexOf(item),
        totalItems: items.length,
        onDragStart: handleRelocDragStart,
        onDragEnd: handleRelocDrop,
        onDragMove: handleRelocDragMove,
        offsetY: itemOffsets.current[item.id] || new _reactNative.Animated.Value(0),
        style: commonProps.style,
        translucentBackground: config.translucentBackground
      });
    }
    if (item.isHost) {
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: item.id,
        style: {
          alignItems: 'center',
          width: '100%'
        }
      }, /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, _extends({}, commonProps, {
        fillColor: item.fillColor,
        text: item.text,
        content: item.content,
        hasCustomContent: !!item.content,
        onClick: () => {
          setHostStates(prev => ({
            ...prev,
            [item.id]: !prev[item.id]
          }));
          logInteraction('Host toggled', item.text);
        }
      })), isExpandedHost && subItems.map((sub, _i) => renderRailItem(sub, items.indexOf(sub))));
    }
    if (item.isCycler) {
      return /*#__PURE__*/_react.default.createElement(_AzCycler.AzCycler, _extends({}, commonProps, {
        fillColor: item.fillColor,
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
        fillColor: item.fillColor,
        isChecked: item.isChecked || false,
        toggleOnText: item.toggleOnText,
        toggleOffText: item.toggleOffText,
        onToggle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Toggle toggled', item.text);
        }
      }));
    }
    return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      key: item.id,
      onLayout: !isFloating || config.infoScreen || item.isNestedRail ? e => handleItemLayout(item.id, e) : undefined
    }, /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, _extends({}, commonProps, {
      fillColor: item.fillColor,
      text: item.text,
      content: item.content,
      hasCustomContent: !!item.content,
      onClick: () => {
        logInteraction('Item clicked', item.text);
        if (item.isNestedRail) {
          setNestedRailVisible(item.id);
          // Use actual bounds if available
          const bounds = itemBounds[item.id];
          if (bounds) {
            setAnchorPosition(bounds);
          } else {
            setAnchorPosition({
              x: 0,
              y: _index * 60 + headerHeight,
              width: config.collapsedRailWidth,
              height: 48
            });
          }
        }
        if (item.onClick) item.onClick();
        if (item.collapseOnClick && !config.noMenu) setIsExpanded(false);
      }
    })));
  };
  const renderMenuItem = (item, depth = 0) => {
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = subItemsMap[item.id] || [];
    return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      key: item.id,
      onLayout: config.infoScreen || item.isHost ? e => handleItemLayout(item.id, e) : undefined
    }, /*#__PURE__*/_react.default.createElement(_RailMenuItem.RailMenuItem, {
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
    }));
  };
  const renderFooter = () => {
    const footerColor = activeColor || '#6200ee';
    const handleUndock = () => {
      if (enableRailDragging) {
        setIsFloating(true);
        setIsExpanded(false);
        logInteraction('Footer undock clicked');
      }
    };
    const handleAbout = () => {
      _reactNative.Linking.openURL('https://github.com/HereLiesAz/AzNavRail').catch(e => console.error("Could not open About", e));
    };
    const handleFeedback = () => {
      _reactNative.Linking.openURL('mailto:hereliesaz@gmail.com?subject=Feedback for AzNavRail').catch(e => console.error("Could not open Mail", e));
    };
    const handleCredit = () => {
      _reactNative.Linking.openURL('https://instagram.com/HereLiesAz').catch(e => console.error("Could not open Credit", e));
    };
    const handleSecLocTrigger = () => {
      setSecLocClicks(prev => {
        const newClicks = prev + 1;
        if (newClicks >= 9) {
          setSecLocVisible(true);
          return 0;
        }
        return newClicks;
      });
    };
    return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      style: [styles.footer, {
        alignItems: 'center'
      }]
    }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      style: styles.divider
    }), enableRailDragging && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      onPress: handleUndock,
      style: {
        paddingVertical: 8
      }
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.menuItemText, {
        color: footerColor,
        fontWeight: 'bold'
      }]
    }, `Undock`)), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      onPress: handleAbout,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.menuItemText, {
        color: footerColor
      }]
    }, "About")), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      onPress: handleFeedback,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.menuItemText, {
        color: footerColor
      }]
    }, "Feedback")), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
      onPress: handleCredit,
      onLongPress: handleSecLocTrigger,
      delayLongPress: 500,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
      style: [styles.menuItemText, {
        color: footerColor,
        opacity: 0.5
      }]
    }, "@HereLiesAz")));
  };
  const effectiveRailItems = (0, _react.useMemo)(() => {
    const list = [];
    items.forEach(i => {
      if (!i.isSubItem && (config.noMenu || i.isRailItem)) {
        list.push(i);
        if (hostStates[i.id] && !i.isNestedRail) {
          const subItems = subItemsMap[i.id] || [];
          subItems.forEach(sub => {
            if (sub.isRailItem) list.push(sub);
          });
        }
      }
    });
    return list;
  }, [items, config.noMenu, hostStates, subItemsMap]);
  const menuItems = (0, _react.useMemo)(() => {
    return items.filter(i => !i.isSubItem);
  }, [items]);
  const getHeaderBorderRadius = () => {
    if (config.headerIconShape === _types.AzHeaderIconShape.SQUARE) return 0;
    if (config.headerIconShape === _types.AzHeaderIconShape.ROUNDED) return 8;
    return 24;
  };
  const flexDirection = config.dockingSide === _types.AzDockingSide.RIGHT ? 'row-reverse' : 'row';
  return /*#__PURE__*/_react.default.createElement(_AzNavRailScope.AzNavRailContext.Provider, {
    value: {
      register,
      unregister,
      updateSettings,
      getDividerId,
      hasItem
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      flexDirection: flexDirection,
      height: '100%',
      flex: 1
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
      height: isFloating ? 'auto' : '100%',
      borderRightWidth: dockingSide === _types.AzDockingSide.LEFT ? 1 : 0,
      borderLeftWidth: dockingSide === _types.AzDockingSide.RIGHT ? 1 : 0
    }]
  }, panResponder.panHandlers), /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
    onPress: handleHeaderTap,
    onLongPress: handleHeaderLongPress,
    delayLongPress: 500,
    style: [styles.header, {
      flexDirection: config.dockingSide === _types.AzDockingSide.RIGHT ? 'row-reverse' : 'row'
    }],
    onLayout: e => setHeaderHeight(e.nativeEvent.layout.height),
    accessibilityRole: "button",
    accessibilityLabel: isFloating ? "Undocked Rail" : isExpanded ? "Collapse Menu" : "Expand Menu",
    accessibilityHint: "Double tap to toggle, long press to drag"
  }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      width: _AzNavRailDefaults.AzNavRailDefaults.HeaderIconSize,
      height: _AzNavRailDefaults.AzNavRailDefaults.HeaderIconSize,
      backgroundColor: 'gray',
      borderRadius: getHeaderBorderRadius(),
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: {
      color: 'white'
    }
  }, "Icon")), !isFloating && isExpanded && config.displayAppNameInHeader && /*#__PURE__*/_react.default.createElement(_reactNative.Text, {
    style: [styles.appName, {
      marginLeft: config.dockingSide === _types.AzDockingSide.RIGHT ? 0 : 16,
      marginRight: config.dockingSide === _types.AzDockingSide.RIGHT ? 16 : 0,
      flexShrink: 0,
      minWidth: 200
    }],
    numberOfLines: 1
  }, "App Name")), isExpanded && !config.noMenu ? /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    style: styles.menuContent
  }, menuItems.map(item => {
    if (item.isDivider) {
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: item.id,
        style: styles.divider
      });
    }
    return renderMenuItem(item);
  }), showFooter && renderFooter()) : /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    contentContainerStyle: styles.railContent
  }, isFloating && !showFloatingButtons ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index)))), items.filter(i => i.isNestedRail).map(item => {
    const effectiveConfig = item.nestedRailSettings ? {
      ...config,
      ...item.nestedRailSettings
    } : config;
    return /*#__PURE__*/_react.default.createElement(_AzNestedRailPopup.AzNestedRailPopup, {
      key: `nested-${item.id}`,
      visible: nestedRailVisible === item.id,
      onDismiss: () => setNestedRailVisible(null),
      items: subItemsMap[item.id] || [],
      alignment: item.nestedRailAlignment || _types.AzNestedRailAlignment.VERTICAL,
      renderItem: (subItem, idx) => {
        return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
          key: `wrap-${subItem.id}`,
          onLayout: e => handleItemLayout(subItem.id, e)
        }, renderRailItem(subItem, idx, effectiveConfig));
      },
      anchorPosition: anchorPosition,
      dockingSide: effectiveConfig.dockingSide,
      helpList: effectiveConfig.helpList
    });
  }), /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: {
      flex: 1,
      marginTop: '20%',
      marginBottom: '10%'
    }
  }, children, secLocVisible && /*#__PURE__*/_react.default.createElement(_reactNative.View, null), " ", secLocClicks > 0 && /*#__PURE__*/_react.default.createElement(_reactNative.View, null), " "), isLoading && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: styles.loaderOverlay
  }, /*#__PURE__*/_react.default.createElement(_AzLoad.AzLoad, null)), infoScreen && /*#__PURE__*/_react.default.createElement(_HelpOverlay.HelpOverlay, {
    items: items,
    helpList: config.helpList || {},
    onDismiss: onDismissInfoScreen,
    itemBounds: itemBounds,
    nestedRailVisibleId: nestedRailVisible,
    tutorials: config.tutorials
  }), /*#__PURE__*/_react.default.createElement(TutorialOverlayWrapper, {
    tutorials: config.tutorials,
    itemBounds: itemBounds
  })));
};
const TutorialOverlayWrapper = ({
  tutorials,
  itemBounds
}) => {
  const tutorialController = (0, _AzTutorialController.useAzTutorialController)();
  const activeId = tutorialController.activeTutorialId;
  if (!activeId || !tutorials || !tutorials[activeId]) {
    return null;
  }
  return /*#__PURE__*/_react.default.createElement(_AzTutorialOverlay.AzTutorialOverlay, {
    tutorialId: activeId,
    tutorial: tutorials[activeId],
    onDismiss: () => tutorialController.endTutorial(),
    itemBoundsCache: itemBounds
  });
};
const AzNavRail = props => {
  return /*#__PURE__*/_react.default.createElement(_AzTutorialController.AzTutorialProvider, null, /*#__PURE__*/_react.default.createElement(AzNavRailInner, props));
};
exports.AzNavRail = AzNavRail;
const styles = _reactNative.StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderColor: '#ddd'
  },
  header: {
    alignItems: 'center',
    padding: _AzNavRailDefaults.AzNavRailDefaults.HeaderPadding
  },
  appName: {
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
  },
  infoOverlay: {
    ..._reactNative.StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.85)',
    zIndex: 20000,
    paddingTop: 40
  },
  infoTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 20,
    textAlign: 'center'
  },
  infoItem: {
    marginBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#555',
    paddingBottom: 8
  },
  infoItemTitle: {
    fontWeight: 'bold',
    color: '#4fc3f7',
    fontSize: 16,
    marginBottom: 4
  },
  infoItemText: {
    color: '#eee',
    fontSize: 14
  },
  closeInfoButton: {
    position: 'absolute',
    top: 40,
    right: 20,
    backgroundColor: '#333',
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#666'
  }
});
//# sourceMappingURL=AzNavRail.js.map