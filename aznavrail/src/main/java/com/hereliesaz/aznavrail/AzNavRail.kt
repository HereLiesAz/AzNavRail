package com.hereliesaz.aznavrail

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.hereliesaz.aznavrail.model.AzOrientation
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
@RequiresOptIn(message = "This API is for internal layout use only. Please use AzHostActivityLayout.")
annotation class AzStrictLayout

@OptIn(AzStrictLayout::class)
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    providedScope: AzNavRailScopeImpl? = null,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    visualDockingSide: AzDockingSide? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit = {}
) {
    if (!LocalAzNavHostPresent.current && providedScope == null) {
        throw IllegalStateException("AzNavRail must be used within an AzHostActivityLayout.")
    }

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) scope.reset()
    navController?.let { scope.navController = it }
    scope.apply(content)

    val actualActiveColor = if (scope.activeColor != Color.Unspecified) scope.activeColor else MaterialTheme.colorScheme.primary
    val actualShape = scope.defaultShape

    val overlayController = LocalAzNavRailOverlayController.current
    val effectiveNoMenu = scope.noMenu && overlayController == null
    val effectiveDockingSide = visualDockingSide ?: scope.dockingSide
    val isRightDocked = effectiveDockingSide == AzDockingSide.RIGHT

    val displayedNavItems = remember(scope.navItems, effectiveNoMenu) {
        if (effectiveNoMenu) scope.navItems.map { it.copy(isRailItem = true) } else scope.navItems
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val appName = remember(packageName) {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }
    val appIcon = remember(packageName) {
        packageManager.getApplicationIcon(packageName)
    }

    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val finalNavItems = remember(displayedNavItems, cyclerStates.toMap()) {
        displayedNavItems.map { item ->
            if (item.isCycler) item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption)
            else item
        }
    }

    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    var isFloating by remember { mutableStateOf(overlayController != null) }
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var wasVisibleOnDragStart by remember { mutableStateOf(false) }

    var isAppIcon by remember(isFloating, scope.displayAppName) { 
        mutableStateOf(if (isFloating) true else !scope.displayAppName) 
    }

    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val toggleHost: (String) -> Unit = { id ->
        val currentState = hostStates[id] ?: false
        hostStates.keys.forEach { hostStates[it] = false }
        hostStates[id] = !currentState
    }

    val safeZones = LocalAzSafeZones.current
    val currentContentOffset = overlayController?.contentOffset?.value ?: railOffset

    BackHandler(enabled = isExpanded && !effectiveNoMenu) {
        isExpanded = false
    }

    val closeMenuAction = {
        if (isExpanded) {
            isExpanded = false
        }
    }

    val onItemSelected: (AzNavItem) -> Unit = { item ->
        if (item.route != null && navController != null) {
            navController.navigate(item.route)
        }
        if (item.collapseOnClick) {
            isExpanded = false
        }
    }

    val onRailCyclerClick: (AzNavItem) -> Unit = { item ->
        if (item.options != null && item.options.isNotEmpty()) {
            val currentState = cyclerStates[item.id]
            val currentOption = currentState?.displayedOption ?: item.selectedOption ?: item.options.first()
            val currentIndex = item.options.indexOf(currentOption)
            val nextIndex = (currentIndex + 1) % item.options.size
            val nextOption = item.options[nextIndex]

            currentState?.job?.cancel()

            val newJob = coroutineScope.launch {
                delay(1000)
                item.onClick?.invoke()
                cyclerStates.remove(item.id)
            }
            cyclerStates[item.id] = CyclerTransientState(nextOption, newJob)
        }
    }

    val reverseLayout = isRightDocked && isLandscape

    Box(modifier = modifier.fillMaxSize()) {
        if (isExpanded && !effectiveNoMenu && !isFloating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            if (if (isRightDocked) dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                isExpanded = false
                                change.consume()
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = false }
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (isExpanded) scope.expandedWidth else scope.collapsedWidth)
                .offset { currentContentOffset }
                .align(if (isRightDocked) Alignment.TopEnd else Alignment.TopStart)
                .padding(top = safeZones.top, bottom = safeZones.bottom)
                .then(
                    if (isFloating) Modifier.wrapContentHeight(Alignment.Top) else Modifier
                )
                .clip(
                    RoundedCornerShape(
                        topStart = if (isRightDocked || isFloating) 16.dp else 0.dp,
                        bottomStart = if (isRightDocked || isFloating) 16.dp else 0.dp,
                        topEnd = if (!isRightDocked || isFloating) 16.dp else 0.dp,
                        bottomEnd = if (!isRightDocked || isFloating) 16.dp else 0.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(
                        topStart = if (isRightDocked || isFloating) 16.dp else 0.dp,
                        bottomStart = if (isRightDocked || isFloating) 16.dp else 0.dp,
                        topEnd = if (!isRightDocked || isFloating) 16.dp else 0.dp,
                        bottomEnd = if (!isRightDocked || isFloating) 16.dp else 0.dp
                    )
                )
                .then(
                    if (!disableSwipeToOpen && !isExpanded && !isFloating && !effectiveNoMenu) {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (if (isRightDocked) dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                    isExpanded = true
                                    change.consume()
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val headerHeight = with(density) { AzNavRailDefaults.HeaderHeightDp.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AzNavRailDefaults.HeaderHeightDp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isFloating) {
                                showFloatingButtons = !showFloatingButtons
                            } else if (!effectiveNoMenu) {
                                if (scope.vibrate) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                isExpanded = !isExpanded
                            }
                        }
                        .pointerInput(isFloating) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    if (!isFloating && (dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX || dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX)) {
                                        if (scope.enableRailDragging) {
                                            if (scope.overlayService != null) {
                                                OverlayHelper.launch(context, scope.overlayService!!)
                                            } else {
                                                isFloating = true
                                                isExpanded = false
                                                showFloatingButtons = false
                                                if (scope.displayAppName) isAppIcon = true
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(isFloating, scope.enableRailDragging, overlayController) {
                            detectDragGestures(
                                onDragStart = { },
                                onDrag = { change, _ -> change.consume() },
                                onDragEnd = { }
                            )
                        }
                        .pointerInput(isFloating, scope.enableRailDragging, overlayController) {
                            detectDragGestures(
                                onDragStart = {
                                    if (overlayController == null && !isFloating && scope.enableRailDragging) {
                                        if (scope.overlayService != null) {
                                            OverlayHelper.launch(context, scope.overlayService!!)
                                        } else {
                                            isFloating = true
                                            isExpanded = false
                                            if (scope.displayAppName) isAppIcon = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                },
                                onDrag = { _, _ -> },
                                onDragEnd = { }
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
                                        if (scope.onRailDrag != null) scope.onRailDrag?.invoke(dragAmount.x, dragAmount.y)
                                        else {
                                            val newY = railOffset.y + dragAmount.y
                                            val bottomBound = with(density) { screenHeight.toPx() } * 0.9f - headerHeight
                                            railOffset = IntOffset(railOffset.x + dragAmount.x.roundToInt(), newY.coerceIn(0f, bottomBound).roundToInt())
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (overlayController != null) {
                                        overlayController.onDragEnd()
                                        if (isFloating) showFloatingButtons = true
                                    } else if (isFloating) {
                                        if (scope.onRailDrag == null) {
                                            if (kotlin.math.sqrt(railOffset.x.toFloat().pow(2) + railOffset.y.toFloat().pow(2)) < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                railOffset = IntOffset.Zero
                                                isFloating = false
                                                if (scope.displayAppName) isAppIcon = false
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else if (wasVisibleOnDragStart) showFloatingButtons = true
                                        } else if (wasVisibleOnDragStart) showFloatingButtons = true
                                    }
                                }
                            )
                        },
                    contentAlignment = if (isAppIcon) Alignment.Center else Alignment.CenterStart
                ) {
                    if (isAppIcon) {
                        val headerIconShapeModifier = when (scope.headerIconShape) {
                            AzHeaderIconShape.CIRCLE -> CircleShape
                            AzHeaderIconShape.ROUNDED -> RoundedCornerShape(8.dp)
                            AzHeaderIconShape.NONE -> RectangleShape
                        }
                        if (appIcon != null) {
                            Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "Toggle menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize).clip(headerIconShapeModifier))
                        } else {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle Menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                        }
                    } else {
                        Text(text = appName, style = MaterialTheme.typography.titleMedium, softWrap = false, maxLines = 1, textAlign = TextAlign.Start)
                    }
                }

                val contentBlock = @Composable {
                    if (isExpanded) {
                        val displayedItems = if (reverseLayout) displayedNavItems.reversed() else displayedNavItems
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .pointerInput(isExpanded) {
                                    detectHorizontalDragGestures(onHorizontalDrag = { change, dragAmount ->
                                        if (isExpanded && (if (isRightDocked) dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX)) {
                                            isExpanded = false
                                            change.consume()
                                        }
                                    })
                                }
                        ) {
                            displayedItems.filter { !it.isSubItem }.forEach { item ->
                                if (item.isDivider) {
                                    com.hereliesaz.aznavrail.AzDivider()
                                } else {
                                    MenuItem(
                                        item = item,
                                        subItems = displayedItems.filter { it.isSubItem && it.hostId == item.id },
                                        currentDestination = currentDestination,
                                        activeColor = actualActiveColor,
                                        activeClassifiers = scope.activeClassifiers,
                                        hostStates = hostStates,
                                        onToggleHost = { toggleHost(item.id) },
                                        onItemClick = { onItemSelected(item); item.onClick?.invoke() },
                                        onMenuCyclerClick = { onRailCyclerClick(it) },
                                        infoScreen = scope.infoScreen
                                    )
                                }
                            }
                        }
                    } else if (!isFloating || showFloatingButtons) {
                        RailItems(
                            items = finalNavItems,
                            currentDestination = currentDestination,
                            activeColor = actualActiveColor,
                            shape = actualShape,
                            buttonSize = AzNavRailDefaults.ButtonSize,
                            activeClassifiers = scope.activeClassifiers,
                            scope = scope,
                            navController = navController,
                            onItemSelected = onItemSelected,
                            onRailCyclerClick = onRailCyclerClick,
                            hostStates = hostStates,
                            packRailButtons = scope.packButtons,
                            orientation = if (isLandscapeEffective && !isFloating) AzOrientation.Horizontal else AzOrientation.Vertical,
                            onItemGloballyPositioned = scope.onItemGloballyPositioned,
                            infoScreen = scope.infoScreen,
                            reverseLayout = reverseLayout
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    contentBlock()
                }

                if (scope.showFooter && !isFloating && (!isLandscapeEffective || isExpanded)) {
                    Footer(
                        appName = appName,
                        onToggle = { if (!effectiveNoMenu) isExpanded = !isExpanded },
                        onUndock = {
                            if (scope.onUndock != null) {
                                scope.onUndock?.invoke()
                            } else if (scope.enableRailDragging) {
                                if (scope.overlayService != null) {
                                    OverlayHelper.launch(context, scope.overlayService!!)
                                } else {
                                    isFloating = true
                                    isExpanded = false
                                    if (scope.displayAppName) isAppIcon = true
                                }
                            }
                        },
                        scope = scope,
                        footerColor = if (isExpanded) {
                            if (scope.navItems.isNotEmpty()) scope.navItems.first().color ?: actualActiveColor else actualActiveColor
                        } else actualActiveColor
                    )
                }
            }
        }

        if (scope.isLoading) {
            Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = false)) {
                AzLoad()
            }
        }

        if (scope.infoScreen) {
            HelpOverlay(
                items = finalNavItems,
                onDismiss = scope.onDismissInfoScreen ?: {}
            )
        }
    }
}

@Composable
internal fun DraggableRailItemWrapper(
    item: AzNavItem,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    orientation: AzOrientation,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: Map<String, Boolean>,
    onClickOverride: ((AzNavItem) -> Unit)?,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    draggedItemId: String?,
    dragOffset: Float,
    currentDropTargetIndex: Int?,
    onDragStart: (String) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (Float) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showHiddenMenu by remember { mutableStateOf(false) }
    val isDragging = draggedItemId == item.id
    val offset = if (isDragging) dragOffset else 0f
    
    val actualActiveColor = if (scope.activeColor != Color.Unspecified) scope.activeColor else MaterialTheme.colorScheme.primary
    val isVertical = orientation == AzOrientation.Vertical
    var showScreenTitle by remember { mutableStateOf(false) }
    val isRightDocked = scope.dockingSide == AzDockingSide.RIGHT

    if (item.isNestedRail) {
        var showNestedRail by remember { mutableStateOf(false) }
        val alignment = item.nestedRailAlignment ?: AzNestedRailAlignment.VERTICAL
        
        Box {
            AzNavRailButton(
                item = item,
                currentDestination = currentDestination,
                activeColor = actualActiveColor,
                activeClassifiers = scope.activeClassifiers,
                onClick = { 
                    if (infoScreen) return@AzNavRailButton
                    showNestedRail = !showNestedRail 
                },
                modifier = Modifier,
                size = buttonSize,
                onGloballyPositioned = { rect -> onItemGloballyPositioned?.invoke(item.id, rect) }
            )
            
            if (showNestedRail) {
                Popup(
                    alignment = if (isRightDocked) Alignment.TopEnd else Alignment.TopStart,
                    onDismissRequest = { showNestedRail = false },
                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
                ) {
                    com.hereliesaz.aznavrail.internal.NestedRail(
                        parentItem = item,
                        items = item.nestedRailItems ?: emptyList(),
                        currentDestination = currentDestination,
                        activeColor = actualActiveColor,
                        activeClassifiers = scope.activeClassifiers,
                        onItemSelected = { 
                            onItemSelected(it)
                            it.onClick?.invoke()
                            showNestedRail = false 
                        },
                        alignment = alignment,
                        isRightDocked = isRightDocked
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .offset {
                if (isVertical) IntOffset(0, offset.roundToInt())
                else IntOffset(offset.roundToInt(), 0)
            }
            .zIndex(if (isDragging) 1f else 0f)
    ) {
        val clickModifier = if (item.isRelocItem && !infoScreen) {
            Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHiddenMenu = false
                        onDragStart(item.id) 
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        onDrag(if (isVertical) dragAmount.y else dragAmount.x)
                    }
                )
            }
        } else Modifier

        Box(modifier = clickModifier) {
            AzNavRailButton(
                item = item,
                currentDestination = currentDestination,
                activeColor = actualActiveColor,
                activeClassifiers = scope.activeClassifiers,
                onClick = {
                    if (infoScreen) return@AzNavRailButton
                    showScreenTitle = true
                    if (item.isRelocItem) {
                        showHiddenMenu = !showHiddenMenu
                    } else {
                        if (onClickOverride != null) onClickOverride(item)
                        else if (item.isCycler) onRailCyclerClick(item)
                        else {
                            if (item.route != null && navController != null) navController.navigate(item.route)
                            onItemSelected(item)
                        }
                    }
                },
                onLongClick = {
                    if (infoScreen) return@AzNavRailButton
                    showScreenTitle = true
                    if (item.isRelocItem && !isDragging) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHiddenMenu = false
                        onDragStart(item.id)
                    }
                },
                modifier = Modifier,
                size = buttonSize,
                onGloballyPositioned = { rect ->
                    if (item.isRelocItem) {
                        com.hereliesaz.aznavrail.internal.RelocItemHandler.itemBoundsCache[item.id] = rect
                    }
                    onItemGloballyPositioned?.invoke(item.id, rect)
                }
            )
        }

        if (showScreenTitle && item.screenTitle != "NO_TITLE") {
            val titleText = item.screenTitle ?: item.text
            if (titleText.isNotEmpty()) {
                Popup(
                    alignment = if (isRightDocked) Alignment.TopStart else Alignment.TopEnd,
                    properties = PopupProperties(focusable = false)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .then(if (isRightDocked) Modifier.padding(start = 32.dp) else Modifier.padding(end = 16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    delay(1500)
                    showScreenTitle = false
                }
            }
        }

        if (showHiddenMenu && item.hiddenMenuItems != null && item.hiddenMenuItems.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(if (isRightDocked) -150 else 150, 0),
                onDismissRequest = { showHiddenMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .width(IntrinsicSize.Max)
                ) {
                    item.hiddenMenuItems.forEach { hiddenItem ->
                        if (hiddenItem.isInput) {
                            var text by remember { mutableStateOf("") }
                            AzTextBox(
                                value = text,
                                onValueChange = { text = it },
                                hint = hiddenItem.hint ?: hiddenItem.text,
                                onSubmit = {
                                    val action = scope.hiddenMenuInputActions[hiddenItem.id]
                                    action?.invoke(it)
                                    showHiddenMenu = false
                                }
                            )
                        } else {
                            Text(
                                text = hiddenItem.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (hiddenItem.route != null && navController != null) {
                                            navController.navigate(hiddenItem.route)
                                        } else {
                                            val action = scope.hiddenMenuActions[hiddenItem.id]
                                            action?.invoke()
                                        }
                                        showHiddenMenu = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
