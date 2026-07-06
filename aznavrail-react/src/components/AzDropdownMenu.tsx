import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  Modal,
  Pressable,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  Vibration,
  Linking,
  useWindowDimensions,
  ViewStyle,
  LayoutChangeEvent,
  Animated,
  Easing as RNEasing,
} from 'react-native';
import { AzButton } from './AzButton';
import { AzButtonShape, AzDockingSide, AzDropdownDesign, AzEasing, AzEntrance, AzExit, AzHeaderIconShape } from '../types';
import { AzNavRailDefaults } from '../AzNavRailDefaults';
import { AboutOverlay } from './AboutOverlay';
import { AzKineticItem, useAzClosing } from './AzKinetics';
import { solveHybridJustify } from '../util/AzJustify';

const DROPDOWN_BASE_FONT_PX = 16;

/** Context the menu provides so item components can navigate, fold the menu, and match its design. */
interface AzDropdownMenuContextValue {
  dismiss: () => void;
  /** The panel design, so items can render rail-style or menu-style. */
  design: AzDropdownDesign;
  /** Navigation handler for item `route`s (AzNavHost-style routing). */
  onNavigate?: (route: string) => void;
  /** Optional style merged over each MENU-design item's label (big/light/wide Metro type). */
  itemTextStyle?: object;
  /** Docking side propagated to the MENU-design items for side-alignment. */
  dockingSide?: AzDockingSide;
  /** Alignment of MENU-design labels within the row. */
  menuItemAlignment?: 'center' | 'side';
  /** When true, MENU-design labels are full-justified via computed letter-spacing. */
  justifyMenuItems?: boolean;
}
const AzDropdownMenuContext = createContext<AzDropdownMenuContextValue | null>(null);

export interface AzDropdownMenuProps {
  /** Whether the panel imitates the collapsed rail or the expanded menu. */
  design?: AzDropdownDesign;
  /** Which screen edge the panel pins to. */
  dockingSide?: AzDockingSide;
  /** Haptic feedback when the trigger is tapped. */
  vibrate?: boolean;
  /** Panel width (px) in the MENU design. */
  expandedWidth?: number;
  /** Panel width (px) in the RAIL design. */
  collapsedWidth?: number;
  /** Clip shape for the app-icon trigger (mirrors the rail's `headerIconShape`). */
  headerIconShape?: AzHeaderIconShape;
  /** Diameter (px) of the app-icon trigger (mirrors the rail's `headerIconSize`). */
  headerIconSize?: number;
  /** Whether the MENU design shows the rail's footer (About / Feedback / @HereLiesAz). */
  showFooter?: boolean;
  /**
   * Repository URL backing the footer's "About" item. When unset/blank, "About" is hidden entirely.
   * When set, "About" opens the in-app reader if {@link inAppAbout} is true, else the URL.
   */
  appRepositoryUrl?: string;
  /**
   * When true (default), the footer "About" opens the full-screen in-app reader; when false it opens
   * {@link appRepositoryUrl} in a browser. Only takes effect when `appRepositoryUrl` is set.
   */
  inAppAbout?: boolean;
  /** When true (default), the in-app About reader offers a "More from Az" carousel. */
  moreFromAzEnabled?: boolean;
  /** Raw URL of the `more-from-az.json` manifest backing the carousel. */
  moreFromAzJsonUrl?: string;
  /** Optional controlled open-state. When omitted the menu manages its own. */
  expanded?: boolean;
  /** Called whenever the open-state changes. */
  onExpandedChange?: (open: boolean) => void;
  /** Navigation handler invoked with an item's `route` before its callback (AzNavHost-style). */
  onNavigate?: (route: string) => void;
  /** Style applied to the trigger container (placement only). */
  style?: ViewStyle;
  // — Kinetic typography (WP7). On by default; pass `None` to opt a surface out. —
  /** Entrance played by each item when the panel opens. Default `Turnstile`. */
  itemEntrance?: AzEntrance;
  /** Exit played by each item when the panel dismisses. Default `Turnstile`. */
  itemExit?: AzExit;
  /** Style merged over each MENU-design item's label. */
  itemTextStyle?: object;
  /** Per-item cascade delay (ms), multiplied by position. Default 55. */
  entranceStaggerMs?: number;
  /** Duration (ms) of each item's entrance/exit. Default 360. */
  entranceDurationMs?: number;
  /** Starting rotateY (deg) for the turnstile sweep. Default 70. */
  entranceStartAngle?: number;
  /** When true, items tilt toward the press point (WP7 tilt effect). Default false. */
  tiltOnPress?: boolean;
  /** Maximum tilt angle (deg) for `tiltOnPress`. Default 10. */
  maxTiltDegrees?: number;
  /** When true, opening the menu draws a dim scrim over the rest of the app. Default false. */
  dimBehindMenu?: boolean;
  /** Alpha of the dim scrim (0..1). Default 0.4. */
  dimBehindMenuAlpha?: number;
  /** How MENU-design labels are aligned. Default `'side'` (docked-side aligned). */
  menuItemAlignment?: 'center' | 'side';
  /** When true, MENU-design labels are full-justified via computed letter-spacing. Default true. */
  justifyMenuItems?: boolean;
  /** The menu items — `AzDropdownItem`, `AzDivider`, etc. */
  children?: React.ReactNode;
}

export interface AzDropdownItemProps {
  text: string;
  onClick: () => void;
  /** Optional navigation route, dispatched via the menu's `onNavigate` before `onClick`. */
  route?: string;
  shape?: AzButtonShape;
  enabled?: boolean;
  color?: string;
  textColor?: string;
  fillColor?: string;
  /** When true (default) the menu folds up after `onClick`. */
  closeOnClick?: boolean;
}

/**
 * A tappable menu entry. In a {@link AzDropdownDesign.MENU} panel it renders as a full-width labeled
 * row (the expanded-drawer look); in a {@link AzDropdownDesign.RAIL} panel it renders as a compact
 * {@link AzButton}. Navigates `route` (if set) then runs `onClick`, folding the menu by default.
 */
export const AzDropdownItem: React.FC<AzDropdownItemProps> = ({
  text,
  onClick,
  route,
  shape = AzButtonShape.RECTANGLE,
  enabled = true,
  color,
  textColor,
  fillColor,
  closeOnClick = true,
}) => {
  const ctx = useContext(AzDropdownMenuContext);
  // Hooks must run unconditionally. Guard reads via optional chaining below.
  const [availableWidth, setAvailableWidth] = useState(0);
  const [naturalWidth, setNaturalWidth] = useState(0);
  if (!ctx) throw new Error('AzDropdownItem must be used inside an <AzDropdownMenu>');
  const {
    dismiss,
    design,
    onNavigate,
    itemTextStyle,
    dockingSide: ctxDockingSide,
    menuItemAlignment: ctxMenuItemAlignment,
    justifyMenuItems: ctxJustifyMenuItems,
  } = ctx;
  const press = () => {
    if (route) onNavigate?.(route);
    onClick();
    if (closeOnClick) dismiss();
  };
  if (design === AzDropdownDesign.MENU) {
    const alignment = ctxMenuItemAlignment ?? 'side';
    const justify = ctxJustifyMenuItems ?? true;
    const textAlign: 'left' | 'right' | 'center' =
      alignment === 'center'
        ? 'center'
        : ctxDockingSide === AzDockingSide.RIGHT ? 'right' : 'left';
    const onContainerLayout = (e: LayoutChangeEvent) => {
      const w = e.nativeEvent.layout.width;
      if (w && Math.abs(w - availableWidth) > 0.5) setAvailableWidth(w);
    };
    const onLabelLayout = (e: LayoutChangeEvent) => {
      const w = e.nativeEvent.layout.width;
      if (w && Math.abs(w - naturalWidth) > 0.5) setNaturalWidth(w);
    };
    // Hybrid kerning + font-scale justify — see src/util/AzJustify.
    let letterSpacing = 0;
    let fontScale = 1;
    if (justify && text.length >= 2 && availableWidth > 0 && naturalWidth > 0) {
      const solved = solveHybridJustify(naturalWidth, availableWidth, text.length, DROPDOWN_BASE_FONT_PX);
      fontScale = solved.scale;
      letterSpacing = solved.letterSpacing;
    }
    const scaledFontSize = DROPDOWN_BASE_FONT_PX * fontScale;
    return (
      <TouchableOpacity
        style={styles.menuRow}
        onPress={enabled ? press : undefined}
        onLayout={onContainerLayout}
        disabled={!enabled}
        accessibilityRole="button"
        accessibilityLabel={text}
      >
        {/* Hidden off-screen measurer — natural width only, no layout impact. */}
        <Text
          onLayout={onLabelLayout}
          style={[
            styles.menuRowText,
            { position: 'absolute', opacity: 0, left: -9999, top: -9999 } as any,
            itemTextStyle,
          ]}
        >
          {text}
        </Text>
        <Text
          numberOfLines={1}
          style={[
            styles.menuRowText,
            { color: textColor || color || '#6750A4', opacity: enabled ? 1 : 0.5, textAlign, letterSpacing, fontSize: scaledFontSize },
            itemTextStyle,
          ]}
        >
          {text}
        </Text>
      </TouchableOpacity>
    );
  }
  return (
    <AzButton
      text={text}
      onClick={press}
      shape={shape}
      enabled={enabled}
      color={color}
      textColor={textColor}
      fillColor={fillColor}
    />
  );
};

interface Anchor { x: number; y: number; width: number; height: number; }

/**
 * A standalone, hamburger-style drop-down menu, declared with the same opinionated surface as the
 * rail. The trigger is the **app icon** (drawn like the rail's header; its shape/size set via
 * `headerIconShape`/`headerIconSize`); the panel
 * is configured by `design` + `dockingSide`, width-constrained to match, pinned to the chosen screen
 * edge, and dropped from the trigger. Items may carry a `route` dispatched through `onNavigate`.
 *
 * ```tsx
 * <AzDropdownMenu design={AzDropdownDesign.MENU} dockingSide={AzDockingSide.LEFT} onNavigate={go}>
 *   <AzDropdownItem text="Home" route="home" onClick={() => {}} />
 *   <AzDivider />
 *   <AzDropdownItem text="Sign out" onClick={signOut} />
 * </AzDropdownMenu>
 * ```
 */
export const AzDropdownMenu: React.FC<AzDropdownMenuProps> = ({
  design = AzDropdownDesign.MENU,
  dockingSide = AzDockingSide.LEFT,
  vibrate = false,
  expandedWidth = AzNavRailDefaults.ExpandedRailWidth,
  collapsedWidth = AzNavRailDefaults.CollapsedRailWidth,
  headerIconShape = AzHeaderIconShape.CIRCLE,
  headerIconSize = AzNavRailDefaults.HeaderIconSize,
  showFooter = true,
  appRepositoryUrl,
  inAppAbout = true,
  moreFromAzEnabled = true,
  moreFromAzJsonUrl = 'https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json',
  expanded,
  onExpandedChange,
  onNavigate,
  style,
  itemEntrance = AzEntrance.Turnstile,
  itemExit = AzExit.Turnstile,
  itemTextStyle,
  entranceStaggerMs = 60,
  entranceDurationMs = 720,
  entranceStartAngle = 90,
  tiltOnPress = false,
  maxTiltDegrees = 10,
  dimBehindMenu = false,
  dimBehindMenuAlpha = 0.4,
  menuItemAlignment = 'side',
  justifyMenuItems = true,
  children,
}) => {
  const [internalOpen, setInternalOpen] = useState(false);
  const isOpen = expanded ?? internalOpen;
  const triggerRef = useRef<View>(null);
  const [anchor, setAnchor] = useState<Anchor>({ x: 0, y: 0, width: 0, height: 0 });
  const [panelSize, setPanelSize] = useState({ width: 0, height: 0 });
  // Full-screen About reader reachable from the dropdown footer (parity with the rail). The
  // "More from Az" carousel is reachable from within AboutOverlay itself.
  const [showAbout, setShowAbout] = useState(false);

  const setOpen = (value: boolean) => {
    if (expanded === undefined) setInternalOpen(value);
    onExpandedChange?.(value);
  };

  const openMenu = () => {
    if (vibrate) Vibration.vibrate();
    if (triggerRef.current) {
      triggerRef.current.measureInWindow((x, y, width, height) => {
        setAnchor({ x, y, width, height });
        setOpen(true);
      });
    } else {
      setOpen(true);
    }
  };

  // Reactive so the panel re-positions on orientation / split-screen changes.
  const screen = useWindowDimensions();
  const panelWidth = design === AzDropdownDesign.RAIL ? collapsedWidth : expandedWidth;
  const triggerSize = headerIconSize;
  // Clip radius mirrors the rail's header icon: circle = half, rounded = 8, anything else = 0.
  const triggerRadius =
    headerIconShape === AzHeaderIconShape.ROUNDED ? 8 :
    headerIconShape === AzHeaderIconShape.CIRCLE ? triggerSize / 2 :
    0;

  const panelPosition: ViewStyle = { position: 'absolute' };
  // Horizontal: pin to the physical docking-side edge.
  panelPosition.left = dockingSide === AzDockingSide.RIGHT ? screen.width - panelWidth : 0;
  // Vertical: drop from the trigger — downward when it fits below, otherwise upward.
  const fitsBelow = anchor.y + anchor.height + panelSize.height <= screen.height;
  panelPosition.top = fitsBelow ? anchor.y + anchor.height : Math.max(0, anchor.y - panelSize.height);

  const onPanelLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width !== panelSize.width || height !== panelSize.height) setPanelSize({ width, height });
  };

  // Keep the panel mounted through the staggered exit so items can animate out before teardown.
  const items = React.Children.toArray(children);
  const rendered = useAzClosing(isOpen, itemExit, items.length, entranceStaggerMs, entranceDurationMs);

  return (
    <View style={style}>
      {/* The app-icon trigger — drawn like the rail's header icon (gray placeholder), clipped to the
          configured shape/size. */}
      <TouchableOpacity
        ref={triggerRef as React.RefObject<any>}
        onPress={() => (isOpen ? setOpen(false) : openMenu())}
        accessibilityRole="button"
        accessibilityLabel="Menu"
      >
        <View
          testID="az-dropdown-trigger"
          style={{
            // Automatic breathing room around the app-icon trigger (parity with the rail header).
            margin: 8,
            width: triggerSize,
            height: triggerSize,
            borderRadius: triggerRadius,
            backgroundColor: 'gray',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }}
        />
      </TouchableOpacity>

      {rendered && (
        <Modal visible transparent animationType="fade" onRequestClose={() => setOpen(false)}>
          <Pressable
            style={[
              StyleSheet.absoluteFill,
              dimBehindMenu
                ? { backgroundColor: `rgba(0,0,0,${Math.max(0, Math.min(1, dimBehindMenuAlpha))})` }
                : null,
            ]}
            onPress={() => setOpen(false)}
          >
            <View
              testID="az-dropdown-panel"
              onLayout={onPanelLayout}
              style={[
                styles.panel,
                panelPosition,
                { maxHeight: screen.height * 0.8, width: panelWidth },
              ]}
            >
              {/* Stop propagation so taps inside the panel don't dismiss it. */}
              <Pressable>
                <ScrollView
                  contentContainerStyle={
                    design === AzDropdownDesign.MENU
                      ? { alignItems: 'stretch' }
                      : { alignItems: 'center', padding: 8 }
                  }
                >
                  <AzDropdownMenuContext.Provider value={{ dismiss: () => setOpen(false), design, onNavigate, itemTextStyle, dockingSide, menuItemAlignment, justifyMenuItems }}>
                    {items.map((child, i) => (
                      <AzKineticItem
                        key={i}
                        index={i}
                        count={items.length}
                        visible={isOpen}
                        entrance={itemEntrance}
                        exit={itemExit}
                        staggerMs={entranceStaggerMs}
                        durationMs={entranceDurationMs}
                        startAngle={entranceStartAngle}
                        tiltOnPress={tiltOnPress}
                        maxTiltDegrees={maxTiltDegrees}
                        dockingSide={dockingSide}
                      >
                        {child}
                      </AzKineticItem>
                    ))}
                  </AzDropdownMenuContext.Provider>
                  {/* The expanded-menu design carries the rail's footer. */}
                  {design === AzDropdownDesign.MENU && showFooter && (
                    <AzDropdownFooter
                      appRepositoryUrl={appRepositoryUrl}
                      inAppAbout={inAppAbout}
                      onInAppAbout={() => setShowAbout(true)}
                      visible={isOpen}
                      menuItemCount={items.length}
                      staggerMs={entranceStaggerMs}
                      durationMs={entranceDurationMs}
                    />
                  )}
                </ScrollView>
              </Pressable>
            </View>

            {/* Full-screen footer screens, rendered above the dropdown panel — mirrors the rail.
                Gated on `appRepositoryUrl`; About is hidden entirely when it is unset. */}
            {showAbout && !!appRepositoryUrl && (
              <AboutOverlay
                repoUrl={appRepositoryUrl}
                moreFromAzEnabled={moreFromAzEnabled}
                moreFromAzJsonUrl={moreFromAzJsonUrl}
                onDismiss={() => setShowAbout(false)}
              />
            )}
          </Pressable>
        </Modal>
      )}
    </View>
  );
};

/** The MENU design's footer — mirrors the rail's footer (About / Feedback / @HereLiesAz). */
const AzDropdownFooter: React.FC<{
  appRepositoryUrl?: string;
  inAppAbout?: boolean;
  onInAppAbout?: () => void;
  visible?: boolean;
  menuItemCount?: number;
  staggerMs?: number;
  durationMs?: number;
}> = ({
  appRepositoryUrl,
  inAppAbout = true,
  onInAppAbout,
  visible = true,
  menuItemCount = 0,
  staggerMs = 60,
  durationMs = 720,
}) => {
  const footerColor = '#6750A4';
  // Only open safe schemes — this also runs on the web via react-native-web, where a `javascript:`
  // URL would otherwise execute.
  const open = (url: string) => {
    const isSafe = url.startsWith('http://') || url.startsWith('https://') || url.startsWith('mailto:');
    if (isSafe) Linking.openURL(url).catch(() => {});
  };
  const onAbout = () => {
    if (!appRepositoryUrl) return;
    if (inAppAbout) onInAppAbout?.();
    else open(appRepositoryUrl);
  };

  // Accordion unfold from the top edge when the last dropdown item begins its own kinetic entrance.
  const anim = useRef(new Animated.Value(visible ? 1 : 0)).current;
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
    anim.setValue(0);
    return undefined;
  }, [visible, menuItemCount, staggerMs, durationMs, anim]);

  return (
    <Animated.View
      style={{
        opacity: anim,
        transform: [{ scaleY: anim }],
        ...( { transformOrigin: 'top center' } as any ),
      }}
    >
      <View style={styles.footer}>
        {/* About is hidden entirely when no repository URL is configured. */}
        {!!appRepositoryUrl && (
          <TouchableOpacity onPress={onAbout} style={styles.footerRow} accessibilityRole="button">
            <Text style={[styles.footerText, { color: footerColor }]}>About</Text>
          </TouchableOpacity>
        )}
        <TouchableOpacity onPress={() => open('mailto:hereliesaz@gmail.com?subject=Feedback')} style={styles.footerRow} accessibilityRole="button">
          <Text style={[styles.footerText, { color: footerColor }]}>Feedback</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => open('https://instagram.com/HereLiesAz')} style={styles.footerRow} accessibilityRole="button">
          <Text style={[styles.footerText, { color: footerColor, opacity: 0.5 }]}>@HereLiesAz</Text>
        </TouchableOpacity>
      </View>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  panel: {
    borderRadius: 12,
    backgroundColor: '#ffffff',
    elevation: 8,
    shadowColor: '#000',
    shadowOpacity: 0.18,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 8 },
  },
  // Expanded-drawer look: full-width labeled row matching the rail's menu item (16px text, 12/16 pad).
  menuRow: {
    width: '100%',
    paddingHorizontal: 16,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  menuRowText: {
    fontSize: 16,
    textAlign: 'center',
  },
  footer: {
    padding: 16,
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(103, 80, 164, 0.12)',
  },
  footerRow: {
    paddingVertical: 6,
  },
  footerText: {
    fontSize: 16,
  },
});
