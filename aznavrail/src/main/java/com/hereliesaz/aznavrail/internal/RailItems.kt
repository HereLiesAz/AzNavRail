package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.size
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
                                        // Total distance item moved physically = (targetIndex - originalIndex) * height
                                        // Current visual offset = dragOffset
                                        // To be seamless at new position, initial offset at new pos = dragOffset - distance moved

                                        val movedDistance = (itemHeights[draggedItemId!!] ?: 0) * (currentDropTargetIndex!! - currentIdx)
                                        val startSnapOffset = dragOffset - movedDistance

                                        val animatable = Animatable(startSnapOffset, Float.VectorConverter)
                                        snappingOffsets[draggedItemId!!] = animatable

                                        coroutineScope.launch {
                                            animatable.animateTo(0f)
                                            snappingOffsets.remove(draggedItemId!!)
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

                                                        val movedDistance = (itemHeights[draggedItemId!!] ?: 0) * (currentDropTargetIndex!! - currentIdx)
                                                        val startSnapOffset = dragOffset - movedDistance

                                                        val animatable = Animatable(startSnapOffset, Float.VectorConverter)
                                                        snappingOffsets[draggedItemId!!] = animatable

                                                        coroutineScope.launch {
                                                            animatable.animateTo(0f)
                                                            snappingOffsets.remove(draggedItemId!!)
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

    // Calculate Animation Offset
    // If I am NOT the dragged item, I might need to shift.
    // Logic:
    // val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
    // val draggedIdx = scope.navItems.indexOfFirst { it.id == draggedItemId } (This is stable because we don't swap list yet)
    // Actually we need the *original* dragged index. But draggedItemId is stable.

    var visualOffsetY by remember { mutableStateOf(0.dp) }

    val myHeightPx = itemHeights[item.id] ?: 0
    val density = androidx.compose.ui.platform.LocalDensity.current
    val myHeightDp = with(density) { myHeightPx.toDp() }

    if (draggedItemId != null && !isDragging && item.isRelocItem && currentDropTargetIndex != null) {
        val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
        val draggedStartIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }

        if (currentIdx != -1 && draggedStartIdx != -1) {
            // Check if in same cluster
            val draggedItem = scope.navItems[draggedStartIdx]
            if (item.hostId == draggedItem.hostId) {
                // If item is between draggedStart and target
                if (draggedStartIdx < currentDropTargetIndex!!) {
                     // Dragging DOWN. Items between start+1 and target should move UP (-height)
                     if (currentIdx > draggedStartIdx && currentIdx <= currentDropTargetIndex!!) {
                         visualOffsetY = -myHeightDp
                     } else {
                         visualOffsetY = 0.dp
                     }
                } else if (draggedStartIdx > currentDropTargetIndex!!) {
                     // Dragging UP. Items between target and start-1 should move DOWN (+height)
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

    // Ghost effect: Opacity 0 if dragging. But wait, if we use offset logic, the "Hole" stays at original position.
    // So the item at `draggedStartIdx` should be invisible.
    // Also handle snapping visibility
    val alpha = if (isDragging) 0f else 1f

    // If snapping, we override offset
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

                // Start long press timer
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
                var gestureCompletedSuccessfully = false

                try {
                    // We need to loop until the gesture is finished (up or cancelled)
                    // If we move before timeout -> Cancel long press (it's not a drag yet).
                    // If we timeout -> It's a drag.

                    // We use `drag` block only if we are dragging.
                    // But we are in `awaitEachGesture` which is low level.
                    // We can loop consuming events.

                    var pointerId = down.id
                    var currentPosition = down.position

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }

                        if (change == null) break // Pointer lost

                        val changedToUp = !change.pressed && change.previousPressed
                        if (changedToUp) {
                            // Released
                            change.consume()
                            gestureCompletedSuccessfully = true
                            break
                        }

                        val positionChange = change.position - change.previousPosition
                        if (positionChange != androidx.compose.ui.geometry.Offset.Zero) {
                            if (!isLongPress) {
                                // Moved before long press triggers
                                // If movement is significant, cancel long press and mark as moved
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    hasMoved = true
                                    longPressJob?.cancel()
                                }
                            } else {
                                // Dragging logic
                                change.consume()
                                val dragY = (change.position - currentPosition).y
                                totalDragY += dragY
                                onDragDelta(dragY)

                                // Logic for target index (same as before)
                                val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
                                if (currentIdx != -1) {
                                    val target = RelocItemHandler.calculateTargetIndex(
                                        items = scope.navItems,
                                        draggedItemId = item.id,
                                        currentDragOffset = dragOffset,
                                        itemHeights = itemHeights
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
                    } else if (!hasMoved && gestureCompletedSuccessfully) {
                        // It was a tap, and gestures completed successfully (not cancelled)

                        // Check if item is selected
                        // Either by route matching OR by lastTappedId (for items without route)
                        val isRouteSelected = item.route != null && item.route == currentDestination
                        val isIdSelected = lastTappedId == item.id

                        if (isRouteSelected || isIdSelected) {
                             // Already selected -> Open Hidden Menu
                             onMenuOpen(item.id)
                             onUpdateLastTappedId(item.id)
                        } else {
                             // Not selected -> Select (and onClick)
                             // We invoke the click handler which handles navigation and onItemSelected
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

    Box(modifier = Modifier.zIndex(if (isDragging) 1f else 0f)) {
         Box(modifier = Modifier
             .offset(y = finalOffsetY)
             .alpha(alpha)
         ) {
             RailContent(
                 item = item,
                 navController = navController,
                 isSelected = isSelected,
                 buttonSize = buttonSize,
                 onClick = if (onClickOverride != null) { { onClickOverride(item) } } else scope.onClickMap[item.id],
                 onRailCyclerClick = onRailCyclerClick,
                 onItemClick = { onItemSelected(item) },
                 onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
                 onItemGloballyPositioned = onItemGloballyPositioned,
                 infoScreen = infoScreen,
                 dragModifier = dragModifier,
                 activeColor = scope.activeColor
             )
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
             // Render dragging item with offset
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
        offset = IntOffset(x = anchorWidth, y = 0), // Appear to the right of the item
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
                    // Local state for the input
                    var text by remember { mutableStateOf("") }

                    com.hereliesaz.aznavrail.AzTextBox(
                        modifier = Modifier.padding(8.dp),
                        hint = menuItem.hint ?: "",
                        value = text,
                        onValueChange = { text = it },
                        onSubmit = { value -> onInputSubmit(menuItem, value) },
                        submitButtonContent = {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Submit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        outlineColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = menuItem.text,
                        modifier = Modifier
                            .clickable { onItemClick(menuItem) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
