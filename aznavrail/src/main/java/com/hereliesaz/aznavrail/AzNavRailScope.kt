package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * A DSL scope for building the content of the [AzNavRail].
 */
interface AzNavRailScope {
    /**
     * Configures the settings for the [AzNavRail].
     * @param displayAppNameInHeader Whether to display the app name in the header.
     * @param packRailButtons Whether to pack the rail buttons together at the top of the rail.
     * @param expandedRailWidth The width of the rail when it is expanded.
     * @param collapsedRailWidth The width of the rail when it is collapsed.
     * @param showFooter Whether to show the footer.
     */
    fun azSettings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false,
        expandedRailWidth: Dp = 260.dp,
        collapsedRailWidth: Dp = 80.dp,
        showFooter: Boolean = true
    )

    /**
     * Adds a menu item that only appears in the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azMenuItem(id: String, text: String, onClick: () -> Unit)

    /**
     * Adds a rail item that appears in both the collapsed rail and the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param color The color of the item.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azRailItem(id: String, text: String, color: Color? = null, onClick: () -> Unit)

    /**
     * Adds a toggle switch item that only appears in the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param isChecked Whether the switch is checked.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azMenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit)

    /**
     * Adds a toggle switch item that appears in both the collapsed rail and the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param color The color of the item.
     * @param isChecked Whether the switch is checked.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azRailToggle(id: String, text: String, color: Color? = null, isChecked: Boolean, onClick: () -> Unit)

    /**
     * Adds a cycler item that only appears in the expanded menu.
     * A cycler item cycles through a list of options when clicked.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azMenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit)

    /**
     * Adds a cycler item that appears in both the collapsed rail and the expanded menu.
     * A cycler item cycles through a list of options when clicked.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param color The color of the item.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param onClick The callback to be invoked when the item is clicked.
     */
    fun azRailCycler(id: String, text: String, color: Color? = null, options: List<String>, selectedOption: String, onClick: () -> Unit)
}

internal class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableListOf<AzNavItem>()
    var displayAppNameInHeader: Boolean = false
    var packRailButtons: Boolean = false
    var expandedRailWidth: Dp = 260.dp
    var collapsedRailWidth: Dp = 80.dp
    var showFooter: Boolean = true

    override fun azSettings(
        displayAppNameInHeader: Boolean,
        packRailButtons: Boolean,
        expandedRailWidth: Dp,
        collapsedRailWidth: Dp,
        showFooter: Boolean
    ) {
        this.displayAppNameInHeader = displayAppNameInHeader
        this.packRailButtons = packRailButtons
        this.expandedRailWidth = expandedRailWidth
        this.collapsedRailWidth = collapsedRailWidth
        this.showFooter = showFooter
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
