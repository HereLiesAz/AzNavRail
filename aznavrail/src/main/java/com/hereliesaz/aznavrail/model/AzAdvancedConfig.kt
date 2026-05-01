package com.hereliesaz.aznavrail.model

import androidx.compose.ui.geometry.Rect

/**
 * Aggregated advanced settings for the rail, populated by [com.hereliesaz.aznavrail.AzNavRailScope.azAdvanced]
 * and [com.hereliesaz.aznavrail.AzNavRailScope.azSettings].
 *
 * @param isLoading When true, the rail content is replaced by a full-screen [com.hereliesaz.aznavrail.AzLoad] spinner.
 * @param helpEnabled Whether the interactive help/info overlay is enabled.
 * @param onDismissHelp Callback invoked when the help overlay is dismissed.
 * @param overlayService Service class used to launch a system overlay (FAB mode). Automatically
 *   sets [enableRailDragging] to true when non-null.
 * @param onUndock Callback invoked when the rail is undocked to FAB mode.
 * @param enableRailDragging Whether the user can drag the rail to detach it (FAB mode).
 * @param onRailDrag Callback reporting `(dx, dy)` during in-app drag events.
 * @param onOverlayDrag Callback reporting `(dx, dy)` during system-overlay drag events.
 * @param onItemGloballyPositioned Reports the window-space [Rect] of an item by its ID after layout;
 *   primarily used by the tutorial and help systems.
 * @param secLoc Developer configuration key that unlocks the Secret Screens debug menu.
 *   Long-pressing the `@HereLiesAz` footer item prompts for this key.
 * @param secLocPort TCP port used by the location history sync server. Defaults to 10203.
 * @param helpList Map of item ID → help text (String or string resource Int) shown in the help overlay.
 * @param tutorials Map of item ID → [com.hereliesaz.aznavrail.tutorial.AzTutorial] for interactive step-by-step guides.
 */
data class AzAdvancedConfig(
    val isLoading: Boolean = false,
    val helpEnabled: Boolean = false,
    val onDismissHelp: (() -> Unit)? = null,
    val overlayService: Class<out android.app.Service>? = null,
    val onUndock: (() -> Unit)? = null,
    val enableRailDragging: Boolean = false,
    val onRailDrag: ((Float, Float) -> Unit)? = null,
    val onOverlayDrag: ((Float, Float) -> Unit)? = null,
    val onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    val secLoc: String? = null,
    val secLocPort: Int = 10203,
    val helpList: Map<String, Any> = emptyMap(),
    val tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial> = emptyMap()
)
