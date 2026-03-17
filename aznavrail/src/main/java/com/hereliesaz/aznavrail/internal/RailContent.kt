// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailContent.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun RailContent(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    buttonSize: Dp,
    onClick: (() -> Unit)?,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit = {},
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    onBoundsCalculated: ((String, Rect) -> Unit)? = null,
    helpEnabled: Boolean = false,
    dragModifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color? = null
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val isEnabled = if (helpEnabled) (item.isHost || item.isHelpItem) else !item.disabled

    val finalOnClick: (() -> Unit)? = if (helpEnabled) {
        if (item.isHelpItem) { { onClick?.invoke() } } else if (item.isHost) { { onHostClick() } } else { null }
    } else if (item.isRelocItem) {
        null
    } else {
        if (item.isHost) {
            { handleHostItemClick(item, navController, onClick, onItemClick, onHostClick) }
        } else if (item.isCycler) {
            {
                onRailCyclerClick(item)
                onItemClick()
            }
        } else if (item.isNestedRail) {
            {
                onClick?.invoke()
                // Explicitly do not trigger navigate or collapse for NestedRail parent.
            }
        } else {
            {
                item.route?.let { navController?.navigate(it) }
                onClick?.invoke()
                onItemClick()
            }
        }
    }

    // Small margin applied uniformly on all sides to all items.
    Box(
        modifier = Modifier
            .padding(4.dp)
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                onBoundsCalculated?.invoke(item.id, bounds)
                onItemGloballyPositioned?.invoke(item.id, bounds)
            }
            .then(dragModifier)
    ) {
        AzNavRailButton(
            onClick = finalOnClick,
            text = textToShow,
            modifier = Modifier,
            color = item.color ?: MaterialTheme.colorScheme.primary,
            activeColor = activeColor ?: MaterialTheme.colorScheme.primary,
            textColor = item.textColor,
            fillColor = item.fillColor,
            size = buttonSize,
            shape = item.shape,
            enabled = isEnabled,
            isSelected = isSelected
        )
    }
}

internal fun handleHostItemClick(
    item: AzNavItem,
    navController: NavController?,
    onClick: (() -> Unit)?,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit
) {
    onHostClick()
    item.route?.let { navController?.navigate(it) }
    onClick?.invoke()
    onItemClick()
}
