// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@RequiresOptIn(message = "AzNavRail must be used within AzHostActivityLayout to ensure safe zones and proper behavior.")
@Retention(AnnotationRetention.BINARY)
annotation class AzStrictLayout

object AzNavRail {
    const val EXTRA_ROUTE = "com.hereliesaz.aznavrail.extra.ROUTE"
}

@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    providedScope: AzNavRailScopeImpl? = null,
    orientation: AzOrientation = AzOrientation.Vertical,
    visualDockingSide: AzDockingSide = AzDockingSide.LEFT,
    railAlignment: Alignment = Alignment.TopStart,
    reverseLayout: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val isHostPresent = LocalAzNavHostPresent.current
    if (!isHostPresent) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
            Text(
                "AzNavRail Error: Must be used inside AzHostActivityLayout",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) scope.reset()
    scope.apply(content)

    val packageManager = context.packageManager
    val packageName = context.packageName
    val appName = remember(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            "App"
        }
    }
    val appIcon = remember(packageName) {
        try { packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    var isExpanded by remember { mutableStateOf(if (scope.noMenu) false else isExpandedInternal) }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var railContentHeight by remember { mutableStateOf(0f) }
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }

    val railWidth by animateDpAsState(targetValue = if (isExpanded) scope.expandedWidth else scope.collapsedWidth)

    val effectiveNavController = navController ?: rememberNavController()
    val navBackStackEntry by effectiveNavController.currentBackStackEntryAsState()
    val actualCurrentDestination = currentDestination ?: navBackStackEntry?.destination?.route
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(scope.navItems) {
        scope.navItems.forEach { item ->
            if (item.isCycler) cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
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
                            val options = item.options ?: emptyList()
                            val currentIndexInVm = options.indexOf(item.selectedOption)
                            val targetIndex = options.indexOf(state.displayedOption)
                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                val onClick = scope.onClickMap[item.id]
                                if (onClick != null) repeat(clicksToCatchUp) { onClick() }
                            }
                            cyclerStates[id] = state.copy(job = null)
                        }
                    }
                }
            }
        }
    }

    fun toggleExpanded() {
        if (!scope.infoScreen) {
            if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else if (!scope.noMenu) {
                isExpanded = !isExpanded
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val sizeModifier = if (isFloating) {
        if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).wrapContentHeight()
        else Modifier.height(railWidth).wrapContentWidth()
    } else {
        if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).fillMaxHeight()
        else Modifier.height(railWidth).fillMaxWidth()
    }

    // Top 10% to Bottom 10% bounds rule enforced.
    val safeTopDp = (configuration.screenHeightDp * 0.1f).dp
    val safeBottomDp = (configuration.screenHeightDp * 0.1f).dp
    val safeZoneModifier = if (!isFloating && orientation == AzOrientation.Vertical) {
        Modifier.padding(top = safeTopDp, bottom = safeBottomDp)
    } else { Modifier }

    // No background shape when collapsed. Drawer visible only when expanded.
    val surfaceColor = if (isExpanded && !isFloating) MaterialTheme.colorScheme.surface else Color.Transparent
    val surfaceElevation = if (isExpanded && !isFloating) 2.dp else 0.dp

    Box(
        modifier = modifier,
        contentAlignment = if (isFloating) Alignment.TopStart else railAlignment
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(safeZoneModifier)
                .then(sizeModifier)
                .onGloballyPositioned { if (isFloating) railContentHeight = it.size.height.toFloat() }
                .pointerInput(isFloating, disableSwipeToOpen, visualDockingSide) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isFloating) {
                                offsetX += dragAmount.x
                                val minY = screenHeightPx * 0.1f
                                val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
                                offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                            } else {
                                if (scope.enableRailDragging && kotlin.math.abs(dragAmount.y) > 20 && kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                                    isFloating = true
                                    isExpanded = false
                                    offsetX = 0f
                                    offsetY = screenHeightPx * 0.1f
                                    if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (!disableSwipeToOpen && !scope.noMenu) {
                                    if (visualDockingSide == AzDockingSide.LEFT) {
                                        if (dragAmount.x > 20 && !isExpanded) isExpanded = true
                                        else if (dragAmount.x < -20 && isExpanded) isExpanded = false
                                    } else {
                                        if (dragAmount.x < -20 && !isExpanded) isExpanded = true
                                        else if (dragAmount.x > 20 && isExpanded) isExpanded = false
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (isFloating && (offsetX*offsetX + offsetY*offsetY < AzNavRailDefaults.SNAP_BACK_RADIUS_PX*AzNavRailDefaults.SNAP_BACK_RADIUS_PX)) {
                                isFloating = false
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    )
                }
                .then(if (isFloating) Modifier.shadow(8.dp, RectangleShape) else Modifier),
            color = surfaceColor,
            tonalElevation = surfaceElevation
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header handles Unconstrained App Name -> App Icon transformation logic
                Row(
                    modifier = Modifier
                        .padding(AzNavRailDefaults.HeaderPadding)
                        .height(AzNavRailDefaults.HeaderHeightDp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { toggleExpanded() },
                                onLongPress = {
                                    if (scope.enableRailDragging) {
                                        isFloating = !isFloating
                                        if (isFloating) {
                                            isExpanded = false
                                            offsetY = screenHeightPx * 0.1f
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (scope.displayAppName && !isFloating) {
                        // Unconstrained bleeding text
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.requiredWidth(1000.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    } else {
                        // Reverts to identical App Icon logic
                        Box(
                            modifier = Modifier.size(AzNavRailDefaults.ButtonWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            if (appIcon != null) {
                                val baseModifier = Modifier.fillMaxSize()
                                val clipModifier = when (scope.headerIconShape) {
                                    AzHeaderIconShape.CIRCLE -> baseModifier.clip(CircleShape)
                                    AzHeaderIconShape.ROUNDED -> baseModifier.clip(RoundedCornerShape(12.dp))
                                    else -> baseModifier
                                }
                                Image(painter = rememberAsyncImagePainter(appIcon), contentDescription = "App Icon", modifier = clipModifier)
                            } else {
                                Icon(Icons.Default.Menu, "Menu")
                            }
                        }
                    }
                }

                if (isFloating && !showFloatingButtons) return@Column

                // MAIN CONTENT and MENU separation
                Box(modifier = Modifier.weight(1f)) {
                    if (isExpanded) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                        ) {
                            scope.navItems.filter { !it.isSubItem }.forEach { item ->
                                MenuItem(
                                    item = item,
                                    navController = effectiveNavController,
                                    isSelected = (item.route != null && item.route == actualCurrentDestination) ||
                                            item.classifiers.any { it in scope.activeClassifiers },
                                    onClick = {
                                        scope.onClickMap[item.id]?.invoke()
                                        if (item.collapseOnClick) isExpanded = false
                                    },
                                    onCyclerClick = { scope.onClickMap[item.id]?.invoke() },
                                    onToggle = { if (item.collapseOnClick) isExpanded = false },
                                    onItemClick = { if (item.collapseOnClick) isExpanded = false },
                                    onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
                                    onItemGloballyPositioned = scope.onItemGloballyPositioned,
                                    infoScreen = scope.infoScreen,
                                    activeColor = scope.activeColor
                                )

                                if (hostStates[item.id] == true) {
                                    scope.navItems.filter { it.isSubItem && it.hostId == item.id }.forEach { subItem ->
                                        MenuItem(
                                            item = subItem,
                                            navController = effectiveNavController,
                                            isSelected = (subItem.route != null && subItem.route == actualCurrentDestination) ||
                                                    subItem.classifiers.any { it in scope.activeClassifiers },
                                            onClick = {
                                                scope.onClickMap[subItem.id]?.invoke()
                                                if (subItem.collapseOnClick) isExpanded = false
                                            },
                                            onCyclerClick = { scope.onClickMap[subItem.id]?.invoke() },
                                            onToggle = { if (subItem.collapseOnClick) isExpanded = false },
                                            onItemClick = { if (subItem.collapseOnClick) isExpanded = false },
                                            onItemGloballyPositioned = scope.onItemGloballyPositioned,
                                            infoScreen = scope.infoScreen,
                                            activeColor = scope.activeColor
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        RailItems(
                            items = scope.navItems,
                            scope = scope,
                            navController = effectiveNavController,
                            currentDestination = actualCurrentDestination,
                            buttonSize = AzNavRailDefaults.ButtonWidth,
                            onRailCyclerClick = { item ->
                                val state = cyclerStates[item.id]
                                if (state != null && !item.disabled) {
                                    state.job?.cancel()
                                    val options = item.options ?: emptyList()
                                    val enabledOptions = options.filterNot { it in (item.disabledOptions ?: emptyList()) }
                                    if (enabledOptions.isNotEmpty()) {
                                        val currentIndex = enabledOptions.indexOf(state.displayedOption)
                                        val nextOption = enabledOptions[(currentIndex + 1) % enabledOptions.size]

                                        cyclerStates[item.id] = state.copy(
                                            displayedOption = nextOption,
                                            job = coroutineScope.launch {
                                                delay(1000L)
                                                scope.onClickMap[item.id]?.invoke()
                                                cyclerStates[item.id] = cyclerStates[item.id]?.copy(job = null) ?: state
                                            }
                                        )
                                    }
                                }
                            },
                            onItemSelected = { item ->
                                if (item.collapseOnClick && !scope.noMenu) isExpanded = false
                            },
                            hostStates = hostStates,
                            packRailButtons = isFloating || scope.packButtons, // Forced pack in FAB mode
                            visualDockingSide = visualDockingSide,
                            onItemGloballyPositioned = scope.onItemGloballyPositioned,
                            infoScreen = scope.infoScreen
                        )
                    }
                }

                // FIXED FOOTER (Does not scroll, pinned below menu)
                if (scope.showFooter && isExpanded) {
                    Column {
                        AzDivider()
                        Footer(
                            appName = appName,
                            onToggle = { toggleExpanded() },
                            onUndock = {
                                isFloating = true
                                isExpanded = false
                                offsetY = screenHeightPx * 0.1f
                                scope.onUndock?.invoke()
                            },
                            scope = scope,
                            footerColor = scope.activeColor
                        )
                    }
                }
            }
        }
    }

    if (scope.infoScreen) HelpOverlay(items = scope.navItems, onDismiss = { scope.onDismissInfoScreen?.invoke() })
}