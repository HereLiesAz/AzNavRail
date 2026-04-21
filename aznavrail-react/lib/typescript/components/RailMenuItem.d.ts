import React from 'react';
import { AzNavItem } from '../types';
interface RailMenuItemProps {
    item: AzNavItem;
    depth: number;
    isExpandedHost: boolean;
    onToggleHost: () => void;
    onItemClick: () => void;
    renderSubItems: () => React.ReactNode;
}
export declare const RailMenuItem: React.FC<RailMenuItemProps>;
export {};
//# sourceMappingURL=RailMenuItem.d.ts.map