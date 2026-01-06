package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.internal.RelocItemHandler
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelocItemHandlerTest {

    private fun createItem(id: String, isReloc: Boolean, hostId: String?): AzNavItem {
        return AzNavItem(
            id = id,
            text = "Item $id",
            isRailItem = true,
            isRelocItem = isReloc,
            hostId = hostId,
            shape = com.hereliesaz.aznavrail.model.AzButtonShape.CIRCLE
        )
    }

    @Test
    fun `findCluster - single item cluster`() {
        val items = listOf(
            createItem("1", false, null),
            createItem("2", true, "host1"),
            createItem("3", false, null)
        )
        val range = RelocItemHandler.findCluster(items, "2")
        assertEquals(1..1, range)
    }

    @Test
    fun `findCluster - multi item cluster`() {
        val items = listOf(
            createItem("1", false, null),
            createItem("2", true, "host1"),
            createItem("3", true, "host1"),
            createItem("4", true, "host1"),
            createItem("5", false, null)
        )
        val range = RelocItemHandler.findCluster(items, "3")
        assertEquals(1..3, range)
    }

    @Test
    fun `findCluster - mixed hosts`() {
        val items = listOf(
            createItem("1", true, "host1"),
            createItem("2", true, "host1"),
            createItem("3", true, "host2"),
            createItem("4", true, "host2")
        )
        val range1 = RelocItemHandler.findCluster(items, "2")
        assertEquals(0..1, range1)

        val range2 = RelocItemHandler.findCluster(items, "3")
        assertEquals(2..3, range2)
    }

    @Test
    fun `findCluster - not reloc item returns null`() {
        val items = listOf(
            createItem("1", false, null)
        )
        val range = RelocItemHandler.findCluster(items, "1")
        assertNull(range)
    }

    @Test
    fun `updateOrder - swaps items correctly within cluster`() {
        val items = mutableListOf(
            createItem("1", true, "host1"),
            createItem("2", true, "host1"),
            createItem("3", true, "host1")
        )

        // Swap 1 and 2
        RelocItemHandler.updateOrder(items, "1", 1)
        assertEquals("2", items[0].id)
        assertEquals("1", items[1].id)
        assertEquals("3", items[2].id)
    }

    @Test
    fun `updateOrder - prevents swap outside cluster`() {
        val items = mutableListOf(
            createItem("1", true, "host1"),
            createItem("2", false, null)
        )

        RelocItemHandler.updateOrder(items, "1", 1)
        // Should not change because index 1 is not part of cluster
        // Actually findCluster for "1" is 0..0. So target 1 is outside.

        assertEquals("1", items[0].id)
        assertEquals("2", items[1].id)
    }
}
