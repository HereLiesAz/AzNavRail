// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailItems.kt
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.isSpecified
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration

/**
 * Renders the full ordered set of rail buttons in the collapsed rail, including nested-rail popups
 * and hidden-menu popups for relocatable items.
 *
 * Each item is wrapped in [DraggableRailItemWrapper] which attaches the long-press-drag gesture for
 * relocatable items, manages the visual drag shadow, and handles the snap-back animation on drop.
 * Sub-items belonging to host items are rendered inline when their host is expanded.
 *
 * @param items All items in the scope (including sub-items and cyclers).
 * @param scope The active [AzNavRailScopeImpl] providing callbacks and state.
 * @param navController Used for route-based navigation on item click.
 * @param currentDestination The active route; used to determine selection state.
 * @param buttonSize Uniform button size (shrunk when a vertical nested rail is open).
 * @param onRailCyclerClick Invoked when a cycler item is tapped in the rail.
 * @param onItemSelected Invoked after any item is selected (e.g., to collapse the menu).
 * @param hostStates Mutable map tracking which host items are expanded.
 * @param packRailButtons If true, no vertical spacing is added between items.
 * @param visualDockingSide Used to position nested-rail popups on the correct side.
 * @param onClickOverride Optional override that replaces the default click handling (used for tutorial mode).
 * @param onItemGloballyPositioned Reports bounds to the scope's cache for help/tutorial overlays.
 * @param helpEnabled When true, non-host/non-help items are visually dimmed and non-interactive.
 * @param rotationDegrees Rotation to apply to buttons "in place".
 */
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
    visualDockingSide: AzDockingSide,
    onClickOverride: ((AzNavItem) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    helpEnabled: Boolean = false,
    rotationDegrees: Float = 0f,
    isRailOpen: Boolean = true,
    railItemsCount: Int = items.size
) {
    val density = LocalDensity.current
    val topLevelItems = items.filter { !it.isSubItem }
    val itemsToRender =
        if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var itemHeights by remember { mutableStateOf(mapOf<String, Int>()) }
    var itemWidths by remember { mutableStateOf(mapOf<String, Int>()) }
    var hiddenMenuOpenId by remember { mutableStateOf<String?>(null) }
    var currentDropTargetIndex by remember { mutableStateOf<Int?>(null) }




    val snappingOffsets = remember { androidx.compose.runtime.mutableStateMapOf<String, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }
    var lastTappedId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val baseButtonSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth
    val sizeRatio = buttonSize / baseButtonSize
    val spacingDp = if (packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement * sizeRatio
    val spacingPx = with(density) { spacingDp.roundToPx() }

    val prefixSums by remember(scope.navItems, spacingPx) {
        derivedStateOf {
            val sums = IntArray(scope.navItems.size + 1)
            var currentSum = 0
            for (i in scope.navItems.indices) {
                sums[i] = currentSum
                currentSum += (itemHeights[scope.navItems[i].id] ?: 0) + spacingPx
            }
            sums[scope.navItems.size] = currentSum
            sums
        }
    }

    // Shared drag handlers, hoisted once so every nesting depth uses the same implementation.
    val onDragStart: (String) -> Unit = { id ->
        draggedItemId = id
        currentDropTargetIndex = scope.navItems.indexOfFirst { it.id == id }
    }
    val onDragEnd: () -> Unit = {
        if (draggedItemId != null && currentDropTargetIndex != null) {
            val currentIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }
            if (currentIdx != -1 && currentDropTargetIndex != -1 && currentIdx != currentDropTargetIndex) {
                var movedDistance = 0
                if (currentDropTargetIndex!! > currentIdx) {
                    movedDistance = prefixSums[currentDropTargetIndex!! + 1] - prefixSums[currentIdx + 1]
                } else if (currentDropTargetIndex!! < currentIdx) {
                    movedDistance = -(prefixSums[currentIdx] - prefixSums[currentDropTargetIndex!!])
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
                // Persist the per-host reloc order so the new arrangement survives recomposition.
                val draggedHostId = scope.navItems.firstOrNull { it.id == draggedItemId }?.hostId
                if (draggedHostId != null) {
                    scope.savedRelocOrders[draggedHostId] = scope.navItems
                        .filter { it.isRelocItem && it.hostId == draggedHostId }
                        .map { it.id }
                }
                scope.onRelocateMap[draggedItemId!!]?.invoke(currentIdx, currentDropTargetIndex!!, currentOrder)
            }
        }
        draggedItemId = null
        dragOffset = 0f
        currentDropTargetIndex = null
    }
    val onDragDelta: (Float) -> Unit = { delta -> dragOffset += delta }
    val onDragTargetChange: (Int) -> Unit = { index -> currentDropTargetIndex = index }
    val onMenuOpen: (String) -> Unit = { id -> hiddenMenuOpenId = id }

    // Any change to the *visible* item set — a host auto-expands via `expandWhen` (the
    // AnimatedVisibility below reveals sub-items and shifts everything beneath it), a reorder, or
    // items added/removed — must abort an in-flight drag/snap. Otherwise a leftover additive
    // `Modifier.offset` (snap-back or drag reflow) keeps an item drawn at its OLD slot while its real
    // slot has moved, rendering it on top of a neighbour (two labels overlap). Keyed on the item ids
    // AND the set of expanded hosts, because expansion changes the visible layout without changing
    // `scope.navItems` itself.
    val expandedHostKey = hostStates.filterValues { it }.keys.sorted().joinToString(",")
    val structuralKey = scope.navItems.joinToString(",") { it.id } + "|" + expandedHostKey
    LaunchedEffect(structuralKey) {
        if (snappingOffsets.isNotEmpty()) snappingOffsets.clear()
        if (draggedItemId != null) {
            draggedItemId = null
            dragOffset = 0f
            currentDropTargetIndex = null
        }
    }

    val onHeightReported: (String, Int) -> Unit = { id, height -> itemHeights = itemHeights + (id to height) }
    val onWidthReported: (String, Int) -> Unit = { id, width -> itemWidths = itemWidths + (id to width) }
    val onHiddenMenuDismiss: () -> Unit = { hiddenMenuOpenId = null }
    val onUpdateLastTappedId: (String) -> Unit = { id -> lastTappedId = id }

    Box(modifier = Modifier.pointerInput(Unit) {
        detectTapGestures(onTap = {
            if (scope.nestedRailOpenId != null) {
                val openNestedItem = scope.navItems.find { it.id == scope.nestedRailOpenId }
                if (openNestedItem?.keepNestedRailOpen != true) {
                    scope.nestedRailOpenId = null
                }
            }
        })
    }) {
        Column {
            itemsToRender.forEachIndexed { index, item ->
                key(item.id) {
                    if (item.isRailItem) {
                        RailItemNode(
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
                            helpEnabled = helpEnabled,
                            draggedItemId = draggedItemId,
                            dragOffset = dragOffset,
                            currentDropTargetIndex = currentDropTargetIndex,
                            onDragStart = onDragStart,
                            onDragEnd = onDragEnd,
                            onDragDelta = onDragDelta,
                            onDragTargetChange = onDragTargetChange,
                            onMenuOpen = onMenuOpen,
                            itemHeights = itemHeights,
                            onHeightReported = onHeightReported,
                            itemWidths = itemWidths,
                            onWidthReported = onWidthReported,
                            coroutineScope = coroutineScope,
                            hiddenMenuOpenId = hiddenMenuOpenId,
                            onHiddenMenuDismiss = onHiddenMenuDismiss,
                            lastTappedId = lastTappedId,
                            onUpdateLastTappedId = onUpdateLastTappedId,
                            snappingOffsets = snappingOffsets,
                            visualDockingSide = visualDockingSide,
                            rotationDegrees = rotationDegrees,
                            index = index,
                            count = railItemsCount,
                            isRailOpen = isRailOpen
                        )
                    } else {
                        Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
                    }
                }
            }
        }
    }
}

/**
 * Renders a single rail item plus, when it is an expanded host, its sub-items nested beneath it.
 *
 * Because a sub-item may itself be a host (declared via `azRailSubHostItem`), this composable calls
 * itself recursively for each child, so hosts can nest to any depth. Opening a sub-host reveals its
 * children inline while its sibling sub-items stay visible (accordion behavior at every level).
 */
@Composable
private fun RailItemNode(
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
    helpEnabled: Boolean,
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
    snappingOffsets: Map<String, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>,
    visualDockingSide: AzDockingSide,
    rotationDegrees: Float = 0f,
    index: Int = 0,
    count: Int = 0,
    isRailOpen: Boolean = true
) {
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
        helpEnabled = helpEnabled,
        draggedItemId = draggedItemId,
        dragOffset = dragOffset,
        currentDropTargetIndex = currentDropTargetIndex,
        onDragStart = onDragStart,
        onDragEnd = onDragEnd,
        onDragDelta = onDragDelta,
        onDragTargetChange = onDragTargetChange,
        onMenuOpen = onMenuOpen,
        itemHeights = itemHeights,
        onHeightReported = onHeightReported,
        itemWidths = itemWidths,
        onWidthReported = onWidthReported,
        coroutineScope = coroutineScope,
        hiddenMenuOpenId = hiddenMenuOpenId,
        onHiddenMenuDismiss = onHiddenMenuDismiss,
        lastTappedId = lastTappedId,
        onUpdateLastTappedId = onUpdateLastTappedId,
        snappingOffset = snappingOffsets[item.id]?.value,
        visualDockingSide = visualDockingSide,
        nestedRailOpenId = scope.nestedRailOpenId,
        onNestedRailToggle = { scope.nestedRailOpenId = it },
        rotationDegrees = rotationDegrees,
        index = index,
        count = count,
        isRailOpen = isRailOpen
    )

    AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
        Column {
            val subItems = scope.navItems.filter { it.hostId == item.id && it.isRailItem }
            subItems.forEach { subItem ->
                key(subItem.id) {
                    RailItemNode(
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
                        helpEnabled = helpEnabled,
                        draggedItemId = draggedItemId,
                        dragOffset = dragOffset,
                        currentDropTargetIndex = currentDropTargetIndex,
                        onDragStart = onDragStart,
                        onDragEnd = onDragEnd,
                        onDragDelta = onDragDelta,
                        onDragTargetChange = onDragTargetChange,
                        onMenuOpen = onMenuOpen,
                        itemHeights = itemHeights,
                        onHeightReported = onHeightReported,
                        itemWidths = itemWidths,
                        onWidthReported = onWidthReported,
                        coroutineScope = coroutineScope,
                        hiddenMenuOpenId = hiddenMenuOpenId,
                        onHiddenMenuDismiss = onHiddenMenuDismiss,
                        lastTappedId = lastTappedId,
                        onUpdateLastTappedId = onUpdateLastTappedId,
                        snappingOffsets = snappingOffsets,
                        visualDockingSide = visualDockingSide,
                        rotationDegrees = rotationDegrees,
                        index = index,
                        count = count,
                        isRailOpen = isRailOpen
                    )
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
    helpEnabled: Boolean,
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
    snappingOffset: Float?,
    visualDockingSide: AzDockingSide,
    nestedRailOpenId: String?,
    onNestedRailToggle: (String?) -> Unit,
    rotationDegrees: Float = 0f,
    index: Int = 0,
    count: Int = 0,
    isRailOpen: Boolean = true
) {
    val isDragging = draggedItemId == item.id
    var visualOffsetY by remember { mutableStateOf(0.dp) }

    androidx.compose.runtime.LaunchedEffect(item.forceHiddenMenuOpen) {
        if (item.forceHiddenMenuOpen) {
            onMenuOpen(item.id)
        }
    }
    val isRightDocked = visualDockingSide == AzDockingSide.RIGHT

    val myHeightPx = itemHeights[item.id] ?: 0
    val density = LocalDensity.current
    val myHeightDp = with(density) { myHeightPx.toDp() }

    val itemHeightsState = rememberUpdatedState(itemHeights)

    if (draggedItemId != null && !isDragging && item.isRelocItem && currentDropTargetIndex != null) {
        val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
        val draggedStartIdx = scope.navItems.indexOfFirst { it.id == draggedItemId }

        if (currentIdx != -1 && draggedStartIdx != -1) {
            val draggedItem = scope.navItems[draggedStartIdx]
            if (item.hostId == draggedItem.hostId) {
                if (draggedStartIdx < currentDropTargetIndex) {
                    if (currentIdx > draggedStartIdx && currentIdx <= currentDropTargetIndex) {
                        visualOffsetY = -myHeightDp
                    } else {
                        visualOffsetY = 0.dp
                    }
                } else if (draggedStartIdx > currentDropTargetIndex) {
                    if (currentIdx >= currentDropTargetIndex && currentIdx < draggedStartIdx) {
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
    val viewConfiguration = LocalViewConfiguration.current
    val nestedRailOpenIdState = rememberUpdatedState(nestedRailOpenId)

    val dragModifier = if (item.isRelocItem && !helpEnabled) {
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
                    scope.onFocusMap[item.id]?.invoke()
                    onMenuOpen(item.id)
                    // onDragStart(item.id) -- Deferred until movement
                }

                var totalDragY = 0f
                var hasMoved = false // Moved before long press
                var hasDragged = false // Moved after long press
                var dragStarted = false // Officially started dragging
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
                                    longPressJob.cancel()
                                }
                            } else {
                                change.consume()

                                if (!dragStarted) {
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        dragStarted = true
                                        onHiddenMenuDismiss()
                                        onDragStart(item.id)
                                    }
                                }

                                if (dragStarted) {
                                    val dragY = (change.position - currentPosition).y
                                    totalDragY += dragY
                                    onDragDelta(dragY)
                                    hasDragged = true

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
                        }
                        currentPosition = change.position
                    }
                } finally {
                    longPressJob.cancel()
                    if (isLongPress) {
                        if (dragStarted) {
                            onDragEnd()
                            scope.advancedConfig.onInteraction?.invoke(item.id, item)
                        }
                    } else if (!hasMoved && gestureCompletedSuccessfully) {
                        val isRouteSelected = item.route != null && item.route == currentDestination
                        val isIdSelected = lastTappedId == item.id

                        scope.onFocusMap[item.id]?.invoke()

                        onUpdateLastTappedId(item.id)
                        if (onClickOverride != null) {
                            onClickOverride(item)
                        } else {
                            if (item.isHelpItem) {
                                // Explicitly toggle help overlay if it's a help item, even in helpEnabled mode
                                onItemSelected(item)
                            } else {
                                if (item.isNestedRail) {
                                    onNestedRailToggle(if (nestedRailOpenIdState.value == item.id) null else item.id)
                                    scope.onClickMap[item.id]?.invoke()
                                } else {
                                    scope.onClickMap[item.id]?.invoke()
                                    item.route?.let { navController?.navigate(it) }
                                    onItemSelected(item)
                                }
                            }
                        }
                        scope.advancedConfig.onInteraction?.invoke(item.id, item)
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

    val isClassifierActive = item.classifiers.any { scope.activeClassifiers.contains(it) }
    val isVisuallyActive = isSelected || isClassifierActive

    val accordionModifier = rememberAzAccordionModifier(
        index = index,
        count = count,
        visible = isRailOpen,
        isHorizontal = false,
        staggerMs = scope.entranceStaggerMs,
        durationMs = scope.entranceDurationMs,
        baseRotationZ = rotationDegrees
    )

    Box(modifier = Modifier
        .then(accordionModifier)
        .zIndex(if (isDragging) 1f else 0f)
    ) {
        Box(modifier = Modifier
            .offset(y = finalOffsetY)
            .alpha(alpha)
        ) {
            if (item.isRelocItem) {
                RailContent(
                                    defaultShape = scope.defaultShape,
                                    item = item,
                    navController = null,
                    isSelected = isVisuallyActive,
                    buttonSize = buttonSize,
                    onClick = {},
                    onRailCyclerClick = {},
                    onItemClick = {},
                    onHostClick = {},
                    onItemGloballyPositioned = onItemGloballyPositioned,
                    onBoundsCalculated = { id, bounds -> scope.itemBoundsCache[id] = bounds },
                    helpEnabled = helpEnabled,
                    dragModifier = dragModifier,
                    activeColor = scope.activeColor,
                    rotationDegrees = rotationDegrees
                )
            } else {
                RailContent(
                                    defaultShape = scope.defaultShape,
                                    item = item,
                    navController = navController,
                    isSelected = isVisuallyActive,
                    buttonSize = buttonSize,
                    onClick = {
                        scope.onFocusMap[item.id]?.invoke()
                        if (item.isNestedRail) {
                            onNestedRailToggle(if (scope.nestedRailOpenId == item.id) null else item.id)
                            scope.onClickMap[item.id]?.invoke()
                        } else {
                            if (scope.nestedRailOpenId != null) {
                                val openItem = scope.navItems.find { it.id == scope.nestedRailOpenId }
                                if (openItem?.keepNestedRailOpen != true) {
                                    onNestedRailToggle(null)
                                }
                            }
                            if (onClickOverride != null) {
                                onClickOverride(item)
                            } else {
                                // Help items are toggled exclusively via onItemClick → onItemSelected
                                // below — invoking it here too would fire the toggle twice per tap and
                                // cancel itself out, leaving the help overlay invisible.
                                if (!item.isHelpItem) {
                                    scope.onClickMap[item.id]?.invoke()
                                }
                            }
                        }
                        scope.advancedConfig.onInteraction?.invoke(item.id, item)
                    },
                    onRailCyclerClick = onRailCyclerClick,
                    onItemClick = { onItemSelected(item) },
                    onHostClick = {
                        val newHostState = !(hostStates[item.id] ?: false)
                        hostStates[item.id] = newHostState
                        scope.onExpandedChangeMap[item.id]?.invoke(newHostState)
                    },
                    onItemGloballyPositioned = onItemGloballyPositioned,
                            onBoundsCalculated = { id, bounds -> scope.itemBoundsCache[id] = bounds },
                            onBoundsCleared = { id -> scope.itemBoundsCache.remove(id) },
                    helpEnabled = helpEnabled,
                    dragModifier = dragModifier,
                    activeColor = scope.activeColor,
                    rotationDegrees = rotationDegrees
                )
            }
        }

        if (scope.nestedRailOpenId == item.id && item.isNestedRail) {
            val anchorWidthPx = itemWidths[item.id] ?: 0
            if (item.nestedRailAlignment == AzNestedRailAlignment.VERTICAL) {
                Popup(
                    popupPositionProvider = DockedCenteredPopupPositionProvider(isRightDocked, anchorWidthPx),
                    onDismissRequest = { onNestedRailToggle(null) },
                    properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false)
                ) {
                    NestedRail(
                        parentItem = item,
                        items = item.nestedRailItems ?: emptyList(),
                        currentDestination = currentDestination,
                        activeColor = scope.activeColor,
                        activeClassifiers = scope.activeClassifiers,
                        onItemSelected = { subItem ->
                            scope.onClickMap[subItem.id]?.invoke()
                            subItem.route?.let { navController?.navigate(it) }
                            onItemSelected(subItem)
                            scope.advancedConfig.onInteraction?.invoke(subItem.id, subItem)
                            if (!item.keepNestedRailOpen) {
                                onNestedRailToggle(null)
                            }
                        },
                        alignment = item.nestedRailAlignment,
                        isRightDocked = isRightDocked,
                        helpList = scope.advancedConfig.helpList,
                        onItemGloballyPositioned = { id, bounds ->
                            // Rect.Zero is the sentinel emitted by NestedItemWrapper's onDispose —
                            // remove the cached entry so the help overlay won't draw a card or line
                            // for a now-invisible nested item.
                            if (bounds == Rect.Zero) scope.itemBoundsCache.remove(id)
                            else scope.itemBoundsCache[id] = bounds
                        },
                        rotationDegrees = rotationDegrees,
                        onHostExpandedChange = { id, expanded ->
                            scope.onExpandedChangeMap[id]?.invoke(expanded)
                        }
                    )
                }
            } else {
                val marginPx = with(density) { 8.dp.roundToPx() }
                Popup(
                    popupPositionProvider = DockedHorizontalPopupPositionProvider(isRightDocked, marginPx),
                    onDismissRequest = { onNestedRailToggle(null) },
                    properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false)
                ) {
                    NestedRail(
                        parentItem = item,
                        items = item.nestedRailItems ?: emptyList(),
                        currentDestination = currentDestination,
                        activeColor = scope.activeColor,
                        activeClassifiers = scope.activeClassifiers,
                        onItemSelected = { subItem ->
                            scope.onClickMap[subItem.id]?.invoke()
                            subItem.route?.let { navController?.navigate(it) }
                            onItemSelected(subItem)
                            scope.advancedConfig.onInteraction?.invoke(subItem.id, subItem)
                            if (!item.keepNestedRailOpen) {
                                onNestedRailToggle(null)
                            }
                        },
                        alignment = item.nestedRailAlignment ?: AzNestedRailAlignment.HORIZONTAL,
                        isRightDocked = isRightDocked,
                        helpList = scope.advancedConfig.helpList,
                        onItemGloballyPositioned = { id, bounds ->
                            // Rect.Zero is the sentinel emitted by NestedItemWrapper's onDispose —
                            // remove the cached entry so the help overlay won't draw a card or line
                            // for a now-invisible nested item.
                            if (bounds == Rect.Zero) scope.itemBoundsCache.remove(id)
                            else scope.itemBoundsCache[id] = bounds
                        },
                        rotationDegrees = rotationDegrees,
                        onHostExpandedChange = { id, expanded ->
                            scope.onExpandedChangeMap[id]?.invoke(expanded)
                        }
                    )
                }
            }
        }

        if (hiddenMenuOpenId == item.id && !item.hiddenMenuItems.isNullOrEmpty()) {
            HiddenMenuPopup(
                items = item.hiddenMenuItems,
                onDismiss = {
                    item.onHiddenMenuDismiss?.invoke()
                    onHiddenMenuDismiss()
                },
                onItemClick = { menuItem ->
                    scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                    menuItem.route?.let { navController?.navigate(it) }
                    item.onHiddenMenuDismiss?.invoke()
                    onHiddenMenuDismiss()
                },
                onInputSubmit = { menuItem, value ->
                    scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                    item.onHiddenMenuDismiss?.invoke()
                    onHiddenMenuDismiss()
                },
                backgroundColor = if (scope.translucentBackground != androidx.compose.ui.graphics.Color.Unspecified) scope.translucentBackground else AzTextBoxDefaults.getBackgroundColor(),
                backgroundOpacity = AzTextBoxDefaults.getBackgroundOpacity(),
                anchorWidth = itemWidths[item.id] ?: 0
            )
        }

        if (isDragging) {
            Box(modifier = Modifier.offset { IntOffset(0, dragOffset.roundToInt()) }) {
                RailContent(
                                    defaultShape = scope.defaultShape,
                                    item = item,
                    navController = navController,
                    isSelected = isSelected,
                    buttonSize = buttonSize,
                    onClick = null,
                    onRailCyclerClick = {},
                    onItemClick = {},
                    helpEnabled = helpEnabled,
                    activeColor = scope.activeColor,
                    rotationDegrees = rotationDegrees
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
    // The hidden menu sizes itself to its content: input boxes are given an
    // explicit width so the text fields (not the popup) dictate the menu width.
    val menuItemWidth = 250.dp

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
                .width(IntrinsicSize.Max)
                .background(effectiveBg.copy(alpha = backgroundOpacity))
                .border(1.dp, MaterialTheme.colorScheme.primary)
                .padding(8.dp)
        ) {
            items.forEach { menuItem ->
                if (menuItem.isInput) {
                    var text by remember { mutableStateOf(menuItem.initialValue) }
                    com.hereliesaz.aznavrail.AzTextBox(
                        modifier = Modifier.padding(8.dp).width(menuItemWidth),
                        hint = menuItem.hint ?: "",
                        value = text,
                        onValueChange = { text = it },
                        onSubmit = { value -> onInputSubmit(menuItem, value) },
                        submitButtonContent = {
                            Icon(
                                imageVector = AzIcons.Check,
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
                            .fillMaxWidth()
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
