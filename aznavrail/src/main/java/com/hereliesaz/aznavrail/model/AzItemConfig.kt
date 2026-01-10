package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

data class AzItemConfig(
    val color: Color? = null,
    val shape: AzButtonShape? = null,
    val disabled: Boolean = false,
    val screenTitle: String? = null,
    val info: String? = null,
    val classifiers: Set<String> = emptySet(),
    val onFocus: (() -> Unit)? = null
)
