package com.hereliesaz.aznavrail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.OverlayHelper
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.service.LocalAzNavRailOverlayController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

@Target(AnnotationTarget.FUNCTION)
@RequiresOptIn(message = "This API is strictly controlled. Use AzHostActivityLayout.")
annotation class AzStrictLayout

object AzNavRail {
    const val noTitle = "AZNAVRAIL_NO_TITLE"
    const val EXTRA_ROUTE = "com.hereliesaz.aznavrail.extra.ROUTE"
}

@AzStrictLayout
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    providedScope: AzNavRailScopeImpl? = null,
    content: AzNavRailScope.() -> Unit
) {
    val isHostPresent = LocalAzNavHostPresent.current
    val overlayController = LocalAzNavRailOverlayController.current

    if (!isHostPresent && overlayController == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "FATAL LAYOUT VIOLATION",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AzNavRail MUST be wrapped in AzHostActivityLayout.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        error("FATAL LAYOUT VIOLATION: AzNavRail instantiated without AzHostActivityLayout.")
    }

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }

    if (providedScope == null) {
        scope.reset()
    }
    navController?.let { scope.navController = it }

    scope.apply(content)
    val effectiveNoMenu = scope.noMenu && overlayController == null
    val isRightDocked = scope.dockingSide == AzDockingSide.RIGHT

    val displayedNavItems = remember(scope.navItems, effectiveNoMenu) {
        if (effectiveNoMenu) {
            scope.navItems.map { it.copy(isRailItem = true) }
        } else {
            scope.navItems
        }
    }

    displayedNavItems.forEach { item ->
        if (item.isSubItem && item.isRailItem) {
            val host = scope.navItems.find { it.id == item.hostId }
            require(host != null && host.isRailItem) {
                "HIERARCHY ERROR: Rail sub-item '${item.id}' must be hosted by a valid rail host item."
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
            "App"
        }
    }

    val appIcon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app icon", e)
            null
        }
    }

    val footerColor = remember(displayedNavItems) {
        displayedNavItems.firstOrNull()?.color ?: Color.Unspecified
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    
    val isExpandedState = remember(overlayController, effectiveNoMenu) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = if (overlayController != null || effectiveNoMenu) false else isExpandedInternal
                set(v) {
                     if (overlayController == null && !effectiveNoMenu) isExpandedInternal = v
                }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var isExpanded by isExpandedState

    var showFooterPopup by remember { mutableStateOf(false) }
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isFloating by remember { mutableStateOf(false) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var wasVisibleOnDragStart by remember { mutableStateOf(false) }
    var isAppIcon by remember { mutableStateOf(!scope.displayAppName) }
    var headerHeight by remember { mutableStateOf(0) }
    var railItemsHeight by remember { mutableStateOf(0) }

    val hapticFeedback = LocalHapticFeedback.current

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedWidth else scope.collapsedWidth,
        label = "railWidth"
    )

    val coroutineScope = rememberCoroutineScope()
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    var selectedItem by rememberSaveable { mutableStateOf<AzNavItem?>(null) }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val itemPositions = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val descriptionPositions = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var rootPosition by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val itemsWithInfo by remember {
        derivedStateOf {
            displayedNavItems.filter { item ->
                val hasInfo = !item.info.isNullOrBlank()
                val isRailItem = item.isRailItem
                val isVisible = if (item.isSubItem) {
                    hostStates[item.hostId] == true
                } else {
                    true
                }
                hasInfo && isRailItem && isVisible
            }
        }
    }

    LaunchedEffect(displayedNavItems) {
        val initialSelectedItem = if (currentDestination != null) {
            displayedNavItems.find { it.route == currentDestination }
        } else {
            displayedNavItems.firstOrNull()
        }
        selectedItem = initialSelectedItem

        displayedNavItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
            }
        }
    }

    // Cycler sync logic
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            cyclerStates.forEach { (id, state) ->
                if (state.job != null) {
                    state.job.cancel()
                    val item = displayedNavItems.find { it.id == id }
                    if (item != null) {
                        coroutineScope.launch {
                            val options = requireNotNull(item.options)
                            val currentStateInVm = item.selectedOption
                            val targetState = state.displayedOption
                            val currentIndexInVm = options.indexOf(currentStateInVm)
                            val targetIndex = options.indexOf(targetState)

                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                val onClick = scope.onClickMap[item.id]
                                if (onClick != null) {
                                    repeat(clicksToCatchUp) { onClick() }
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

    Box(
        modifier = modifier
            .onGloballyPositioned { rootPosition = it.boundsInWindow() }
            .drawBehind {
                if (scope.infoScreen && rootPosition != null) {
                    val rootRect = rootPosition!!

                    itemsWithInfo.forEach { item ->
                        val itemRect = itemPositions[item.id]
                        val descRect = descriptionPositions[item.id]
                        if (itemRect != null && descRect != null) {
                             val startX = if (isRightDocked) itemRect.left else itemRect.right
                             val startY = itemRect.center.y

                             val endX = if (isRightDocked) descRect.right else descRect.left
                             val endY = descRect.center.y

                             val startLocal = Offset(startX - rootRect.left, startY - rootRect.top)
                             val endLocal = Offset(endX - rootRect.left, endY - rootRect.top)

                             drawLine(
                                 color = Color.Gray,
                                 start = startLocal,
                                 end = endLocal,
                                 strokeWidth = 2.dp.toPx()
                             )
                        }
                    }
                }
            },
        contentAlignment = if (isRightDocked) Alignment.TopEnd else Alignment.TopStart
    ) {
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isRightDocked) Alignment.TopEnd else Alignment.TopStart
        ) {
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
                containerColor = if (isExpanded) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(bottom = AzNavRailDefaults.HeaderPadding)
                            .onSizeChanged { headerHeight = it.height }
                            .pointerInput(isFloating, scope.enableRailDragging, scope.displayAppName, scope.onRailDrag, overlayController) {
                                detectTapGestures(
                                    onTap = {
                                        if (effectiveNoMenu) {
                                            showFooterPopup = !showFooterPopup
                                        } else if (isFloating) {
                                            showFloatingButtons = !showFloatingButtons
                                        } else {
                                            if (overlayController == null) isExpanded = !isExpanded
                                        }
                                    },
                                    onLongPress = {
                                        if (isFloating) {
                                            if (scope.onRailDrag == null && overlayController == null) railOffset = IntOffset.Zero
                                            isFloating = false
                                            if (scope.displayAppName) isAppIcon = false
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } else if (scope.enableRailDragging) {
                                            val overlayService = scope.overlayService
                                            if (overlayService != null) {
                                                OverlayHelper.launch(context, overlayService)
                                            } else {
                                                isFloating = true
                                                isExpanded = false
                                                if (scope.displayAppName) isAppIcon = true
                                            }
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                )
                            }
                            .pointerInput(isFloating, scope.enableRailDragging, scope.onRailDrag, overlayController) {
                                detectDragGestures(
                                    onDragStart = {
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
                                            } else {
                                                val newY = railOffset.y + dragAmount.y
                                                val screenHeightPx = with(density) { screenHeight.toPx() }
                                                val bottomBound = screenHeightPx * 0.9f - headerHeight
                                                val clampedY = newY.coerceIn(0f, bottomBound)
                                                railOffset = IntOffset(railOffset.x + dragAmount.x.roundToInt(), clampedY.roundToInt())
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (overlayController != null) {
                                            overlayController.onDragEnd()
                                            if (isFloating) showFloatingButtons = true
                                        } else if (isFloating) {
                                            if (scope.onRailDrag == null) {
                                                val distance = kotlin.math.sqrt(railOffset.x.toFloat().pow(2) + railOffset.y.toFloat().pow(2))
                                                if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                    railOffset = IntOffset.Zero
                                                    isFloating = false
                                                    if (scope.displayAppName) isAppIcon = false
                                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } else if (wasVisibleOnDragStart) {
                                                    showFloatingButtons = true
                                                }
                                            } else {
                                                if (wasVisibleOnDragStart) showFloatingButtons = true
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
                                    Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "Toggle menu, showing $appName icon", modifier = finalModifier)
                                } else {
                                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle Menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                                }
                            }
                        } else {
                            Text(text = appName, style = MaterialTheme.typography.titleMedium, softWrap = false, maxLines = 1, textAlign = TextAlign.Start)
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
                                        onDragStart = { },
                                        onHorizontalDrag = { change, dragAmount ->
                                            val shouldClose = if (isRightDocked) dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX
                                            if (isExpanded && shouldClose) {
                                                isExpanded = false
                                                change.consume()
                                            }
                                        }
                                    )
                                }
                        ) {
                            val itemsToShow = displayedNavItems.filter { !it.isSubItem }
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
                                                            delay(1000L)
                                                            val finalItemState = displayedNavItems.find { it.id == item.id } ?: item
                                                            val currentStateInVm = finalItemState.selectedOption
                                                            val targetState = nextOption
                                                            val currentIndexInVm = options.indexOf(currentStateInVm)
                                                            val targetIndex = options.indexOf(targetState)
                                                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                                                if (onClick != null) repeat(clicksToCatchUp) { onClick() }
                                                            }
                                                            isExpanded = false
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
                                        onToggle = { isExpanded = !isExpanded },
                                        onItemClick = { selectedItem = finalItem },
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
                                                    onToggle = { isExpanded = !isExpanded },
                                                    onItemClick = { selectedItem = subItem },
                                                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                                    infoScreen = scope.infoScreen,
                                                    activeColor = scope.activeColor
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
                                        if (scope.displayAppName) isAppIcon = true
                                        showFooterPopup = false
                            }
                                },
                                scope = scope,
                                footerColor = if (footerColor != Color.Unspecified) footerColor else MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val handleOverlayClick: (AzNavItem) -> Unit = { item ->
                            if (overlayController != null && item.route != null) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    launchIntent.putExtra(AzNavRail.EXTRA_ROUTE, item.route)
                                    context.startActivity(launchIntent)
                                } else {
                                    AzNavRailLogger.e("AzNavRail", "Could not find launch intent for package $packageName")
                                }
                            } else {
                                scope.onClickMap[item.id]?.invoke()
                            }
                        }
                        val effectiveNavController = if (overlayController != null) null else navController

                        AnimatedVisibility(visible = !isFloating || showFloatingButtons, modifier = Modifier.weight(1f)) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
                                    .verticalScroll(rememberScrollState())
                                    .onSizeChanged { railItemsHeight = it.height }
                                    .pointerInput(isExpanded, disableSwipeToOpen) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { },
                                            onHorizontalDrag = { change, dragAmount ->
                                                val shouldOpen = if (isRightDocked) dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX
                                                if (!isExpanded && !disableSwipeToOpen && shouldOpen) {
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

                                Column(
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                                        if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                                    ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RailItems(
                                        items = displayedNavItems,
                                        scope = scope,
                                        navController = effectiveNavController,
                                        currentDestination = currentDestination,
                                        buttonSize = buttonSize,
                                        onRailCyclerClick = onRailCyclerClick,
                                        onItemSelected = { navItem -> selectedItem = navItem },
                                        hostStates = hostStates,
                                        packRailButtons = if (isFloating) true else scope.packButtons,
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

        if (showFooterPopup && effectiveNoMenu) {
            Popup(onDismissRequest = { showFooterPopup = false }, popupPositionProvider = CenteredPopupPositionProvider) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Footer(
                        appName = appName,
                        onToggle = { showFooterPopup = false },
                        onUndock = {
                            val overlayService = scope.overlayService
                            if (scope.onUndock != null) {
                                scope.onUndock?.invoke()
                            } else if (overlayService != null) {
                                OverlayHelper.launch(context, overlayService)
                            } else {
                                isFloating = true
                                isExpanded = false
                                if (scope.displayAppName) isAppIcon = true
                                showFooterPopup = false
                            }
                        },
                        scope = scope,
                        footerColor = if (footerColor != Color.Unspecified) footerColor else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (scope.infoScreen) {
            HelpOverlay(
                items = displayedNavItems,
                itemPositions = itemPositions,
                hostStates = hostStates,
                railWidth = railWidth,
                onDismiss = { scope.onDismissInfoScreen?.invoke() },
                isRightDocked = isRightDocked,
                safeZones = LocalAzSafeZones.current,
                onDescriptionGloballyPositioned = { id, rect -> descriptionPositions[id] = rect }
            )
        }
    }
}
