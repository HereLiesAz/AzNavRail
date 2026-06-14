package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for [handleHostItemClick], the shared host-tap dispatcher used by both the
 * compact rail ([RailContent]) and the expanded menu ([MenuItem]).
 *
 * Consumers that listen for `azAdvanced(onInteraction = ...)` rely on a host tap being reported
 * exactly like a leaf tap: tapping an expandable host item (e.g. an `azRailHostItem`) must toggle
 * its expansion AND invoke the item's `onClick` callback — the lambda that, in both call sites,
 * ends by firing `scope.advancedConfig.onInteraction`. If a future refactor toggled expansion
 * without invoking `onClick`, host taps would silently stop emitting `onInteraction` and any
 * consumer driving UI off that signal (e.g. an onboarding coach advancing to the next item) would
 * stall. These tests pin that contract in place.
 */
class HandleHostItemClickTest {

    private fun hostItem(id: String = "host"): AzNavItem = AzNavItem(
        id = id,
        text = "Host",
        isRailItem = true,
        isHost = true,
        shape = AzButtonShape.CIRCLE
    )

    @Test
    fun `host tap toggles expansion and invokes onClick`() {
        var hostToggled = false
        var clicked = false
        var itemClicked = false

        handleHostItemClick(
            item = hostItem(),
            navController = null,
            onClick = { clicked = true },
            onItemClick = { itemClicked = true },
            onHostClick = { hostToggled = true }
        )

        assertTrue("host expansion should toggle on tap", hostToggled)
        assertTrue("onClick (the onInteraction carrier) should fire on host tap", clicked)
        assertTrue("onItemClick should fire on host tap", itemClicked)
    }

    @Test
    fun `host tap fires callbacks in expand-then-report order`() {
        val order = mutableListOf<String>()

        handleHostItemClick(
            item = hostItem(),
            navController = null,
            onClick = { order.add("onClick") },
            onItemClick = { order.add("onItemClick") },
            onHostClick = { order.add("onHostClick") }
        )

        // Expansion is toggled first, then the interaction is reported via onClick, then the
        // general item-click handler runs. The relative order of onClick before onItemClick
        // matters: consumers see the interaction before any collapse side-effects.
        assertEquals(listOf("onHostClick", "onClick", "onItemClick"), order)
    }

    @Test
    fun `host tap without an onClick handler still toggles expansion`() {
        var hostToggled = false

        // A host declared without its own onClick must not crash and must still expand.
        handleHostItemClick(
            item = hostItem(),
            navController = null,
            onClick = null,
            onItemClick = {},
            onHostClick = { hostToggled = true }
        )

        assertTrue(hostToggled)
    }
}
