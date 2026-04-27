import React, { useMemo } from 'react';
import AzNavRail from './AzNavRail';
import { AzNavHostContext } from '../AzNavRail';
import './AzNavHost.css';

/**
 * A container component that manages the layout of the Navigation Rail,
 * background content, and onscreen content with safe zones.
 *
 * @param {object} railProps - Props to be passed to the AzNavRail component.
 * @param {React.ReactNode} background - Content rendered in the background (z-index 0).
 * @param {React.ReactNode} children - Main screen content (z-index 1), confined to safe zones.
 */
const AzNavHost = ({
    railProps = {},
    background,
    children
}) => {
    const settings = railProps.settings || {};
    const {
        dockingSide = 'LEFT',
        collapsedRailWidth = '100px'
    } = settings;

    const currentDestination = railProps.currentDestination;
    const items = railProps.content || [];

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

    const showTitle = currentTitle && currentTitle !== 'NO_TITLE';

    // Apply padding to content to avoid overlap with the persistent rail strip
    const paddingSide = dockingSide === 'RIGHT' ? 'paddingRight' : 'paddingLeft';
    const paddingValue = collapsedRailWidth;

    const titleAlignment = dockingSide === 'LEFT' ? 'flex-end' : 'flex-start';
    const titlePaddingSide = dockingSide === 'LEFT' ? { paddingRight: '32px' } : { paddingLeft: '32px' };

    return (
        <div className="az-nav-host">
            {/* Layer 0: Background */}
            <div className="az-nav-host-background">
                {background}
            </div>

            {/* Layer 1: Content (Onscreen) */}
            <div
                className="az-nav-host-content"
                style={{
                    [paddingSide]: paddingValue,
                }}
            >
                {showTitle && (
                    <div
                        className="az-nav-host-title"
                        style={{
                            display: 'flex',
                            position: 'absolute',
                            top: '10%',
                            left: 0,
                            right: 0,
                            height: '10%',
                            justifyContent: titleAlignment,
                            alignItems: 'center',
                            zIndex: 5,
                            ...titlePaddingSide
                        }}
                    >
                        <h1 style={{ fontSize: '48px', fontWeight: 'bold', margin: 0 }}>{currentTitle}</h1>
                    </div>
                )}
                <div style={{ marginTop: '10vh', marginBottom: '10vh', height: '80vh', overflowY: 'auto' }}>
                    {children}
                </div>
            </div>

            {/* Layer 2: Rail */}
            <div className={`az-nav-host-rail-wrapper ${dockingSide === 'RIGHT' ? 'right' : 'left'}`}>
                 <AzNavHostContext.Provider value={true}>
                     <AzNavRail {...railProps} />
                 </AzNavHostContext.Provider>
            </div>
        </div>
    );
};

export default AzNavHost;
