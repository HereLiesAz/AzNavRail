package com.hereliesaz.aznavrail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.CenteredPopupPositionProvider
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

object AzNavRail {
    const val noTitle = "AZNAVRAIL_NO_TITLE"
}

@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val scope = remember { AzNavRailScopeImpl() }
    scope.navItems.clear()
    navController?.let { scope.navController = it }

    scope.apply(content)

    scope.navItems.forEach { item ->
        if (item.isSubItem && item.isRailItem) {
            val host = scope.navItems.find { it.id == item.hostId }
            require(host != null && host.isRailItem) {
                "A `azRailSubItem` can only be hosted by a `azRailHostItem`."
            }
        }
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = remember(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app name", e)
            "App" // Fallback name
        }
    }

    val appIcon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app icon", e)
            null // Fallback to default icon
        }
    }

    val initialExpansion = if (scope.bubbleMode) true else initiallyExpanded
    var isExpanded by rememberSaveable(initialExpansion) { mutableStateOf(initialExpansion) }
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isFloating by remember { mutableStateOf(false) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var wasVisibleOnDragStart by remember { mutableStateOf(false) }
    var isAppIcon by remember { mutableStateOf(!scope.displayAppNameInHeader) }
    var headerHeight by remember { mutableStateOf(0) }
    var railItemsHeight by remember { mutableStateOf(0) }

    val hapticFeedback = LocalHapticFeedback.current

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedRailWidth else scope.collapsedRailWidth,
        label = "railWidth"
    )

    val coroutineScope = rememberCoroutineScope()
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    var selectedItem by rememberSaveable { mutableStateOf<AzNavItem?>(null) }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(scope.navItems) {
        val initialSelectedItem = if (currentDestination != null) {
            scope.navItems.find { it.route == currentDestination }
        } else {
            scope.navItems.firstOrNull()
        }
        selectedItem = initialSelectedItem

        scope.navItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
            }
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            cyclerStates.forEach { (id, state) ->
                if (state.job != null) {
                    state.job.cancel()
                    val item = scope.navItems.find { it.id == id }
                    if (item != null) {
                        coroutineScope.launch {
                            val options = requireNotNull(item.options)
                            val currentStateInVm = item.selectedOption
                            val targetState = state.displayedOption

                            val currentIndexInVm = options.indexOf(currentStateInVm)
                            val targetIndex = options.indexOf(targetState)

                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp =
                                    (targetIndex - currentIndexInVm + options.size) % options.size
                                val onClick = scope.onClickMap[item.id]
                                if (onClick != null) {
                                    repeat(clicksToCatchUp) {
                                        onClick()
                                    }
                                }
                            }
                            cyclerStates[id] = state.copy(job = null)
                        }
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    LaunchedEffect(showFloatingButtons) {
        if (showFloatingButtons) {
            val screenHeightPx = with(density) { screenHeight.toPx() }
            val bottomBound = screenHeightPx * 0.9f
            val railBottom = railOffset.y + headerHeight + railItemsHeight
            if (railBottom > bottomBound) {
                val newY = bottomBound - headerHeight - railItemsHeight
                railOffset = IntOffset(railOffset.x, newY.roundToInt())
            }
        }
    }

    Box(modifier = modifier) {
        val buttonSize = AzNavRailDefaults.HeaderIconSize
        selectedItem?.screenTitle?.let { screenTitle ->
            if (screenTitle.isNotEmpty()) {
                Popup(alignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
        if (scope.isLoading) {
            Popup(
                popupPositionProvider = CenteredPopupPositionProvider,
                properties = PopupProperties(focusable = false)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AzLoad()
                }
            }
        }
        Box {
            NavigationRail(
                modifier = Modifier
                    .width(railWidth)
                    .offset { railOffset },
                containerColor = if (isExpanded) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(bottom = AzNavRailDefaults.HeaderPadding)
                            .onSizeChanged { headerHeight = it.height }
                            .pointerInput(
                                isFloating,
                                scope.enableRailDragging,
                                scope.displayAppNameInHeader
                            ) {
                                detectTapGestures(
                                    onTap = {
                                        if (isFloating) {
                                            showFloatingButtons = !showFloatingButtons
                                        } else {
                                            isExpanded = !isExpanded
                                        }
                                    },
                                    onLongPress = {
                                        if (isFloating) {
                                            // Long press in FAB mode -> dock
                                            railOffset = IntOffset.Zero
                                            isFloating = false
                                            if (scope.displayAppNameInHeader) isAppIcon = false
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (scope.enableRailDragging) {
                                            // Long press in docked mode -> FAB
                                            isFloating = true
                                            isExpanded = false
                                            if (scope.displayAppNameInHeader) isAppIcon = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            }
                            .pointerInput(isFloating, scope.enableRailDragging) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        if (isFloating) {
                                            wasVisibleOnDragStart = showFloatingButtons
                                            showFloatingButtons = false
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (isFloating) {
                                            change.consume()
                                            val newY = railOffset.y + dragAmount.y
                                            val screenHeightPx =
                                                with(density) { screenHeight.toPx() }
                                            val topBound = 0f
                                            val bottomBound = screenHeightPx * 0.9f - headerHeight
                                            val clampedY = newY.coerceIn(topBound, bottomBound)
                                            railOffset = IntOffset(
                                                railOffset.x + dragAmount.x.roundToInt(),
                                                clampedY.roundToInt()
                                            )
                                        }
                                        // **REMOVED CONFLICTING LOGIC**:
                                        // The 'else if (scope.enableRailDragging)' block that
                                        // handled vertical swipe-to-undock was here, causing
                                        // the gesture conflict with detectTapGestures.
                                        // That logic is correctly handled by the rail's pointerInput.
                                    },
                                    onDragEnd = {
                                        if (isFloating) {
                                            val distance = kotlin.math.sqrt(
                                                railOffset.x.toFloat()
                                                    .pow(2) + railOffset.y.toFloat().pow(2)
                                            )
                                            if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                railOffset = IntOffset.Zero
                                                isFloating = false
                                                if (scope.displayAppNameInHeader) isAppIcon = false
                                                hapticFeedback.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                            } else if (wasVisibleOnDragStart) {
                                                showFloatingButtons = true
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = if (isAppIcon) Alignment.Center else Alignment.CenterStart
                    ) {
                        if (isAppIcon) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    val baseModifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                    val finalModifier = when (scope.headerIconShape) {
                                        AzHeaderIconShape.CIRCLE -> baseModifier.clip(CircleShape)
                                        AzHeaderIconShape.ROUNDED -> baseModifier.clip(RoundedCornerShape(12.dp))
                                        AzHeaderIconShape.NONE -> baseModifier
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(model = appIcon),
                                        contentDescription = "Toggle menu, showing $appName icon",
                                        modifier = finalModifier
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle Menu",
                                        modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                softWrap = false,
                                maxLines = 1,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .pointerInput(isExpanded) {
                                    detectDragGestures(
                                        onDragStart = { _ -> },
                                        onDrag = { change, dragAmount ->
                                            val (x, y) = dragAmount
                                            if (kotlin.math.abs(x) > kotlin.math.abs(y)) { // Horizontal swipe
                                                if (isExpanded && x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                    isExpanded = false
                                                    change.consume()
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            val itemsToShow = scope.navItems.filter { !it.isSubItem }
                            itemsToShow.forEach { item ->
                                if (item.isDivider) {
                                    AzDivider()
                                } else {
                                    val onClick = scope.onClickMap[item.id]
                                    val onCyclerClick = if (item.isCycler) {
                                        {
                                            val state = cyclerStates[item.id]
                                            if (state != null && !item.disabled) {
                                                state.job?.cancel()

                                                val options =
                                                    requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                                                val disabledOptions =
                                                    item.disabledOptions ?: emptyList()
                                                val enabledOptions =
                                                    options.filterNot { it in disabledOptions }

                                                if (enabledOptions.isNotEmpty()) {
                                                    val currentDisplayed = state.displayedOption
                                                    val currentIndexInEnabled =
                                                        enabledOptions.indexOf(currentDisplayed)

                                                    val nextIndex =
                                                        if (currentIndexInEnabled != -1) {
                                                            (currentIndexInEnabled + 1) % enabledOptions.size
                                                        } else {
                                                            0
                                                        }
                                                    val nextOption = enabledOptions[nextIndex]

                                                    cyclerStates[item.id] = state.copy(
                                                        displayedOption = nextOption,
                                                        job = coroutineScope.launch {
                                                            delay(1000L)

                                                            val finalItemState =
                                                                scope.navItems.find { it.id == item.id }
                                                                    ?: item
                                                            val currentStateInVm =
                                                                finalItemState.selectedOption
                                                            val targetState = nextOption

                                                            val currentIndexInVm =
                                                                options.indexOf(currentStateInVm)
                                                            val targetIndex =
                                                                options.indexOf(targetState)

                                                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                                                val clicksToCatchUp =
                                                                    (targetIndex - currentIndexInVm + options.size) % options.size
                                                                if (onClick != null) {
                                                                    repeat(clicksToCatchUp) {
                                                                        onClick()
                                                                    }
                                                                }
                                                            }
                                                            isExpanded = false
                                                            cyclerStates[item.id] =
                                                                cyclerStates[item.id]!!.copy(job = null)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        onClick
                                    }
                                    val finalItem = if (item.isCycler) {
                                        item.copy(
                                            selectedOption = cyclerStates[item.id]?.displayedOption
                                                ?: item.selectedOption
                                        )
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
                                        onToggle = { isExpanded = !isExpanded },
                                        onItemClick = { selectedItem = finalItem },
                                        onHostClick = {
                                            hostStates[item.id] = !(hostStates[item.id] ?: false)
                                        }
                                    )

                                    AnimatedVisibility(
                                        visible = item.isHost && (hostStates[item.id] ?: false)
                                    ) {
                                        Column {
                                            val subItems =
                                                scope.navItems.filter { it.hostId == item.id }
                                            subItems.forEach { subItem ->
                                                MenuItem(
                                                    item = subItem,
                                                    navController = navController,
                                                    isSelected = subItem.route == currentDestination,
                                                    onClick = scope.onClickMap[subItem.id],
                                                    onCyclerClick = null,
                                                    onToggle = { isExpanded = !isExpanded },
                                                    onItemClick = { selectedItem = subItem }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (scope.showFooter) {
                            Footer(
                                appName = appName,
                                onToggle = { isExpanded = !isExpanded },
                                onUndock = {
                                    if (scope.onUndock != null) {
                                        scope.onUndock?.invoke()
                                    } else {
                                        isFloating = true
                                        isExpanded = false
                                        if (scope.displayAppNameInHeader) isAppIcon = true
                                    }
                                },
                                scope = scope
                            )
                        }
                    } else {
                        AnimatedVisibility(visible = !isFloating || showFloatingButtons) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
                                    .verticalScroll(rememberScrollState())
                                    .onSizeChanged { railItemsHeight = it.height }
                                    .pointerInput(
                                        isExpanded,
                                        disableSwipeToOpen,
                                        scope.enableRailDragging
                                    ) {
                                        detectDragGestures(
                                            onDragStart = { _ -> },
                                            onDrag = { change, dragAmount ->
                                                val (x, y) = dragAmount
                                                if (kotlin.math.abs(x) > kotlin.math.abs(y)) { // Horizontal swipe
                                                    if (!isExpanded && !disableSwipeToOpen && x > AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                        isExpanded = true
                                                        change.consume()
                                                    }
                                                } else if (scope.enableRailDragging) { // Vertical swipe
                                                    isFloating = true
                                                    isExpanded = false
                                                    if (scope.displayAppNameInHeader) isAppIcon =
                                                        true
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    change.consume()
                                                }
                                            }
                                        )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val onRailCyclerClick: (AzNavItem) -> Unit = { item ->
                                    val state = cyclerStates[item.id]
                                    if (state != null) {
                                        val options =
                                            requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                                        val disabledOptions = item.disabledOptions ?: emptyList()
                                        val enabledOptions =
                                            options.filterNot { it in disabledOptions }

                                        if (enabledOptions.isNotEmpty()) {
                                            val currentDisplayed = item.selectedOption
                                            val currentIndexInEnabled =
                                                enabledOptions.indexOf(currentDisplayed)

                                            val nextIndex = if (currentIndexInEnabled != -1) {
                                                (currentIndexInEnabled + 1) % enabledOptions.size
                                            } else {
                                                0
                                            }
                                            val nextOption = enabledOptions[nextIndex]

                                            val finalItemState =
                                                scope.navItems.find { it.id == item.id } ?: item
                                            val currentStateInVm = finalItemState.selectedOption
                                            val targetState = nextOption

                                            val currentIndexInVm = options.indexOf(currentStateInVm)
                                            val targetIndex = options.indexOf(targetState)

                                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                                val clicksToCatchUp =
                                                    (targetIndex - currentIndexInVm + options.size) % options.size
                                                val onClick = scope.onClickMap[item.id]
                                                if (onClick != null) {
                                                    repeat(clicksToCatchUp) {
                                                        onClick()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                        if (scope.packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                                    ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RailItems(
                                        items = scope.navItems,
                                        scope = scope,
                                        navController = navController,
                                        currentDestination = currentDestination,
                                        buttonSize = buttonSize,
                                        onRailCyclerClick = onRailCyclerClick,
                                        onItemSelected = { navItem -> selectedItem = navItem },
                                        hostStates = hostStates,
                                        packRailButtons = if (isFloating) true else scope.packRailButtons
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}