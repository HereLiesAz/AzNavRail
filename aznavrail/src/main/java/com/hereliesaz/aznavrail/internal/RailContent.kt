// aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailContent.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun RailContent(
    item: AzNavItem,
    navController: androidx.navigation.NavController?,
    isSelected: Boolean,
    buttonSize: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)?,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemClick: () -> Unit,
    onHostClick: (() -> Unit)? = null,
    onItemGloballyPositioned: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    infoScreen: Boolean,
    dragModifier: Modifier = Modifier,
    activeColor: Color,
    shape: AzButtonShape
) {
    val displayText = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    Box(modifier = dragModifier) {
        AzNavRailButton(
            onClick = {
                // Ensure base onClick (which contains onToggle for MenuItems) fires regardless of type
                onClick?.invoke()

                if (item.isCycler) {
                    onRailCyclerClick(item)
                } else if (item.isHost) {
                    onHostClick?.invoke()
                } else {
                    onItemClick()
                }
            },
            text = displayText ?: "",
            modifier = Modifier,
            size = buttonSize,
            color = MaterialTheme.colorScheme.onSurface,
            activeColor = activeColor,
            shape = shape,
            enabled = !item.disabled,
            isSelected = isSelected,
            itemContent = item.content
        )
    }
}