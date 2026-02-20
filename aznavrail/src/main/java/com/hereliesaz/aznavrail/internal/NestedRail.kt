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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzButtonShape
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
    rootBounds: Rect,
    onDismiss: () -> Unit,
    isRightDocked: Boolean,
    onItemSelected: (AzNavItem) -> Unit,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    val alignment = parentItem.nestedRailAlignment
    val positionProvider = remember(anchorBounds, rootBounds, isRightDocked, alignment) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBoundsIgnored: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize
            ): IntOffset {
                val x = if (isRightDocked) anchorBounds.left.toInt() - popupContentSize.width else anchorBounds.right.toInt()
                val y = if (alignment == AzNestedRailAlignment.VERTICAL) {
                    if (popupContentSize.height >= windowSize.height) 0 else (windowSize.height - popupContentSize.height) / 2 
                } else anchorBounds.top.toInt()
                return IntOffset(x, y)
            }
        }
    }

    Popup(popupPositionProvider = positionProvider, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(4.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                items.forEach { item ->
                    RailContent(
                        item = item, navController = navController, isSelected = item.route == currentDestination,
                        buttonSize = 48.dp, onClick = { scope.onClickMap[item.id]?.invoke() },
                        onRailCyclerClick = {}, onItemClick = { onItemSelected(item) },
                        infoScreen = false, activeColor = activeColor, shape = defaultShape
                    )
                }
            }
        }
    }
}
