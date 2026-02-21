package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Rect
import com.hereliesaz.aznavrail.model.AzNavItem

object RelocItemHandler {
    val itemBoundsCache = mutableMapOf<String, Rect>()

    fun findCluster(items: List<AzNavItem>, itemId: String): IntRange? {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return null

        val item = items[index]
        if (!item.isRelocItem || item.hostId == null) return null

        var start = index
        while (start > 0 && items[start - 1].isRelocItem && items[start - 1].hostId == item.hostId) {
            start--
        }

        var end = index
        while (end < items.size - 1 && items[end + 1].isRelocItem && items[end + 1].hostId == item.hostId) {
            end++
        }

        return start..end
    }

    fun updateOrder(items: MutableList<AzNavItem>, draggedId: String, targetIndex: Int) {
        val currentIndex = items.indexOfFirst { it.id == draggedId }
        if (currentIndex == -1 || currentIndex == targetIndex) return

        val cluster = findCluster(items, draggedId) ?: return
        if (targetIndex !in cluster) return

        val item = items.removeAt(currentIndex)
        items.add(targetIndex, item)
    }

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

        val draggedCenter = if (isVertical) {
            draggedBounds.center.y + currentDragOffset
        } else {
            draggedBounds.center.x + currentDragOffset
        }

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
