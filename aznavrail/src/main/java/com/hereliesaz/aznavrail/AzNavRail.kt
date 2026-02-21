package com.hereliesaz.aznavrail

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.hereliesaz.aznavrail.internal.*
import com.hereliesaz.aznavrail.model.*
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
    orientation: AzOrientation = AzOrientation.Vertical,
    visualDockingSide: AzDockingSide? = null,
    railAlignment: Alignment? = null,
    reverseLayout: Boolean = false,
    content: AzNavRailScope.() -> Unit = {}
) {
    val isHostPresent = LocalAzNavHostPresent.current
    val overlayController = LocalAzNavRailOverlayController.current

    if (!isHostPresent && overlayController == null) {
        error("FATAL LAYOUT VIOLATION: AzNavRail instantiated without AzHostActivityLayout.")
    }

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) scope.reset()
    navController?.let { scope.navController = it }
    scope.apply(content)

    var isFloating by remember { mutableStateOf(overlayController != null) }
    val effectiveNoMenu = scope.noMenu && overlayController == null && !isFloating
    val effectiveDockingSide = visualDockingSide ?: scope.dockingSide
    val isRightDocked = effectiveDockingSide == AzDockingSide.RIGHT

    val displayedNavItems = remember(scope.navItems, effectiveNoMenu) {
        if (effectiveNoMenu) scope.navItems.map { it.copy(isRailItem = true) } else scope.navItems
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = remember(packageName) {
        try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString() } catch (e: Exception) { "App" }
    }
    val appIcon = remember(packageName) {
        try { packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }
    }

    val footerColor = remember(displayedNavItems) { displayedNavItems.firstOrNull()?.color ?: Color.Unspecified }

    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val finalNavItems = remember(displayedNavItems, cyclerStates.toMap()) {
        displayedNavItems.map { item ->
            if (item.isCycler) item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption) else item
        }
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    var isExpanded by remember(overlayController, effectiveNoMenu) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = if (overlayController != null || effectiveNoMenu) false else isExpandedInternal
                set(v) { if (overlayController == null && !effectiveNoMenu) isExpandedInternal = v }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }

    var showFooterPopup by remember { mutableStateOf(false) }
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }

    var showFloatingButtons by remember { mutableStateOf(false) }
    var wasVisibleOnDragStart by remember { mutableStateOf(false) }

    var isAppIcon by remember(isFloating, scope.displayAppName) {
        mutableStateOf(if (isFloating) true else !scope.displayAppName)
    }

    var headerHeight by remember { mutableStateOf(0) }
    val hapticFeedback = LocalHapticFeedback.current
    val isVertical = orientation == AzOrientation.Vertical

    val railThickness by animateDpAsState(targetValue = if (isExpanded) scope.expandedWidth else scope.collapsedWidth, label = "railThickness")
    val coroutineScope = rememberCoroutineScope()
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    val closeMenuAction = { if (isExpanded) isExpanded = false }
    val currentContentOffset = overlayController?.contentOffset?.value ?: railOffset

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
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isExpanded = false })
                    }
            )
        }

        val safeZones = LocalAzSafeZones.current

        Box(
            modifier = Modifier
                .then(if (isVertical) Modifier.fillMaxHeight().width(railThickness) else Modifier.fillMaxWidth().height(railThickness))
                .offset { currentContentOffset }
                .align(railAlignment ?: if (isRightDocked) Alignment.TopEnd else Alignment.TopStart)
                .then(if (isVertical) Modifier.padding(top = safeZones.top, bottom = safeZones.bottom) else Modifier.padding(start = safeZones.top, end = safeZones.bottom))
                .then(if (isFloating) Modifier.wrapContentSize(Alignment.TopStart) else Modifier)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AzNavRailDefaults.HeaderHeightDp)
                        .onSizeChanged { headerHeight = it.height }
                        .pointerInput(isFloating) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    if (!isFloating && (dragAmount > AzNavRailDefaults.SWIPE_THRESHOLD_PX || dragAmount < -AzNavRailDefaults.SWIPE_THRESHOLD_PX)) {
                                        if (scope.enableRailDragging) {
                                            if (scope.overlayService != null) OverlayHelper.launch(context, scope.overlayService!!)
                                            else {
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
                                onDragStart = {
                                    if (overlayController == null && !isFloating && scope.enableRailDragging) {
                                        if (scope.overlayService != null) OverlayHelper.launch(context, scope.overlayService!!)
                                        else {
                                            isFloating = true
                                            isExpanded = false
                                            if (scope.displayAppName) isAppIcon = true
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                    if (overlayController != null) {
                                        overlayController.onDragStart()
                                        if (isFloating) { wasVisibleOnDragStart = showFloatingButtons; showFloatingButtons = false }
                                    } else if (isFloating) {
                                        wasVisibleOnDragStart = showFloatingButtons
                                        showFloatingButtons = false
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (overlayController != null) overlayController.onDrag(dragAmount)
                                    else if (scope.onOverlayDrag != null) scope.onOverlayDrag?.invoke(dragAmount.x, dragAmount.y)
                                    else if (isFloating) {
                                        if (scope.onRailDrag != null) scope.onRailDrag?.invoke(dragAmount.x, dragAmount.y)
                                        else {
                                            railOffset = IntOffset(railOffset.x + dragAmount.x.roundToInt(), railOffset.y + dragAmount.y.roundToInt())
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
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (isFloating) showFloatingButtons = !showFloatingButtons
                                    else if (!effectiveNoMenu) {
                                        if (scope.vibrate) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpanded = !isExpanded
                                    }
                                }
                            )
                        },
                    contentAlignment = if (isAppIcon) Alignment.Center else Alignment.CenterStart
                ) {
                    if (isAppIcon) {
                        if (appIcon != null) Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "Toggle menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                        else Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                    } else {
                        Text(text = appName, style = MaterialTheme.typography.titleMedium, softWrap = false, maxLines = 1, textAlign = TextAlign.Start, modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isExpanded) {
                        val displayedItems = if (reverseLayout) displayedNavItems.reversed() else displayedNavItems
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            displayedItems.filter { !it.isSubItem }.forEach { item ->
                                if (item.isDivider) AzDivider()
                                else MenuItem(
                                    item = item,
                                    subItems = displayedItems.filter { it.isSubItem && it.hostId == item.id },
                                    currentDestination = currentDestination,
                                    activeColor = scope.activeColor ?: MaterialTheme.colorScheme.primary,
                                    activeClassifiers = scope.activeClassifiers,
                                    hostStates = hostStates,
                                    onToggleHost = { id -> hostStates[id] = !(hostStates[id] ?: false) },
                                    onItemClick = {
                                        if (it.route != null && navController != null) navController.navigate(it.route)
                                        if (it.collapseOnClick) closeMenuAction()
                                    },
                                    onMenuCyclerClick = { },
                                    infoScreen = scope.infoScreen
                                )
                            }
                        }
                    } else if (!isFloating || showFloatingButtons) {
                        com.hereliesaz.aznavrail.internal.RailContent(
                            items = finalNavItems,
                            currentDestination = currentDestination,
                            activeColor = scope.activeColor ?: MaterialTheme.colorScheme.primary,
                            shape = scope.defaultShape,
                            activeClassifiers = scope.activeClassifiers,
                            scope = scope,
                            navController = navController,
                            onItemSelected = {
                                if (it.route != null && navController != null) navController.navigate(it.route)
                                if (it.collapseOnClick) closeMenuAction()
                            },
                            hostStates = hostStates,
                            packRailButtons = scope.packButtons,
                            orientation = orientation,
                            onItemGloballyPositioned = scope.onItemGloballyPositioned,
                            infoScreen = scope.infoScreen,
                            reverseLayout = reverseLayout,
                            isRightDocked = isRightDocked
                        )
                    }
                }
            }
        }
    }
}