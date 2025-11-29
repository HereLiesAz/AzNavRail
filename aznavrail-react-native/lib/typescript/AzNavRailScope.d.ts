import React from 'react';
import { AzNavItem, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps } from './types';
export declare const AzNavRailContext: React.Context<{
    register: (item: AzNavItem) => void;
    unregister: (id: string) => void;
} | null>;
export declare const AzRailItem: React.FC<AzNavItemProps>;
export declare const AzMenuItem: React.FC<AzNavItemProps>;
export declare const AzRailToggle: React.FC<AzToggleProps>;
export declare const AzMenuToggle: React.FC<AzToggleProps>;
export declare const AzRailCycler: React.FC<AzCyclerProps>;
export declare const AzMenuCycler: React.FC<AzCyclerProps>;
export declare const AzDivider: React.FC;
export declare const AzRailHostItem: React.FC<AzHostItemProps>;
export declare const AzMenuHostItem: React.FC<AzHostItemProps>;
export declare const AzRailSubItem: React.FC<AzSubItemProps>;
export declare const AzMenuSubItem: React.FC<AzSubItemProps>;
export declare const AzRailSubToggle: React.FC<AzSubToggleProps>;
export declare const AzMenuSubToggle: React.FC<AzSubToggleProps>;
export declare const AzRailSubCycler: React.FC<AzSubCyclerProps>;
export declare const AzMenuSubCycler: React.FC<AzSubCyclerProps>;
//# sourceMappingURL=AzNavRailScope.d.ts.map