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
     * SECTOR 1: VISUAL COMPLIANCE
     * Configures the aesthetic properties of the rail.
     *
     * @param activeColor Custom color for the active/selected item.
     * @param defaultShape The default shape for rail items.
     * @param headerIconShape The shape of the header icon.
     * @param expandedWidth The width of the expanded rail/menu.
     * @param collapsedWidth The width of the collapsed rail.
     * @param showFooter If true, the footer is displayed in the expanded menu.
     */
    fun azTheme(
        activeColor: Color? = null,
        defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
        expandedWidth: Dp = 260.dp,
        collapsedWidth: Dp = 80.dp,
        showFooter: Boolean = true
    )

    /**
     * SECTOR 2: BEHAVIORAL PROTOCOLS
     * Configures the interaction models and structural behavior.
     *
     * @param dockingSide The side of the screen to dock the rail (LEFT or RIGHT).
     * @param packButtons If true, rail buttons are packed together without spacing.
     * @param noMenu If true, all items are treated as rail items and the side drawer is disabled.
     * @param vibrate If true, enables haptic feedback for gestures.
     * @param displayAppName If true, the app name is displayed in the header instead of the icon.
     * @param activeClassifiers Set of classifiers to mark items as active aside from route/selection.
     */
    fun azConfig(
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        packButtons: Boolean = false,
        noMenu: Boolean = false,
        vibrate: Boolean = false,
        displayAppName: Boolean = false,
        activeClassifiers: Set<String> = emptySet()
    )

    /**
     * SECTOR 3: SPECIAL OPERATIONS
     * Advanced configurations for overlays, loading states, and help modes.
     *
     * @param isLoading If true, a full-screen loading overlay is shown.
     * @param infoScreen If true, activates the interactive help overlay.
     * @param onDismissInfoScreen Callback to dismiss the info screen.
     * @param overlayService The class of the service to start for system overlay mode.
     * @param onUndock Callback triggered when the rail is undocked (e.g., long-press header).
     * @param enableRailDragging If true, enables FAB mode (draggable rail).
     * @param onRailDrag Callback for custom drag handling in FAB mode.
     * @param onOverlayDrag Callback for custom drag handling in overlay mode.
     * @param onItemGloballyPositioned Callback for getting item positions (used for Info Screen).
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
    fun azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String? = null)

    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info: String? = null, onClick: () -> Unit)
    fun azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape? = null, route: String, disabled: Boolean = false, screenTitle: String? = null, info
