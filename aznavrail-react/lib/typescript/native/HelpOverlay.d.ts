import React from 'react';
import { AzNavItem } from '../types';
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
}
export declare const HelpOverlay: React.FC<HelpOverlayProps>;
export {};
//# sourceMappingURL=HelpOverlay.d.ts.map