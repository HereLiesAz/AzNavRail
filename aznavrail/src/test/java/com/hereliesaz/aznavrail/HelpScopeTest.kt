package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the parent-lookup logic that backs `toggleHelpOverlay` in [AzNavRail]:
 *
 * ```kotlin
 * helpScopeId = itemId?.let { id ->
 *     scope.navItems.firstOrNull { parent ->
 *         parent.isNestedRail && parent.nestedRailItems?.any { it.id == id } == true
 *     }?.id
 * }
 * ```
 *
 * A help item declared directly on the main rail must resolve to `helpScopeId = null`
 * (main-rail scope). A help item declared inside `azNestedRail { ... }` must resolve to the
 * parent nested-rail's id so the overlay scopes its cards to that rail.
 */
class HelpScopeTest {

    /** Inlined copy of the production lookup so the test is independent of `@Composable` plumbing. */
    private fun findHelpScopeId(scope: AzNavRailScopeImpl, helpItemId: String): String? =
        scope.navItems.firstOrNull { parent ->
            parent.isNestedRail && parent.nestedRailItems?.any { it.id == helpItemId } == true
        }?.id

    @Test
    fun mainRailHelpItem_resolvesToNullScope() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "home", text = "Home")
        scope.azHelpRailItem(id = "main-help", text = "Help")

        val mainHelp = scope.navItems.firstOrNull { it.id == "main-help" }
        assertNotNull(
            "azHelpRailItem must register a top-level entry in navItems with the requested id; " +
                "ids present = ${scope.navItems.map { it.id }}.",
            mainHelp,
        )
        assertTrue(
            "azHelpRailItem must mark the entry as `isHelpItem = true` so RailItems can route the tap " +
                "to the help overlay. Got isHelpItem = ${mainHelp!!.isHelpItem}.",
            mainHelp.isHelpItem,
        )

        val scopeId = findHelpScopeId(scope, "main-help")
        assertNull(
            "A help item declared on the main rail must resolve helpScopeId = null so the overlay " +
                "renders cards for ALL main-rail items — got '$scopeId'. " +
                "Verify the firstOrNull lookup in AzNavRail.toggleHelpOverlay().",
            scopeId,
        )
    }

    @Test
    fun nestedRailHelpItem_resolvesToParentNestedRailId() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "home", text = "Home")
        scope.azNestedRail(id = "tools", text = "Tools", alignment = AzNestedRailAlignment.VERTICAL) {
            azRailItem("brush", "Brush")
            azHelpRailItem(id = "nested-help", text = "Help")
        }

        val parent = scope.navItems.firstOrNull { it.id == "tools" }
        assertNotNull(
            "azNestedRail must register a parent entry in navItems with id = 'tools'; " +
                "got ids = ${scope.navItems.map { it.id }}.",
            parent,
        )
        assertTrue(
            "The parent entry registered by azNestedRail must have isNestedRail = true so the " +
                "help-scope lookup can identify it. Got isNestedRail = ${parent!!.isNestedRail}.",
            parent.isNestedRail,
        )
        assertNotNull(
            "azNestedRail must populate `nestedRailItems` with the children registered in its " +
                "nestedContent { ... } block. Got null.",
            parent.nestedRailItems,
        )
        assertTrue(
            "The nested help item must live inside the parent's nestedRailItems list; " +
                "got ${parent.nestedRailItems?.map { it.id }}.",
            parent.nestedRailItems!!.any { it.id == "nested-help" && it.isHelpItem },
        )

        val scopeId = findHelpScopeId(scope, "nested-help")
        assertEquals(
            "A help item declared inside azNestedRail { ... } must resolve helpScopeId = the parent " +
                "nested rail's id ('tools') so the overlay scopes its cards to that rail only — " +
                "got '$scopeId'. Verify the firstOrNull lookup in AzNavRail.toggleHelpOverlay().",
            "tools",
            scopeId,
        )
    }

    @Test
    fun unknownHelpId_resolvesToNull() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem(id = "home", text = "Home")

        val scopeId = findHelpScopeId(scope, "no-such-help")
        assertNull(
            "An id that doesn't appear in any nested rail must resolve helpScopeId = null " +
                "(falling back to main-rail scope) — got '$scopeId'.",
            scopeId,
        )
    }

    @Test
    fun multipleNestedRails_helpItemResolvesToCorrectParent() {
        val scope = AzNavRailScopeImpl()
        scope.azNestedRail(id = "rail-A", text = "A", alignment = AzNestedRailAlignment.VERTICAL) {
            azHelpRailItem(id = "help-A", text = "?")
        }
        scope.azNestedRail(id = "rail-B", text = "B", alignment = AzNestedRailAlignment.VERTICAL) {
            azHelpRailItem(id = "help-B", text = "?")
        }

        assertEquals(
            "Each nested help item must resolve to its OWN parent rail — the lookup must not " +
                "short-circuit on the first nested rail it finds. help-A should map to 'rail-A'.",
            "rail-A",
            findHelpScopeId(scope, "help-A"),
        )
        assertEquals(
            "Each nested help item must resolve to its OWN parent rail; help-B should map to 'rail-B'.",
            "rail-B",
            findHelpScopeId(scope, "help-B"),
        )
    }
}
