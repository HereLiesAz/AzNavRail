function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useState, useCallback, useRef, useEffect } from 'react';
import { View, Text, TouchableOpacity, Animated, StyleSheet, PanResponder, Dimensions, ScrollView } from 'react-native';
import { AzNavRailContext } from './AzNavRailScope';
import { AzButtonShape } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';
import { AzButton } from './components/AzButton';
import { AzToggle } from './components/AzToggle';
import { AzCycler } from './components/AzCycler';
import { RailMenuItem } from './components/RailMenuItem';
import { AzLoad } from './components/AzLoad';
const logInteraction = (action, details) => {
  console.log(`[AzNavRail] ${action} ${details || ''}`);
};
export const AzNavRail = ({
  children,
  initiallyExpanded = false,
  displayAppNameInHeader = true,
  expandedRailWidth = AzNavRailDefaults.ExpandedRailWidth,
  collapsedRailWidth = AzNavRailDefaults.CollapsedRailWidth,
  showFooter = true,
  isLoading = false,
  defaultShape = AzButtonShape.CIRCLE,
  enableRailDragging = false,
  onExpandedChange
}) => {
  const [items, setItems] = useState([]);
  const [isExpanded, setIsExpanded] = useState(initiallyExpanded);
  const [isFloating, setIsFloating] = useState(false);
  const [showFloatingButtons, setShowFloatingButtons] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const [hostStates, setHostStates] = useState({});
  const railWidthAnim = useRef(new Animated.Value(initiallyExpanded ? expandedRailWidth : collapsedRailWidth)).current;
  const pan = useRef(new Animated.ValueXY()).current;
  const panValue = useRef({
    x: 0,
    y: 0
  });
  const dragStartPos = useRef({
    x: 0,
    y: 0
  });

  // Refs for PanResponder stale closures
  const isFloatingRef = useRef(isFloating);
  const enableRailDraggingRef = useRef(enableRailDragging);
  const showFloatingButtonsRef = useRef(showFloatingButtons);
  const wasVisibleOnDragStartRef = useRef(false);
  const itemsRef = useRef(items);
  useEffect(() => {
    isFloatingRef.current = isFloating;
  }, [isFloating]);
  useEffect(() => {
    enableRailDraggingRef.current = enableRailDragging;
  }, [enableRailDragging]);
  useEffect(() => {
    showFloatingButtonsRef.current = showFloatingButtons;
  }, [showFloatingButtons]);
  useEffect(() => {
    itemsRef.current = items;
  }, [items]);
  useEffect(() => {
    const id = pan.addListener(value => {
      panValue.current = value;
    });
    return () => pan.removeListener(id);
  }, [pan]);

  // --- Item Management ---
  const register = useCallback(item => {
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
  const unregister = useCallback(id => {
    setItems(prev => prev.filter(i => i.id !== id));
  }, []);

  // --- Animation & Effects ---
  useEffect(() => {
    Animated.timing(railWidthAnim, {
      toValue: isExpanded ? expandedRailWidth : collapsedRailWidth,
      duration: 300,
      useNativeDriver: false
    }).start();
    if (onExpandedChange) onExpandedChange(isExpanded);
  }, [isExpanded, expandedRailWidth, collapsedRailWidth]);

  // --- Gestures ---
  const panResponder = useRef(PanResponder.create({
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
      const screenHeight = Dimensions.get('window').height;
      const iconSize = AzNavRailDefaults.HeaderIconSize;

      // 10% margin bounds
      const minY = screenHeight * 0.1;
      const maxY = screenHeight * 0.9 - iconSize;

      // Calculate target absolute Y
      // dragStartPos is the absolute position at start of drag (because we set offset to it)
      // Wait, if we set offset, then pan.y (value) is deviation.
      // So AbsoluteY = OffsetY + ValueY.
      // Here ValueY should be dy.

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
        if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
          setIsFloating(false);
          pan.setValue({
            x: 0,
            y: 0
          });
          logInteraction('Snap back', 'Docked');
        } else {
          if (wasVisibleOnDragStartRef.current) {
            // Unfold logic
            setShowFloatingButtons(true);

            // Check bottom bound
            const screenHeight = Dimensions.get('window').height;
            const railItemsCount = itemsRef.current.filter(i => i.isRailItem && !i.isSubItem).length;
            // Estimated height: Header + (Count * (48 + 8)) + Padding
            // 48 = button size, 8 = margin
            const contentHeight = headerHeight + railItemsCount * 56 + 16;
            const bottomY = currentY + contentHeight;
            const limitY = screenHeight * 0.9;
            if (bottomY > limitY) {
              const shift = bottomY - limitY;
              const newY = currentY - shift;
              Animated.timing(pan, {
                toValue: {
                  x: currentX,
                  y: newY
                },
                duration: 200,
                useNativeDriver: false
              }).start();
              logInteraction('Adjusted position', `Shifted up by ${shift}`);
            }
          }
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
        // Check bottom bound (duplicated logic, could be refactored)
        const currentX = panValue.current.x;
        const currentY = panValue.current.y;
        const screenHeight = Dimensions.get('window').height;
        const railItemsCount = items.filter(i => i.isRailItem && !i.isSubItem).length;
        const contentHeight = headerHeight + railItemsCount * 56 + 16;
        const bottomY = currentY + contentHeight;
        const limitY = screenHeight * 0.9;
        if (bottomY > limitY) {
          const shift = bottomY - limitY;
          const newY = currentY - shift;
          Animated.timing(pan, {
            toValue: {
              x: currentX,
              y: newY
            },
            duration: 200,
            useNativeDriver: false
          }).start();
        }
      }
    } else {
      setIsExpanded(!isExpanded);
    }
  };

  // --- Render Helpers ---

  const renderRailItem = item => {
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = items.filter(i => i.hostId === item.id);
    const isRect = item.shape === AzButtonShape.RECTANGLE;
    const commonProps = {
      key: item.id,
      color: item.color,
      shape: item.shape || defaultShape,
      disabled: item.disabled,
      style: {
        marginBottom: isRect ? 2 : AzNavRailDefaults.RailContentVerticalArrangement
      }
    };
    if (item.isHost) {
      return /*#__PURE__*/React.createElement(View, {
        key: item.id,
        style: {
          alignItems: 'center',
          width: '100%'
        }
      }, /*#__PURE__*/React.createElement(AzButton, _extends({}, commonProps, {
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
      return /*#__PURE__*/React.createElement(AzCycler, _extends({}, commonProps, {
        options: item.options || [],
        selectedOption: item.selectedOption || '',
        onCycle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Cycler cycled', item.text);
        }
      }));
    }
    if (item.isToggle) {
      return /*#__PURE__*/React.createElement(AzToggle, _extends({}, commonProps, {
        isChecked: item.isChecked || false,
        toggleOnText: item.toggleOnText,
        toggleOffText: item.toggleOffText,
        onToggle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Toggle toggled', item.text);
        }
      }));
    }
    return /*#__PURE__*/React.createElement(AzButton, _extends({}, commonProps, {
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
    return /*#__PURE__*/React.createElement(RailMenuItem, {
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
      renderSubItems: () => /*#__PURE__*/React.createElement(React.Fragment, null, subItems.map(subItem => renderMenuItem(subItem, depth + 1)))
    });
  };
  const railItems = items.filter(i => i.isRailItem && !i.isSubItem);
  const menuItems = items.filter(i => !i.isSubItem);
  return /*#__PURE__*/React.createElement(AzNavRailContext.Provider, {
    value: {
      register,
      unregister
    }
  }, /*#__PURE__*/React.createElement(View, {
    style: {
      flexDirection: 'row',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(Animated.View, _extends({
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
  }, panResponder.panHandlers), /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleHeaderTap,
    onLongPress: handleHeaderLongPress,
    delayLongPress: 500,
    style: styles.header,
    onLayout: e => setHeaderHeight(e.nativeEvent.layout.height),
    accessibilityRole: "button",
    accessibilityLabel: isFloating ? "Undocked Rail" : isExpanded ? "Collapse Menu" : "Expand Menu",
    accessibilityHint: "Double tap to toggle, long press to drag"
  }, /*#__PURE__*/React.createElement(View, {
    style: {
      width: AzNavRailDefaults.HeaderIconSize,
      height: AzNavRailDefaults.HeaderIconSize,
      backgroundColor: 'gray',
      borderRadius: 24,
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(Text, {
    style: {
      color: 'white'
    }
  }, "Icon")), !isFloating && isExpanded && displayAppNameInHeader && /*#__PURE__*/React.createElement(Text, {
    style: styles.appName,
    numberOfLines: 1
  }, "App Name")), isExpanded ? /*#__PURE__*/React.createElement(ScrollView, {
    style: styles.menuContent
  }, menuItems.map(item => {
    if (item.isDivider) {
      return /*#__PURE__*/React.createElement(View, {
        key: item.id,
        style: styles.divider
      });
    }
    return renderMenuItem(item);
  }), showFooter && /*#__PURE__*/React.createElement(View, {
    style: styles.footer
  }, /*#__PURE__*/React.createElement(Text, null, "Footer"))) : /*#__PURE__*/React.createElement(ScrollView, {
    contentContainerStyle: styles.railContent
  }, isFloating && !showFloatingButtons ? null : railItems.map(renderRailItem))), children, isLoading && /*#__PURE__*/React.createElement(View, {
    style: styles.loaderOverlay
  }, /*#__PURE__*/React.createElement(AzLoad, null))));
};
const styles = StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderRightWidth: 1,
    borderRightColor: '#ddd'
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: AzNavRailDefaults.HeaderPadding
  },
  appName: {
    marginLeft: 16,
    fontWeight: 'bold',
    fontSize: 18
  },
  railContent: {
    paddingHorizontal: AzNavRailDefaults.RailContentHorizontalPadding,
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
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10000,
    elevation: 10
  }
});
//# sourceMappingURL=AzNavRail.js.map