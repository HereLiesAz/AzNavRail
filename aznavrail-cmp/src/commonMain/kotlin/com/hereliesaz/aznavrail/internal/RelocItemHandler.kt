package com.hereliesaz.aznavrail.internal

import androidx.compose.ui.geometry.Rect
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Helper object to manage RelocItem interactions.
 *
 * ## Slots and blocks
 *
 * A reloc cluster is a run of *slots*, not of single items. A slot is one direct reloc member of a
 * host — an `azRailRelocItem` or an `azRailRelocSubHostItem` — plus its **block**: every item
 * immediately after it in the flat item list that descends from it (its children, their children,
 * and so on, through `hostId`). A relocatable sub-host therefore occupies exactly one slot however
 * many descendants it has, its children move with it, and they never split the parent's run.
 *
 * For a plain reloc item the block is the item alone, so every function here reduces to its
 * original single-item behaviour. Descendants must be declared directly after their sub-host (the
 * natural recursive DSL order) to be carried in its block.
 */
object RelocItemHandler {

    /**
     * Index of the last item in the block that starts at [index]: [index] itself, extended over the
     * contiguous items that follow it and descend from `items[index]` (via their `hostId` chain).
     */
    fun blockEnd(items: List<AzNavItem>, index: Int): Int {
        val head = items.getOrNull(index) ?: return index
        if (!head.isHost) return index
        val byId = items.associateBy { it.id }
        var end = index
        while (end < items.lastIndex && isDescendant(items[end + 1], head.id, byId)) end++
        return end
    }

    /** True when [item]'s `hostId` chain reaches [ancestorId]. Guarded against cycles. */
    private fun isDescendant(item: AzNavItem, ancestorId: String, byId: Map<String, AzNavItem>): Boolean {
        var hostId = item.hostId
        var guard = 0
        while (hostId != null && guard++ < 64) {
            if (hostId == ancestorId) return true
            hostId = byId[hostId]?.hostId
        }
        return false
    }

    /**
     * The slots of the reloc cluster containing [itemId], top-first, each as the range of its block
     * in [items]. Null when [itemId] is missing, is not a reloc item, or has no host.
     *
     * A cluster is the maximal run of consecutive reloc slots sharing [itemId]'s `hostId`, where
     * each slot starts right after the previous slot's block ends.
     */
    fun clusterSlots(items: List<AzNavItem>, itemId: String): List<IntRange>? {
        val index = items.indexOfFirst { it.id == itemId }
        if (index == -1) return null
        val item = items[index]
        val hostId = item.hostId
        if (!item.isRelocItem || hostId == null) return null

        // Walk every slot of every run of this host's reloc members, then keep the run with the item.
        val runs = mutableListOf<MutableList<IntRange>>()
        var current: MutableList<IntRange>? = null
        var i = 0
        while (i < items.size) {
            val it = items[i]
            if (it.isRelocItem && it.hostId == hostId) {
                val end = blockEnd(items, i)
                val prev = current?.lastOrNull()
                if (current == null || prev == null || prev.last + 1 != i) {
                    current = mutableListOf()
                    runs.add(current)
                }
                current.add(i..end)
                i = end + 1
            } else {
                current = null
                i++
            }
        }
        return runs.firstOrNull { run -> run.any { it.first == index } }
    }

    /**
     * Finds the contiguous cluster of RelocItems surrounding the given item.
     * Returns the start and end indices (inclusive) in the list. The range spans whole blocks, so it
     * includes the descendants of any relocatable sub-host in the cluster.
     */
    fun findCluster(items: List<AzNavItem>, itemId: String): IntRange? {
        val slots = clusterSlots(items, itemId) ?: return null
        return slots.first().first..slots.last().last
    }

    /**
     * Moves [draggedId]'s whole block to slot [targetSlot] (an index into [clusterSlots]) of its
     * cluster, shifting the blocks in between. No-op when out of range or unchanged.
     */
    fun moveToSlot(items: MutableList<AzNavItem>, draggedId: String, targetSlot: Int) {
        val slots = clusterSlots(items, draggedId) ?: return
        val from = slots.indexOfFirst { items[it.first].id == draggedId }
        if (from == -1 || targetSlot !in slots.indices || targetSlot == from) return
        val blocks = slots.map { r -> items.subList(r.first, r.last + 1).toList() }.toMutableList()
        val moved = blocks.removeAt(from)
        blocks.add(targetSlot, moved)
        val start = slots.first().first
        val end = slots.last().last
        val flat = blocks.flatten()
        for (k in start..end) items[k] = flat[k - start]
    }

    /**
     * Rewrites the reloc members of [hostId] into [order] (their ids, top-first), carrying each
     * member's block with it. Returns false, leaving [items] untouched, when [order] is not exactly
     * the current set of members.
     */
    fun applyOrder(items: MutableList<AzNavItem>, hostId: String, order: List<String>): Boolean {
        val heads = items.indices.filter { items[it].isRelocItem && items[it].hostId == hostId }
        if (heads.isEmpty() || heads.map { items[it].id }.toSet() != order.toSet() || order.size != heads.size) return false
        val blocks = heads.associate { h -> items[h].id to items.subList(h, blockEnd(items, h) + 1).toList() }
        val rebuilt = ArrayList<AzNavItem>(items.size)
        var k = 0
        var i = 0
        while (i < items.size) {
            if (k < heads.size && i == heads[k]) {
                rebuilt.addAll(blocks.getValue(order[k]))
                i = blockEnd(items, i) + 1
                k++
            } else {
                rebuilt.add(items[i]); i++
            }
        }
        if (rebuilt.size != items.size) return false
        for (j in items.indices) items[j] = rebuilt[j]
        return true
    }

    /**
     * Swaps items in the mutable list to reflect the drag operation.
     * @param items The mutable list of items.
     * @param draggedId The ID of the item being dragged.
     * @param targetIndex The index where the dragged item should be. It is resolved to the slot
     *   whose block contains it, and the dragged item's whole block moves to that slot.
     */
    fun updateOrder(items: MutableList<AzNavItem>, draggedId: String, targetIndex: Int) {
        val currentIndex = items.indexOfFirst { it.id == draggedId }
        if (currentIndex == -1) return
        if (currentIndex == targetIndex) return

        // Validation: Target must be within the cluster.
        val slots = clusterSlots(items, draggedId) ?: return
        val targetSlot = slots.indexOfFirst { targetIndex in it }
        if (targetSlot == -1) return
        moveToSlot(items, draggedId, targetSlot)
    }

    /**
     * Calculates the target index for a dragged item based on its drag offset and item heights.
     * Steps a whole slot at a time: a slot's height is the sum of its block's reported heights, and
     * the returned index is the first index of the landing slot.
     */
    fun calculateTargetIndex(
        items: List<AzNavItem>,
        draggedItemId: String,
        currentDragOffset: Float,
        itemHeights: Map<String, Int>
    ): Int? = slotTarget(items, draggedItemId, currentDragOffset) { r ->
        val head = itemHeights[items[r.first].id] ?: 0
        if (head == 0) 0f else r.sumOf { itemHeights[items[it].id] ?: 0 }.toFloat()
    }

    /**
     * Overload using item bounds directly instead of a height map. The [Map]-based overload above is
     * used by the docked rail. Both are live, parallel implementations of the same 40% threshold.
     */
    fun calculateTargetIndex(
        items: List<AzNavItem>,
        draggedItemId: String,
        currentDragOffset: Float,
        itemBounds: Map<String, Rect>,
        isVertical: Boolean
    ): Int? = slotTarget(items, draggedItemId, currentDragOffset) { r ->
        val head = itemBounds[items[r.first].id] ?: return@slotTarget 0f
        val headDim = if (isVertical) head.height else head.width
        if (headDim == 0f) 0f else r.sumOf { i ->
            val b = itemBounds[items[i].id]
            (if (b == null) 0f else if (isVertical) b.height else b.width).toDouble()
        }.toFloat()
    }

    /** The 40%-overlap walk over whole slots. [dim] returns a slot's extent, 0 meaning unknown. */
    private inline fun slotTarget(
        items: List<AzNavItem>,
        draggedItemId: String,
        currentDragOffset: Float,
        dim: (IntRange) -> Float,
    ): Int? {
        val slots = clusterSlots(items, draggedItemId) ?: return null
        val from = slots.indexOfFirst { items[it.first].id == draggedItemId }
        if (from == -1) return null
        var target = from
        var remaining = currentDragOffset
        if (remaining > 0) {
            while (target < slots.lastIndex) {
                val next = dim(slots[target + 1])
                if (next == 0f) break // Safety check
                // User requested 40% overlap threshold
                if (remaining > next * 0.4f) { remaining -= next; target++ } else break
            }
        } else {
            while (target > 0) {
                val prev = dim(slots[target - 1])
                if (prev == 0f) break // Safety check
                if (remaining < -(prev * 0.4f)) { remaining += prev; target-- } else break
            }
        }
        return slots[target].first
    }

    /** Ids of [hostId]'s direct reloc members (reloc items and relocatable sub-hosts), top-first. */
    fun memberIds(items: List<AzNavItem>, hostId: String): List<String> =
        items.filter { it.isRelocItem && it.hostId == hostId }.map { it.id }
}
