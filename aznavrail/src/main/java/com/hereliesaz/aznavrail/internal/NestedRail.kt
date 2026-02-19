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
    isRightDocked: Boolean,
    onItemSelected: (AzNavItem) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val buttonSize = AzNavRailDefaults.HeaderIconSize
    val alignment = parentItem.nestedRailAlignment

    // Custom PositionProvider for "Center on SCREEN" (Vertical) and "Top Align" (Horizontal)
    val positionProvider = remember(anchorBounds, rootBounds, isRightDocked, alignment) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBoundsIgnored: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize
            ): IntOffset {
                // X Position: Always to the side of the parent
                val x = if (isRightDocked) {
                    anchorBounds.left.toInt() - popupContentSize.width
                } else {
                    anchorBounds.right.toInt()
                }

                val contentHeight = popupContentSize.height
                val windowHeight = windowSize.height
                
                // Y Position Calculation
                val y = if (alignment == AzNestedRailAlignment.VERTICAL) {
                    // Vertical Nested Rail: Center on SCREEN
                    // Note: Content size is already constrained by the Box below, so it fits.
                    if (contentHeight >= windowHeight) {
                        0 
                    } else {
                        (windowHeight - contentHeight) / 2 // Center relative to Screen
                    }
                } else {
                    // Horizontal Nested Rail: Align top with parent top
                    anchorBounds.top.toInt()
                }

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
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        
        // 4/5ths Rule: Cap nested rail size at 80% of screen
        val maxRailHeight = config.screenHeightDp.dp * 0.8f
        val maxRailWidth = config.screenWidthDp.dp * 0.8f

        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, border, RoundedCornerShape(8.dp))
                .padding(4.dp)
                // Apply BOTH constraints (safe since one dim will be naturally small)
                .heightIn(max = maxRailHeight)
                .widthIn(max = maxRailWidth)
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
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    // Scrollable Column
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
            NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected)

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

    // Scrollable Row
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
             Column {
                 NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected)

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
        activeColor = MaterialTheme.colorScheme.primary
    )
}
