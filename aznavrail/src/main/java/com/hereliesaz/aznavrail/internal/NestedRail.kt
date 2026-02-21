// aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/NestedRail.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.util.EqualWidthLayout

@Composable
internal fun NestedRail(
    parentItem: AzNavItem,
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    anchorBounds: Rect,
    rootBounds: Rect,
    onDismiss: () -> Unit,
    isRightDocked: Boolean,
    onItemSelected: (AzNavItem) -> Unit,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    val buttonSize = AzNavRailDefaults.HeaderIconSize
    val alignment = parentItem.nestedRailAlignment

    val positionProvider = remember(anchorBounds, rootBounds, isRightDocked, alignment) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBoundsIgnored: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize
            ): IntOffset {
                val contentWidth = popupContentSize.width
                val windowWidth = windowSize.width
                val contentHeight = popupContentSize.height
                val windowHeight = windowSize.height

                val x = if (alignment == AzNestedRailAlignment.VERTICAL) {
                    (windowWidth - contentWidth) / 2
                } else {
                    if (isRightDocked) {
                        anchorBounds.left.toInt() - contentWidth
                    } else {
                        anchorBounds.right.toInt()
                    }
                }

                val maxRailHeightPx = (windowHeight * 0.8f).toInt()

                val y = if (alignment == AzNestedRailAlignment.VERTICAL) {
                    if (contentHeight >= maxRailHeightPx) {
                        0
                    } else {
                        (windowHeight - contentHeight) / 2
                    }
                } else {
                    val desiredY = anchorBounds.top.toInt()
                    if (desiredY + contentHeight > windowHeight) {
                        (windowHeight - contentHeight).coerceAtLeast(0)
                    } else {
                        desiredY
                    }
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

        val maxRailHeight = config.screenHeightDp.dp * 0.8f
        val maxRailWidth = config.screenWidthDp.dp * 0.8f

        Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, border, RoundedCornerShape(8.dp))
                .padding(4.dp)
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
                    onItemSelected = onItemSelected,
                    activeColor = activeColor,
                    defaultShape = defaultShape
                )
            } else {
                VerticalNestedRailContent(
                    items = items,
                    scope = scope,
                    navController = navController,
                    currentDestination = currentDestination,
                    buttonSize = buttonSize,
                    onItemSelected = onItemSelected,
                    activeColor = activeColor,
                    defaultShape = defaultShape
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
    onItemSelected: (AzNavItem) -> Unit,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    EqualWidthLayout(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalSpacing = AzNavRailDefaults.RailContentVerticalArrangement
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
            NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected, activeColor = activeColor, defaultShape = defaultShape)

            if (item.isHost && (hostStates[item.id] == true)) {
                items.filter { it.hostId == item.id }.forEach { subItem ->
                    NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true, onItemSelected = onItemSelected, activeColor = activeColor, defaultShape = defaultShape)
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
    onItemSelected: (AzNavItem) -> Unit,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        items.filter { !it.isSubItem }.forEach { item ->
            Column {
                NestedRailItemWrapper(item, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = false, onItemSelected = onItemSelected, activeColor = activeColor, defaultShape = defaultShape)

                if (item.isHost && (hostStates[item.id] == true)) {
                    items.filter { it.hostId == item.id }.forEach { subItem ->
                        NestedRailItemWrapper(subItem, scope, navController, currentDestination, buttonSize, hostStates, isSubItem = true, onItemSelected = onItemSelected, activeColor = activeColor, defaultShape = defaultShape)
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
    onItemSelected: (AzNavItem) -> Unit,
    activeColor: Color,
    defaultShape: AzButtonShape
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
        onRailCyclerClick = { },
        onItemClick = { onItemSelected(item) },
        onHostClick = {
            hostStates[item.id] = !(hostStates[item.id] ?: false)
        },
        infoScreen = false,
        activeColor = activeColor,
        shape = defaultShape,
        dragModifier = Modifier.fillMaxWidth()
    )
}