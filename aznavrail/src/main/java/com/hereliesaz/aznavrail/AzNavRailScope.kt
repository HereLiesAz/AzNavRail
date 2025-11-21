package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * A DSL scope for building the content of the [AzNavRail].
 */
interface AzNavRailScope {
    /**
     * Configures the settings for the [AzNavRail].
     * @param displayAppNameInHeader If true, displays the app name in the header instead of the app icon. Defaults to false.
     * @param packRailButtons Whether to pack the rail buttons together at the top of the rail.
     * @param expandedRailWidth The width of the rail when it is expanded.
     * @param collapsedRailWidth The width of the rail when it is collapsed.
     * @param showFooter Whether to show the footer.
     * @param isLoading Whether to show the loading animation.
     */
    fun azSettings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false,
        expandedRailWidth: Dp = 260.dp,
        collapsedRailWidth: Dp = 80.dp,
        showFooter: Boolean = true,
        isLoading: Boolean = false,
        defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
        enableRailDragging: Boolean = false,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
    )

    /**
     * Adds a menu item that only appears in the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azMenuItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null)
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)


    /**
     * Adds a rail item that appears in both the collapsed rail and the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param color The color of the item.
     * @param shape The shape of the button.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azRailItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null)
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle item that only appears in the expanded menu. The text of the item changes to reflect the state.
     * @param id The unique identifier for the item.
     * @param isChecked Whether the toggle is in the "on" state.
     * @param toggleOnText The text to display when the toggle is on.
     * @param toggleOffText The text to display when the toggle is off.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null)

    /**
     * Adds a toggle item that appears in both the collapsed rail and the expanded menu. The text of the item changes to reflect the state.
     * @param id The unique identifier for the item.
     * @param color The color of the item.
     * @param isChecked Whether the toggle is in the "on" state.
     * @param toggleOnText The text to display when the toggle is on.
     * @param toggleOffText The text to display when the toggle is off.
     * @param shape The shape of the button.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null)

    /**
     * Adds a cycler item that only appears in the expanded menu.
     *
     * A cycler item cycles through a list of options when clicked. The displayed option is updated
     * immediately, but the `onClick` action is delayed by one second. Each click resets the timer.
     * The action for the final selected option is triggered after the delay, and the menu collapses.
     *
     * @param id The unique identifier for the item.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions The list of options to disable.
     * @param onClick The callback to be invoked for the final selected option after the delay.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null)

    /**
     * Adds a cycler item that appears in both the collapsed rail and the expanded menu.
     *
     * A cycler item cycles through a list of options when clicked. The displayed option is updated
     * immediately, but the `onClick` action is delayed by one second. Each click resets the timer.
     * The action for the final selected option is triggered after the delay, and the menu collapses.
     *
     * @param id The unique identifier for the item.
     * @param color The color of the item.
     * @param options The list of options to cycle through.
     * @param selectedOption The currently selected option.
     * @param shape The shape of the button.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions The list of options to disable.
     * @param onClick The callback to be invoked for the final selected option after the delay.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null)

    /**
     * Adds a divider to the expanded menu.
     */
    fun azDivider()

    /**
     * Adds a host item that only appears in the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azMenuHostItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null)
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)


    /**
     * Adds a host item that appears in both the collapsed rail and the expanded menu.
     * @param id The unique identifier for the item.
     * @param text The text to display for the item.
     * @param color The color of the item.
     * @param shape The shape of the button.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azRailHostItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null)
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)


    /**
     * Adds a sub item that only appears in the expanded menu.
     * @param id The unique identifier for the item.
     * @param hostId The unique identifier of the host item.
     * @param text The text to display for the item.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null)
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)


    /**
     * Adds a sub item that appears in both the collapsed rail and the expanded menu.
     * @param id The unique identifier for the item.
     * @param hostId The unique identifier of the host item.
     * @param text The text to display for the item.
     * @param disabled Whether the item is disabled.
     * @param onClick The callback to be invoked when the item is clicked.
     * @param screenTitle The text to display as the screen title when this item is selected.
     * @param route The route to navigate to when the item is clicked.
     */
    fun azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null)
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)

    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null)

    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean = false, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean = false, screenTitle: String? = null)

    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null)

    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, onClick: () -> Unit)
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null)
}

internal class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableStateListOf<AzNavItem>()
    val onClickMap = mutableMapOf<String, () -> Unit>()
    var navController: NavController? = null
    var displayAppNameInHeader: Boolean = false
    var packRailButtons: Boolean = false
    var expandedRailWidth: Dp = 260.dp
    var collapsedRailWidth: Dp = 80.dp
    var showFooter: Boolean = true
    var isLoading: Boolean = false
    var defaultShape: AzButtonShape = AzButtonShape.CIRCLE
    var enableRailDragging: Boolean = false
    var headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE

    override fun azSettings(
        displayAppNameInHeader: Boolean,
        packRailButtons: Boolean,
        expandedRailWidth: Dp,
        collapsedRailWidth: Dp,
        showFooter: Boolean,
        isLoading: Boolean,
        defaultShape: AzButtonShape,
        enableRailDragging: Boolean,
        headerIconShape: AzHeaderIconShape
    ) {
        require(expandedRailWidth > collapsedRailWidth) {
            """
            `expandedRailWidth` must be greater than `collapsedRailWidth`.

            // azSettings sample
            azSettings(
                expandedRailWidth = 260.dp,
                collapsedRailWidth = 80.dp
            )
            """.trimIndent()
        }
        this.displayAppNameInHeader = displayAppNameInHeader
        this.packRailButtons = packRailButtons
        this.expandedRailWidth = expandedRailWidth
        this.collapsedRailWidth = collapsedRailWidth
        this.showFooter = showFooter
        this.isLoading = isLoading
        this.defaultShape = defaultShape
        this.enableRailDragging = enableRailDragging
        this.headerIconShape = headerIconShape
    }

    override fun azMenuItem(id: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuItem(id, text, null, disabled, screenTitle, onClick)
    }

    override fun azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?) {
        addMenuItem(id, text, route, disabled, screenTitle) {}
    }

    override fun azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuItem(id, text, route, disabled, screenTitle, onClick)
    }

    private fun addMenuItem(id: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = false, disabled = disabled, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailItem(id, text, null, color, shape, disabled, screenTitle, onClick)
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?) {
        addRailItem(id, text, route, color, shape, disabled, screenTitle) {}
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailItem(id, text, route, color, shape, disabled, screenTitle, onClick)
    }

    private fun addRailItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = true, color = color, shape = shape, disabled = disabled, onClick = onClick)
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, null, disabled, screenTitle, onClick)
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, onClick)
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle) {}
    }

    private fun addMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, isRailItem = false, isSubItem = false, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, null, disabled, screenTitle, onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle) {}
    }

    private fun addRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addToggle(
            id = id,
            color = color,
            isChecked = isChecked,
            toggleOnText = toggleOnText,
            toggleOffText = toggleOffText,
            shape = shape ?: defaultShape,
            route = route,
            disabled = disabled,
            screenTitle = screenTitle,
            isRailItem = true,
            isSubItem = false,
            onClick = onClick
        )
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addMenuCycler(id, options, selectedOption, null, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?) {
        addMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle) {}
    }

    private fun addMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addCycler(
            id = id,
            options = options,
            selectedOption = selectedOption,
            route = route,
            disabled = disabled,
            disabledOptions = disabledOptions,
            screenTitle = screenTitle,
            isRailItem = false,
            isSubItem = false,
            shape = AzButtonShape.NONE,
            onClick = onClick
        )
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addRailCycler(id, color, options, selectedOption, shape, null, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addRailCycler(id, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?) {
        addRailCycler(id, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle) {}
    }

    private fun addRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addCycler(
            id = id,
            color = color,
            options = options,
            selectedOption = selectedOption,
            shape = shape ?: defaultShape,
            route = route,
            disabled = disabled,
            disabledOptions = disabledOptions,
            screenTitle = screenTitle,
            isRailItem = true,
            isSubItem = false,
            onClick = onClick
        )
    }

    override fun azDivider() {
        navItems.add(AzNavItem(id = "divider_${navItems.size}", text = "", isRailItem = false, isDivider = true))
    }

    override fun azMenuHostItem(id: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuHostItem(id, text, null, disabled, screenTitle, onClick)
    }

    override fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?) {
        addMenuHostItem(id, text, route, disabled, screenTitle) {}
    }

    override fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuHostItem(id, text, route, disabled, screenTitle, onClick)
    }

    private fun addMenuHostItem(id: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = false, disabled = disabled, isHost = true, onClick = onClick)
    }

    override fun azRailHostItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailHostItem(id, text, null, color, shape, disabled, screenTitle, onClick)
    }

    override fun azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?) {
        addRailHostItem(id, text, route, color, shape, disabled, screenTitle) {}
    }

    override fun azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailHostItem(id, text, route, color, shape, disabled, screenTitle, onClick)
    }

    private fun addRailHostItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = true, color = color, shape = shape, disabled = disabled, isHost = true, onClick = onClick)
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubItem(id, hostId, text, null, disabled, screenTitle, onClick)
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?) {
        addMenuSubItem(id, hostId, text, route, disabled, screenTitle) {}
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubItem(id, hostId, text, route, disabled, screenTitle, onClick)
    }

    private fun addMenuSubItem(id: String, hostId: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = false, disabled = disabled, isSubItem = true, hostId = hostId, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailSubItem(id, hostId, text, null, disabled, screenTitle, onClick)
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?) {
        addRailSubItem(id, hostId, text, route, disabled, screenTitle) {}
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailSubItem(id, hostId, text, route, disabled, screenTitle, onClick)
    }

    private fun addRailSubItem(id: String, hostId: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, isRailItem = true, disabled = disabled, isSubItem = true, hostId = hostId, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubCycler(id, hostId, options, selectedOption, null, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?) {
        addMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle) {}
    }

    private fun addMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, isRailItem = false, isSubItem = true, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, null, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle) {}
    }

    private fun addRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit) {
        addCycler(id = id, hostId = hostId, color = color, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, isRailItem = true, isSubItem = true, shape = shape, onClick = onClick)
    }

    private fun addCycler(
        id: String,
        hostId: String? = null,
        color: Color? = null,
        options: List<String>,
        selectedOption: String,
        route: String?,
        disabled: Boolean,
        disabledOptions: List<String>?,
        screenTitle: String?,
        isRailItem: Boolean,
        isSubItem: Boolean,
        shape: AzButtonShape?,
        onClick: () -> Unit
    ) {
        require(selectedOption in options) {
            """
            `selectedOption` must be one of the provided options.
            """.trimIndent()
        }
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: selectedOption
        onClickMap[id] = onClick
        navItems.add(
            AzNavItem(
                id = id,
                text = "",
                route = route,
                screenTitle = finalScreenTitle,
                isRailItem = isRailItem,
                color = color,
                isCycler = true,
                options = options,
                selectedOption = selectedOption,
                shape = shape ?: defaultShape,
                disabled = disabled,
                disabledOptions = disabledOptions,
                isSubItem = isSubItem,
                hostId = hostId
            )
        )
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, null, disabled, screenTitle, onClick)
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, onClick)
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle) {}
    }

    private fun addMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, isRailItem = false, isSubItem = true, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, null, disabled, screenTitle, onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle) {}
    }

    private fun addRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit) {
        addToggle(id = id, hostId = hostId, color = color, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, isRailItem = true, isSubItem = true, shape = shape, onClick = onClick)
    }

    private fun addToggle(
        id: String,
        hostId: String? = null,
        color: Color? = null,
        isChecked: Boolean,
        toggleOnText: String,
        toggleOffText: String,
        route: String?,
        disabled: Boolean,
        screenTitle: String?,
        isRailItem: Boolean,
        isSubItem: Boolean,
        shape: AzButtonShape?,
        onClick: () -> Unit
    ) {
        require(toggleOnText.isNotEmpty() && toggleOffText.isNotEmpty()) {
            """
            `toggleOnText` and `toggleOffText` must not be empty.
            """.trimIndent()
        }
        val text = if (isChecked) toggleOnText else toggleOffText
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text
        onClickMap[id] = onClick
        navItems.add(
            AzNavItem(
                id = id,
                text = "",
                route = route,
                screenTitle = finalScreenTitle,
                isRailItem = isRailItem,
                color = color,
                isToggle = true,
                isChecked = isChecked,
                toggleOnText = toggleOnText,
                toggleOffText = toggleOffText,
                shape = shape ?: defaultShape,
                disabled = disabled,
                isSubItem = isSubItem,
                hostId = hostId
            )
        )
    }

    private fun addItem(
        id: String,
        text: String,
        route: String?,
        screenTitle: String?,
        isRailItem: Boolean,
        color: Color? = null,
        shape: AzButtonShape? = null,
        disabled: Boolean = false,
        isHost: Boolean = false,
        isSubItem: Boolean = false,
        hostId: String? = null,
        onClick: () -> Unit
    ) {
        require(text.isNotEmpty()) {
            """
            `text` must not be empty for item with id `$id`.
            """.trimIndent()
        }
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text
        onClickMap[id] = onClick
        navItems.add(
            AzNavItem(
                id = id,
                text = text,
                route = route,
                screenTitle = finalScreenTitle,
                isRailItem = isRailItem,
                color = color,
                shape = shape ?: defaultShape,
                disabled = disabled,
                isHost = isHost,
                isSubItem = isSubItem,
                hostId = hostId
            )
        )
    }
}
