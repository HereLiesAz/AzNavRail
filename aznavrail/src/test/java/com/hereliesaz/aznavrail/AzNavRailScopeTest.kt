package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before

class AzNavRailScopeTest {

    private val scope = AzNavRailScopeImpl()

    @Before
    fun setUp() {
        scope.reset()
    }

    @Test
    fun `azConfig updates configuration`() {
        scope.azConfig(
            dockingSide = AzDockingSide.RIGHT,
            packButtons = true,
            noMenu = true,
            vibrate = true,
            displayAppName = true
        )

        assertEquals(AzDockingSide.RIGHT, scope.dockingSide)
        assertTrue(scope.packButtons)
        assertTrue(scope.noMenu)
        assertTrue(scope.vibrate)
        assertTrue(scope.displayAppName)
    }

    @Test
    fun `azTheme updates theme`() {
        scope.azTheme(
            activeColor = Color.Red,
            defaultShape = AzButtonShape.SQUARE
        )

        assertEquals(Color.Red, scope.activeColor)
        assertEquals(AzButtonShape.SQUARE, scope.defaultShape)
    }

    @Test
    fun `azAdvanced updates advanced settings`() {
        scope.azAdvanced(
            isLoading = true,
            helpEnabled = true,
            enableRailDragging = true,
            secLoc = "secret-key",
            secLocPort = 5555
        )

        assertTrue(scope.isLoading)
        assertTrue(scope.helpEnabled)
        assertTrue(scope.enableRailDragging)
        assertEquals("secret-key", scope.secLoc)
        assertEquals(5555, scope.secLocPort)
    }

    @Test
    fun `azRailItem adds item to list`() {
        scope.azRailItem(
            id = "home",
            text = "Home",
            route = "home_route",
            info = "Home Info"
        )

        assertEquals(1, scope.navItems.size)
        val item = scope.navItems[0]
        assertEquals("home", item.id)
        assertEquals("Home", item.text)
        assertEquals("home_route", item.route)
        assertEquals("Home Info", item.info)
        assertTrue(item.isRailItem)
    }

    @Test
    fun `azMenuItem adds menu item`() {
        scope.azMenuItem(id = "settings", text = "Settings")

        assertEquals(1, scope.navItems.size)
        val item = scope.navItems[0]
        assertEquals("settings", item.id)
        assertEquals(false, item.isRailItem)
    }

    @Test
    fun `azNestedRail adds nested rail item`() {
        scope.azNestedRail(
            id = "nested",
            text = "Nested",
            alignment = AzNestedRailAlignment.HORIZONTAL
        ) {
            azRailItem("sub1", "Sub 1")
        }

        assertEquals(1, scope.navItems.size)
        val item = scope.navItems[0]
        assertEquals("nested", item.id)
        assertTrue(item.isNestedRail)
        assertEquals(AzNestedRailAlignment.HORIZONTAL, item.nestedRailAlignment)
        
        assertNotNull(item.nestedRailItems)
        assertEquals(1, item.nestedRailItems?.size)
        assertEquals("sub1", item.nestedRailItems?.get(0)?.id)
    }

    @Test
    fun `azRailToggle adds toggle item`() {
        scope.azRailToggle(
            id = "toggle",
            isChecked = true,
            toggleOnText = "On",
            toggleOffText = "Off"
        )

        val item = scope.navItems[0]
        assertTrue(item.isToggle)
        assertEquals(true, item.isChecked)
        assertEquals("On", item.toggleOnText)
    }

    @Test
    fun `azRailCycler adds cycler item`() {
        scope.azRailCycler(
            id = "cycler",
            options = listOf("A", "B"),
            selectedOption = "A"
        )

        val item = scope.navItems[0]
        assertTrue(item.isCycler)
        assertEquals(listOf("A", "B"), item.options)
        assertEquals("A", item.selectedOption)
    }

    @Test
    fun `azRailHostItem and SubItems`() {
        scope.azRailHostItem("host", "Host")
        scope.azRailSubItem("sub", "host", "Sub", classifiers = emptySet(), onFocus = null, onClick = null)

        assertEquals(2, scope.navItems.size)
        
        val host = scope.navItems[0]
        assertTrue(host.isHost)
        
        val sub = scope.navItems[1]
        assertTrue(sub.isSubItem)
        assertEquals("host", sub.hostId)
    }
    
    @Test
    fun `azRailRelocItem sets up hidden menu`() {
        scope.azRailRelocItem(
            id = "reloc",
            hostId = "host",
            text = "Reloc"
        ) {
            listItem("Action") {}
        }
        
        val item = scope.navItems[0]
        assertTrue(item.isRelocItem)
        assertNotNull(item.hiddenMenuItems)
        assertEquals(1, item.hiddenMenuItems?.size)
        assertEquals("reloc_hidden_item_0", item.hiddenMenuItems?.get(0)?.id)
        assertEquals("Action", item.hiddenMenuItems?.get(0)?.text)
    }
}
