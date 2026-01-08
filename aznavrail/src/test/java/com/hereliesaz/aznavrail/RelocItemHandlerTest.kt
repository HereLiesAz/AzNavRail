package com.hereliesaz.aznavrail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.hereliesaz.aznavrail.internal.RelocItemHandler
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzButtonShape
import org.junit.Test
import org.junit.Assert.*

class RelocItemHandlerTest {

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
