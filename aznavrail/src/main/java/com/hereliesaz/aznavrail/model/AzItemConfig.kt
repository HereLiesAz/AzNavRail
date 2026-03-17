package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * Internal configuration object used to group common parameters
 * passed from the DSL methods to the internal helper functions
 * ([addItem], [addToggle], [addCycler]) in order to avoid excessively
 * long parameter lists.
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
