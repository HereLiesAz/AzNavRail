package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class MenuItem(
    open val id: String,
    open val text: String,
    open val icon: ImageVector?
) {
    data class MenuAction(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val onClick: () -> Unit
    ) : MenuItem(id, text, icon)

    data class MenuToggle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : MenuItem(id, text, icon)

    data class MenuCycle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : MenuItem(id, text, icon)
}
