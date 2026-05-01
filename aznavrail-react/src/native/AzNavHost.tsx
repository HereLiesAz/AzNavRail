import React, { useMemo } from 'react';
import { View, Text, StyleSheet, Dimensions, Platform, StatusBar } from 'react-native';
import { AzNavRail, AzNavHostContext } from '../AzNavRail';
import { AzNavRailDefaults } from '../AzNavRailDefaults';
import { AzDockingSide, AzNavItem } from '../types';

/** Props for the native `AzNavHost` layout component. */
interface AzNavHostProps {
  /** All props forwarded directly to the embedded `AzNavRail`. */
  railProps: any;
  /** Optional background content rendered behind the rail and screen content. */
  background?: React.ReactNode;
  /** Screen content rendered in the content area beside the rail. */
  children: React.ReactNode;
}

/** Native implementation: Full-screen host layout that layers a background, content area, and the `AzNavRail`. */
const AzNavHost: React.FC<AzNavHostProps> = ({ railProps, background, children }) => {
  const settings = railProps.settings || {};
  const dockingSide = settings.dockingSide || AzDockingSide.LEFT;
  const collapsedWidth = settings.collapsedRailWidth || AzNavRailDefaults.CollapsedRailWidth;

  const { width: screenWidth, height: screenHeight } = Dimensions.get('window');

  // Safe zones: top 10%, bottom 10%
  const safeTop = screenHeight * 0.1;
  const safeBottom = screenHeight * 0.1;

  // Header Title Logic (Parity with Android)
  const currentDestination = railProps.currentDestination;
  const items: AzNavItem[] = railProps.content || [];

  const currentActiveItem = items.find(item =>
    (item.route && item.route === currentDestination) ||
    (item.id === currentDestination)
  );

  let currentTitle = '';
  if (currentActiveItem) {
    if (currentActiveItem.isToggle) {
        currentTitle = currentActiveItem.isChecked ? currentActiveItem.toggleOnText : currentActiveItem.toggleOffText;
    } else if (currentActiveItem.isCycler) {
        currentTitle = currentActiveItem.selectedOption || currentActiveItem.text;
    } else {
        currentTitle = currentActiveItem.screenTitle || currentActiveItem.text;
    }
  }

  const showTitle = currentTitle && currentTitle !== AzNavRailDefaults.NO_TITLE;

  const titleAlignment = dockingSide === AzDockingSide.LEFT ? 'flex-end' : 'flex-start';
  const titlePaddingSide = dockingSide === AzDockingSide.LEFT ? { paddingRight: 32 } : { paddingLeft: 32 };

  return (
    <View style={styles.container}>
      {/* Layer 0: Background */}
      <View style={StyleSheet.absoluteFill}>
        {background}
      </View>

      {/* Layer 1: Content with Title and Safe Zones */}
      <View style={[
          styles.contentWrapper,
          {
              paddingLeft: dockingSide === AzDockingSide.LEFT ? collapsedWidth : 0,
              paddingRight: dockingSide === AzDockingSide.RIGHT ? collapsedWidth : 0,
          }
      ]}>
        {showTitle && (
            <View style={[styles.titleContainer, { alignItems: titleAlignment }, titlePaddingSide]}>
                <Text style={styles.titleText}>{currentTitle}</Text>
            </View>
        )}
        <View style={[styles.onscreenContent, { marginTop: safeTop, marginBottom: safeBottom }]}>
            {children}
        </View>
      </View>

      {/* Layer 2: Rail */}
      <View style={[styles.railWrapper, dockingSide === AzDockingSide.RIGHT ? { right: 0 } : { left: 0 }]}>
        <AzNavHostContext.Provider value={true}>
          <AzNavRail {...railProps} />
        </AzNavHostContext.Provider>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  contentWrapper: {
    flex: 1,
  },
  titleContainer: {
    position: 'absolute',
    top: '10%',
    width: '100%',
    height: '10%',
    justifyContent: 'center',
    zIndex: 5,
  },
  titleText: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#333',
  },
  onscreenContent: {
    flex: 1,
  },
  railWrapper: {
    position: 'absolute',
    top: 0,
    bottom: 0,
    zIndex: 10,
  },
});

export default AzNavHost;
