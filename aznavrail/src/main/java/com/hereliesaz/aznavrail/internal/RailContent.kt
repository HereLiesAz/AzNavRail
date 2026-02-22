package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
internal fun RailContent(
    items: List<AzNavItem>,
    currentDestination: String?,
    activeColor: Color,
    shape: AzButtonShape,
    activeClassifiers: Set<String>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: Map<String, Boolean>,
    packRailButtons: Boolean,
    orientation: AzOrientation,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    reverseLayout: Boolean,
    isRightDocked: Boolean
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

            DraggableItem(
                item = item,
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                activeColor = activeColor,
                defaultShape = shape,
                activeClassifiers = activeClassifiers,
                orientation = orientation,
                onItemSelected = onItemSelected,
                onItemGloballyPositioned = onItemGloballyPositioned,
                infoScreen = infoScreen,
                isRightDocked = isRightDocked,
                draggedItemId = draggedItemId,
                dragOffset = dragOffset,
                currentDropTargetIndex = currentDropTargetIndex,
                mutableItemsList = mutableItems,
                itemsToRender = itemsToRender
            )

            if (isHostExpanded) {
                val subItems = items.filter { it.isSubItem && it.hostId == item.id }
                val subItemsToRender = if (reverseLayout) subItems.reversed() else subItems
                val mutableSubItems = remember(subItemsToRender) { subItemsToRender.toMutableList() }

                mutableSubItems.forEach { subItem ->
                    DraggableItem(
                        item = subItem,
                        scope = scope,
                        navController = navController,
                        currentDestination = currentDestination,
                        activeColor = activeColor,
                        defaultShape = shape,
                        activeClassifiers = activeClassifiers,
                        orientation = orientation,
                        onItemSelected = onItemSelected,
                        onItemGloballyPositioned = onItemGloballyPositioned,
                        infoScreen = infoScreen,
                        isRightDocked = isRightDocked,
                        draggedItemId = draggedItemId,
                        dragOffset = dragOffset,
                        currentDropTargetIndex = currentDropTargetIndex,
                        mutableItemsList = mutableSubItems,
                        itemsToRender = subItemsToRender
                    )
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    if (isVertical) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(if (packRailButtons) 0.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    } else {
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(if (packRailButtons) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

@Composable
private fun DraggableItem(
    item: AzNavItem,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    activeColor: Color,
    defaultShape: AzButtonShape,
    activeClassifiers: Set<String>,
    orientation: AzOrientation,
    onItemSelected: (AzNavItem) -> Unit,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    isRightDocked: Boolean,
    draggedItemId: MutableState<String?>,
    dragOffset: MutableState<Float>,
    currentDropTargetIndex: MutableState<Int?>,
    mutableItemsList: MutableList<AzNavItem>,
    itemsToRender: List<AzNavItem>
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showHiddenMenu by remember { mutableStateOf(false) }
    var showScreenTitle by remember { mutableStateOf(false) }
    var showNestedRail by remember { mutableStateOf(false) }

    val isDragging = draggedItemId.value == item.id
    val offset = if (isDragging) dragOffset.value else 0f
    val isVertical = orientation == AzOrientation.Vertical

    if (item.isNestedRail) {
        Box {
            AzNavRailButton(
                onClick = { if (!infoScreen) showNestedRail = !showNestedRail },
                text = item.text,
                color = item.color ?: MaterialTheme.colorScheme.onSurface,
                activeColor = activeColor,
                shape = item.shape ?: defaultShape,
                enabled = !item.disabled,
                isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                itemContent = item.content,
                onGloballyPositioned = { rect -> onItemGloballyPositioned?.invoke(item.id, rect) }
            )
            if (showNestedRail) {
                Popup(
                    alignment = if (isRightDocked) Alignment.TopEnd else Alignment.TopStart,
                    onDismissRequest = { showNestedRail = false },
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                ) {
                    NestedRail(
                        parentItem = item,
                        items = item.nestedRailItems ?: emptyList(),
                        currentDestination = currentDestination,
                        activeColor = activeColor,
                        activeClassifiers = activeClassifiers,
                        onItemSelected = {
                            onItemSelected(it)
                            showNestedRail = false
                        },
                        alignment = item.nestedRailAlignment ?: AzNestedRailAlignment.VERTICAL,
                        isRightDocked = isRightDocked
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .offset { if (isVertical) IntOffset(0, offset.roundToInt()) else IntOffset(offset.roundToInt(), 0) }
            .zIndex(if (isDragging) 1f else 0f)
    ) {
        val clickModifier = if (item.isRelocItem && !infoScreen) {
            Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHiddenMenu = false
                        draggedItemId.value = item.id
                    },
                    onDragEnd = {
                        val targetIdx = currentDropTargetIndex.value
                        if (targetIdx != null && draggedItemId.value != null) {
                            RelocItemHandler.updateOrder(mutableItemsList, draggedItemId.value!!, targetIdx)
                            scope.onRelocateMap[draggedItemId.value!!]?.invoke(
                                itemsToRender.indexOfFirst { it.id == draggedItemId.value },
                                targetIdx,
                                mutableItemsList.map { it.id }
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
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset.value += if (isVertical) dragAmount.y else dragAmount.x
                        currentDropTargetIndex.value = RelocItemHandler.calculateTargetIndex(
                            items = mutableItemsList,
                            draggedItemId = draggedItemId.value!!,
                            currentDragOffset = dragOffset.value,
                            itemBounds = RelocItemHandler.itemBoundsCache,
                            isVertical = isVertical
                        )
                    }
                )
            }
        } else Modifier

        Box(modifier = clickModifier) {
            AzNavRailButton(
                onClick = {
                    if (infoScreen) return@AzNavRailButton
                    showScreenTitle = true
                    if (item.isRelocItem) showHiddenMenu = !showHiddenMenu
                    else {
                        onItemSelected(item)
                    }
                },
                onLongClick = {
                    if (infoScreen) return@AzNavRailButton
                    showScreenTitle = true
                    if (item.isRelocItem && !isDragging) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHiddenMenu = false
                        draggedItemId.value = item.id
                    }
                },
                text = item.text,
                color = item.color ?: MaterialTheme.colorScheme.onSurface,
                activeColor = activeColor,
                shape = item.shape ?: defaultShape,
                enabled = !item.disabled,
                isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                itemContent = item.content,
                onGloballyPositioned = { rect ->
                    if (item.isRelocItem) RelocItemHandler.itemBoundsCache[item.id] = rect
                    onItemGloballyPositioned?.invoke(item.id, rect)
                }
            )
        }

        // STRICT RULE: No Rounded Corners. Changed shape to RectangleShape.
        if (showScreenTitle && item.screenTitle != AzNavRailDefaults.NO_TITLE) {
            val titleText = item.screenTitle ?: item.text
            if (titleText.isNotEmpty()) {
                Popup(
                    alignment = if (isRightDocked) Alignment.TopStart else Alignment.TopEnd,
                    properties = PopupProperties(focusable = false)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .then(if (isRightDocked) Modifier.padding(start = 32.dp) else Modifier.padding(end = 16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = titleText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LaunchedEffect(Unit) { delay(1500); showScreenTitle = false }
            }
        }

        if (showHiddenMenu && item.hiddenMenuItems != null && item.hiddenMenuItems.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(if (isRightDocked) -150 else 150, 0),
                onDismissRequest = { showHiddenMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RectangleShape) // No rounded
                        .padding(8.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    item.hiddenMenuItems.forEach { hiddenItem ->
                        if (hiddenItem.isInput) {
                            var text by remember { mutableStateOf("") }
                            AzTextBox(
                                value = text,
                                onValueChange = { text = it },
                                hint = hiddenItem.hint ?: hiddenItem.text,
                                onSubmit = {
                                    showHiddenMenu = false
                                }
                            )
                        } else {
                            Text(
                                text = hiddenItem.text,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (hiddenItem.route != null && navController != null) navController.navigate(hiddenItem.route)
                                    showHiddenMenu = false
                                }.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
