import React from 'react';
import { AzNavItem, AzTutorial } from '../types';
interface HelpOverlayProps {
    items: AzNavItem[];
    onDismiss: () => void;
    helpList: Record<string, string>;
    itemBounds: Record<string, {
        x: number;
        y: number;
        width: number;
        height: number;
    }>;
    nestedRailVisibleId?: string | null;
    tutorials?: Record<string, AzTutorial>;
}
export declare const HelpOverlay: React.FC<HelpOverlayProps>;
export {};
//# sourceMappingURL=HelpOverlay.d.ts.map