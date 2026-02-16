package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Rect
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Helper object to manage RelocItem interactions.
 */
object RelocItemHandler {

    /**
     * Finds the contiguous cluster of RelocItems surrounding the given item.
     * Returns the start and end indices (inclusive) in the list.
     *
     * @param items The list of all items.
     * @param itemId The ID of the item to search around.
     * @return The range of indices for the cluster, or null if not found.
     */
    fun findCluster(items: List<AzNavItem>, itemId: String): IntRange? {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return null

        val item = items[index]
        if (!item.isRelocItem || item.hostId == null) return null

        var start = index
        while (start > 0) {
            val prev = items[start - 1]
            if (prev.isRelocItem && prev.hostId == item.hostId) {
                start--
            } else {
                break
            }
        }

        var end = index
        while (end < items.lastIndex) {
            val next = items[end + 1]
            if (next.isRelocItem && next.hostId == item.hostId) {
                end++
            } else {
                break
            }
        }

        return start..end
    }

    /**
     * Swaps items in the mutable list to reflect the drag operation.
     *
     * @param items The mutable list of items.
     * @param draggedId The ID of the item being dragged.
     * @param targetIndex The index where the dragged item should be.
     */
    fun updateOrder(items: MutableList<AzNavItem>, draggedId: String, targetIndex: Int) {
        val currentIndex = items.indexOfFirst { it.id == draggedId }
        if (currentIndex == -1) return
        if (currentIndex == targetIndex) return

        // Validation: Target must be within the cluster.
        val cluster = findCluster(items, draggedId) ?: return
        if (targetIndex !in cluster) return

        val item = items.removeAt(currentIndex)
        items.add(targetIndex, item)
    }

    /**
     * Calculates the target index for a dragged item based on its current position.
     *
     * @param items The list of items.
     * @param draggedItemId The ID of the item being dragged.
     * @param currentDragOffset The current drag offset in pixels.
     * @param itemBounds A map of item IDs to their bounds.
     * @param isVertical Whether the layout is vertical.
     * @return The target index, or null if no valid target.
     */
    fun calculateTargetIndex(
        items: List<AzNavItem>,
        draggedItemId: String,
        currentDragOffset: Float,
        itemBounds: Map<String, Rect>,
        isVertical: Boolean
    ): Int? {
        val currentIndex = items.indexOfFirst { it.id == draggedItemId }
        if (currentIndex == -1) return null

        val draggedBounds = itemBounds[draggedItemId] ?: return null
        val cluster = findCluster(items, draggedItemId) ?: return null

        // Calculate the center of the dragged item
        val draggedCenter = if (isVertical) {
            draggedBounds.center.y + currentDragOffset
        } else {
            draggedBounds.center.x + currentDragOffset
        }

        // Find which item in the cluster contains this center point
        // If the center is between items, we snap to the closest one.

        var bestTarget = currentIndex
        var minDistance = Float.MAX_VALUE

        for (i in cluster) {
            val item = items[i]
            val bounds = itemBounds[item.id]
            if (bounds != null) {
                val itemCenter = if (isVertical) bounds.center.y else bounds.center.x
                val distance = kotlin.math.abs(draggedCenter - itemCenter)

                if (distance < minDistance) {
                    minDistance = distance
                    bestTarget = i
                }
            }
        }

        return bestTarget
    }
}
