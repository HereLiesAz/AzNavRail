package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Composable for displaying a single item in the collapsed rail.
 *
 * @param item The navigation item to display.
 * @param buttonSize The size of the button.
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
    onHostClick: () -> Unit = {}
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val finalOnClick = if (item.isHost) {
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

    Box(
        modifier = if (item.shape == AzButtonShape.RECTANGLE) Modifier.padding(vertical = 2.dp) else Modifier
    ) {
        AzNavRailButton(
            onClick = finalOnClick,
            text = textToShow,
            modifier = Modifier.width(buttonSize),
            color = item.color ?: MaterialTheme.colorScheme.primary,
            size = buttonSize,
            shape = item.shape,
            enabled = !item.disabled,
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
