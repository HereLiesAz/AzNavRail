package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import android.util.Log
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MenuItem(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onCyclerClick: (() -> Unit)? = null,
    onToggle: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onHostClick: () -> Unit = {},
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    helpEnabled: Boolean = false,
    activeColor: androidx.compose.ui.graphics.Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.menuToggleOnText ?: item.toggleOnText else item.menuToggleOffText ?: item.toggleOffText
        item.isCycler -> {
            // Find the index of the selected option, and use the menu option at that index if available.
            // If configuration is inconsistent, log a warning so it is visible during development.
            val index = item.options?.indexOf(item.selectedOption) ?: -1
            val menuOptions = item.menuOptions

            if (index != -1 && menuOptions != null && index in menuOptions.indices) {
                menuOptions[index]
            } else {
                if (menuOptions != null) {
                    Log.w(
                        "MenuItem",
                        "Cycler configuration mismatch for item='${item.text}': " +
                            "selectedOption='${item.selectedOption}', " +
                            "index=$index, " +
                            "optionsSize=${item.options?.size}, " +
                            "menuOptionsSize=${menuOptions.size}"
                    )
                }
                item.selectedOption ?: ""
            }
        }
        else -> item.menuText ?: item.text
    }

    // Interaction Logic:
    // If helpEnabled: only Host and Help items are interactive.
    // If normal: depends on item.disabled.

    // Visual Logic:
    // If helpEnabled: non-Host/Help items look disabled (grey).
    // If normal: disabled items look disabled.

    val isDisabled = if (helpEnabled) !(item.isHost || item.isHelpItem) else item.disabled

    val modifier = if (isDisabled) Modifier else {
        if (helpEnabled) {
             // Host or Help item in helpEnabled mode is interactive
             Modifier.clickable(interactionSource = interactionSource, indication = null) {
                 if (item.isHelpItem) onClick?.invoke() else onHostClick()
             }
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
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = onLongClick,
                    onClick = {
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
                )
            }
        }
    }

    val effectiveActiveColor = activeColor ?: MaterialTheme.colorScheme.primary
    val effectiveDefaultColor = item.textColor ?: item.color ?: MaterialTheme.colorScheme.primary

    val textColor = if (isDisabled) {
        effectiveDefaultColor.copy(alpha = 0.5f)
    } else {
        effectiveDefaultColor
    }

    val backgroundColor = if (isSelected && !isPressed) {
        (item.fillColor ?: effectiveActiveColor).copy(alpha = 0.12f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                onItemGloballyPositioned?.invoke(item.id, coordinates.boundsInWindow())
            }
            .background(backgroundColor)
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
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor,
                        modifier = if (index > 0) Modifier.padding(start = 16.dp) else Modifier
                    )
                }
            }
        }
    }
}
