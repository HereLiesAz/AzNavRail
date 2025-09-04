package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class RailItem(
    open val id: String,
    open val text: String,
    open val icon: ImageVector?
) {
    data class RailAction(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val onClick: () -> Unit
    ) : RailItem(id, text, icon)

    data class RailToggle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : RailItem(id, text, icon)

    data class RailCycle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : RailItem(id, text, icon)
}
