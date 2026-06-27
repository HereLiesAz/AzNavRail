"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AzNavRail = void 0;
var _react = _interopRequireWildcard(require("react"));
var _reactNative = require("react-native");
var _AzNavRailScope = require("./AzNavRailScope");
var _types = require("./types");
var _AzKinetics = require("./components/AzKinetics");
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
var _AboutOverlay = require("./components/AboutOverlay");
var _MoreFromAzOverlay = require("./components/MoreFromAzOverlay");
var _AzGuidanceController = require("./guidance/AzGuidanceController");
var _AzStatusEngine = require("./guidance/AzStatusEngine");
var _AzGuidance = require("./guidance/AzGuidance");
var _AzInstructionOverlay = require("./components/AzInstructionOverlay");
function _interopRequireWildcard(e, t) { if ("function" == typeof WeakMap) var r = new WeakMap(), n = new WeakMap(); return (_interopRequireWildcard = function (e, t) { if (!t && e && e.__esModule) return e; var o, i, f = { __proto__: null, default: e }; if (null === e || "object" != typeof e && "function" != typeof e) return f; if (o = t ? n : r) { if (o.has(e)) return o.get(e); o.set(e, f); } for (const t in e) "default" !== t && {}.hasOwnProperty.call(e, t) && ((i = (o = Object.defineProperty) && Object.getOwnPropertyDescriptor(e, t)) && (i.get || i.set) ? o(f, t, i) : f[t] = e[t]); return f; })(e, t); }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Props for the `AzNavRail` component, extending all `AzNavRailSettings` options. */

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
    headerIconSize,
    infoScreen = false,
    onDismissInfoScreen,
    activeColor,
    headerIconShape = _types.AzHeaderIconShape.CIRCLE,
    translucentBackground,
    vibrate = false,
    onExpandedChange,
    onInteraction,
    helpList = {},
    appRepositoryUrl,
    inAppAbout = true,
    moreFromAzEnabled = true,
    moreFromAzJsonUrl = 'https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json',
    moreRailItem = false
  } = props;
  const logInteraction = (0, _react.useCallback)((action, details, item) => {
    if (onInteraction) {
      onInteraction(action, details, item);
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
    headerIconSize: dslOverrides.headerIconSize ?? headerIconSize,
    infoScreen: dslOverrides.infoScreen ?? infoScreen,
    activeColor: dslOverrides.activeColor ?? activeColor,
    headerIconShape: dslOverrides.headerIconShape ?? headerIconShape,
    translucentBackground: dslOverrides.translucentBackground ?? translucentBackground,
    vibrate: dslOverrides.vibrate ?? vibrate,
    onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
    helpList: dslOverrides.helpList ?? helpList,
    appRepositoryUrl: dslOverrides.appRepositoryUrl ?? appRepositoryUrl,
    inAppAbout: dslOverrides.inAppAbout ?? inAppAbout,
    moreFromAzEnabled: dslOverrides.moreFromAzEnabled ?? moreFromAzEnabled,
    moreFromAzJsonUrl: dslOverrides.moreFromAzJsonUrl ?? moreFromAzJsonUrl,
    moreRailItem: dslOverrides.moreRailItem ?? moreRailItem
  };
  const [isExpanded, setIsExpanded] = (0, _react.useState)(initiallyExpanded && !config.noMenu);
  const [showAbout, setShowAbout] = (0, _react.useState)(false);
  const [showMoreFromAz, setShowMoreFromAz] = (0, _react.useState)(false);
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
  // Edge-tracking for host auto-expansion (expandWhen / per-host initiallyExpanded).
  const expandWhenSeenRef = (0, _react.useRef)({});
  const initiallyExpandedSeenRef = (0, _react.useRef)({});
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

  // Harden against a host auto-expanding (or any reorder/add/remove) mid-shuffle: zero every
  // sibling-displacement offset so a stale one can't leave an item rendered on top of its neighbour.
  // Mirrors the Android RailItems structural-change guard. Keyed on item ids + expanded host set,
  // since expansion changes the visible layout without changing the items array.
  const expandedHostKey = Object.keys(hostStates).filter(k => hostStates[k]).sort().join(',');
  const structuralKey = items.map(i => i.id).join(',') + '|' + expandedHostKey;
  (0, _react.useEffect)(() => {
    Object.values(itemOffsets.current).forEach(anim => anim.setValue(0));
  }, [structuralKey]);

  // Host auto-expansion — parity with Android's `expandWhen` / per-host `initiallyExpanded`. A host
  // expands on the rising edge of its `expandWhen` condition and collapses on the falling edge; a
  // manual collapse while the condition stays true is preserved (we act only on a real transition).
  // The first observation only auto-expands (never clobbering an initiallyExpanded/manual state).
  //
  // Evaluated after every render (so conditions backed by React state/props are picked up at once)
  // AND on a low-rate interval, so conditions backed by NON-React sources (a mutable ref, an external
  // store that doesn't trigger a re-render) still drive expansion — within the poll interval.
  const applyAutoExpansions = (0, _react.useCallback)(() => {
    const updates = {};
    let changed = false;
    itemsRef.current.forEach(item => {
      if (!item.isHost) return;
      // Per-host initiallyExpanded: rising-edge auto-expand, once.
      if (item.initiallyExpanded && initiallyExpandedSeenRef.current[item.id] !== true) {
        updates[item.id] = true;
        changed = true;
      }
      initiallyExpandedSeenRef.current[item.id] = !!item.initiallyExpanded;
      // expandWhen: transition-based.
      if (typeof item.expandWhen === 'function') {
        const now = !!item.expandWhen();
        const before = expandWhenSeenRef.current[item.id];
        if (before === undefined) {
          if (now) {
            updates[item.id] = true;
            changed = true;
          }
        } else if (before !== now) {
          updates[item.id] = now;
          changed = true;
        }
        expandWhenSeenRef.current[item.id] = now;
      }
    });
    if (!changed) return;
    setHostStates(prev => {
      let differs = false;
      for (const k in updates) if (prev[k] !== updates[k]) {
        differs = true;
        break;
      }
      return differs ? {
        ...prev,
        ...updates
      } : prev;
    });
  }, []);

  // After every render: React-state/prop-driven conditions are caught here immediately.
  (0, _react.useEffect)(() => {
    applyAutoExpansions();
  });
  // Low-rate poll: the safety net for conditions that aren't React state.
  (0, _react.useEffect)(() => {
    const t = setInterval(applyAutoExpansions, 300);
    return () => clearInterval(t);
  }, [applyAutoExpansions]);

  // --- Status-driven guidance (the reactive replacement for the scripted tutorial) ---
  // Observe which statuses are true, route from the live state toward each active goal, and render the
  // next-hop instruction as a callout adjacent to its target. Auto-advances when a target becomes true.
  const guidance = (0, _AzGuidanceController.useAzGuidanceContext)();
  const guidanceActiveItemId = (0, _react.useMemo)(() => {
    var _items$find;
    return ((_items$find = items.find(it => it.route && it.route === currentDestination)) === null || _items$find === void 0 ? void 0 : _items$find.id) ?? null;
  }, [items, currentDestination]);
  const guidanceClassifiers = props.activeClassifiers;
  const guidanceBuiltins = (0, _react.useCallback)(() => (0, _AzStatusEngine.computeBuiltinStatuses)({
    railExpanded: isExpanded,
    railFloating: isFloating,
    hostStates,
    currentRoute: currentDestination,
    activeItemId: guidanceActiveItemId,
    nestedRailOpenId: nestedRailVisible,
    helpOpen: infoScreen
  }), [isExpanded, isFloating, hostStates, currentDestination, guidanceActiveItemId, nestedRailVisible, infoScreen]);
  const activeStatuses = (0, _AzStatusEngine.useActiveStatuses)(guidance.statusPredicates, guidanceClassifiers, guidanceBuiltins);
  const guidanceEdges = (0, _react.useMemo)(() => [...guidance.edges, ...(0, _AzGuidance.computeAutoEdges)(items)], [guidance.edges, items]);
  const guidanceFrame = (0, _react.useMemo)(() => (0, _AzGuidance.routeInstructions)(guidanceEdges, guidance.goals, guidance.activeGoals, activeStatuses), [guidanceEdges, guidance.goals, guidance.activeGoals, activeStatuses]);
  // Self-arming onboarding goals: activate once their trigger status holds (unless already completed).
  (0, _react.useEffect)(() => {
    Object.values(guidance.goals).forEach(goal => {
      if (goal.autoStartWhen && activeStatuses.has(goal.autoStartWhen) && !guidance.isCompleted(goal.id) && !guidance.activeGoals.includes(goal.id)) {
        guidance.activate(goal.id);
      }
    });
  }, [activeStatuses, guidance]);
  // Auto-advance: a goal whose target is now true is reached → deactivate it and persist.
  (0, _react.useEffect)(() => {
    guidanceFrame.reachedGoals.forEach(g => guidance.markReached(g));
  }, [guidanceFrame, guidance]);
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
  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth]);
  (0, _react.useEffect)(() => {
    onExpandedChange === null || onExpandedChange === void 0 || onExpandedChange(isExpanded);
  }, [isExpanded, onExpandedChange]);
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
    logInteraction('Reloc drag started', items[draggedItemIndex].text, items[draggedItemIndex]);
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
  const renderRailItem = (item, _index, overrideConfig = config, ancestors = new Set()) => {
    // Cycle guard: unlike the Kotlin DSL (which requires a host be declared before its sub-items,
    // forming a DAG), the React DSL has no ordering guarantee, so a cyclic `hostId` chain
    // (A -> B -> A, or a self-reference) is possible. Bail before recursing into an ancestor to
    // avoid "Maximum call stack size exceeded".
    if (ancestors.has(item.id)) return null;
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
          var _item$onExpandedChang;
          const newHostState = !(hostStates[item.id] || false);
          setHostStates(prev => ({
            ...prev,
            [item.id]: newHostState
          }));
          logInteraction('Host toggled', item.text, item);
          (_item$onExpandedChang = item.onExpandedChange) === null || _item$onExpandedChang === void 0 || _item$onExpandedChang.call(item, newHostState);
        }
      })), isExpandedHost && subItems.filter(sub => sub.isRailItem).map(sub => renderRailItem(sub, items.indexOf(sub), overrideConfig, new Set(ancestors).add(item.id))));
    }
    if (item.isCycler) {
      return /*#__PURE__*/_react.default.createElement(_AzCycler.AzCycler, _extends({}, commonProps, {
        fillColor: item.fillColor,
        options: item.options || [],
        selectedOption: item.selectedOption || '',
        onCycle: () => {
          if (item.onClick) item.onClick();
          logInteraction('Cycler cycled', item.text, item);
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
          logInteraction('Toggle toggled', item.text, item);
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
        logInteraction('Item clicked', item.text, item);
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
  const renderMenuItem = (item, index = 0, count = 1, visible = true, depth = 0, ancestors = new Set()) => {
    // Cycle guard (see renderRailItem): stop before recursing into an ancestor so a cyclic or
    // self-referential `hostId` chain can't overflow the stack.
    if (ancestors.has(item.id)) return null;
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = subItemsMap[item.id] || [];
    return /*#__PURE__*/_react.default.createElement(_AzKinetics.AzKineticItem, {
      key: item.id,
      index: index,
      count: count,
      visible: visible,
      entrance: kItemEntrance,
      exit: kItemExit,
      staggerMs: kStaggerMs,
      durationMs: kDurationMs,
      startAngle: kStartAngle
      // Suppress the press-tilt on draggable items so it never fights the drag gesture.
      ,
      tiltOnPress: kTiltOnPress && !item.isRelocItem,
      maxTiltDegrees: kMaxTilt,
      dockingSide: kDockingSide,
      floating: isFloating
    }, /*#__PURE__*/_react.default.createElement(_reactNative.View, {
      onLayout: config.infoScreen || item.isHost ? e => handleItemLayout(item.id, e) : undefined
    }, /*#__PURE__*/_react.default.createElement(_RailMenuItem.RailMenuItem, {
      item: item,
      depth: depth,
      isExpandedHost: isExpandedHost,
      textStyle: kItemTextStyle,
      onToggleHost: () => {
        var _item$onExpandedChang2;
        const newHostState = !(hostStates[item.id] || false);
        setHostStates(prev => ({
          ...prev,
          [item.id]: newHostState
        }));
        (_item$onExpandedChang2 = item.onExpandedChange) === null || _item$onExpandedChang2 === void 0 || _item$onExpandedChang2.call(item, newHostState);
      },
      onItemClick: () => {
        logInteraction('Menu item clicked', item.text, item);
        if (item.onClick) item.onClick();
        if (item.collapseOnClick) setIsExpanded(false);
      },
      renderSubItems: () => /*#__PURE__*/_react.default.createElement(_react.default.Fragment, null, subItems.map((subItem, i) => renderMenuItem(subItem, i, subItems.length, visible, depth + 1, new Set(ancestors).add(item.id))))
    })));
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
      if (config.inAppAbout) {
        setIsExpanded(false);
        setShowAbout(true);
      } else if (config.appRepositoryUrl) {
        // Only follow safe web URLs — on react-native-web a `javascript:` URL would otherwise execute.
        const isSafe = config.appRepositoryUrl.startsWith('http://') || config.appRepositoryUrl.startsWith('https://');
        if (isSafe) {
          _reactNative.Linking.openURL(config.appRepositoryUrl).catch(e => console.error("Could not open About", e));
        }
      }
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
    }, `Undock`)), !!config.appRepositoryUrl && /*#__PURE__*/_react.default.createElement(_reactNative.TouchableOpacity, {
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
    // Only the top-level items are listed here; `renderRailItem`'s host branch renders each
    // expanded host's sub-items inline and recurses for sub-hosts, so hosts nest to any depth
    // without this list having to be flattened (which would otherwise double-render sub-items).
    return items.filter(i => !i.isSubItem && (config.noMenu || i.isRailItem));
  }, [items, config.noMenu]);
  const menuItems = (0, _react.useMemo)(() => {
    return items.filter(i => !i.isSubItem);
  }, [items]);

  // — Kinetic typography (WP7). Defaults animate; opt out via settings (itemEntrance: None, …). —
  const kItemEntrance = config.itemEntrance ?? _types.AzEntrance.Turnstile;
  const kItemExit = config.itemExit ?? _types.AzExit.Turnstile;
  const kStaggerMs = config.entranceStaggerMs ?? 55;
  const kDurationMs = config.entranceDurationMs ?? 360;
  const kStartAngle = config.entranceStartAngle ?? 70;
  const kTiltOnPress = config.tiltOnPress ?? false;
  const kMaxTilt = config.maxTiltDegrees ?? 10;
  const kItemTextStyle = config.itemTextStyle;
  const kDockingSide = config.dockingSide ?? _types.AzDockingSide.LEFT;
  // Keep the menu mounted through the staggered exit so items can turnstile out as the rail collapses.
  const menuRendered = (0, _AzKinetics.useAzClosing)(isExpanded && !config.noMenu, kItemExit, menuItems.length, kStaggerMs, kDurationMs);
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
  }, "App Name")), menuRendered && !noMenu ? /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    style: styles.menuContent
  }, menuItems.map((item, index) => {
    if (item.isDivider) {
      return /*#__PURE__*/_react.default.createElement(_reactNative.View, {
        key: item.id,
        style: styles.divider
      });
    }
    return renderMenuItem(item, index, menuItems.length, isExpanded);
  }), showFooter && renderFooter()) : /*#__PURE__*/_react.default.createElement(_reactNative.ScrollView, {
    contentContainerStyle: styles.railContent
  }, isFloating && !showFloatingButtons ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index)), !isFloating && config.moreRailItem && config.moreFromAzEnabled && /*#__PURE__*/_react.default.createElement(_AzButton.AzButton, {
    text: "More",
    color: config.activeColor || '#6200ee',
    shape: config.defaultShape,
    onClick: () => setShowMoreFromAz(true)
  }))), items.filter(i => i.isNestedRail).map(item => {
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
  }, /*#__PURE__*/_react.default.createElement(_AzLoad.AzLoad, null)), infoScreen && !showAbout && !showMoreFromAz && /*#__PURE__*/_react.default.createElement(_reactNative.View, {
    style: _reactNative.StyleSheet.absoluteFill
  }, /*#__PURE__*/_react.default.createElement(_HelpOverlay.HelpOverlay, {
    items: items,
    helpList: config.helpList || {},
    onDismiss: onDismissInfoScreen,
    itemBounds: itemBounds,
    nestedRailVisibleId: nestedRailVisible
  })), showAbout && !showMoreFromAz && !!config.appRepositoryUrl && /*#__PURE__*/_react.default.createElement(_AboutOverlay.AboutOverlay, {
    repoUrl: config.appRepositoryUrl,
    settings: {
      activeColor: config.activeColor,
      translucentBackground: config.translucentBackground
    },
    moreFromAzEnabled: config.moreFromAzEnabled,
    moreFromAzJsonUrl: config.moreFromAzJsonUrl,
    onDismiss: () => setShowAbout(false)
  }), showMoreFromAz && /*#__PURE__*/_react.default.createElement(_MoreFromAzOverlay.MoreFromAzOverlay, {
    jsonUrl: config.moreFromAzJsonUrl,
    settings: {
      activeColor: config.activeColor,
      translucentBackground: config.translucentBackground
    },
    onDismiss: () => setShowMoreFromAz(false)
  }), !showAbout && !showMoreFromAz && guidance.enabled && /*#__PURE__*/_react.default.createElement(_AzInstructionOverlay.AzInstructionOverlay, {
    instructions: guidanceFrame.instructions,
    itemBounds: itemBounds,
    accent: typeof config.activeColor === 'string' ? config.activeColor : undefined
  })));
};

/**
 * Main navigation rail component for React Native.
 * Renders a collapsible side rail with icon buttons, an expandable menu, and optional guidance/help overlay support.
 * Declare items as JSX children using the `AzNavRailScope` DSL helpers.
 *
 * @example
 * ```tsx
 * <AzNavRail
 *   dockingSide={AzDockingSide.LEFT}
 *   activeColor="#6200ee"
 *   enableRailDragging
 * >
 *   <AzRailItem id="home" text="Home" onClick={() => nav.push('/home')} />
 *   <AzRailToggle
 *     id="dark"
 *     text="Theme"
 *     isChecked={dark}
 *     toggleOnText="Dark"
 *     toggleOffText="Light"
 *     onClick={() => setDark(v => !v)}
 *   />
 *   <AzRailDivider />
 *   <AzMenuItem id="about" text="About" onClick={openAbout} />
 *   <MyScreen />
 * </AzNavRail>
 * ```
 */
const AzNavRail = props => {
  return /*#__PURE__*/_react.default.createElement(_AzGuidanceController.AzGuidanceProvider, null, /*#__PURE__*/_react.default.createElement(AzNavRailInner, props));
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