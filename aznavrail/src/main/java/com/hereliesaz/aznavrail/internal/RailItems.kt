package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown

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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val topLevelItems = items.filter { !it.isSubItem }
    val itemsToRender =
        if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems

    // Shared state for dragging
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var itemHeights by remember { mutableStateOf(mapOf<String, Int>()) }
    var itemWidths by remember { mutableStateOf(mapOf<String, Int>()) }
    var hiddenMenuOpenId by remember { mutableStateOf<String?>(null) }
    var currentDropTargetIndex by remember { mutableStateOf<Int?>(null) }

    // State for snapping animations
    val snappingOffsets = remember { androidx.compose.runtime.mutableStateMapOf<String, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }

    // Track last tapped item for double-tap logic (backward compatibility)
    var lastTappedId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Box {
        Column {
            itemsToRender.forEach { item ->
                key(item.id) {
                    if (item.isRailItem) {
                        // Render the item (or wrap it)
                        DraggableRailItemWrapper(
                            item = item,
                            scope = scope,
                            navController = navController,
                            currentDestination = currentDestination,
                            buttonSize = buttonSize,
                            onRailCyclerClick = onRailCyclerClick,
                            onItemSelected = onItemSelected,
                            hostStates = hostStates,
                            onClickOverride = onClickOverride,
                            onItemGloballyPositioned = onItemGloballyPositioned,
                            infoScreen = infoScreen,
                            draggedItemId = draggedItemId,
                            dragOffset = dragOffset,
                            currentDropTargetIndex = currentDropTargetIndex,
                            onDragStart = { id ->
                                draggedItemId = id
                                currentDropTargetIndex = scope.navItems.indexOfFirst { it.id == id }
                            },
                            onDragEnd = {
                                // Commit the move on drop
                                if (draggedItemId != null && currentDropTargetIndex != null) {
                                    val currentIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }
                                    if (currentIdx != -1 && currentDropTargetIndex != -1 && currentIdx != currentDropTargetIndex) {
                                        // Calculate snap offset
                                        // Total distance item moved physically = Sum of heights of intervening items + spacing
                                        // Current visual offset = dragOffset
                                        // To be seamless at new position, initial offset at new pos = dragOffset - distance moved

                                        val spacingDp = if (packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                                        val spacingPx = with(density) { spacingDp.roundToPx() }

                                        var movedDistance = 0
                                        if (currentDropTargetIndex!! > currentIdx) {
                                            // Moving Down: Sum items from currentIdx + 1 to target
                                            for (i in (currentIdx + 1)..currentDropTargetIndex!!) {
                                                movedDistance += (itemHeights[scope.navItems[i].id] ?: 0) + spacingPx
                                            }
                                        } else if (currentDropTargetIndex!! < currentIdx) {
                                            // Moving Up: Sum items from target to currentIdx - 1
                                            for (i in currentDropTargetIndex!! until currentIdx) {
                                                movedDistance -= ((itemHeights[scope.navItems[i].id] ?: 0) + spacingPx)
                                            }
                                        }

                                        val startSnapOffset = dragOffset - movedDistance

                                        val animatable = Animatable(startSnapOffset, Float.VectorConverter)
                                        snappingOffsets[draggedItemId!!] = animatable

                                        val capturedId = draggedItemId!!
                                        coroutineScope.launch {
                                            animatable.animateTo(0f)
                                            snappingOffsets.remove(capturedId)
                                        }

                                        RelocItemHandler.updateOrder(scope.navItems, draggedItemId!!, currentDropTargetIndex!!)
                                        val currentOrder = scope.navItems.map { it.id }
                                        scope.onRelocateMap[draggedItemId!!]?.invoke(currentIdx, currentDropTargetIndex!!, currentOrder)
                                    }
                                }
                                // Reset state
                                draggedItemId = null
                                dragOffset = 0f
                                currentDropTargetIndex = null
                            },
                            onDragDelta = { delta -> dragOffset += delta },
                            onDragTargetChange = { index -> currentDropTargetIndex = index },
                            onMenuOpen = { id -> hiddenMenuOpenId = id },
                            itemHeights = itemHeights,
                            onHeightReported = { id, height -> itemHeights = itemHeights + (id to height) },
                            itemWidths = itemWidths,
                            onWidthReported = { id, width -> itemWidths = itemWidths + (id to width) },
                            coroutineScope = coroutineScope,
                            hiddenMenuOpenId = hiddenMenuOpenId,
                            onHiddenMenuDismiss = { hiddenMenuOpenId = null },
                            lastTappedId = lastTappedId,
                            onUpdateLastTappedId = { id -> lastTappedId = id },
                            snappingOffset = snappingOffsets[item.id]?.value
                        )

                        AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                            Column {
                                val subItems = scope.navItems.filter { it.hostId == item.id && it.isRailItem }
                                subItems.forEach { subItem ->
                                    key(subItem.id) {
                                        DraggableRailItemWrapper(
                                            item = subItem,
                                            scope = scope,
                                            navController = navController,
                                            currentDestination = currentDestination,
                                            buttonSize = buttonSize,
                                            onRailCyclerClick = onRailCyclerClick,
                                            onItemSelected = onItemSelected,
                                            hostStates = hostStates,
                                            onClickOverride = onClickOverride,
                                            onItemGloballyPositioned = onItemGloballyPositioned,
                                            infoScreen = infoScreen,
                                            draggedItemId = draggedItemId,
                                            dragOffset = dragOffset,
                                            currentDropTargetIndex = currentDropTargetIndex,
                                            onDragStart = { id ->
                                                draggedItemId = id
                                                currentDropTargetIndex = scope.navItems.indexOfFirst { it.id == id }
                                            },
                                            onDragEnd = {
                                                if (draggedItemId != null && currentDropTargetIndex != null) {
                                                    val currentIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }
                                                    if (currentIdx != -1 && currentDropTargetIndex != -1 && currentIdx != currentDropTargetIndex) {

                                                        val spacingDp = if (packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                                                        val spacingPx = with(density) { spacingDp.roundToPx() }

                                                        var movedDistance = 0
                                                        if (currentDropTargetIndex!! > currentIdx) {
                                                            // Moving Down: Sum items from currentIdx + 1 to target
                                                            for (i in (currentIdx + 1)..currentDropTargetIndex!!) {
                                                                movedDistance += (itemHeights[scope.navItems[i].id] ?: 0) + spacingPx
                                                            }
                                                        } else if (currentDropTargetIndex!! < currentIdx) {
                                                            // Moving Up: Sum items from target to currentIdx - 1
                                                            for (i in currentDropTargetIndex!! until currentIdx) {
                                                                movedDistance -= ((itemHeights[scope.navItems[i].id] ?: 0) + spacingPx)
                                                            }
                                                        }

                                                        val startSnapOffset = dragOffset - movedDistance

                                                        val animatable = Animatable(startSnapOffset, Float.VectorConverter)
                                                        snappingOffsets[draggedItemId!!] = animatable

                                                        val capturedId = draggedItemId!!
                                                        coroutineScope.launch {
                                                            animatable.animateTo(0f)
                                                            snappingOffsets.remove(capturedId)
                                                        }

                                                        RelocItemHandler.updateOrder(scope.navItems, draggedItemId!!, currentDropTargetIndex!!)
                                                        val currentOrder = scope.navItems.map { it.id }
                                                        scope.onRelocateMap[draggedItemId!!]?.invoke(currentIdx, currentDropTargetIndex!!, currentOrder)
                                                    }
                                                }
                                                draggedItemId = null
                                                dragOffset = 0f
                                                currentDropTargetIndex = null
                                            },
                                            onDragDelta = { delta -> dragOffset += delta },
                                            onDragTargetChange = { index -> currentDropTargetIndex = index },
                                            onMenuOpen = { id -> hiddenMenuOpenId = id },
                                            itemHeights = itemHeights,
                                            onHeightReported = { id, height -> itemHeights = itemHeights + (id to height) },
                                            itemWidths = itemWidths,
                                            onWidthReported = { id, width -> itemWidths = itemWidths + (id to width) },
                                            coroutineScope = coroutineScope,
                                            hiddenMenuOpenId = hiddenMenuOpenId,
                                            onHiddenMenuDismiss = { hiddenMenuOpenId = null },
                                            lastTappedId = lastTappedId,
                                            onUpdateLastTappedId = { id -> lastTappedId = id },
                                            snappingOffset = snappingOffsets[subItem.id]?.value
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableRailItemWrapper(
    item: AzNavItem,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: MutableMap<String, Boolean>,
    onClickOverride: ((AzNavItem) -> Unit)?,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    draggedItemId: String?,
    dragOffset: Float,
    currentDropTargetIndex: Int?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragTargetChange: (Int) -> Unit,
    onMenuOpen: (String) -> Unit,
    itemHeights: Map<String, Int>,
    onHeightReported: (String, Int) -> Unit,
    itemWidths: Map<String, Int>,
    onWidthReported: (String, Int) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    hiddenMenuOpenId: String?,
    onHiddenMenuDismiss: () -> Unit,
    lastTappedId: String?,
    onUpdateLastTappedId: (String) -> Unit,
    snappingOffset: Float?
) {
    val isDragging = draggedItemId == item.id
    var visualOffsetY by remember { mutableStateOf(0.dp) }

    val myHeightPx = itemHeights[item.id] ?: 0
    val density = androidx.compose.ui.platform.LocalDensity.current
    val myHeightDp = with(density) { myHeightPx.toDp() }

    val itemHeightsState = androidx.compose.runtime.rememberUpdatedState(itemHeights)

    if (draggedItemId != null && !isDragging && item.isRelocItem && currentDropTargetIndex != null) {
        val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
        val draggedStartIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }

        if (currentIdx != -1 && draggedStartIdx != -1) {
            val draggedItem = scope.navItems[draggedStartIdx]
            if (item.hostId == draggedItem.hostId) {
                if (draggedStartIdx < currentDropTargetIndex!!) {
                     if (currentIdx > draggedStartIdx && currentIdx <= currentDropTargetIndex!!) {
                         visualOffsetY = -myHeightDp
                     } else {
                         visualOffsetY = 0.dp
                     }
                } else if (draggedStartIdx > currentDropTargetIndex!!) {
                     if (currentIdx >= currentDropTargetIndex!! && currentIdx < draggedStartIdx) {
                         visualOffsetY = myHeightDp
                     } else {
                         visualOffsetY = 0.dp
                     }
                } else {
                    visualOffsetY = 0.dp
                }
            } else {
                visualOffsetY = 0.dp
            }
        } else {
             visualOffsetY = 0.dp
        }
    } else {
        visualOffsetY = 0.dp
    }

    val animatedOffsetY by animateDpAsState(targetValue = visualOffsetY)
    val alpha = if (isDragging) 0f else 1f
    val finalOffsetY = if (snappingOffset != null) {
        with(density) { snappingOffset.toDp() }
    } else {
        animatedOffsetY
    }

    val hapticFeedback = LocalHapticFeedback.current
    val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current

    val dragModifier = if (item.isRelocItem && !infoScreen) {
        Modifier.pointerInput(item.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                var longPressJob: Job? = null
                var isLongPress = false

                longPressJob = coroutineScope.launch {
                    delay(longPressTimeout)
                    isLongPress = true
                    if (scope.vibrate) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onDragStart(item.id)
                }

                var totalDragY = 0f
                var hasMoved = false
                var hasDragged = false
                var gestureCompletedSuccessfully = false

                try {
                    var pointerId = down.id
                    var currentPosition = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null) break

                        val changedToUp = !change.pressed && change.previousPressed
                        if (changedToUp) {
                            change.consume()
                            gestureCompletedSuccessfully = true
                            break
                        }

                        val positionChange = change.position - change.previousPosition
                        if (positionChange != androidx.compose.ui.geometry.Offset.Zero) {
                            if (!isLongPress) {
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    hasMoved = true
                                    longPressJob?.cancel()
                                }
                            } else {
                                change.consume()
                                val dragY = (change.position - currentPosition).y
                                totalDragY += dragY
                                onDragDelta(dragY)

                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    hasDragged = true
                                }

                                val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
                                if (currentIdx != -1) {
                                    val target = RelocItemHandler.calculateTargetIndex(
                                        items = scope.navItems,
                                        draggedItemId = item.id,
                                        currentDragOffset = totalDragY,
                                        itemHeights = itemHeightsState.value
                                    )
                                    if (target != null && target != currentDropTargetIndex) {
                                        onDragTargetChange(target)
                                    }
                                }
                            }
                        }
                        currentPosition = change.position
                    }
                } finally {
                    longPressJob?.cancel()
                    if (isLongPress) {
                        onDragEnd()
                        if (!hasDragged) {
                            scope.onFocusMap[item.id]?.invoke()
                            onMenuOpen(item.id)
                        }
                    } else if (!hasMoved && gestureCompletedSuccessfully) {
                        val isRouteSelected = item.route != null && item.route == currentDestination
                        val isIdSelected = lastTappedId == item.id

                        scope.onFocusMap[item.id]?.invoke()

                        if (isRouteSelected || isIdSelected) {
                             onMenuOpen(item.id)
                             onUpdateLastTappedId(item.id)
                        } else {
                             onUpdateLastTappedId(item.id)
                             if (onClickOverride != null) {
                                 onClickOverride(item)
                             } else {
                                 scope.onClickMap[item.id]?.invoke()
                                 item.route?.let { navController?.navigate(it) }
                                 onItemSelected(item)
                             }
                        }
                    }
                }
            }
        }
        .onGloballyPositioned { coordinates ->
             onHeightReported(item.id, coordinates.size.height)
             onWidthReported(item.id, coordinates.size.width)
        }
    } else {
        Modifier
    }

    val isSelected = if (item.route != null) {
        item.route == currentDestination
    } else {
        lastTappedId == item.id
    }

    val isClassifierActive = item.classifiers.any { it in scope.activeClassifiers }
    val isVisuallyActive = isSelected || isClassifierActive

    Box(modifier = Modifier.zIndex(if (isDragging) 1f else 0f)) {
         Box(modifier = Modifier
             .offset(y = finalOffsetY)
             .alpha(alpha)
         ) {
             if (item.isRelocItem) {
                 RailContent(
                     item = item,
                     navController = null,
                     isSelected = isVisuallyActive,
                     buttonSize = buttonSize,
                     onClick = {},
                     onRailCyclerClick = {},
                     onItemClick = {},
                     onHostClick = {},
                     onItemGloballyPositioned = onItemGloballyPositioned,
                     infoScreen = infoScreen,
                     dragModifier = dragModifier,
                     activeColor = scope.activeColor
                 )
             } else {
                 RailContent(
                     item = item,
                     navController = navController,
                     isSelected = isVisuallyActive,
                     buttonSize = buttonSize,
                     onClick = {
                         scope.onFocusMap[item.id]?.invoke()
                         if (onClickOverride != null) {
                             onClickOverride(item)
                         } else {
                             scope.onClickMap[item.id]?.invoke()
                         }
                     },
                     onRailCyclerClick = onRailCyclerClick,
                     onItemClick = { onItemSelected(item) },
                     onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
                     onItemGloballyPositioned = onItemGloballyPositioned,
                     infoScreen = infoScreen,
                     dragModifier = dragModifier,
                     activeColor = scope.activeColor
                 )
             }
         }

         if (hiddenMenuOpenId == item.id && !item.hiddenMenuItems.isNullOrEmpty()) {
             HiddenMenuPopup(
                 items = item.hiddenMenuItems!!,
                 onDismiss = onHiddenMenuDismiss,
                 onItemClick = { menuItem ->
                      scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                      menuItem.route?.let { navController?.navigate(it) }
                      onHiddenMenuDismiss()
                 },
                 onInputSubmit = { menuItem, value ->
                      scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                      onHiddenMenuDismiss()
                 },
                 backgroundColor = AzTextBoxDefaults.getBackgroundColor(),
                 backgroundOpacity = AzTextBoxDefaults.getBackgroundOpacity(),
                 anchorWidth = itemWidths[item.id] ?: 0
             )
         }

         if (isDragging) {
             Box(modifier = Modifier.offset { IntOffset(0, dragOffset.roundToInt()) }) {
                 RailContent(
                     item = item,
                     navController = navController,
                     isSelected = isSelected,
                     buttonSize = buttonSize,
                     onClick = null,
                     onRailCyclerClick = {},
                     onItemClick = {},
                     infoScreen = infoScreen,
                     activeColor = scope.activeColor
                 )
             }
         }
    }
}

@Composable
private fun HiddenMenuPopup(
    items: List<com.hereliesaz.aznavrail.model.HiddenMenuItem>,
    onDismiss: () -> Unit,
    onItemClick: (com.hereliesaz.aznavrail.model.HiddenMenuItem) -> Unit,
    onInputSubmit: (com.hereliesaz.aznavrail.model.HiddenMenuItem, String) -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color,
    backgroundOpacity: Float,
    anchorWidth: Int
) {
    Popup(
        alignment = androidx.compose.ui.Alignment.TopStart,
        offset = IntOffset(x = anchorWidth, y = 0),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
         val surfaceColor = MaterialTheme.colorScheme.surface
         val effectiveBg = if (backgroundColor == androidx.compose.ui.graphics.Color.Transparent) surfaceColor else backgroundColor

         Column(
            modifier = Modifier
                .background(effectiveBg.copy(alpha = 0.95f))
                .border(1.dp, MaterialTheme.colorScheme.primary)
                .padding(8.dp)
        ) {
            items.forEach { menuItem ->
                if (menuItem.isInput) {
                    var text by remember { mutableStateOf("") }
                    com.hereliesaz.aznavrail.AzTextBox(
                        modifier = Modifier.padding(8.dp),
                        hint = menuItem.hint ?: "",
                        value = text,
                        onValueChange = { text = it },
                        onSubmit = { value -> onInputSubmit(menuItem, value) },
                        submitButtonContent = {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Submit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        outlineColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = menuItem.text,
                        modifier = Modifier
                            .androidx.compose.foundation.clickable { onItemClick(menuItem) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
