package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzNavItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AzNavRailInfoTest {

    @Test
    fun `azAdvanced should update infoScreen properties`() {
        val scope = AzNavRailScopeImpl()
        val onDismiss = {}

        scope.azAdvanced(
            infoScreen = true,
            onDismissInfoScreen = onDismiss
        )

        assertEquals(true, scope.infoScreen)
        assertEquals(onDismiss, scope.onDismissInfoScreen)
    }

    @Test
    fun `azRailItem should accept info parameter`() {
        val scope = AzNavRailScopeImpl()
        val infoText = "This is a rail item"
        scope.azRailItem("home", "Home", Color.Red, info = infoText, onClick = {})

        val item = scope.navItems[0]
        assertEquals(infoText, item.info)
    }

    @Test
    fun `azMenuItem should accept info parameter`() {
        val scope = AzNavRailScopeImpl()
        val infoText = "This is a menu item"
        scope.azMenuItem("menu_item", "Menu Item", info = infoText, onClick = {})

        val item = scope.navItems[0]
        assertEquals(infoText, item.info)
    }

    @Test
    fun `azRailHostItem should accept info parameter`() {
        val scope = AzNavRailScopeImpl()
        val infoText = "This is a host item"
        scope.azRailHostItem("host", "Host", info = infoText, onClick = {})

        val item = scope.navItems[0]
        assertEquals(infoText, item.info)
    }

    @Test
    fun `azMenuToggle should accept info parameter`() {
        val scope = AzNavRailScopeImpl()
        val infoText = "This is a toggle"
        scope.azMenuToggle("toggle", true, "On", "Off", info = infoText, onClick = {})

        val item = scope.navItems[0]
        assertEquals(infoText, item.info)
    }

    @Test
    fun `azMenuCycler should accept info parameter`() {
        val scope = AzNavRailScopeImpl()
        val infoText = "This is a cycler"
        scope.azMenuCycler("cycler", listOf("A", "B"), "A", info = infoText, onClick = {})

        val item = scope.navItems[0]
        assertEquals(infoText, item.info)
    }
}
