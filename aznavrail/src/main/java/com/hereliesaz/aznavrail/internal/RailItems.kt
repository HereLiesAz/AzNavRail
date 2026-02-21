package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.DraggableRailItemWrapper
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation

@Composable
internal fun RailItems(
    items: List<AzNavItem>,
    currentDestination: String?,
    activeColor: Color,
    shape: AzButtonShape,
    buttonSize: Dp,
    activeClassifiers: Set<String>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    onItemSelected: (AzNavItem) -> Unit,
    onRailCyclerClick: (AzNavItem) -> Unit,
    hostStates: Map<String, Boolean>,
    packRailButtons: Boolean,
    orientation: AzOrientation,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    reverseLayout: Boolean
) {
    val isVertical = orientation == AzOrientation.Vertical
    val topLevelItems = items.filter { !it.isSubItem }
    val baseItems = if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems
    val itemsToRender = if (reverseLayout) baseItems.reversed() else baseItems

    val draggedItemId = remember { mutableStateOf<String?>(null) }
    val dragOffset = remember { mutableStateOf(0f) }
    val currentDropTargetIndex = remember { mutableStateOf<Int?>(null) }
    val mutableItems = remember(itemsToRender) { itemsToRender.toMutableList() }

    val content = @Composable {
        mutableItems.forEach { item ->
            val isHostExpanded = hostStates[item.id] == true
            
            DraggableRailItemWrapper(
                item = item,
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                buttonSize = buttonSize,
                orientation = orientation,
                onRailCyclerClick = onRailCyclerClick,
                onItemSelected = onItemSelected,
                hostStates = hostStates,
                onClickOverride = null,
                onItemGloballyPositioned = onItemGloballyPositioned,
                infoScreen = infoScreen,
                draggedItemId = draggedItemId.value,
                dragOffset = dragOffset.value,
                currentDropTargetIndex = currentDropTargetIndex.value,
                onDragStart = { id -> draggedItemId.value = id },
                onDragEnd = {
                    val targetIdx = currentDropTargetIndex.value
                    if (targetIdx != null && draggedItemId.value != null) {
                        RelocItemHandler.updateOrder(mutableItems, draggedItemId.value!!, targetIdx)
                        scope.onRelocateMap[draggedItemId.value!!]?.invoke(
                            itemsToRender.indexOfFirst { it.id == draggedItemId.value },
                            targetIdx,
                            mutableItems.map { it.id }
                        )
                    }
                    draggedItemId.value = null
                    dragOffset.value = 0f
                    currentDropTargetIndex.value = null
                },
                onDragCancel = {
                    draggedItemId.value = null
                    dragOffset.value = 0f
                    currentDropTargetIndex.value = null
                },
                onDrag = { delta ->
                    dragOffset.value += delta
                    currentDropTargetIndex.value = RelocItemHandler.calculateTargetIndex(
                        items = mutableItems,
                        draggedItemId = draggedItemId.value!!,
                        currentDragOffset = dragOffset.value,
                        itemBounds = RelocItemHandler.itemBoundsCache,
                        isVertical = isVertical
                    )
                }
            )

            if (isHostExpanded) {
                val subItems = items.filter { it.isSubItem && it.hostId == item.id }
                val subItemsToRender = if (reverseLayout) subItems.reversed() else subItems
                val mutableSubItems = remember(subItemsToRender) { subItemsToRender.toMutableList() }
                
                mutableSubItems.forEach { subItem ->
                    DraggableRailItemWrapper(
                        item = subItem,
                        scope = scope,
                        navController = navController,
                        currentDestination = currentDestination,
                        buttonSize = buttonSize,
                        orientation = orientation,
                        onRailCyclerClick = onRailCyclerClick,
                        onItemSelected = onItemSelected,
                        hostStates = hostStates,
                        onClickOverride = null,
                        onItemGloballyPositioned = onItemGloballyPositioned,
                        infoScreen = infoScreen,
                        draggedItemId = draggedItemId.value,
                        dragOffset = dragOffset.value,
                        currentDropTargetIndex = currentDropTargetIndex.value,
                        onDragStart = { id -> draggedItemId.value = id },
                        onDragEnd = {
                            val targetIdx = currentDropTargetIndex.value
                            if (targetIdx != null && draggedItemId.value != null) {
                                RelocItemHandler.updateOrder(mutableSubItems, draggedItemId.value!!, targetIdx)
                                scope.onRelocateMap[draggedItemId.value!!]?.invoke(
                                    subItemsToRender.indexOfFirst { it.id == draggedItemId.value },
                                    targetIdx,
                                    mutableSubItems.map { it.id }
                                )
                            }
                            draggedItemId.value = null
                            dragOffset.value = 0f
                            currentDropTargetIndex.value = null
                        },
                        onDragCancel = {
                            draggedItemId.value = null
                            dragOffset.value = 0f
                            currentDropTargetIndex.value = null
                        },
                        onDrag = { delta ->
                            dragOffset.value += delta
                            currentDropTargetIndex.value = RelocItemHandler.calculateTargetIndex(
                                items = mutableSubItems,
                                draggedItemId = draggedItemId.value!!,
                                currentDragOffset = dragOffset.value,
                                itemBounds = RelocItemHandler.itemBoundsCache,
                                isVertical = isVertical
                            )
                        }
                    )
                }
            }
        }
    }

    if (isVertical) {
        Column(verticalArrangement = Arrangement.spacedBy(if (packRailButtons) 0.dp else 8.dp)) { content() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(if (packRailButtons) 0.dp else 8.dp)) { content() }
    }
}
