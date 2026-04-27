package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzItemConfig
import com.hereliesaz.aznavrail.model.AzAdvancedConfig
import java.util.Collections.emptySet

/**
 * The primary DSL scope for configuring the AzNavRail.
 *
 * This interface provides methods to define navigation items, menus, settings, and advanced behaviors.
 * It is used within the content lambda of [AzNavRail] and [AzHostActivityLayout].
 */
interface AzNavRailScope {
    // Legacy/DSL split methods

    /**
     * Configures the behavioral and layout properties of the rail.
     *
     * @param dockingSide The side of the screen where the rail is docked (Left/Right).
     * @param packButtons Whether to pack buttons tightly (true) or space them out (false).
     * @param noMenu If true, disables the side menu drawer; all items are treated as rail items.
     * @param vibrate Whether to provide haptic feedback on interactions.
     * @param displayAppName If true, displays the app name in the header instead of the icon.
     * @param activeClassifiers A set of strings used to highlight items programmatically.
     * @param usePhysicalDocking If true, anchors the rail to the physical side of the device (rotates with device).
     * @param expandedWidth The width of the rail when expanded (menu visible).
     * @param collapsedWidth The width of the rail when collapsed.
     * @param showFooter Whether to show the footer (Privacy, Terms, Help) in the menu.
     * @param appRepositoryUrl The URL of the application's repository to link to in the footer's "About" section. Defaults to the AzNavRail repo.
     */
    fun azConfig(dockingSide: AzDockingSide = AzDockingSide.LEFT, packButtons: Boolean = false, noMenu: Boolean = false, vibrate: Boolean = false, displayAppName: Boolean = false, activeClassifiers: Set<String> = emptySet(), usePhysicalDocking: Boolean = false, expandedWidth: Dp = 160.dp, collapsedWidth: Dp = 100.dp, showFooter: Boolean = true, appRepositoryUrl: String = "https://github.com/HereLiesAz/AzNavRail")

    /**
     * Configures the visual theme of the rail.
     *
     * @param activeColor The color used for active/selected items.
     * @param defaultShape The default shape for buttons (Circle, Square, Rectangle, None).
     * @param headerIconShape The shape of the header icon (Circle, Rounded, None).
     * @param translucentBackground The translucent background color for the rail and popup menus.
     * @param helpLineColors List of colors to use for the connecting lines in the Help overlay.
     */
    fun azTheme(activeColor: Color = Color.Unspecified, defaultShape: AzButtonShape = AzButtonShape.CIRCLE, headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE, translucentBackground: Color = Color.Unspecified, helpLineColors: List<Color> = emptyList())

    /**
     * Configures advanced features like loading states, help screens, and overlays.
     *
     * @param isLoading Whether to show a full-screen loading spinner.
     * @param helpEnabled Whether to show the interactive help overlay.
     * @param onDismissHelp Callback invoked when the help screen is dismissed.
     * @param overlayService The class of the service to use for the system overlay (FAB mode).
     * @param onUndock Callback invoked when the rail is undocked (FAB mode activated).
     * @param enableRailDragging Whether to allow the user to detach and drag the rail (FAB mode).
     * @param onRailDrag Callback invoked during rail drag events.
     * @param onOverlayDrag Callback invoked during overlay drag events.
     * @param onItemGloballyPositioned Callback invoked with the bounds of items (used for tutorials/help).
     * @param secLoc Optional developer configuration key to enable the Secret Screens.
     * @param secLocPort The network port used for the location history sync server. Defaults to 10203.
     * @param helpList An optional map of Item ID to help text.
     * @param tutorials An optional map of Item ID to interactive AzTutorials.
     */
    fun azAdvanced(isLoading: Boolean = false, helpEnabled: Boolean = false, onDismissHelp: (() -> Unit)? = null, overlayService: Class<out android.app.Service>? = null, onUndock: (() -> Unit)? = null, enableRailDragging: Boolean = false, onRailDrag: ((Float, Float) -> Unit)? = null, onOverlayDrag: ((Float, Float) -> Unit)? = null, onItemGloballyPositioned: ((String, Rect) -> Unit)? = null, secLoc: String? = null, secLocPort: Int = 10203, helpList: Map<String, Any> = emptyMap(), tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial> = emptyMap())

    /**
     * A comprehensive configuration method combining settings, theme, and advanced options.
     * This mirrors the structure used in the `AZNAVRAIL_COMPLETE_GUIDE.md`.
     */
    fun azSettings(
        displayAppNameInHeader: Boolean = false,
        packRailButtons: Boolean = false,
        expandedRailWidth: Dp = 280.dp,
        collapsedRailWidth: Dp = 136.dp,
        showFooter: Boolean = true,
        isLoading: Boolean = false,
        defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
        enableRailDragging: Boolean = false,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
        onUndock: (() -> Unit)? = null,
        overlayService: Class<out android.app.Service>? = null,
        onOverlayDrag: ((Float, Float) -> Unit)? = null,
        onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
        helpEnabled: Boolean = false,
        onDismissHelp: (() -> Unit)? = null,
        activeColor: Color? = null,
        vibrate: Boolean = false,
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        noMenu: Boolean = false,
        usePhysicalDocking: Boolean = false,
        secLoc: String? = null,
        secLocPort: Int = 10203,
        helpList: Map<String, Any> = emptyMap(),
        tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial> = emptyMap(),
        helpLineColors: List<Color> = emptyList()
    )

    /**
     * Adds an item to the expandable menu drawer.
     *
     * @param id Unique identifier for the item.
     * @param text The text to display.
     * @param route The navigation route to navigate to when clicked.
     * @param content Optional custom content (Color, Image, Resource ID).
     * @param color Optional specific color for this item.
     * @param shape Optional specific shape for this item.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display in the header when this item is active.
     * @param info Description text for the Help/Info screen.
     * @param onClick Callback invoked when the item is clicked.
     */
    fun azMenuItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds an explicit Help trigger item to the rail.
     * When clicked, this item toggles the interactive Help/Info overlay.
     *
     * @param id Unique identifier.
     * @param text Text display.
     * @param content Custom content.
     * @param color Custom color.
     * @param shape Custom shape.
     */
    fun azHelpRailItem(id: String, text: String = "Help", content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null)

    /**
     * Adds an explicit Help trigger item as a SubItem to a Host Item.
     * When clicked, this item toggles the interactive Help/Info overlay.
     *
     * @param id Unique identifier.
     * @param hostId The unique identifier of the parent Host Item.
     * @param text Text display.
     * @param content Custom content.
     * @param color Custom color.
     * @param shape Custom shape.
     */
    fun azHelpSubItem(id: String, hostId: String, text: String = "Help", content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null)

    /**
     * Adds an item to the always-visible rail.
     *
     * @param id Unique identifier for the item.
     * @param text The text to display (often hidden in rail mode unless shape is RECTANGLE/NONE).
     * @param route The navigation route to navigate to when clicked.
     * @param content Optional custom content (Color, Image, Resource ID).
     * @param color Optional specific color for this item.
     * @param shape Optional specific shape for this item.
     * @param disabled Whether the item is disabled.
     * @param screenTitle The title to display in the header when this item is active.
     * @param info Description text for the Help/Info screen.
     * @param classifiers Additional strings to identify this item for highlighting.
     * @param onFocus Callback invoked when the item receives focus.
     * @param onClick Callback invoked when the item is clicked.
     */
    fun azRailItem(id: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Nested Rail item. This item opens a secondary popup rail when clicked.
     *
     * @param id Unique identifier.
     * @param text Text display.
     * @param route The navigation route (optional).
     * @param content Custom content.
     * @param color Custom color.
     * @param shape Custom shape.
     * @param alignment Orientation of the nested rail (VERTICAL or HORIZONTAL).
     * @param disabled Whether the item is disabled.
     * @param screenTitle Title to display when active.
     * @param info Help info string.
     * @param classifiers Active classifiers.
     * @param onFocus Focus callback.
     * @param keepNestedRailOpen If true, the nested rail remains open until the parent item is tapped again.
     * @param nestedContent DSL block to define the items within the nested rail.
     */
    fun azNestedRail(id: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, alignment: AzNestedRailAlignment = AzNestedRailAlignment.VERTICAL, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onFocus: (() -> Unit)? = null, keepNestedRailOpen: Boolean = false, nestedContent: AzNavRailScope.() -> Unit)

    /**
     * Adds a toggle switch item to the menu.
     *
     * @param isChecked The current state of the toggle.
     * @param toggleOnText Text to display when checked.
     * @param toggleOffText Text to display when unchecked.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuToggleOnText: String? = null, menuToggleOffText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a toggle switch item to the rail.
     *
     * @param id Unique identifier.
     * @param isChecked The current state of the toggle.
     * @param toggleOnText Text to display when checked.
     * @param toggleOffText Text to display when unchecked.
     * @param route Optional navigation route.
     * @param color Custom color.
     * @param shape Custom shape.
     * @param disabled Whether the item is disabled.
     * @param screenTitle Title to display when active.
     * @param info Help info string.
     * @param onClick Click callback.
     */
    fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuToggleOnText: String? = null, menuToggleOffText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a cycler item (multi-state button) to the menu.
     *
     * @param options List of possible string options.
     * @param selectedOption The currently selected option.
     * @param disabledOptions List of options that cannot be selected.
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, menuOptions: List<String>? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a cycler item to the rail.
     */
    fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, menuOptions: List<String>? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a visual divider line to the menu.
     */
    fun azDivider()

    /**
     * Adds a Host Item to the menu. Host items can contain sub-items which expand inline.
     *
     * @param id Unique identifier.
     * @param text Text display.
     * @param route Optional navigation route.
     * @param content Custom content.
     * @param color Custom color.
     * @param shape Custom shape.
     * @param disabled Whether the item is disabled.
     * @param screenTitle Title to display when active.
     * @param info Help info string.
     * @param onClick Click callback.
     */
    fun azMenuHostItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Host Item to the rail.
     */
    fun azRailHostItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Item to a Host Item in the menu.
     *
     * @param hostId The ID of the parent Host Item.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Item to a Host Item in the rail.
     */
    fun azRailSubItem(id: String, hostId: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Toggle to a Host Item in the menu.
     */
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuToggleOnText: String? = null, menuToggleOffText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Toggle to a Host Item in the rail.
     */
    fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, menuToggleOnText: String? = null, menuToggleOffText: String? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Cycler to a Host Item in the menu.
     */
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, menuOptions: List<String>? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Cycler to a Host Item in the rail.
     */
    fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, menuOptions: List<String>? = null, textColor: Color? = null, fillColor: Color? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Relocatable Item to a Host Item in the rail.
     * This item supports drag-and-drop reordering within its cluster.
     *
     * @param id Unique identifier.
     * @param hostId The ID of the parent Host Item.
     * @param text Text display.
     * @param route Optional navigation route.
     * @param content Custom content.
     * @param color Custom color.
     * @param shape Custom shape.
     * @param disabled Whether the item is disabled.
     * @param screenTitle Title to display when active.
     * @param info Help info string.
     * @param classifiers Active classifiers.
     * @param onFocus Focus callback.
     * @param onClick Click callback (selection).
     * @param onRelocate Callback invoked when the item is moved. Provides old index, new index, and the new ID order.
     * @param nestedRailAlignment The alignment of the nested rail (VERTICAL or HORIZONTAL).
     * @param nestedContent DSL block to define the items within the nested rail.
     * @param keepNestedRailOpen If true, the nested rail remains open until the parent item is tapped again.
     * @param hiddenMenu Scope to define context menu actions available via tap-when-focused.
     * @param forceHiddenMenuOpen Programmatic control to explicitly show or hide the hidden menu.
     * @param onHiddenMenuDismiss Callback invoked when the hidden menu dismisses itself.
     */
    fun azRailRelocItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), menuText: String? = null, textColor: Color? = null, fillColor: Color? = null, onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, nestedRailAlignment: AzNestedRailAlignment = AzNestedRailAlignment.VERTICAL, keepNestedRailOpen: Boolean = false, nestedContent: (AzNavRailScope.() -> Unit)? = null, forceHiddenMenuOpen: Boolean = false, onHiddenMenuDismiss: (() -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit = {})
}

/**
 * Scope for defining the hidden menu of a [AzNavItem] (specifically for relocatable items).
 */
interface HiddenMenuScope {
    /**
     * Adds a clickable list item to the hidden menu.
     *
     * @param text The text to display.
     * @param onClick The action to perform when clicked.
     */
    fun listItem(text: String, onClick: () -> Unit)
    /**
     * Adds a navigation list item to the hidden menu.
     *
     * @param text The text to display.
     * @param route The route to navigate to.
     */
    fun listItem(text: String, route: String)
    /**
     * Adds a text input item to the hidden menu.
     *
     * @param hint The hint text for the input field.
     * @param onValueChange Callback invoked when the input value changes.
     */
    fun inputItem(hint: String, onValueChange: (String) -> Unit)
    /**
     * Adds a text input item to the hidden menu with an initial value.
     *
     * @param hint The hint text for the input field.
     * @param initialValue The pre-filled initial value.
     * @param onValueChange Callback invoked when the input value changes.
     */
    fun inputItem(hint: String, initialValue: String, onValueChange: (String) -> Unit)
}

internal class HiddenMenuScopeImpl(
    private val parentId: String,
    private val targetOnClickMap: MutableMap<String, () -> Unit>,
    private val targetOnValueChangeMap: MutableMap<String, (String) -> Unit>
) : HiddenMenuScope {
    var items: MutableList<com.hereliesaz.aznavrail.model.HiddenMenuItem>? = null

    override fun listItem(text: String, onClick: () -> Unit) {
        val list = items ?: mutableListOf<com.hereliesaz.aznavrail.model.HiddenMenuItem>().also { items = it }
        val id = "${parentId}_hidden_item_${list.size}"
        list.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = text))
        targetOnClickMap[id] = onClick
    }

    override fun listItem(text: String, route: String) {
        val list = items ?: mutableListOf<com.hereliesaz.aznavrail.model.HiddenMenuItem>().also { items = it }
        val id = "${parentId}_hidden_item_${list.size}"
        list.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = text, route = route))
    }

    override fun inputItem(hint: String, onValueChange: (String) -> Unit) {
        inputItem(hint, "", onValueChange)
    }

    override fun inputItem(hint: String, initialValue: String, onValueChange: (String) -> Unit) {
        val list = items ?: mutableListOf<com.hereliesaz.aznavrail.model.HiddenMenuItem>().also { items = it }
        val id = "${parentId}_hidden_item_${list.size}"
        list.add(com.hereliesaz.aznavrail.model.HiddenMenuItem(id = id, text = "", isInput = true, hint = hint, initialValue = initialValue))
        targetOnValueChangeMap[id] = onValueChange
    }
}

class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableStateListOf<AzNavItem>()
    val onClickMap = mutableMapOf<String, () -> Unit>()
    val onFocusMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnClickMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnValueChangeMap = mutableMapOf<String, (String) -> Unit>()
    val onRelocateMap = mutableMapOf<String, (Int, Int, List<String>) -> Unit>()
    val itemBoundsCache = mutableStateMapOf<String, Rect>()
    var navController: NavController? = null
    var nestedRailOpenId: String? by mutableStateOf(null)

    fun reset() {
        navItems.clear()
        onClickMap.clear()
        onFocusMap.clear()
        hiddenMenuOnClickMap.clear()
        hiddenMenuOnValueChangeMap.clear()
        onRelocateMap.clear()
        itemBoundsCache.clear()

    }

    // Config
    var expandedWidth: Dp = 160.dp
    var collapsedWidth: Dp = 100.dp
    var showFooter: Boolean = true
    var appRepositoryUrl: String = "https://github.com/HereLiesAz/AzNavRail"
    var dockingSide: AzDockingSide = AzDockingSide.LEFT
    var packButtons: Boolean = false
    var noMenu: Boolean = false
    var vibrate: Boolean = false
    var displayAppName: Boolean = false
    var activeClassifiers: Set<String> = emptySet()
    var usePhysicalDocking: Boolean = false

    // Theme
    var activeColor: Color = Color.Unspecified
    var defaultShape: AzButtonShape = AzButtonShape.CIRCLE // Restored: default is circle
    var headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE // Default per legacy, overridden in UI
    var translucentBackground: Color = Color.Unspecified
    var helpLineColors: List<Color> = emptyList()

    // Advanced
    // Advanced
    var advancedConfig: AzAdvancedConfig = AzAdvancedConfig()


    override fun azConfig(dockingSide: AzDockingSide, packButtons: Boolean, noMenu: Boolean, vibrate: Boolean, displayAppName: Boolean, activeClassifiers: Set<String>, usePhysicalDocking: Boolean, expandedWidth: Dp, collapsedWidth: Dp, showFooter: Boolean, appRepositoryUrl: String) {
        this.dockingSide = dockingSide
        this.packButtons = packButtons
        this.noMenu = noMenu
        this.vibrate = vibrate
        this.displayAppName = displayAppName
        this.activeClassifiers = activeClassifiers
        this.usePhysicalDocking = usePhysicalDocking
        this.expandedWidth = expandedWidth
        this.collapsedWidth = collapsedWidth
        this.showFooter = showFooter
        this.appRepositoryUrl = appRepositoryUrl
    }

    override fun azTheme(activeColor: Color, defaultShape: AzButtonShape, headerIconShape: AzHeaderIconShape, translucentBackground: Color, helpLineColors: List<Color>) {
        this.activeColor = activeColor
        this.defaultShape = defaultShape
        this.headerIconShape = headerIconShape
        this.translucentBackground = translucentBackground
        this.helpLineColors = helpLineColors
    }

    override fun azAdvanced(isLoading: Boolean, helpEnabled: Boolean, onDismissHelp: (() -> Unit)?, overlayService: Class<out android.app.Service>?, onUndock: (() -> Unit)?, enableRailDragging: Boolean, onRailDrag: ((Float, Float) -> Unit)?, onOverlayDrag: ((Float, Float) -> Unit)?, onItemGloballyPositioned: ((String, Rect) -> Unit)?, secLoc: String?, secLocPort: Int, helpList: Map<String, Any>, tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial>) {
        this.advancedConfig = this.advancedConfig.copy(
            isLoading = isLoading,
            helpEnabled = helpEnabled,
            onDismissHelp = onDismissHelp,
            overlayService = overlayService,
            onUndock = onUndock,
            enableRailDragging = enableRailDragging || overlayService != null || onOverlayDrag != null,
            onRailDrag = onRailDrag,
            onOverlayDrag = onOverlayDrag,
            onItemGloballyPositioned = onItemGloballyPositioned,
            secLoc = secLoc ?: this.advancedConfig.secLoc,
            secLocPort = secLocPort,
            helpList = if (helpList.isNotEmpty()) helpList else this.advancedConfig.helpList,
            tutorials = if (tutorials.isNotEmpty()) tutorials else this.advancedConfig.tutorials
        )
    }

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
        overlayService: Class<out android.app.Service>?,
        onOverlayDrag: ((Float, Float) -> Unit)?,
        onItemGloballyPositioned: ((String, Rect) -> Unit)?,
        helpEnabled: Boolean,
        onDismissHelp: (() -> Unit)?,
        activeColor: Color?,
        vibrate: Boolean,
        dockingSide: AzDockingSide,
        noMenu: Boolean,
        usePhysicalDocking: Boolean,
        secLoc: String?,
        secLocPort: Int,
        helpList: Map<String, Any>,
        tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial>,
        helpLineColors: List<Color>
    ) {
        // Map to internal properties
        this.displayAppName = displayAppNameInHeader
        this.packButtons = packRailButtons
        this.expandedWidth = expandedRailWidth
        this.collapsedWidth = collapsedRailWidth
        this.showFooter = showFooter
        this.defaultShape = defaultShape
        this.headerIconShape = headerIconShape
        if (activeColor != null) this.activeColor = activeColor
        this.vibrate = vibrate
        this.dockingSide = dockingSide
        this.noMenu = noMenu
        this.usePhysicalDocking = usePhysicalDocking
        this.helpLineColors = helpLineColors

        this.advancedConfig = this.advancedConfig.copy(
            isLoading = isLoading,
            enableRailDragging = enableRailDragging || overlayService != null || onOverlayDrag != null,
            onUndock = onUndock,
            overlayService = overlayService,
            onOverlayDrag = onOverlayDrag,
            onItemGloballyPositioned = onItemGloballyPositioned,
            helpEnabled = helpEnabled,
            onDismissHelp = onDismissHelp,
            secLoc = secLoc ?: this.advancedConfig.secLoc,
            secLocPort = secLocPort,
            helpList = if (helpList.isNotEmpty()) helpList else this.advancedConfig.helpList,
            tutorials = if (tutorials.isNotEmpty()) tutorials else this.advancedConfig.tutorials
        )
    }

    private fun checkId(id: String) {
        if (navItems.any { it.id == id }) {
            throw IllegalArgumentException(
                "Duplicate ID detected: '$id'. All items in AzNavRail must have a unique ID to ensure state consistency."
            )
        }
    }

    override fun azMenuItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azHelpRailItem(id: String, text: String, content: Any?, color: Color?, shape: AzButtonShape?, menuText: String?, textColor: Color?, fillColor: Color?) {
        checkId(id)
        navItems.add(
            AzNavItem.Help(
                id = id,
                text = text,
                menuText = menuText,
                isRailItem = true,
                content = content,
                color = color,
                textColor = textColor,
                fillColor = fillColor,
                shape = shape ?: defaultShape
            )
        )
    }

    override fun azHelpSubItem(id: String, hostId: String, text: String, content: Any?, color: Color?, shape: AzButtonShape?, menuText: String?, textColor: Color?, fillColor: Color?) {
        checkId(id)
        if (navItems.none { it.id == hostId }) {
            throw IllegalArgumentException("Host item with ID '$hostId' must be added before adding its sub-items.")
        }
        navItems.add(
            AzNavItem(
                id = id,
                text = text,
                menuText = menuText,
                isRailItem = true,
                isHelpItem = true,
                isSubItem = true,
                hostId = hostId,
                content = content,
                color = color,
                textColor = textColor,
                fillColor = fillColor,
                shape = shape ?: defaultShape
            )
        )
    }

    override fun azRailItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, menuText: String?, textColor: Color?, fillColor: Color?, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, classifiers = classifiers, onFocus = onFocus, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azNestedRail(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, alignment: AzNestedRailAlignment, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, menuText: String?, textColor: Color?, fillColor: Color?, onFocus: (() -> Unit)?, keepNestedRailOpen: Boolean, nestedContent: AzNavRailScope.() -> Unit) {
        checkId(id)
        val nestedScope = AzNavRailScopeImpl()
        nestedScope.azConfig(dockingSide = this.dockingSide, packButtons = this.packButtons, noMenu = this.noMenu, vibrate = this.vibrate, displayAppName = this.displayAppName, activeClassifiers = this.activeClassifiers, expandedWidth = this.expandedWidth, collapsedWidth = this.collapsedWidth, showFooter = this.showFooter, appRepositoryUrl = this.appRepositoryUrl)
        nestedScope.azTheme(activeColor = this.activeColor, defaultShape = this.defaultShape, headerIconShape = this.headerIconShape, translucentBackground = this.translucentBackground)
        nestedScope.nestedContent()

        nestedScope.onClickMap.forEach { (k, v) ->
            if (onClickMap.containsKey(k)) throw IllegalStateException("Scope collision: Nested item ID '$k' duplicates an existing parent ID.")
            onClickMap[k] = v
        }
        nestedScope.onFocusMap.forEach { (k, v) -> onFocusMap[k] = v }
        nestedScope.hiddenMenuOnClickMap.forEach { (k, v) -> hiddenMenuOnClickMap[k] = v }
        nestedScope.hiddenMenuOnValueChangeMap.forEach { (k, v) -> hiddenMenuOnValueChangeMap[k] = v }
        nestedScope.onRelocateMap.forEach { (k, v) -> onRelocateMap[k] = v }

        if (onFocus != null) onFocusMap[id] = onFocus

        navItems.add(
            AzNavItem(
                id = id, text = text, menuText = menuText, route = route, isRailItem = true, isNestedRail = true,
                nestedRailAlignment = alignment, nestedRailItems = nestedScope.navItems.toList(),
                disabled = disabled, screenTitle = screenTitle ?: text,
                info = info, classifiers = classifiers, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape ?: defaultShape,
                keepNestedRailOpen = keepNestedRailOpen
            )
        )
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuToggleOnText: String?, menuToggleOffText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, menuToggleOnText = menuToggleOnText, menuToggleOffText = menuToggleOffText, config = AzItemConfig(route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuToggleOnText: String?, menuToggleOffText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, menuToggleOnText = menuToggleOnText, menuToggleOffText = menuToggleOffText, config = AzItemConfig(route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, menuOptions: List<String>?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, menuOptions = menuOptions, selectedOption = selectedOption, disabledOptions = disabledOptions, config = AzItemConfig(route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, menuOptions: List<String>?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, menuOptions = menuOptions, selectedOption = selectedOption, disabledOptions = disabledOptions, config = AzItemConfig(route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azDivider() {
        navItems.add(AzNavItem(id = "divider_${navItems.size}", text = "", isRailItem = false, isDivider = true))
    }

    override fun azMenuHostItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isHost = true, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailHostItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isHost = true, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isSubItem = true, hostId = hostId, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, menuText: String?, textColor: Color?, fillColor: Color?, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, menuText = menuText, config = AzItemConfig(route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isSubItem = true, hostId = hostId, classifiers = classifiers, onFocus = onFocus, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuToggleOnText: String?, menuToggleOffText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, menuToggleOnText = menuToggleOnText, menuToggleOffText = menuToggleOffText, config = AzItemConfig(hostId = hostId, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, menuToggleOnText: String?, menuToggleOffText: String?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, menuToggleOnText = menuToggleOnText, menuToggleOffText = menuToggleOffText, config = AzItemConfig(hostId = hostId, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, menuOptions: List<String>?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, menuOptions = menuOptions, selectedOption = selectedOption, disabledOptions = disabledOptions, config = AzItemConfig(hostId = hostId, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, menuOptions: List<String>?, textColor: Color?, fillColor: Color?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, menuOptions = menuOptions, selectedOption = selectedOption, disabledOptions = disabledOptions, config = AzItemConfig(hostId = hostId, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, color = color, textColor = textColor, fillColor = fillColor, shape = shape), onClick = onClick ?: {})
    }

    override fun azRailRelocItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, menuText: String?, textColor: Color?, fillColor: Color?, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, nestedRailAlignment: AzNestedRailAlignment, keepNestedRailOpen: Boolean, nestedContent: (AzNavRailScope.() -> Unit)?, forceHiddenMenuOpen: Boolean, onHiddenMenuDismiss: (() -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        checkId(id)
        val hiddenMenuScope = HiddenMenuScopeImpl(id, hiddenMenuOnClickMap, hiddenMenuOnValueChangeMap)
        hiddenMenuScope.hiddenMenu()

        if (onClick != null) onClickMap[id] = onClick
        if (onRelocate != null) onRelocateMap[id] = onRelocate
        if (onFocus != null) onFocusMap[id] = onFocus

        val finalScreenTitle = if (screenTitle == AzNavRailDefaults.NO_TITLE) null else screenTitle ?: text

        val nestedItems = if (nestedContent != null) {
            val nestedScope = AzNavRailScopeImpl()
            nestedScope.azConfig(dockingSide = this.dockingSide, packButtons = this.packButtons, noMenu = this.noMenu, vibrate = this.vibrate, displayAppName = this.displayAppName, activeClassifiers = this.activeClassifiers, expandedWidth = this.expandedWidth, collapsedWidth = this.collapsedWidth, showFooter = this.showFooter)
            nestedScope.azTheme(activeColor = this.activeColor, defaultShape = this.defaultShape, headerIconShape = this.headerIconShape, translucentBackground = this.translucentBackground)
            nestedScope.nestedContent()

            nestedScope.onClickMap.forEach { (k, v) ->
                if (onClickMap.containsKey(k)) throw IllegalStateException("Scope collision: Nested item ID '$k' duplicates an existing parent ID.")
                onClickMap[k] = v
            }
            nestedScope.onFocusMap.forEach { (k, v) -> onFocusMap[k] = v }
            nestedScope.hiddenMenuOnClickMap.forEach { (k, v) -> hiddenMenuOnClickMap[k] = v }
            nestedScope.hiddenMenuOnValueChangeMap.forEach { (k, v) -> hiddenMenuOnValueChangeMap[k] = v }
            nestedScope.onRelocateMap.forEach { (k, v) -> onRelocateMap[k] = v }

            nestedScope.navItems.toList()
        } else null

        navItems.add(
            AzNavItem(
                id = id, text = text, menuText = menuText, route = route, isRailItem = true, isSubItem = true, hostId = hostId,
                isRelocItem = true, disabled = disabled, screenTitle = finalScreenTitle, info = info,
                hiddenMenuItems = hiddenMenuScope.items, forceHiddenMenuOpen = forceHiddenMenuOpen, onHiddenMenuDismiss = onHiddenMenuDismiss, classifiers = classifiers, content = content, color = color, textColor = textColor, fillColor = fillColor, shape = shape ?: defaultShape,
                isNestedRail = nestedContent != null, nestedRailAlignment = nestedRailAlignment, nestedRailItems = nestedItems,
                keepNestedRailOpen = keepNestedRailOpen
            )
        )
    }

    private fun addCycler(id: String, options: List<String>, menuOptions: List<String>? = null, selectedOption: String, disabledOptions: List<String>? = null, config: AzItemConfig, onClick: () -> Unit) {
        checkId(id)
        require(menuOptions == null || options.size == menuOptions.size) {
            "Error adding cycler '$id': 'menuOptions' must have the same size as 'options' if provided."
        }
        require(options.contains(selectedOption)) {
            "Error adding item '$id': selectedOption '$selectedOption' must be present in the options list: $options"
        }
        val finalScreenTitle = if (config.screenTitle == AzNavRailDefaults.NO_TITLE) null else config.screenTitle ?: selectedOption
        onClickMap[id] = onClick
        navItems.add(
            AzNavItem(
                id = id, text = "", route = config.route, screenTitle = finalScreenTitle, isRailItem = config.isRailItem,
                isCycler = true, options = options, menuOptions = menuOptions, selectedOption = selectedOption,
                disabled = config.disabled, disabledOptions = disabledOptions, isSubItem = config.isSubItem,
                hostId = config.hostId, info = config.info, color = config.color, textColor = config.textColor,
                fillColor = config.fillColor, shape = config.shape ?: defaultShape
            )
        )
    }

    private fun addToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, menuToggleOnText: String? = null, menuToggleOffText: String? = null, config: AzItemConfig, onClick: () -> Unit) {
        checkId(id)
        require(toggleOnText.isNotBlank() && toggleOffText.isNotBlank()) {
            "Error adding item '$id': toggle text options cannot be blank."
        }
        val text = if (isChecked) toggleOnText else toggleOffText
        val finalScreenTitle = if (config.screenTitle == AzNavRailDefaults.NO_TITLE) null else config.screenTitle ?: text
        onClickMap[id] = onClick
        navItems.add(
            AzNavItem(
                id = id, text = "", route = config.route, screenTitle = finalScreenTitle, isRailItem = config.isRailItem,
                isToggle = true, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText,
                menuToggleOnText = menuToggleOnText, menuToggleOffText = menuToggleOffText, disabled = config.disabled,
                isSubItem = config.isSubItem, hostId = config.hostId, info = config.info, color = config.color,
                textColor = config.textColor, fillColor = config.fillColor, shape = config.shape ?: defaultShape
            )
        )
    }

    private fun addItem(id: String, text: String, menuText: String? = null, config: AzItemConfig, onClick: () -> Unit) {
        checkId(id)
        require(text.isNotBlank()) {
           "Error adding item '$id': 'text' parameter cannot be blank."
        }
        val finalScreenTitle = if (config.screenTitle == AzNavRailDefaults.NO_TITLE) null else config.screenTitle ?: text
        onClickMap[id] = onClick
        if (config.onFocus != null) onFocusMap[id] = config.onFocus
        navItems.add(
            AzNavItem(
                id = id, text = text, menuText = menuText, route = config.route, screenTitle = finalScreenTitle,
                isRailItem = config.isRailItem, disabled = config.disabled, isHost = config.isHost,
                isSubItem = config.isSubItem, hostId = config.hostId, info = config.info,
                classifiers = config.classifiers, content = config.content, color = config.color,
                textColor = config.textColor, fillColor = config.fillColor, shape = config.shape ?: defaultShape
            )
        )
    }
}
