package com.hereliesaz.aznavrail.model

import androidx.compose.ui.geometry.Rect

/**
 * Internal configuration object used to group common parameters
 * passed from the DSL methods to the internal helper functions
 * in order to avoid excessively long parameter lists.
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
    val helpList: Map<String, String> = emptyMap()
)
