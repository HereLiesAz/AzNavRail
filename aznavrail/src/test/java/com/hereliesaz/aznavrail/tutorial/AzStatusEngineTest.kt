package com.hereliesaz.aznavrail.tutorial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the built-in `az.*` status id scheme an app inherits for free. */
class AzStatusEngineTest {

    @Test
    fun `builtin status ids reflect live rail state`() {
        val s = computeBuiltinStatuses(
            railExpanded = true,
            railFloating = false,
            hostStates = mapOf("design" to true, "modes" to false),
            currentRoute = "home",
            activeItemId = "home",
            nestedRailOpenId = null,
            helpOpen = false,
            onscreenVisibleIds = setOf("canvas"),
        )
        assertTrue("az.rail.expanded" in s)
        assertFalse("az.rail.collapsed" in s)
        assertFalse("az.rail.floating" in s)
        assertTrue("az.host.design.expanded" in s)
        assertFalse("az.host.modes.expanded" in s)
        assertTrue("az.screen.home" in s)
        assertTrue("az.item.home.active" in s)
        assertTrue("az.onscreen.canvas.visible" in s)
        assertFalse("az.help.open" in s)
    }

    @Test
    fun `rail reports collapsed when not expanded`() {
        val s = computeBuiltinStatuses(
            railExpanded = false,
            railFloating = true,
            hostStates = emptyMap(),
            currentRoute = null,
            activeItemId = null,
            nestedRailOpenId = "more",
            helpOpen = true,
        )
        assertTrue("az.rail.collapsed" in s)
        assertFalse("az.rail.expanded" in s)
        assertTrue("az.rail.floating" in s)
        assertTrue("az.nestedRail.more.open" in s)
        assertTrue("az.help.open" in s)
    }
}
