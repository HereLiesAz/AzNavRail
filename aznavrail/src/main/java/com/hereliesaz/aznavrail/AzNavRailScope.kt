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
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

interface AzNavRailScope {
    fun azConfig(dockingSide: AzDockingSide = AzDockingSide.LEFT, packButtons: Boolean = false, noMenu: Boolean = false, vibrate: Boolean = false, displayAppName: Boolean = false, activeClassifiers: Set<String> = emptySet(), usePhysicalDocking: Boolean = false, expandedWidth: Dp = 130.dp, collapsedWidth: Dp = 80.dp, showFooter: Boolean = true)
    fun azTheme(activeColor: Color = Color.Unspecified, defaultShape: AzButtonShape = AzButtonShape.CIRCLE, headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE)
    fun azAdvanced(isLoading: Boolean = false, infoScreen: Boolean = false, onDismissInfoScreen: (() -> Unit)? = null, overlayService: Class<out android.app.Service>? = null, onUndock: (() -> Unit)? = null, enableRailDragging: Boolean = false, onRailDrag: ((Float, Float) -> Unit)? = null, onOverlayDrag: ((Float, Float) -> Unit)? = null, onItemGloballyPositioned: ((String, Rect) -> Unit)? = null)

    fun azMenuItem(id: String, text: String, route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailItem(id: String, text: String = "", route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)
    fun azNestedRail(id: String, text: String = "", route: String? = null, content: Any? = null, alignment: AzNestedRailAlignment = AzNestedRailAlignment.VERTICAL, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, nestedContent: AzNavRailScope.() -> Unit)
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azDivider()
    fun azMenuHostItem(id: String, text: String, route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailHostItem(id: String, text: String, route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azMenuSubItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailSubItem(id: String, hostId: String, text: String = "", route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null)
    fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String? = null, disabled: Boolean = false, disabledOptions: List<String>? = null, screenTitle: String? = null, info: String? = null, onClick: (() -> Unit)? = null)
    fun azRailRelocItem(id: String, hostId: String, text: String, route: String? = null, content: Any? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, onClick: (() -> Unit)? = null, onRelocate: ((Int, Int, List<String>) -> Unit)? = null, hiddenMenu: HiddenMenuScope.() -> Unit = {})
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
    
    // Config
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

    // Theme
    var activeColor: Color = Color.Unspecified
    var defaultShape: AzButtonShape = AzButtonShape.CIRCLE
    var headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE

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

    override fun azMenuItem(id: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, content = content, onClick = onClick ?: {})
    }

    override fun azRailItem(id: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, classifiers = classifiers, onFocus = onFocus, content = content, onClick = onClick ?: {})
    }

    override fun azNestedRail(id: String, text: String, route: String?, content: Any?, alignment: AzNestedRailAlignment, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, nestedContent: AzNavRailScope.() -> Unit) {
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
                info = info, classifiers = classifiers, content = content
            )
        )
    }

    override fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, onClick = onClick ?: {})
    }

    override fun azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, onClick = onClick ?: {})
    }

    override fun azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = false, onClick = onClick ?: {})
    }

    override fun azRailCycler(id: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = null, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = false, onClick = onClick ?: {})
    }

    override fun azDivider() {
        navItems.add(AzNavItem(id = "divider_${navItems.size}", text = "", isRailItem = false, isDivider = true))
    }

    override fun azMenuHostItem(id: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isHost = true, content = content, onClick = onClick ?: {})
    }

    override fun azRailHostItem(id: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isHost = true, content = content, onClick = onClick ?: {})
    }

    override fun azMenuSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = false, disabled = disabled, isSubItem = true, hostId = hostId, content = content, onClick = onClick ?: {})
    }

    override fun azRailSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?) {
        addItem(id = id, text = text, route = route, screenTitle = screenTitle, info = info, isRailItem = true, disabled = disabled, isSubItem = true, hostId = hostId, classifiers = classifiers, onFocus = onFocus, content = content, onClick = onClick ?: {})
    }

    override fun azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, onClick = onClick ?: {})
    }

    override fun azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addToggle(id = id, hostId = hostId, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, route = route, disabled = disabled, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, onClick = onClick ?: {})
    }

    override fun azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = false, isSubItem = true, onClick = onClick ?: {})
    }

    override fun azRailSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: (() -> Unit)?) {
        addCycler(id = id, hostId = hostId, options = options, selectedOption = selectedOption, route = route, disabled = disabled, disabledOptions = disabledOptions, screenTitle = screenTitle, info = info, isRailItem = true, isSubItem = true, onClick = onClick ?: {})
    }

    override fun azRailRelocItem(id: String, hostId: String, text: String, route: String?, content: Any?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit) {
        val hiddenMenuScope = HiddenMenuScopeImpl()
        hiddenMenuScope.hiddenMenu()

        hiddenMenuScope.onClickMap.forEach { (key, value) -> hiddenMenuOnClickMap["${id}_$key"] = value }
        hiddenMenuScope.onValueChangeMap.forEach { (key, value) -> hiddenMenuOnValueChangeMap["${id}_$key"] = value }
        val prefixedItems = hiddenMenuScope.items.map { it.copy(id = "${id}_${it.id}") }

        if (onClick != null) onClickMap[id] = onClick
        if (onRelocate != null) onRelocateMap[id] = onRelocate
        if (onFocus != null) onFocusMap[id] = onFocus

        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text

        navItems.add(
            AzNavItem(
                id = id, text = text, route = route, isRailItem = true, isSubItem = true, hostId = hostId,
                isRelocItem = true, disabled = disabled, screenTitle = finalScreenTitle, info = info, 
                hiddenMenuItems = prefixedItems, classifiers = classifiers, content = content
            )
        )
    }

    private fun addCycler(id: String, hostId: String? = null, options: List<String>, selectedOption: String, route: String?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, isRailItem: Boolean, isSubItem: Boolean, onClick: () -> Unit) {
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: selectedOption
        onClickMap[id] = onClick
        navItems.add(AzNavItem(id = id, text = "", route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, isCycler = true, options = options, selectedOption = selectedOption, disabled = disabled, disabledOptions = disabledOptions, isSubItem = isSubItem, hostId = hostId, info = info))
    }

    private fun addToggle(id: String, hostId: String? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, disabled: Boolean, screenTitle: String?, info: String?, isRailItem: Boolean, isSubItem: Boolean, onClick: () -> Unit) {
        val text = if (isChecked) toggleOnText else toggleOffText
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text
        onClickMap[id] = onClick
        navItems.add(AzNavItem(id = id, text = "", route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, isToggle = true, isChecked = isChecked, toggleOnText = toggleOnText, toggleOffText = toggleOffText, disabled = disabled, isSubItem = isSubItem, hostId = hostId, info = info))
    }

    private fun addItem(id: String, text: String, route: String?, screenTitle: String?, info: String?, isRailItem: Boolean, disabled: Boolean = false, isHost: Boolean = false, isSubItem: Boolean = false, hostId: String? = null, classifiers: Set<String> = emptySet(), onFocus: (() -> Unit)? = null, content: Any? = null, onClick: () -> Unit) {
        val finalScreenTitle = if (screenTitle == AzNavRail.noTitle) null else screenTitle ?: text
        onClickMap[id] = onClick
        if (onFocus != null) onFocusMap[id] = onFocus
        navItems.add(AzNavItem(id = id, text = text, route = route, screenTitle = finalScreenTitle, isRailItem = isRailItem, disabled = disabled, isHost = isHost, isSubItem = isSubItem, hostId = hostId, info = info, classifiers = classifiers, content = content))
    }
}
