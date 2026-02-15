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
    isRightDocked: Boolean
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val buttonSize = AzNavRailDefaults.HeaderIconSize

    val offsetX = if (isRightDocked) {
        // To the left of the parent
        // anchorBounds is in window coordinates?
        // Popup relative to anchor?
        // If we use Popup with offset, it is relative to the parent Composable (the RailItem).
        // Since NestedRail is called inside RailItems (which is inside the rail), the parent is the RailItem wrapper.
        // So offset (0,0) is top-left of RailItem.
        // Left of parent = -width of nested rail.
        // But we don't know width yet.
        // We might need to use absolute positioning or Alignment.TopEnd with negative offset.
        // Or simply IntOffset(-width, 0) if we knew width.
        // For now let's assume standard Popup logic.
        // Actually, if we use `PopupPositionProvider`, we can be precise.
        0 // Placeholder
    } else {
        // To the right of the parent
        with(density) { anchorBounds.width.toInt() }
    }

    val offsetY = 0 // Top aligned

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
                     isRightDocked = isRightDocked
                 )
            } else {
                 VerticalNestedRailContent(
                     items = items,
                     scope = scope,
                     navController = navController,
                     currentDestination = currentDestination,
                     buttonSize = buttonSize
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
    buttonSize: androidx.compose.ui.unit.Dp
) {
    // Reuse similar logic to RailItems but simpler (no drag)
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
            // Render item
            NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates)

            // Render subitems if expanded
            if (item.isHost && (hostStates[item.id] == true)) {
                items.filter { it.hostId == item.id }.forEach { subItem ->
                     NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true)
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
    isRightDocked: Boolean
) {
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
             Column {
                 NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates)

                 // "RailSubItems should expand downward, vertically"
                 // So we put them in a Column under the item
                 if (item.isHost && (hostStates[item.id] == true)) {
                      items.filter { it.hostId == item.id }.forEach { subItem ->
                           NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true)
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
    isSubItem: Boolean = false
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
        onItemClick = {}, // Maybe tracking?
        onHostClick = {
            hostStates[item.id] = !(hostStates[item.id] ?: false)
        },
        infoScreen = false,
        activeColor = scope.activeColor
    )
}
