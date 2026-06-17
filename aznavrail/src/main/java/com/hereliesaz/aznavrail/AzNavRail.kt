// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AboutOverlay
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.MoreFromAzOverlay
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzDropdownSource
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
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)) {
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
    if (providedScope == null) {
        scope.reset()
        scope.apply(content)
        scope.applyRelocReorders()
    }
    // When providedScope is non-null, AzHostActivityLayout already applied the DSL + reorders.

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

    var isExpanded by remember { mutableStateOf(if (scope.noMenu) false else initiallyExpanded) }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var railContentHeight by remember { mutableStateOf(0f) }
    var showHelpOverlay by remember { mutableStateOf(false) }
    // About reader + "More from Az" carousel overlays (drawn over the live UI like the help overlay).
    var showAboutOverlay by remember { mutableStateOf(false) }
    var showMoreFromAz by remember { mutableStateOf(false) }
    // When the help item that triggered the overlay lives inside a nested rail, this holds the
    // parent item's id so the overlay shows only that nested rail's cards. Null = main rail scope.
    var helpScopeId by remember { mutableStateOf<String?>(null) }
    var wasFloatingOpenBeforeDrag by remember { mutableStateOf(false) }
    val tutorialController = LocalAzTutorialController.current
    val activeTutorialId by tutorialController.activeTutorialId
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val onSecretClick = SecretScreens(secLoc = scope.advancedConfig.secLoc, secLocPort = scope.advancedConfig.secLocPort)

    LaunchedEffect(showFloatingButtons, railContentHeight) {
        if (isFloating) {
            val minY = screenHeightPx * 0.1f
            val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
            if (offsetY > maxY) {
                offsetY = maxY
                Log.d("AzNavRail", "FAB mode: Adjusted position to stay within 10-90% safe zone.")
            }
        }
    }

    val rotation = LocalView.current.display?.rotation ?: android.view.Surface.ROTATION_0
    val rotationDegrees = when (rotation) {
        android.view.Surface.ROTATION_90 -> 90f
        android.view.Surface.ROTATION_180 -> 180f
        android.view.Surface.ROTATION_270 -> 270f
        else -> 0f
    }

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
    // Tracks each host's last-seen `initiallyExpanded` flag so we auto-expand only on the rising edge
    // (host first appears, or its initiallyExpanded condition flips false -> true). After that the
    // user is free to collapse it; we won't fight them until the flag goes false then true again.
    val initiallyExpandedSeen = remember { mutableMapOf<String, Boolean>() }

    val toggleHelpOverlay = remember(scope) {
        { itemId: String? ->
            if (itemId != null && scope.advancedConfig.tutorials.containsKey(itemId)) {
                tutorialController.startTutorial(itemId)
                showHelpOverlay = false // ensure help overlay is closed
                helpScopeId = null
            } else {
                if (showHelpOverlay) {
                    showHelpOverlay = false
                    helpScopeId = null
                    scope.advancedConfig.onDismissHelp?.invoke()
                } else {
                    // Locate the tapped help item's parent nested rail (if any) so the overlay
                    // can scope its cards to that rail. A help item under a `azNestedRail { ... }`
                    // block lives in some parent's `nestedRailItems`; a main-rail help item lives
                    // directly in scope.navItems and resolves to scope=null.
                    helpScopeId = itemId?.let { id ->
                        scope.navItems.firstOrNull { parent ->
                            parent.isNestedRail && parent.nestedRailItems?.any { it.id == id } == true
                        }?.id
                    }
                    showHelpOverlay = true
                }
            }
        }
    }

    LaunchedEffect(scope.navItems) {
        scope.navItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
                scope.transientCyclerOptions.putIfAbsent(item.id, item.selectedOption ?: "")
            }
            if (item.isHost) {
                // Rising-edge auto-expand: expand when initiallyExpanded becomes true, then leave it alone.
                if (item.initiallyExpanded && initiallyExpandedSeen[item.id] != true) {
                    hostStates[item.id] = true
                }
                initiallyExpandedSeen[item.id] = item.initiallyExpanded
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

    // DROP-DOWN MENU MODE.
    //
    // When enabled, the rail abandons its docked side-strip identity entirely and behaves as a
    // single app-icon trigger anchored at the top (the icon replaces the hamburger). Tapping it
    // unfolds the chosen item set downward — reusing the same fold/unfold idea as FAB mode: a
    // single boolean gates whether the items render. There is no rail<->menu expansion, no
    // dragging, no swipes, no footer, no nested-rail popups, and no help overlay here; this branch
    // returns before any of that machinery is reached. `onscreen` content is widened to the full
    // screen by AzHostActivityLayout (it zeroes the rail-offset padding in this mode).
    if (scope.dropdownMenu) {
        var isDropdownOpen by remember { mutableStateOf(false) }
        val dropAlignment = if (visualDockingSide == AzDockingSide.RIGHT) Alignment.TopEnd else Alignment.TopStart
        val iconDiameter = if (scope.headerIconSize != androidx.compose.ui.unit.Dp.Unspecified) {
            scope.headerIconSize
        } else {
            minOf(100.dp, scope.collapsedWidth)
        }
        val maxPanelHeight = (configuration.screenHeightDp * 0.8f).dp

        // Shared rail-cycler debounce handler, identical to the docked rail's behaviour.
        val onDropdownCyclerClick: (AzNavItem) -> Unit = { item ->
            val state = cyclerStates[item.id]
            if (state != null && !item.disabled) {
                state.job?.cancel()
                val options = item.options ?: emptyList()
                val enabledOptions = options.filterNot { it in (item.disabledOptions ?: emptyList()) }
                if (enabledOptions.isNotEmpty()) {
                    val currentIndex = enabledOptions.indexOf(state.displayedOption)
                    val nextOption = enabledOptions[(currentIndex + 1) % enabledOptions.size]
                    scope.transientCyclerOptions[item.id] = nextOption
                    cyclerStates[item.id] = state.copy(
                        displayedOption = nextOption,
                        job = coroutineScope.launch {
                            delay(1000L)
                            scope.onClickMap[item.id]?.invoke()
                            cyclerStates[item.id] = cyclerStates[item.id]?.copy(job = null) ?: state
                        }
                    )
                }
                scope.advancedConfig.onInteraction?.invoke(item.id, item)
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            // Tap anywhere outside the open dropdown to fold it back up.
            if (isDropdownOpen) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                isDropdownOpen = false
                                Log.d("AzNavRail", "Dropdown: tap outside, folded up.")
                            })
                        }
                )
            }

            Column(
                modifier = Modifier
                    .align(dropAlignment)
                    .padding(AzNavRailDefaults.HeaderPadding),
                horizontalAlignment = if (visualDockingSide == AzDockingSide.RIGHT) Alignment.End else Alignment.Start
            ) {
                // App icon == hamburger trigger.
                Box(
                    modifier = Modifier
                        .size(iconDiameter)
                        .pointerInput(scope.vibrate) {
                            detectTapGestures(onTap = {
                                isDropdownOpen = !isDropdownOpen
                                if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Log.d(
                                    "AzNavRail",
                                    "Dropdown trigger tapped: ${if (isDropdownOpen) "unfolding" else "folding"} ${scope.dropdownSource} items."
                                )
                            })
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        val baseModifier = Modifier.fillMaxSize()
                        val clipModifier = when (scope.headerIconShape) {
                            AzHeaderIconShape.CIRCLE -> baseModifier.clip(CircleShape)
                            AzHeaderIconShape.ROUNDED -> baseModifier.clip(RoundedCornerShape(12.dp))
                            else -> baseModifier
                        }
                        Image(
                            painter = rememberAsyncImagePainter(appIcon),
                            contentDescription = "Menu",
                            modifier = clipModifier
                        )
                    } else {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }

                // The unfolded set — exactly one of the two renderings.
                if (isDropdownOpen) {
                    Surface(
                        color = if (scope.translucentBackground != Color.Unspecified) scope.translucentBackground else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 8.dp
                    ) {
                        val dropScroll = rememberScrollState()
                        if (scope.dropdownSource == AzDropdownSource.MENU) {
                            Column(
                                modifier = Modifier
                                    .width(scope.expandedWidth)
                                    .heightIn(max = maxPanelHeight)
                                    .verticalScroll(dropScroll)
                            ) {
                                scope.navItems.forEach { item ->
                                    if (!item.isSubItem) {
                                        MenuItemNode(
                                            item = item,
                                            allItems = scope.navItems,
                                            navController = effectiveNavController,
                                            currentDestination = actualCurrentDestination,
                                            scope = scope,
                                            hostStates = hostStates,
                                            showHelpOverlay = false,
                                            onToggleHelp = { },
                                            onCollapseMenu = { isDropdownOpen = false }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = maxPanelHeight)
                                    .verticalScroll(dropScroll),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RailItems(
                                    items = scope.navItems,
                                    scope = scope,
                                    navController = effectiveNavController,
                                    currentDestination = actualCurrentDestination,
                                    buttonSize = activeButtonSize,
                                    onRailCyclerClick = onDropdownCyclerClick,
                                    onItemSelected = { item ->
                                        if (item.collapseOnClick) isDropdownOpen = false
                                    },
                                    hostStates = hostStates,
                                    packRailButtons = true,
                                    visualDockingSide = visualDockingSide,
                                    onItemGloballyPositioned = null,
                                    helpEnabled = false,
                                    rotationDegrees = rotationDegrees
                                )
                            }
                        }
                    }
                }
            }
        }
        return
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
        if (orientation == AzOrientation.Vertical) Modifier
            .width(railWidth)
            .heightIn(max = maxFabSize)
            .wrapContentHeight()
        else Modifier
            .height(railWidth)
            .widthIn(max = maxFabSize)
            .wrapContentWidth()
    } else {
        if (orientation == AzOrientation.Vertical) Modifier
            .width(railWidth)
            .fillMaxHeight()
        else Modifier
            .height(railWidth)
            .fillMaxWidth()
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

    val tapOutsideToCollapse = if (isExpanded) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { isExpanded = false })
        }
    } else Modifier

    Box(

        modifier = modifier.then(tapOutsideToCollapse),

        contentAlignment = if (isFloating) Alignment.TopStart else railAlignment
    ) {
        val swipeWidthIncrease = if (isExpanded) 40.dp else 0.dp
        val snapBackRadius = with(density) { 36.dp.toPx() } // Half of 72dp ButtonWidth

        // Scrim covering only the space outside the expanded rail, so taps on the rail's column
        // (including its safe-zone padding) fall through to the rail itself while taps elsewhere
        // collapse the menu without stealing presses from host content when the rail is closed.
        if (isExpanded && !isFloating) {
            val scrimPadding = when {
                orientation == AzOrientation.Vertical && visualDockingSide == AzDockingSide.LEFT ->
                    Modifier.padding(start = railWidth)
                orientation == AzOrientation.Vertical && visualDockingSide == AzDockingSide.RIGHT ->
                    Modifier.padding(end = railWidth)
                railAlignment == Alignment.BottomStart ||
                        railAlignment == Alignment.BottomCenter ||
                        railAlignment == Alignment.BottomEnd ->
                    Modifier.padding(bottom = railWidth)
                else -> Modifier.padding(top = railWidth)
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(scrimPadding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isExpanded = false })
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(safeZoneModifier)
                .width(railWidth + swipeWidthIncrease)
                .pointerInput(isFloating, disableSwipeToOpen, visualDockingSide) {
                    detectDragGestures(
                        onDragStart = {
                            if (isFloating) {
                                wasFloatingOpenBeforeDrag = showFloatingButtons
                                showFloatingButtons = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (isFloating) {
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                                val minY = screenHeightPx * 0.1f
                                val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
                                offsetY = offsetY.coerceIn(minY, maxY)

                                val minX = 0f
                                val maxX =
                                    (configuration.screenWidthDp * density.density) - railWidth.toPx()
                                offsetX = offsetX.coerceIn(minX, maxX)
                            } else {
                                val absX = kotlin.math.abs(dragAmount.x)
                                val absY = kotlin.math.abs(dragAmount.y)

                                if (scope.advancedConfig.enableRailDragging && absY > 20 && absY > absX) {
                                    isFloating = true
                                    isExpanded = false
                                    offsetX = 0f
                                    offsetY = screenHeightPx * 0.1f

                                    if (dragAmount.y < 0) {
                                        showFloatingButtons = false
                                        wasFloatingOpenBeforeDrag = false
                                        Log.d(
                                            "AzNavRail",
                                            "Vertical swipe UP: FAB mode activated, items folded."
                                        )
                                    } else {
                                        showFloatingButtons = false
                                        wasFloatingOpenBeforeDrag = false
                                        Log.d(
                                            "AzNavRail",
                                            "Vertical swipe DOWN: FAB mode activated, dragging initiated."
                                        )
                                    }

                                    if (scope.vibrate) haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                } else if (!disableSwipeToOpen && !scope.noMenu) {
                                    if (visualDockingSide == AzDockingSide.LEFT) {
                                        if (dragAmount.x > 20 && !isExpanded) {
                                            isExpanded = true
                                            Log.d(
                                                "AzNavRail",
                                                "Horizontal swipe RIGHT: Menu expanded."
                                            )
                                        } else if (dragAmount.x < -20 && isExpanded) {
                                            isExpanded = false
                                            Log.d(
                                                "AzNavRail",
                                                "Horizontal swipe LEFT: Menu collapsed."
                                            )
                                        }
                                    } else {
                                        if (dragAmount.x < -20 && !isExpanded) {
                                            isExpanded = true
                                            Log.d(
                                                "AzNavRail",
                                                "Horizontal swipe LEFT: Menu expanded."
                                            )
                                        } else if (dragAmount.x > 20 && isExpanded) {
                                            isExpanded = false
                                            Log.d(
                                                "AzNavRail",
                                                "Horizontal swipe RIGHT: Menu collapsed."
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (isFloating) {
                                if (wasFloatingOpenBeforeDrag) {
                                    showFloatingButtons = true
                                    Log.d("AzNavRail", "FAB drag end: items unfolded.")
                                }

                                if (offsetX * offsetX + offsetY * offsetY < snapBackRadius * snapBackRadius) {
                                    isFloating = false
                                    offsetX = 0f
                                    offsetY = 0f
                                    Log.d("AzNavRail", "FAB docked via snapping.")
                                }
                            }
                        }
                    )
                }
        ) {
            Surface(
                modifier = sizeModifier
                    .onGloballyPositioned {
                        if (isFloating) railContentHeight = it.size.height.toFloat()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { /* Consume */ }
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
                                    onTap = {
                                        Log.d("AzNavRail", "Header Tapped: toggling expansion.")
                                        toggleExpanded()
                                    },
                                    onLongPress = {
                                        if (scope.advancedConfig.enableRailDragging) {
                                            isFloating = !isFloating
                                            if (isFloating) {
                                                isExpanded = false
                                                offsetY = screenHeightPx * 0.1f
                                                Log.d(
                                                    "AzNavRail",
                                                    "Header Long Pressed: FAB mode activated."
                                                )
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                                Log.d(
                                                    "AzNavRail",
                                                    "Header Long Pressed: FAB mode deactivated (redocked)."
                                                )
                                            }
                                            if (scope.vibrate) haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
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
                        val headerIconDiameter = if (scope.headerIconSize != androidx.compose.ui.unit.Dp.Unspecified) {
                            scope.headerIconSize
                        } else {
                            minOf(100.dp, targetRailWidth)
                        }
                        Box(
                            modifier = Modifier.size(headerIconDiameter),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
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
                                    MenuItemNode(
                                        item = item,
                                        allItems = displayItems,
                                        navController = effectiveNavController,
                                        currentDestination = actualCurrentDestination,
                                        scope = scope,
                                        hostStates = hostStates,
                                        showHelpOverlay = showHelpOverlay,
                                        onToggleHelp = { toggleHelpOverlay(it) },
                                        onCollapseMenu = { isExpanded = false }
                                    )
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
                                // A nested sub-item is visible only when EVERY ancestor host in its
                                // hostId chain is expanded (and none is a nested-rail popup). Walk up
                                // the chain and bail on the first collapsed/nested-rail ancestor so
                                // deep host nesting reports the correct height.
                                var visible = true
                                var hostId: String? = item.hostId
                                val seen = HashSet<String>()
                                while (hostId != null && seen.add(hostId)) {
                                    val host = scope.navItems.find { it.id == hostId }
                                    // A dangling hostId (no matching host), a collapsed ancestor, or a
                                    // nested-rail ancestor all make this sub-item non-visible. The
                                    // explicit null check also lets Kotlin smart-cast `host` below.
                                    if (host == null || hostStates[hostId] != true || host.isNestedRail) {
                                        visible = false
                                        break
                                    }
                                    hostId = host.hostId
                                }
                                visible
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

                                            scope.transientCyclerOptions[item.id] = nextOption
                                            cyclerStates[item.id] = state.copy(
                                                displayedOption = nextOption,
                                                job = coroutineScope.launch {
                                                    delay(1000L)
                                                    scope.onClickMap[item.id]?.invoke()
                                                    cyclerStates[item.id] = cyclerStates[item.id]?.copy(job = null) ?: state
                                                }
                                            )
                                        }
                                        scope.advancedConfig.onInteraction?.invoke(item.id, item)
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
                                helpEnabled = showHelpOverlay,
                                rotationDegrees = rotationDegrees
                            )

                            // Optional pinned "More" rail item that opens the More-from-Az carousel.
                            if (scope.advancedConfig.moreFromAzRailItem &&
                                scope.advancedConfig.moreFromAzEnabled && !isFloating
                            ) {
                                val moreColor = scope.activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
                                AzButton(
                                    onClick = { showMoreFromAz = true },
                                    text = "More",
                                    color = moreColor,
                                    activeColor = moreColor,
                                    shape = scope.defaultShape
                                )
                            }
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
                            footerColor = scope.activeColor,
                            onAboutClick = if (scope.advancedConfig.inAppAbout) {
                                { isExpanded = false; showAboutOverlay = true }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

    if (showHelpOverlay) {
        // helpScopeId controls the overlay's item scope: null = main rail, non-null = the parent
        // nested-rail id whose children are the only items the overlay should describe.
        HelpOverlay(
            items = scope.navItems,
            helpLineColors = scope.helpLineColors,
            onDismiss = { toggleHelpOverlay(null) },
            itemBoundsCache = scope.itemBoundsCache,
            helpList = scope.advancedConfig.helpList,
            nestedRailOpenId = helpScopeId,
            tutorials = scope.advancedConfig.tutorials,
            onTutorialLaunch = { toggleHelpOverlay(it) }
        )
    }

    // In-app About reader overlay (auto-generated from the repo's markdown docs).
    if (showAboutOverlay) {
        AboutOverlay(
            repoUrl = scope.appRepositoryUrl,
            scope = scope,
            onOpenMoreFromAz = if (scope.advancedConfig.moreFromAzEnabled) {
                { showMoreFromAz = true }
            } else null,
            onDismiss = { showAboutOverlay = false }
        )
    }

    // "More from Az" carousel overlay, drawn above the About reader.
    if (showMoreFromAz) {
        MoreFromAzOverlay(
            jsonUrl = scope.advancedConfig.moreFromAzJsonUrl,
            scope = scope,
            onDismiss = { showMoreFromAz = false }
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

/**
 * Renders a single menu entry plus, when it is an expanded host, its sub-items beneath it.
 *
 * A sub-item may itself be a host (declared via `azMenuSubHostItem`), so this composable recurses
 * for each child, letting hosts nest to any depth in the expanded menu. Opening a sub-host reveals
 * its children while sibling sub-items remain visible (accordion behavior at every level).
 */
@Composable
private fun MenuItemNode(
    item: AzNavItem,
    allItems: List<AzNavItem>,
    navController: NavController?,
    currentDestination: String?,
    scope: AzNavRailScopeImpl,
    hostStates: MutableMap<String, Boolean>,
    showHelpOverlay: Boolean,
    onToggleHelp: (String?) -> Unit,
    onCollapseMenu: () -> Unit
) {
    MenuItem(
        item = item,
        navController = navController,
        isSelected = (item.route != null && item.route == currentDestination) ||
                item.classifiers.any { scope.activeClassifiers.contains(it) },
        onClick = {
            if (item.isHelpItem) {
                onToggleHelp(item.id)
            } else {
                scope.onClickMap[item.id]?.invoke()
            }
            scope.advancedConfig.onInteraction?.invoke(item.id, item)
            if (item.collapseOnClick) onCollapseMenu()
        },
        onCyclerClick = {
            scope.onClickMap[item.id]?.invoke()
            scope.advancedConfig.onInteraction?.invoke(item.id, item)
        },
        onToggle = {
            scope.advancedConfig.onInteraction?.invoke(item.id, item)
            if (item.collapseOnClick) onCollapseMenu()
        },
        onItemClick = { if (item.collapseOnClick) onCollapseMenu() },
        onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
        onItemGloballyPositioned = { id, bounds ->
            scope.itemBoundsCache[id] = bounds
            scope.advancedConfig.onItemGloballyPositioned?.invoke(id, bounds)
        },
        onBoundsCleared = { id -> scope.itemBoundsCache.remove(id) },
        helpEnabled = showHelpOverlay,
        activeColor = scope.activeColor
    )

    if (item.isHost && hostStates[item.id] == true) {
        allItems.forEach { child ->
            if (child.isSubItem && child.hostId == item.id) {
                MenuItemNode(
                    item = child,
                    allItems = allItems,
                    navController = navController,
                    currentDestination = currentDestination,
                    scope = scope,
                    hostStates = hostStates,
                    showHelpOverlay = showHelpOverlay,
                    onToggleHelp = onToggleHelp,
                    onCollapseMenu = onCollapseMenu
                )
            }
        }
    }
}
