package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzItemAlert
import com.hereliesaz.aznavrail.model.AzItemState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `azItemState` / `itemOverrides` and the `applyItemStates` pass that stamps them onto the
 * freshly-declared items.
 *
 * The DSL is re-applied on every recomposition, so anything layered on top of an item has to be
 * re-applied afterwards or it survives exactly one frame — these tests pin that contract down.
 */
class AzItemStateTest {

    @Test
    fun `azItemState decorates an item declared before it`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "sync", text = "Sync")
        scope.azItemState(id = "sync", isLoading = true, badge = "3")
        scope.applyItemStates()

        val item = scope.navItems.single { it.id == "sync" }
        assertTrue(item.isLoading)
        assertEquals("3", item.badge)
    }

    @Test
    fun `azItemState decorates an item declared after it`() {
        // It is applied once the whole block has run, so declaration order must not matter.
        val scope = AzNavRailScopeImpl()
        scope.azItemState(id = "sync", badge = "9")
        scope.azRailItem(id = "sync", text = "Sync")
        scope.applyItemStates()

        assertEquals("9", scope.navItems.single { it.id == "sync" }.badge)
    }

    @Test
    fun `null fields leave the existing value alone`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "sync", text = "Sync", badge = "declared")
        scope.azItemState(id = "sync", isLoading = true)
        scope.applyItemStates()

        val item = scope.navItems.single { it.id == "sync" }
        assertTrue(item.isLoading)
        assertEquals("declared", item.badge)
    }

    @Test
    fun `an unknown id is ignored`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "sync", text = "Sync")
        scope.azItemState(id = "nope", badge = "x")
        scope.applyItemStates()

        assertEquals(1, scope.navItems.size)
        assertNull(scope.navItems.single().badge)
    }

    @Test
    fun `a popup override wins over the declared state`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "sync", text = "Sync")
        scope.azItemState(id = "sync", badge = "declared")
        scope.itemOverrides["sync"] = AzItemState(badge = "pushed", alert = AzItemAlert.WARNING)
        scope.applyItemStates()

        val item = scope.navItems.single { it.id == "sync" }
        assertEquals("pushed", item.badge)
        assertEquals(AzItemAlert.WARNING, item.alert)
    }

    @Test
    fun `overrides survive a reset but declared states do not`() {
        // reset() runs before the DSL is re-applied on every recomposition. A popup's push has to
        // outlive that; the DSL's own declaration is simply re-made.
        val scope = AzNavRailScopeImpl()
        scope.azItemState(id = "sync", badge = "declared")
        scope.itemOverrides["sync"] = AzItemState(badge = "pushed")

        scope.reset()

        assertTrue(scope.declaredItemStates.isEmpty())
        assertEquals("pushed", scope.itemOverrides["sync"]?.badge)
    }

    @Test
    fun `nested-rail children are decorated too`() {
        val scope = AzNavRailScopeImpl()
        scope.azNestedRail(id = "tools", text = "Tools") {
            azRailItem(id = "hammer", text = "Hammer")
        }
        scope.azItemState(id = "hammer", badge = "1")
        scope.applyItemStates()

        val nested = scope.navItems.single { it.id == "tools" }.nestedRailItems.orEmpty()
        assertEquals("1", nested.single { it.id == "hammer" }.badge)
    }
}
