package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

/**
 * Composable that renders a nested rail popup.
 *
 * @param parentItem The item triggering the nested rail.
 * @param items The items within the nested rail.
 * @param scope The configuration scope.
 * @param navController The navigation controller.
 * @param currentDestination The current navigation route.
 * @param anchorBounds The screen bounds of the parent item (anchor).
 * @param rootBounds The screen bounds of the root container.
 * @param onDismiss Callback to dismiss the popup.
 * @param isRightDocked Whether the main rail is right-docked.
 * @param onItemSelected Callback for item selection.
 */
@Composable
internal fun NestedRail(
    parentItem: AzNavItem,
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    anchorBounds: Rect,
    rootBounds: Rect, // Bounds of the AzNavRail
    onDismiss: () -> Unit,
    isRightDocked: Boolean,
    onItemSelected: (AzNavItem) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val buttonSize = AzNavRailDefaults.HeaderIconSize

    // We use a custom PositionProvider to handle "anchored to side" and "visible boundaries"
    val positionProvider = remember(anchorBounds, rootBounds, isRightDocked, parentItem.nestedRailAlignment) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize
            ): IntOffset {
                // anchorBounds here is relative to window.
                // We want to align top with anchorBounds.top.
                // We want to align Left/Right based on docking.

                val x = if (isRightDocked) {
                    anchorBounds.left - popupContentSize.width
                } else {
                    anchorBounds.right
                }

                val y = anchorBounds.top

                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        val backgroundColor = MaterialTheme.colorScheme.surface
        val border = MaterialTheme.colorScheme.outline

        // Calculate max height based on rootBounds and anchor
        // "visible boundaries being the same as the AzNavRail itself"
        // Since we align top to anchor, max height = rootBounds.bottom - anchor.top
        // Wait, rootBounds might be the visible area.
        // Let's rely on screen height or passed rootBounds.
        val maxHeightDp = with(density) { (rootBounds.bottom - anchorBounds.top).toDp() }

        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, border, RoundedCornerShape(8.dp))
                .padding(4.dp)
                .heightIn(max = maxHeightDp)
        ) {
            if (parentItem.nestedRailAlignment == AzNestedRailAlignment.HORIZONTAL) {
                 HorizontalNestedRailContent(
                     items = items,
                     scope = scope,
                     navController = navController,
                     currentDestination = currentDestination,
                     buttonSize = buttonSize,
                     isRightDocked = isRightDocked,
                     onItemSelected = onItemSelected
                 )
            } else {
                 VerticalNestedRailContent(
                     items = items,
                     scope = scope,
                     navController = navController,
                     currentDestination = currentDestination,
                     buttonSize = buttonSize,
                     onItemSelected = onItemSelected
                 )
            }
        }
    }
}

@Composable
private fun VerticalNestedRailContent(
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: androidx.compose.ui.unit.Dp,
    onItemSelected: (AzNavItem) -> Unit
) {
    // Reuse similar logic to RailItems but simpler (no drag)
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
            // Render item
            NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected)

            // Render subitems if expanded
            if (item.isHost && (hostStates[item.id] == true)) {
                items.filter { it.hostId == item.id }.forEach { subItem ->
                     NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true, onItemSelected = onItemSelected)
                }
            }
        }
    }
}

@Composable
private fun HorizontalNestedRailContent(
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: androidx.compose.ui.unit.Dp,
    isRightDocked: Boolean,
    onItemSelected: (AzNavItem) -> Unit
) {
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
             Column {
                 NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected)

                 // "RailSubItems should expand downward, vertically"
                 // So we put them in a Column under the item
                 if (item.isHost && (hostStates[item.id] == true)) {
                      items.filter { it.hostId == item.id }.forEach { subItem ->
                           NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true, onItemSelected = onItemSelected)
                      }
                 }
             }
        }
    }
}

@Composable
private fun NestedRailItemWrapper(
    item: AzNavItem,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: androidx.compose.ui.unit.Dp,
    hostStates:  MutableMap<String, Boolean>,
    isSubItem: Boolean = false,
    onItemSelected: (AzNavItem) -> Unit
) {
    val isSelected = item.route != null && item.route == currentDestination

    RailContent(
        item = item,
        navController = navController,
        isSelected = isSelected,
        buttonSize = buttonSize,
        onClick = {
             scope.onClickMap[item.id]?.invoke()
        },
        onRailCyclerClick = { /* Support if needed */ },
        onItemClick = { onItemSelected(item) },
        onHostClick = {
            hostStates[item.id] = !(hostStates[item.id] ?: false)
        },
        infoScreen = false,
        activeColor = scope.activeColor
    )
}
