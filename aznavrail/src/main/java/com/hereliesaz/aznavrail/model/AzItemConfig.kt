package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * Transient parameter bundle passed from DSL builder methods to the private `addItem`,
 * `addToggle`, and `addCycler` helpers inside [com.hereliesaz.aznavrail.AzNavRailScopeImpl].
 *
 * Not part of the public API; callers interact through [com.hereliesaz.aznavrail.AzNavRailScope].
 *
 * @param route Navigation route associated with the item.
 * @param screenTitle Title displayed on the screen when this item is active.
 * @param info Help description shown in the Help overlay.
 * @param isRailItem Whether the item appears on the collapsed rail (vs. menu-only).
 * @param disabled Whether the item is non-interactive.
 * @param isHost Whether the item is a parent that can expand sub-items inline.
 * @param isSubItem Whether the item is a child of a host item.
 * @param hostId ID of the parent host item; required when [isSubItem] is true.
 * @param classifiers Strings used for programmatic active-state highlighting.
 * @param onFocus Callback invoked when the item receives focus (focus map wiring).
 * @param content Custom visual content for the button (Color, resource Int, image model, or [AzComposableContent]).
 * @param color Border/icon color override.
 * @param textColor Text color override.
 * @param fillColor Translucent fill color override.
 * @param shape Button shape override; falls back to the scope's [com.hereliesaz.aznavrail.AzNavRailScopeImpl.defaultShape].
 */
data class AzItemConfig(
    val route: String? = null,
    val screenTitle: String? = null,
    val info: String? = null,
    val isRailItem: Boolean = false,
    val disabled: Boolean = false,
    val isHost: Boolean = false,
    val isSubItem: Boolean = false,
    val hostId: String? = null,
    val classifiers: Set<String> = emptySet(),
    val onFocus: (() -> Unit)? = null,
    val content: Any? = null,
    val color: Color? = null,
    val textColor: Color? = null,
    val fillColor: Color? = null,
    val shape: AzButtonShape? = null
)
