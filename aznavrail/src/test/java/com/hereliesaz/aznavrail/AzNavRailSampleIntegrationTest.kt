package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzNavRailSampleIntegrationTest {

    @Test
    fun `SampleApp configuration should apply correctly`() {
        val scope = AzNavRailScopeImpl()

        // Mimic SampleScreen configuration
        scope.azConfig(
            packButtons = true,
            dockingSide = AzDockingSide.RIGHT,
            noMenu = true,
            usePhysicalDocking = true
        )

        scope.azTheme(
            defaultShape = AzButtonShape.RECTANGLE
        )

        scope.azAdvanced(
            isLoading = true,
            enableRailDragging = true,
            infoScreen = true
        )

        assertEquals(true, scope.packButtons)
        assertEquals(AzDockingSide.RIGHT, scope.dockingSide)
        assertEquals(true, scope.noMenu)
        assertEquals(true, scope.usePhysicalDocking)
        assertEquals(AzButtonShape.RECTANGLE, scope.defaultShape)
        assertEquals(true, scope.isLoading)
        assertEquals(true, scope.enableRailDragging)
        assertEquals(true, scope.infoScreen)
    }

    @Test
    fun `SampleApp rail items should be added correctly`() {
        val scope = AzNavRailScopeImpl()

        // Mimic SampleScreen items
        scope.azMenuItem(id = "home", text = "Home", route = "home")
        scope.azRailToggle(id = "pack-rail", isChecked = false, toggleOnText = "Pack", toggleOffText = "Unpack")
        scope.azRailItem(id = "profile", text = "Profile", disabled = true)
        scope.azDivider()
        scope.azRailCycler(id = "cycler", options = listOf("A", "B"), selectedOption = "A")

        assertEquals(5, scope.navItems.size)

        val home = scope.navItems[0]
        assertEquals("home", home.id)
        assertEquals(false, home.isRailItem)

        val toggle = scope.navItems[1]
        assertEquals("pack-rail", toggle.id)
        assertEquals(true, toggle.isRailItem)
        assertEquals(true, toggle.isToggle)

        val profile = scope.navItems[2]
        assertEquals("profile", profile.id)
        assertEquals(true, profile.disabled)

        val divider = scope.navItems[3]
        assertEquals(true, divider.isDivider)

        val cycler = scope.navItems[4]
        assertEquals("cycler", cycler.id)
        assertEquals(true, cycler.isCycler)
    }

    @Test
    fun `azNestedRail should add nested items correctly for Horizontal`() {
        val scope = AzNavRailScopeImpl()

        scope.azNestedRail(
            id = "nested_h",
            text = "Nested Horizontal",
            alignment = AzNestedRailAlignment.HORIZONTAL
        ) {
            azRailItem("child1", "Child 1")
            azRailItem("child2", "Child 2")
        }

        assertEquals(1, scope.navItems.size)
        val nested = scope.navItems[0]
        assertEquals("nested_h", nested.id)
        assertEquals(true, nested.isNestedRail)
        assertEquals(AzNestedRailAlignment.HORIZONTAL, nested.nestedRailAlignment)

        assertEquals(2, nested.nestedRailItems!!.size)
        assertEquals("child1", nested.nestedRailItems!![0].id)
    }

    @Test
    fun `azNestedRail should add nested items correctly for Vertical`() {
        val scope = AzNavRailScopeImpl()

        scope.azNestedRail(
            id = "nested_v",
            text = "Nested Vertical",
            alignment = AzNestedRailAlignment.VERTICAL
        ) {
            azRailItem("child3", "Child 3")
        }

        assertEquals(1, scope.navItems.size)
        val nested = scope.navItems[0]
        assertEquals("nested_v", nested.id)
        assertEquals(true, nested.isNestedRail)
        assertEquals(AzNestedRailAlignment.VERTICAL, nested.nestedRailAlignment)

        assertEquals(1, nested.nestedRailItems!!.size)
        assertEquals("child3", nested.nestedRailItems!![0].id)
    }

    @Test
    fun `azRailRelocItem should add relocatable item with hidden menu`() {
        val scope = AzNavRailScopeImpl()

        scope.azRailRelocItem(
            id = "reloc",
            hostId = "host",
            text = "Relocatable"
        ) {
            listItem("Action 1") {}
            inputItem("Rename") {}
        }

        assertEquals(1, scope.navItems.size)
        val item = scope.navItems[0]
        assertEquals("reloc", item.id)
        assertEquals(true, item.isRelocItem)

        // Check hidden menu items
        assertEquals(2, item.hiddenMenuItems!!.size)
        assertEquals("reloc_hidden_item_0", item.hiddenMenuItems!![0].id)
        assertEquals("Action 1", item.hiddenMenuItems!![0].text)
        assertEquals("reloc_hidden_item_1", item.hiddenMenuItems!![1].id)
        assertEquals(true, item.hiddenMenuItems!![1].isInput)
    }
}
