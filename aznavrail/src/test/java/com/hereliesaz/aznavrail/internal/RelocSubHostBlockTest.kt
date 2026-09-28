package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Pure block/slot logic for relocatable sub-hosts. Expected values are literal. */
class RelocSubHostBlockTest {

    private fun item(id: String, hostId: String?, reloc: Boolean = true, host: Boolean = false) =
        AzNavItem(id = id, text = id, isRailItem = true, isRelocItem = reloc, isHost = host,
            isSubItem = hostId != null, hostId = hostId, shape = AzButtonShape.CIRCLE)

    /** P, a, g(g1, h(h1)), b — g and h are relocatable sub-hosts. */
    private fun tree() = mutableListOf(
        item("P", null, reloc = false, host = true),
        item("a", "P"),
        item("g", "P", host = true),
        item("g1", "g"),
        item("h", "g", host = true),
        item("h1", "h"),
        item("b", "P"),
    )

    @Test
    fun `a host and all its descendants are one slot`() {
        val items = tree()
        assertEquals(listOf(1..1, 2..5, 6..6), RelocItemHandler.clusterSlots(items, "a"))
        assertEquals(1..6, RelocItemHandler.findCluster(items, "b"))
        assertEquals(listOf(3..3, 4..5), RelocItemHandler.clusterSlots(items, "g1"))
    }

    @Test
    fun `moving a block carries its descendants`() {
        val items = tree()
        RelocItemHandler.moveToSlot(items, "g", 2)
        assertEquals(listOf("P", "a", "b", "g", "g1", "h", "h1"), items.map { it.id })
    }

    @Test
    fun `updateOrder with an index inside a block lands on that block's slot`() {
        val items = tree()
        RelocItemHandler.updateOrder(items, "a", 4)
        assertEquals(listOf("P", "g", "g1", "h", "h1", "a", "b"), items.map { it.id })
    }

    @Test
    fun `height walk steps over a whole block`() {
        val items = tree()
        val h = mapOf("a" to 10, "g" to 10, "g1" to 10, "h" to 10, "h1" to 10, "b" to 10)
        // Block g is 40 tall: 15 is under 40% of it, 17 is over.
        assertEquals(1, RelocItemHandler.calculateTargetIndex(items, "a", 15f, h))
        assertEquals(2, RelocItemHandler.calculateTargetIndex(items, "a", 17f, h))
        assertEquals(2, RelocItemHandler.calculateTargetIndex(items, "b", -17f, h))
    }

    @Test
    fun `applyOrder restores a saved order with blocks, and rejects a mismatched set`() {
        val items = tree()
        assert(RelocItemHandler.applyOrder(items, "P", listOf("b", "g", "a")))
        assertEquals(listOf("P", "b", "g", "g1", "h", "h1", "a"), items.map { it.id })
        assertFalse(RelocItemHandler.applyOrder(items, "P", listOf("b", "a")))
    }
}
