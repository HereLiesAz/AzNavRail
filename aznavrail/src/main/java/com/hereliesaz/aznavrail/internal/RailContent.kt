package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
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

/**
 * Internal composable for displaying a single item within the collapsed rail.
 *
 * It wraps [AzNavRailButton] and handles item-specific logic like click handling,
 * state management (toggles, cyclers), and coordinate reporting.
 *
 * @param item The navigation item to display.
 * @param navController The navigation controller for routing.
 * @param isSelected Whether this item is currently selected.
 * @param buttonSize The size of the button.
 * @param onClick Optional additional click callback.
 * @param onRailCyclerClick Callback for cycler item clicks.
 * @param onItemClick Callback when any item is clicked.
 * @param onHostClick Callback when a host item is clicked.
 * @param onItemGloballyPositioned Callback reporting the item's global bounds.
 * @param infoScreen Whether the info screen (help mode) is active.
 * @param dragModifier Modifier for drag-and-drop interactions.
 * @param activeColor The active color for the button.
 */
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
    infoScreen: Boolean = false,
    dragModifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color? = null
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    // In infoScreen mode, only Host items are interactive.
    // Others are disabled.
    val isInteractive = if (infoScreen) item.isHost else !item.disabled

    // If infoScreen is true, we force enabled=false for non-hosts to give visual cue (greyed out).
    // If not infoScreen, we respect item.disabled.
    val isEnabled = if (infoScreen) item.isHost else !item.disabled

    val finalOnClick: () -> Unit = if (infoScreen) {
        if (item.isHost) {
            { onHostClick() }
        } else {
            {} // No-op
        }
    } else if (item.isRelocItem) {
        // Reloc items do not support navigation/clicking via RailContent;
        // logic is handled by DraggableRailItemWrapper.
        {}
    } else {
        if (item.isHost) {
            {
                handleHostItemClick(item, navController, onClick, onItemClick, onHostClick)
            }
        } else if (item.isCycler) {
            {
                onRailCyclerClick(item)
                onItemClick()
            }
        } else {
            {
                item.route?.let { navController?.navigate(it) }
                onClick?.invoke()
                onItemClick()
            }
        }
    }

    Box(
        modifier = (if (item.shape == AzButtonShape.RECTANGLE) Modifier.padding(vertical = 2.dp) else Modifier)
            .onGloballyPositioned { coordinates ->
                onItemGloballyPositioned?.invoke(item.id, coordinates.boundsInWindow())
            }
            .then(dragModifier)
    ) {
        AzNavRailButton(
            onClick = finalOnClick,
            text = textToShow,
            modifier = Modifier.requiredWidth(buttonSize),
            color = item.color ?: MaterialTheme.colorScheme.primary,
            activeColor = activeColor ?: MaterialTheme.colorScheme.primary,
            size = buttonSize,
            shape = item.shape,
            enabled = isEnabled,
            isSelected = isSelected,
            itemContent = item.content
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
