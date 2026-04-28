import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { View, StyleSheet, Dimensions, Text } from 'react-native';
import { AzNavRail } from './AzNavRail';
import { AzDockingSide, AzNavRailSettings } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';

// Types for alignment mimicking Compose Alignment
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

interface AzBackgroundItem {
  id: string;
  weight: number;
  content: React.ReactNode;
}

interface AzOnscreenItem {
  id: string;
  alignment: AzAlignment;
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

export const AzHostContext = createContext<AzHostContextType | null>(null);

export const useAzHostContext = () => useContext(AzHostContext);

// --- Component Wrappers for the Host Scope ---

export const AzBackground: React.FC<{ weight?: number; children: React.ReactNode }> = ({ weight = 0, children }) => {
  const context = useAzHostContext();
  const id = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  useEffect(() => {
    if (context) {
      context.registerBackground({ id, weight, content: children });
      return () => context.unregisterBackground(id);
    }
  }, [context, id, weight, children]);

  return null;
};

export const AzOnscreen: React.FC<{ alignment?: AzAlignment; children: React.ReactNode }> = ({ alignment = AzAlignment.TopStart, children }) => {
  const context = useAzHostContext();
  const id = useMemo(() => Math.random().toString(36).substr(2, 9), []);

  useEffect(() => {
    if (context) {
      context.registerOnscreen({ id, alignment, content: children });
      return () => context.unregisterOnscreen(id);
    }
  }, [context, id, alignment, children]);

  return null;
};

// --- Layout component ---

export interface AzHostActivityLayoutProps extends AzNavRailSettings {
  navController?: any;
  currentDestination?: string;
  isLandscape?: boolean;
  initiallyExpanded?: boolean;
  disableSwipeToOpen?: boolean;
  children: React.ReactNode;
}

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

export const AzHostActivityLayout: React.FC<AzHostActivityLayoutProps> = (props) => {
  const {
    navController,
    currentDestination,
    dockingSide = AzDockingSide.LEFT,
    children,
    ...railProps
  } = props;

  const [backgrounds, setBackgrounds] = useState<AzBackgroundItem[]>([]);
  const [onscreenItems, setOnscreenItems] = useState<AzOnscreenItem[]>([]);
  const [screenHeight, setScreenHeight] = useState(Dimensions.get('window').height);
  const [screenWidth, setScreenWidth] = useState(Dimensions.get('window').width);

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
        {[...backgrounds].sort((a, b) => a.weight - b.weight).map(bg => (
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
               <Text style={styles.titleText}>{currentTitle}</Text>
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
            {onscreenItems.map(item => (
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
