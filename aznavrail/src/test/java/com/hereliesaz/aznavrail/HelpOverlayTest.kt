package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpOverlayTest {

    data class TestItem(val id: String, val info: String, val targetY: Float)
    data class LayoutItem(val id: String, val top: Float, val bottom: Float)

    @Test
    fun testLayoutLogic_avoidsTopMargin() {
        val topMargin = 200f // e.g., 20% of 1000
        val items = listOf(
            TestItem("1", "Info 1", 100f), // Target is above margin
            TestItem("2", "Info 2", 300f)
        )

        val layout = calculateLayout(items, topMargin)

        // Item 1 should be pushed down to topMargin
        assertTrue("Item 1 should start at or below margin", layout[0].top >= topMargin)
    }

    @Test
    fun testLayoutLogic_avoidsOverlap() {
        val topMargin = 0f
        val itemHeight = 50f
        val spacing = 32f
        val items = listOf(
            TestItem("1", "Info 1", 100f),
            TestItem("2", "Info 2", 110f) // Very close target
        )

        val layout = calculateLayout(items, topMargin, fixedHeight = itemHeight, spacing = spacing)

        val item1Bottom = layout[0].bottom
        val item2Top = layout[1].top

        assertTrue("Item 2 should start after Item 1 + spacing", item2Top >= item1Bottom + spacing)
    }

    // Mock implementation of the layout logic to verify the algorithm before porting to Compose
    private fun calculateLayout(
        items: List<TestItem>,
        topMargin: Float,
        fixedHeight: Float = 50f,
        spacing: Float = 32f
    ): List<LayoutItem> {
        val result = mutableListOf<LayoutItem>()
        var currentY = topMargin

        items.sortedBy { it.targetY }.forEach { item ->
            // Ideal top aligns center with targetY
            val idealTop = item.targetY - (fixedHeight / 2)

            // Push down if needed
            val top = maxOf(currentY, idealTop)
            val bottom = top + fixedHeight

            result.add(LayoutItem(item.id, top, bottom))

            currentY = bottom + spacing
        }
        return result
    }

    private fun maxOf(a: Float, b: Float): Float = if (a > b) a else b
}
