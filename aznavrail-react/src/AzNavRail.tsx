import React, {
  useState,
  useCallback,
  useRef,
  useEffect,
  useMemo,
} from 'react';
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
import {
  AzNavItem,
  AzNavRailSettings,
  AzButtonShape,
  AzDockingSide,
  AzHeaderIconShape,
  AzNestedRailAlignment,
  AzEntrance,
  AzExit,
} from './types';
import { AzKineticItem, useAzClosing } from './components/AzKinetics';
import { Easing as RNEasing } from 'react-native';
import { AzEasing } from './types';
import { AzNavRailDefaults, AzMotion } from './AzNavRailDefaults';
import { AzButton } from './components/AzButton';
import { AzFooterLabel } from './components/AzFooterLabel';
import { AzAboutSurface, useAzAboutOwnership } from './services/aboutPresence';
import { useAzAboutWarmup } from './services/aboutPrefetch';
import { AzToggle } from './components/AzToggle';
import { AzCycler } from './components/AzCycler';
import { RailMenuItem } from './components/RailMenuItem';
import { AzLoad } from './components/AzLoad';
import { AzRailSliderItem } from './components/AzRailSliderItem';
import { DraggableRailItemWrapper } from './components/DraggableRailItemWrapper';
import { RelocItemHandler } from './util/RelocItemHandler';
import { AzNestedRailPopup } from './components/AzNestedRailPopup';
import { HelpOverlay } from './components/HelpOverlay';
import { AboutOverlay } from './components/AboutOverlay';
import {
  AzRailPaletteContext,
  resolveRailAccent,
  usePublishRailPalette,
  AZ_ACCENT_FALLBACK,
} from './AzRailPalette';
import { MoreFromAzOverlay } from './components/MoreFromAzOverlay';
import {
  AzGuidanceProvider,
  useAzGuidanceContext,
} from './guidance/AzGuidanceController';
import {
  useActiveStatuses,
  computeBuiltinStatuses,
  useGuidanceSuppressed,
} from './guidance/AzStatusEngine';
import {
  routeInstructions,
  computeAutoEdges,
  snapshotsOf,
  edgeStepKey,
} from './guidance/AzGuidance';
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
  /**
   * Controls the rail's expanded/collapsed state directly, mirroring `AzDropdownMenu`'s
   * controlled/uncontrolled `expanded` pattern. When set (including `false`), the rail is fully
   * controlled by the caller — every internal toggle path (header tap, swipe, outside-tap
   * dismiss, an item's `collapseOnClick`, etc.) still calls `onExpandedChange` so the caller can
   * react, but this component's own state no longer drives what's rendered. Omit (default) for
   * today's uncontrolled behavior, seeded once by `initiallyExpanded`.
   */
  expanded?: boolean;
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
        ...({ transformOrigin: 'top center' } as any),
      }}
    >
      {children}
    </Animated.View>
  );
};

/** True when `classifiers` overlaps `set` (which may be a Set or a plain array). */
function classifierHit(
  classifiers: Set<string> | string[] | undefined,
  set: Set<string> | string[] | undefined
): boolean {
  if (!classifiers || !set) return false;
  const haystack = Array.isArray(set) ? new Set(set) : set;
  const list = Array.isArray(classifiers)
    ? classifiers
    : Array.from(classifiers);
  return list.some((c) => haystack.has(c));
}

/**
 * The colour a rail item is drawn in, resolving the four highlights.
 *
 * They answer four different questions — **active** ("where am I?"), **focus** ("what am I
 * touching?"), **secondary** ("whatever the app decides") and **tertiary** (a second app-driven
 * channel) — and they outrank each other in that order reversed: a press is the most immediate
 * thing happening, so focus wins for as long as it lasts, then active, then secondary, then
 * tertiary. An item with none of them lit wears its own `color`.
 */
export function resolveHighlight(
  item: AzNavItem,
  cfg: any,
  currentDestination?: string,
  lastTappedId?: string | null
): string | undefined {
  const activeColor = (item as any).activeColor ?? cfg.activeColor;
  const secondaryColor =
    (item as any).secondaryColor ?? cfg.secondaryColor ?? activeColor;
  const tertiaryColor =
    (item as any).tertiaryColor ?? cfg.tertiaryColor ?? activeColor;

  const isActive =
    !!item.isChecked ||
    item.id === currentDestination ||
    (!!item.route && item.route === currentDestination) ||
    classifierHit(item.classifiers, cfg.activeClassifiers) ||
    (!item.route && !!lastTappedId && lastTappedId === item.id);
  const isSecondary =
    !!(item as any).secondary ||
    classifierHit(item.classifiers, cfg.secondaryClassifiers);
  const isTertiary =
    !!(item as any).tertiary ||
    classifierHit(item.classifiers, cfg.tertiaryClassifiers);

  if (isActive && activeColor) return activeColor;
  if (isSecondary && secondaryColor) return secondaryColor;
  if (isTertiary && tertiaryColor) return tertiaryColor;
  return item.color;
}

const AzNavRailInner: React.FC<AzNavRailProps> = (props) => {
  const {
    children,
    navController: _navController,
    currentDestination,
    initiallyExpanded = false,
    expanded: controlledExpanded,
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
    // The three highlights' colours and classifier sets. Left undestructured here, these silently
    // never reached `resolveHighlight` — every item rendered with only the ACTIVE highlight ever
    // able to fire, however a developer configured the rail.
    focusColor,
    secondaryColor,
    tertiaryColor,
    activeClassifiers,
    secondaryClassifiers,
    tertiaryClassifiers,
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
    aboutRailItem = true,
    dedupeAbout = true,
  } = props;
  const logInteraction = useCallback(
    (action: string, details?: string, item?: AzNavItem) => {
      if (onInteraction) {
        onInteraction(action, details, item);
        return;
      }

      if (typeof __DEV__ !== 'undefined' && __DEV__) {
        const suffix = details ? ` ${details}` : '';

        console.log(`[AzNavRail] ${action}${suffix}`);
      }
    },
    [onInteraction]
  );
  const [items, setItems] = useState<AzNavItem[]>([]);
  const [dslOverrides, setDslOverrides] = useState<Partial<AzNavRailSettings>>(
    {}
  );

  const config = {
    displayAppNameInHeader:
      dslOverrides.displayAppNameInHeader ?? displayAppNameInHeader,
    packRailButtons: dslOverrides.packRailButtons ?? props.packRailButtons,
    expandedRailWidth: dslOverrides.expandedRailWidth ?? expandedRailWidth,
    collapsedRailWidth: dslOverrides.collapsedRailWidth ?? collapsedRailWidth,
    railItemWidth: dslOverrides.railItemWidth ?? props.railItemWidth,
    showFooter: dslOverrides.showFooter ?? showFooter,
    isLoading: dslOverrides.isLoading ?? isLoading,
    defaultShape: dslOverrides.defaultShape ?? defaultShape,
    enableRailDragging: dslOverrides.enableRailDragging ?? enableRailDragging,
    dockingSide: dslOverrides.dockingSide ?? dockingSide,
    noMenu: dslOverrides.noMenu ?? noMenu,
    headerIconSize: dslOverrides.headerIconSize ?? headerIconSize,
    infoScreen: dslOverrides.infoScreen ?? infoScreen,
    activeColor: dslOverrides.activeColor ?? activeColor,
    focusColor: dslOverrides.focusColor ?? focusColor,
    secondaryColor: dslOverrides.secondaryColor ?? secondaryColor,
    tertiaryColor: dslOverrides.tertiaryColor ?? tertiaryColor,
    activeClassifiers: dslOverrides.activeClassifiers ?? activeClassifiers,
    secondaryClassifiers:
      dslOverrides.secondaryClassifiers ?? secondaryClassifiers,
    tertiaryClassifiers:
      dslOverrides.tertiaryClassifiers ?? tertiaryClassifiers,
    headerIconShape: dslOverrides.headerIconShape ?? headerIconShape,
    translucentBackground:
      dslOverrides.translucentBackground ?? translucentBackground,
    vibrate: dslOverrides.vibrate ?? vibrate,
    onItemGloballyPositioned: dslOverrides.onItemGloballyPositioned,
    helpList: dslOverrides.helpList ?? helpList,
    appRepositoryUrl:
      (dslOverrides as any).appRepositoryUrl ?? appRepositoryUrl,
    inAppAbout: dslOverrides.inAppAbout ?? inAppAbout,
    moreFromAzEnabled: dslOverrides.moreFromAzEnabled ?? moreFromAzEnabled,
    moreFromAzJsonUrl: dslOverrides.moreFromAzJsonUrl ?? moreFromAzJsonUrl,
    moreRailItem: dslOverrides.moreRailItem ?? moreRailItem,
    aboutRailItem: dslOverrides.aboutRailItem ?? aboutRailItem,
    dedupeAbout: dslOverrides.dedupeAbout ?? dedupeAbout,
  };

  // Controlled/uncontrolled `expanded`, mirroring `AzDropdownMenu`'s `expanded ?? internalOpen`
  // pattern: `controlledExpanded === undefined` is the (default) uncontrolled path — identical to
  // today's behavior — and any other value hands the rail's rendered state to the caller, while
  // every internal toggle path still notifies via `onExpandedChange` so a controlling caller can
  // react.
  const [internalExpanded, setInternalExpanded] = useState(
    initiallyExpanded && !config.noMenu
  );
  const isExpanded =
    controlledExpanded !== undefined ? controlledExpanded : internalExpanded;
  const setIsExpanded = useCallback(
    (value: boolean) => {
      if (controlledExpanded === undefined) setInternalExpanded(value);
      onExpandedChange?.(value);
    },
    [controlledExpanded, onExpandedChange]
  );
  const [showAbout, setShowAbout] = useState(false);
  const [showMoreFromAz, setShowMoreFromAz] = useState(false);
  const [isFloating, setIsFloating] = useState(false);
  const [showFloatingButtons, setShowFloatingButtons] = useState(false);
  const [headerHeight, setHeaderHeight] = useState(0);
  const [hostStates, setHostStates] = useState<Record<string, boolean>>({});
  const [nestedRailVisible, setNestedRailVisible] = useState<string | null>(
    null
  );
  const [anchorPosition, setAnchorPosition] = useState<
    { x: number; y: number; width: number; height: number } | undefined
  >(undefined);
  const [itemBounds, setItemBounds] = useState<Record<string, any>>({});
  // The secret-location easter egg (nine long-presses on the credit line) is wired but has no
  // payoff yet. These two reads keep the state honest without rendering anything: they used to be
  // `{flag && <View />} {/* Silence unused var */}` inside the content View, and the stray space
  // before the comment rendered a bare " " text node — an invariant violation that crashes a real
  // React Native app, and which a hand-rolled `react-native` test mock had been hiding.
  const [, setSecLocClicks] = useState(0);
  const [, setSecLocVisible] = useState(false);

  const handleItemLayout = useCallback(
    (id: string, e: any) => {
      const target = e.target as any;
      if (target) {
        UIManager.measureInWindow(
          target,
          (x: number, y: number, width: number, height: number) => {
            if (
              x !== undefined &&
              y !== undefined &&
              width !== undefined &&
              height !== undefined
            ) {
              const bounds = { x, y, width, height };
              setItemBounds((prev) => ({ ...prev, [id]: bounds }));
              if (config.onItemGloballyPositioned)
                config.onItemGloballyPositioned(id, bounds);
            } else {
              const bounds = { ...e.nativeEvent.layout };
              setItemBounds((prev) => ({ ...prev, [id]: bounds }));
              if (config.onItemGloballyPositioned)
                config.onItemGloballyPositioned(id, bounds);
            }
          }
        );
      } else {
        const bounds = { ...e.nativeEvent.layout };
        setItemBounds((prev) => ({ ...prev, [id]: bounds }));
        if (config.onItemGloballyPositioned)
          config.onItemGloballyPositioned(id, bounds);
      }
    },
    [config]
  );

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

  // id -> item lookup, used to find a sub-item's host (e.g. to tell whether a tapped child belongs
  // to a nested-rail popup rather than an inline host expansion).
  const itemById = useMemo(() => {
    const map: Record<string, AzNavItem> = {};
    items.forEach((item) => {
      map[item.id] = item;
    });
    return map;
  }, [items]);

  // Feature 1 (reflectSelectionInParent): which child is currently "the selection" for a
  // nested-rail host, keyed by host id. Owned by the rail itself so it survives DSL re-declaration
  // the same way `hostStates`/cycler state does — the DSL re-runs registration on every render, but
  // this state lives above that and is never cleared by it.
  const [selectedChildByHost, setSelectedChildByHost] = useState<
    Record<string, string>
  >({});
  const selectedChildSeenRef = useRef<Record<string, boolean>>({});

  const railWidthAnim = useRef(
    new Animated.Value(
      initiallyExpanded ? expandedRailWidth : collapsedRailWidth
    )
  ).current;
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
  const expandedHostKey = Object.keys(hostStates)
    .filter((k) => hostStates[k])
    .sort()
    .join(',');
  const structuralKey =
    items.map((i) => i.id).join(',') + '|' + expandedHostKey;
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
    // Feature 4.3: nested rails reuse the identical rising/falling-edge machinery, but drive the
    // single `nestedRailVisible` id rather than a per-host boolean map, so at most one transition
    // is applied per pass (the same "user wins" contract still holds per-item via
    // `expandWhenSeenRef`, which is keyed by item id and shared with the host loop below — ids are
    // unique across all items, so hosts and nested rails never collide in it).
    let nestedRailAction: { open: boolean; id: string } | null = null;
    itemsRef.current.forEach((item) => {
      if (item.isNestedRail) {
        if (typeof item.expandWhen === 'function') {
          const now = !!item.expandWhen();
          const before = expandWhenSeenRef.current[item.id];
          if (before === undefined) {
            if (now) nestedRailAction = { open: true, id: item.id };
          } else if (before !== now) {
            nestedRailAction = { open: now, id: item.id };
          }
          expandWhenSeenRef.current[item.id] = now;
        }
        return;
      }
      if (!item.isHost) return;
      // Per-host initiallyExpanded: rising-edge auto-expand, once.
      if (
        item.initiallyExpanded &&
        initiallyExpandedSeenRef.current[item.id] !== true
      ) {
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
    if (changed) {
      setHostStates((prev) => {
        let differs = false;
        for (const k in updates)
          if (prev[k] !== updates[k]) {
            differs = true;
            break;
          }
        return differs ? { ...prev, ...updates } : prev;
      });
    }
    if (nestedRailAction) {
      const action = nestedRailAction as { open: boolean; id: string };
      if (action.open) {
        setNestedRailVisible(action.id);
      } else {
        setNestedRailVisible((prev) => (prev === action.id ? null : prev));
      }
    }
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

  // Feature 1 (reflectSelectionInParent): seed `selectedChildByHost` from a developer-supplied
  // `item.selectedChildId` on first appearance only — the same rising-edge-once semantics as
  // `initiallyExpanded`, so a later DSL re-render with a different `selectedChildId` doesn't fight
  // a user's own subsequent tap.
  useEffect(() => {
    items.forEach((item) => {
      if (
        item.isNestedRail &&
        item.reflectSelectionInParent &&
        item.selectedChildId
      ) {
        if (selectedChildSeenRef.current[item.id] !== true) {
          selectedChildSeenRef.current[item.id] = true;
          const seedId = item.selectedChildId;
          setSelectedChildByHost((prev) =>
            prev[item.id] ? prev : { ...prev, [item.id]: seedId }
          );
        }
      }
    });
  }, [items]);

  // Status-driven guidance runs in its own isolated <AzGuidanceLayer> (rendered below) so its engine
  // state never re-renders this host — keeping the rail's item-registry effect timing untouched.

  useEffect(() => {
    const id = pan.addListener((value) => {
      panValue.current = value;
    });
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
    setDslOverrides((prev) => ({ ...prev, ...newSettings }));
  }, []);

  const dividerCounter = useRef(0);
  const getDividerId = useCallback(() => {
    const currentItems = itemsRef.current || [];
    return `divider_${currentItems.length + dividerCounter.current++}`;
  }, []);

  const hasItem = useCallback((id: string) => {
    return (itemsRef.current || []).some((i) => i.id === id);
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
      toValue: isExpanded
        ? config.expandedRailWidth
        : config.collapsedRailWidth,
      duration: 300,
      useNativeDriver: false,
    }).start();
  }, [isExpanded, config.expandedRailWidth, config.collapsedRailWidth]);

  // Every subsequent change to `isExpanded` is now notified directly from `setIsExpanded` at each
  // toggle site (required so a CONTROLLED caller is notified even when the derived `isExpanded`
  // doesn't itself change until the caller reacts). This mount-only effect preserves the original
  // uncontrolled behavior of also notifying once with the initial value.
  useEffect(() => {
    onExpandedChange?.(isExpanded);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    items.forEach((item) => {
      if (!itemOffsets.current[item.id]) {
        itemOffsets.current[item.id] = new Animated.Value(0);
      }
    });
  }, [items]);

  // Reloc Item Logic
  const handleRelocDragStart = (draggedItemIndex: number) => {
    if (vibrate) Vibration.vibrate(50);
    logInteraction(
      'Reloc drag started',
      items[draggedItemIndex].text,
      items[draggedItemIndex]
    );
  };

  const handleRelocDragEnd = (_draggedItemIndex: number) => {
    Object.values(itemOffsets.current).forEach((anim) => anim.setValue(0));
    logInteraction('Reloc drag ended');
  };

  const handleRelocDragMove = (dy: number, draggedItemIndex: number) => {
    const draggedItem = items[draggedItemIndex];
    if (!draggedItem.isRelocItem || !draggedItem.hostId) return;

    const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId);
    const itemHeight = 48 + 8;

    const draggedClusterIndex = cluster.findIndex(
      (i) => i.id === draggedItem.id
    );
    const targetClusterIndex = RelocItemHandler.calculateTargetIndex(
      dy,
      draggedClusterIndex,
      cluster.length,
      itemHeight
    );

    // Animation logic omitted for brevity, but same as before
    cluster.forEach((item, index) => {
      if (item.id === draggedItem.id) return;
      let offset = 0;
      if (draggedClusterIndex < targetClusterIndex) {
        if (index > draggedClusterIndex && index <= targetClusterIndex)
          offset = -itemHeight;
      } else if (draggedClusterIndex > targetClusterIndex) {
        if (index >= targetClusterIndex && index < draggedClusterIndex)
          offset = itemHeight;
      }
      Animated.spring(itemOffsets.current[item.id], {
        toValue: offset,
        useNativeDriver: false,
      }).start();
    });

    (panValue as any).targetIndex = targetClusterIndex;
  };

  const handleRelocDrop = (draggedItemIndex: number) => {
    const draggedItem = items[draggedItemIndex];
    const cluster = RelocItemHandler.getCluster(items, draggedItem.hostId!);
    const draggedClusterIndex = cluster.findIndex(
      (i) => i.id === draggedItem.id
    );
    const targetClusterIndex =
      (panValue as any).targetIndex ?? draggedClusterIndex;

    if (draggedClusterIndex !== targetClusterIndex) {
      const newItems = RelocItemHandler.reorderItems(
        items,
        draggedItem.id,
        draggedItem.hostId!,
        targetClusterIndex
      );
      setItems(newItems);
      const newCluster = RelocItemHandler.getCluster(
        newItems,
        draggedItem.hostId!
      );
      const newOrder = newCluster.map((i) => i.id);
      if (draggedItem.onRelocate) {
        draggedItem.onRelocate(
          draggedClusterIndex,
          targetClusterIndex,
          newOrder
        );
      }
    }
    delete (panValue as any).targetIndex;
    handleRelocDragEnd(draggedItemIndex);
  };

  const adjustFloatingRailWithinBounds = (
    currentX: number,
    currentY: number
  ) => {
    const screenHeight = screenHeightRef.current;
    const railItemsCountForDrag = itemsRef.current.filter(
      (i) => i.isRailItem && !i.isSubItem
    ).length;
    const contentHeight = headerHeight + railItemsCountForDrag * 56 + 16;
    const bottomY = currentY + contentHeight;
    const limitY = screenHeight * 0.9;
    if (bottomY > limitY) {
      const shift = bottomY - limitY;
      const newY = currentY - shift;
      Animated.timing(pan, {
        toValue: { x: currentX, y: newY },
        duration: 200,
        useNativeDriver: false,
      }).start();
    }
  };

  const panResponder = useRef(
    PanResponder.create({
      onMoveShouldSetPanResponder: (_, gestureState) => {
        if (!enableRailDraggingRef.current && !isFloatingRef.current)
          return false;
        const { dx, dy } = gestureState;
        if (
          !isFloatingRef.current &&
          Math.abs(dy) > Math.abs(dx) &&
          Math.abs(dy) > 10 &&
          enableRailDraggingRef.current
        ) {
          return true;
        }
        return isFloatingRef.current;
      },
      onPanResponderGrant: () => {
        if (isFloatingRef.current) {
          pan.setOffset({ x: panValue.current.x, y: panValue.current.y });
          pan.setValue({ x: 0, y: 0 });
          dragStartPos.current = {
            x: panValue.current.x,
            y: panValue.current.y,
          };
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
          const distance = Math.sqrt(
            Math.pow(currentX, 2) + Math.pow(currentY, 2)
          );
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
    // Reaching for the rail means the user is done reading; the reader must never be the thing
    // standing between them and the app icon.
    setShowAbout(false);
    setShowMoreFromAz(false);
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

  const isVerticalNestedRailOpen = useMemo(() => {
    if (!nestedRailVisible) return false;
    const openItem = items.find((i) => i.id === nestedRailVisible);
    return openItem?.nestedRailAlignment === AzNestedRailAlignment.VERTICAL;
  }, [nestedRailVisible, items]);

  const baseButtonSize = config.railItemWidth || AzNavRailDefaults.ButtonWidth;
  const calculatedShrunkSize = baseButtonSize * 0.75;
  const actualShrunkSize = Math.max(calculatedShrunkSize, 35);
  const targetShrunkSize = Math.min(baseButtonSize, actualShrunkSize);
  const activeButtonSize = isVerticalNestedRailOpen
    ? targetShrunkSize
    : baseButtonSize;
  const sizeRatio = activeButtonSize / baseButtonSize;

  /** Opens the About reader (or the repo in a browser), and closes it again on a second tap. */
  const toggleAbout = () => {
    if (config.inAppAbout) {
      setShowAbout((prev) => !prev);
    } else if (config.appRepositoryUrl) {
      // Only follow safe web URLs — on react-native-web a `javascript:` URL would otherwise execute.
      const isSafe =
        config.appRepositoryUrl.startsWith('http://') ||
        config.appRepositoryUrl.startsWith('https://');
      if (isSafe) {
        Linking.openURL(config.appRepositoryUrl).catch((e) =>
          console.error('Could not open About', e)
        );
      }
    }
  };

  /**
   * Leaves the About / More-from-Az reader. Any other interaction with the rail means the user is
   * done reading — a full-screen reader you can only escape through its own close button is a room
   * with a keyhole for a door.
   */
  const dismissFooterScreens = () => {
    setShowAbout(false);
    setShowMoreFromAz(false);
  };

  // The colour this rail actually reads as, published to every other AzNavRail surface — a second
  // (floating) rail, a drop-down, the About reader, the Help overlay. Chrome belonging to the same
  // navigation system has to look like it.
  // The last item the user tapped, which is what the FOCUS highlight follows for items that carry
  // no route of their own.
  const [lastTappedId, setLastTappedId] = useState<string | null>(null);

  /**
   * Known pre-existing gap fixed as part of Feature 1: neither the popup nor a tapped child ever
   * closed the popup on its own — only a backdrop tap did. Called whenever a non-host child of a
   * nested rail is tapped (from any of `renderRailItem`'s item-kind branches): records the tap as
   * that host's `selectedChildId` when `reflectSelectionInParent` is on, then closes the popup
   * unless the host opted into `keepNestedRailOpen`.
   */
  const recordNestedRailSelection = useCallback(
    (host: AzNavItem, child: AzNavItem) => {
      if (host.reflectSelectionInParent && !child.isHost) {
        setSelectedChildByHost((prev) =>
          prev[host.id] === child.id ? prev : { ...prev, [host.id]: child.id }
        );
      }
      if (!host.keepNestedRailOpen) {
        setNestedRailVisible(null);
      }
    },
    []
  );

  const railAccent = useMemo(
    () => resolveRailAccent(config.activeColor, items),
    [config.activeColor, items]
  );
  const railPalette = useMemo(
    () => ({ accent: railAccent, surface: config.translucentBackground }),
    [railAccent, config.translucentBackground]
  );

  // Who draws About. Every surface that could offer one registers itself; only the highest-ranked
  // registration actually draws, so an app with a rail AND a drop-down doesn't show About three
  // times over. A developer-declared `?` outranks everything and is never suppressed — it only
  // suppresses others — which is why its ownership result is ignored.
  const dedupeAboutEnabled = config.dedupeAbout !== false;
  const hasExplicitAboutItem = items.some((i) => i.isAboutItem);
  useAzAboutOwnership(
    AzAboutSurface.RAIL_ITEM_EXPLICIT,
    hasExplicitAboutItem,
    dedupeAboutEnabled
  );
  const footerOwnsAbout = useAzAboutOwnership(
    AzAboutSurface.RAIL_MENU_FOOTER,
    !config.noMenu && config.showFooter !== false,
    dedupeAboutEnabled
  );
  const autoAboutOwned = useAzAboutOwnership(
    AzAboutSurface.RAIL_ITEM_AUTO,
    config.aboutRailItem !== false && !hasExplicitAboutItem,
    dedupeAboutEnabled
  );

  // Warm the About reader's content now, in the background, so opening it shows the page rather
  // than a spinner. Cheap: cached, conditional requests that usually come back 304.
  useAzAboutWarmup(
    config.inAppAbout === false ? undefined : config.appRepositoryUrl,
    config.moreFromAzEnabled !== false,
    config.moreFromAzJsonUrl
  );
  // Context reaches this rail's own children; the published palette reaches its siblings — a second
  // floating rail, or anything the host draws outside `<AzNavRail>`.
  usePublishRailPalette(railPalette);

  const renderRailItem = (
    item: AzNavItem,
    _index: number,
    overrideConfig: any = config,
    ancestors: Set<string> = new Set()
  ): React.ReactNode => {
    // Cycle guard: unlike the Kotlin DSL (which requires a host be declared before its sub-items,
    // forming a DAG), the React DSL has no ordering guarantee, so a cyclic `hostId` chain
    // (A -> B -> A, or a self-reference) is possible. Bail before recursing into an ancestor to
    // avoid "Maximum call stack size exceeded".
    if (ancestors.has(item.id)) return null;
    const isExpandedHost = hostStates[item.id] || false;
    const subItems = subItemsMap[item.id] || [];
    const isRect = item.shape === AzButtonShape.RECTANGLE;
    // Feature 1: this item's host, when it is a non-host child of a nested rail (i.e. rendered
    // either as a top-level item's own popup content, or — since `renderRailItem` is reused
    // verbatim inside `AzNestedRailPopup`'s `renderItem` — as the popup's own child rows).
    const hostItem = item.hostId ? itemById[item.hostId] : undefined;
    const isNestedRailChild = !!(hostItem && hostItem.isNestedRail);
    const commonProps = {
      // A rail item's declared id is its identity everywhere else in this library, so it is its
      // identity on screen too: consumers (and these tests) can address any item by the id they
      // gave it, without knowing what it renders as.
      testID: item.id,
      // The three highlights, in the order they outrank each other. Focus is about the gesture
      // happening right now; active is about where the user is; secondary is whatever the app says.
      color: resolveHighlight(
        item,
        overrideConfig,
        currentDestination,
        lastTappedId
      ),
      shape: item.shape || overrideConfig.defaultShape,
      enabled: !item.disabled,
      style: {
        marginBottom: isRect
          ? 2
          : (config.packRailButtons
              ? 0
              : AzNavRailDefaults.RailContentVerticalArrangement) * sizeRatio,
      },
      size: activeButtonSize,
      badge: item.badge,
      persistentBadge: item.persistentBadge,
    };

    // A slider item unfolds where it stands. Nothing opens over the rail and nothing moves the
    // user elsewhere: the slot the item occupies grows into the track, and folds back when the
    // value is set. The control ends up exactly where the user's attention already was.
    if (item.isSlider) {
      return (
        <AzRailSliderItem
          key={item.id}
          item={item}
          buttonSize={activeButtonSize}
          enabled={!item.disabled}
          color={item.color ?? railAccent ?? AzNavRailDefaults.AccentFallback}
        />
      );
    }

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
            translucentBackgroundColor={item.translucentBackgroundColor}
            text={item.text}
            content={item.content}
            hasCustomContent={!!item.content}
            onClick={() => {
              const newHostState = !(hostStates[item.id] || false);
              setHostStates((prev) => ({ ...prev, [item.id]: newHostState }));
              logInteraction('Host toggled', item.text, item);
              item.onExpandedChange?.(newHostState);
            }}
          />
          {isExpandedHost &&
            subItems
              .filter((sub) => sub.isRailItem)
              .map((sub) =>
                renderRailItem(
                  sub,
                  items.indexOf(sub),
                  overrideConfig,
                  new Set(ancestors).add(item.id)
                )
              )}
        </View>
      );
    }

    if (item.isCycler) {
      return (
        <AzCycler
          key={item.id}
          {...commonProps}
          fillColor={item.fillColor}
          translucentBackgroundColor={item.translucentBackgroundColor}
          options={item.options || []}
          selectedOption={item.selectedOption || ''}
          onCycle={() => {
            if (item.onClick) item.onClick();
            setLastTappedId(item.id);
            logInteraction('Cycler cycled', item.text, item);
            if (isNestedRailChild && hostItem)
              recordNestedRailSelection(hostItem, item);
          }}
        />
      );
    }
    if (item.isToggle) {
      return (
        <AzToggle
          key={item.id}
          {...commonProps}
          fillColor={item.fillColor}
          translucentBackgroundColor={item.translucentBackgroundColor}
          isChecked={item.isChecked || false}
          toggleOnText={item.toggleOnText}
          toggleOffText={item.toggleOffText}
          onToggle={() => {
            if (item.onClick) item.onClick();
            setLastTappedId(item.id);
            logInteraction('Toggle toggled', item.text, item);
            if (isNestedRailChild && hostItem)
              recordNestedRailSelection(hostItem, item);
          }}
        />
      );
    }

    // Feature 1: when this nested-rail host has `reflectSelectionInParent`, its button mirrors the
    // currently selected child's text/content, a tap invokes that child directly (the popup does
    // NOT open), and a long-press opens the popup instead — the shape/color/fillColor above are
    // still resolved from the HOST item's own `commonProps`/`fillColor`, unaffected.
    const reflectSelection = !!(
      item.isNestedRail && item.reflectSelectionInParent
    );
    const nestedRailNonHostChildren = reflectSelection
      ? subItems.filter((sub) => !sub.isHost)
      : [];
    const selectedChildIdForHost = selectedChildByHost[item.id];
    const selectedChild = reflectSelection
      ? (selectedChildIdForHost &&
          nestedRailNonHostChildren.find(
            (c) => c.id === selectedChildIdForHost
          )) ||
        nestedRailNonHostChildren[0]
      : undefined;

    const openNestedRailPopup = () => {
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
          height: 48,
        });
      }
    };

    return (
      <View
        key={item.id}
        onLayout={
          !isFloating || config.infoScreen || item.isNestedRail
            ? (e) => handleItemLayout(item.id, e)
            : undefined
        }
      >
        <AzButton
          {...commonProps}
          fillColor={item.fillColor}
          translucentBackgroundColor={item.translucentBackgroundColor}
          text={
            reflectSelection && selectedChild ? selectedChild.text : item.text
          }
          content={
            reflectSelection && selectedChild
              ? (selectedChild.content ?? item.content)
              : item.content
          }
          hasCustomContent={
            !!(reflectSelection && selectedChild
              ? (selectedChild.content ?? item.content)
              : item.content)
          }
          onClick={() => {
            if (reflectSelection) {
              // Invoke the selected child's action directly, exactly as if the user had tapped
              // it inside the (closed) popup — the popup itself must NOT open.
              if (selectedChild) {
                setLastTappedId(selectedChild.id);
                logInteraction(
                  'Item clicked',
                  selectedChild.text,
                  selectedChild
                );
                dismissFooterScreens();
                if (selectedChild.onClick) selectedChild.onClick();
                if (selectedChild.collapseOnClick && !config.noMenu)
                  setIsExpanded(false);
              }
            } else {
              setLastTappedId(item.id);
              logInteraction('Item clicked', item.text, item);
              // Reaching for any other rail item is the user leaving the About reader; only the
              // About item itself toggles it.
              if (item.isAboutItem) {
                toggleAbout();
              } else {
                dismissFooterScreens();
              }
              if (item.isNestedRail) {
                openNestedRailPopup();
              }
              if (item.onClick) item.onClick();
              if (item.collapseOnClick && !config.noMenu) setIsExpanded(false);
            }
            // Known pre-existing gap fixed by Feature 1: tapping a child inside an open nested
            // rail's popup now closes the popup (unless the host set `keepNestedRailOpen`), and
            // records the selection when the host reflects it. Never fires for the host's own
            // tap above (`item.hostId` is unset for the host itself).
            if (isNestedRailChild && hostItem) {
              recordNestedRailSelection(hostItem, item);
            }
          }}
          onLongPress={reflectSelection ? openNestedRailPopup : undefined}
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
    ancestors: Set<string> = new Set()
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
          onLayout={
            config.infoScreen || item.isHost
              ? (e) => handleItemLayout(item.id, e)
              : undefined
          }
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
              setHostStates((prev) => ({ ...prev, [item.id]: newHostState }));
              item.onExpandedChange?.(newHostState);
            }}
            onItemClick={() => {
              logInteraction('Menu item clicked', item.text, item);
              // Same contract as the rail strip: acting on the menu means the user is done with
              // the About / More-from-Az reader, so it gets out of the way.
              if (item.isAboutItem) {
                toggleAbout();
              } else {
                dismissFooterScreens();
                if (item.onClick) item.onClick();
              }
              if (item.collapseOnClick) setIsExpanded(false);
            }}
            renderSubItems={() => (
              <>
                {subItems.map((subItem, i) =>
                  renderMenuItem(
                    subItem,
                    i,
                    subItems.length,
                    visible,
                    depth + 1,
                    new Set(ancestors).add(item.id)
                  )
                )}
              </>
            )}
          />
        </View>
      </AzKineticItem>
    );
  };

  const renderFooter = () => {
    // The rail's own accent — `activeColor` (DSL overrides included) when set, else the colour the
    // rail's items are drawn in. The footer belongs to the rail, so it looks like the rail.
    const footerColor = railAccent || AZ_ACCENT_FALLBACK;

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
        const isSafe =
          config.appRepositoryUrl.startsWith('http://') ||
          config.appRepositoryUrl.startsWith('https://');
        if (isSafe) {
          Linking.openURL(config.appRepositoryUrl).catch((e) =>
            console.error('Could not open About', e)
          );
        }
      }
    };

    const handleFeedback = () => {
      Linking.openURL(
        'mailto:hereliesaz@gmail.com?subject=Feedback for AzNavRail'
      ).catch((e) => console.error('Could not open Mail', e));
    };

    const handleCredit = () => {
      Linking.openURL('https://instagram.com/HereLiesAz').catch((e) =>
        console.error('Could not open Credit', e)
      );
    };

    const handleSecLocTrigger = () => {
      setSecLocClicks((prev) => {
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
            <AzFooterLabel
              text="Undock"
              color={footerColor}
              bold
              onPress={handleUndock}
              style={{ paddingVertical: 8 }}
            />
          )}
          {!!config.appRepositoryUrl && footerOwnsAbout && (
            <AzFooterLabel
              text="About"
              color={footerColor}
              onPress={handleAbout}
            />
          )}
          <AzFooterLabel
            text="Feedback"
            color={footerColor}
            onPress={handleFeedback}
          />
          <AzFooterLabel
            text="@HereLiesAz"
            color={footerColor}
            onPress={handleCredit}
            onLongPress={handleSecLocTrigger}
          />
        </View>
      </FooterAccordion>
    );
  };

  const effectiveRailItems = useMemo(() => {
    // Only the top-level items are listed here; `renderRailItem`'s host branch renders each
    // expanded host's sub-items inline and recurses for sub-hosts, so hosts nest to any depth
    // without this list having to be flattened (which would otherwise double-render sub-items).
    const strip = items.filter(
      (i) => !i.isSubItem && (config.noMenu || i.isRailItem)
    );
    // The rail strip ends with the About ("?") button — unless the developer declared their own
    // with `AzAboutRailItem`, in which case theirs stands where they put it. It persists; it is
    // not fixed. The drawer does not need one: its footer already carries About.
    if (!autoAboutOwned) return strip;
    return [
      ...strip,
      {
        id: AzNavRailDefaults.AutoAboutId,
        text: '?',
        isRailItem: true,
        isAboutItem: true,
        collapseOnClick: false,
        shape: config.defaultShape,
      } as AzNavItem,
    ];
  }, [items, config.noMenu, autoAboutOwned, config.defaultShape]);
  const menuItems = useMemo(() => {
    return items.filter((i) => !i.isSubItem);
  }, [items]);

  // — Kinetic typography (WP7). Defaults animate; opt out via settings (itemEntrance: None, …). —
  const kItemEntrance =
    (config as AzNavRailSettings).itemEntrance ?? AzEntrance.Turnstile;
  const kItemExit = (config as AzNavRailSettings).itemExit ?? AzExit.Turnstile;
  const kStaggerMs = (config as AzNavRailSettings).entranceStaggerMs ?? 60;
  const kDurationMs =
    (config as AzNavRailSettings).entranceDurationMs ?? AzMotion.ItemDurationMs;
  const kStartAngle = (config as AzNavRailSettings).entranceStartAngle ?? 90;
  const kTiltOnPress = (config as AzNavRailSettings).tiltOnPress ?? false;
  const kMaxTilt = (config as AzNavRailSettings).maxTiltDegrees ?? 10;
  const kItemTextStyle = (config as AzNavRailSettings).itemTextStyle;
  const kDockingSide = config.dockingSide ?? AzDockingSide.LEFT;
  // Menu-drawer look/feel — new defaults (side-aligned + justified) preserve caller intent when unset.
  const menuItemAlignment =
    (config as AzNavRailSettings).menuItemAlignment ?? 'side';
  const justifyMenuItems =
    (config as AzNavRailSettings).justifyMenuItems ?? true;
  const dimBehindMenu = (config as AzNavRailSettings).dimBehindMenu ?? false;
  const dimBehindMenuAlpha =
    (config as AzNavRailSettings).dimBehindMenuAlpha ?? 0.4;
  // Keep the menu mounted through the staggered exit so items can turnstile out as the rail collapses.
  const menuRendered = useAzClosing(
    isExpanded && !config.noMenu,
    kItemExit,
    menuItems.length,
    kStaggerMs,
    kDurationMs
  );

  const getHeaderBorderRadius = () => {
    if (config.headerIconShape === AzHeaderIconShape.SQUARE) return 0;
    if (config.headerIconShape === AzHeaderIconShape.ROUNDED) return 8;
    return 24;
  };

  const flexDirection =
    config.dockingSide === AzDockingSide.RIGHT ? 'row-reverse' : 'row';

  return (
    <AzNavRailContext.Provider
      value={{ register, unregister, updateSettings, getDividerId, hasItem }}
    >
      <AzRailPaletteContext.Provider value={railPalette}>
        <View style={{ flexDirection: flexDirection, height: '100%', flex: 1 }}>
          {/* The scrim behind the drawer. It exists whenever the drawer is open, not only when
              `dimBehindMenu` is on — tapping outside the menu collapses it either way, and the
              dimming is just whether that area is also darkened. It sits below the rail's own
              z-index, so taps on the rail still reach the rail. */}
          {isExpanded && !isFloating && (
            <TouchableOpacity
              onPress={() => setIsExpanded(false)}
              activeOpacity={1}
              style={{
                ...(StyleSheet.absoluteFill as object),
                backgroundColor: dimBehindMenu
                  ? `rgba(0,0,0,${Math.max(0, Math.min(1, dimBehindMenuAlpha))})`
                  : 'transparent',
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
                transform: isFloating
                  ? [{ translateX: pan.x }, { translateY: pan.y }]
                  : [],
                zIndex: isFloating ? 1000 : 10,
                height: isFloating ? 'auto' : '100%',
                borderRightWidth: dockingSide === AzDockingSide.LEFT ? 1 : 0,
                borderLeftWidth: dockingSide === AzDockingSide.RIGHT ? 1 : 0,
              },
            ]}
            {...panResponder.panHandlers}
          >
            <TouchableOpacity
              onPress={handleHeaderTap}
              onLongPress={handleHeaderLongPress}
              delayLongPress={500}
              style={[
                styles.header,
                {
                  flexDirection:
                    config.dockingSide === AzDockingSide.RIGHT
                      ? 'row-reverse'
                      : 'row',
                },
              ]}
              onLayout={(e) => setHeaderHeight(e.nativeEvent.layout.height)}
              accessibilityRole="button"
              accessibilityLabel={
                isFloating
                  ? 'Undocked Rail'
                  : isExpanded
                    ? 'Collapse Menu'
                    : 'Expand Menu'
              }
              accessibilityHint="Double tap to toggle, long press to drag"
            >
              <View
                style={{
                  width: AzNavRailDefaults.HeaderIconSize,
                  height: AzNavRailDefaults.HeaderIconSize,
                  backgroundColor: 'gray',
                  borderRadius: getHeaderBorderRadius(),
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Text style={{ color: 'white' }}>Icon</Text>
              </View>
              {!isFloating && isExpanded && config.displayAppNameInHeader && (
                <Text
                  style={[
                    styles.appName,
                    {
                      marginLeft:
                        config.dockingSide === AzDockingSide.RIGHT ? 0 : 16,
                      marginRight:
                        config.dockingSide === AzDockingSide.RIGHT ? 16 : 0,
                      flexShrink: 0,
                      minWidth: 200,
                    },
                  ]}
                  numberOfLines={1}
                >
                  App Name
                </Text>
              )}
            </TouchableOpacity>

            {menuRendered && !noMenu ? (
              <ScrollView style={styles.menuContent}>
                {menuItems.map((item, index) => {
                  if (item.isDivider) {
                    return (
                      <View
                        key={item.id}
                        style={[
                          styles.divider,
                          { backgroundColor: railAccent || AZ_ACCENT_FALLBACK },
                        ]}
                      />
                    );
                  }
                  return renderMenuItem(
                    item,
                    index,
                    menuItems.length,
                    isExpanded
                  );
                })}
                {showFooter && renderFooter()}
              </ScrollView>
            ) : (
              <ScrollView contentContainerStyle={styles.railContent}>
                {isFloating && !showFloatingButtons
                  ? null
                  : effectiveRailItems.map((item, index) =>
                      renderRailItem(item, index)
                    )}
                {!isFloating &&
                  config.moreRailItem &&
                  config.moreFromAzEnabled && (
                    <AzButton
                      text="More"
                      color={railAccent || AZ_ACCENT_FALLBACK}
                      shape={config.defaultShape}
                      onClick={() => setShowMoreFromAz(true)}
                    />
                  )}
              </ScrollView>
            )}
          </Animated.View>

          {items
            .filter((i) => i.isNestedRail)
            .map((item) => {
              const effectiveConfig = item.nestedRailSettings
                ? { ...config, ...item.nestedRailSettings }
                : config;
              return (
                <AzNestedRailPopup
                  key={`nested-${item.id}`}
                  visible={nestedRailVisible === item.id}
                  onDismiss={() => setNestedRailVisible(null)}
                  items={subItemsMap[item.id] || []}
                  alignment={
                    item.nestedRailAlignment || AzNestedRailAlignment.VERTICAL
                  }
                  renderItem={(subItem, idx) => {
                    return (
                      <View
                        key={`wrap-${subItem.id}`}
                        onLayout={(e) => handleItemLayout(subItem.id, e)}
                      >
                        {renderRailItem(subItem, idx, effectiveConfig)}
                      </View>
                    );
                  }}
                  anchorPosition={anchorPosition}
                  dockingSide={effectiveConfig.dockingSide}
                  activeButtonSize={activeButtonSize}
                />
              );
            })}

          {/* Content with Safe Zones */}
          <View style={{ flex: 1, marginTop: '20%', marginBottom: '10%' }}>
            {children}
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
              settings={{
                activeColor: railAccent,
                translucentBackground: config.translucentBackground,
              }}
              moreFromAzEnabled={config.moreFromAzEnabled}
              moreFromAzJsonUrl={config.moreFromAzJsonUrl}
              onDismiss={() => setShowAbout(false)}
            />
          )}

          {showMoreFromAz && (
            <MoreFromAzOverlay
              jsonUrl={config.moreFromAzJsonUrl}
              settings={{
                activeColor: railAccent,
                translucentBackground: config.translucentBackground,
              }}
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
              accent={
                typeof config.activeColor === 'string'
                  ? config.activeColor
                  : undefined
              }
            />
          )}
        </View>
      </AzRailPaletteContext.Provider>
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
}> = ({
  items,
  itemBounds,
  isExpanded,
  isFloating,
  hostStates,
  currentDestination,
  nestedRailVisible,
  infoScreen,
  activeClassifiers,
  accent,
}) => {
  const guidance = useAzGuidanceContext();
  const activeItemId = useMemo(
    () =>
      items.find((it) => it.route && it.route === currentDestination)?.id ??
      null,
    [items, currentDestination]
  );
  const builtins = useCallback(
    () =>
      computeBuiltinStatuses({
        railExpanded: isExpanded,
        railFloating: isFloating,
        hostStates,
        currentRoute: currentDestination,
        activeItemId,
        nestedRailOpenId: nestedRailVisible,
        helpOpen: infoScreen,
      }),
    [
      isExpanded,
      isFloating,
      hostStates,
      currentDestination,
      activeItemId,
      nestedRailVisible,
      infoScreen,
    ]
  );
  const activeStatuses = useActiveStatuses(
    guidance.statusPredicates,
    activeClassifiers,
    builtins
  );
  const edges = useMemo(
    () => [...guidance.edges, ...computeAutoEdges(items)],
    [guidance.edges, items]
  );
  const frame = useMemo(
    () =>
      routeInstructions(
        edges,
        guidance.goals,
        guidance.activeGoals,
        activeStatuses,
        (k) => guidance.stepIndex(k),
        guidance.consumedStatuses
      ),
    [
      edges,
      guidance.goals,
      guidance.activeGoals,
      activeStatuses,
      guidance.stepIndex,
      guidance.consumedStatuses,
    ]
  );
  // Self-arming onboarding goals: `autoStartWhen` is discouraged; it honours both completion and the
  // user's skip, so a finished or dismissed tutorial never auto-restarts.
  useEffect(() => {
    Object.values(guidance.goals).forEach((goal) => {
      if (
        goal.autoStartWhen &&
        activeStatuses.has(goal.autoStartWhen) &&
        !guidance.isCompleted(goal.id) &&
        !guidance.isDismissed(goal.id) &&
        !guidance.activeGoals.includes(goal.id)
      ) {
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
        if (guidance.stepIndex(key) < r.stepIndex)
          guidance.setStep(key, r.stepIndex);
      }
    });
    pendingConsume.current.forEach((to) => {
      if (activeStatuses.has(to)) guidance.consume(to);
    });
  }, [frame, activeStatuses, guidance]);
  // Publish the observable snapshot (while enabled) so a host can mirror it with bespoke rendering.
  const snapshots = useMemo(
    () =>
      guidance.enabled
        ? snapshotsOf(frame.resolved, itemBounds, activeItemId)
        : [],
    [guidance.enabled, frame, itemBounds, activeItemId]
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
    ...StyleSheet.absoluteFill,
    backgroundColor: 'rgba(255,255,255,0.5)',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 10000,
    elevation: 10,
  },
  infoOverlay: {
    ...StyleSheet.absoluteFill,
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
  },
});
