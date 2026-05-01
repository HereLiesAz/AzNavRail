// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.tutorial.AzTutorialOverlay
import com.hereliesaz.aznavrail.tutorial.LocalAzTutorialController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Annotation marking that the [AzNavRail] composable must be used within an [AzHostActivityLayout].
 *
 * This enforces strict layout rules (safe zones, padding, z-ordering) and ensures that the
 * navigation rail behaves correctly within the application structure. Instantiating [AzNavRail]
 * directly without the host wrapper will result in a runtime error or a visual warning.
 */
@RequiresOptIn(message = "AzNavRail must be used within AzHostActivityLayout to ensure safe zones and proper behavior.")
@Retention(AnnotationRetention.BINARY)
annotation class AzStrictLayout

/** Singleton holding library-level constants. */
object AzNavRail {
    /** Intent extra key used to pass a target route when launching an activity via the rail. */
    const val EXTRA_ROUTE = "com.hereliesaz.aznavrail.extra.ROUTE"
}

/**
 * The core composable for the AzNavRail navigation system.
 *
 * This component renders a vertical navigation rail that can expand into a full drawer menu.
 * It supports advanced features like FAB mode (draggable rail), cyclers, toggles, hierarchical
 * navigation, and strict layout enforcement via [AzHostActivityLayout].
 *
 * **Note:** This composable is marked with [AzStrictLayout] and should primarily be used via
 * the [AzHostActivityLayout] wrapper, which handles placement and safe zones automatically.
 *
 * @param modifier The modifier to be applied to the rail container.
 * @param navController The [NavHostController] used for navigation. If null, a new one is created.
 * @param currentDestination The current route destination. If null, it is derived from the [navController].
 * @param isLandscape Explicitly set the orientation mode. If null, it is derived from configuration.
 * @param initiallyExpanded Whether the rail should be expanded (show menu) by default.
 * @param disableSwipeToOpen Whether the swipe gesture to open the menu is disabled.
 * @param providedScope An optional pre-configured [AzNavRailScopeImpl]. Used internally by [AzHostActivityLayout].
 * @param orientation The orientation of the rail (Vertical or Horizontal). Default is Vertical.
 * @param visualDockingSide The side of the screen where the rail is visually docked (Left or Right).
 * @param railAlignment The alignment of the rail within its container.
 * @param reverseLayout Whether to reverse the layout direction (e.g., for Right-to-Left languages or right docking).
 * @param content The configuration block for the rail, defined using the [AzNavRailScope] DSL.
 */
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
        val errorMessage = """
            CRITICAL ERROR: AzNavRail invoked without AzHostActivityLayout!
            
            AzNavRail enforces strict layout rules for safe zones, rotation, and docking.
            It MUST be wrapped in an AzHostActivityLayout.
            
            Correct Usage:
            AzHostActivityLayout(navController = ...) {
                // Your AzNavRail content here
            }
            
            Or ensure you are using the generated AzGraph system.
        """.trimIndent()

        // Log the error for debugging
        AzNavRailLogger.e("AzNavRail", errorMessage)

        // Visual Error Feedback
        Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AzNavRail Configuration Error",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Must be used inside AzHostActivityLayout.",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Check Logcat for details.",
                    color = Color.Yellow,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        // Strictly throw in debug builds or if desired
        // throw IllegalStateException(errorMessage)
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
    var showHelpOverlay by remember { mutableStateOf(false) }
    val tutorialController = LocalAzTutorialController.current
    val activeTutorialId by tutorialController.activeTutorialId
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val onSecretClick = SecretScreens(secLoc = scope.advancedConfig.secLoc, secLocPort = scope.advancedConfig.secLocPort)

    val isVerticalNestedRailOpen by remember {
        derivedStateOf {
            scope.nestedRailOpenId?.let { id ->
                scope.navItems.any { it.id == id && it.nestedRailAlignment == AzNestedRailAlignment.VERTICAL }
            } ?: false
        }
    }

    // Shrink button size and rail width further when a vertical nested rail is open
    val activeButtonSize = if (isVerticalNestedRailOpen) AzNavRailDefaults.ShrunkButtonWidth else AzNavRailDefaults.ButtonWidth

    val targetRailWidth = if (isExpanded) {
        scope.expandedWidth
    } else if (isVerticalNestedRailOpen) {
        60.dp
    } else {
        scope.collapsedWidth
    }

    val railWidth by animateDpAsState(targetValue = targetRailWidth)

    val effectiveNavController = navController ?: rememberNavController()
    val navBackStackEntry by effectiveNavController.currentBackStackEntryAsState()
    val actualCurrentDestination = currentDestination ?: navBackStackEntry?.destination?.route
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    val toggleHelpOverlay = remember(scope) {
        { itemId: String? ->
            if (itemId != null && scope.advancedConfig.tutorials.containsKey(itemId)) {
                tutorialController.startTutorial(itemId)
                showHelpOverlay = false // ensure help overlay is closed
            } else {
                if (showHelpOverlay) {
                    showHelpOverlay = false
                    scope.advancedConfig.onDismissHelp?.invoke()
                } else {
                    showHelpOverlay = true
                }
            }
        }
    }

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
        if (!showHelpOverlay) {
            if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else if (!scope.noMenu) {
                isExpanded = !isExpanded
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val sizeModifier = if (isFloating) {
        // Enforce max height/width in FAB mode to ensure it fits within 10-90% safe zone
        val maxFabSize = (configuration.screenHeightDp * 0.8f).dp
        if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).heightIn(max = maxFabSize).wrapContentHeight()
        else Modifier.height(railWidth).widthIn(max = maxFabSize).wrapContentWidth()
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
    val surfaceColor = Color.Transparent
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
                                offsetY += dragAmount.y
                                val minY = screenHeightPx * 0.1f
                                val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
                                offsetY = offsetY.coerceIn(minY, maxY)

                                val minX = 0f
                                val maxX = (configuration.screenWidthDp * density.density) - railWidth.toPx()
                                offsetX = offsetX.coerceIn(minX, maxX)
                            } else {
                                if (scope.advancedConfig.enableRailDragging && kotlin.math.abs(dragAmount.y) > 20 && kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
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
                        .fillMaxWidth() // Center alignment requires full width
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { toggleExpanded() },
                                onLongPress = {
                                    if (scope.advancedConfig.enableRailDragging) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // Center content horizontally
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
                            modifier = Modifier.size(minOf(100.dp, targetRailWidth)),
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
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    if (isExpanded) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                        ) {
                            val hasExplicitHelpItem = scope.navItems.any { it.isHelpItem || it.id == AzNavRailDefaults.AUTO_HELP_ID }
                            val displayItems = if (scope.advancedConfig.helpEnabled && !hasExplicitHelpItem) {
                                scope.navItems + AzNavItem.Help(
                                    id = AzNavRailDefaults.AUTO_HELP_ID,
                                    isRailItem = false
                                )
                            } else {
                                scope.navItems
                            }

                            displayItems.forEach { item ->
                                if (!item.isSubItem) {
                                    MenuItem(
                                        item = item,
                                        navController = effectiveNavController,
                                        isSelected = (item.route != null && item.route == actualCurrentDestination) ||
                                                item.classifiers.any { scope.activeClassifiers.contains(it) },
                                        onClick = {
                                            if (item.isHelpItem) {
                                                toggleHelpOverlay(item.id)
                                            } else {
                                                scope.onClickMap[item.id]?.invoke()
                                            }
                                            if (item.collapseOnClick) isExpanded = false
                                        },
                                        onCyclerClick = { scope.onClickMap[item.id]?.invoke() },
                                        onToggle = { if (item.collapseOnClick) isExpanded = false },
                                        onItemClick = { if (item.collapseOnClick) isExpanded = false },
                                        onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
                                        onItemGloballyPositioned = { id, bounds ->
                                            scope.itemBoundsCache[id] = bounds
                                            scope.advancedConfig.onItemGloballyPositioned?.invoke(id, bounds)
                                        },
                                        helpEnabled = showHelpOverlay,
                                        activeColor = scope.activeColor
                                    )

                                    if (hostStates[item.id] == true) {
                                        displayItems.forEach { subItem ->
                                            if (subItem.isSubItem && subItem.hostId == item.id) {
                                                MenuItem(
                                                    item = subItem,
                                                    navController = effectiveNavController,
                                                    isSelected = (subItem.route != null && subItem.route == actualCurrentDestination) ||
                                                            subItem.classifiers.any { scope.activeClassifiers.contains(it) },
                                                    onClick = {
                                                        if (subItem.isHelpItem) {
                                                            toggleHelpOverlay(subItem.id)
                                                        } else {
                                                            scope.onClickMap[subItem.id]?.invoke()
                                                        }
                                                        if (subItem.collapseOnClick) isExpanded = false
                                                    },
                                                    onCyclerClick = { scope.onClickMap[subItem.id]?.invoke() },
                                                    onToggle = { if (subItem.collapseOnClick) isExpanded = false },
                                                    onItemClick = { if (subItem.collapseOnClick) isExpanded = false },
                                                    onItemGloballyPositioned = { id, bounds ->
                                                        scope.itemBoundsCache[id] = bounds
                                                        scope.advancedConfig.onItemGloballyPositioned?.invoke(id, bounds)
                                                    },
                                                    helpEnabled = showHelpOverlay,
                                                    activeColor = scope.activeColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Calculate total height of items
                        // We only include sub-items if they belong to a standard HostItem that expands inline.
                        // Sub-items belonging to a NestedRail (which is a popup) should NOT be included in the main rail's height.
                        val isItemVisible = { item: AzNavItem ->
                            if (!item.isRailItem) false
                            else if (!item.isSubItem) true
                            else {
                                val host = scope.navItems.find { it.id == item.hostId }
                                val isHostExpanded = hostStates[item.hostId] == true
                                val isHostNestedRail = host?.isNestedRail == true
                                isHostExpanded && !isHostNestedRail
                            }
                        }

                        val totalItemHeight = scope.navItems.filter(isItemVisible).sumOf {
                            (activeButtonSize.value + (if(scope.packButtons || isFloating) 0f else AzNavRailDefaults.RailContentVerticalArrangement.value)).toDouble()
                        }.dp

                        val availableHeight = maxHeight
                        val isScrollable = totalItemHeight > (availableHeight * 0.65f)

                        val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

                        Column(
                            modifier = Modifier
                                .fillMaxWidth() // Ensure column takes full width for centering
                                .then(scrollModifier),
                            horizontalAlignment = Alignment.CenterHorizontally // Center buttons horizontally
                        ) {
                            RailItems(
                                items = scope.navItems,
                                scope = scope,
                                navController = effectiveNavController,
                                currentDestination = actualCurrentDestination,
                                buttonSize = activeButtonSize,
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
                                    if (item.isHelpItem) {
                                        toggleHelpOverlay(item.id)
                                    }
                                    if (item.collapseOnClick && !scope.noMenu) isExpanded = false
                                },
                                hostStates = hostStates,
                                packRailButtons = isFloating || scope.packButtons, // Forced pack in FAB mode
                                visualDockingSide = visualDockingSide,
                                onItemGloballyPositioned = scope.advancedConfig.onItemGloballyPositioned,
                                helpEnabled = showHelpOverlay
                            )
                        }
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
                                scope.advancedConfig.onUndock?.invoke()
                            },
                            onSecretClick = onSecretClick,
                            scope = scope,
                            footerColor = scope.activeColor
                        )
                    }
                }
            }
        }
    }

    if (showHelpOverlay) {
        HelpOverlay(
            items = scope.navItems,
            helpLineColors = scope.helpLineColors,
            onDismiss = { toggleHelpOverlay(null) },
            itemBoundsCache = scope.itemBoundsCache,
            helpList = scope.advancedConfig.helpList,
            nestedRailOpenId = scope.nestedRailOpenId,
            tutorials = scope.advancedConfig.tutorials,
            onTutorialLaunch = { toggleHelpOverlay(it) }
        )
    }

    activeTutorialId?.let { tutorialId ->
        scope.advancedConfig.tutorials[tutorialId]?.let { tutorial ->
            AzTutorialOverlay(
                tutorialId = tutorialId,
                tutorial = tutorial,
                onDismiss = { tutorialController.endTutorial() },
                itemBoundsCache = scope.itemBoundsCache
            )
        }
    }
}
