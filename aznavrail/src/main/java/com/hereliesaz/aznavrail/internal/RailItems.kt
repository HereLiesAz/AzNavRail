package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun RailItems(
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: MutableMap<String, Boolean>,
    packRailButtons: Boolean,
    onClickOverride: ((AzNavItem) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    infoScreen: Boolean = false
) {
    val topLevelItems = items.filter { !it.isSubItem }
    val itemsToRender =
        if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems

    itemsToRender.forEach { item ->
        if (item.isRailItem) {
            RailContent(
                item = item,
                navController = navController,
                isSelected = item.route == currentDestination,
                buttonSize = buttonSize,
                onClick = if (onClickOverride != null) { { onClickOverride(item) } } else scope.onClickMap[item.id],
                onRailCyclerClick = onRailCyclerClick,
                onItemClick = { onItemSelected(item) },
                onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
                onItemGloballyPositioned = onItemGloballyPositioned,
                infoScreen = infoScreen
            )
            AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                Column {
                    val subItems = scope.navItems.filter { it.hostId == item.id && it.isRailItem }
                    subItems.forEach { subItem ->
                        RailContent(
                            item = subItem,
                            navController = navController,
                            isSelected = subItem.route == currentDestination,
                            buttonSize = buttonSize,
                            onClick = if (onClickOverride != null) { { onClickOverride(subItem) } } else scope.onClickMap[subItem.id],
                            onRailCyclerClick = onRailCyclerClick,
                            onItemClick = { onItemSelected(subItem) },
                            onItemGloballyPositioned = onItemGloballyPositioned,
                            infoScreen = infoScreen
                        )
                    }
                }
            }
        } else { // This branch is only taken when packRailButtons is false for non-rail items
            Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
        }
    }
}