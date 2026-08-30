import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import {
  View,
  Text,
  Image,
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
import {
  AzButtonShape,
  AzDockingSide,
  AzDropdownDesign,
  AzDropdownTrigger,
  AzDropdownTriggerPlacement,
  AzEasing,
  AzEntrance,
  AzExit,
  AzHeaderIconShape,
} from '../types';
import { AzNavRailDefaults, AzMotion } from '../AzNavRailDefaults';
import { AboutOverlay } from './AboutOverlay';
import { AzFooterLabel } from './AzFooterLabel';
import { AzAboutSurface, useAzAboutOwnership } from '../services/aboutPresence';
import { useAzAboutWarmup } from '../services/aboutPrefetch';
import { AzKineticItem, useAzClosing } from './AzKinetics';
import { solveHybridJustify } from '../util/AzJustify';
import { useAzAccent, useAzPanelColor, azReadableOn } from '../AzRailPalette';
import { isSafeExternalUrl } from '../util/AzSafeUrl';

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
  /** The panel's resolved, legible accent — what an item that declared no colour of its own wears. */
  panelAccent: string;
}
const AzDropdownMenuContext = createContext<AzDropdownMenuContextValue | null>(
  null
);

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
  /** Clip shape for the `AppIcon` trigger (mirrors the rail's `headerIconShape`). */
  headerIconShape?: AzHeaderIconShape;
  /** Diameter (px) of the `AppIcon` trigger (mirrors the rail's `headerIconSize`). */
  headerIconSize?: number;
  /**
   * What the user taps to open the menu. Defaults to `AzDropdownTrigger.MoreVert` — the three
   * vertical dots. Pass `AzDropdownTrigger.Text('Filter')` for a word, `AzDropdownTrigger.Icon(...)`
   * for your own glyph/image, `AzDropdownTrigger.Hamburger` for the bars, or
   * `AzDropdownTrigger.AppIcon` for the launcher icon (the pre-trigger default).
   */
  trigger?: AzDropdownTrigger;
  /**
   * Where that trigger is drawn. React has no host-activity title row to lift a trigger into yet
   * (see `KNOWN_GAPS.md`), so `AUTO` and `TITLE` both currently resolve to `INLINE` — the trigger
   * always renders at the call site.
   */
  triggerPlacement?: AzDropdownTriggerPlacement;
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
  /**
   * When true (default) this footer drops its "About" row whenever a higher-ranked surface — a
   * declared `AzAboutRailItem`, or a rail's own expanded-menu footer — is already offering one, so
   * the app never shows About twice. False always draws it (and stops this menu suppressing
   * anyone else's).
   */
  dedupeAbout?: boolean;
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
 * One logical line of a MENU-design dropdown label. Split the label on `\n` and render one of
 * these per line: each line owns its own natural-width measurer, its own `charCount` fed to the
 * solver, and its own resolved `letterSpacing` + font scale. Prevents newline characters from
 * skewing the hybrid-justify math and keeps auto-wrap disabled without eating explicit breaks.
 */
const JustifiedDropdownLine: React.FC<{
  line: string;
  justify: boolean;
  containerWidth: number;
  textAlign: 'left' | 'right' | 'center';
  color: string;
  enabled: boolean;
  itemTextStyle?: object;
}> = ({
  line,
  justify,
  containerWidth,
  textAlign,
  color,
  enabled,
  itemTextStyle,
}) => {
  const [naturalWidth, setNaturalWidth] = useState(0);
  const onLabelLayout = (e: LayoutChangeEvent) => {
    const w = e.nativeEvent.layout.width;
    if (w && Math.abs(w - naturalWidth) > 0.5) setNaturalWidth(w);
  };
  let letterSpacing = 0;
  let fontScale = 1;
  if (justify && line.length >= 1 && containerWidth > 0 && naturalWidth > 0) {
    const solved = solveHybridJustify(
      naturalWidth,
      containerWidth,
      line.length,
      DROPDOWN_BASE_FONT_PX
    );
    fontScale = solved.scale;
    letterSpacing = solved.letterSpacing;
  }
  const scaledFontSize = DROPDOWN_BASE_FONT_PX * fontScale;
  return (
    <>
      {/* The natural-width measurer. It is a second copy of the same label, so it must be invisible
          to assistive tech as well as to the eye — otherwise a screen reader announces every menu
          row twice. */}
      <Text
        onLayout={onLabelLayout}
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={[
          styles.menuRowText,
          { position: 'absolute', opacity: 0, left: -9999, top: -9999 } as any,
          itemTextStyle,
        ]}
      >
        {line}
      </Text>
      <Text
        numberOfLines={1}
        style={[
          styles.menuRowText,
          {
            color,
            opacity: enabled ? 1 : 0.5,
            textAlign,
            letterSpacing,
            fontSize: scaledFontSize,
            alignSelf: 'stretch',
          },
          itemTextStyle,
        ]}
      >
        {line}
      </Text>
    </>
  );
};

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
  if (!ctx)
    throw new Error('AzDropdownItem must be used inside an <AzDropdownMenu>');
  const {
    dismiss,
    design,
    onNavigate,
    itemTextStyle,
    dockingSide: ctxDockingSide,
    menuItemAlignment: ctxMenuItemAlignment,
    justifyMenuItems: ctxJustifyMenuItems,
    panelAccent: railAccent,
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
        : ctxDockingSide === AzDockingSide.RIGHT
          ? 'right'
          : 'left';
    const onContainerLayout = (e: LayoutChangeEvent) => {
      const w = e.nativeEvent.layout.width;
      if (w && Math.abs(w - availableWidth) > 0.5) setAvailableWidth(w);
    };
    // Split up-front on `\n` so each line gets its own measurement + solve — the newline count
    // would otherwise skew both `naturalWidth` and `charCount` for multi-line labels.
    const lines = text.split('\n');
    // The host rail's accent when there is one, so a rail's own drop-down does not arrive in a
    // different colour from the rail.
    const rowColor = textColor || color || railAccent;
    return (
      <TouchableOpacity
        style={styles.menuRow}
        onPress={enabled ? press : undefined}
        onLayout={onContainerLayout}
        disabled={!enabled}
        accessibilityRole="button"
        accessibilityLabel={text}
      >
        <View style={{ flex: 1, flexDirection: 'column' }}>
          {lines.map((line, i) => (
            <JustifiedDropdownLine
              key={i}
              line={line}
              justify={justify}
              containerWidth={availableWidth}
              textAlign={textAlign}
              color={rowColor}
              enabled={enabled}
              itemTextStyle={itemTextStyle}
            />
          ))}
        </View>
      </TouchableOpacity>
    );
  }
  return (
    <AzButton
      text={text}
      onClick={press}
      shape={shape}
      enabled={enabled}
      color={color || railAccent}
      textColor={textColor}
      fillColor={fillColor}
    />
  );
};

interface Anchor {
  x: number;
  y: number;
  width: number;
  height: number;
}

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
  trigger = AzDropdownTrigger.MoreVert,
  triggerPlacement: _triggerPlacement = AzDropdownTriggerPlacement.AUTO,
  showFooter = true,
  appRepositoryUrl,
  inAppAbout = true,
  dedupeAbout = true,
  moreFromAzEnabled = true,
  moreFromAzJsonUrl = 'https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json',
  expanded,
  onExpandedChange,
  onNavigate,
  style,
  itemEntrance = AzEntrance.Turnstile,
  itemExit = AzExit.Turnstile,
  itemTextStyle,
  entranceStaggerMs = AzMotion.ItemStaggerMs,
  entranceDurationMs = AzMotion.ItemDurationMs,
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
  const [anchor, setAnchor] = useState<Anchor>({
    x: 0,
    y: 0,
    width: 0,
    height: 0,
  });
  const [panelSize, setPanelSize] = useState({ width: 0, height: 0 });
  // Full-screen About reader reachable from the dropdown footer (parity with the rail). The
  // "More from Az" carousel is reachable from within AboutOverlay itself.
  const [showAbout, setShowAbout] = useState(false);

  // Whether THIS menu is the surface that draws About. Claimed from the trigger's render (not the
  // panel's) so the answer doesn't flip every time the panel opens and closes.
  const ownsAbout = useAzAboutOwnership(
    AzAboutSurface.DROPDOWN_FOOTER,
    showFooter && design === AzDropdownDesign.MENU && !!appRepositoryUrl,
    dedupeAbout
  );

  // Warm the reader's content in the background so the page opens populated, not spinning.
  useAzAboutWarmup(
    inAppAbout ? appRepositoryUrl : undefined,
    moreFromAzEnabled,
    moreFromAzJsonUrl
  );

  const setOpen = (value: boolean) => {
    if (expanded === undefined) setInternalOpen(value);
    onExpandedChange?.(value);
  };

  const openMenu = () => {
    if (vibrate) Vibration.vibrate();
    // Open first. The panel's visibility must not be hostage to a measurement callback: if the
    // trigger has not been laid out yet, or the host does not implement `measureInWindow`, the
    // callback never fires and the menu simply never opens. The anchor refines the position when
    // the measurement does land, and the fallback placement holds until then.
    setOpen(true);
    triggerRef.current?.measureInWindow((x, y, width, height) => {
      setAnchor({ x, y, width, height });
    });
  };

  // Reactive so the panel re-positions on orientation / split-screen changes.
  const screen = useWindowDimensions();
  // The panel's own colour, resolved once for the whole drop-down so the rows, the divider and the
  // footer are guaranteed to agree.
  const panelColor = useAzPanelColor();
  // The rail's accent, checked against the panel it lands on. A menu whose words disappear into its
  // own background is not a menu.
  const railAccent = useAzAccent();
  const panelAccent = azReadableOn(panelColor, railAccent);
  const panelWidth =
    design === AzDropdownDesign.RAIL ? collapsedWidth : expandedWidth;
  const triggerSize = headerIconSize;
  // Clip radius mirrors the rail's header icon: circle = half, rounded = 8, anything else = 0.
  const triggerRadius =
    headerIconShape === AzHeaderIconShape.ROUNDED
      ? 8
      : headerIconShape === AzHeaderIconShape.CIRCLE
        ? triggerSize / 2
        : 0;

  const panelPosition: ViewStyle = { position: 'absolute' };
  // Horizontal: pin to the physical docking-side edge.
  panelPosition.left =
    dockingSide === AzDockingSide.RIGHT ? screen.width - panelWidth : 0;
  // Vertical: drop from the trigger — downward when it fits below, otherwise upward.
  const fitsBelow =
    anchor.y + anchor.height + panelSize.height <= screen.height;
  panelPosition.top = fitsBelow
    ? anchor.y + anchor.height
    : Math.max(0, anchor.y - panelSize.height);

  const onPanelLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width !== panelSize.width || height !== panelSize.height)
      setPanelSize({ width, height });
  };

  // Keep the panel mounted through the staggered exit so items can animate out before teardown.
  const items = React.Children.toArray(children);
  const rendered = useAzClosing(
    isOpen,
    itemExit,
    items.length,
    entranceStaggerMs,
    entranceDurationMs
  );

  const renderTriggerContent = () => {
    switch (trigger.kind) {
      case 'MoreVert':
        return (
          <Text style={{ fontSize: 22, color: panelAccent, lineHeight: 22 }}>
            {'⋮'}
          </Text>
        );
      case 'Hamburger':
        return (
          <Text style={{ fontSize: 20, color: panelAccent, lineHeight: 20 }}>
            {'☰'}
          </Text>
        );
      case 'Text':
        return (
          <Text
            style={{ fontSize: 16, color: panelAccent, fontWeight: '600' }}
            numberOfLines={1}
          >
            {trigger.text}
          </Text>
        );
      case 'Icon':
        return typeof trigger.model === 'string' ? (
          <Image
            source={{ uri: trigger.model }}
            style={{ width: triggerSize, height: triggerSize }}
            resizeMode="contain"
          />
        ) : (
          (trigger.model as React.ReactNode)
        );
      case 'AppIcon':
      default:
        // The launcher-icon look — drawn like the rail's header icon (gray placeholder).
        return (
          <View
            style={{
              width: triggerSize,
              height: triggerSize,
              borderRadius: triggerRadius,
              backgroundColor: 'gray',
              overflow: 'hidden',
            }}
          />
        );
    }
  };

  return (
    <View style={style}>
      <TouchableOpacity
        ref={triggerRef as React.RefObject<any>}
        onPress={() => (isOpen ? setOpen(false) : openMenu())}
        accessibilityRole="button"
        accessibilityLabel="Menu"
      >
        <View
          testID="az-dropdown-trigger"
          style={{
            // Automatic breathing room around the trigger (parity with the rail header).
            margin: 8,
            minWidth: triggerSize,
            minHeight: triggerSize,
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {renderTriggerContent()}
        </View>
      </TouchableOpacity>

      {rendered && (
        <Modal
          visible
          transparent
          animationType="fade"
          onRequestClose={() => setOpen(false)}
        >
          <Pressable
            style={[
              StyleSheet.absoluteFill,
              dimBehindMenu
                ? {
                    backgroundColor: `rgba(0,0,0,${Math.max(0, Math.min(1, dimBehindMenuAlpha))})`,
                  }
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
                {
                  maxHeight: screen.height * 0.8,
                  width: panelWidth,
                  // The rail's own panel colour, drawn opaque (see useAzPanelColor). The hardcoded
                  // white this replaces was the app theme's UI, not the rail's — and on a dark rail
                  // it was somebody else's.
                  backgroundColor: panelColor,
                },
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
                  <AzDropdownMenuContext.Provider
                    value={{
                      dismiss: () => setOpen(false),
                      design,
                      onNavigate,
                      itemTextStyle,
                      dockingSide,
                      menuItemAlignment,
                      justifyMenuItems,
                      panelAccent,
                    }}
                  >
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
                      panelAccent={panelAccent}
                      showAbout={ownsAbout}
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
  /** The panel's resolved, legible accent — the same colour its item rows wear. */
  panelAccent: string;
  /** False when a higher-ranked surface already offers About — see `aboutPresence`. */
  showAbout?: boolean;
  visible?: boolean;
  menuItemCount?: number;
  staggerMs?: number;
  durationMs?: number;
}> = ({
  appRepositoryUrl,
  inAppAbout = true,
  onInAppAbout,
  panelAccent,
  showAbout = true,
  visible = true,
  menuItemCount = 0,
  staggerMs = AzMotion.ItemStaggerMs,
  durationMs = AzMotion.ItemDurationMs,
}) => {
  // Only open safe schemes — this also runs on the web via react-native-web, where a `javascript:`
  // URL would otherwise execute.
  const open = (url: string) => {
    if (isSafeExternalUrl(url)) Linking.openURL(url).catch(() => {});
  };
  const onAbout = () => {
    if (!appRepositoryUrl) return;
    if (inAppAbout) onInAppAbout?.();
    else open(appRepositoryUrl);
  };

  // Always start collapsed so the first mount plays the fold-in animation.
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
    // Fold-up on close — no delay; the footer is the FIRST thing to go before items exit.
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
        ...({ transformOrigin: 'top center' } as any),
      }}
    >
      <View style={styles.footer}>
        {/* About is hidden entirely when no repository URL is configured, and when a higher-ranked
            surface is already offering one. */}
        {!!appRepositoryUrl && showAbout && (
          <AzFooterLabel
            text="About"
            color={panelAccent}
            onPress={onAbout}
            style={styles.footerRow}
          />
        )}
        <AzFooterLabel
          text="Feedback"
          color={panelAccent}
          onPress={() => open('mailto:hereliesaz@gmail.com?subject=Feedback')}
          style={styles.footerRow}
        />
        <AzFooterLabel
          text="@HereLiesAz"
          color={panelAccent}
          onPress={() => open('https://instagram.com/HereLiesAz')}
          style={styles.footerRow}
        />
      </View>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  panel: {
    borderRadius: 12,
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
