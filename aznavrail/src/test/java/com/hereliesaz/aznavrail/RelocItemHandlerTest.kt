package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.internal.RelocItemHandler
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Test
import org.junit.Assert.*

class RelocItemHandlerTest {

    // --- findCluster Tests ---

    @Test
    fun findCluster_returnsNullForNonRelocItem() {
        val items = listOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = false, hostId = "host")
        )
        val cluster = RelocItemHandler.findCluster(items, "1")
        assertNull(cluster)
    }

    @Test
    fun findCluster_returnsSingleItemRange() {
        val items = listOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host")
        )
        val cluster = RelocItemHandler.findCluster(items, "1")
        assertEquals(0..0, cluster)
    }

    @Test
    fun findCluster_identifiesContiguousItems() {
        val items = listOf(
            AzNavItem(id = "A", text = "A", isRailItem = true, isRelocItem = false),
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "2", text = "2", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "3", text = "3", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "B", text = "B", isRailItem = true, isRelocItem = false)
        )

        // Test middle item
        val cluster2 = RelocItemHandler.findCluster(items, "2")
        assertEquals(1..3, cluster2)

        // Test start item
        val cluster1 = RelocItemHandler.findCluster(items, "1")
        assertEquals(1..3, cluster1)

        // Test end item
        val cluster3 = RelocItemHandler.findCluster(items, "3")
        assertEquals(1..3, cluster3)
    }

    @Test
    fun findCluster_stopsAtDifferentHost() {
        val items = listOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host1"),
            AzNavItem(id = "2", text = "2", isRailItem = true, isRelocItem = true, hostId = "host2")
        )

        val cluster1 = RelocItemHandler.findCluster(items, "1")
        assertEquals(0..0, cluster1)

        val cluster2 = RelocItemHandler.findCluster(items, "2")
        assertEquals(1..1, cluster2)
    }

    // --- updateOrder Tests ---

    @Test
    fun updateOrder_swapsItemsCorrectly() {
        val items = mutableListOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "2", text = "2", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "3", text = "3", isRailItem = true, isRelocItem = true, hostId = "host")
        )

        // Move "1" to index 2 (end)
        RelocItemHandler.updateOrder(items, "1", 2)

        assertEquals("2", items[0].id)
        assertEquals("3", items[1].id)
        assertEquals("1", items[2].id)
    }

    @Test
    fun updateOrder_doesNothingIfTargetOutsideCluster() {
        val items = mutableListOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "A", text = "A", isRailItem = true, isRelocItem = false)
        )

        // Try to move "1" to index 1 (which is "A", not in cluster)
        RelocItemHandler.updateOrder(items, "1", 1)

        // Order should remain unchanged
        assertEquals("1", items[0].id)
        assertEquals("A", items[1].id)
    }

    // --- calculateTargetIndex Tests ---

    @Test
    fun calculateTargetIndex_returnsNullForInvalidItem() {
        val items = listOf(AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host"))
        val target = RelocItemHandler.calculateTargetIndex(
            items = items,
            draggedItemId = "nonexistent",
            currentDragOffset = 100f,
            itemHeights = mapOf("1" to 100)
        )
        assertNull(target)
    }

    @Test
    fun calculateTargetIndex_movesDownCorrectly() {
        val items = listOf(
            AzNavItem(id = "1", text = "1", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "2", text = "2", isRailItem = true, isRelocItem = true, hostId = "host"),
            AzNavItem(id = "3", text = "3", isRailItem = true, isRelocItem = true, hostId = "host")
        )
        val heights = mapOf("1" to 100, "2" to 100, "3" to 100)

        // Drag item "1" down by 50px. Next item "2" is 100px.
        // 50 > 0.4 * 100 (40). So it should swap.
        val target1 = RelocItemHandler.calculateTargetIndex(items, "1", 50f, heights)
        assertEquals(1, target1)

        // Drag item "1" down by 150px.
        // Swap with "2" (consumes 100, remaining 50).
        // 50 > 0.4 * 100 (40). Swap with "3".
        val target2 = RelocItemHandler.calculateTargetIndex(items, "1", 150f, heights)
        assertEquals(2, target2)

         // Drag item "1" down by 30px.
        // 30 < 40. No swap.
        val target3 = RelocItemHandler.calculateTargetIndex(items, "1", 30f, heights)
        assertEquals(0, target3)
    }
}
