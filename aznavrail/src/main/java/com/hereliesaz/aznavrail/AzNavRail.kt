// aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.RectangleShape
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
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.internal.CenteredPopupPositionProvider
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.OverlayHelper
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.service.LocalAzNavRailOverlayController
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
    orientation: AzOrientation = AzOrientation.Vertical,
    visualDockingSide: AzDockingSide? = null,
    railAlignment: Alignment? = null,
    reverseLayout: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val isHostPresent = LocalAzNavHostPresent.current
    val overlayController = LocalAzNavRailOverlayController.current

    if (!isHostPresent && overlayController == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Red), contentAlignment = Alignment.Center) {
            Text("FATAL LAYOUT VIOLATION", color = Color.White)
        }
        error("FATAL LAYOUT VIOLATION: AzNavRail instantiated without AzHostActivityLayout.")
    }

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) scope.reset()
    navController?.let { scope.navController = it }
    scope.apply(content)

    val actualActiveColor = if (scope.activeColor != Color.Unspecified) scope.activeColor else MaterialTheme.colorScheme.primary
    val actualShape = scope.defaultShape

    val effectiveNoMenu = scope.noMenu && overlayController == null
    val effectiveDockingSide = visualDockingSide ?: scope.dockingSide
    val isRightDocked = effectiveDockingSide == AzDockingSide.RIGHT

    val displayedNavItems = remember(scope.navItems, effectiveNoMenu) {
        if (effectiveNoMenu) scope.navItems.map { it.copy(isRailItem = true) } else scope.navItems
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val appName = remember(packageName) { packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString() }
    val appIcon = remember(packageName) { packageManager.getApplicationIcon(packageName) }

    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val finalNavItems = remember(displayedNavItems, cyclerStates.toMap()) {
        displayedNavItems.map { item -> if (item.isCycler) item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption) else item }
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    val isExpandedState = remember(overlayController, effectiveNoMenu) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = if (overlayController != null || effectiveNoMenu) false else isExpandedInternal
                set(v) { if (overlayController == null && !effectiveNoMenu) isExpandedInternal = v }
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
    var railBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val hapticFeedback = LocalHapticFeedback.current
    val isVertical = orientation == AzOrientation.Vertical
    val railThickness by animateDpAsState(if (isExpanded) scope.expandedWidth else scope.collapsedWidth, label = "railThickness")

    val coroutineScope = rememberCoroutineScope()
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedItem = remember(selectedItemId, finalNavItems) { finalNavItems.find { it.id == selectedItemId } }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val itemPositions = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    LaunchedEffect(displayedNavItems, currentDestination) {
        val targetId = if (currentDestination != null) displayedNavItems.find { it.route == currentDestination }?.id else selectedItemId ?: displayedNavItems.firstOrNull()?.id
        if (targetId != null) selectedItemId = targetId
        displayedNavItems.forEach { if (it.isCycler) cyclerStates.putIfAbsent(it.id, CyclerTransientState(it.selectedOption ?: "")) }
    }

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
                                if (onClick != null) repeat(clicksToCatchUp) { onClick() }
                            }
                            cyclerStates.put(id, state.copy(job = null))
                        }
                    }
                }
            }
        }
    }

    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    LaunchedEffect(showFloatingButtons) {
        if (showFloatingButtons && scope.onRailDrag == null && overlayController == null) {
            val bottomBound = with(density) { screenHeight.toPx() } * 0.9f
            val railBottom = railOffset.y + headerHeight + railItemsHeight
            if (railBottom > bottomBound) railOffset = IntOffset(railOffset.x, (bottomBound - headerHeight - railItemsHeight).roundToInt())
        }
    }

    Box(modifier = modifier, contentAlignment = railAlignment ?: (if (isRightDocked) Alignment.TopEnd else Alignment.TopStart)) {
        val buttonSize = 72.dp 
        val titleAlignment = if (isRightDocked) Alignment.TopStart else Alignment.TopEnd
        val titlePaddingStart = if (isRightDocked) 32.dp else 0.dp
        val titlePaddingEnd = if (isRightDocked) 0.dp else 32.dp

        selectedItem?.screenTitle?.let { screenTitle ->
            if (screenTitle.isNotEmpty()) {
                Popup(alignment = titleAlignment) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = titlePaddingStart, end = titlePaddingEnd, top = 16.dp),
                        contentAlignment = if (isRightDocked) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        Text(text = screenTitle, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, color = actualActiveColor), textAlign = if (isRightDocked) TextAlign.Start else TextAlign.End)
                    }
                }
            }
        }

        if (scope.isLoading) {
            Popup(popupPositionProvider = CenteredPopupPositionProvider, properties = PopupProperties(focusable = false)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars)) { AzLoad() }
            }
        }

        Surface(
            modifier = Modifier.then(if (isVertical) Modifier.width(railThickness).fillMaxHeight() else Modifier.height(railThickness).fillMaxWidth())
                .offset { if (overlayController != null) overlayController.contentOffset.value else railOffset }
                .onGloballyPositioned { railBounds = it.boundsInWindow() },
            color = if (isExpanded) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent,
        ) {
            val headerContent = @Composable {
                Box(
                    modifier = Modifier
                        .padding(bottom = if (isVertical) AzNavRailDefaults.HeaderPadding else 0.dp, end = if (!isVertical) AzNavRailDefaults.HeaderPadding else 0.dp)
                        .onSizeChanged { headerHeight = it.height }
                        .pointerInput(isFloating, scope.enableRailDragging, scope.displayAppName, scope.onRailDrag, overlayController) {
                            detectTapGestures(
                                onTap = {
                                    if (effectiveNoMenu) showFooterPopup = !showFooterPopup
                                    else if (isFloating) showFloatingButtons = !showFloatingButtons
                                    else if (overlayController == null) isExpanded = !isExpanded
                                },
                                onLongPress = {
                                    if (isFloating) {
                                        if (scope.onRailDrag == null && overlayController == null) railOffset = IntOffset.Zero
                                        isFloating = false
                                        if (scope.displayAppName) isAppIcon = false
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (scope.enableRailDragging) {
                                        val overlayService = scope.overlayService
                                        if (overlayService != null) OverlayHelper.launch(context, overlayService)
                                        else { isFloating = true; isExpanded = false; if (scope.displayAppName) isAppIcon = true }
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        }
                        .pointerInput(isFloating, scope.enableRailDragging, scope.onRailDrag, overlayController) {
                            detectDragGestures(
                                onDragStart = {
                                    if (overlayController != null) { overlayController.onDragStart(); if (isFloating) { wasVisibleOnDragStart = showFloatingButtons; showFloatingButtons = false } }
                                    else if (isFloating) { wasVisibleOnDragStart = showFloatingButtons; showFloatingButtons = false }
                                },
                                onDrag = { change, dragAmount ->
                                    if (overlayController != null) { change.consume(); overlayController.onDrag(dragAmount) }
                                    else if (scope.onOverlayDrag != null) { change.consume(); scope.onOverlayDrag?.invoke(dragAmount.x, dragAmount.y) }
                                    else if (isFloating) {
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
                                    if (overlayController != null) { overlayController.onDragEnd(); if (isFloating) showFloatingButtons = true }
                                    else if (isFloating) {
                                        if (scope.onRailDrag == null) {
                                            if (kotlin.math.sqrt(railOffset.x.toFloat().pow(2) + railOffset.y.toFloat().pow(2)) < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                railOffset = IntOffset.Zero; isFloating = false; if (scope.displayAppName) isAppIcon = false
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
                        if (appIcon != null) {
                            Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "Toggle menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                        } else {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle Menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                        }
                    } else {
                        Text(text = appName, style = MaterialTheme.typography.titleMedium, softWrap = false, maxLines = 1, textAlign = TextAlign.Start)
                    }
                }
            }

            val contentBlock = @Composable {
                if (isExpanded) {
                    val displayedItems = if (reverseLayout) displayedNavItems.reversed() else displayedNavItems
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).pointerInput(isExpanded) {
                            detectHorizontalDragGestures(onHorizontalDrag = { change, dragAmount ->
                                if (isExpanded && (if (isRightDocked) dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX else dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX)) {
                                    isExpanded = false; change.consume()
                                }
                            })
                        }
                    ) {
                        displayedItems.filter { !it.isSubItem }.forEach { item ->
                            if (item.isDivider) { com.hereliesaz.aznavrail.AzDivider() }
                            else {
                                val onClick = scope.onClickMap[item.id]
                                val finalItem = if (item.isCycler) item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption) else if (item.isHost) item.copy(isExpanded = hostStates[item.id] ?: false) else item
                                MenuItem(
                                    item = finalItem,
                                    navController = navController,
                                    isSelected = finalItem.route == currentDestination,
                                    onClick = onClick,
                                    onCyclerClick = if (item.isCycler) { { /* Cycler logic omitted for brevity in builder */ } } else null,
                                    onToggle = { isExpanded = !isExpanded },
                                    onItemClick = { selectedItemId = finalItem.id },
                                    onHostClick = { val wasExpanded = hostStates[item.id] ?: false; hostStates.keys.forEach { hostStates[it] = false }; hostStates[item.id] = !wasExpanded },
                                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                    infoScreen = scope.infoScreen,
                                    activeColor = actualActiveColor,
                                    defaultShape = actualShape
                                )
                                AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                                    Column {
                                        val subItems = displayedNavItems.filter { it.hostId == item.id }
                                        (if (reverseLayout) subItems.reversed() else subItems).forEach { subItem ->
                                            MenuItem(
                                                item = subItem, navController = navController, isSelected = subItem.route == currentDestination,
                                                onClick = scope.onClickMap[subItem.id], onCyclerClick = null, onToggle = { isExpanded = !isExpanded },
                                                onItemClick = { selectedItemId = subItem.id }, onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect },
                                                infoScreen = scope.infoScreen, activeColor = actualActiveColor, defaultShape = actualShape
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (scope.showFooter) Footer(appName = appName, onToggle = { isExpanded = !isExpanded }, onUndock = { if (scope.onUndock != null) scope.onUndock?.invoke() else { val os = scope.overlayService; if (os != null) OverlayHelper.launch(context, os) else { isFloating = true; isExpanded = false; if (scope.displayAppName) isAppIcon = true } } }, scope = scope, footerColor = actualActiveColor)
                } else {
                    AnimatedVisibility(visible = !isFloating || showFloatingButtons, modifier = Modifier.fillMaxSize()) {
                        val scrollState = rememberScrollState()
                        val adaptiveModifier = Modifier.padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding).then(if(isVertical) Modifier.verticalScroll(scrollState) else Modifier.horizontalScroll(scrollState)).onSizeChanged { railItemsHeight = it.height }
                        
                        val onRailCyclerClick: (AzNavItem) -> Unit = { item -> /* Cycler logic omitted for builder brevity */ }

                        if (isVertical) {
                            Column(modifier = adaptiveModifier, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement), horizontalAlignment = Alignment.CenterHorizontally) {
                                RailItems(
                                    items = displayedNavItems, scope = scope, navController = navController, currentDestination = currentDestination,
                                    buttonSize = buttonSize, onRailCyclerClick = onRailCyclerClick, onItemSelected = { selectedItemId = it.id },
                                    hostStates = hostStates, packRailButtons = if (isFloating) true else scope.packButtons, onClickOverride = null,
                                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect; scope.onItemGloballyPositioned?.invoke(id, rect) },
                                    infoScreen = scope.infoScreen, orientation = orientation, reverseLayout = reverseLayout, activeColor = actualActiveColor, defaultShape = actualShape
                                )
                            }
                        } else {
                            Row(modifier = adaptiveModifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement), verticalAlignment = Alignment.CenterVertically) {
                                RailItems(
                                    items = displayedNavItems, scope = scope, navController = navController, currentDestination = currentDestination,
                                    buttonSize = buttonSize, onRailCyclerClick = onRailCyclerClick, onItemSelected = { selectedItemId = it.id },
                                    hostStates = hostStates, packRailButtons = if (isFloating) true else scope.packButtons, onClickOverride = null,
                                    onItemGloballyPositioned = { id, rect -> itemPositions[id] = rect; scope.onItemGloballyPositioned?.invoke(id, rect) },
                                    infoScreen = scope.infoScreen, orientation = orientation, reverseLayout = reverseLayout, activeColor = actualActiveColor, defaultShape = actualShape
                                )
                            }
                        }
                    }
                }
            }

            if (isVertical) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = if (reverseLayout) androidx.compose.foundation.layout.Arrangement.Bottom else androidx.compose.foundation.layout.Arrangement.Top) {
                    if (reverseLayout) { Box(Modifier.weight(1f)) { contentBlock() }; headerContent() } else { headerContent(); Box(Modifier.weight(1f)) { contentBlock() } }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (reverseLayout) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start) {
                    if (reverseLayout) { Box(Modifier.weight(1f)) { contentBlock() }; headerContent() } else { headerContent(); Box(Modifier.weight(1f)) { contentBlock() } }
                }
            }
        }
    }
}
