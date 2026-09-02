import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import {
  Dimensions,
  Linking,
  Modal,
  PanResponder,
  StyleSheet,
  TouchableOpacity,
  UIManager,
  View,
  Vibration,
} from 'react-native';
import {
  AzButtonShape,
  AzDockingSide,
  AzNavItem,
  AzNestedRailAlignment,
  AzUnattachedAnchor,
} from '../types';
import { AzButton } from './AzButton';
import { AzToggle } from './AzToggle';
import { AzCycler } from './AzCycler';
import { AzRailSliderItem } from './AzRailSliderItem';
import { AzNestedRailPopup } from './AzNestedRailPopup';
import { AzWindow } from './AzWindow';
import { AzTextBox } from './AzTextBox';
import { resolveHighlight } from '../highlight';
import { AzNavRailDefaults } from '../AzNavRailDefaults';
import {
  AzFloatingDock,
  FloatingGeomConfig,
  FloatingStates,
  createFloatingRailState,
  directBelowDependent,
  directRightDependent,
  resolveDrop,
  resolvedPosition,
  topRowWidthPx,
} from '../services/floatingDockMath';
import { AzUnattachedFloatingStore } from '../services/unattachedFloatingStore';
import { isSafeExternalUrl } from '../util/AzSafeUrl';

/** Config the unattached layer needs from the rail's own settings. */
export interface AzUnattachedRailConfig {
  activeColor?: string;
  focusColor?: string;
  secondaryColor?: string;
  tertiaryColor?: string;
  packRailButtons?: boolean;
  railItemWidth?: number;
  translucentBackground?: string;
  vibrate?: boolean;
  dockingSide?: AzDockingSide;
  activeClassifiers?: Set<string>;
  secondaryClassifiers?: Set<string>;
  tertiaryClassifiers?: Set<string>;
}

/** Props for `AzUnattachedRail`. */
export interface AzUnattachedRailProps {
  /** Every registered item — hosts, sub-items and everything else — not just the unattached ones. */
  items: AzNavItem[];
  config: AzUnattachedRailConfig;
  currentDestination?: string;
  buttonSize: number;
  railAccent: string;
}

/** Every id in the unattached hosts' subtrees — the hosts themselves plus their sub-items to any depth. */
function unattachedSubtreeIds(items: AzNavItem[]): Set<string> {
  const roots = items.filter((i) => i.isUnattached).map((i) => i.id);
  if (roots.length === 0) return new Set();
  const ids = new Set(roots);
  for (let pass = 0; pass < items.length; pass++) {
    let grew = false;
    items.forEach((i) => {
      if (i.isSubItem && i.hostId && ids.has(i.hostId) && !ids.has(i.id)) {
        ids.add(i.id);
        grew = true;
      }
    });
    if (!grew) break;
  }
  return ids;
}

/**
 * Draws the `AzUnattachedAnchor` stacks — the rail host items declared with `AzUnattachedHostItem`,
 * which live outside the rail strip and the expanded menu. Renders nothing when no unattached host
 * is registered.
 *
 * Mirrors Android/CMP's `AzUnattachedRail` composable: `OPPOSITE`/`BOTTOM` hosts stack into a fixed
 * column in declaration order; `FLOATING` hosts float and dock independently (see
 * `FloatingDockGroup` below).
 *
 * Expansion state, the shared hidden-menu-open id, and the shared nested-rail-open id are all owned
 * here rather than by `<AzNavRail>` — an unattached host is not part of the rail's own item
 * bookkeeping and must keep working while the rail's own drawer/nested-rail state is untouched. One
 * consequence: an unattached item's "last tapped" focus highlight and an unattached nested rail's
 * open/closed state are tracked independently of the main rail strip's own (Android/CMP share one
 * scope-wide flag for each) — a harmless, cosmetic divergence given each surface already renders
 * disjoint items.
 */
export const AzUnattachedRail: React.FC<AzUnattachedRailProps> = ({
  items,
  config,
  currentDestination,
  buttonSize,
  railAccent,
}) => {
  const unattached = useMemo(
    () => items.filter((i) => i.isUnattached),
    [items]
  );
  if (unattached.length === 0) return null;

  return (
    <AzUnattachedRailInner
      items={items}
      unattached={unattached}
      config={config}
      currentDestination={currentDestination}
      buttonSize={buttonSize}
      railAccent={railAccent}
    />
  );
};

const AzUnattachedRailInner: React.FC<
  AzUnattachedRailProps & { unattached: AzNavItem[] }
> = ({
  items,
  unattached,
  config,
  currentDestination,
  buttonSize,
  railAccent,
}) => {
  const [hostStates, setHostStates] = useState<Record<string, boolean>>({});
  const [lastTappedId, setLastTappedId] = useState<string | null>(null);
  const [hiddenMenuOpenId, setHiddenMenuOpenId] = useState<string | null>(null);
  const [nestedRailOpenId, setNestedRailOpenId] = useState<string | null>(null);
  const [itemBounds, setItemBounds] = useState<
    Record<string, { x: number; y: number; width: number; height: number }>
  >({});

  const subItemsMap = useMemo(() => {
    const map: Record<string, AzNavItem[]> = {};
    items.forEach((item) => {
      if (item.isSubItem && item.hostId) {
        (map[item.hostId] ??= []).push(item);
      }
    });
    return map;
  }, [items]);

  const subtreeIds = useMemo(() => unattachedSubtreeIds(items), [items]);

  // Rising-edge auto-expand: expand once when `initiallyExpanded` is first seen true, on every
  // unattached host (top-level or nested), then leave the user alone — same contract as the rail.
  const initiallyExpandedSeen = useRef<Record<string, boolean>>({});
  useEffect(() => {
    items.forEach((item) => {
      if (item.isHost && subtreeIds.has(item.id)) {
        if (item.initiallyExpanded && !initiallyExpandedSeen.current[item.id]) {
          setHostStates((prev) => ({ ...prev, [item.id]: true }));
        }
        initiallyExpandedSeen.current[item.id] = !!item.initiallyExpanded;
      }
    });
  }, [items, subtreeIds]);

  // Reactive expansion (`expandWhen`), polled — same edge-triggered contract as the rail's own
  // hosts and nested rails.
  const expandWhenSeen = useRef<Record<string, boolean>>({});
  useEffect(() => {
    const withCondition = items.filter(
      (i) => subtreeIds.has(i.id) && typeof i.expandWhen === 'function'
    );
    if (withCondition.length === 0) return undefined;
    const interval = setInterval(() => {
      withCondition.forEach((item) => {
        const conditionNow = !!item.expandWhen!();
        const before = expandWhenSeen.current[item.id];
        if (before === undefined) {
          if (conditionNow) {
            if (item.isNestedRail) setNestedRailOpenId(item.id);
            else setHostStates((prev) => ({ ...prev, [item.id]: true }));
          }
        } else if (before !== conditionNow) {
          if (item.isNestedRail) {
            setNestedRailOpenId((prev) =>
              conditionNow ? item.id : prev === item.id ? null : prev
            );
          } else {
            setHostStates((prev) => ({ ...prev, [item.id]: conditionNow }));
          }
        }
        expandWhenSeen.current[item.id] = conditionNow;
      });
    }, 300);
    return () => clearInterval(interval);
  }, [items, subtreeIds]);

  const handleItemLayout = useCallback((id: string, e: any) => {
    const target = e.target as any;
    const fallback = () =>
      setItemBounds((prev) => ({ ...prev, [id]: { ...e.nativeEvent.layout } }));
    if (target && UIManager.measureInWindow) {
      UIManager.measureInWindow(
        target,
        (x: number, y: number, width: number, height: number) => {
          if (
            x !== undefined &&
            y !== undefined &&
            width !== undefined &&
            height !== undefined
          ) {
            setItemBounds((prev) => ({
              ...prev,
              [id]: { x, y, width, height },
            }));
          } else {
            fallback();
          }
        }
      );
    } else {
      fallback();
    }
  }, []);

  const spacing = config.packRailButtons
    ? 0
    : AzNavRailDefaults.RailContentVerticalArrangement;
  const railOnLeft =
    (config.dockingSide ?? AzDockingSide.LEFT) === AzDockingSide.LEFT;

  const byAnchor = useMemo(() => {
    const map: Record<AzUnattachedAnchor, AzNavItem[]> = {
      [AzUnattachedAnchor.OPPOSITE]: [],
      [AzUnattachedAnchor.BOTTOM]: [],
      [AzUnattachedAnchor.FLOATING]: [],
    };
    unattached.forEach((item) => {
      const anchor = item.unattachedAnchor ?? AzUnattachedAnchor.OPPOSITE;
      map[anchor].push(item);
    });
    return map;
  }, [unattached]);

  const sharedNodeProps: Omit<UnattachedNodeProps, 'item' | 'popupOpensLeft'> =
    {
      items,
      subItemsMap,
      config,
      currentDestination,
      buttonSize,
      railAccent,
      hostStates,
      setHostStates,
      lastTappedId,
      setLastTappedId,
      hiddenMenuOpenId,
      setHiddenMenuOpenId,
      nestedRailOpenId,
      setNestedRailOpenId,
      itemBounds,
      onItemLayout: handleItemLayout,
      spacing,
    };

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {byAnchor[AzUnattachedAnchor.OPPOSITE].length > 0 && (
        <View
          pointerEvents="box-none"
          style={[
            styles.edgeColumn,
            railOnLeft ? styles.opRight : styles.opLeft,
            styles.oppositeInset,
          ]}
        >
          <UnattachedStack
            hosts={byAnchor[AzUnattachedAnchor.OPPOSITE]}
            popupOpensLeft={railOnLeft}
            {...sharedNodeProps}
          />
        </View>
      )}
      {byAnchor[AzUnattachedAnchor.BOTTOM].length > 0 && (
        <View
          pointerEvents="box-none"
          style={[
            styles.edgeColumn,
            railOnLeft ? styles.opRight : styles.opLeft,
            styles.bottomInset,
          ]}
        >
          <UnattachedStack
            hosts={byAnchor[AzUnattachedAnchor.BOTTOM]}
            popupOpensLeft={railOnLeft}
            {...sharedNodeProps}
          />
        </View>
      )}
      {byAnchor[AzUnattachedAnchor.FLOATING].length > 0 && (
        <FloatingDockGroup
          hosts={byAnchor[AzUnattachedAnchor.FLOATING]}
          subItemsMap={subItemsMap}
          railOnLeft={railOnLeft}
          buttonSize={buttonSize}
          spacing={spacing}
          railAccent={railAccent}
          nodeProps={sharedNodeProps}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  edgeColumn: {
    position: 'absolute',
    alignItems: 'center',
  },
  opLeft: { left: 8 },
  opRight: { right: 8 },
  oppositeInset: { top: '10%', bottom: '10%' },
  bottomInset: { bottom: '10%', justifyContent: 'flex-end' },
  centeredColumn: { alignItems: 'center' },
  nestedRailPopupItem: { margin: 4 },
  hiddenMenuWindow: { position: 'absolute', minWidth: 150, padding: 8 },
  hiddenMenuInputRow: { paddingVertical: 4 },
  hiddenMenuInputBox: { width: 250 },
  floatingHost: { position: 'absolute' },
  grabBar: {
    alignSelf: 'center',
    height: 6,
    marginHorizontal: 4,
    marginVertical: 2,
    borderRadius: 3,
    opacity: 0.5,
  },
});

// ---------------------------------------------------------------------------------------------
// Shared node rendering — one unattached item (and, when an expanded host, its own sub-items).
// ---------------------------------------------------------------------------------------------

interface UnattachedNodeProps {
  item: AzNavItem;
  items: AzNavItem[];
  subItemsMap: Record<string, AzNavItem[]>;
  config: AzUnattachedRailConfig;
  currentDestination?: string;
  buttonSize: number;
  railAccent: string;
  hostStates: Record<string, boolean>;
  setHostStates: React.Dispatch<React.SetStateAction<Record<string, boolean>>>;
  lastTappedId: string | null;
  setLastTappedId: (id: string) => void;
  hiddenMenuOpenId: string | null;
  setHiddenMenuOpenId: (id: string | null) => void;
  nestedRailOpenId: string | null;
  setNestedRailOpenId: (id: string | null) => void;
  itemBounds: Record<
    string,
    { x: number; y: number; width: number; height: number }
  >;
  onItemLayout: (id: string, e: any) => void;
  spacing: number;
  /** Whether this node's own nested-rail popup should open to the left of it. */
  popupOpensLeft: boolean;
}

/** One anchor's column of unattached hosts, each unfolding its own sub-items beneath it. */
const UnattachedStack: React.FC<
  Omit<UnattachedNodeProps, 'item'> & { hosts: AzNavItem[] }
> = ({ hosts, spacing, ...rest }) => (
  <View style={styles.centeredColumn}>
    {hosts.map((host, i) => (
      <View key={host.id} style={i > 0 ? { marginTop: spacing } : undefined}>
        <UnattachedNode item={host} spacing={spacing} {...rest} />
      </View>
    ))}
  </View>
);

/**
 * One unattached item — plus, when it is an expanded host, its rail sub-items beneath it.
 */
const UnattachedNode: React.FC<UnattachedNodeProps> = (props) => {
  const {
    item,
    subItemsMap,
    config,
    currentDestination,
    buttonSize,
    railAccent,
    hostStates,
    setHostStates,
    lastTappedId,
    setLastTappedId,
    hiddenMenuOpenId,
    setHiddenMenuOpenId,
    nestedRailOpenId,
    setNestedRailOpenId,
    itemBounds,
    onItemLayout,
    spacing,
    popupOpensLeft,
  } = props;

  const color = resolveHighlight(
    item,
    config,
    currentDestination,
    lastTappedId
  );
  const commonStyle = { marginBottom: 0 };

  const tapAndFocus = () => {
    setLastTappedId(item.id);
    item.onClick?.();
  };

  let content: React.ReactNode;

  if (item.isSlider) {
    content = (
      <AzRailSliderItem
        item={item}
        buttonSize={buttonSize}
        enabled={!item.disabled}
        color={color ?? railAccent}
      />
    );
  } else if (item.isHost) {
    content = (
      <AzButton
        testID={item.id}
        text={item.text}
        content={item.content}
        hasCustomContent={!!item.content}
        color={color}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape || AzButtonShape.CIRCLE}
        enabled={!item.disabled}
        size={buttonSize}
        style={commonStyle}
        badge={item.badge}
        persistentBadge={item.persistentBadge}
        isLoading={item.isLoading}
        onClick={() => {
          const expanded = !(hostStates[item.id] || false);
          setHostStates((prev) => ({ ...prev, [item.id]: expanded }));
          item.onExpandedChange?.(expanded);
        }}
        onLongPress={
          item.hiddenMenu && item.hiddenMenu.length > 0
            ? () => {
                if (config.vibrate) Vibration.vibrate(50);
                setLastTappedId(item.id);
                setHiddenMenuOpenId(item.id);
              }
            : undefined
        }
      />
    );
  } else if (item.isToggle) {
    content = (
      <AzToggle
        testID={item.id}
        isChecked={!!item.isChecked}
        toggleOnText={item.toggleOnText}
        toggleOffText={item.toggleOffText}
        color={color}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape}
        disabled={item.disabled}
        size={buttonSize}
        style={commonStyle}
        badge={item.badge}
        persistentBadge={item.persistentBadge}
        onToggle={tapAndFocus}
      />
    );
  } else if (item.isCycler) {
    content = (
      <AzCycler
        testID={item.id}
        options={item.options || []}
        selectedOption={item.selectedOption || ''}
        disabledOptions={item.disabledOptions}
        color={color}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape}
        disabled={item.disabled}
        size={buttonSize}
        style={commonStyle}
        badge={item.badge}
        persistentBadge={item.persistentBadge}
        onCycle={() => {
          setLastTappedId(item.id);
          item.onClick?.();
        }}
      />
    );
  } else if (item.isRelocItem) {
    // Drag-to-reorder is deliberately not replicated outside the rail strip (see the docs on
    // `AzRailRelocItem`'s `onRelocate`); tap and long-press-to-open-hidden-menu both work.
    content = (
      <AzButton
        testID={item.id}
        text={item.text}
        content={item.content}
        hasCustomContent={!!item.content}
        color={color}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape || AzButtonShape.NONE}
        enabled={!item.disabled}
        size={buttonSize}
        style={commonStyle}
        badge={item.badge}
        persistentBadge={item.persistentBadge}
        isLoading={item.isLoading}
        onClick={tapAndFocus}
        onLongPress={
          item.hiddenMenu && item.hiddenMenu.length > 0
            ? () => {
                if (config.vibrate) Vibration.vibrate(50);
                setLastTappedId(item.id);
                setHiddenMenuOpenId(item.id);
              }
            : undefined
        }
      />
    );
  } else {
    content = (
      <AzButton
        testID={item.id}
        text={item.text}
        content={item.content}
        hasCustomContent={!!item.content}
        color={color}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape || AzButtonShape.CIRCLE}
        enabled={!item.disabled}
        size={buttonSize}
        style={commonStyle}
        badge={item.badge}
        persistentBadge={item.persistentBadge}
        isLoading={item.isLoading}
        onClick={() => {
          tapAndFocus();
          if (item.isNestedRail) {
            setNestedRailOpenId(nestedRailOpenId === item.id ? null : item.id);
          }
        }}
        onLongPress={
          item.hiddenMenu && item.hiddenMenu.length > 0
            ? () => {
                if (config.vibrate) Vibration.vibrate(50);
                setLastTappedId(item.id);
                setHiddenMenuOpenId(item.id);
              }
            : undefined
        }
      />
    );
  }

  const children = item.isHost ? subItemsMap[item.id] || [] : [];
  const isExpanded = item.isHost && !!hostStates[item.id];

  return (
    <View>
      <View onLayout={(e) => onItemLayout(item.id, e)}>{content}</View>

      {hiddenMenuOpenId === item.id && !!item.hiddenMenu?.length && (
        <HiddenMenuWindow
          item={item}
          accent={railAccent}
          translucentBackground={config.translucentBackground}
          anchor={itemBounds[item.id]}
          onDismiss={() => {
            item.onHiddenMenuDismiss?.();
            setHiddenMenuOpenId(null);
          }}
        />
      )}

      {item.isNestedRail && nestedRailOpenId === item.id && (
        <AzNestedRailPopup
          visible
          onDismiss={() => setNestedRailOpenId(null)}
          items={subItemsMap[item.id] || []}
          alignment={item.nestedRailAlignment || AzNestedRailAlignment.VERTICAL}
          dockingSide={popupOpensLeft ? 'RIGHT' : 'LEFT'}
          anchorPosition={itemBounds[item.id]}
          activeButtonSize={buttonSize}
          renderItem={(subItem) => (
            <View key={subItem.id} style={styles.nestedRailPopupItem}>
              <UnattachedNode
                {...props}
                item={subItem}
                popupOpensLeft={popupOpensLeft}
              />
            </View>
          )}
        />
      )}

      {isExpanded && children.length > 0 && (
        <View style={[styles.centeredColumn, { marginTop: spacing }]}>
          {children.map((child, i) => (
            <View
              key={child.id}
              style={i > 0 ? { marginTop: spacing } : undefined}
            >
              <UnattachedNode {...props} item={child} />
            </View>
          ))}
        </View>
      )}
    </View>
  );
};

/**
 * The long-press hidden menu for a reloc item rendered outside the rail strip. A `Modal` positioned
 * at the item's own measured bounds, the same idiom `DraggableRailItemWrapper` uses for the rail
 * strip's own reloc items — text entries, text-input entries, and route entries (opened via
 * `Linking`) all work; only drag-to-reorder does not apply here (see the KDoc on
 * `AzRailRelocItem`'s `onRelocate`).
 */
/**
 * Renders a hidden context menu as a floating `AzWindow`, anchored beside `anchor`. Shared with the
 * docked rail strip (`AzNavRail.tsx`) — a hidden menu is not an unattached-rail-only affordance.
 */
export const HiddenMenuWindow: React.FC<{
  item: AzNavItem;
  accent: string;
  translucentBackground?: string;
  anchor?: { x: number; y: number; width: number; height: number };
  onDismiss: () => void;
}> = ({ item, accent, translucentBackground, anchor, onDismiss }) => {
  const top = anchor ? anchor.y : 0;
  const left = anchor ? anchor.x + anchor.width + 8 : 0;
  return (
    <Modal transparent visible onRequestClose={onDismiss}>
      <TouchableOpacity
        style={StyleSheet.absoluteFill}
        activeOpacity={1}
        onPress={onDismiss}
      >
        <AzWindow
          accent={accent}
          surfaceColor={translucentBackground || '#1A1A1F'}
          onDismiss={onDismiss}
          testID={`${item.id}_hidden_menu`}
          style={StyleSheet.flatten([styles.hiddenMenuWindow, { top, left }])}
        >
          {(item.hiddenMenu || []).map((menuItem) =>
            menuItem.isInput ? (
              <View key={menuItem.id} style={styles.hiddenMenuInputRow}>
                <AzTextBox
                  containerStyle={styles.hiddenMenuInputBox}
                  initialValue={menuItem.initialValue}
                  hint={menuItem.hint}
                  onValueChange={menuItem.onValueChange}
                  onSubmit={(val) => {
                    menuItem.onValueChange?.(val);
                    onDismiss();
                  }}
                  showSubmitButton
                  outlined
                />
              </View>
            ) : (
              <AzButton
                key={menuItem.id}
                text={menuItem.text}
                shape={AzButtonShape.NONE}
                onClick={() => {
                  menuItem.onClick?.();
                  if (menuItem.route && isSafeExternalUrl(menuItem.route)) {
                    Linking.openURL(menuItem.route).catch(() => {});
                  }
                  onDismiss();
                }}
              />
            )
          )}
        </AzWindow>
      </TouchableOpacity>
    </Modal>
  );
};

// ---------------------------------------------------------------------------------------------
// FLOATING: independent per-rail docking, rail-to-rail attachment, and group dragging.
// ---------------------------------------------------------------------------------------------

interface FloatingDockGroupProps {
  hosts: AzNavItem[];
  subItemsMap: Record<string, AzNavItem[]>;
  railOnLeft: boolean;
  buttonSize: number;
  spacing: number;
  railAccent: string;
  nodeProps: Omit<UnattachedNodeProps, 'item' | 'popupOpensLeft'>;
}

const DP = 1; // React Native layout units are already density-independent "px" for our purposes.
const GRAB_BAR_HEIGHT = 10 * DP;
const EDGE_START = 8 * DP;
const EDGE_SNAP = 56 * DP;
const RAIL_SNAP = 24 * DP;

/**
 * Renders every top-level `FLOATING`-anchored unattached host as an independently draggable stack,
 * with screen-edge docking, rail-to-rail attachment (capped at two columns), and group dragging via
 * a grab bar — see `services/floatingDockMath.ts` for the geometry this is built on, and the
 * Android/CMP `FloatingDockGroup` this ports.
 */
const FloatingDockGroup: React.FC<FloatingDockGroupProps> = ({
  hosts,
  subItemsMap,
  railOnLeft,
  buttonSize,
  spacing,
  railAccent,
  nodeProps,
}) => {
  const [screen, setScreen] = useState(() => {
    const w = Dimensions.get('window');
    return { width: w.width, height: w.height };
  });
  useEffect(() => {
    const sub = Dimensions.addEventListener('change', ({ window }) =>
      setScreen({ width: window.width, height: window.height })
    );
    return () => sub?.remove?.();
  }, []);

  const minY = screen.height * 0.1;
  const maxYBase = screen.height * 0.9;

  const cfg: FloatingGeomConfig = {
    spacingPx: spacing,
    edgeStartPx: EDGE_START,
    edgeSnapPx: EDGE_SNAP,
    railSnapPx: RAIL_SNAP,
    grabBarHeightPx: GRAB_BAR_HEIGHT,
    minY,
    maxYBase,
    screenWidthPx: screen.width,
    screenHeightPx: screen.height,
    railOnLeft,
  };

  const [states, setStates] = useState<FloatingStates>({});

  const worstCaseHeightPx = useCallback(
    (hostId: string): number => {
      const rowCount = (id: string): number => {
        let count = 1;
        (subItemsMap[id] || []).forEach((child) => {
          count += child.isHost ? rowCount(child.id) : 1;
        });
        return count;
      };
      return rowCount(hostId) * (buttonSize + spacing) - spacing;
    },
    [subItemsMap, buttonSize, spacing]
  );

  // Create state for newly-declared hosts (loading any persisted dock/free position), drop state
  // for hosts no longer declared, and heal a survivor's attachment pointer if its target vanished.
  useEffect(() => {
    const ids = new Set(hosts.map((h) => h.id));
    setStates((prev) => {
      const next: FloatingStates = {};
      Object.entries(prev).forEach(([id, st]) => {
        if (!ids.has(id)) return;
        next[id] = {
          ...st,
          rightOf: st.rightOf && ids.has(st.rightOf) ? st.rightOf : null,
          belowOf: st.belowOf && ids.has(st.belowOf) ? st.belowOf : null,
        };
      });
      let nextY = minY;
      hosts.forEach((host) => {
        if (next[host.id]) return;
        const saved = AzUnattachedFloatingStore.loadFloatingSync(host.id);
        next[host.id] = saved
          ? createFloatingRailState(
              saved.dock,
              saved.dock === AzFloatingDock.FREE
                ? { x: saved.a * screen.width, y: saved.b * screen.height }
                : null,
              saved.a
            )
          : createFloatingRailState(
              AzFloatingDock.FREE,
              { x: railOnLeft ? screen.width - buttonSize : 0, y: nextY },
              0
            );
        nextY += worstCaseHeightPx(host.id) + spacing;
      });
      return next;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hosts.map((h) => h.id).join(',')]);

  // Async (native `AsyncStorage`) load layered on top of the synchronous localStorage default,
  // applied only if the host is still exactly where it was first placed (a user hasn't already
  // dragged it in the meantime).
  useEffect(() => {
    hosts.forEach((host) => {
      AzUnattachedFloatingStore.loadFloatingAsync(host.id).then((saved) => {
        if (!saved) return;
        setStates((prev) => {
          const st = prev[host.id];
          if (!st || st.dragging || st.rightOf || st.belowOf) return prev;
          return {
            ...prev,
            [host.id]: {
              ...st,
              dock: saved.dock,
              freeOffset:
                saved.dock === AzFloatingDock.FREE
                  ? { x: saved.a * screen.width, y: saved.b * screen.height }
                  : st.freeOffset,
              priority: saved.a,
            },
          };
        });
      });
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hosts.map((h) => h.id).join(',')]);

  const beginDrag = useCallback((id: string) => {
    setStates((prev) => {
      const st = prev[id];
      if (!st) return prev;
      const current = resolvedPosition(prev, id, cfgRef.current);
      return {
        ...prev,
        [id]: {
          ...st,
          rightOf: null,
          belowOf: null,
          liveDragOffset: current,
          dragging: true,
        },
      };
    });
  }, []);

  const dragBy = useCallback((id: string, dx: number, dy: number) => {
    setStates((prev) => {
      const st = prev[id];
      if (!st) return prev;
      return {
        ...prev,
        [id]: {
          ...st,
          liveDragOffset: {
            x: st.liveDragOffset.x + dx,
            y: st.liveDragOffset.y + dy,
          },
        },
      };
    });
  }, []);

  // `cfg` is recreated every render (it depends on `screen`), but the drag callbacks above are
  // created once per `id` via `useMemo` below and need the LATEST cfg when they fire. A ref sidesteps
  // recreating every PanResponder on every render (e.g. every keystroke elsewhere in the app).
  const cfgRef = useRef(cfg);
  cfgRef.current = cfg;

  const endDrag = useCallback(
    (id: string) => {
      setStates((prev) => {
        const st = prev[id];
        if (!st) return prev;
        const outcome = resolveDrop(
          prev,
          id,
          cfgRef.current,
          worstCaseHeightPx
        );
        const base = { ...st, dragging: false };
        if (outcome.kind === 'attach') {
          const next = {
            ...prev,
            [id]: {
              ...base,
              rightOf: outcome.attachRight ? outcome.targetId : null,
              belowOf: outcome.attachRight ? null : outcome.targetId,
            },
          };
          return next;
        }
        if (outcome.dock === AzFloatingDock.FREE) {
          AzUnattachedFloatingStore.saveFloating(id, {
            dock: AzFloatingDock.FREE,
            a: outcome.freeOffset!.x / cfgRef.current.screenWidthPx,
            b: outcome.freeOffset!.y / cfgRef.current.screenHeightPx,
          });
          return {
            ...prev,
            [id]: {
              ...base,
              dock: outcome.dock,
              freeOffset: outcome.freeOffset!,
            },
          };
        }
        AzUnattachedFloatingStore.saveFloating(id, {
          dock: outcome.dock,
          a: outcome.priority ?? 0,
          b: 0,
        });
        return {
          ...prev,
          [id]: {
            ...base,
            dock: outcome.dock,
            priority: outcome.priority ?? 0,
          },
        };
      });
    },
    [worstCaseHeightPx]
  );

  const panResponders = useRef<
    Record<string, ReturnType<typeof PanResponder.create>>
  >({});
  const getPanResponder = (id: string) => {
    if (!panResponders.current[id]) {
      panResponders.current[id] = PanResponder.create({
        onStartShouldSetPanResponder: () => true,
        onMoveShouldSetPanResponder: (_e, g) =>
          Math.abs(g.dx) > 4 || Math.abs(g.dy) > 4,
        onPanResponderGrant: () => beginDrag(id),
        onPanResponderMove: (_e, g) => dragBy(id, g.dx, g.dy),
        onPanResponderRelease: () => endDrag(id),
        onPanResponderTerminate: () => endDrag(id),
      });
    }
    return panResponders.current[id]!;
  };

  return (
    <>
      {hosts.map((host) => {
        const st = states[host.id];
        if (!st) return null;
        const pos = resolvedPosition(states, host.id, cfg);
        const isColumnRoot = st.rightOf === null && st.belowOf === null;
        const hasDependent =
          directRightDependent(states, host.id) !== undefined ||
          directBelowDependent(states, host.id) !== undefined;
        const popupOpensLeft =
          st.rightOf !== null
            ? false
            : directRightDependent(states, host.id) !== undefined
              ? true
              : railOnLeft;

        return (
          <View
            key={host.id}
            style={[styles.floatingHost, { left: pos.x, top: pos.y }]}
            pointerEvents="box-none"
          >
            {isColumnRoot && hasDependent && (
              <FloatingGrabBar
                widthPx={topRowWidthPx(states, host.id, spacing)}
                accent={railAccent}
                panHandlers={getPanResponder(host.id).panHandlers}
              />
            )}
            <View
              onLayout={(e) => {
                const { width, height } = e.nativeEvent.layout;
                setStates((prev) => {
                  const cur = prev[host.id];
                  if (!cur) return prev;
                  if (cur.size.width === width && cur.size.height === height)
                    return prev;
                  return {
                    ...prev,
                    [host.id]: { ...cur, size: { width, height } },
                  };
                });
              }}
              {...getPanResponder(host.id).panHandlers}
            >
              <UnattachedStack
                hosts={[host]}
                popupOpensLeft={popupOpensLeft}
                {...nodeProps}
              />
            </View>
          </View>
        );
      })}
    </>
  );
};

/**
 * A thin drag handle shown above a `FloatingDockGroup` column-root that has at least one other rail
 * attached to it, spanning that rail's row so the whole group reads as one draggable unit.
 */
const FloatingGrabBar: React.FC<{
  widthPx: number;
  accent: string;
  panHandlers: ReturnType<typeof PanResponder.create>['panHandlers'];
}> = ({ widthPx, accent, panHandlers }) => (
  <View
    {...panHandlers}
    style={[
      styles.grabBar,
      { width: Math.max(24, widthPx), backgroundColor: accent },
    ]}
  />
);
