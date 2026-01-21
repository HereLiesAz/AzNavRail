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

interface AzNavRailScope {
    /**
     * Configures global settings for the navigation rail.
     *
     * @param displayAppNameInHeader If true, the app name is displayed in the header instead of the icon.
     * @param packRailButtons If true, rail buttons are packed together without spacing.
     * @param expandedRailWidth The width of the expanded rail/menu.
     * @param collapsedRailWidth The width of the collapsed rail.
     * @param showFooter If true, the footer is displayed in the expanded menu.
     * @param isLoading If true, a full-screen loading overlay is shown.
     * @param defaultShape The default shape for rail items.
     * @param enableRailDragging If true, enables FAB mode (draggable rail).
     * @param headerIconShape The shape of the header icon.
     * @param onUndock Callback triggered when the rail is undocked (e.g., long-press header).
     * @param onRailDrag Callback for custom drag handling in FAB mode.
     * @param overlayService The class of the service to start for system overlay mode.
     * @param onOverlayDrag Callback for custom drag handling in overlay mode.
     * @param onItemGloballyPositioned Callback for getting item positions (used for Info Screen).
     * @param infoScreen If true, activates the interactive help overlay.
     * @param onDismissInfoScreen Callback to dismiss the info screen.
     * @param activeColor Custom color for the active/selected item.
     * @param vibrate If true, enables haptic feedback for gestures.
     * @param activeClassifiers Set of classifiers to mark items as active aside from route/selection.
     * @param dockingSide The side of the screen to dock the rail (LEFT or RIGHT).
     * @param noMenu If true, all items are treated as rail items and the side drawer is disabled.
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

    fun azMenuItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)
    fun azMenuItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    fun azRailItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null)
    fun azRailItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    fun azRailItem(id: String, text: String, config: AzItemConfig, onClick: () -> Unit)
    fun azRailItem(id: String, text: String, route: String, config: AzItemConfig = AzItemConfig())
    fun azRailItem(id: String, text: String, route: String, config: AzItemConfig, onClick: () -> Unit)

    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    fun azDivider()

    fun azMenuHostItem(id: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)
    fun azMenuHostItem(id: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    fun azRailHostItem(id: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)
    fun azRailHostItem(id: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    fun azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)

    fun azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null)
    fun azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: () -> Unit)

    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailSubToggle(id: String, hostId: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null)

    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailSubCycler(id: String, hostId: String, color: Color? = null, options: List<String>, selectedOption: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null)

    fun azRailRelocItem(id: String, hostId: String, text: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit = {})
    fun azRailRelocItem(id: String, hostId: String, text: String, route: String, color: Color? = null, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit)
}

interface HiddenMenuScope {
    fun listItem(text: String, onClick: () -> Unit)
    fun listItem(text: String, route: String)
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
    var navController: NavController? = null

    fun reset() {
        navItems.clear()
        onClickMap.clear()
        onFocusMap.clear()
        hiddenMenuOnClickMap.clear()
        hiddenMenuOnValueChangeMap.clear()
        onRelocateMap.clear()
    }
    var displayAppNameInHeader: Boolean = false
    var packRailButtons: Boolean = false
    var expandedRailWidth: Dp = 260.dp
    var collapsedRailWidth: Dp = 80.dp
    var showFooter: Boolean = true
    var isLoading: Boolean = false
    var defaultShape: AzButtonShape = AzButtonShape.CIRCLE
    var enableRailDragging: Boolean = false
    var headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
    var onUndock: (() -> Unit)? = null
    var onRailDrag: ((Float, Float) -> Unit)? = null
    var overlayService: Class<out android.app.Service>? = null
    var onOverlayDrag: ((Float, Float) -> Unit)? = null
    var onItemGloballyPositioned: ((String, Rect) -> Unit)? = null
    var infoScreen: Boolean = false
    var onDismissInfoScreen: (() -> Unit)? = null
    var activeColor: Color? = null
    var vibrate: Boolean = false
    var activeClassifiers: Set<String> = emptySet()
    var dockingSide: AzDockingSide = AzDockingSide.LEFT
    var noMenu: Boolean = false
    var secLoc: String? = null

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
        require(expandedRailWidth > collapsedRailWidth) {
            """
            `expandedRailWidth` must be greater than `collapsedRailWidth`.
            """.trimIndent()
        }
        this.displayAppNameInHeader = displayAppNameInHeader
        this.packRailButtons = packRailButtons
        this.expandedRailWidth = expandedRailWidth
        this.collapsedRailWidth = collapsedRailWidth
        this.showFooter = showFooter
        this.isLoading = isLoading
        this.defaultShape = defaultShape
        this.enableRailDragging = enableRailDragging || overlayService != null || onOverlayDrag != null
        this.headerIconShape = headerIconShape
        this.onUndock = onUndock
        this.overlayService = overlayService
        this.onOverlayDrag = onOverlayDrag
        this.onItemGloballyPositioned = onItemGloballyPositioned
        this.infoScreen = infoScreen
        this.onDismissInfoScreen = onDismissInfoScreen
        this.activeColor = activeColor
        this.vibrate = vibrate
        this.activeClassifiers = activeClassifiers
        this.dockingSide = dockingSide
        this.noMenu = noMenu
        this.secLoc = secLoc
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
        addRailItem(id, text, null, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?) {
        addRailItem(id, text, route, color, shape, disabled, screenTitle, info, classifiers, onFocus) {}
    }

    override fun azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addRailItem(id, text, route, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)
    }

    override fun azRailItem(id: String, text: String, config: AzItemConfig, onClick: () -> Unit) {
        addRailItem(
            id, text, null, config.color, config.shape, config.disabled, config.screenTitle, config.info, config.classifiers, config.onFocus, onClick
        )
    }

    override fun azRailItem(id: String, text: String, route: String, config: AzItemConfig) {
        addRailItem(
            id, text, route, config.color, config.shape, config.disabled, config.screenTitle, config.info, config.classifiers, config.onFocus
        ) {}
    }

    override fun azRailItem(id: String, text: String, route: String, config: AzItemConfig, onClick: () -> Unit) {
        addRailItem(
            id, text, route, config.color, config.shape, config.disabled, config.screenTitle, config.info, config.classifiers, config.onFocus, onClick
        )
    }

    private fun addRailItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, color = color, shape = shape, disabled = disabled, classifiers = classifiers, onFocus = onFocus, onClick = onClick)
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
            info = info,
            isRailItem = true,
            isSubItem = false,
            onClick = onClick
        )
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
                classifiers = classifiers
            )
        )
    }
}
