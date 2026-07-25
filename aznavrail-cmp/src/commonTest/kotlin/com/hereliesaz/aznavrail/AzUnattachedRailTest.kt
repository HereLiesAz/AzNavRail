package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.internal.azUnattachedSubtreeIds
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the pure part of the unattached-host feature: which ids the rail and drawer must skip.
 *
 * The rendering is Compose and needs a device, but the subtree walk is plain data — and getting it
 * wrong either leaks an unattached host back into the rail or, worse, silently deletes a sub-item
 * from the whole app.
 */
class AzUnattachedRailTest {

    @Test
    fun `no unattached hosts means nothing is filtered`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "home", text = "Home")
        scope.azRailHostItem(id = "host", text = "Host")
        scope.azRailSubItem(id = "child", hostId = "host", text = "Child")

        assertTrue(azUnattachedSubtreeIds(scope.navItems).isEmpty())
    }

    @Test
    fun `an unattached host takes its whole subtree with it`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "home", text = "Home")
        scope.azUnattachedHostItem(id = "tools", text = "Tools", anchor = AzUnattachedAnchor.FLOATING)
        scope.azRailSubItem(id = "measure", hostId = "tools", text = "Measure")
        scope.azRailSubHostItem(id = "shapes", hostId = "tools", text = "Shapes")
        scope.azRailSubItem(id = "circle", hostId = "shapes", text = "Circle")

        assertEquals(
            setOf("tools", "measure", "shapes", "circle"),
            azUnattachedSubtreeIds(scope.navItems),
        )
    }

    @Test
    fun `menu sub-items of an unattached host are part of the subtree`() {
        // They are filtered out of the drawer, so the unattached stack is the only place left that
        // can draw them — it used to skip them, which deleted them from the UI entirely.
        val scope = AzNavRailScopeImpl()
        scope.azUnattachedHostItem(id = "tools", text = "Tools")
        scope.azMenuSubItem(id = "menu-child", hostId = "tools", text = "Menu child")

        assertTrue("menu-child" in azUnattachedSubtreeIds(scope.navItems))
    }

    @Test
    fun `an attached host's subtree is untouched`() {
        val scope = AzNavRailScopeImpl()
        scope.azUnattachedHostItem(id = "tools", text = "Tools")
        scope.azRailHostItem(id = "normal", text = "Normal")
        scope.azRailSubItem(id = "normal-child", hostId = "normal", text = "Child")

        val ids = azUnattachedSubtreeIds(scope.navItems)
        assertEquals(setOf("tools"), ids)
    }

    @Test
    fun `the default anchor is OPPOSITE`() {
        val scope = AzNavRailScopeImpl()
        scope.azUnattachedHostItem(id = "tools", text = "Tools")
        val item = scope.navItems.single { it.id == "tools" }

        assertTrue(item.isUnattached)
        assertTrue(item.isHost)
        assertEquals(AzUnattachedAnchor.OPPOSITE, item.unattachedAnchor)
    }
}
