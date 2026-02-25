package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzButtonShape
import org.junit.Assert.assertEquals
import org.junit.Test

class RelocItemHandlerTest {

    private fun item(id: String, hostId: String? = null, isReloc: Boolean = false): AzNavItem {
        return AzNavItem(
            id = id,
            text = "",
            isRailItem = true,
            isRelocItem = isReloc,
            hostId = hostId,
            shape = AzButtonShape.CIRCLE
        )
    }

    @Test
    fun `findCluster finds contiguous items with same host`() {
        val items = listOf(
            item("1", "A", true),
            item("2", "A", true),
            item("3", "B", true),
            item("4", "A", false),
            item("5", "A", true)
        )

        val cluster1 = RelocItemHandler.findCluster(items, "1")
        assertEquals(0..1, cluster1)

        val cluster2 = RelocItemHandler.findCluster(items, "2")
        assertEquals(0..1, cluster2)

        val cluster3 = RelocItemHandler.findCluster(items, "3")
        assertEquals(2..2, cluster3)

        val cluster5 = RelocItemHandler.findCluster(items, "5")
        assertEquals(4..4, cluster5)
    }

    @Test
    fun `updateOrder swaps items correctly within cluster`() {
        val items = mutableListOf(
            item("1", "A", true),
            item("2", "A", true),
            item("3", "A", true)
        )

        RelocItemHandler.updateOrder(items, "1", 2)

        assertEquals("2", items[0].id)
        assertEquals("3", items[1].id)
        assertEquals("1", items[2].id)
    }

    @Test
    fun `updateOrder prevents moving outside cluster`() {
        val items = mutableListOf(
            item("1", "A", true),
            item("2", "B", true)
        )

        // Attempt to move item "1" (host A) to index 1 (where host B item is)
        RelocItemHandler.updateOrder(items, "1", 1)

        // Should not move because index 1 is not in cluster for item 1
        assertEquals("1", items[0].id)
        assertEquals("2", items[1].id)
    }

    @Test
    fun `calculateTargetIndex finds correct swap target`() {
        val items = listOf(
            item("1", "A", true),
            item("2", "A", true),
            item("3", "A", true)
        )
        val heights = mapOf("1" to 100, "2" to 100, "3" to 100)

        val target = RelocItemHandler.calculateTargetIndex(items, "1", 50f, heights)
        assertEquals(1, target)

        val target2 = RelocItemHandler.calculateTargetIndex(items, "1", 150f, heights)
        assertEquals(2, target2)
    }
}
