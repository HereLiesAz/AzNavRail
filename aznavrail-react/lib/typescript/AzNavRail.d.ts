import React from 'react';
import { AzNavRailSettings } from './types';
interface AzNavRailProps extends AzNavRailSettings {
    children: React.ReactNode;
    navController?: any;
    currentDestination?: string;
    isLandscape?: boolean;
    initiallyExpanded?: boolean;
    disableSwipeToOpen?: boolean;
    onExpandedChange?: (expanded: boolean) => void;
    onInteraction?: (action: string, details?: string) => void;
}
export declare const AzNavRail: React.FC<AzNavRailProps>;
export {};
//# sourceMappingURL=AzNavRail.d.ts.map