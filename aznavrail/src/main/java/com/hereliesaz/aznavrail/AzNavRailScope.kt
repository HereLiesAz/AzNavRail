package com.hereliesaz.aznavrail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
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
     */
    fun azConfig(dockingSide: AzDockingSide = AzDockingSide.LEFT, packButtons: Boolean = false, noMenu: Boolean = false, vibrate: Boolean = false, displayAppName: Boolean = false, activeClassifiers: Set<String> = emptySet(), usePhysicalDocking: Boolean = false, expandedWidth: Dp = 130.dp, collapsedWidth: Dp = 80.dp, showFooter: Boolean = true)

    /**
     * Configures the visual theme of the rail.
     *
     * @param activeColor The color used for active/selected items.
     * @param defaultShape The default shape for buttons (Circle, Square, Rectangle, None).
     * @param headerIconShape The shape of the header icon (Circle, Rounded, None).
     */
    fun azTheme(activeColor: Color = Color.Unspecified, defaultShape: AzButtonShape = AzButtonShape.CIRCLE, headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE)

    /**
     * Configures advanced features like loading states, help screens, and overlays.
     *
     * @param isLoading Whether to show a full-screen loading spinner.
     * @param infoScreen Whether to show the interactive help overlay.
     * @param onDismissInfoScreen Callback invoked when the info screen is dismissed.
     * @param overlayService The class of the service to use for the system overlay (FAB mode).
     * @param onUndock Callback invoked when the rail is undocked (FAB mode activated).
     * @param enableRailDragging Whether to allow the user to detach and drag the rail (FAB mode).
     * @param onRailDrag Callback invoked during rail drag events.
     * @param onOverlayDrag Callback invoked during overlay drag events.
     * @param onItemGloballyPositioned Callback invoked with the bounds of items (used for tutorials/help).
     */
    fun azAdvanced(isLoading: Boolean = false, infoScreen: Boolean = false, onDismissInfoScreen: (() -> Unit)? = null, overlayService: Class<out android.app.Service>? = null, onUndock: (() -> Unit)? = null, enableRailDragging: Boolean = false, onRailDrag: ((Float, Float) -> Unit)? = null, onOverlayDrag: ((Float, Float) -> Unit)? = null, onItemGloballyPositioned: ((String, Rect) -> Unit)? = null)

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
        infoScreen: Boolean = false,
        onDismissInfoScreen: (() -> Unit)? = null,
        activeColor: Color? = null,
        vibrate: Boolean = false,
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        noMenu: Boolean = false,
        usePhysicalDocking: Boolean = false
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
    fun azMenuItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

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
    fun azRailItem(id: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)

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
     * @param nestedContent DSL block to define the items within the nested rail.
     */
    fun azNestedRail(id: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, alignment: AzNestedRailAlignment = AzNestedRailAlignment.VERTICAL, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, nestedContent: AzNavRailScope.() -> Unit)

    /**
     * Adds a toggle switch item to the menu.
     *
     * @param isChecked The current state of the toggle.
     * @param toggleOnText Text to display when checked.
     * @param toggleOffText Text to display when unchecked.
     */
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

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
    fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a cycler item (multi-state button) to the menu.
     *
     * @param options List of possible string options.
     * @param selectedOption The currently selected option.
     * @param disabledOptions List of options that cannot be selected.
     */
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a cycler item to the rail.
     */
    fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

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
    fun azMenuHostItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Host Item to the rail.
     */
    fun azRailHostItem(id: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Item to a Host Item in the menu.
     *
     * @param hostId The ID of the parent Host Item.
     */
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Item to a Host Item in the rail.
     */
    fun azRailSubItem(id: String, hostId: String, text: String = "", route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Toggle to a Host Item in the menu.
     */
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Toggle to a Host Item in the rail.
     */
    fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Cycler to a Host Item in the menu.
     */
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

    /**
     * Adds a Sub Cycler to a Host Item in the rail.
     */
    fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)

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
     * @param hiddenMenu Scope to define context menu actions available via tap-when-focused.
     */
    fun azRailRelocItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit = {})
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
}

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

class AzNavRailScopeImpl : AzNavRailScope {
    val navItems = mutableStateListOf<AzNavItem>()
    val onClickMap = mutableMapOf<String, () -> Unit>()
    val onFocusMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnClickMap = mutableMapOf<String, () -> Unit>()
    val hiddenMenuOnValueChangeMap = mutableMapOf<String, (String) -> Unit>()
    val onRelocateMap = mutableMapOf<String, (Int, Int, List<String>) -> Unit>()
    val itemBoundsCache = mutableMapOf<String, Rect>()
    var navController: NavController? = null

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
    var expandedWidth: Dp = 280.dp
    var collapsedWidth: Dp = 136.dp
    var showFooter: Boolean = true
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

    // Advanced
    var isLoading: Boolean = false
    var infoScreen: Boolean = false
    var onDismissInfoScreen: (() -> Unit)? = null
    var overlayService: Class<out android.app.Service>? = null
    var onUndock: (() -> Unit)? = null
    var enableRailDragging: Boolean = false
    var onRailDrag: ((Float, Float) -> Unit)? = null
    var onOverlayDrag: ((Float, Float) -> Unit)? = null
    var onItemGloballyPositioned: ((String, Rect) -> Unit)? = null

    override fun azConfig(dockingSide: AzDockingSide, packButtons: Boolean, noMenu: Boolean, vibrate: Boolean, displayAppName: Boolean, activeClassifiers: Set<String>, usePhysicalDocking: Boolean, expandedWidth: Dp, collapsedWidth: Dp, showFooter: Boolean) {
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
    }

    override fun azTheme(activeColor: Color, defaultShape: AzButtonShape, headerIconShape: AzHeaderIconShape) {
        this.activeColor = activeColor
        this.defaultShape = defaultShape
        this.headerIconShape = headerIconShape
    }

    override fun azAdvanced(isLoading: Boolean, infoScreen: Boolean, onDismissInfoScreen: (() -> Unit)?, overlayService: Class<out android.app.Service>?, onUndock: (() -> Unit)?, enableRailDragging: Boolean, onRailDrag: ((Float, Float) -> Unit)?, onOverlayDrag: ((Float, Float) -> Unit)?, onItemGloballyPositioned: ((String, Rect) -> Unit)?) {
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
        infoScreen: Boolean,
        onDismissInfoScreen: (() -> Unit)?,
        activeColor: Color?,
        vibrate: Boolean,
        dockingSide: AzDockingSide,
        noMenu: Boolean,
        usePhysicalDocking: Boolean
    ) {
        // Map to internal properties
        this.displayAppName = displayAppNameInHeader
        this.packButtons = packRailButtons
        this.expandedWidth = expandedRailWidth
        this.collapsedWidth = collapsedRailWidth
        this.showFooter = showFooter
        this.isLoading = isLoading
        this.defaultShape = defaultShape
        this.enableRailDragging = enableRailDragging
        this.headerIconShape = headerIconShape
        this.onUndock = onUndock
        this.overlayService = overlayService
        this.onOverlayDrag = onOverlayDrag
        this.onItemGloballyPositioned = onItemGloballyPositioned
        this.infoScreen = infoScreen
        this.onDismissInfoScreen = onDismissInfoScreen
        if (activeColor != null) this.activeColor = activeColor
        this.vibrate = vibrate
        this.dockingSide = dockingSide
        this.noMenu = noMenu
        this.usePhysicalDocking = usePhysicalDocking
    }

    private fun checkId(id: String) {
        if (navItems.any { it.id == id }) {
            throw IllegalArgumentException(
                "Duplicate ID detected: '$id'. All items in AzNavRail must have a unique ID to ensure state consistency."
            )
        }
    }

    override fun azMenuItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, classifiers = classifiers, onFocus = onFocus, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azNestedRail(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, alignment: AzNestedRailAlignment, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, nestedContent: AzNavRailScope.() -> Unit) {
        checkId(id)
        val nestedScope = AzNavRailScopeImpl()
        nestedScope.azConfig(dockingSide = this.dockingSide, packButtons = this.packButtons, noMenu = this.noMenu, vibrate = this.vibrate, displayAppName = this.displayAppName, activeClassifiers = this.activeClassifiers, expandedWidth = this.expandedWidth, collapsedWidth = this.collapsedWidth, showFooter = this.showFooter)
        nestedScope.azTheme(activeColor = this.activeColor, defaultShape = this.defaultShape, headerIconShape = this.headerIconShape)
        nestedScope.nestedContent()

        nestedScope.onClickMap.forEach { (k, v) -> onClickMap[k] = v }
        nestedScope.onFocusMap.forEach { (k, v) -> onFocusMap[k] = v }
        nestedScope.hiddenMenuOnClickMap.forEach { (k, v) -> hiddenMenuOnClickMap[k] = v }
        nestedScope.hiddenMenuOnValueChangeMap.forEach { (k, v) -> hiddenMenuOnValueChangeMap[k] = v }
        nestedScope.onRelocateMap.forEach { (k, v) -> onRelocateMap[k] = v }

        if (onFocus != null) onFocusMap[id] = onFocus

        navItems.add(
            AzNavItem(
                id = id, text = text, route = route, isRailItem = true, isNestedRail = true,
                nestedRailAlignment = alignment, nestedRailItems = nestedScope.navItems.toList(),
                disabled = disabled, screenTitle = screenTitle ?: text,
                info = info, classifiers = classifiers, content = content, color = color, shape = shape ?: defaultShape
            )
        )
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = null, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azDivider() {
        navItems.add(AzNavItem(id = "divider_${navItems.size}", text = "", isRailItem = false, isDivider = true))
    }

    override fun azMenuHostItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isHost = true, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailHostItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isHost = true, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isSubItem = true, hostId = hostId, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isSubItem = true, hostId = hostId, classifiers = classifiers, onFocus = onFocus, content = content, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, color = color, shape = shape, onClick = onClick ?: {})
    }

    override fun azRailRelocItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        checkId(id)
        val hiddenMenuScope = HiddenMenuScopeImpl()
        hiddenMenuScope.hiddenMenu()

        hiddenMenuScope.onClickMap.forEach { (key, value) -> hiddenMenuOnClickMap["${id}_$key"] = value }
        hiddenMenuScope.onValueChangeMap.forEach { (key, value) -> hiddenMenuOnValueChangeMap["${id}_$key"] = value }
        val prefixedItems = hiddenMenuScope.items.map { it.copy(id = "${id}_${it.id}") }

        if (onClick != null) onClickMap[id] = onClick
        if (onRelocate != null) onRelocateMap[id] = onRelocate
        if (onFocus != null) onFocusMap[id] = onFocus

        val finalScreenTitle = if (screenTitle == AzNavRailDefaults.NO_TITLE) null else screenTitle ?: text

        navItems.add(
            AzNavItem(
                id = id, text = text, route = route, isRailItem = true, isSubItem = true, hostId = hostId,
                isRelocItem = true, disabled = disabled, screenTitle = finalScreenTitle, info = info,
                hiddenMenuItems = prefixedItems, classifiers = classifiers, content = content, color = color, shape = shape ?: defaultShape
            )
        )
    }

    private fun addCycler(id: String, hostId: String? = null, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, isRailItem: Boolean, isSubItem: Boolean, color: Color?, shape: AzButtonShape?, onClick: () -> Unit) {
        checkId(id)
        require(options.contains(selectedOption)) {
            "Error adding item '$id': selectedOption '$selectedOption' must be present in the options list: $options"
        }
        val finalScreenTitle = if (screenTitle == AzNavRailDefaults.NO_TITLE) null else screenTitle ?: selectedOption
        onClickMap[id] = onClick
        navItems.add(AzNavItem(id = id, text = "", route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, isCycler = true, options = options, selectedOption = selectedOption, disabled = disabled, disabledOptions = disabledOptions, isSubItem = isSubItem, hostId = hostId, info = info, color = color, shape = shape ?: defaultShape))
    }

    private fun addToggle(id: String, hostId: String? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, isRailItem: Boolean, isSubItem: Boolean, color: Color?, shape: AzButtonShape?, onClick: () -> Unit) {
        checkId(id)
        require(toggleOnText.isNotBlank() && toggleOffText.isNotBlank()) {
            "Error adding item '$id': toggle text options cannot be blank."
        }
        val text = if (isChecked) toggleOnText else toggleOffText
        val finalScreenTitle = if (screenTitle == AzNavRailDefaults.NO_TITLE) null else screenTitle ?: text
        onClickMap[id] = onClick
        navItems.add(AzNavItem(id = id, text = "", route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, isToggle = true, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, disabled = disabled, isSubItem = isSubItem, hostId = hostId, info = info, color = color, shape = shape ?: defaultShape))
    }

    private fun addItem(id: String, text: String, route: String?, screenTitle: String?, info: String?, isRailItem: Boolean, disabled: Boolean = false, isHost: Boolean = false, isSubItem: Boolean = false, hostId: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, content: Any? = null, color: Color? = null, shape: AzButtonShape? = null, onClick: () -> Unit) {
        checkId(id)
        require(text.isNotBlank()) {
           "Error adding item '$id': 'text' parameter cannot be blank."
        }
        val finalScreenTitle = if (screenTitle == AzNavRailDefaults.NO_TITLE) null else screenTitle ?: text
        onClickMap[id] = onClick
        if (onFocus != null) onFocusMap[id] = onFocus
        navItems.add(AzNavItem(id = id, text = text, route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, disabled = disabled, isHost = isHost, isSubItem = isSubItem, hostId = hostId, info = info, classifiers = classifiers, content = content, color = color, shape = shape ?: defaultShape))
    }
}
