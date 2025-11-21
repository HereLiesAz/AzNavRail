package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AzNavRailTest {

    @Test
    fun `azSettings should update scope properties`() {
        val scope = AzNavRailScopeImpl()

        scope.azSettings(
            displayAppNameInHeader = true,
            packRailButtons = true,
            expandedRailWidth = 300.dp,
            collapsedRailWidth = 100.dp,
            showFooter = false
        )

        assertEquals(true, scope.displayAppNameInHeader)
        assertEquals(true, scope.packRailButtons)
        assertEquals(300.dp, scope.expandedRailWidth)
        assertEquals(100.dp, scope.collapsedRailWidth)
        assertEquals(false, scope.showFooter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azSettings with invalid widths should throw exception`() {
        val scope = AzNavRailScopeImpl()
        scope.azSettings(expandedRailWidth = 100.dp, collapsedRailWidth = 200.dp)
    }

    @Test
    fun `azMenuItem should add a menu item`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuItem("home", "Home", onClick = {})
        val expectedItem = AzNavItem("home", "Home", isRailItem = false)
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuItem with empty text should throw exception`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuItem("home", "", onClick = {})
    }

    @Test
    fun `azRailItem should add a rail item`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem("home", "Home", Color.Red, onClick = {})
        val expectedItem = AzNavItem("home", "Home", isRailItem = true, color = Color.Red)
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
        assertEquals(expectedItem.color, scope.navItems[0].color)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azRailItem with empty text should throw exception`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem("home", "", onClick = {})
    }

    @Test
    fun `azMenuToggle should add a menu toggle item`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuToggle("toggle", true, "On", "Off") {}
        val expectedItem = AzNavItem("toggle", "", isRailItem = false, isToggle = true, isChecked = true, toggleOnText = "On", toggleOffText = "Off")
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
        assertEquals(expectedItem.isToggle, scope.navItems[0].isToggle)
        assertEquals(expectedItem.isChecked, scope.navItems[0].isChecked)
        assertEquals(expectedItem.toggleOnText, scope.navItems[0].toggleOnText)
        assertEquals(expectedItem.toggleOffText, scope.navItems[0].toggleOffText)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azRailToggle with empty text should throw exception`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailToggle("toggle", Color.Blue, false, "On", "") {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuToggle with empty text should throw exception`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuToggle("toggle", true, "", "Off") {}
    }

    @Test
    fun `azRailToggle should add a rail toggle item`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailToggle("toggle", Color.Blue, false, "On", "Off") {}
        val expectedItem = AzNavItem("toggle", "", isRailItem = true, color = Color.Blue, isToggle = true, isChecked = false, toggleOnText = "On", toggleOffText = "Off")
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
        assertEquals(expectedItem.color, scope.navItems[0].color)
        assertEquals(expectedItem.isToggle, scope.navItems[0].isToggle)
        assertEquals(expectedItem.isChecked, scope.navItems[0].isChecked)
        assertEquals(expectedItem.toggleOnText, scope.navItems[0].toggleOnText)
        assertEquals(expectedItem.toggleOffText, scope.navItems[0].toggleOffText)
    }

    @Test
    fun `azMenuCycler should add a menu cycler item`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azMenuCycler("cycler", options, "A") {}
        val expectedItem = AzNavItem("cycler", "", isRailItem = false, isCycler = true, options = options, selectedOption = "A")
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
        assertEquals(expectedItem.isCycler, scope.navItems[0].isCycler)
        assertEquals(expectedItem.options, scope.navItems[0].options)
        assertEquals(expectedItem.selectedOption, scope.navItems[0].selectedOption)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azRailCycler with invalid selectedOption should throw exception`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azRailCycler("cycler", Color.Green, options, "D") {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun `azMenuCycler with invalid selectedOption should throw exception`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azMenuCycler("cycler", options, "D") {}
    }

    @Test
    fun `azRailCycler should add a rail cycler item`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azRailCycler("cycler", Color.Green, options, "B") {}
        val expectedItem = AzNavItem("cycler", "", isRailItem = true, color = Color.Green, isCycler = true, options = options, selectedOption = "B")
        assertEquals(expectedItem.id, scope.navItems[0].id)
        assertEquals(expectedItem.text, scope.navItems[0].text)
        assertEquals(expectedItem.isRailItem, scope.navItems[0].isRailItem)
        assertEquals(expectedItem.color, scope.navItems[0].color)
        assertEquals(expectedItem.isCycler, scope.navItems[0].isCycler)
        assertEquals(expectedItem.options, scope.navItems[0].options)
        assertEquals(expectedItem.selectedOption, scope.navItems[0].selectedOption)
    }

    @Test
    fun `azMenuItem should add item with screenTitle`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuItem("home", "Home", onClick = {}, screenTitle = "My Home")
        assertEquals("My Home", scope.navItems[0].screenTitle)
    }

    @Test
    fun `azRailItem should add item with screenTitle`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailItem("favorites", "Favorites", onClick = {}, screenTitle = "My Favorites")
        assertEquals("My Favorites", scope.navItems[0].screenTitle)
    }

    @Test
    fun `azMenuSubToggle should add a menu sub-toggle item`() {
        val scope = AzNavRailScopeImpl()
        scope.azMenuHostItem("host", "Host", onClick = {})
        scope.azMenuSubToggle("sub_toggle", "host", true, "On", "Off") {}
        val expectedItem = AzNavItem("sub_toggle", "", isRailItem = false, isToggle = true, isChecked = true, toggleOnText = "On", toggleOffText = "Off", isSubItem = true, hostId = "host")
        assertEquals(expectedItem.id, scope.navItems[1].id)
        assertEquals(expectedItem.isSubItem, scope.navItems[1].isSubItem)
        assertEquals(expectedItem.hostId, scope.navItems[1].hostId)
    }

    @Test
    fun `azRailSubToggle should add a rail sub-toggle item`() {
        val scope = AzNavRailScopeImpl()
        scope.azRailHostItem("host", "Host", onClick = {})
        scope.azRailSubToggle("sub_toggle", "host", Color.Blue, false, "On", "Off", shape = null) {}
        val expectedItem = AzNavItem("sub_toggle", "", isRailItem = true, color = Color.Blue, isToggle = true, isChecked = false, toggleOnText = "On", toggleOffText = "Off", isSubItem = true, hostId = "host")
        assertEquals(expectedItem.id, scope.navItems[1].id)
        assertEquals(expectedItem.isRailItem, scope.navItems[1].isRailItem)
        assertEquals(expectedItem.isSubItem, scope.navItems[1].isSubItem)
        assertEquals(expectedItem.hostId, scope.navItems[1].hostId)
    }

    @Test
    fun `azMenuSubCycler should add a menu sub-cycler item`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azMenuHostItem("host", "Host", onClick = {})
        scope.azMenuSubCycler("sub_cycler", "host", options, "A") {}
        val expectedItem = AzNavItem("sub_cycler", "", isRailItem = false, isCycler = true, options = options, selectedOption = "A", isSubItem = true, hostId = "host")
        assertEquals(expectedItem.id, scope.navItems[1].id)
        assertEquals(expectedItem.isSubItem, scope.navItems[1].isSubItem)
        assertEquals(expectedItem.hostId, scope.navItems[1].hostId)
    }

    @Test
    fun `azRailSubCycler should add a rail sub-cycler item`() {
        val scope = AzNavRailScopeImpl()
        val options = listOf("A", "B", "C")
        scope.azRailHostItem("host", "Host", onClick = {})
        scope.azRailSubCycler("sub_cycler", "host", Color.Green, options, "B", shape = null) {}
        val expectedItem = AzNavItem("sub_cycler", "", isRailItem = true, color = Color.Green, isCycler = true, options = options, selectedOption = "B", isSubItem = true, hostId = "host")
        assertEquals(expectedItem.id, scope.navItems[1].id)
        assertEquals(expectedItem.isRailItem, scope.navItems[1].isRailItem)
        assertEquals(expectedItem.isSubItem, scope.navItems[1].isSubItem)
        assertEquals(expectedItem.hostId, scope.navItems[1].hostId)
    }

    @Test
    fun `azSettings should update headerIconShape`() {
        val scope = AzNavRailScopeImpl()
        scope.azSettings(headerIconShape = com.hereliesaz.aznavrail.model.AzHeaderIconShape.ROUNDED)
        assertEquals(com.hereliesaz.aznavrail.model.AzHeaderIconShape.ROUNDED, scope.headerIconShape)
    }
}
