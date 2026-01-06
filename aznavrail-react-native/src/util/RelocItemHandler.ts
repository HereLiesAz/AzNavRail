import { AzNavItem } from '../types';

export class RelocItemHandler {
    static getCluster(items: AzNavItem[], hostId: string): AzNavItem[] {
        const cluster: AzNavItem[] = [];
        let inCluster = false;

        for (const item of items) {
            if (item.hostId === hostId) {
                if (item.isRelocItem) {
                    inCluster = true;
                    cluster.push(item);
                } else if (inCluster) {
                    // Break cluster if we encounter a non-reloc sibling under the same host
                    break;
                }
            } else if (inCluster) {
                 // Break cluster if we leave the host
                 break;
            }
        }
        return cluster;
    }

    static calculateTargetIndex(dy: number, currentIndex: number, clusterSize: number, itemHeight: number): number {
        const slotsMoved = Math.round(dy / itemHeight);
        const target = currentIndex + slotsMoved;
        return Math.max(0, Math.min(target, clusterSize - 1));
    }

    static reorderItems(items: AzNavItem[], itemId: string, hostId: string, targetClusterIndex: number): AzNavItem[] {
        const cluster = this.getCluster(items, hostId);
        if (targetClusterIndex < 0 || targetClusterIndex >= cluster.length) return items;

        const currentClusterIndex = cluster.findIndex(i => i.id === itemId);
        if (currentClusterIndex === -1 || currentClusterIndex === targetClusterIndex) return items;

        // Create new order within cluster
        const newCluster = [...cluster];
        const [movedItem] = newCluster.splice(currentClusterIndex, 1);
        newCluster.splice(targetClusterIndex, 0, movedItem);

        // Map back to main list
        // We need to find where the cluster starts in the main list
        const clusterStartGlobalIndex = items.findIndex(i => i.id === cluster[0].id);

        if (clusterStartGlobalIndex === -1) return items;

        const newItems = [...items];
        // Replace the cluster segment
        newItems.splice(clusterStartGlobalIndex, cluster.length, ...newCluster);

        return newItems;
    }
}
