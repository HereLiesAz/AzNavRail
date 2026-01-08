package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag

/**
 * Helper object to manage RelocItem interactions.
 */
object RelocItemHandler {

    /**
     * Finds the contiguous cluster of RelocItems surrounding the given item.
     * Returns the start and end indices (inclusive) in the list.
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
     * @param items The mutable list of items.
     * @param draggedId The ID of the item being dragged.
     * @param targetIndex The index where the dragged item should be.
     */
    fun updateOrder(items: MutableList<AzNavItem>, draggedId: String, targetIndex: Int) {
        val currentIndex = items.indexOfFirst { it.id == draggedId }
        if (currentIndex == -1) return
        if (currentIndex == targetIndex) return

        // Simple swap logic: remove and insert.
        // But prompt says "shifting by moving the RelocItems immediately adjacent".
        // Remove at current, insert at target is the standard reorder.

        // Validation: Target must be within the cluster.
        val cluster = findCluster(items, draggedId) ?: return
        if (targetIndex !in cluster) return

        val item = items.removeAt(currentIndex)
        items.add(targetIndex, item)
    }

    /**
     * Calculates the target index for a dragged item based on its drag offset and item heights.
     */
    fun calculateTargetIndex(
        items: List<AzNavItem>,
        draggedItemId: String,
        currentDragOffset: Float,
        itemHeights: Map<String, Int>
    ): Int? {
        val currentIndex = items.indexOfFirst { it.id == draggedItemId }
        if (currentIndex == -1) return null

        val cluster = findCluster(items, draggedItemId) ?: return null

        var target = currentIndex
        var remainingOffset = currentDragOffset

        if (remainingOffset > 0) {
            while (target < cluster.last) {
                val nextItem = items[target + 1]
                val nextHeight = itemHeights[nextItem.id] ?: 0
                if (nextHeight == 0) break // Safety check

                // User requested 40% overlap threshold
                // If remaining offset covers > 40% of next item height, swap.
                if (remainingOffset > nextHeight * 0.4f) {
                    remainingOffset -= nextHeight
                    target++
                } else {
                    break
                }
            }
        } else {
            while (target > cluster.first) {
                val prevItem = items[target - 1]
                val prevHeight = itemHeights[prevItem.id] ?: 0
                if (prevHeight == 0) break // Safety check

                // User requested 40% overlap threshold (negative direction)
                if (remainingOffset < -(prevHeight * 0.4f)) {
                    remainingOffset += prevHeight
                    target--
                } else {
                    break
                }
            }
        }
        return target
    }
}
