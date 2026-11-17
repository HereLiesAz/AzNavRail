package com.hereliesaz.aznavrail.internal

import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The collapsed strip lays out rail items only. A menu-only item used to take a blank 72 dp slot
 * when `packButtons` was false, so the strip scrolled one slot per menu-only item past its last
 * button. Expected ids below are literals, not derived from the filter under test.
 */
class RailStripItemsTest {

    @Test
    fun menuOnlyItemsTakeNoSlotInTheStrip() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "a", text = "A")
        scope.azMenuItem(id = "m1", text = "M1")
        scope.azRailItem(id = "b", text = "B")
        scope.azMenuItem(id = "m2", text = "M2")
        scope.azMenuItem(id = "m3", text = "M3")

        assertEquals(listOf("a", "b"), railStripItems(scope.navItems).map { it.id })
    }

    @Test
    fun subItemsAreNotTopLevelStripItems() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem(id = "host", text = "Host")
        scope.azRailSubItem(id = "child", hostId = "host", text = "Child")
        scope.azRailItem(id = "solo", text = "Solo")

        assertEquals(listOf("host", "solo"), railStripItems(scope.navItems).map { it.id })
    }

    @Test
    fun aMenuOnlyRailKeepsNothing() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuItem(id = "m1", text = "M1")
        scope.azMenuItem(id = "m2", text = "M2")

        assertEquals(emptyList<String>(), railStripItems(scope.navItems).map { it.id })
    }
}
