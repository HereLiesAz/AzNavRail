package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.NavItem

/**
 * A DSL scope for building a list of navigation items and configuring the [AzNavRail] component.
 */
interface AzNavRailScope {
    /**
     * Configures the overall behavior of the navigation rail.
     *
     * @param displayAppNameInHeader If `true`, the header will display the application's name instead of its icon.
     * @param packRailButtons If `true`, the rail buttons will be packed together at the top of the rail.
     */
    fun settings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false
    )

    /**
     * Declares a simple action item that appears only in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param onClick The lambda to execute when the item is clicked.
     */
    fun MenuItem(id: String, text: String, onClick: () -> Unit)

    /**
     * Declares a simple action item that appears on the rail and in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param color The color of the rail button.
     * @param onClick The lambda to execute when the item is clicked.
     */
    fun RailItem(id: String, text: String, color: Color? = null, onClick: () -> Unit)

    /**
     * Declares a toggleable item that appears only in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param isChecked The current checked state of the toggle.
     * @param onClick The lambda to execute when the item is clicked. You should update your `isChecked` state within this lambda.
     */
    fun MenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit)

    /**
     * Declares a toggleable item that appears on the rail and in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param color The color of the rail button.
     * @param isChecked The current checked state of the toggle.
     * @param onClick The lambda to execute when the item is clicked. You should update your `isChecked` state within this lambda.
     */
    fun RailToggle(id: String, text: String, color: Color? = null, isChecked: Boolean, onClick: () -> Unit)

    /**
     * Declares a cycle item that appears only in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param onClick The lambda to execute when the item is clicked. You should update your `selectedOption` state within this lambda.
     */
    fun MenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit)

    /**
     * Declares a cycle item that appears on the rail and in the expanded menu.
     *
     * @param id A unique identifier for this item.
     * @param text The text to display for this item.
     * @param color The color of the rail button.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param onClick The lambda to execute when the item is clicked. You should update your `selectedOption` state within this lambda.
     */
    fun RailCycler(id: String, text: String, color: Color? = null, options: List<String>, selectedOption: String, onClick: () -> Unit)
}

internal class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableListOf<NavItem>()
    var displayAppNameInHeader: Boolean = false
    var packRailButtons: Boolean = false

    override fun settings(displayAppNameInHeader: Boolean, packRailButtons: Boolean) {
        this.displayAppNameInHeader = displayAppNameInHeader
        this.packRailButtons = packRailButtons
    }

    override fun MenuItem(id: String, text: String, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = false, onClick = onClick))
    }

    override fun RailItem(id: String, text: String, color: Color?, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = true, color = color, onClick = onClick))
    }

    override fun MenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = false, isToggle = true, isChecked = isChecked, onClick = onClick))
    }

    override fun RailToggle(id: String, text: String, color: Color?, isChecked: Boolean, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = true, color = color, isToggle = true, isChecked = isChecked, onClick = onClick))
    }

    override fun MenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = false, isCycler = true, options = options, selectedOption = selectedOption, onClick = onClick))
    }

    override fun RailCycler(id: String, text: String, color: Color?, options: List<String>, selectedOption: String, onClick: () -> Unit) {
        navItems.add(NavItem(id = id, text = text, isRailItem = true, color = color, isCycler = true, options = options, selectedOption = selectedOption, onClick = onClick))
    }
}
