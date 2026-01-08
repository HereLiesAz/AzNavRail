package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Composable for displaying a single item in the expanded menu.
 *
 * This composable handles the display and interaction for all types of
 * menu items, including standard, toggle, and cycler items. It supports
 * multi-line text with indentation for all lines after the first.
 *
 * @param item The navigation item to display.
 * @param onCyclerClick The click handler for cycler items, which includes
 *    the delay logic.
 * @param onToggle The click handler for toggling the rail's expanded
 *    state. This is called immediately for standard and toggle items, and
 *    with a delay for cycler items.
 */
@Composable
internal fun MenuItem(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    onCyclerClick: (() -> Unit)? = null,
    onToggle: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onHostClick: () -> Unit = {},
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    infoScreen: Boolean = false,
    activeColor: androidx.compose.ui.graphics.Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    // Interaction Logic:
    // If infoScreen: only Host items are interactive.
    // If normal: depends on item.disabled.

    // Visual Logic:
    // If infoScreen: non-Host items look disabled (grey).
    // If normal: disabled items look disabled.

    val isDisabled = if (infoScreen) !item.isHost else item.disabled

    val modifier = if (isDisabled) Modifier else {
        if (infoScreen) {
             // Host item in infoScreen is interactive
             Modifier.clickable(interactionSource = interactionSource, indication = null) { onHostClick() }
        } else {
            // Normal mode
            if (item.isToggle) {
                Modifier.toggleable(
                    value = item.isChecked ?: false,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = {
                        item.route?.let { navController?.navigate(it) }
                        onClick?.invoke()
                        onToggle()
                        onItemClick()
                    }
                )
            } else {
                Modifier.clickable(interactionSource = interactionSource, indication = null) {
                    if (item.isHost) {
                        handleHostItemClick(item, navController, onClick, onItemClick, onHostClick)
                    } else if (item.isCycler) {
                        onCyclerClick?.invoke()
                    } else {
                        item.route?.let { navController?.navigate(it) }
                        onClick?.invoke()
                        onToggle()
                        onItemClick()
                    }
                }
            }
        }
    }

    val effectiveActiveColor = activeColor ?: MaterialTheme.colorScheme.primary
    val effectiveDefaultColor = item.color ?: MaterialTheme.typography.bodyMedium.color

    val targetColor = if (isPressed) {
        if (isSelected) effectiveDefaultColor else effectiveActiveColor
    } else {
        if (isSelected) effectiveActiveColor else effectiveDefaultColor
    }

    val textColor = if (isDisabled) {
        MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.5f)
    } else {
        targetColor
    }

    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            onItemGloballyPositioned?.invoke(item.id, coordinates.boundsInWindow())
        }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AzNavRailDefaults.MenuItemHorizontalPadding,
                    vertical = AzNavRailDefaults.MenuItemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val lines = textToShow.split('\n')
            Column(modifier = Modifier.weight(1f)) {
                lines.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = if (index > 0) Modifier.padding(start = 16.dp) else Modifier
                    )
                }
            }
        }
    }
}
