package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
    infoScreen: Boolean = false
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val modifier = if (item.disabled) Modifier else {
        if (infoScreen) {
            if (item.isHost) {
                Modifier.clickable { onHostClick() }
            } else {
                Modifier.clickable { /* No-op for info screen */ }
            }
        } else {
            if (item.isToggle) {
                Modifier.toggleable(
                    value = item.isChecked ?: false,
                    onValueChange = {
                        item.route?.let { navController?.navigate(it) }
                        onClick?.invoke()
                        onToggle()
                        onItemClick()
                    }
                )
            } else {
                Modifier.clickable {
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

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        item.disabled -> MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.5f)
        else -> item.color ?: MaterialTheme.typography.bodyMedium.color
    }

    Box {
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

        if (infoScreen && !item.info.isNullOrBlank()) {
            Popup(
                alignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.inverseSurface,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}
