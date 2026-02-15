package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * Configuration data class for an AzNavRail item.
 *
 * @param color The color of the item.
 * @param shape The shape of the item.
 * @param disabled Whether the item is disabled.
 * @param screenTitle The title to display on the screen when this item is active.
 * @param info The help text for the info screen.
 * @param classifiers A set of strings to classify this item.
 * @param onFocus Callback invoked when the item gains focus.
 */
data class AzItemConfig(
    val color: Color? = null,
    val shape: AzButtonShape? = null,
    val disabled: Boolean = false,
    val screenTitle: String? = null,
    val info: String? = null,
    val classifiers: Set<String> = emptySet(),
    val onFocus: (() -> Unit)? = null
)
