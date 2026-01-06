package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
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
                                        RelocItemHandler.updateOrder(scope.navItems, draggedItemId!!, currentDropTargetIndex!!)
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
                            onHiddenMenuDismiss = { hiddenMenuOpenId = null }
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
                                                        RelocItemHandler.updateOrder(scope.navItems, draggedItemId!!, currentDropTargetIndex!!)
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
                                            onHiddenMenuDismiss = { hiddenMenuOpenId = null }
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
    onHiddenMenuDismiss: () -> Unit
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
    val alpha = if (isDragging) 0f else 1f

    val dragModifier = if (item.isRelocItem && !infoScreen) {
        Modifier.pointerInput(item.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var menuJob: Job? = null
                var isMenuOpen = false

                menuJob = coroutineScope.launch {
                    delay(500)
                    isMenuOpen = true
                    onMenuOpen(item.id)
                }

                var totalDragY = 0f

                try {
                    drag(down.id) { change ->
                        if (isMenuOpen) {
                            change.consume()
                            return@drag
                        }

                        val dragY = (change.position - change.previousPosition).y
                        totalDragY += dragY

                        if (Math.abs(totalDragY) > 10 && menuJob?.isActive == true) {
                            menuJob?.cancel()
                            onDragStart(item.id)
                        }

                        if (draggedItemId == item.id) {
                            change.consume()
                            onDragDelta(dragY)

                            // Calculate Target Index
                            val currentIdx = scope.navItems.indexOfFirst { it.id == item.id }
                            if (currentIdx != -1) {
                                val myHeight = itemHeights[item.id] ?: 0
                                // Calculate absolute drag distance relative to start
                                // dragOffset is cumulative.
                                // We need to determine how many "slots" we moved.
                                // Assuming uniform height for RelocItems is safest, but we have `itemHeights`.

                                // Simple approximation: distance / height
                                if (myHeight > 0) {
                                    val slotsMoved = (dragOffset / myHeight).roundToInt()
                                    var target = currentIdx + slotsMoved

                                    // Clamp to cluster
                                    val cluster = RelocItemHandler.findCluster(scope.navItems, item.id)
                                    if (cluster != null) {
                                        target = target.coerceIn(cluster)
                                        if (target != currentDropTargetIndex) {
                                            onDragTargetChange(target)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    menuJob?.cancel()
                    if (draggedItemId == item.id) {
                        onDragEnd()
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

    Box(modifier = Modifier.zIndex(if (isDragging) 1f else 0f)) {
         Box(modifier = Modifier
             .offset(y = animatedOffsetY)
             .alpha(alpha)
         ) {
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
                 infoScreen = infoScreen,
                 dragModifier = dragModifier
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
                     isSelected = item.route == currentDestination,
                     buttonSize = buttonSize,
                     onClick = null,
                     onRailCyclerClick = {},
                     onItemClick = {},
                     infoScreen = infoScreen
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
