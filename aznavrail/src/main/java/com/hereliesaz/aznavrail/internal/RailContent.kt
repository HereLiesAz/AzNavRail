package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
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
    val shapeModifier = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.RECTANGLE -> RectangleShape
        AzButtonShape.ROUNDED_RECTANGLE -> RoundedCornerShape(12.dp)
    }

    val displayText = when {
        item.isToggle -> if (item.isChecked) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    Box(modifier = dragModifier.clip(shapeModifier)) {
        AzButton(
            onClick = {
                if (item.isCycler) {
                    onRailCyclerClick(item)
                } else if (item.isHost) {
                    onHostClick?.invoke()
                } else {
                    onClick?.invoke()
                    onItemClick()
                }
            },
            text = displayText ?: "",
            shape = shape,
            activeColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
            enabled = !item.disabled
        )
    }
}
