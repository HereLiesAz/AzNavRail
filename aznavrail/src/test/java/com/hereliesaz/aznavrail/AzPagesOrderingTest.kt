package com.hereliesaz.aznavrail

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the pages Z-ordering helpers [azOrderBackgrounds] and [azOrderOnscreen]. */
class AzPagesOrderingTest {

    private fun bg(weight: Int, page: Float) = AzBackgroundItem(weight = weight, page = page) {}
    private fun on(tag: Alignment, page: Float) = AzOnscreenItem(alignment = tag, page = page) {}

    @Test
    fun backgrounds_higherPageDrawnFurtherBack_whenPagesEnabled() {
        val a = bg(weight = 0, page = 0f)
        val b = bg(weight = 0, page = 2f)
        val c = bg(weight = 0, page = 1f)
        // Back-to-front render order: highest page first (backmost), lowest page last (front).
        assertEquals(
            listOf(2f, 1f, 0f),
            azOrderBackgrounds(listOf(a, b, c), pagesEnabled = true).map { it.page },
        )
    }

    @Test
    fun backgrounds_weightBreaksTiesWithinAPage() {
        val heavy = bg(weight = 5, page = 1f)
        val light = bg(weight = 0, page = 1f)
        // Within the same page, lower weight is further back (drawn first).
        assertEquals(
            listOf(0, 5),
            azOrderBackgrounds(listOf(heavy, light), pagesEnabled = true).map { it.weight },
        )
    }

    @Test
    fun backgrounds_fallBackToLegacyWeightSort_whenPagesDisabled() {
        val a = bg(weight = 10, page = 0f)
        val b = bg(weight = 1, page = 5f)
        // Pages off: order by weight ascending, page ignored.
        assertEquals(
            listOf(1, 10),
            azOrderBackgrounds(listOf(a, b), pagesEnabled = false).map { it.weight },
        )
    }

    @Test
    fun onscreen_higherPageDrawnFurtherBack_whenPagesEnabled() {
        val front = on(Alignment.TopStart, page = 0f)
        val back = on(Alignment.TopEnd, page = 3f)
        val mid = on(Alignment.Center, page = 1f)
        assertEquals(
            listOf(3f, 1f, 0f),
            azOrderOnscreen(listOf(front, back, mid), pagesEnabled = true).map { it.page },
        )
    }

    @Test
    fun onscreen_samePageKeepsDeclarationOrder() {
        val first = on(Alignment.TopStart, page = 1f)
        val second = on(Alignment.TopEnd, page = 1f)
        // Stable sort: same page keeps declaration order.
        assertEquals(
            listOf(Alignment.TopStart, Alignment.TopEnd),
            azOrderOnscreen(listOf(first, second), pagesEnabled = true).map { it.alignment },
        )
    }

    @Test
    fun onscreen_preservesDeclarationOrder_whenPagesDisabled() {
        val a = on(Alignment.TopStart, page = 9f)
        val b = on(Alignment.BottomEnd, page = 0f)
        // Pages off: declaration order unchanged, page ignored.
        assertEquals(
            listOf(Alignment.TopStart, Alignment.BottomEnd),
            azOrderOnscreen(listOf(a, b), pagesEnabled = false).map { it.alignment },
        )
    }
}
