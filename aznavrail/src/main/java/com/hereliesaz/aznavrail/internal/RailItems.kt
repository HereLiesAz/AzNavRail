package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration

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
    orientation: AzOrientation = AzOrientation.Vertical,
    visualSide: AzVisualSide,
    onClickOverride: ((AzNavItem) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    infoScreen: Boolean = false,
    reverseLayout: Boolean = false
) {
    val density = LocalDensity.current
    val topLevelItems = items.filter { !it.isSubItem }
    val baseItems = if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems
    val itemsToRender = if (reverseLayout) baseItems.reversed() else baseItems

    // Shared state for dragging
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var itemHeights by remember { mutableStateOf(mapOf<String, Int>()) }
    var itemWidths by remember { mutableStateOf(mapOf<String, Int>()) }
    var itemBounds by remember { mutableStateOf(mapOf<String, Rect>()) }
    var hiddenMenuOpenId by remember { mutableStateOf<String?>(null) }
    var nestedRailOpenId by remember { mutableStateOf<String?>(null) }
    var currentDropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }

    // State for snapping animations
    val snappingOffsets = remember { androidx.compose.runtime.mutableStateMapOf<String, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }

    // Track last tapped item for double-tap logic (backward compatibility)
    var lastTappedId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val isVertical = orientation == AzOrientation.Vertical

    // Determine which size map to use for calculations
    val currentItemSizes = if (isVertical) itemHeights else itemWidths

    val renderItem: @Composable (AzNavItem) -> Unit = { item ->
        key(item.id) {
            if (item.isRailItem) {
                // Render the item (or wrap it)
                DraggableRailItemWrapper(
                    item = item,
                    scope = scope,
                    navController = navController,
                    currentDestination = currentDestination,
                    buttonSize = buttonSize,
                    orientation = orientation,
                    visualSide = visualSide,
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
                                val spacingDp = if (packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                                val spacingPx = with(density) { spacingDp.roundToPx() }

                                var movedDistance = 0
                                if (currentDropTargetIndex!! > currentIdx) {
                                    // Moving Down/Right: Sum items from currentIdx + 1 to target
                                    for (i in (currentIdx + 1)..currentDropTargetIndex!!) {
                                        movedDistance += (currentItemSizes[scope.navItems[i].id] ?: 0) + spacingPx
                                    }
                                } else if (currentDropTargetIndex!! < currentIdx) {
                                    // Moving Up/Left: Sum items from target to currentIdx - 1
                                    for (i in currentDropTargetIndex!! until currentIdx) {
                                        movedDistance -= ((currentItemSizes[scope.navItems[i].id] ?: 0) + spacingPx)
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
                    itemSizes = currentItemSizes,
                    itemWidths = itemWidths,
                    onHeightReported = { id, height -> itemHeights = itemHeights + (id to height) },
                    onWidthReported = { id, width -> itemWidths = itemWidths + (id to width) },
                    onBoundsReported = { id, bounds -> itemBounds = itemBounds + (id to bounds) },
                    coroutineScope = coroutineScope,
                    hiddenMenuOpenId = hiddenMenuOpenId,
                    nestedRailOpenId = nestedRailOpenId,
                    onNestedRailToggle = { id -> nestedRailOpenId = if (nestedRailOpenId == id) null else id },
                    onHiddenMenuDismiss = { hiddenMenuOpenId = null },
                    lastTappedId = lastTappedId,
                    onUpdateLastTappedId = { id -> lastTappedId = id },
                    snappingOffset = snappingOffsets[item.id]?.value
                )

                AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                    val subContent: @Composable () -> Unit = {
                        val subItems = scope.navItems.filter { it.hostId == item.id && it.isRailItem }
                        val finalSubItems = if (reverseLayout) subItems.reversed() else subItems
                        finalSubItems.forEach { subItem ->
                            key(subItem.id) {
                                DraggableRailItemWrapper(
                                    item = subItem,
                                    scope = scope,
                                    navController = navController,
                                    currentDestination = currentDestination,
                                    buttonSize = buttonSize,
                                    orientation = orientation,
                                    visualSide = visualSide,
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
                                                    for (i in (currentIdx + 1)..currentDropTargetIndex!!) {
                                                        movedDistance += (currentItemSizes[scope.navItems[i].id] ?: 0) + spacingPx
                                                    }
                                                } else if (currentDropTargetIndex!! < currentIdx) {
                                                    for (i in currentDropTargetIndex!! until currentIdx) {
                                                        movedDistance -= ((currentItemSizes[scope.navItems[i].id] ?: 0) + spacingPx)
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
                                    itemSizes = currentItemSizes,
                                    itemWidths = itemWidths,
                                    onHeightReported = { id, height -> itemHeights = itemHeights + (id to height) },
                                    onWidthReported = { id, width -> itemWidths = itemWidths + (id to width) },
                                    onBoundsReported = { id, bounds -> itemBounds = itemBounds + (id to bounds) },
                                    coroutineScope = coroutineScope,
                                    hiddenMenuOpenId = hiddenMenuOpenId,
                                    nestedRailOpenId = nestedRailOpenId,
                                    onNestedRailToggle = { id -> nestedRailOpenId = if (nestedRailOpenId == id) null else id },
                                    onHiddenMenuDismiss = { hiddenMenuOpenId = null },
                                    lastTappedId = lastTappedId,
                                    onUpdateLastTappedId = { id -> lastTappedId = id },
                                    snappingOffset = snappingOffsets[subItem.id]?.value
                                )
                            }
                        }
                    }

                    if (isVertical) {
                        Column { subContent() }
                    } else {
                        Row { subContent() }
                    }
                }
            } else {
                if (isVertical) {
                    Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
                } else {
                    Spacer(modifier = Modifier.width(AzNavRailDefaults.RailContentSpacerHeight))
                }
            }
        }

        if (nestedRailOpenId != null) {
            rootBounds?.let { rb ->
                val item = items.find { it.id == nestedRailOpenId }
                val bounds = itemBounds[nestedRailOpenId]
                if (item != null && bounds != null && item.nestedRailItems != null && item.nestedRailAlignment != null) {
                     NestedRail(
                         parentItem = item,
                         items = item.nestedRailItems,
                         scope = scope,
                         navController = navController,
                         currentDestination = currentDestination,
                         anchorBounds = bounds,
                         rootBounds = rb,
                         onDismiss = { nestedRailOpenId = null },
                         isRightDocked = scope.dockingSide == AzDockingSide.RIGHT,
                         onItemSelected = onItemSelected
                     )
                }
            }
        }
    }

    Box {
        if (isVertical) {
            Column { itemsToRender.forEach { renderItem(it) } }
        } else {
            Row { itemsToRender.forEach { renderItem(it) } }
        }
    }

    Box {
        if (isVertical) {
            Column { itemsToRender.forEach { renderItem(it) } }
        } else {
            Row { itemsToRender.forEach { renderItem(it) } }
        }
    }

    Box {
        if (isVertical) {
            Column { itemsToRender.forEach { renderItem(it) } }
        } else {
            Row { itemsToRender.forEach { renderItem(it) } }
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
    orientation: AzOrientation,
    visualSide: AzVisualSide,
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
    itemSizes: Map<String, Int>,
    itemWidths: Map<String, Int>,
    onHeightReported: (String, Int) -> Unit,
    onWidthReported: (String, Int) -> Unit,
    onBoundsReported: (String, Rect) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    hiddenMenuOpenId: String?,
    nestedRailOpenId: String?,
    onNestedRailToggle: (String) -> Unit,
    onHiddenMenuDismiss: () -> Unit,
    lastTappedId: String?,
    onUpdateLastTappedId: (String) -> Unit,
    snappingOffset: Float?
) {
    val isVertical = orientation == AzOrientation.Vertical
    val isDragging = draggedItemId == item.id
    var visualOffset by remember { mutableStateOf(0.dp) }

    val mySizePx = itemSizes[item.id] ?: 0
    val density = LocalDensity.current
    val mySizeDp = with(density) { mySizePx.toDp() }

    val itemSizesState = rememberUpdatedState(itemSizes)

    if (draggedItemId != null && !isDragging && item.isRelocItem && currentDropTargetIndex != null) {
        val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
        val draggedStartIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }

        if (currentIdx != -1 && draggedStartIdx != -1) {
            val draggedItem = scope.navItems[draggedStartIdx]
            if (item.hostId == draggedItem.hostId) {
                if (draggedStartIdx < currentDropTargetIndex!!) {
                     if (currentIdx > draggedStartIdx && currentIdx <= currentDropTargetIndex!!) {
                         visualOffset = -mySizeDp
                     } else {
                         visualOffset = 0.dp
                     }
                } else if (draggedStartIdx > currentDropTargetIndex!!) {
                     if (currentIdx >= currentDropTargetIndex!! && currentIdx < draggedStartIdx) {
                         visualOffset = mySizeDp
                     } else {
                         visualOffset = 0.dp
                     }
                } else {
                    visualOffset = 0.dp
                }
            } else {
                visualOffset = 0.dp
            }
        } else {
             visualOffset = 0.dp
        }
    } else {
        visualOffset = 0.dp
    }

    val animatedOffset by animateDpAsState(targetValue = visualOffset)
    val alpha = if (isDragging) 0f else 1f
    val finalOffset = if (snappingOffset != null) {
        with(density) { snappingOffset.toDp() }
    } else {
        animatedOffset
    }

    val hapticFeedback = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current

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

                var totalDrag = 0f
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
                        if (positionChange != Offset.Zero) {
                            if (!isLongPress) {
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    hasMoved = true
                                    longPressJob?.cancel()
                                }
                            } else {
                                change.consume()
                                val dragDelta = if (isVertical) (change.position - currentPosition).y else (change.position - currentPosition).x
                                totalDrag += dragDelta
                                onDragDelta(dragDelta)

                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    hasDragged = true
                                }

                                val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
                                if (currentIdx != -1) {
                                    val target = RelocItemHandler.calculateTargetIndex(
                                        items = scope.navItems,
                                        draggedItemId = item.id,
                                        currentDragOffset = totalDrag,
                                        itemSizes = itemSizesState.value
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
             onBoundsReported(item.id, coordinates.boundsInWindow())
        }
    } else {
        Modifier.onGloballyPositioned { coordinates ->
             onBoundsReported(item.id, coordinates.boundsInWindow())
        }
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
             .offset(
                 x = if (!isVertical) finalOffset else 0.dp,
                 y = if (isVertical) finalOffset else 0.dp
             )
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
                         if (item.isNestedRail) {
                             onNestedRailToggle(item.id)
                         } else if (onClickOverride != null) {
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
                 visualSide = visualSide
             )
         }

         if (isDragging) {
             Box(modifier = Modifier.offset {
                 if (isVertical) {
                     IntOffset(0, dragOffset.roundToInt())
                 } else {
                     IntOffset(dragOffset.roundToInt(), 0)
                 }
             }) {
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
    visualSide: AzVisualSide
) {
    Popup(
        popupPositionProvider = RailMenuPositionProvider(visualSide),
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
                            Icon(
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
