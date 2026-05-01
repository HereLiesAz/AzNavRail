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

/**
 * Renders a single button in the collapsed rail, wiring click behaviour based on the item's type.
 *
 * Handles standard items, toggles, cyclers, host items, nested-rail parents, and relocatable items.
 * When [helpEnabled] is true, only host and help items are interactive.
 *
 * @param defaultShape Fallback shape from the scope when [item.shape] is null.
 * @param item The nav item to render.
 * @param navController Used to trigger route navigation on click.
 * @param isSelected Whether the button is visually highlighted as the active item.
 * @param buttonSize The uniform button size in dp (shrunk when a vertical nested rail is open).
 * @param onClick Primary click callback (null renders the button as inert for relocatable items).
 * @param onRailCyclerClick Invoked when a cycler item is tapped (includes delay-commit logic).
 * @param onItemClick Invoked after click for side-effects like collapsing the menu.
 * @param onHostClick Invoked when a host item is tapped to expand/collapse its sub-items.
 * @param onItemGloballyPositioned Reports window-space bounds to the scope for help/tutorial overlays.
 * @param onBoundsCalculated Secondary bounds callback written directly to the scope's bounds cache.
 * @param helpEnabled When true, disables non-host/non-help interactions and dims other items.
 * @param dragModifier Additional pointer-input modifier attached for relocatable drag handling.
 * @param activeColor Override for the active-state color; falls back to [MaterialTheme.colorScheme.primary].
 */
@Composable
internal fun RailContent(
    defaultShape: com.hereliesaz.aznavrail.model.AzButtonShape,
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
            shape = item.shape ?: defaultShape,
            enabled = isEnabled,
            isSelected = isSelected
        )
    }
}

/**
 * Executes the standard click sequence for a host item: toggles expansion, navigates if a route
 * is set, fires the item's own callback, and notifies the general item-click handler.
 */
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
