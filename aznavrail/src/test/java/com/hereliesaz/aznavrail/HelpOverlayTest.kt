package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.internal.HelpLayoutItem
import com.hereliesaz.aznavrail.internal.HelpTargetItem
import com.hereliesaz.aznavrail.internal.calculateHelpLayout
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpOverlayTest {

    @Test
    fun testLayoutLogic_avoidsTopMargin() {
        val topMargin = 200f // e.g., 20% of 1000
        val items = listOf(
            HelpTargetItem("1", "Info 1", 100f), // Target is above margin
            HelpTargetItem("2", "Info 2", 300f)
        )

        // The layout logic is now fully integrated into the Compose tree in HelpOverlay.kt
        val layout = calculateHelpLayout(items, topMargin)

        // Item 1 should be pushed down to topMargin
        assertTrue("Item 1 should start at or below margin", layout[0].top >= topMargin)
    }

    @Test
    fun testLayoutLogic_avoidsOverlap() {
        val topMargin = 0f
        val itemHeight = 50f
        val spacing = 32f
        val items = listOf(
            HelpTargetItem("1", "Info 1", 100f),
            HelpTargetItem("2", "Info 2", 110f) // Very close target
        )

        val layout = calculateHelpLayout(items, topMargin, fixedHeight = itemHeight, spacing = spacing)

        val item1Bottom = layout[0].bottom
        val item2Top = layout[1].top

        assertTrue("Item 2 should start after Item 1 + spacing", item2Top >= item1Bottom + spacing)
    }
}