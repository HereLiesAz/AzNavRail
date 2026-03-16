
export const RelocItemHandler = {
  findCluster: (items, itemId, knownIndex = -1) => {
    let index = knownIndex;
    if (index === -1 || index >= items.length || items[index].id !== itemId) {
        index = items.findIndex(it => it.id === itemId);
    }
    if (index === -1) return null;

    const item = items[index];
    if (!item.isRelocItem || !item.hostId) return null;

    let start = index;
    while (start > 0) {
      const prev = items[start - 1];
      if (prev.isRelocItem && prev.hostId === item.hostId) {
        start--;
      } else {
        break;
      }
    }

    let end = index;
    while (end < items.length - 1) {
      const next = items[end + 1];
      if (next.isRelocItem && next.hostId === item.hostId) {
        end++;
      } else {
        break;
      }
    }

    return { start, end };
  },

  updateOrder: (items, draggedId, targetIndex, knownCurrentIndex = -1) => {
    const currentIndex = knownCurrentIndex !== -1 ? knownCurrentIndex : items.findIndex(it => it.id === draggedId);
    if (currentIndex === -1 || currentIndex === targetIndex) return items;

    // Check cluster
    const cluster = RelocItemHandler.findCluster(items, draggedId, currentIndex);
    if (!cluster || targetIndex < cluster.start || targetIndex > cluster.end) return items;

    const newItems = [...items];
    const [item] = newItems.splice(currentIndex, 1);
    newItems.splice(targetIndex, 0, item);
    return newItems;
  }
};
