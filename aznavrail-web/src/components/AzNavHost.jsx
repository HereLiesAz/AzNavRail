import React from 'react';
import AzNavRail from './AzNavRail';
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
        collapsedRailWidth = '80px'
    } = settings;

    // Apply padding to content to avoid overlap with the persistent rail strip
    const paddingSide = dockingSide === 'RIGHT' ? 'paddingRight' : 'paddingLeft';
    const paddingValue = collapsedRailWidth;

    return (
        <div className="az-nav-host">
            {/* Layer 0: Background */}
            <div className="az-nav-host-background">
                {background}
            </div>

            {/* Layer 1: Content (Onscreen) */}
            <div
                className="az-nav-host-content"
                style={{ [paddingSide]: paddingValue }}
            >
                {children}
            </div>

            {/* Layer 2: Rail */}
            <div className={`az-nav-host-rail-wrapper ${dockingSide === 'RIGHT' ? 'right' : 'left'}`}>
                 <AzNavRail {...railProps} />
            </div>
        </div>
    );
};

export default AzNavHost;
