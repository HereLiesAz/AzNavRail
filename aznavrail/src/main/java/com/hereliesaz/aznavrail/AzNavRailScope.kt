package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzItemConfig
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import kotlinx.parcelize.RawValue

/**
 * Scope for configuring the [AzNavRail] and its items.
 *
 * This interface defines the Domain Specific Language (DSL) used to set up the navigation rail's
 * theme, configuration, advanced settings, and navigation items.
 */
interface AzNavRailScope {

    /**
     * ⛔ DEPRECATED: THE BUREAUCRACY HAS ABOLISHED THIS FUNCTION.
     *
     * You must now split your configuration into three distinct sectors:
     * 1. [azTheme] - For visuals (colors, shapes, dimensions).
     * 2. [azConfig] - For behavior (docking, packing, vibration).
     * 3. [azAdvanced] - For special operations (overlays, help screens).
     *
     * Consult the Golden Sample in the README for the strict protocol.
     */
    @Deprecated(
        message = "VIOLATION: azSettings has been dissolved. Use azTheme, azConfig, and azAdvanced.",
        level = DeprecationLevel.ERROR
    )
    fun azSettings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false,
        expandedRailWidth: Dp = 260.dp,
        collapsedRailWidth: Dp = 80.dp,
        showFooter: Boolean = true,
        isLoading: Boolean = false,
        defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
        enableRailDragging: Boolean = false,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
        onUndock: (() -> Unit)? = null,
        onRailDrag: ((Float, Float) -> Unit)? = null,
        overlayService: Class<out android.app.Service>? = null,
        onOverlayDrag: ((Float, Float) -> Unit)? = null,
        onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
        infoScreen: Boolean = false,
        onDismissInfoScreen: (() -> Unit)? = null,
        activeColor: Color? = null,
        vibrate: Boolean = false,
        activeClassifiers: Set<String> = emptySet(),
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        noMenu: Boolean = false,
        secLoc: String? = null
    )

    /**
     * Configures the visual theme of the navigation rail.
     *
     * @param activeColor The color used for the active/selected item.
     * @param defaultShape The default shape for rail buttons.
     * @param headerIconShape The shape of the header icon (app icon).
     * @param expandedWidth The width of the rail when expanded.
     * @param collapsedWidth The width of the rail when collapsed.
     * @param showFooter Whether to display the footer section.
     */
    fun azTheme(
        activeColor: Color? = null,
        defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
        expandedWidth: Dp = 130.dp,
        collapsedWidth: Dp = 80.dp,
        showFooter: Boolean = true
    )

    /**
     * Configures the behavioral settings of the navigation rail.
     *
     * @param dockingSide The side of the screen where the rail is docked ([AzDockingSide.LEFT] or [AzDockingSide.RIGHT]).
     * @param packButtons If true, reduces the vertical spacing between rail items.
     * @param noMenu If true, disables the expanded menu drawer, treating all items as rail-only.
     * @param vibrate If true, enables haptic feedback for gestures.
     * @param displayAppName If true, displays the application name in the header.
     * @param activeClassifiers A set of classifiers that mark items as active beyond standard route matching.
     * @param usePhysicalDocking If true, enables experimental physical docking logic.
     */
    fun azConfig(
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        packButtons: Boolean = false,
        noMenu: Boolean = false,
        vibrate: Boolean = false,
        displayAppName: Boolean = false,
        activeClassifiers: Set<String> = emptySet(),
        usePhysicalDocking: Boolean = false
    )

    /**
     * Configures advanced and special operations for the navigation rail.
     *
     * @param isLoading If true, shows a full-screen loading overlay.
     * @param infoScreen If true, activates the interactive help mode overlay.
     * @param onDismissInfoScreen Callback invoked when the help mode is dismissed.
     * @param overlayService The Service class to launch as a system overlay when undocked.
     * @param onUndock Callback invoked when the undock action is triggered.
     * @param enableRailDragging If true, allows the rail to be dragged (FAB mode).
     * @param onRailDrag Callback to handle drag events when the rail is floating.
     * @param onOverlayDrag Callback to handle drag events when running in an overlay.
     * @param onItemGloballyPositioned Callback to report the global position of rail items (used for help overlays).
     */
    fun azAdvanced(
        isLoading: Boolean = false,
        infoScreen: Boolean = false,
        onDismissInfoScreen: (() -> Unit)? = null,
        overlayService: Class<out android.app.Service>? = null,
        onUndock: (() -> Unit)? = null,
        enableRailDragging: Boolean = false,
        onRailDrag: ((Float, Float) -> Unit)? = null,
        onOverlayDrag: ((Float, Float) -> Unit)? = null,
        onItemGloballyPositioned: ((String, Rect) -> Unit)? = null
    )

    /**
     * Adds a menu item that appears only in the expanded menu drawer.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azMenuItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a menu item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a menu item that appears only in the expanded menu drawer, associated with a navigation route and a custom click action.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a rail item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus (e.g., in TV or keyboard navigation).
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a rail item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     */
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null)

    /**
     * Adds a rail item that appears in both the collapsed rail and the expanded menu, associated with a navigation route and a custom click action.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a rail item with dynamic content (Color, Number, Image).
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param content The dynamic content to display on the rail button (Color, Number, or Image URL/Resource).
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, content: Any, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a rail item with dynamic content (Color, Number, Image) and a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param content The dynamic content to display on the rail button.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     */
    fun azRailItem(id: String, text: String, route: String, content: Any, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null)

    /**
     * Adds a rail item with dynamic content (Color, Number, Image), a navigation route, and a custom click action.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param content The dynamic content to display on the rail button.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, route: String, content: Any, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a rail item configured via [AzItemConfig].
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param config The configuration object for the item.
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, config: AzItemConfig, onClick: () -> Unit)

    /**
     * Adds a rail item associated with a route, configured via [AzItemConfig].
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param config The configuration object for the item.
     */
    fun azRailItem(id: String, text: String, route: String, config: AzItemConfig = AzItemConfig())

    /**
     * Adds a rail item associated with a route and custom action, configured via [AzItemConfig].
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param config The configuration object for the item.
     * @param onClick The action to perform when clicked.
     */
    fun azRailItem(id: String, text: String, route: String, config: AzItemConfig, onClick: () -> Unit)

    /**
     * Defines a nested rail structure.
     *
     * Nested rails allow for hierarchical navigation. Items defined within the [content] block are displayed
     * in a popup anchored to the parent item.
     *
     * @param id The unique identifier for the parent item.
     * @param text The text to display for the parent item.
     * @param alignment The alignment of the nested rail ([AzNestedRailAlignment.VERTICAL] or [AzNestedRailAlignment.HORIZONTAL]).
     * @param color The specific color for the parent item's button.
     * @param shape The specific shape for the parent item's button.
     * @param disabled Whether the parent item is disabled.
     * @param screenTitle The title to display on the screen when the parent item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the parent item gains focus.
     * @param content The DSL scope for defining nested items.
     */
    fun azNestedRail(id: String, text: String, alignment: AzNestedRailAlignment = AzNestedRailAlignment.VERTICAL, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, content: AzNavRailScope.() -> Unit)

    /**
     * Adds a toggle item that appears only in the expanded menu drawer.
     *
     * @param id The unique identifier for the item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a toggle item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a cycler item that appears only in the expanded menu drawer.
     *
     * A cycler item cycles through a list of options when clicked.
     *
     * @param id The unique identifier for the item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes (after a delay).
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a cycler item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a horizontal divider to the expanded menu drawer.
     */
    fun azDivider()

    /**
     * Adds a host item that appears only in the expanded menu drawer.
     *
     * Host items can contain sub-items.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked (toggles expansion).
     */
    fun azMenuHostItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a host item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a host item that appears only in the expanded menu drawer, associated with a navigation route and custom action.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a host item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azRailHostItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a host item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a host item that appears in both the collapsed rail and the expanded menu, associated with a navigation route and custom action.
     *
     * @param id The unique identifier for the item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a sub-item that appears only in the expanded menu drawer.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a sub-item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    /**
     * Adds a sub-item that appears only in the expanded menu drawer, associated with a navigation route and custom action.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when clicked.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a sub-item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display in the menu.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick The action to perform when clicked.
     */
    fun azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     */
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null)

    /**
     * Adds a sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route and custom action.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display in the menu.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick The action to perform when clicked.
     */
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    /**
     * Adds a toggle sub-item that appears only in the expanded menu drawer.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle sub-item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle sub-item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String?)

    /**
     * Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when toggled.
     */
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param isChecked Whether the toggle is currently checked.
     * @param toggleOnText The text to display when checked.
     * @param toggleOffText The text to display when unchecked.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String?)

    /**
     * Adds a cycler sub-item that appears only in the expanded menu drawer.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler sub-item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler sub-item that appears only in the expanded menu drawer, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String?)

    /**
     * Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param onClick The action to perform when the cycle completes.
     */
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    /**
     * Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu, associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param color The specific color for this item's button.
     * @param options The list of available options.
     * @param selectedOption The currently selected option.
     * @param shape The specific shape for this item's button.
     * @param route The navigation route to navigate to.
     * @param disabled Whether the item is disabled.
     * @param disabledOptions A list of specific options that are disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     */
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String?)

    /**
     * Adds a reorderable rail sub-item.
     *
     * These items allow users to drag and drop to reorder them within their cluster.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick Callback invoked when the item is clicked.
     * @param onRelocate Callback invoked when the item is reordered. Provides (fromIndex, toIndex, newOrder).
     * @param hiddenMenu Scope to define the context menu that appears on double-tap/long-press interactions.
     */
    fun azRailRelocItem(id: String, hostId: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit = {})

    /**
     * Adds a reorderable rail sub-item associated with a navigation route.
     *
     * @param id The unique identifier for the item.
     * @param hostId The ID of the parent host item.
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     * @param color The specific color for this item's button.
     * @param shape The specific shape for this item's button.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display on the screen when this item is active.
     * @param info The help text for the info screen.
     * @param classifiers A set of strings to classify this item.
     * @param onFocus Callback invoked when the item gains focus.
     * @param onClick Callback invoked when the item is clicked.
     * @param onRelocate Callback invoked when the item is reordered. Provides (fromIndex, toIndex, newOrder).
     * @param hiddenMenu Scope to define the context menu.
     */
    fun azRailRelocItem(id: String, hostId: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit)
}

/**
 * Scope for defining the hidden menu items for relocatable rail items.
 */
interface HiddenMenuScope {
    /**
     * Adds a standard clickable list item to the hidden menu.
     *
     * @param text The text to display.
     * @param onClick The action to perform when clicked.
     */
    fun listItem(text: String, onClick: () -> Unit)

    /**
     * Adds a list item associated with a navigation route to the hidden menu.
     *
     * @param text The text to display.
     * @param route The navigation route to navigate to.
     */
    fun listItem(text: String, route: String)

    /**
     * Adds a text input item to the hidden menu.
     *
     * @param hint The hint text for the input field.
     * @param onValueChange Callback invoked when the input value changes.
     */
    fun inputItem(hint: String, onValueChange: (String) -> Unit)
}

/**
 * Internal implementation of the [HiddenMenuScope].
 */
internal class HiddenMenuScopeImpl : HiddenMenuScope {
    val items = mutableListOf<com.hereliesaz.aznavrail.model.HiddenMenuItem>()
    val onClickMap = mutableMapOf<String, () -> Unit>()
    val onValueChangeMap = mutableMapOf<String, (String) -> Unit>()

    override fun listItem(text: String, onClick: () -> Unit) {
        val id = "hidden_item_${items.size}"
        items.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = text))
        onClickMap[id] = onClick
    }

    override fun listItem(text: String, route: String) {
        val id = "hidden_item_${items.size}"
        items.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = text, route = route))
    }

    override fun inputItem(hint: String, onValueChange: (String) -> Unit) {
        val id = "hidden_item_${items.size}"
        items.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = "", isInput = true, hint = hint))
        onValueChangeMap[id] = onValueChange
    }
}

/**
 * Implementation of the [AzNavRailScope].
 *
 * This class collects the configuration and items defined via the DSL.
 */
class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableStateListOf<AzNavItem>()
    val onClickMap = mutableMapOf<String, () -> Unit>()
    val onFocusMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnClickMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnValueChangeMap = mutableMapOf<String, (String) -> Unit>()
    val onRelocateMap = mutableMapOf<String, (Int, Int, List<String>) -> Unit>()
    var navController: NavController? = null

    fun reset() {
        navItems.clear()
        onClickMap.clear()
        onFocusMap.clear()
        hiddenMenuOnClickMap.clear()
        hiddenMenuOnValueChangeMap.clear()
        onRelocateMap.clear()
    }
    
    // Config Properties
    var activeColor: Color? = null
    var defaultShape: AzButtonShape = AzButtonShape.CIRCLE
    var headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
    var expandedWidth: Dp = 130.dp
    var collapsedWidth: Dp = 80.dp
    var showFooter: Boolean = true
    
    var dockingSide: AzDockingSide = AzDockingSide.LEFT
    var packButtons: Boolean = false
    var noMenu: Boolean = false
    var vibrate: Boolean = false
    var displayAppName: Boolean = false
    var activeClassifiers: Set<String> = emptySet()
    var usePhysicalDocking: Boolean = false

    var isLoading: Boolean = false
    var infoScreen: Boolean = false
    var onDismissInfoScreen: (() -> Unit)? = null
    var overlayService: Class<out android.app.Service>? = null
    var onUndock: (() -> Unit)? = null
    var enableRailDragging: Boolean = false
    var onRailDrag: ((Float, Float) -> Unit)? = null
    var onOverlayDrag: ((Float, Float) -> Unit)? = null
    var onItemGloballyPositioned: ((String, Rect) -> Unit)? = null
    
    // Internal usage/legacy check
    var secLoc: String? = null

    @Deprecated(
        message = "VIOLATION: azSettings has been dissolved. Use azTheme, azConfig, and azAdvanced.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("OverridingDeprecatedMember")
    override fun azSettings(
        displayAppNameInHeader: Boolean,
        packRailButtons: Boolean,
        expandedRailWidth: Dp,
        collapsedRailWidth: Dp,
        showFooter: Boolean,
        isLoading: Boolean,
        defaultShape: AzButtonShape,
        enableRailDragging: Boolean,
        headerIconShape: AzHeaderIconShape,
        onUndock: (() -> Unit)?,
        onRailDrag: ((Float, Float) -> Unit)?,
        overlayService: Class<out android.app.Service>?,
        onOverlayDrag: ((Float, Float) -> Unit)?,
        onItemGloballyPositioned: ((String, Rect) -> Unit)?,
        infoScreen: Boolean,
        onDismissInfoScreen: (() -> Unit)?,
        activeColor: Color?,
        vibrate: Boolean,
        activeClassifiers: Set<String>,
        dockingSide: AzDockingSide,
        noMenu: Boolean,
        secLoc: String?
    ) {
        throw IllegalStateException(
            """
            FATAL ERROR: You are attempting to use the forbidden 'azSettings' function.
            
            This layout architecture has been strictly reorganized.
            
            MIGRATION ORDER:
            1. Move visual settings (colors, shapes, widths) to 'azTheme { ... }'
            2. Move behavior settings (docking, packing, vibration) to 'azConfig { ... }'
            3. Move advanced settings (overlays, help screens) to 'azAdvanced { ... }'
            
            FAILURE TO COMPLY WILL RESULT IN BUILD FAILURES.
            """.trimIndent()
        )
    }

    override fun azTheme(
        activeColor: Color?,
        defaultShape: AzButtonShape,
        headerIconShape: AzHeaderIconShape,
        expandedWidth: Dp,
        collapsedWidth: Dp,
        showFooter: Boolean
    ) {
        require(expandedWidth > collapsedWidth) {
            "LAYOUT VIOLATION: `expandedWidth` ($expandedWidth) must be greater than `collapsedWidth` ($collapsedWidth)."
        }
        this.activeColor = activeColor
        this.defaultShape = defaultShape
        this.headerIconShape = headerIconShape
        this.expandedWidth = expandedWidth
        this.collapsedWidth = collapsedWidth
        this.showFooter = showFooter
    }

    override fun azConfig(
        dockingSide: AzDockingSide,
        packButtons: Boolean,
        noMenu: Boolean,
        vibrate: Boolean,
        displayAppName: Boolean,
        activeClassifiers: Set<String>,
        usePhysicalDocking: Boolean
    ) {
        this.dockingSide = dockingSide
        this.packButtons = packButtons
        this.noMenu = noMenu
        this.vibrate = vibrate
        this.displayAppName = displayAppName
        this.activeClassifiers = activeClassifiers
        this.usePhysicalDocking = usePhysicalDocking
    }

    override fun azAdvanced(
        isLoading: Boolean,
        infoScreen: Boolean,
        onDismissInfoScreen: (() -> Unit)?,
        overlayService: Class<out android.app.Service>?,
        onUndock: (() -> Unit)?,
        enableRailDragging: Boolean,
        onRailDrag: ((Float, Float) -> Unit)?,
        onOverlayDrag: ((Float, Float) -> Unit)?,
        onItemGloballyPositioned: ((String, Rect) -> Unit)?
    ) {
        this.isLoading = isLoading
        this.infoScreen = infoScreen
        this.onDismissInfoScreen = onDismissInfoScreen
        this.overlayService = overlayService
        this.onUndock = onUndock
        this.enableRailDragging = enableRailDragging || overlayService != null || onOverlayDrag != null
        this.onRailDrag = onRailDrag
        this.onOverlayDrag = onOverlayDrag
        this.onItemGloballyPositioned = onItemGloballyPositioned
    }

    override fun azMenuItem(id: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuItem(id, text, null, disabled, screenTitle, info, onClick)
    }

    override fun azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addMenuItem(id, text, route, disabled, screenTitle, info) {}
    }

    override fun azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuItem(id, text, route, disabled, screenTitle, info, onClick)
    }

    private fun addMenuItem(id: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailItem(id = id, text = text, route = null, content = null, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?) {
        addRailItem(id = id, text = text, route = route, content = null, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = {})
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailItem(id = id, text = text, route = route, content = null, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, config: AzItemConfig, onClick: () -> Unit) {
        addRailItem(
            id = id, text = text, route = null, content = null, color = config.color, shape = config.shape, disabled = config.disabled, screenTitle = config.screenTitle, info = config.info, classifiers = config.classifiers, onFocus = config.onFocus, onClick = onClick
        )
    }

    override fun azRailItem(id: String, text: String, route: String, config: AzItemConfig) {
        addRailItem(
            id = id, text = text, route = route, content = null, color = config.color, shape = config.shape, disabled = config.disabled, screenTitle = config.screenTitle, info = config.info, classifiers = config.classifiers, onFocus = config.onFocus, onClick = {}
        )
    }

    override fun azRailItem(id: String, text: String, content: Any, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailItem(id = id, text = text, route = null, content = content, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, route: String, content: Any, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?) {
        addRailItem(id = id, text = text, route = route, content = content, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = {})
    }

    override fun azRailItem(id: String, text: String, route: String, content: Any, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailItem(id = id, text = text, route = route, content = content, color = color, shape = shape, disabled = disabled, screenTitle = screenTitle, info = info, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
    }

    override fun azRailItem(id: String, text: String, route: String, config: AzItemConfig, onClick: () -> Unit) {
        addRailItem(
            id = id, text = text, route = route, content = null, color = config.color, shape = config.shape, disabled = config.disabled, screenTitle = config.screenTitle, info = config.info, classifiers = config.classifiers, onFocus = config.onFocus, onClick = onClick
        )
    }

    private fun addRailItem(id: String, text: String, route: String?, content: Any? = null, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, color = color, shape = shape, disabled = disabled, classifiers = classifiers, onFocus = onFocus, content = content, onClick = onClick)
    }

    override fun azNestedRail(id: String, text: String, alignment: AzNestedRailAlignment, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, content: AzNavRailScope.() -> Unit) {
        val nestedScope = AzNavRailScopeImpl()
        nestedScope.azConfig(dockingSide = this.dockingSide, packButtons = this.packButtons, noMenu = this.noMenu, vibrate = this.vibrate, displayAppName = this.displayAppName, activeClassifiers = this.activeClassifiers)
        nestedScope.azTheme(activeColor = this.activeColor, defaultShape = this.defaultShape, headerIconShape = this.headerIconShape, expandedWidth = this.expandedWidth, collapsedWidth = this.collapsedWidth, showFooter = this.showFooter)
        nestedScope.content()

        val nestedItems = nestedScope.navItems.toList()

        // Merge maps
        nestedScope.onClickMap.forEach { (k, v) -> onClickMap[k] = v }
        nestedScope.onFocusMap.forEach { (k, v) -> onFocusMap[k] = v }
        nestedScope.hiddenMenuOnClickMap.forEach { (k, v) -> hiddenMenuOnClickMap[k] = v }
        nestedScope.hiddenMenuOnValueChangeMap.forEach { (k, v) -> hiddenMenuOnValueChangeMap[k] = v }
        nestedScope.onRelocateMap.forEach { (k, v) -> onRelocateMap[k] = v }

        if (onFocus != null) {
            onFocusMap[id] = onFocus
        }

        navItems.add(
            AzNavItem(
                id = id,
                text = text,
                isRailItem = true,
                isNestedRail = true,
                nestedRailAlignment = alignment,
                nestedRailItems = nestedItems,
                color = color,
                shape = shape ?: defaultShape,
                disabled = disabled,
                screenTitle = screenTitle ?: text,
                info = info,
                classifiers = classifiers
            )
        )
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, null, disabled, screenTitle, info, onClick)
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info) {}
    }

    private fun addMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, null, disabled, screenTitle, info, onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info, onClick)
    }

    override fun azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info) {}
    }

    private fun addRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
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
            info = info,
            isRailItem = true,
            isSubItem = false,
            onClick = onClick
        )
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuCycler(id, options, selectedOption, null, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?) {
        addMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info) {}
    }

    private fun addMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addCycler(id = id, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailCycler(id, color, options, selectedOption, shape, null, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailCycler(id, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?) {
        addRailCycler(id, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info) {}
    }

    private fun addRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addCycler(id = id, hostId = null, color = color, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, shape = shape, onClick = onClick)
    }

    override fun azDivider() {
        navItems.add(AzNavItem(id = "divider_${navItems.size}", text = "", isRailItem = false, isDivider = true))
    }

    override fun azMenuHostItem(id: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuHostItem(id, text, null, disabled, screenTitle, info, onClick)
    }

    override fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addMenuHostItem(id, text, route, disabled, screenTitle, info) {}
    }

    override fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuHostItem(id, text, route, disabled, screenTitle, info, onClick)
    }

    private fun addMenuHostItem(id: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isHost = true, onClick = onClick)
    }

    override fun azRailHostItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailHostItem(id, text, null, color, shape, disabled, screenTitle, info, onClick)
    }

    override fun azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?) {
        addRailHostItem(id, text, route, color, shape, disabled, screenTitle, info) {}
    }

    override fun azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailHostItem(id, text, route, color, shape, disabled, screenTitle, info, onClick)
    }

    private fun addRailHostItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, color = color, shape = shape, disabled = disabled, isHost = true, onClick = onClick)
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubItem(id, hostId, text, null, disabled, screenTitle, info, onClick)
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addMenuSubItem(id, hostId, text, route, disabled, screenTitle, info) {}
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubItem(id, hostId, text, route, disabled, screenTitle, info, onClick)
    }

    private fun addMenuSubItem(id: String, hostId: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isSubItem = true, hostId = hostId, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailSubItem(id, hostId, text, null, disabled, screenTitle, info, classifiers, onFocus, onClick)
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?) {
        addRailSubItem(id, hostId, text, route, disabled, screenTitle, info, classifiers, onFocus) {}
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailSubItem(id, hostId, text, route, disabled, screenTitle, info, classifiers, onFocus, onClick)
    }

    private fun addRailSubItem(id: String, hostId: String, text: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isSubItem = true, hostId = hostId, shape = AzButtonShape.NONE, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, null, disabled, screenTitle, info, onClick)
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info) {}
    }

    private fun addMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, null, disabled, screenTitle, info, onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info, onClick)
    }

    override fun azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?) {
        addRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info) {}
    }

    private fun addRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addToggle(
            id = id,
            hostId = hostId,
            color = color,
            isChecked = isChecked,
            toggleOnText = toggleOnText,
            toggleOffText = toggleOffText,
            route = route,
            disabled = disabled,
            screenTitle = screenTitle,
            info = info,
            isRailItem = true,
            isSubItem = true,
            shape = shape,
            onClick = onClick
        )
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubCycler(id, hostId, options, selectedOption, null, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?) {
        addMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, info) {}
    }

    private fun addMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, shape = AzButtonShape.NONE, onClick = onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, null, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info, onClick)
    }

    override fun azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?) {
        addRailSubCycler(id, hostId, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info) {}
    }

    private fun addRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit) {
        addCycler(id = id, hostId = hostId, color = color, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, shape = shape, onClick = onClick)
    }

    override fun azRailRelocItem(id: String, hostId: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        addRailRelocItem(id, hostId, text, null, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, hiddenMenu)
    }

    override fun azRailRelocItem(id: String, hostId: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        addRailRelocItem(id, hostId, text, route, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, hiddenMenu)
    }

    private fun addRailRelocItem(id: String, hostId: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        val hiddenMenuScope = HiddenMenuScopeImpl()
        hiddenMenuScope.hiddenMenu()

        // Prefix keys with parent ID to avoid collision
        hiddenMenuScope.onClickMap.forEach { (key, value) ->
            hiddenMenuOnClickMap["${id}_$key"] = value
        }
        hiddenMenuScope.onValueChangeMap.forEach { (key, value) ->
            hiddenMenuOnValueChangeMap["${id}_$key"] = value
        }

        val prefixedItems = hiddenMenuScope.items.map { it.copy(id = "${id}_${it.id}") }

        if (onClick != null) {
            onClickMap[id] = onClick
        }
        if (onRelocate != null) {
            onRelocateMap[id] = onRelocate
        }
        if (onFocus != null) {
            onFocusMap[id] = onFocus
        }

        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text

        navItems.add(
            AzNavItem(
                id = id,
                text = text,
                route = route,
                isRailItem = true,
                isSubItem = true,
                hostId = hostId,
                isRelocItem = true,
                color = color,
                shape = shape ?: defaultShape,
                disabled = disabled,
                screenTitle = finalScreenTitle,
                info = info,
                hiddenMenuItems = prefixedItems,
                classifiers = classifiers
            )
        )
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
        info: String?,
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
                hostId = hostId,
                info = info
            )
        )
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
        info: String?,
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
                hostId = hostId,
                info = info
            )
        )
    }

    private fun addItem(
        id: String,
        text: String,
        route: String?,
        screenTitle: String?,
        info: String?,
        isRailItem: Boolean,
        color: Color? = null,
        shape: AzButtonShape? = null,
        disabled: Boolean = false,
        isHost: Boolean = false,
        isSubItem: Boolean = false,
        hostId: String? = null,
        classifiers: Set<String> = emptySet(),
        onFocus: (() -> Unit)? = null,
        content: Any? = null,
        onClick: () -> Unit
    ) {
        require(text.isNotEmpty()) {
            """
            `text` must not be empty for item with id `$id`.
            """.trimIndent()
        }
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text
        onClickMap[id] = onClick
        if (onFocus != null) {
            onFocusMap[id] = onFocus
        }
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
                hostId = hostId,
                info = info,
                classifiers = classifiers,
                content = content
            )
        )
    }
}
