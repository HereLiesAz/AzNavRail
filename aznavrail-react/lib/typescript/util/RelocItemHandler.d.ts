import { AzNavItem } from '../types';
export declare class RelocItemHandler {
    static getCluster(items: AzNavItem[], hostId: string): AzNavItem[];
    static calculateTargetIndex(dy: number, currentIndex: number, clusterSize: number, itemHeight: number): number;
    static reorderItems(items: AzNavItem[], itemId: string, hostId: string, targetClusterIndex: number): AzNavItem[];
}
//# sourceMappingURL=RelocItemHandler.d.ts.map