package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlinx.coroutines.launch

@Composable
internal fun ExpandedRailContent(
    scope: AzNavRailScopeImpl,
    displayedNavItems: List<AzNavItem>,
    navController: NavController?,
    currentDestination: String?,
    cyclerStates: MutableMap<String, CyclerTransientState>,
    hostStates: MutableMap<String, Boolean>,
    itemPositions: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    isRightDocked: Boolean,
    appName: String,
    footerColor: androidx.compose.ui.graphics.Color,
    onToggle: () -> Unit,
    onUndock: () -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onItemSelected: (AzNavItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .pointerInput(true) {
                detectHorizontalDragGestures(
                    onDragStart = { },
                    onHorizontalDrag = { change, dragAmount ->
                        val shouldClose = if (isRightDocked) dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX
                        if (shouldClose) {
                            onToggle()
                            change.consume()
                        }
                    }
                )
            }
    ) {
        val itemsToShow = displayedNavItems.filter { !it.isSubItem }
        itemsToShow.forEach { item ->
            if (item.isDivider) {
                com.hereliesaz.aznavrail.AzDivider()
            } else {
                val onClick = scope.onClickMap[item.id]
                val onCyclerClick = if (item.isCycler) {
                    {
                        val state = cyclerStates[item.id]
                        if (state != null && !item.disabled) {
                            state.job?.cancel()
                            val options = requireNotNull(item.options)
                            val disabledOptions = item.disabledOptions ?: emptyList()
                            val enabledOptions = options.filterNot { it in disabledOptions }
                            if (enabledOptions.isNotEmpty()) {
                                val currentDisplayed = state.displayedOption
                                val currentIndexInEnabled = enabledOptions.indexOf(currentDisplayed)
                                val nextIndex = if (currentIndexInEnabled != -1) (currentIndexInEnabled + 1) % enabledOptions.size else 0
                                val nextOption = enabledOptions[nextIndex]
                                cyclerStates[item.id] = state.copy(
                                    displayedOption = nextOption,
                                    job = coroutineScope.launch {
                                        kotlinx.coroutines.delay(1000L)
                                        val finalItemState = displayedNavItems.find { it.id == item.id } ?: item
                                        val currentStateInVm = finalItemState.selectedOption
                                        val targetState = nextOption
                                        val currentIndexInVm = options.indexOf(currentStateInVm)
                                        val targetIndex = options.indexOf(targetState)
                                        if (currentIndexInVm != -1 && targetIndex != -1) {
                                            val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                            if (onClick != null) repeat(clicksToCatchUp) { onClick() }
                                        }
                                        onToggle()
                                        cyclerStates[item.id] = cyclerStates[item.id]!!.copy(job = null)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    onClick
                }
                val finalItem = if (item.isCycler) {
                    item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption)
                } else if (item.isHost) {
                    item.copy(isExpanded = hostStates[item.id] ?: false)
                } else {
                    item
                }
                MenuItem(
                    item = finalItem,
                    navController = navController,
                    isSelected = finalItem.route == currentDestination,
                    onClick = onClick,
                    onCyclerClick = onCyclerClick,
                    onToggle = onToggle,
                    onItemClick = { onItemSelected(finalItem) },
                    onHostClick = {
                        val wasExpanded = hostStates[item.id] ?: false
                        val keys = hostStates.keys.toList()
                        keys.forEach { key -> hostStates[key] = false }
                        hostStates[item.id] = !wasExpanded
                    },
                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                    infoScreen = scope.infoScreen,
                    activeColor = scope.activeColor
                )
                AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                    Column {
                        val subItems = displayedNavItems.filter { it.hostId == item.id }
                        subItems.forEach { subItem ->
                            MenuItem(
                                item = subItem,
                                navController = navController,
                                isSelected = subItem.route == currentDestination,
                                onClick = scope.onClickMap[subItem.id],
                                onCyclerClick = null,
                                onToggle = onToggle,
                                onItemClick = { onItemSelected(subItem) },
                                onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                infoScreen = scope.infoScreen,
                                activeColor = scope.activeColor
                            )
                        }
                    }
                }
            }
        }
        if (scope.showFooter) {
            Footer(
                appName = appName,
                onToggle = onToggle,
                onUndock = onUndock,
                scope = scope,
                footerColor = footerColor
            )
        }
    }
}

@Composable
internal fun CollapsedRailContent(
    scope: AzNavRailScopeImpl,
    displayedNavItems: List<AzNavItem>,
    navController: NavController?,
    currentDestination: String?,
    cyclerStates: MutableMap<String, CyclerTransientState>,
    hostStates: MutableMap<String, Boolean>,
    itemPositions: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    buttonSize: Dp,
    orientation: AzOrientation,
    visualSide: AzVisualSide,
    isRightDocked: Boolean,
    disableSwipeToOpen: Boolean,
    isFloating: Boolean,
    showFloatingButtons: Boolean,
    onHeightChanged: (Int) -> Unit,
    onOpen: () -> Unit,
    onClickOverride: ((AzNavItem) -> Unit)?,
    onItemSelected: (AzNavItem) -> Unit,
    reverseLayout: Boolean
) {
    AnimatedVisibility(visible = !isFloating || showFloatingButtons, modifier = Modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        val isVertical = orientation == AzOrientation.Vertical
        val adaptiveModifier = Modifier
            .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
            .then(if(isVertical) Modifier.verticalScroll(scrollState) else Modifier.horizontalScroll(scrollState))
            .onSizeChanged { onHeightChanged(it.height) }
            .pointerInput(disableSwipeToOpen, isVertical, isRightDocked) {
                if (isVertical) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onHorizontalDrag = { change, dragAmount ->
                            val shouldOpen = if (isRightDocked) dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX
                            if (!disableSwipeToOpen && shouldOpen) {
                                onOpen()
                                change.consume()
                            }
                        }
                    )
                } else {
                    detectVerticalDragGestures(
                        onDragStart = { },
                        onVerticalDrag = { change, dragAmount ->
                            // Infer top/bottom swipe
                            val isBottom = isRightDocked // Simplified assumption for horizontal
                            val shouldOpen = if (isBottom) dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX
                            if (!disableSwipeToOpen && shouldOpen) {
                                onOpen()
                                change.consume()
                            }
                        }
                    )
                }
            }

        val onRailCyclerClick: (AzNavItem) -> Unit = { item ->
            val state = cyclerStates[item.id]
            if (state != null) {
                val options = requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                val disabledOptions = item.disabledOptions ?: emptyList()
                val enabledOptions = options.filterNot { it in disabledOptions }
                if (enabledOptions.isNotEmpty()) {
                    val currentDisplayed = item.selectedOption
                    val currentIndexInEnabled = enabledOptions.indexOf(currentDisplayed)
                    val nextIndex = if (currentIndexInEnabled != -1) (currentIndexInEnabled + 1) % enabledOptions.size else 0
                    val nextOption = enabledOptions[nextIndex]
                    val finalItemState = displayedNavItems.find { it.id == item.id } ?: item
                    val currentStateInVm = finalItemState.selectedOption
                    val targetState = nextOption
                    val currentIndexInVm = options.indexOf(currentStateInVm)
                    val targetIndex = options.indexOf(targetState)
                    if (currentIndexInVm != -1 && targetIndex != -1) {
                        val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                        val onClick = scope.onClickMap[item.id]
                        if (onClick != null) repeat(clicksToCatchUp) { onClick() }
                    }
                }
            }
        }

        if (isVertical) {
            Column(
                modifier = adaptiveModifier,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RailItems(
                    items = displayedNavItems,
                    scope = scope,
                    navController = navController,
                    currentDestination = currentDestination,
                    buttonSize = buttonSize,
                    onRailCyclerClick = onRailCyclerClick,
                    onItemSelected = onItemSelected,
                    hostStates = hostStates,
                    packRailButtons = if (isFloating) true else scope.packButtons,
                    onClickOverride = onClickOverride,
                    onItemGloballyPositioned = { id, rect ->
                        itemPositions[id] = rect
                        scope.onItemGloballyPositioned?.invoke(id, rect)
                    },
                    infoScreen = scope.infoScreen,
                    orientation = orientation,
                    visualSide = visualSide,
                    reverseLayout = reverseLayout
                )
            }
        } else {
            Row(
                modifier = adaptiveModifier,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                    if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RailItems(
                    items = displayedNavItems,
                    scope = scope,
                    navController = navController,
                    currentDestination = currentDestination,
                    buttonSize = buttonSize,
                    onRailCyclerClick = onRailCyclerClick,
                    onItemSelected = onItemSelected,
                    hostStates = hostStates,
                    packRailButtons = if (isFloating) true else scope.packButtons,
                    onClickOverride = onClickOverride,
                    onItemGloballyPositioned = { id, rect ->
                        itemPositions[id] = rect
                        scope.onItemGloballyPositioned?.invoke(id, rect)
                    },
                    infoScreen = scope.infoScreen,
                    orientation = orientation,
                    visualSide = visualSide,
                    reverseLayout = reverseLayout
                )
            }
        }
    }
}
