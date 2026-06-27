function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React, { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { View, Text, TouchableOpacity, Animated, StyleSheet, PanResponder, Dimensions, ScrollView, Linking, Vibration, UIManager } from 'react-native';
import { AzNavRailContext } from './AzNavRailScope';
import { AzButtonShape, AzDockingSide, AzHeaderIconShape, AzNestedRailAlignment, AzEntrance, AzExit } from './types';
import { AzKineticItem, useAzClosing } from './components/AzKinetics';
import { AzNavRailDefaults } from './AzNavRailDefaults';
import { AzButton } from './components/AzButton';
import { AzToggle } from './components/AzToggle';
import { AzCycler } from './components/AzCycler';
import { RailMenuItem } from './components/RailMenuItem';
import { AzLoad } from './components/AzLoad';
import { DraggableRailItemWrapper } from './components/DraggableRailItemWrapper';
import { RelocItemHandler } from './util/RelocItemHandler';
import { AzNestedRailPopup } from './components/AzNestedRailPopup';
import { HelpOverlay } from './components/HelpOverlay';
import { AboutOverlay } from './components/AboutOverlay';
import { MoreFromAzOverlay } from './components/MoreFromAzOverlay';
import { AzGuidanceProvider, useAzGuidanceContext } from './guidance/AzGuidanceController';
import { useActiveStatuses, computeBuiltinStatuses } from './guidance/AzStatusEngine';
import { routeInstructions, computeAutoEdges } from './guidance/AzGuidance';
import { AzInstructionOverlay } from './components/AzInstructionOverlay';

/** Props for the `AzNavRail` component, extending all `AzNavRailSettings` options. */

const AzNavRailInner = props => {
  const {
    children,
    navController: _navController,
    currentDestination,
    initiallyExpanded = false,
    displayAppNameInHeader = true,
    expandedRailWidth = AzNavRailDefaults.ExpandedRailWidth,
    collapsedRailWidth = AzNavRailDefaults.CollapsedRailWidth,
    showFooter = true,
    isLoading = false,
    defaultShape = AzButtonShape.CIRCLE,
    enableRailDragging = false,
    dockingSide = AzDockingSide.LEFT,
    noMenu = false,
    headerIconSize,
    infoScreen = false,
    onDismissInfoScreen,
    activeColor,
    headerIconShape = AzHeaderIconShape.CIRCLE,
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
  const logInteraction = useCallback((action, details, item) => {
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
  const [items, setItems] = useState([]);
  const [dslOverrides, setDslOverrides] = useState({});
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
  const [isExpanded, setIsExpanded] = useState(initiallyExpanded && !config.noMenu);
  const [showAbout, setShowAbout] = useState(false);
  const [showMoreFromAz, setShowMoreFromAz] = useState(false);
  const [isFloating, setIsFloating] = useState(false);
  const [showFloatingButtons, setShowFloatingButtons] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const [hostStates, setHostStates] = useState({});
  const [nestedRailVisible, setNestedRailVisible] = useState(null);
  const [anchorPosition, setAnchorPosition] = useState(undefined);
  const [itemBounds, setItemBounds] = useState({});
  const [secLocClicks, setSecLocClicks] = useState(0);
  const [secLocVisible, setSecLocVisible] = useState(false);
  const handleItemLayout = useCallback((id, e) => {
    const target = e.target;
    if (target) {
      UIManager.measureInWindow(target, (x, y, width, height) => {
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
  const subItemsMap = useMemo(() => {
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
  const itemOffsets = useRef({});
  const screenHeightRef = useRef(Dimensions.get('window').height);
  const isFloatingRef = useRef(isFloating);
  const enableRailDraggingRef = useRef(enableRailDragging);
  const showFloatingButtonsRef = useRef(showFloatingButtons);
  const wasVisibleOnDragStartRef = useRef(false);
  const itemsRef = useRef(items);
  // Edge-tracking for host auto-expansion (expandWhen / per-host initiallyExpanded).
  const expandWhenSeenRef = useRef({});
  const initiallyExpandedSeenRef = useRef({});
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

  // Harden against a host auto-expanding (or any reorder/add/remove) mid-shuffle: zero every
  // sibling-displacement offset so a stale one can't leave an item rendered on top of its neighbour.
  // Mirrors the Android RailItems structural-change guard. Keyed on item ids + expanded host set,
  // since expansion changes the visible layout without changing the items array.
  const expandedHostKey = Object.keys(hostStates).filter(k => hostStates[k]).sort().join(',');
  const structuralKey = items.map(i => i.id).join(',') + '|' + expandedHostKey;
  useEffect(() => {
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
  const applyAutoExpansions = useCallback(() => {
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
  useEffect(() => {
    applyAutoExpansions();
  });
  // Low-rate poll: the safety net for conditions that aren't React state.
  useEffect(() => {
    const t = setInterval(applyAutoExpansions, 300);
    return () => clearInterval(t);
  }, [applyAutoExpansions]);

  // --- Status-driven guidance (the reactive replacement for the scripted tutorial) ---
  // Observe which statuses are true, route from the live state toward each active goal, and render the
  // next-hop instruction as a callout adjacent to its target. Auto-advances when a target becomes true.
  const guidance = useAzGuidanceContext();
  const guidanceActiveItemId = useMemo(() => {
    var _items$find;
    return ((_items$find = items.find(it => it.route && it.route === currentDestination)) === null || _items$find === void 0 ? void 0 : _items$find.id) ?? null;
  }, [items, currentDestination]);
  const guidanceClassifiers = props.activeClassifiers;
  const guidanceBuiltins = useCallback(() => computeBuiltinStatuses({
    railExpanded: isExpanded,
    railFloating: isFloating,
    hostStates,
    currentRoute: currentDestination,
    activeItemId: guidanceActiveItemId,
    nestedRailOpenId: nestedRailVisible,
    helpOpen: infoScreen
  }), [isExpanded, isFloating, hostStates, currentDestination, guidanceActiveItemId, nestedRailVisible, infoScreen]);
  const activeStatuses = useActiveStatuses(guidance.statusPredicates, guidanceClassifiers, guidanceBuiltins);
  const guidanceEdges = useMemo(() => [...guidance.edges, ...computeAutoEdges(items)], [guidance.edges, items]);
  const guidanceFrame = useMemo(() => routeInstructions(guidanceEdges, guidance.goals, guidance.activeGoals, activeStatuses), [guidanceEdges, guidance.goals, guidance.activeGoals, activeStatuses]);
  // Self-arming onboarding goals: activate once their trigger status holds (unless already completed).
  useEffect(() => {
    Object.values(guidance.goals).forEach(goal => {
      if (goal.autoStartWhen && activeStatuses.has(goal.autoStartWhen) && !guidance.isCompleted(goal.id) && !guidance.activeGoals.includes(goal.id)) {
        guidance.activate(goal.id);
      }
    });
  }, [activeStatuses, guidance]);
  // Auto-advance: a goal whose target is now true is reached → deactivate it and persist.
  useEffect(() => {
    guidanceFrame.reachedGoals.forEach(g => guidance.markReached(g));
  }, [guidanceFrame, guidance]);
  useEffect(() => {
    const id = pan.addListener(value => {
      panValue.current = value;
    });
    return () => pan.removeListener(id);
  }, [pan]);
  useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({
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
  const updateSettings = useCallback(newSettings => {
    setDslOverrides(prev => ({
      ...prev,
      ...newSettings
    }));
  }, []);
  const dividerCounter = useRef(0);
  const getDividerId = useCallback(() => {
    const currentItems = itemsRef.current || [];
    return `divider_${currentItems.length + dividerCounter.current++}`;
  }, []);
  const hasItem = useCallback(id => {
    return (itemsRef.current || []).some(i => i.id === id);
  }, []);
  const register = useCallback(item => {
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
  const unregister = useCallback(id => {
    setItems(prev => prev.filter(i => i.id !== id));
    if (itemOffsets.current[id]) delete itemOffsets.current[id];
  }, []);
  useEffect(() => {
    Animated.timing(railWidthAnim, {
      toValue: isExpanded ? config.expandedRailWidth : config.collapsedRailWidth,
      duration: 300,
      useNativeDriver: false
    }).start();
  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth]);
  useEffect(() => {
    onExpandedChange === null || onExpandedChange === void 0 || onExpandedChange(isExpanded);
  }, [isExpanded, onExpandedChange]);
  useEffect(() => {
    items.forEach(item => {
      if (!itemOffsets.current[item.id]) {
        itemOffsets.current[item.id] = new Animated.Value(0);
      }
    });
  }, [items]);

  // Reloc Item Logic
  const handleRelocDragStart = draggedItemIndex => {
    if (vibrate) Vibration.vibrate(50);
    logInteraction('Reloc drag started', items[draggedItemIndex].text, items[draggedItemIndex]);
  };
  const handleRelocDragEnd = _draggedItemIndex => {
    Object.values(itemOffsets.current).forEach(anim => anim.setValue(0));
    logInteraction('Reloc drag ended');
  };
  const handleRelocDragMove = (dy, draggedItemIndex) => {
    const draggedItem = items[draggedItemIndex];
    if (!draggedItem.isRelocItem || !draggedItem.hostId) return;
    const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId);
    const itemHeight = 48 + 8;
    const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
    const targetClusterIndex = RelocItemHandler.calculateTargetIndex(dy, draggedClusterIndex, cluster.length, itemHeight);

    // Animation logic omitted for brevity, but same as before
    cluster.forEach((item, index) => {
      if (item.id === draggedItem.id) return;
      let offset = 0;
      if (draggedClusterIndex < targetClusterIndex) {
        if (index > draggedClusterIndex && index <= targetClusterIndex) offset = -itemHeight;
      } else if (draggedClusterIndex > targetClusterIndex) {
        if (index >= targetClusterIndex && index < draggedClusterIndex) offset = itemHeight;
      }
      Animated.spring(itemOffsets.current[item.id], {
        toValue: offset,
        useNativeDriver: false
      }).start();
    });
    panValue.targetIndex = targetClusterIndex;
  };
  const handleRelocDrop = draggedItemIndex => {
    const draggedItem = items[draggedItemIndex];
    const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId);
    const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
    const targetClusterIndex = panValue.targetIndex ?? draggedClusterIndex;
    if (draggedClusterIndex !== targetClusterIndex) {
      const newItems = RelocItemHandler.reorderItems(items, draggedItem.id, draggedItem.hostId, targetClusterIndex);
      setItems(newItems);
      const newCluster = RelocItemHandler.getCluster(newItems, draggedItem.hostId);
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
      Animated.timing(pan, {
        toValue: {
          x: currentX,
          y: newY
        },
        duration: 200,
        useNativeDriver: false
      }).start();
    }
  };
  const panResponder = useRef(PanResponder.create({
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
        if (vibrate) Vibration.vibrate(50);
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
      const iconSize = AzNavRailDefaults.HeaderIconSize;
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
        if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
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
      if (vibrate) Vibration.vibrate(50);
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
    const isRect = item.shape === AzButtonShape.RECTANGLE;
    const commonProps = {
      key: item.id,
      color: overrideConfig.activeColor && (item.isChecked || item.id === currentDestination) ? overrideConfig.activeColor : item.color,
      shape: item.shape || overrideConfig.defaultShape,
      enabled: !item.disabled,
      style: {
        marginBottom: isRect ? 2 : AzNavRailDefaults.RailContentVerticalArrangement
      }
    };
    if (item.isRelocItem) {
      return /*#__PURE__*/React.createElement(DraggableRailItemWrapper, {
        key: item.id,
        item: item,
        index: items.indexOf(item),
        totalItems: items.length,
        onDragStart: handleRelocDragStart,
        onDragEnd: handleRelocDrop,
        onDragMove: handleRelocDragMove,
        offsetY: itemOffsets.current[item.id] || new Animated.Value(0),
        style: commonProps.style,
        translucentBackground: config.translucentBackground
      });
    }
    if (item.isHost) {
      return /*#__PURE__*/React.createElement(View, {
        key: item.id,
        style: {
          alignItems: 'center',
          width: '100%'
        }
      }, /*#__PURE__*/React.createElement(AzButton, _extends({}, commonProps, {
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
      return /*#__PURE__*/React.createElement(AzCycler, _extends({}, commonProps, {
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
      return /*#__PURE__*/React.createElement(AzToggle, _extends({}, commonProps, {
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
    return /*#__PURE__*/React.createElement(View, {
      key: item.id,
      onLayout: !isFloating || config.infoScreen || item.isNestedRail ? e => handleItemLayout(item.id, e) : undefined
    }, /*#__PURE__*/React.createElement(AzButton, _extends({}, commonProps, {
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
    return /*#__PURE__*/React.createElement(AzKineticItem, {
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
    }, /*#__PURE__*/React.createElement(View, {
      onLayout: config.infoScreen || item.isHost ? e => handleItemLayout(item.id, e) : undefined
    }, /*#__PURE__*/React.createElement(RailMenuItem, {
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
      renderSubItems: () => /*#__PURE__*/React.createElement(React.Fragment, null, subItems.map((subItem, i) => renderMenuItem(subItem, i, subItems.length, visible, depth + 1, new Set(ancestors).add(item.id))))
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
          Linking.openURL(config.appRepositoryUrl).catch(e => console.error("Could not open About", e));
        }
      }
    };
    const handleFeedback = () => {
      Linking.openURL('mailto:hereliesaz@gmail.com?subject=Feedback for AzNavRail').catch(e => console.error("Could not open Mail", e));
    };
    const handleCredit = () => {
      Linking.openURL('https://instagram.com/HereLiesAz').catch(e => console.error("Could not open Credit", e));
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
    return /*#__PURE__*/React.createElement(View, {
      style: [styles.footer, {
        alignItems: 'center'
      }]
    }, /*#__PURE__*/React.createElement(View, {
      style: styles.divider
    }), enableRailDragging && /*#__PURE__*/React.createElement(TouchableOpacity, {
      onPress: handleUndock,
      style: {
        paddingVertical: 8
      }
    }, /*#__PURE__*/React.createElement(Text, {
      style: [styles.menuItemText, {
        color: footerColor,
        fontWeight: 'bold'
      }]
    }, `Undock`)), !!config.appRepositoryUrl && /*#__PURE__*/React.createElement(TouchableOpacity, {
      onPress: handleAbout,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/React.createElement(Text, {
      style: [styles.menuItemText, {
        color: footerColor
      }]
    }, "About")), /*#__PURE__*/React.createElement(TouchableOpacity, {
      onPress: handleFeedback,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/React.createElement(Text, {
      style: [styles.menuItemText, {
        color: footerColor
      }]
    }, "Feedback")), /*#__PURE__*/React.createElement(TouchableOpacity, {
      onPress: handleCredit,
      onLongPress: handleSecLocTrigger,
      delayLongPress: 500,
      style: {
        paddingVertical: 4
      }
    }, /*#__PURE__*/React.createElement(Text, {
      style: [styles.menuItemText, {
        color: footerColor,
        opacity: 0.5
      }]
    }, "@HereLiesAz")));
  };
  const effectiveRailItems = useMemo(() => {
    // Only the top-level items are listed here; `renderRailItem`'s host branch renders each
    // expanded host's sub-items inline and recurses for sub-hosts, so hosts nest to any depth
    // without this list having to be flattened (which would otherwise double-render sub-items).
    return items.filter(i => !i.isSubItem && (config.noMenu || i.isRailItem));
  }, [items, config.noMenu]);
  const menuItems = useMemo(() => {
    return items.filter(i => !i.isSubItem);
  }, [items]);

  // — Kinetic typography (WP7). Defaults animate; opt out via settings (itemEntrance: None, …). —
  const kItemEntrance = config.itemEntrance ?? AzEntrance.Turnstile;
  const kItemExit = config.itemExit ?? AzExit.Turnstile;
  const kStaggerMs = config.entranceStaggerMs ?? 55;
  const kDurationMs = config.entranceDurationMs ?? 360;
  const kStartAngle = config.entranceStartAngle ?? 70;
  const kTiltOnPress = config.tiltOnPress ?? false;
  const kMaxTilt = config.maxTiltDegrees ?? 10;
  const kItemTextStyle = config.itemTextStyle;
  const kDockingSide = config.dockingSide ?? AzDockingSide.LEFT;
  // Keep the menu mounted through the staggered exit so items can turnstile out as the rail collapses.
  const menuRendered = useAzClosing(isExpanded && !config.noMenu, kItemExit, menuItems.length, kStaggerMs, kDurationMs);
  const getHeaderBorderRadius = () => {
    if (config.headerIconShape === AzHeaderIconShape.SQUARE) return 0;
    if (config.headerIconShape === AzHeaderIconShape.ROUNDED) return 8;
    return 24;
  };
  const flexDirection = config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row';
  return /*#__PURE__*/React.createElement(AzNavRailContext.Provider, {
    value: {
      register,
      unregister,
      updateSettings,
      getDividerId,
      hasItem
    }
  }, /*#__PURE__*/React.createElement(View, {
    style: {
      flexDirection: flexDirection,
      height: '100%',
      flex: 1
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
      height: isFloating ? 'auto' : '100%',
      borderRightWidth: dockingSide === AzDockingSide.LEFT ? 1 : 0,
      borderLeftWidth: dockingSide === AzDockingSide.RIGHT ? 1 : 0
    }]
  }, panResponder.panHandlers), /*#__PURE__*/React.createElement(TouchableOpacity, {
    onPress: handleHeaderTap,
    onLongPress: handleHeaderLongPress,
    delayLongPress: 500,
    style: [styles.header, {
      flexDirection: config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row'
    }],
    onLayout: e => setHeaderHeight(e.nativeEvent.layout.height),
    accessibilityRole: "button",
    accessibilityLabel: isFloating ? "Undocked Rail" : isExpanded ? "Collapse Menu" : "Expand Menu",
    accessibilityHint: "Double tap to toggle, long press to drag"
  }, /*#__PURE__*/React.createElement(View, {
    style: {
      width: AzNavRailDefaults.HeaderIconSize,
      height: AzNavRailDefaults.HeaderIconSize,
      backgroundColor: 'gray',
      borderRadius: getHeaderBorderRadius(),
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement(Text, {
    style: {
      color: 'white'
    }
  }, "Icon")), !isFloating && isExpanded && config.displayAppNameInHeader && /*#__PURE__*/React.createElement(Text, {
    style: [styles.appName, {
      marginLeft: config.dockingSide === AzDockingSide.RIGHT ? 0 : 16,
      marginRight: config.dockingSide === AzDockingSide.RIGHT ? 16 : 0,
      flexShrink: 0,
      minWidth: 200
    }],
    numberOfLines: 1
  }, "App Name")), menuRendered && !noMenu ? /*#__PURE__*/React.createElement(ScrollView, {
    style: styles.menuContent
  }, menuItems.map((item, index) => {
    if (item.isDivider) {
      return /*#__PURE__*/React.createElement(View, {
        key: item.id,
        style: styles.divider
      });
    }
    return renderMenuItem(item, index, menuItems.length, isExpanded);
  }), showFooter && renderFooter()) : /*#__PURE__*/React.createElement(ScrollView, {
    contentContainerStyle: styles.railContent
  }, isFloating && !showFloatingButtons ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index)), !isFloating && config.moreRailItem && config.moreFromAzEnabled && /*#__PURE__*/React.createElement(AzButton, {
    text: "More",
    color: config.activeColor || '#6200ee',
    shape: config.defaultShape,
    onClick: () => setShowMoreFromAz(true)
  }))), items.filter(i => i.isNestedRail).map(item => {
    const effectiveConfig = item.nestedRailSettings ? {
      ...config,
      ...item.nestedRailSettings
    } : config;
    return /*#__PURE__*/React.createElement(AzNestedRailPopup, {
      key: `nested-${item.id}`,
      visible: nestedRailVisible === item.id,
      onDismiss: () => setNestedRailVisible(null),
      items: subItemsMap[item.id] || [],
      alignment: item.nestedRailAlignment || AzNestedRailAlignment.VERTICAL,
      renderItem: (subItem, idx) => {
        return /*#__PURE__*/React.createElement(View, {
          key: `wrap-${subItem.id}`,
          onLayout: e => handleItemLayout(subItem.id, e)
        }, renderRailItem(subItem, idx, effectiveConfig));
      },
      anchorPosition: anchorPosition,
      dockingSide: effectiveConfig.dockingSide,
      helpList: effectiveConfig.helpList
    });
  }), /*#__PURE__*/React.createElement(View, {
    style: {
      flex: 1,
      marginTop: '20%',
      marginBottom: '10%'
    }
  }, children, secLocVisible && /*#__PURE__*/React.createElement(View, null), " ", secLocClicks > 0 && /*#__PURE__*/React.createElement(View, null), " "), isLoading && /*#__PURE__*/React.createElement(View, {
    style: styles.loaderOverlay
  }, /*#__PURE__*/React.createElement(AzLoad, null)), infoScreen && !showAbout && !showMoreFromAz && /*#__PURE__*/React.createElement(View, {
    style: StyleSheet.absoluteFill
  }, /*#__PURE__*/React.createElement(HelpOverlay, {
    items: items,
    helpList: config.helpList || {},
    onDismiss: onDismissInfoScreen,
    itemBounds: itemBounds,
    nestedRailVisibleId: nestedRailVisible
  })), showAbout && !showMoreFromAz && !!config.appRepositoryUrl && /*#__PURE__*/React.createElement(AboutOverlay, {
    repoUrl: config.appRepositoryUrl,
    settings: {
      activeColor: config.activeColor,
      translucentBackground: config.translucentBackground
    },
    moreFromAzEnabled: config.moreFromAzEnabled,
    moreFromAzJsonUrl: config.moreFromAzJsonUrl,
    onDismiss: () => setShowAbout(false)
  }), showMoreFromAz && /*#__PURE__*/React.createElement(MoreFromAzOverlay, {
    jsonUrl: config.moreFromAzJsonUrl,
    settings: {
      activeColor: config.activeColor,
      translucentBackground: config.translucentBackground
    },
    onDismiss: () => setShowMoreFromAz(false)
  }), !showAbout && !showMoreFromAz && guidance.enabled && /*#__PURE__*/React.createElement(AzInstructionOverlay, {
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
export const AzNavRail = props => {
  return /*#__PURE__*/React.createElement(AzGuidanceProvider, null, /*#__PURE__*/React.createElement(AzNavRailInner, props));
};
const styles = StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderColor: '#ddd'
  },
  header: {
    alignItems: 'center',
    padding: AzNavRailDefaults.HeaderPadding
  },
  appName: {
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
  },
  infoOverlay: {
    ...StyleSheet.absoluteFillObject,
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