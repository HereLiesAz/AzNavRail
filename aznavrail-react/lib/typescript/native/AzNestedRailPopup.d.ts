import React from 'react';
import { AzNavItem, AzNestedRailAlignment } from '../types';
interface AzNestedRailPopupProps {
    visible: boolean;
    onDismiss: () => void;
    items: AzNavItem[];
    alignment: AzNestedRailAlignment;
    renderItem: (item: AzNavItem, index: number) => React.ReactNode;
    anchorPosition?: {
        x: number;
        y: number;
        width: number;
        height: number;
    };
    dockingSide: 'LEFT' | 'RIGHT';
    helpList?: Record<string, string>;
}
export declare const AzNestedRailPopup: React.FC<AzNestedRailPopupProps>;
export {};
//# sourceMappingURL=AzNestedRailPopup.d.ts.map