// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailItems.kt
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.isSpecified
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
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
    orientation: com.hereliesaz.aznavrail.model.AzOrientation = com.hereliesaz.aznavrail.model.AzOrientation.Vertical,
    isRailOpen: Boolean = true,
    railItemsCount: Int = items.size
) {
    val isHorizontal = orientation == com.hereliesaz.aznavrail.model.AzOrientation.Horizontal
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

    // Listen for the "tap the rail to dismiss the open nested rail" gesture only while there IS an
    // open nested rail. Installed unconditionally it sat over every rail button for the whole life
    // of the rail, taking taps the app might otherwise have received.
    val dismissNestedRailOnTap = if (scope.nestedRailOpenId == null) Modifier else {
        Modifier.pointerInput(scope.nestedRailOpenId) {
            detectTapGestures(onTap = {
                val openNestedItem = scope.navItems.find { it.id == scope.nestedRailOpenId }
                if (openNestedItem?.keepNestedRailOpen != true) {
                    scope.nestedRailOpenId = null
                }
            })
        }
    }

    Box(modifier = dismissNestedRailOnTap) {
        if (isHorizontal) {
            Row {
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
                                orientation = orientation,
                                index = index,
                                count = railItemsCount,
                                isRailOpen = isRailOpen
                            )
                        } else {
                            Spacer(modifier = Modifier.width(AzNavRailDefaults.RailContentSpacerHeight))
                        }
                    }
                }
            }
        } else {
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
                                orientation = orientation,
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
    orientation: com.hereliesaz.aznavrail.model.AzOrientation = com.hereliesaz.aznavrail.model.AzOrientation.Vertical,
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
        orientation = orientation,
        index = index,
        count = count,
        isRailOpen = isRailOpen
    )

    AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
        val subContent = @Composable {
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
                        orientation = orientation,
                        index = index,
                        count = count,
                        isRailOpen = isRailOpen
                    )
                }
            }
        }
        if (orientation == com.hereliesaz.aznavrail.model.AzOrientation.Horizontal) Row { subContent() }
        else Column { subContent() }
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
    orientation: com.hereliesaz.aznavrail.model.AzOrientation = com.hereliesaz.aznavrail.model.AzOrientation.Vertical,
    index: Int = 0,
    count: Int = 0,
    isRailOpen: Boolean = true
) {
    val isHorizontal = orientation == com.hereliesaz.aznavrail.model.AzOrientation.Horizontal
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
    val lastTappedIdState = rememberUpdatedState(lastTappedId)

    val dragModifier = if (item.isRelocItem && !helpEnabled) {
        Modifier
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                    // An item the user already selected with a previous tap (`lastTappedId`, the same
                    // state that drives its "last tapped" highlight) skips the hold-then-drag contract
                    // entirely: this press is a tap-and-drag — movement past touch slop starts
                    // reordering immediately, with no dwell time. This is what keeps the hidden menu's
                    // long-press and the reorder-drag from racing each other on the SAME press: on a
                    // not-yet-selected item only the long-press timer can ever open the menu or arm a
                    // drag (unchanged below); the immediate-drag fast path only ever applies to a press
                    // that lands on the item the user just selected.
                    val preSelected = lastTappedIdState.value == item.id

                    var longPressJob: Job? = null
                    var isLongPress = false

                    longPressJob = coroutineScope.launch {
                        delay(longPressTimeout)
                        isLongPress = true
                        if (scope.vibrate) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        scope.onFocusMap[item.id]?.invoke()
                        if (!item.hiddenMenuItems.isNullOrEmpty()) {
                            onMenuOpen(item.id)
                        }
                        // onDragStart(item.id) -- Deferred until movement
                    }

                    var totalDragY = 0f
                    var hasMoved = false // Moved before long press, on a not-yet-selected item
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
                                val distance = (change.position - down.position).getDistance()
                                if (!dragStarted) {
                                    if (preSelected) {
                                        if (distance > viewConfiguration.touchSlop) {
                                            longPressJob.cancel()
                                            change.consume()
                                            dragStarted = true
                                            onHiddenMenuDismiss()
                                            onDragStart(item.id)
                                        }
                                    } else if (!isLongPress) {
                                        if (distance > viewConfiguration.touchSlop) {
                                            hasMoved = true
                                            longPressJob.cancel()
                                        }
                                    } else {
                                        change.consume()
                                        if (distance > viewConfiguration.touchSlop) {
                                            dragStarted = true
                                            onHiddenMenuDismiss()
                                            onDragStart(item.id)
                                        }
                                    }
                                }

                                if (dragStarted) {
                                    change.consume()
                                    val dragY = (change.position - currentPosition).y
                                    totalDragY += dragY
                                    onDragDelta(dragY)

                                    val currentIdx =
                                        scope.navItems.indexOfFirst { it.id == item.id }
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
                        longPressJob.cancel()
                        if (dragStarted) {
                            onDragEnd()
                            scope.advancedConfig.onInteraction?.invoke(item.id, item)
                        } else if (
                            gestureCompletedSuccessfully && !hasMoved &&
                            (!isLongPress || item.hiddenMenuItems.isNullOrEmpty())
                        ) {
                            // Reaches here two ways: an ordinary quick tap (`!isLongPress`), or a
                            // press held past the long-press threshold that never turned into a drag
                            // and had no hidden menu to show for it (`item.hiddenMenuItems` empty) —
                            // from the user's perspective the latter is still just a slow tap, not a
                            // gesture that should be silently discarded. Only a press that legitimately
                            // opened a hidden menu (checked above) is left alone here, since that menu
                            // is now the interaction the user is looking at.
                            val isRouteSelected =
                                item.route != null && item.route == currentDestination
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

    // The two highlights are two different questions about this item, answered separately.
    // "Where you are": the item's route IS the current destination.
    val isRouteActive = item.route != null && item.route == currentDestination
    // A routeless item (toggle, cycler, action) that was last tapped stays visually active so the
    // user knows which one they interacted with. This is the pre-existing behaviour.
    val isLastTapped = item.route == null && lastTappedId == item.id
    val isSelected = isRouteActive || isLastTapped

    val isClassifierActive = item.classifiers.any { scope.activeClassifiers.contains(it) }
    val isVisuallyActive = isRouteActive || isClassifierActive || isLastTapped
    // "Whatever the app says": never set by the library, only by azItemState / secondaryClassifiers.
    val isSecondaryActive = item.isSecondaryActive ||
        item.classifiers.any { scope.secondaryClassifiers.contains(it) }
    val isTertiaryActive = item.isTertiaryActive ||
        item.classifiers.any { scope.tertiaryClassifiers.contains(it) }

    val accordionModifier = rememberAzAccordionModifier(
        index = index,
        count = count,
        visible = isRailOpen,
        isHorizontal = isHorizontal,
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
                    activeColor = scope.railAccent,
                    isFocused = false,
                    isSecondaryActive = isSecondaryActive,
                    isTertiaryActive = isTertiaryActive,
                    focusColor = scope.focusColor,
                    secondaryColor = scope.secondaryColor,
                    tertiaryColor = scope.tertiaryColor,
                    rotationDegrees = rotationDegrees,
                    onSliderChange = { id, v -> scope.onSliderChangeMap[id]?.invoke(v) },
                    onSliderRangeChange = { id, r -> scope.onSliderRangeChangeMap[id]?.invoke(r) },
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
                            if (item.reflectSelectionInParent) {
                                // A plain tap fires the SELECTED CHILD's own action directly — exactly
                                // as if the user had tapped it inside the (closed) popup — instead of
                                // opening the popup. Long-press (wired below) opens the popup instead.
                                val selectedChild = item.resolveReflectedChild()
                                if (selectedChild != null) {
                                    scope.onClickMap[selectedChild.id]?.invoke()
                                    selectedChild.route?.let { navController?.navigate(it) }
                                    onItemSelected(selectedChild)
                                    scope.advancedConfig.onInteraction?.invoke(selectedChild.id, selectedChild)
                                }
                            } else {
                                onNestedRailToggle(if (scope.nestedRailOpenId == item.id) null else item.id)
                                scope.onClickMap[item.id]?.invoke()
                                scope.advancedConfig.onInteraction?.invoke(item.id, item)
                            }
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
                            scope.advancedConfig.onInteraction?.invoke(item.id, item)
                        }
                    },
                    onLongClick = if (item.isNestedRail && item.reflectSelectionInParent) {
                        {
                            // Same toggle a plain tap performs today when `reflectSelectionInParent`
                            // is false — long-press is simply where that behaviour moved to.
                            scope.onFocusMap[item.id]?.invoke()
                            onNestedRailToggle(if (scope.nestedRailOpenId == item.id) null else item.id)
                        }
                    } else if (!item.hiddenMenuItems.isNullOrEmpty()) {
                        // Mirrors the reloc item's own long-press-opens-menu contract (and
                        // NestedItemWrapper's identical handling for nested-rail children) — a
                        // hidden menu is not a reloc-only affordance, any rail item can carry one.
                        {
                            scope.onFocusMap[item.id]?.invoke()
                            onMenuOpen(item.id)
                        }
                    } else null,
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
                    activeColor = scope.railAccent,
                    isFocused = false,
                    isSecondaryActive = isSecondaryActive,
                    isTertiaryActive = isTertiaryActive,
                    focusColor = scope.focusColor,
                    secondaryColor = scope.secondaryColor,
                    tertiaryColor = scope.tertiaryColor,
                    rotationDegrees = rotationDegrees,
                    onSliderChange = { id, v -> scope.onSliderChangeMap[id]?.invoke(v) },
                    onSliderRangeChange = { id, r -> scope.onSliderRangeChangeMap[id]?.invoke(r) },
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
                        activeColor = scope.railAccent,
                        focusColor = scope.focusColor,
                        secondaryColor = scope.secondaryColor,
                        tertiaryColor = scope.tertiaryColor,
                        activeClassifiers = scope.activeClassifiers,
                        secondaryClassifiers = scope.secondaryClassifiers,
                        tertiaryClassifiers = scope.tertiaryClassifiers,
                        onItemSelected = { subItem ->
                            // Record the tapped (non-host — NestedItemWrapper only calls
                            // onItemSelected for non-host items) child as the new selection so the
                            // parent's own displayed text/content updates on the next render. Written
                            // to the scope's survive-`reset()` map, not the item directly, since the
                            // DSL rebuilds `item` from scratch every recomposition.
                            if (item.reflectSelectionInParent) {
                                scope.selectedNestedChildMap[item.id] = subItem.id
                            }
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
                        },
                        itemSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth,
                        hiddenMenuOpenId = hiddenMenuOpenId,
                        onMenuOpen = onMenuOpen,
                        onHiddenMenuDismiss = onHiddenMenuDismiss,
                        onHiddenMenuItemClick = { menuItem ->
                            scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                            menuItem.route?.let { navController?.navigate(it) }
                        },
                        onHiddenMenuInputSubmit = { menuItem, value ->
                            scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                        },
                        hiddenMenuBackgroundColor = scope.translucentBackground,
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
                        activeColor = scope.railAccent,
                        focusColor = scope.focusColor,
                        secondaryColor = scope.secondaryColor,
                        tertiaryColor = scope.tertiaryColor,
                        activeClassifiers = scope.activeClassifiers,
                        secondaryClassifiers = scope.secondaryClassifiers,
                        tertiaryClassifiers = scope.tertiaryClassifiers,
                        onItemSelected = { subItem ->
                            // Record the tapped (non-host — NestedItemWrapper only calls
                            // onItemSelected for non-host items) child as the new selection so the
                            // parent's own displayed text/content updates on the next render. Written
                            // to the scope's survive-`reset()` map, not the item directly, since the
                            // DSL rebuilds `item` from scratch every recomposition.
                            if (item.reflectSelectionInParent) {
                                scope.selectedNestedChildMap[item.id] = subItem.id
                            }
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
                        },
                        itemSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth,
                        hiddenMenuOpenId = hiddenMenuOpenId,
                        onMenuOpen = onMenuOpen,
                        onHiddenMenuDismiss = onHiddenMenuDismiss,
                        onHiddenMenuItemClick = { menuItem ->
                            scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                            menuItem.route?.let { navController?.navigate(it) }
                        },
                        onHiddenMenuInputSubmit = { menuItem, value ->
                            scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                        },
                        hiddenMenuBackgroundColor = scope.translucentBackground,
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
                anchorWidth = itemWidths[item.id] ?: 0,
                // Open beside the item that raised it — the popup layer is now the whole window, so
                // the vertical anchor has to be stated rather than inherited from the parent.
                anchorTop = scope.itemBoundsCache[item.id]?.top?.toInt() ?: 0,
                accent = scope.railAccent,
                railDockingSide = scope.dockingSide,
                railWidth = scope.collapsedWidth,
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
                    activeColor = scope.railAccent,
                    isFocused = false,
                    isSecondaryActive = isSecondaryActive,
                    isTertiaryActive = isTertiaryActive,
                    focusColor = scope.focusColor,
                    secondaryColor = scope.secondaryColor,
                    tertiaryColor = scope.tertiaryColor,
                    rotationDegrees = rotationDegrees,
                    onSliderChange = { id, v -> scope.onSliderChangeMap[id]?.invoke(v) },
                    onSliderRangeChange = { id, r -> scope.onSliderRangeChangeMap[id]?.invoke(r) },
                )
            }
        }
    }
}

@Composable
internal fun HiddenMenuPopup(
    items: List<com.hereliesaz.aznavrail.model.HiddenMenuItem>,
    onDismiss: () -> Unit,
    onItemClick: (com.hereliesaz.aznavrail.model.HiddenMenuItem) -> Unit,
    onInputSubmit: (com.hereliesaz.aznavrail.model.HiddenMenuItem, String) -> Unit,
    backgroundColor: androidx.compose.ui.graphics.Color,
    backgroundOpacity: Float,
    anchorWidth: Int,
    /** Window-space top of the item that raised the menu, so it opens beside that item. */
    anchorTop: Int = 0,
    /** The rail's accent, so the menu's window matches the rail it came out of. */
    accent: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    /** The docking side of the rail that raised this menu, so it can be kept clear of that rail. */
    railDockingSide: AzDockingSide = AzDockingSide.LEFT,
    /** The collapsed width of the rail that raised this menu, so its gutter can be built as an obstruction. */
    railWidth: Dp = 0.dp,
) {
    // The hidden menu sizes itself to its content: input boxes are given an
    // explicit width so the text fields (not the popup) dictate the menu width.
    val menuItemWidth = 250.dp

    // The menu is one of the library's windows, which is what gives it a grab bar, a fold control
    // and a close: a context menu with a text field in it is something the user may well need to
    // move off whatever they are typing about, and folding it beats dismissing and re-summoning it.
    val windowState = com.hereliesaz.aznavrail.rememberAzWindowState()

    // The rail's own strip is chrome this popup layer draws over, so the window is never allowed to
    // land — or be dragged — back underneath it, on open or afterward. Only the vertical rail cases
    // build a rect; a horizontal rail is rarer here and better left unconstrained than guessed wrong.
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val railObstruction = remember(railDockingSide, railWidth, containerSize, density) {
        val railWidthPx = with(density) { railWidth.toPx() }
        when (railDockingSide) {
            AzDockingSide.LEFT -> Rect(0f, 0f, railWidthPx, containerSize.height.toFloat())
            AzDockingSide.RIGHT -> Rect(
                containerSize.width - railWidthPx,
                0f,
                containerSize.width.toFloat(),
                containerSize.height.toFloat(),
            )
        }
    }

    // The menu sizes itself to its content, but never past what is actually left on screen: the
    // window's own chrome bar plus a margin, capped overall at 80% of the screen height. Past that
    // it scrolls internally instead of overflowing off-screen with items below the fold unreachable.
    val maxMenuHeight = with(density) {
        val chromeHeightPx = com.hereliesaz.aznavrail.AzWindowDefaults.ChromeHeight.toPx()
        ((containerSize.height * 0.8f) - chromeHeightPx).coerceAtLeast(0f).toDp()
    }

    Popup(
        popupPositionProvider = AzHiddenMenuPositionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false)
    ) {
        val surfaceColor = MaterialTheme.colorScheme.surface
        val effectiveBg = if (backgroundColor == androidx.compose.ui.graphics.Color.Transparent) surfaceColor else backgroundColor

        // The window floats in a full-screen layer so it can be dragged anywhere on the screen
        // rather than being trapped in a popup measured to its own content. Taps on the empty part
        // of that layer are what "outside" now means.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
        ) {
            com.hereliesaz.aznavrail.AzWindow(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopStart)
                    .absoluteOffset { IntOffset(anchorWidth, anchorTop) }
                    // Taps inside the window are the window's own business.
                    .pointerInput(Unit) { detectTapGestures { } },
                state = windowState,
                accent = accent,
                surfaceColor = effectiveBg.copy(alpha = backgroundOpacity),
                onDismiss = onDismiss,
                obstruction = railObstruction,
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .heightIn(max = maxMenuHeight)
                        .verticalScroll(rememberScrollState())
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
    }
}

/** Positions the hidden menu's [Popup] at the window origin; the window inside places itself. */
internal val AzHiddenMenuPositionProvider = object : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: androidx.compose.ui.unit.IntSize
    ): IntOffset = IntOffset(0, 0)
}
