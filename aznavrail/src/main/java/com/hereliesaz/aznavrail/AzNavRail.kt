package com.hereliesaz.aznavrail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.OverlayHelper
import com.hereliesaz.aznavrail.internal.CenteredPopupPositionProvider
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.service.LocalAzNavRailOverlayController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

object AzNavRail {
    const val noTitle = "AZNAVRAIL_NO_TITLE"
    const val EXTRA_ROUTE = "com.hereliesaz.aznavrail.extra.ROUTE"
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
    val overlayController = LocalAzNavRailOverlayController.current

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

    // Force Footer Text Color: use color of first item, or primary if not set/available
    val footerColor = remember(scope.navItems) {
        scope.navItems.firstOrNull()?.color ?: Color.Unspecified
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    // If overlayController is present, isExpanded is always false.
    // Use a derived boolean for read operations to enforce logic, but write to internal state (or ignore if blocked).

    val isExpandedState = remember(overlayController) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = if (overlayController != null) false else isExpandedInternal
                set(v) {
                     if (overlayController == null) isExpandedInternal = v
                }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var isExpanded by isExpandedState

    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    // If initially expanded, we are not floating. But if initially NOT expanded, we might be?
    // Actually, FAB mode is triggered by user action or specific config.
    // If the rail is meant to be in FAB mode initially (e.g. for Overlay), the user should use `onUndock` logic
    // or just start in a state that looks like FAB.
    // However, `isFloating` state determines if we show just the header (FAB) or the rail.
    // For Overlay, we likely want `isFloating = true` initially if it's "undocked".
    // But `initiallyExpanded` handles "Expanded vs Collapsed".
    // "Floating" is "Collapsed to just a FAB".

    // Logic: If onRailDrag is provided, we might assume we are in a 'floating window' context.
    // In floating window context, 'isFloating' usually means 'collapsed to FAB'.
    // If 'initiallyExpanded' is true, we show full rail.
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
    val itemPositions = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

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
            // Only apply screen bounds clamping if we are NOT using external drag (Overlay mode might have its own bounds)
            if (scope.onRailDrag == null && overlayController == null) {
                val screenHeightPx = with(density) { screenHeight.toPx() }
                val bottomBound = screenHeightPx * 0.9f
                val railBottom = railOffset.y + headerHeight + railItemsHeight
                if (railBottom > bottomBound) {
                    val newY = bottomBound - headerHeight - railItemsHeight
                    railOffset = IntOffset(railOffset.x, newY.roundToInt())
                }
            }
        }
    }

    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    LaunchedEffect(activity) {
        activity?.intent?.let { intent ->
            if (intent.hasExtra(AzNavRail.EXTRA_ROUTE)) {
                val route = intent.getStringExtra(AzNavRail.EXTRA_ROUTE)
                if (route != null) {
                    navController?.navigate(route)
                    intent.removeExtra(AzNavRail.EXTRA_ROUTE)
                }
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
                    .offset {
                         if (overlayController != null) {
                             overlayController.contentOffset.value
                         } else {
                             railOffset
                         }
                    },
                containerColor = if (isExpanded) MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.95f
                ) else Color.Transparent,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(bottom = AzNavRailDefaults.HeaderPadding)
                            .onSizeChanged { headerHeight = it.height }
                            .pointerInput(
                                isFloating,
                                scope.enableRailDragging,
                                scope.displayAppNameInHeader,
                                scope.onRailDrag,
                                overlayController
                            ) {
                                detectTapGestures(
                                    onTap = {
                                        if (isFloating) {
                                            showFloatingButtons = !showFloatingButtons
                                        } else {
                                            // Only allow expanding if NOT in overlay mode
                                            if (overlayController == null) {
                                                isExpanded = !isExpanded
                                            }
                                        }
                                    },
                                    onLongPress = {
                                        if (isFloating) {
                                            // Long press in FAB mode -> dock
                                            // If dragging externally, we might not want to reset railOffset to Zero relative to container?
                                            // But let's keep behavior consistent: Docking means exiting FAB mode.

                                            // If onRailDrag is set, we probably shouldn't reset railOffset because we are moving the window.
                                            // But 'docking' usually implies returning to the side of the screen.
                                            // In Overlay mode, 'docking' might not make sense or might mean 'stick to edge'.
                                            // For now, if onRailDrag is present, we assume the user controls position.

                                            if (scope.onRailDrag == null && overlayController == null) {
                                                railOffset = IntOffset.Zero
                                            }

                                            isFloating = false
                                            if (scope.displayAppNameInHeader) isAppIcon = false
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (scope.enableRailDragging) {
                                            val overlayService = scope.overlayService
                                            if (overlayService != null) {
                                                OverlayHelper.launch(context, overlayService)
                                            } else {
                                                // Long press in docked mode -> FAB
                                                isFloating = true
                                                isExpanded = false
                                                if (scope.displayAppNameInHeader) isAppIcon = true
                                            }
                                            hapticFeedback.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        }
                                    }
                                )
                            }
                            .pointerInput(isFloating, scope.enableRailDragging, scope.onRailDrag, overlayController) {
                                detectDragGestures(
                                    onDragStart = { _ ->
                                        if (overlayController != null) {
                                            overlayController.onDragStart()
                                            if (isFloating) {
                                                wasVisibleOnDragStart = showFloatingButtons
                                                showFloatingButtons = false
                                            }
                                        } else if (isFloating) {
                                            wasVisibleOnDragStart = showFloatingButtons
                                            showFloatingButtons = false
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (overlayController != null) {
                                            change.consume()
                                            overlayController.onDrag(dragAmount)
                                        } else if (scope.onOverlayDrag != null) {
                                            change.consume()
                                            scope.onOverlayDrag?.invoke(dragAmount.x, dragAmount.y)
                                        } else if (isFloating) {
                                            change.consume()

                                            val onDrag = scope.onRailDrag
                                            if (onDrag != null) {
                                                onDrag(dragAmount.x, dragAmount.y)
                                                // Do not update internal railOffset
                                            } else {
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
                                        }
                                    },
                                    onDragEnd = {
                                        if (overlayController != null) {
                                            overlayController.onDragEnd()
                                            // Always show buttons on drag end in overlay mode, as requested.
                                            // This ensures the rail expands when dropped.
                                            if (isFloating) {
                                                showFloatingButtons = true
                                            }
                                        } else if (isFloating) {
                                            if (scope.onRailDrag == null) {
                                                // If dragging externally, we don't snap back to dock on release near origin
                                                // because the origin of the Window is top-left of screen, not rail container.
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
                                            } else {
                                                // External drag end
                                                if (wasVisibleOnDragStart) {
                                                    showFloatingButtons = true
                                                }
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
                                    detectHorizontalDragGestures(
                                        onDragStart = { _ -> },
                                        onHorizontalDrag = { change, dragAmount ->
                                            // Horizontal swipe to close
                                            if (isExpanded && dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                isExpanded = false
                                                change.consume()
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
                                            // Close other hosts
                                            val wasExpanded = hostStates[item.id] ?: false
                                            val keys = hostStates.keys.toList()
                                            keys.forEach { key ->
                                                hostStates[key] = false
                                            }
                                            // Toggle current (if it was expanded, it's now collapsed; if collapsed, now expanded)
                                            hostStates[item.id] = !wasExpanded
                                        },
                                        onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                        infoScreen = scope.infoScreen
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
                                                    onItemClick = { selectedItem = subItem },
                                                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                                    infoScreen = scope.infoScreen
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
                                    val overlayService = scope.overlayService
                                    if (scope.onUndock != null) {
                                        scope.onUndock?.invoke()
                                    } else if (overlayService != null) {
                                        OverlayHelper.launch(context, overlayService)
                                    } else {
                                        isFloating = true
                                        isExpanded = false
                                        if (scope.displayAppNameInHeader) isAppIcon = true
                                    }
                                },
                                scope = scope,
                                footerColor = if (footerColor != Color.Unspecified) footerColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Prepare custom click handling for Overlay mode
                        val handleOverlayClick: (AzNavItem) -> Unit = { item ->
                            if (overlayController != null && item.route != null) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    launchIntent.putExtra(AzNavRail.EXTRA_ROUTE, item.route)
                                    context.startActivity(launchIntent)
                                } else {
                                    // Fallback if no launch intent
                                    AzNavRailLogger.e("AzNavRail", "Could not find launch intent for package $packageName")
                                }
                            } else {
                                // Standard behavior
                                scope.onClickMap[item.id]?.invoke()
                            }
                        }

                        // If in Overlay Mode, pass null for navController to intercept routing via onClick.
                        val effectiveNavController = if (overlayController != null) null else navController

                        AnimatedVisibility(
                            visible = !isFloating || showFloatingButtons,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
                                    .verticalScroll(rememberScrollState())
                                    .onSizeChanged { railItemsHeight = it.height }
                                    .pointerInput(
                                        isExpanded,
                                        disableSwipeToOpen
                                    ) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { _ -> },
                                            onHorizontalDrag = { change, dragAmount ->
                                                // Horizontal swipe to open
                                                if (!isExpanded && !disableSwipeToOpen && dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                    isExpanded = true
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
                                        navController = effectiveNavController,
                                        currentDestination = currentDestination,
                                        buttonSize = buttonSize,
                                        onRailCyclerClick = onRailCyclerClick,
                                        onItemSelected = { navItem -> selectedItem = navItem },
                                        hostStates = hostStates,
                                        packRailButtons = if (isFloating) true else scope.packRailButtons,
                                        onClickOverride = if (overlayController != null) handleOverlayClick else null,
                                        onItemGloballyPositioned = { id, rect ->
                                            itemPositions[id] = rect
                                            scope.onItemGloballyPositioned?.invoke(id, rect)
                                        },
                                        infoScreen = scope.infoScreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (scope.infoScreen) {
            HelpOverlay(
                items = scope.navItems,
                itemPositions = itemPositions,
                hostStates = hostStates,
                railWidth = railWidth,
                onDismiss = { scope.onDismissInfoScreen?.invoke() }
            )
        }
    }
}
