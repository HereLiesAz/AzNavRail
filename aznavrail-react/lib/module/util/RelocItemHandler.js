export class RelocItemHandler {
  static getCluster(items, hostId) {
    const cluster = [];
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
  static calculateTargetIndex(dy, currentIndex, clusterSize, itemHeight) {
    const slotsMoved = Math.round(dy / itemHeight);
    const target = currentIndex + slotsMoved;
    return Math.max(0, Math.min(target, clusterSize - 1));
  }
  static reorderItems(items, itemId, hostId, targetClusterIndex) {
    // Find cluster start and length directly
    let clusterStartGlobalIndex = -1;
    let clusterSize = 0;
    let currentClusterIndex = -1;
    for (let i = 0; i < items.length; i++) {
      const item = items[i];
      if (item.hostId === hostId) {
        if (item.isRelocItem) {
          if (clusterStartGlobalIndex === -1) {
            clusterStartGlobalIndex = i;
          }
          if (item.id === itemId) {
            currentClusterIndex = clusterSize;
          }
          clusterSize++;
        } else if (clusterStartGlobalIndex !== -1) {
          break;
        }
      } else if (clusterStartGlobalIndex !== -1) {
        break;
      }
    }
    if (clusterStartGlobalIndex === -1 || targetClusterIndex < 0 || targetClusterIndex >= clusterSize || currentClusterIndex === -1 || currentClusterIndex === targetClusterIndex) {
      return items;
    }
    const newItems = [...items];
    const [movedItem] = newItems.splice(clusterStartGlobalIndex + currentClusterIndex, 1);
    newItems.splice(clusterStartGlobalIndex + targetClusterIndex, 0, movedItem);
    return newItems;
  }
}
//# sourceMappingURL=RelocItemHandler.js.map