import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { View, StyleSheet, Dimensions, Text } from 'react-native';
import { AzNavRail } from './AzNavRail';
import { AzDockingSide, AzEntrance, AzNavItem, AzNavRailSettings } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';
import { AzKineticTitle } from './components/AzKinetics';

/** Alignment positions for `AzOnscreen` overlays, mirroring Jetpack Compose `Alignment` values. */
export enum AzAlignment {
  TopStart = 'TopStart',
  TopCenter = 'TopCenter',
  TopEnd = 'TopEnd',
  CenterStart = 'CenterStart',
  Center = 'Center',
  CenterEnd = 'CenterEnd',
  BottomStart = 'BottomStart',
  BottomCenter = 'BottomCenter',
  BottomEnd = 'BottomEnd',
}

/** Internal registration record for a background layer declared via `<AzBackground>`. */
interface AzBackgroundItem {
  id: string;
  weight: number;
  page: number;
  content: React.ReactNode;
}

/** Internal registration record for an overlay declared via `<AzOnscreen>`. */
interface AzOnscreenItem {
  id: string;
  alignment: AzAlignment;
  page: number;
  content: React.ReactNode;
}

interface AzHostContextType {
  registerBackground: (item: AzBackgroundItem) => void;
  unregisterBackground: (id: string) => void;
  registerOnscreen: (item: AzOnscreenItem) => void;
  unregisterOnscreen: (id: string) => void;
  navController: any;
  dockingSide: AzDockingSide;
}

/** Internal React context shared between `AzHostActivityLayout` and its `AzBackground`/`AzOnscreen` children. */
export const AzHostContext = createContext<AzHostContextType | null>(null);

/** Returns the `AzHostContextType` from the nearest `AzHostActivityLayout` ancestor, or `null` if outside one. */
export const useAzHostContext = () => useContext(AzHostContext);

// --- Component Wrappers for the Host Scope ---

/**
 * Registers a background layer inside `AzHostActivityLayout`.
 *
 * Backgrounds form their own "book" of pages beneath the onscreen book (see {@link AzOnscreen}).
 * @param props.weight - Tie-breaker Z-order within a page; lower values render behind higher values.
 * @param props.page - The page (Z-layer). Higher numbers render further back; decimals (e.g. `1.5`)
 *   insert a page between existing ones. Honoured when the host's `pagesEnabled` is `true` (default).
 * @param props.children - Content rendered as an absolute fill layer behind the rail and screen content.
 */
export const AzBackground: React.FC<{ weight?: number; page?: number; children: React.ReactNode }> = ({ weight = 0, page = 0, children }) => {
  const context = useAzHostContext();
  const id = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  useEffect(() => {
    if (context) {
      context.registerBackground({ id, weight, page, content: children });
      return () => context.unregisterBackground(id);
    }
    return undefined;
  }, [context, id, weight, page, children]);

  return null;
};

/**
 * Registers an absolutely-positioned overlay inside `AzHostActivityLayout`, respecting safe zones.
 *
 * Items sharing a `page` render co-planar (positioned via `alignment`); items on different pages are
 * stacked in Z and may overlap — a **higher** page number renders **further back**. Decimals insert
 * a page between existing ones. Honoured when the host's `pagesEnabled` is `true` (default).
 * @param props.alignment - Where the overlay is anchored relative to the content area.
 * @param props.page - The page (Z-layer); see above. Defaults to `0`.
 * @param props.children - Content rendered as an overlay at the specified alignment.
 */
export const AzOnscreen: React.FC<{ alignment?: AzAlignment; page?: number; children: React.ReactNode }> = ({ alignment = AzAlignment.TopStart, page = 0, children }) => {
  const context = useAzHostContext();
  const id = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  useEffect(() => {
    if (context) {
      context.registerOnscreen({ id, alignment, page, content: children });
      return () => context.unregisterOnscreen(id);
    }
    return undefined;
  }, [context, id, alignment, page, children]);

  return null;
};

// --- Layout component ---

/** Props for `AzHostActivityLayout` — the full-screen host container that positions the rail and content. */
export interface AzHostActivityLayoutProps extends AzNavRailSettings {
  /** Navigation controller reference forwarded to the embedded `AzNavRail`. */
  navController?: any;
  /** Active route string used to highlight the matching item and display the screen title. */
  currentDestination?: string;
  /** When true, applies landscape-aware layout adjustments to the rail. */
  isLandscape?: boolean;
  /** When true, the embedded rail starts in its expanded (menu) state. */
  initiallyExpanded?: boolean;
  /** When true, disables the swipe gesture that expands the rail. */
  disableSwipeToOpen?: boolean;
  /** Called whenever the rail transitions between collapsed and expanded states. */
  onExpandedChange?: (expanded: boolean) => void;
  /** Called whenever any rail item is interacted with. Receives the action name, an optional detail string, and the interacted item. */
  onInteraction?: (action: string, details?: string, item?: AzNavItem) => void;
  /**
   * Whether the pages Z-ordering system is active (default `true`). When on, the `page` of every
   * `AzBackground`/`AzOnscreen` is honoured and forced — items with no explicit page share page `0`.
   * When off, items render in declaration order (backgrounds by `weight`) and `page` is ignored.
   */
  pagesEnabled?: boolean;
  /** Screen content and any `AzBackground`/`AzOnscreen` declarations. */
  children: React.ReactNode;
}

/**
 * Orders the background "book" back-to-front (first element is drawn first / backmost). When
 * `pagesEnabled`, a higher page draws further back, with `weight` breaking ties within a page;
 * otherwise falls back to the legacy weight sort.
 */
export const orderBackgrounds = (items: AzBackgroundItem[], pagesEnabled: boolean): AzBackgroundItem[] =>
  pagesEnabled
    ? [...items].sort((a, b) => (b.page - a.page) || (a.weight - b.weight))
    : [...items].sort((a, b) => a.weight - b.weight);

/**
 * Orders the onscreen "book" back-to-front (first element is drawn first / backmost). When
 * `pagesEnabled`, a higher page draws further back, with declaration order preserved within a page
 * (stable sort); otherwise declaration order is preserved unchanged.
 */
export const orderOnscreen = (items: AzOnscreenItem[], pagesEnabled: boolean): AzOnscreenItem[] =>
  pagesEnabled ? [...items].sort((a, b) => b.page - a.page) : items;

const getAlignmentStyle = (alignment: AzAlignment, dockingSide: AzDockingSide) => {
  // Mirror logic for right docking
  let finalAlignment = alignment;
  if (dockingSide === AzDockingSide.RIGHT) {
    if (alignment === AzAlignment.TopStart) finalAlignment = AzAlignment.TopEnd;
    else if (alignment === AzAlignment.TopEnd) finalAlignment = AzAlignment.TopStart;
    else if (alignment === AzAlignment.CenterStart) finalAlignment = AzAlignment.CenterEnd;
    else if (alignment === AzAlignment.CenterEnd) finalAlignment = AzAlignment.CenterStart;
    else if (alignment === AzAlignment.BottomStart) finalAlignment = AzAlignment.BottomEnd;
    else if (alignment === AzAlignment.BottomEnd) finalAlignment = AzAlignment.BottomStart;
  }

  const style: any = { position: 'absolute' };
  switch (finalAlignment) {
    case AzAlignment.TopStart: style.top = 0; style.left = 0; break;
    case AzAlignment.TopCenter: style.top = 0; style.alignSelf = 'center'; break;
    case AzAlignment.TopEnd: style.top = 0; style.right = 0; break;
    case AzAlignment.CenterStart: style.top = '50%'; style.left = 0; style.transform = [{ translateY: -50 }]; break;
    case AzAlignment.Center: style.top = '50%'; style.left = '50%'; style.transform = [{ translateX: '-50%' }, { translateY: '-50%' }]; break;
    case AzAlignment.CenterEnd: style.top = '50%'; style.right = 0; style.transform = [{ translateY: -50 }]; break;
    case AzAlignment.BottomStart: style.bottom = 0; style.left = 0; break;
    case AzAlignment.BottomCenter: style.bottom = 0; style.alignSelf = 'center'; break;
    case AzAlignment.BottomEnd: style.bottom = 0; style.right = 0; break;
  }
  return style;
};

/**
 * Full-screen layout container that composes `AzNavRail` with layered background and onscreen overlay slots.
 * Mirrors the Jetpack Compose `AzNavHost` + `AzHostActivityLayout` API.
 */
export const AzHostActivityLayout: React.FC<AzHostActivityLayoutProps> = (props) => {
  const {
    navController,
    currentDestination,
    dockingSide = AzDockingSide.LEFT,
    pagesEnabled = true,
    children,
    ...railProps
  } = props;

  const [backgrounds, setBackgrounds] = useState<AzBackgroundItem[]>([]);
  const [onscreenItems, setOnscreenItems] = useState<AzOnscreenItem[]>([]);
  const [screenHeight, setScreenHeight] = useState(Dimensions.get('window').height);
  // Only the setter is used (width is tracked for the resize listener); skip the unread value.
  const [, setScreenWidth] = useState(Dimensions.get('window').width);

  useEffect(() => {
    const sub = Dimensions.addEventListener('change', ({ window }) => {
      setScreenHeight(window.height);
      setScreenWidth(window.width);
    });
    return () => sub?.remove();
  }, []);

  const registerBackground = useCallback((item: AzBackgroundItem) => {
    setBackgrounds(prev => {
      const idx = prev.findIndex(i => i.id === item.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = item;
        return next;
      }
      return [...prev, item];
    });
  }, []);

  const unregisterBackground = useCallback((id: string) => {
    setBackgrounds(prev => prev.filter(i => i.id !== id));
  }, []);

  const registerOnscreen = useCallback((item: AzOnscreenItem) => {
    setOnscreenItems(prev => {
      const idx = prev.findIndex(i => i.id === item.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = item;
        return next;
      }
      return [...prev, item];
    });
  }, []);

  const unregisterOnscreen = useCallback((id: string) => {
    setOnscreenItems(prev => prev.filter(i => i.id !== id));
  }, []);

  const hostContext: AzHostContextType = useMemo(() => ({
    registerBackground,
    unregisterBackground,
    registerOnscreen,
    unregisterOnscreen,
    navController,
    dockingSide,
  }), [registerBackground, unregisterBackground, registerOnscreen, unregisterOnscreen, navController, dockingSide]);

  // Safe zones (10% top, 10% bottom as per Android)
  const safeTop = screenHeight * 0.1;
  const safeBottom = screenHeight * 0.1;
  const collapsedRailWidth = railProps.collapsedRailWidth || AzNavRailDefaults.CollapsedRailWidth;

  // Title rendering logic (stubbed to use currentDestination for parity until Context extraction is possible)
  const currentTitle = currentDestination || '';
  const titleTop = screenHeight * 0.1;
  const titleHeight = screenHeight * 0.1;

  return (
    <AzHostContext.Provider value={hostContext}>
      <View style={styles.container}>
        {/* Backgrounds */}
        {orderBackgrounds(backgrounds, pagesEnabled).map(bg => (
          <View key={bg.id} style={StyleSheet.absoluteFillObject} pointerEvents="box-none">
            {bg.content}
          </View>
        ))}

        {/* Title rendering */}
        {currentTitle ? (
           <View style={[
               styles.titleContainer,
               { top: titleTop, height: titleHeight },
               dockingSide === AzDockingSide.LEFT ? { right: 32, alignItems: 'flex-end' } : { left: 32, alignItems: 'flex-start' }
           ]} pointerEvents="none">
               {/* Keyed on the title so the WP7 sweep replays each time the active screen changes. */}
               <AzKineticTitle
                   key={currentTitle}
                   title={currentTitle}
                   entrance={(railProps as AzNavRailSettings | undefined)?.titleEntrance ?? AzEntrance.Turnstile}
                   dockingSide={dockingSide}
               >
                   <Text style={[styles.titleText, (railProps as AzNavRailSettings | undefined)?.titleTextStyle]}>{currentTitle}</Text>
               </AzKineticTitle>
           </View>
        ) : null}

        {/* Onscreen Fragments */}
        <View style={[
            StyleSheet.absoluteFillObject,
            {
              paddingTop: safeTop,
              paddingBottom: safeBottom,
              paddingLeft: dockingSide === AzDockingSide.LEFT ? collapsedRailWidth : 0,
              paddingRight: dockingSide === AzDockingSide.RIGHT ? collapsedRailWidth : 0,
            }
        ]} pointerEvents="box-none">
            {orderOnscreen(onscreenItems, pagesEnabled).map(item => (
                <View key={item.id} style={getAlignmentStyle(item.alignment, dockingSide)} pointerEvents="box-none">
                    {item.content}
                </View>
            ))}
        </View>

        {/* AzNavRail and standard children */}
        <AzNavRail
            {...railProps}
            dockingSide={dockingSide}
            navController={navController}
            currentDestination={currentDestination}
        >
            {children}
        </AzNavRail>
      </View>
    </AzHostContext.Provider>
  );
};

// Generic NavHost wrapper (React Native doesn't have an exact Compose equivalent natively,
// but we provide the component for parity).
/** Generic nav-host wrapper provided for API parity with the Compose library; renders children in an absolute fill view. */
export const AzNavHost: React.FC<{ children?: React.ReactNode }> = ({ children }) => {
  return <View style={StyleSheet.absoluteFillObject}>{children}</View>;
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    width: '100%',
    height: '100%',
  },
  titleContainer: {
    position: 'absolute',
    width: '100%',
    justifyContent: 'center',
    zIndex: 2,
  },
  titleText: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#000',
  }
});
