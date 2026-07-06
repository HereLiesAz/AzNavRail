import React, { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Animated,
  StyleSheet,
  PanResponder,
  Dimensions,
  ScrollView,
  Linking,
  Vibration,
  UIManager,
} from 'react-native';
import { AzNavRailContext } from './AzNavRailScope';
import { AzNavItem, AzNavRailSettings, AzButtonShape, AzDockingSide, AzHeaderIconShape, AzNestedRailAlignment, AzEntrance, AzExit } from './types';
import { AzKineticItem, useAzClosing } from './components/AzKinetics';
import { Easing as RNEasing } from 'react-native';
import { AzEasing } from './types';
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
import { useActiveStatuses, computeBuiltinStatuses, useGuidanceSuppressed } from './guidance/AzStatusEngine';
import { routeInstructions, computeAutoEdges, snapshotsOf, edgeStepKey } from './guidance/AzGuidance';
import { AzInstructionOverlay } from './components/AzInstructionOverlay';

/** Props for the `AzNavRail` component, extending all `AzNavRailSettings` options. */
interface AzNavRailProps extends AzNavRailSettings {
  /** DSL item declarations (`AzRailItem`, `AzMenuToggle`, etc.) and screen content rendered beside the rail. */
  children: React.ReactNode;
  /** Navigation controller reference (passed through to the host context). */
  navController?: any;
  /** Active route string used to highlight the matching rail item. */
  currentDestination?: string;
  /** When true, applies landscape-aware layout adjustments. */
  isLandscape?: boolean;
  /** When true, the rail starts in expanded (menu) state. */
  initiallyExpanded?: boolean;
  /** Disables the swipe gesture that expands the rail when the user swipes from the edge. */
  disableSwipeToOpen?: boolean;
  /** Called whenever the rail transitions between collapsed and expanded states. */
  onExpandedChange?: (expanded: boolean) => void;
  /** Called whenever any rail item is interacted with. Receives the action name, an optional detail string, and the interacted item (if applicable). */
  onInteraction?: (action: string, details?: string, item?: AzNavItem) => void;
}

/**
 * Unfolds children downward (scaleY 0→1 + fade) with an accordion motion. Kicks off one stagger
 * tick AFTER the last menu item starts its own kinetic entrance — the footer is the natural next
 * beat in the cascade, scheduled `menuItemCount * staggerMs` after the drawer opens.
 */
const FooterAccordion: React.FC<{
  visible: boolean;
  menuItemCount: number;
  staggerMs: number;
  durationMs: number;
  children?: React.ReactNode;
}> = ({ visible, menuItemCount, staggerMs, durationMs, children }) => {
  // Always start collapsed so the first mount plays the fold-in. Previously we initialized to
  // `visible ? 1 : 0`, which meant the very first render on drawer-open showed the footer already
  // in place instead of unfolding.
  const anim = useRef(new Animated.Value(0)).current;
  useEffect(() => {
    if (visible) {
      const a = Animated.timing(anim, {
        toValue: 1,
        duration: durationMs,
        delay: Math.max(0, menuItemCount) * staggerMs,
        easing: RNEasing.bezier(...AzEasing.Wp7Decelerate),
        useNativeDriver: true,
      });
      a.start();
      return () => a.stop();
    }
    // Fold-up on close: animate to 0 (no delay — the footer is the FIRST thing to leave, before
    // the items begin their bottom-up exit cascade).
    const a = Animated.timing(anim, {
      toValue: 0,
      duration: durationMs,
      easing: RNEasing.bezier(...AzEasing.Wp7Decelerate),
      useNativeDriver: true,
    });
    a.start();
    return () => a.stop();
  }, [visible, menuItemCount, staggerMs, durationMs, anim]);
  return (
    <Animated.View
      style={{
        opacity: anim,
        transform: [{ scaleY: anim }],
        // Web-only hinge from the top edge; native RN ignores transformOrigin.
        ...( { transformOrigin: 'top center' } as any ),
      }}
    >
      {children}
    </Animated.View>
  );
};

const AzNavRailInner: React.FC<AzNavRailProps> = (props) => {
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
      moreRailItem = false,
  } = props;
  const logInteraction = useCallback(
    (action: string, details?: string, item?: AzNavItem) => {
      if (onInteraction) {
        onInteraction(action, details, item);
        return;
      }

      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        const suffix = details ? ` ${details}` : '';
        // eslint-disable-next-line no-console
        console.log(`[AzNavRail] ${action}${suffix}`);
      }
    },
    [onInteraction]
  );
  const [items, setItems] = useState<AzNavItem[]>([]);
  const [dslOverrides, setDslOverrides] = useState<Partial<AzNavRailSettings>>({});

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
      appRepositoryUrl: (dslOverrides as any).appRepositoryUrl ?? appRepositoryUrl,
      inAppAbout: dslOverrides.inAppAbout ?? inAppAbout,
      moreFromAzEnabled: dslOverrides.moreFromAzEnabled ?? moreFromAzEnabled,
      moreFromAzJsonUrl: dslOverrides.moreFromAzJsonUrl ?? moreFromAzJsonUrl,
      moreRailItem: dslOverrides.moreRailItem ?? moreRailItem,
  };

  const [isExpanded, setIsExpanded] = useState(initiallyExpanded && !config.noMenu);
  const [showAbout, setShowAbout] = useState(false);
  const [showMoreFromAz, setShowMoreFromAz] = useState(false);
  const [isFloating, setIsFloating] = useState(false);
  const [showFloatingButtons, setShowFloatingButtons] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const [hostStates, setHostStates] = useState<Record<string, boolean>>({});
  const [nestedRailVisible, setNestedRailVisible] = useState<string | null>(null);
  const [anchorPosition, setAnchorPosition] = useState<{ x: number, y: number, width: number, height: number } | undefined>(undefined);
  const [itemBounds, setItemBounds] = useState<Record<string, any>>({});
  const [secLocClicks, setSecLocClicks] = useState(0);
  const [secLocVisible, setSecLocVisible] = useState(false);

  const handleItemLayout = useCallback((id: string, e: any) => {
      const target = e.target as any;
      if (target) {
          UIManager.measureInWindow(target, (x: number, y: number, width: number, height: number) => {
              if (x !== undefined && y !== undefined && width !== undefined && height !== undefined) {
                  const bounds = { x, y, width, height };
                  setItemBounds(prev => ({ ...prev, [id]: bounds }));
                  if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
              } else {
                  const bounds = { ...e.nativeEvent.layout };
                  setItemBounds(prev => ({ ...prev, [id]: bounds }));
                  if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
              }
          });
      } else {
          const bounds = { ...e.nativeEvent.layout };
          setItemBounds(prev => ({ ...prev, [id]: bounds }));
          if (config.onItemGloballyPositioned) config.onItemGloballyPositioned(id, bounds);
      }
  }, [config]);

  const subItemsMap = useMemo(() => {
    const map: Record<string, AzNavItem[]> = {};
    items.forEach((item) => {
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
  const panValue = useRef({ x: 0, y: 0 });
  const dragStartPos = useRef({ x: 0, y: 0 });

  const itemOffsets = useRef<Record<string, Animated.Value>>({});
  const screenHeightRef = useRef(Dimensions.get('window').height);

  const isFloatingRef = useRef(isFloating);
  const enableRailDraggingRef = useRef(enableRailDragging);
  const showFloatingButtonsRef = useRef(showFloatingButtons);
  const wasVisibleOnDragStartRef = useRef(false);
  const itemsRef = useRef(items);
  // Edge-tracking for host auto-expansion (expandWhen / per-host initiallyExpanded).
  const expandWhenSeenRef = useRef<Record<string, boolean>>({});
  const initiallyExpandedSeenRef = useRef<Record<string, boolean>>({});

  useEffect(() => { isFloatingRef.current = isFloating; }, [isFloating]);
  useEffect(() => { enableRailDraggingRef.current = enableRailDragging; }, [enableRailDragging]);
  useEffect(() => { showFloatingButtonsRef.current = showFloatingButtons; }, [showFloatingButtons]);
  useEffect(() => { itemsRef.current = items; }, [items]);

  // Harden against a host auto-expanding (or any reorder/add/remove) mid-shuffle: zero every
  // sibling-displacement offset so a stale one can't leave an item rendered on top of its neighbour.
  // Mirrors the Android RailItems structural-change guard. Keyed on item ids + expanded host set,
  // since expansion changes the visible layout without changing the items array.
  const expandedHostKey = Object.keys(hostStates).filter((k) => hostStates[k]).sort().join(',');
  const structuralKey = items.map((i) => i.id).join(',') + '|' + expandedHostKey;
  useEffect(() => {
    Object.values(itemOffsets.current).forEach((anim) => anim.setValue(0));
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
    const updates: Record<string, boolean> = {};
    let changed = false;
    itemsRef.current.forEach((item) => {
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
          if (now) { updates[item.id] = true; changed = true; }
        } else if (before !== now) {
          updates[item.id] = now;
          changed = true;
        }
        expandWhenSeenRef.current[item.id] = now;
      }
    });
    if (!changed) return;
    setHostStates((prev) => {
      let differs = false;
      for (const k in updates) if (prev[k] !== updates[k]) { differs = true; break; }
      return differs ? { ...prev, ...updates } : prev;
    });
  }, []);

  // After every render: React-state/prop-driven conditions are caught here immediately.
  useEffect(() => { applyAutoExpansions(); });
  // Low-rate poll: the safety net for conditions that aren't React state.
  useEffect(() => {
    const t = setInterval(applyAutoExpansions, 300);
    return () => clearInterval(t);
  }, [applyAutoExpansions]);

  // Status-driven guidance runs in its own isolated <AzGuidanceLayer> (rendered below) so its engine
  // state never re-renders this host — keeping the rail's item-registry effect timing untouched.

  useEffect(() => {
      const id = pan.addListener((value) => { panValue.current = value; });
      return () => pan.removeListener(id);
  }, [pan]);

  useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({ window }) => {
      screenHeightRef.current = window.height;
    });
    return () => {
      if (!subscription) return;
      // @ts-ignore
      if (typeof subscription === 'function') {
        // @ts-ignore
        subscription();
      } else if (typeof (subscription as any).remove === 'function') {
        (subscription as any).remove();
      }
    };
  }, []);

  const updateSettings = useCallback((newSettings: any) => {
      setDslOverrides(prev => ({ ...prev, ...newSettings }));
  }, []);

  const dividerCounter = useRef(0);
  const getDividerId = useCallback(() => {
      const currentItems = itemsRef.current || [];
      return `divider_${currentItems.length + dividerCounter.current++}`;
  }, []);

  const hasItem = useCallback((id: string) => {
      return (itemsRef.current || []).some(i => i.id === id);
  }, []);

  const register = useCallback((item: AzNavItem) => {
    setItems((prev) => {
      const index = prev.findIndex((i) => i.id === item.id);
      if (index >= 0) {
        const old = prev[index];
        const isSame =
          old.text === item.text &&
          old.disabled === item.disabled &&
          old.isChecked === item.isChecked &&
          old.selectedOption === item.selectedOption &&
          old.isExpanded === item.isExpanded &&
          JSON.stringify(old.options) === JSON.stringify(item.options) &&
          old.shape === item.shape &&
          old.color === item.color &&
          old.route === item.route &&
          old.isRelocItem === item.isRelocItem &&
          old.hiddenMenu === item.hiddenMenu &&
          old.info === item.info;

        if (isSame) return prev;
        const newItems = [...prev];
        newItems[index] = item;
        return newItems;
      }
      return [...prev, item];
    });
  }, []);

  const unregister = useCallback((id: string) => {
    setItems((prev) => prev.filter((i) => i.id !== id));
    if (itemOffsets.current[id]) delete itemOffsets.current[id];
  }, []);

  useEffect(() => {
    Animated.timing(railWidthAnim, {
      toValue: isExpanded ? config.expandedRailWidth : config.collapsedRailWidth,
      duration: 300,
      useNativeDriver: false,
    }).start();
  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth]);

  useEffect(() => {
    onExpandedChange?.(isExpanded);
  }, [isExpanded, onExpandedChange]);

  useEffect(() => {
      items.forEach(item => {
          if (!itemOffsets.current[item.id]) {
              itemOffsets.current[item.id] = new Animated.Value(0);
          }
      });
  }, [items]);

  // Reloc Item Logic
  const handleRelocDragStart = (draggedItemIndex: number) => {
      if (vibrate) Vibration.vibrate(50);
      logInteraction('Reloc drag started', items[draggedItemIndex].text, items[draggedItemIndex]);
  };

  const handleRelocDragEnd = (_draggedItemIndex: number) => {
      Object.values(itemOffsets.current).forEach(anim => anim.setValue(0));
      logInteraction('Reloc drag ended');
  };

  const handleRelocDragMove = (dy: number, draggedItemIndex: number) => {
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
          Animated.spring(itemOffsets.current[item.id], { toValue: offset, useNativeDriver: false }).start();
      });

      (panValue as any).targetIndex = targetClusterIndex;
  };

  const handleRelocDrop = (draggedItemIndex: number) => {
       const draggedItem = items[draggedItemIndex];
       const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId!);
       const draggedClusterIndex = cluster.findIndex(i => i.id === draggedItem.id);
       const targetClusterIndex = (panValue as any).targetIndex ?? draggedClusterIndex;

       if (draggedClusterIndex !== targetClusterIndex) {
           const newItems = RelocItemHandler.reorderItems(items, draggedItem.id, draggedItem.hostId!, targetClusterIndex);
           setItems(newItems);
           const newCluster = RelocItemHandler.getCluster(newItems, draggedItem.hostId!);
           const newOrder = newCluster.map(i => i.id);
           if (draggedItem.onRelocate) {
               draggedItem.onRelocate(draggedClusterIndex, targetClusterIndex, newOrder);
           }
       }
       delete (panValue as any).targetIndex;
       handleRelocDragEnd(draggedItemIndex);
  };


  const adjustFloatingRailWithinBounds = (currentX: number, currentY: number) => {
      const screenHeight = screenHeightRef.current;
      const railItemsCountForDrag = itemsRef.current.filter(i => i.isRailItem && !i.isSubItem).length;
      const contentHeight = headerHeight + (railItemsCountForDrag * 56) + 16;
      const bottomY = currentY + contentHeight;
      const limitY = screenHeight * 0.9;
      if (bottomY > limitY) {
          const shift = bottomY - limitY;
          const newY = currentY - shift;
          Animated.timing(pan, { toValue: { x: currentX, y: newY }, duration: 200, useNativeDriver: false }).start();
      }
  };

  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gestureState) => {
        if (!enableRailDraggingRef.current && !isFloatingRef.current) return false;
        const { dx, dy } = gestureState;
        if (!isFloatingRef.current && Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > 10 && enableRailDraggingRef.current) {
            return true;
        }
        return isFloatingRef.current;
      },
      onPanResponderGrant: () => {
        if (isFloatingRef.current) {
             pan.setOffset({ x: panValue.current.x, y: panValue.current.y });
             pan.setValue({ x: 0, y: 0 });
             dragStartPos.current = { x: panValue.current.x, y: panValue.current.y };
             wasVisibleOnDragStartRef.current = showFloatingButtonsRef.current;
             setShowFloatingButtons(false);
             logInteraction('Drag started', 'FAB mode');
        } else {
             if (vibrate) Vibration.vibrate(50);
             setIsFloating(true);
             setIsExpanded(false);
             logInteraction('Swipe detected', 'Entering FAB mode');
             pan.setOffset({ x: 0, y: 0 });
             pan.setValue({ x: 0, y: 0 });
             dragStartPos.current = { x: 0, y: 0 };
        }
      },
      onPanResponderMove: (_, gestureState) => {
          if (!isFloatingRef.current) return;
          const { dx, dy } = gestureState;
          const screenHeight = screenHeightRef.current;
          const iconSize = AzNavRailDefaults.HeaderIconSize;
          const minY = screenHeight * 0.1;
          const maxY = screenHeight * 0.9 - iconSize;
          const targetY = dragStartPos.current.y + dy;
          let clampedY = Math.max(minY, Math.min(targetY, maxY));
          pan.setValue({ x: dx, y: clampedY - dragStartPos.current.y });
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
                 pan.setValue({ x: 0, y: 0 });
                 logInteraction('Snap back', 'Docked');
             } else if (wasVisibleOnDragStartRef.current) {
                 setShowFloatingButtons(true);
                 adjustFloatingRailWithinBounds(currentX, currentY);
             }
        }
      },
    })
  ).current;

  const handleHeaderLongPress = () => {
      if (config.enableRailDragging) {
          if (vibrate) Vibration.vibrate(50);
          if (isFloating) {
              setIsFloating(false);
              pan.setValue({ x: 0, y: 0 });
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

  const renderRailItem = (item: AzNavItem, _index: number, overrideConfig: any = config, ancestors: Set<string> = new Set()): React.ReactNode => {
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
          style: { marginBottom: isRect ? 2 : AzNavRailDefaults.RailContentVerticalArrangement }
      };

      if (item.isRelocItem) {
          return (
              <DraggableRailItemWrapper
                  key={item.id}
                  item={item}
                  index={items.indexOf(item)}
                  totalItems={items.length}
                  onDragStart={handleRelocDragStart}
                  onDragEnd={handleRelocDrop}
                  onDragMove={handleRelocDragMove}
                  offsetY={itemOffsets.current[item.id] || new Animated.Value(0)}
                  style={commonProps.style}
                  translucentBackground={config.translucentBackground}
              />
          );
      }

      if (item.isHost) {
           return (
             <View key={item.id} style={{ alignItems: 'center', width: '100%' }}>
                 <AzButton
                     {...commonProps}
                     fillColor={item.fillColor}
                     text={item.text}
                     content={item.content}
                     hasCustomContent={!!item.content}
                     onClick={() => {
                         const newHostState = !(hostStates[item.id] || false);
                         setHostStates(prev => ({...prev, [item.id]: newHostState}));
                         logInteraction('Host toggled', item.text, item);
                         item.onExpandedChange?.(newHostState);
                     }}
                 />
                 {isExpandedHost && subItems.filter(sub => sub.isRailItem).map((sub) => renderRailItem(sub, items.indexOf(sub), overrideConfig, new Set(ancestors).add(item.id)))}
             </View>
           );
      }

      if (item.isCycler) {
          return (
              <AzCycler
                  {...commonProps}
                  fillColor={item.fillColor}
                  options={item.options || []}
                  selectedOption={item.selectedOption || ''}
                  onCycle={() => {
                      if (item.onClick) item.onClick();
                      logInteraction('Cycler cycled', item.text, item);
                  }}
              />
          );
      }
      if (item.isToggle) {
          return (
              <AzToggle
                  {...commonProps}
                  fillColor={item.fillColor}
                  isChecked={item.isChecked || false}
                  toggleOnText={item.toggleOnText}
                  toggleOffText={item.toggleOffText}
                  onToggle={() => {
                      if (item.onClick) item.onClick();
                      logInteraction('Toggle toggled', item.text, item);
                  }}
              />
          );
      }

      return (
          <View
              key={item.id}
              onLayout={(!isFloating || config.infoScreen || item.isNestedRail) ? (e) => handleItemLayout(item.id, e) : undefined}
          >
            <AzButton
                {...commonProps}
                fillColor={item.fillColor}
                text={item.text}
                content={item.content}
                hasCustomContent={!!item.content}
                onClick={() => {
                    logInteraction('Item clicked', item.text, item);
                    if (item.isNestedRail) {
                        setNestedRailVisible(item.id);
                        // Use actual bounds if available
                        const bounds = itemBounds[item.id];
                        if (bounds) {
                            setAnchorPosition(bounds);
                        } else {
                            setAnchorPosition({ x: 0, y: _index * 60 + headerHeight, width: config.collapsedRailWidth, height: 48 });
                        }
                    }
                    if (item.onClick) item.onClick();
                    if (item.collapseOnClick && !config.noMenu) setIsExpanded(false);
                }}
            />
          </View>
      );
  };

  const renderMenuItem = (
      item: AzNavItem,
      index = 0,
      count = 1,
      visible = true,
      depth = 0,
      ancestors: Set<string> = new Set(),
  ): React.ReactNode => {
      // Cycle guard (see renderRailItem): stop before recursing into an ancestor so a cyclic or
      // self-referential `hostId` chain can't overflow the stack.
      if (ancestors.has(item.id)) return null;
      const isExpandedHost = hostStates[item.id] || false;
      const subItems = subItemsMap[item.id] || [];

      return (
          <AzKineticItem
              key={item.id}
              index={index}
              count={count}
              visible={visible}
              entrance={kItemEntrance}
              exit={kItemExit}
              staggerMs={kStaggerMs}
              durationMs={kDurationMs}
              startAngle={kStartAngle}
              // Suppress the press-tilt on draggable items so it never fights the drag gesture.
              tiltOnPress={kTiltOnPress && !item.isRelocItem}
              maxTiltDegrees={kMaxTilt}
              dockingSide={kDockingSide}
              floating={isFloating}
          >
              <View
                  onLayout={(config.infoScreen || item.isHost) ? (e) => handleItemLayout(item.id, e) : undefined}
              >
                  <RailMenuItem
                      item={item}
                      depth={depth}
                      isExpandedHost={isExpandedHost}
                      textStyle={kItemTextStyle}
                      dockingSide={kDockingSide}
                      menuItemAlignment={menuItemAlignment}
                      justifyMenuItems={justifyMenuItems}
                      onToggleHost={() => {
                          const newHostState = !(hostStates[item.id] || false);
                          setHostStates(prev => ({ ...prev, [item.id]: newHostState }));
                          item.onExpandedChange?.(newHostState);
                      }}
                      onItemClick={() => {
                          logInteraction('Menu item clicked', item.text, item);
                          if (item.onClick) item.onClick();
                          if (item.collapseOnClick) setIsExpanded(false);
                      }}
                      renderSubItems={() => (
                          <>
                          {subItems.map((subItem, i) => renderMenuItem(subItem, i, subItems.length, visible, depth + 1, new Set(ancestors).add(item.id)))}
                          </>
                      )}
                  />
              </View>
          </AzKineticItem>
      );
  };

  const renderFooter = () => {
      // Use `config.activeColor` so DSL overrides (via `dslOverrides.activeColor`) win over the raw
      // prop — matches the pattern used everywhere else the rail reads its theming.
      const footerColor = config.activeColor || '#6200ee';

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

      return (
        <FooterAccordion
          visible={isExpanded}
          menuItemCount={menuItems.length}
          staggerMs={kStaggerMs}
          durationMs={kDurationMs}
        >
          <View style={[styles.footer, { alignItems: 'center' }]}>
             <View style={[styles.divider, { backgroundColor: footerColor }]} />
             {enableRailDragging && (
                 <TouchableOpacity onPress={handleUndock} style={{ paddingVertical: 8 }}><Text style={[styles.menuItemText, { color: footerColor, fontWeight: 'bold' }]}>{`Undock`}</Text></TouchableOpacity>
             )}
             {!!config.appRepositoryUrl && (
                 <TouchableOpacity onPress={handleAbout} style={{ paddingVertical: 4 }}><Text style={[styles.menuItemText, { color: footerColor }]}>About</Text></TouchableOpacity>
             )}
             <TouchableOpacity onPress={handleFeedback} style={{ paddingVertical: 4 }}><Text style={[styles.menuItemText, { color: footerColor }]}>Feedback</Text></TouchableOpacity>
             <TouchableOpacity onPress={handleCredit} onLongPress={handleSecLocTrigger} delayLongPress={500} style={{ paddingVertical: 4 }}><Text style={[styles.menuItemText, { color: footerColor }]}>@HereLiesAz</Text></TouchableOpacity>
          </View>
        </FooterAccordion>
      );
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
  const kItemEntrance = (config as AzNavRailSettings).itemEntrance ?? AzEntrance.Turnstile;
  const kItemExit = (config as AzNavRailSettings).itemExit ?? AzExit.Turnstile;
  const kStaggerMs = (config as AzNavRailSettings).entranceStaggerMs ?? 60;
  const kDurationMs = (config as AzNavRailSettings).entranceDurationMs ?? 720;
  const kStartAngle = (config as AzNavRailSettings).entranceStartAngle ?? 90;
  const kTiltOnPress = (config as AzNavRailSettings).tiltOnPress ?? false;
  const kMaxTilt = (config as AzNavRailSettings).maxTiltDegrees ?? 10;
  const kItemTextStyle = (config as AzNavRailSettings).itemTextStyle;
  const kDockingSide = config.dockingSide ?? AzDockingSide.LEFT;
  // Menu-drawer look/feel — new defaults (side-aligned + justified) preserve caller intent when unset.
  const menuItemAlignment = (config as AzNavRailSettings).menuItemAlignment ?? 'side';
  const justifyMenuItems = (config as AzNavRailSettings).justifyMenuItems ?? true;
  const dimBehindMenu = (config as AzNavRailSettings).dimBehindMenu ?? false;
  const dimBehindMenuAlpha = (config as AzNavRailSettings).dimBehindMenuAlpha ?? 0.4;
  // Keep the menu mounted through the staggered exit so items can turnstile out as the rail collapses.
  const menuRendered = useAzClosing(isExpanded && !config.noMenu, kItemExit, menuItems.length, kStaggerMs, kDurationMs);

  const getHeaderBorderRadius = () => {
    if (config.headerIconShape === AzHeaderIconShape.SQUARE) return 0;
    if (config.headerIconShape === AzHeaderIconShape.ROUNDED) return 8;
    return 24;
  };

  const flexDirection = config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row';


  return (
    <AzNavRailContext.Provider value={{ register, unregister, updateSettings, getDividerId, hasItem }}>
        <View style={{ flexDirection: flexDirection, height: '100%', flex: 1 }}>
            {/* Dim scrim behind the drawer: developer-opt-in via `dimBehindMenu`; alpha via
                `dimBehindMenuAlpha`. Tapping the scrim collapses the menu. */}
            {dimBehindMenu && isExpanded && !isFloating && (
              <TouchableOpacity
                onPress={() => setIsExpanded(false)}
                activeOpacity={1}
                style={{
                  ...(StyleSheet.absoluteFillObject as object),
                  backgroundColor: `rgba(0,0,0,${Math.max(0, Math.min(1, dimBehindMenuAlpha))})`,
                  zIndex: 5,
                }}
              />
            )}
            <Animated.View
                style={[
                    styles.railContainer,
                {
                    width: railWidthAnim,
                    position: isFloating ? 'absolute' : 'relative',
                    transform: isFloating ? [{ translateX: pan.x }, { translateY: pan.y }] : [],
                    zIndex: isFloating ? 1000 : 10,
                    height: isFloating ? 'auto' : '100%',
                    borderRightWidth: dockingSide === AzDockingSide.LEFT ? 1 : 0,
                    borderLeftWidth: dockingSide === AzDockingSide.RIGHT ? 1 : 0,
                }
            ]}
            {...panResponder.panHandlers}
        >
            <TouchableOpacity
                onPress={handleHeaderTap}
                onLongPress={handleHeaderLongPress}
                delayLongPress={500}
                style={[styles.header, { flexDirection: config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row' }]}
                onLayout={(e) => setHeaderHeight(e.nativeEvent.layout.height)}
                accessibilityRole="button"
                accessibilityLabel={isFloating ? "Undocked Rail" : (isExpanded ? "Collapse Menu" : "Expand Menu")}
                accessibilityHint="Double tap to toggle, long press to drag"
            >
                 <View style={{ width: AzNavRailDefaults.HeaderIconSize, height: AzNavRailDefaults.HeaderIconSize, backgroundColor: 'gray', borderRadius: getHeaderBorderRadius(), alignItems: 'center', justifyContent: 'center' }}>
                     <Text style={{ color: 'white' }}>Icon</Text>
                 </View>
                 {(!isFloating && isExpanded && config.displayAppNameInHeader) && (
                     <Text style={[styles.appName, { marginLeft: config.dockingSide === AzDockingSide.RIGHT ? 0 : 16, marginRight: config.dockingSide === AzDockingSide.RIGHT ? 16 : 0, flexShrink: 0, minWidth: 200 }]} numberOfLines={1}>App Name</Text>
                 )}
            </TouchableOpacity>

            {menuRendered && !noMenu ? (
                <ScrollView style={styles.menuContent}>
                    {menuItems.map((item, index) => {
                         if (item.isDivider) {
                             return <View key={item.id} style={[styles.divider, { backgroundColor: config.activeColor || '#6200ee' }]} />;
                         }
                         return renderMenuItem(item, index, menuItems.length, isExpanded);
                    })}
                    {showFooter && renderFooter()}
                </ScrollView>
            ) : (
                <ScrollView contentContainerStyle={styles.railContent}>
                     {(isFloating && !showFloatingButtons) ? null : effectiveRailItems.map((item, index) => renderRailItem(item, index))}
                     {(!isFloating && config.moreRailItem && config.moreFromAzEnabled) && (
                         <AzButton
                             text="More"
                             color={config.activeColor || '#6200ee'}
                             shape={config.defaultShape}
                             onClick={() => setShowMoreFromAz(true)}
                         />
                     )}
                </ScrollView>
            )}

            </Animated.View>

            {items.filter(i => i.isNestedRail).map(item => {
                const effectiveConfig = item.nestedRailSettings ? { ...config, ...item.nestedRailSettings } : config;
                return (
                    <AzNestedRailPopup
                        key={`nested-${item.id}`}
                        visible={nestedRailVisible === item.id}
                        onDismiss={() => setNestedRailVisible(null)}
                        items={subItemsMap[item.id] || []}
                        alignment={item.nestedRailAlignment || AzNestedRailAlignment.VERTICAL}
                        renderItem={(subItem, idx) => {
                            return (
                                <View key={`wrap-${subItem.id}`} onLayout={(e) => handleItemLayout(subItem.id, e)}>
                                    {renderRailItem(subItem, idx, effectiveConfig)}
                                </View>
                            );
                        }}
                        anchorPosition={anchorPosition}
                        dockingSide={effectiveConfig.dockingSide}
                        helpList={effectiveConfig.helpList}
                    />
                );
            })}

            {/* Content with Safe Zones */}
            <View style={{ flex: 1, marginTop: '20%', marginBottom: '10%' }}>
                {children}
                {secLocVisible && <View />} {/* Silence unused var */}
                {secLocClicks > 0 && <View />} {/* Silence unused var */}
            </View>

            {isLoading && (
                 <View style={styles.loaderOverlay}>
                     <AzLoad />
                 </View>
            )}

            {/* Help must be fully CLEARED from the screen while a footer screen (About / More-from-Az)
                is open — not merely dimmed — so it is not rendered at all then. */}
            {infoScreen && !showAbout && !showMoreFromAz && (
                <View style={StyleSheet.absoluteFill}>
                    <HelpOverlay
                        items={items}
                        helpList={config.helpList || {}}
                        onDismiss={onDismissInfoScreen!}
                        itemBounds={itemBounds}
                        nestedRailVisibleId={nestedRailVisible}
                    />
                </View>
            )}

            {/* Only one reader at a time: More-from-Az fully replaces About while open, so a
                translucent surface can't let the About doc links bleed through its cards. */}
            {showAbout && !showMoreFromAz && !!config.appRepositoryUrl && (
                <AboutOverlay
                    repoUrl={config.appRepositoryUrl}
                    settings={{ activeColor: config.activeColor, translucentBackground: config.translucentBackground }}
                    moreFromAzEnabled={config.moreFromAzEnabled}
                    moreFromAzJsonUrl={config.moreFromAzJsonUrl}
                    onDismiss={() => setShowAbout(false)}
                />
            )}

            {showMoreFromAz && (
                <MoreFromAzOverlay
                    jsonUrl={config.moreFromAzJsonUrl}
                    settings={{ activeColor: config.activeColor, translucentBackground: config.translucentBackground }}
                    onDismiss={() => setShowMoreFromAz(false)}
                />
            )}

            {/* Guidance instruction callouts are fully CLEARED while a footer screen is open. The
                engine lives in its own component so its state changes never re-render this host. */}
            {!showAbout && !showMoreFromAz && (
              <AzGuidanceLayer
                items={items}
                itemBounds={itemBounds}
                isExpanded={isExpanded}
                isFloating={isFloating}
                hostStates={hostStates}
                currentDestination={currentDestination}
                nestedRailVisible={nestedRailVisible}
                infoScreen={infoScreen}
                activeClassifiers={(props as any).activeClassifiers}
                accent={typeof config.activeColor === 'string' ? config.activeColor : undefined}
              />
            )}
        </View>
    </AzNavRailContext.Provider>
  );
};

/**
 * Isolated host for the status-driven guidance engine. Rendered as a sibling inside `AzNavRailInner`,
 * it receives the live rail state as props and owns all guidance reactivity (status polling, routing,
 * auto-advance, `autoStartWhen`) — so when those update, only this component re-renders, never the
 * rail itself. That keeps the rail's fragile item-registry effect timing untouched.
 */
const AzGuidanceLayer: React.FC<{
  items: AzNavItem[];
  itemBounds: Record<string, any>;
  isExpanded: boolean;
  isFloating: boolean;
  hostStates: Record<string, boolean>;
  currentDestination?: string;
  nestedRailVisible: string | null;
  infoScreen: boolean;
  activeClassifiers?: Set<string> | string[];
  accent?: string;
}> = ({ items, itemBounds, isExpanded, isFloating, hostStates, currentDestination, nestedRailVisible, infoScreen, activeClassifiers, accent }) => {
  const guidance = useAzGuidanceContext();
  const activeItemId = useMemo(
    () => items.find((it) => it.route && it.route === currentDestination)?.id ?? null,
    [items, currentDestination],
  );
  const builtins = useCallback(
    () => computeBuiltinStatuses({
      railExpanded: isExpanded,
      railFloating: isFloating,
      hostStates,
      currentRoute: currentDestination,
      activeItemId,
      nestedRailOpenId: nestedRailVisible,
      helpOpen: infoScreen,
    }),
    [isExpanded, isFloating, hostStates, currentDestination, activeItemId, nestedRailVisible, infoScreen],
  );
  const activeStatuses = useActiveStatuses(guidance.statusPredicates, activeClassifiers, builtins);
  const edges = useMemo(() => [...guidance.edges, ...computeAutoEdges(items)], [guidance.edges, items]);
  const frame = useMemo(
    () => routeInstructions(edges, guidance.goals, guidance.activeGoals, activeStatuses, (k) => guidance.stepIndex(k), guidance.consumedStatuses),
    [edges, guidance.goals, guidance.activeGoals, activeStatuses, guidance.stepIndex, guidance.consumedStatuses],
  );
  // Self-arming onboarding goals: `autoStartWhen` is discouraged; it honours both completion and the
  // user's skip, so a finished or dismissed tutorial never auto-restarts.
  useEffect(() => {
    Object.values(guidance.goals).forEach((goal) => {
      if (goal.autoStartWhen && activeStatuses.has(goal.autoStartWhen) &&
          !guidance.isCompleted(goal.id) && !guidance.isDismissed(goal.id) && !guidance.activeGoals.includes(goal.id)) {
        guidance.activate(goal.id);
      }
    });
  }, [activeStatuses, guidance]);
  // Auto-advance + reactive step-cursor sync; and de-dup: once a shown hop's `to` is reached, consume it
  // so that hop is never re-shown (even if the user later undoes the action).
  const pendingConsume = useRef<Set<string>>(new Set());
  useEffect(() => {
    if (!guidance.enabled) return;
    frame.reachedGoals.forEach((g) => guidance.markReached(g));
    frame.resolved.forEach((r) => {
      if (r.edge.to != null) pendingConsume.current.add(r.edge.to);
      if (r.stepTotal > 1) {
        const key = edgeStepKey(r.edge);
        if (guidance.stepIndex(key) < r.stepIndex) guidance.setStep(key, r.stepIndex);
      }
    });
    pendingConsume.current.forEach((to) => { if (activeStatuses.has(to)) guidance.consume(to); });
  }, [frame, activeStatuses, guidance]);
  // Publish the observable snapshot (while enabled) so a host can mirror it with bespoke rendering.
  const snapshots = useMemo(
    () => (guidance.enabled ? snapshotsOf(frame.resolved, itemBounds, activeItemId) : []),
    [guidance.enabled, frame, itemBounds, activeItemId],
  );
  useEffect(() => {
    guidance.publishCurrent(snapshots);
  }, [snapshots, guidance]);

  // Host-driven suppression (observed even when hidden, so it can re-show after the settle).
  const suppressed = useGuidanceSuppressed(guidance.suppressors);

  if (!guidance.enabled || suppressed) return null;
  return (
    <AzInstructionOverlay
      resolved={frame.resolved}
      itemBounds={itemBounds}
      accent={accent}
      activeItemId={activeItemId}
      targets={guidance.targets}
      onAdvance={(k) => guidance.advance(k)}
      onSkip={() => guidance.skip()}
      renderSlot={guidance.renderer}
    />
  );
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
export const AzNavRail: React.FC<AzNavRailProps> = (props) => {
  return (
    <AzGuidanceProvider>
      <AzNavRailInner {...props} />
    </AzGuidanceProvider>
  );
};

const styles = StyleSheet.create({
  railContainer: {
    backgroundColor: '#f0f0f0',
    overflow: 'hidden',
    borderColor: '#ddd',
  },
  header: {
    alignItems: 'center',
    padding: AzNavRailDefaults.HeaderPadding,
  },
  appName: {
    fontWeight: 'bold',
    fontSize: 18,
  },
  railContent: {
    paddingHorizontal: AzNavRailDefaults.RailContentHorizontalPadding,
    paddingVertical: 8,
    alignItems: 'center',
  },
  menuContent: {
    flex: 1,
  },
  menuItem: {
      padding: 16,
  },
  menuItemText: {
      fontSize: 16,
  },
  divider: {
      // backgroundColor is applied inline from `activeColor` (or the footer's accent) so the
      // divider matches the font next to it — never a muted line.
      height: 1,
      marginVertical: 8,
  },
  footer: {
      padding: 16,
      borderTopWidth: 1,
      borderTopColor: '#ccc',
  },
  loaderOverlay: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(255,255,255,0.5)',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 10000,
      elevation: 10,
  },
  infoOverlay: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(0,0,0,0.85)',
      zIndex: 20000,
      paddingTop: 40,
  },
  infoTitle: {
      fontSize: 24,
      fontWeight: 'bold',
      color: 'white',
      marginBottom: 20,
      textAlign: 'center',
  },
  infoItem: {
      marginBottom: 16,
      borderBottomWidth: 1,
      borderBottomColor: '#555',
      paddingBottom: 8,
  },
  infoItemTitle: {
      fontWeight: 'bold',
      color: '#4fc3f7',
      fontSize: 16,
      marginBottom: 4,
  },
  infoItemText: {
      color: '#eee',
      fontSize: 14,
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
      borderColor: '#666',
  }
});
