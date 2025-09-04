package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzNavItem

interface AzNavRailScope {
    fun azSettings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false
    )
    fun azMenuItem(id: String, text: String, onClick: () -> Unit)
    fun azRailItem(id: String, text: String, color: Color? = null, onClick: () -> Unit)
    fun azMenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit)
    fun azRailToggle(id: String, text: String, color: Color? = null, isChecked: Boolean, onClick: () -> Unit)
    fun azMenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit)
    fun azRailCycler(id: String, text: String, color: Color? = null, options: List<String>, selectedOption: String, onClick: () -> Unit)
}

internal class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableListOf<AzNavItem>()
    var displayAppNameInHeader: Boolean = false
    var packRailButtons: Boolean = false

    override fun azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean) {
        this.displayAppNameInHeader = displayAppNameInHeader
        this.packRailButtons = packRailButtons
    }

    override fun azMenuItem(id: String, text: String, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = false, onClick = onClick))
    }

    override fun azRailItem(id: String, text: String, color: Color?, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = true, color = color, onClick = onClick))
    }

    override fun azMenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = false, isToggle = true, isChecked = isChecked, onClick = onClick))
    }

    override fun azRailToggle(id: String, text: String, color: Color?, isChecked: Boolean, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = true, color = color, isToggle = true, isChecked = isChecked, onClick = onClick))
    }

    override fun azMenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = false, isCycler = true, options = options, selectedOption = selectedOption, onClick = onClick))
    }

    override fun azRailCycler(id: String, text: String, color: Color?, options: List<String>, selectedOption: String, onClick: () -> Unit) {
        navItems.add(AzNavItem(id = id, text = text, isRailItem = true, color = color, isCycler = true, options = options, selectedOption = selectedOption, onClick = onClick))
    }
}
