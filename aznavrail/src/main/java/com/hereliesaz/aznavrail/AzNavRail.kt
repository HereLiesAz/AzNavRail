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
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.rememberAzKineticModifier
import com.hereliesaz.aznavrail.internal.rememberAzClosingState
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.model.AzExit
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.tutorial.AzGuideHighlight
import com.hereliesaz.aznavrail.tutorial.AzInstructionOverlay
import com.hereliesaz.aznavrail.tutorial.LocalAzGuidanceController
import com.hereliesaz.aznavrail.tutorial.computeAutoEdges
import com.hereliesaz.aznavrail.tutorial.computeBuiltinStatuses
import com.hereliesaz.aznavrail.tutorial.rememberActiveStatuses
import com.hereliesaz.aznavrail.tutorial.rememberAzGuidanceController
import com.hereliesaz.aznavrail.tutorial.rememberGuidanceSuppressed
import com.hereliesaz.aznavrail.tutorial.resolveShape
import com.hereliesaz.aznavrail.tutorial.routeInstructions
import com.hereliesaz.aznavrail.tutorial.stepKey
import com.hereliesaz.aznavrail.tutorial.toSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
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
 * @param onExpandedChange Called whenever the rail transitions between collapsed and expanded states.
 *   Receives `true` when the rail expands and `false` when it collapses.
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
    onExpandedChange: ((Boolean) -> Unit)? = null,
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
    // The repo backing the About screen / footer link: the explicit override if set, otherwise
    // auto-derived from the app namespace. Never the AzNavRail library repo.
    val effectiveRepoUrl = remember(scope.appRepositoryUrl, packageName) {
        scope.appRepositoryUrl.ifBlank {
            GithubDocsRepository.repoUrlFromPackage(packageName) ?: scope.appRepositoryUrl
        }
    }

    var isExpanded by remember { mutableStateOf(if (scope.noMenu) false else initiallyExpanded) }
    // Dissolve-overlay state: set on tap when the tapped item collapses the drawer. The label
    // freezes in place at its captured bounds while the actual menu row is skipped from render,
    // then slides toward the middle of the screen and fades out. Cleared once the anim finishes.
    var dissolving: com.hereliesaz.aznavrail.internal.DissolveState? by remember { mutableStateOf(null) }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var railContentHeight by remember { mutableStateOf(0f) }
    // Built-in overlay visibility (About / Help / More-from-Az) lives on the host scope so the host
    // can render those overlays like the rest of the screen. The rail reads it here for
    // snapshot-reactive UI below and flips it via host.show*/hide* in the trigger handlers. The host
    // is always present (the rail errors out above without one), so this is effectively non-null.
    val hostScope = LocalAzNavHostScope.current as? AzNavHostScopeImpl
    val showHelpOverlay = hostScope?.helpVisible == true
    var wasFloatingOpenBeforeDrag by remember { mutableStateOf(false) }
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
    // Tracks the last evaluated result of each host's `expandWhen` condition for edge detection.
    val expandWhenSeen = remember { mutableMapOf<String, Boolean>() }

    val toggleHelpOverlay = remember(scope, hostScope) {
        { itemId: String? ->
            if (hostScope?.helpVisible == true) {
                hostScope.hideHelp()
                scope.advancedConfig.onDismissHelp?.invoke()
            } else {
                // Locate the tapped help item's parent nested rail (if any) so the overlay
                // can scope its cards to that rail. A help item under a `azNestedRail { ... }`
                // block lives in some parent's `nestedRailItems`; a main-rail help item lives
                // directly in scope.navItems and resolves to scope=null.
                val scopeId = itemId?.let { id ->
                    scope.navItems.firstOrNull { parent ->
                        parent.isNestedRail && parent.nestedRailItems?.any { it.id == id } == true
                    }?.id
                }
                hostScope?.showHelp(scopeId)
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

    // Reactive expansion: a host with `expandWhen` expands on the rising edge of its condition and
    // collapses on the falling edge. A manual collapse while the condition stays true is preserved
    // (the condition acts on transitions, never continuously), so we apply only on an actual change.
    //
    // Keyed on the STABLE set of expandWhen host ids — not `scope.navItems`, whose contents change on
    // every item-value update, which used to tear down and relaunch the collectors and swallow the
    // rising edge. Each host is observed via `snapshotFlow` (instant for Compose snapshot state) merged
    // with a low-rate poll, so conditions backed by plain sources (`StateFlow.value`, `LiveData.value`,
    // a `var`) — which `snapshotFlow` cannot see — still drive expansion (within the poll interval).
    val expandWhenKeys = scope.expandWhenMap.keys.sorted().joinToString(",")
    LaunchedEffect(expandWhenKeys) {
        scope.expandWhenMap.toMap().forEach { (id, cond) ->
            launch {
                merge(
                    snapshotFlow { cond() },
                    flow { while (true) { emit(cond()); delay(300) } },
                ).distinctUntilChanged().collect { conditionNow ->
                    val before = expandWhenSeen[id]
                    when {
                        // First observation: only auto-expand on a true condition; never clobber an
                        // initiallyExpanded/manual state with a collapse here.
                        before == null -> if (conditionNow) hostStates[id] = true
                        // A real transition: rising edge expands, falling edge collapses.
                        before != conditionNow -> hostStates[id] = conditionNow
                    }
                    expandWhenSeen[id] = conditionNow
                }
            }
        }
    }

    LaunchedEffect(isExpanded) {
        onExpandedChange?.invoke(isExpanded)
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
            val scrimColor = if (scope.dimBehindMenu) {
                androidx.compose.ui.graphics.Color.Black.copy(
                    alpha = scope.dimBehindMenuAlpha.coerceIn(0f, 1f),
                )
            } else androidx.compose.ui.graphics.Color.Transparent
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(scrimPadding)
                    .background(scrimColor)
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
                    val hasExplicitHelpItem = scope.navItems.any { it.isHelpItem || it.id == AzNavRailDefaults.AUTO_HELP_ID }
                    val displayItems = if (scope.advancedConfig.helpEnabled && !hasExplicitHelpItem) {
                        scope.navItems + AzNavItem.Help(
                            id = AzNavRailDefaults.AUTO_HELP_ID,
                            isRailItem = false
                        )
                    } else {
                        scope.navItems
                    }
                    val topLevelItems = displayItems.filter { !it.isSubItem }
                    // Keep the menu composed through the staggered exit so items can turnstile out as the
                    // rail collapses (mirrors the entrance overlapping the expand). itemExit=None => the
                    // legacy instant teardown.
                    val menuRendered = rememberAzClosingState(
                        open = isExpanded,
                        exit = scope.itemExit,
                        count = topLevelItems.size,
                        staggerMs = scope.entranceStaggerMs,
                        durationMs = scope.entranceDurationMs
                    )
                    if (menuRendered) {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            topLevelItems.forEachIndexed { index, item ->
                                // If this item was the one tapped-to-close, skip its render — the
                                // dissolve overlay is drawing its label at the captured bounds and
                                // sliding it away. Skipping avoids animating the label in two
                                // places at once (once here as it exit-turnstiles, once in the
                                // overlay).
                                if (dissolving?.itemId == item.id) return@forEachIndexed
                                MenuItemNode(
                                    item = item,
                                    allItems = displayItems,
                                    navController = effectiveNavController,
                                    currentDestination = actualCurrentDestination,
                                    scope = scope,
                                    hostStates = hostStates,
                                    showHelpOverlay = showHelpOverlay,
                                    onToggleHelp = { toggleHelpOverlay(it) },
                                    onCollapseMenu = { isExpanded = false },
                                    onDissolveTap = { itemId, text ->
                                        val bounds = scope.itemBoundsCache[itemId]
                                        if (bounds != null) {
                                            dissolving = com.hereliesaz.aznavrail.internal.DissolveState(
                                                itemId = itemId,
                                                text = text,
                                                bounds = bounds,
                                            )
                                        }
                                    },
                                    index = index,
                                    count = topLevelItems.size,
                                    visible = isExpanded,
                                    floating = isFloating,
                                    dockingSide = scope.dockingSide
                                )
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
                                    onClick = { hostScope?.showMoreFromAz() },
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
                // Keep the footer composed for `entranceDurationMs` after `isExpanded` flips false
                // so its accordion fold-UP animation actually plays. Without this the footer would
                // leave composition instantly and never animate out.
                val footerRendered = rememberAzClosingState(
                    open = isExpanded,
                    exit = AzExit.Turnstile,
                    count = 1,
                    staggerMs = scope.entranceStaggerMs,
                    durationMs = scope.entranceDurationMs,
                )
                if (scope.showFooter && footerRendered) {
                    val footerMenuCount = scope.navItems.count { !it.isSubItem }
                    val dividerColor = scope.activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
                    Column {
                        AzDivider(color = dividerColor)
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
                            repoUrl = effectiveRepoUrl,
                            footerColor = scope.activeColor,
                            onAboutClick = if (scope.advancedConfig.inAppAbout) {
                                { isExpanded = false; hostScope?.showAbout() }
                            } else null,
                            visible = isExpanded,
                            menuItemCount = footerMenuCount,
                            staggerMs = scope.entranceStaggerMs,
                            durationMs = scope.entranceDurationMs,
                            easing = scope.entranceEasing,
                        )
                    }
                }
            }
        }
    }

    // Dissolve overlay for a tapped menu item that collapses the drawer. Renders as a full-screen
    // `Popup` outside every clipping ancestor, so the label can slide across the docked-edge as it
    // fades. Cleared once its animation completes.
    dissolving?.let { snapshot ->
        val accent = scope.activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
        val style = MaterialTheme.typography.titleLarge.let { base ->
            scope.itemTextStyle?.let { base.merge(it) } ?: base
        }
        com.hereliesaz.aznavrail.internal.DissolveOverlay(
            state = snapshot,
            textStyle = style,
            color = accent,
            durationMs = scope.entranceDurationMs,
            easing = scope.entranceEasing,
            onFinished = { dissolving = null },
        )
    }
}

    // The About reader, Help overlay, and "More from Az" carousel are now rendered by
    // AzHostActivityLayout (About + More-from-Az flow through the onscreen() layout path; Help stays
    // full-screen). The rail only flips their visibility on the host scope via the trigger handlers
    // above.

    // --- Status-driven guidance (the reactive replacement for the scripted tutorial) ---
    // The engine observes which statuses are true, routes from the live state toward each active goal,
    // and renders the next-hop instruction as a callout adjacent to its target — auto-advancing the
    // instant a target status becomes true. The controller is host-provided (so the developer's handle,
    // returned from AzHostActivityLayout, drives the same instance); standalone rails fall back locally.
    val guidanceFallback = rememberAzGuidanceController()
    val guidanceController = LocalAzGuidanceController.current ?: guidanceFallback

    val guidanceActiveItemId = scope.navItems.firstOrNull { item ->
        (item.route != null && item.route == actualCurrentDestination) ||
            item.classifiers.any { scope.activeClassifiers.contains(it) }
    }?.id

    val activeStatuses by rememberActiveStatuses(
        statusPredicates = scope.statusPredicates,
        activeClassifiers = scope.activeClassifiers,
        builtins = {
            computeBuiltinStatuses(
                railExpanded = isExpanded,
                railFloating = isFloating,
                hostStates = hostStates,
                currentRoute = actualCurrentDestination,
                activeItemId = guidanceActiveItemId,
                nestedRailOpenId = scope.nestedRailOpenId,
                helpOpen = showHelpOverlay,
            )
        },
    )

    val guidanceGoalsMap = remember(scope.guidanceGoals.toList()) {
        scope.guidanceGoals.associateBy { it.id }
    }
    // Self-arming onboarding goals: activate once their trigger status holds. `autoStartWhen` is
    // discouraged (starting should be the developer's explicit `activate(...)` call); it honours both
    // completion and the user's skip, so a finished or dismissed tutorial never auto-restarts.
    LaunchedEffect(activeStatuses, guidanceGoalsMap, guidanceController) {
        guidanceGoalsMap.values.forEach { goal ->
            val trigger = goal.autoStartWhen
            if (trigger != null && trigger in activeStatuses &&
                !guidanceController.isCompleted(goal.id) && !guidanceController.isDismissed(goal.id) &&
                goal.id !in guidanceController.activeGoals
            ) {
                guidanceController.activate(goal.id)
            }
        }
    }

    // Host-driven suppression (e.g. while a gesture is in progress); observed even when the overlay is
    // hidden so it can re-show after the settle delay. Must be called unconditionally each recomposition.
    val guidanceSuppressed by rememberGuidanceSuppressed(scope.guidanceSuppressors)

    if (guidanceController.enabled &&
        hostScope?.aboutVisible != true && hostScope?.moreFromAzVisible != true
    ) {
        // Auto-edge instruction text comes from localizable string resources (apps override the ids).
        val openMenuLabel = stringResource(R.string.az_guide_open_menu)
        val tapTemplate = stringResource(R.string.az_guide_tap_item)
        val guidanceEdges = remember(scope.guidanceEdges.toList(), scope.navItems.toList(), openMenuLabel, tapTemplate) {
            scope.guidanceEdges + computeAutoEdges(
                items = scope.navItems,
                openMenuLabel = openMenuLabel,
                tapLabel = { label -> String.format(tapTemplate, label) },
            )
        }
        // Cache the BFS routing: recompute only when the edges/goals/statuses change (or the reactive
        // active-goal set / paged-step cursor, read inside derivedStateOf) — not on every recomposition
        // from drag/animation.
        val frame = remember(guidanceEdges, guidanceGoalsMap, activeStatuses) {
            derivedStateOf {
                routeInstructions(
                    edges = guidanceEdges,
                    goals = guidanceGoalsMap,
                    activeGoalIds = guidanceController.activeGoals,
                    activeStatuses = activeStatuses,
                    stepIndexOf = { guidanceController.stepIndex(it) },
                    consumedStatuses = guidanceController.consumedStatuses,
                )
            }
        }.value
        // Auto-advance: a goal whose target is now true is reached → deactivate it and persist.
        LaunchedEffect(frame.reachedGoals, guidanceController) {
            frame.reachedGoals.forEach { guidanceController.markReached(it) }
        }
        // De-dup: once a status that was a shown hop's `to` is reached, consume it so that hop never
        // re-shows (even if the user later undoes the action and the router would otherwise re-route to it).
        val pendingConsume = remember(guidanceController) { mutableSetOf<String>() }
        LaunchedEffect(frame.resolved, activeStatuses, guidanceController) {
            frame.resolved.forEach { r -> r.edge.to?.let { if (it !in pendingConsume) pendingConsume.add(it) } }
            pendingConsume.forEach { if (it in activeStatuses) guidanceController.consume(it) }
        }
        // Persist reactive step advances into the cursor so a later tap continues from the shown step.
        LaunchedEffect(frame.resolved, guidanceController) {
            frame.resolved.forEach { r ->
                if (r.stepTotal > 1 && guidanceController.stepIndex(r.edge.stepKey()) < r.stepIndex) {
                    guidanceController.setStep(r.edge.stepKey(), r.stepIndex)
                }
            }
        }
        // Publish the observable snapshot. Target shapes are NOT resolved here (that would subscribe the
        // composition to a moving target's per-frame state); the host has its own shape and gets the
        // `targetId`. Non-target highlights resolve cheaply from the stable bounds cache.
        val activeItemId = guidanceActiveItemId
        val snapshots = remember(frame.resolved, activeItemId, scope.itemBoundsCache.toMap()) {
            frame.resolved.map { r ->
                val h = r.instruction.highlight
                val shape = if (h is AzGuideHighlight.Target) null
                else h.resolveShape(scope.itemBoundsCache, activeItemId, emptyMap())
                r.toSnapshot(shape, activeItemId)
            }
        }
        LaunchedEffect(snapshots, guidanceController) { guidanceController.publishCurrent(snapshots) }

        if (!guidanceSuppressed) {
            AzInstructionOverlay(
                resolved = frame.resolved,
                itemBoundsCache = scope.itemBoundsCache,
                accent = if (scope.activeColor != Color.Unspecified) scope.activeColor else MaterialTheme.colorScheme.primary,
                activeItemId = activeItemId,
                targets = scope.guidanceTargets,
                controller = guidanceController,
                renderSlot = scope.guidanceRenderer,
            )
        }
    } else {
        // Nothing showing: clear the published snapshot so observers see an empty state.
        LaunchedEffect(guidanceController) { guidanceController.publishCurrent(emptyList()) }
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
    onCollapseMenu: () -> Unit,
    // Called just before `onCollapseMenu` fires, when the tapped item has `collapseOnClick = true`.
    // The caller uses this to spawn a `DissolveOverlay` for that item's label.
    onDissolveTap: (itemId: String, text: String) -> Unit = { _, _ -> },
    index: Int,
    count: Int,
    visible: Boolean,
    floating: Boolean,
    dockingSide: AzDockingSide
) {
    // DSL `azDivider()` calls declare a synthetic item with `isDivider = true`. Render it as an
    // actual AzDivider — same accent color as the surrounding labels — instead of falling through
    // to the empty-MenuItem code path (which used to render as a blank clickable row).
    if (item.isDivider) {
        val dividerAccent = scope.activeColor.takeOrElse { MaterialTheme.colorScheme.primary }
        AzDivider(color = dividerAccent)
        return
    }
    val kinetic = rememberAzKineticModifier(
        index = index,
        count = count,
        visible = visible,
        entrance = scope.itemEntrance,
        exit = scope.itemExit,
        staggerMs = scope.entranceStaggerMs,
        durationMs = scope.entranceDurationMs,
        easing = scope.entranceEasing,
        startAngle = scope.entranceStartAngle,
        // Suppress the press-tilt on draggable/relocatable items so it never fights the drag gesture.
        tiltOnPress = scope.tiltOnPress && !item.isRelocItem,
        maxTiltDegrees = scope.maxTiltDegrees,
        dockingSide = dockingSide,
        floating = floating
    )
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
        // MenuItem invokes `onItemClick` last for both standard and toggle items — the single
        // place where the tap becomes a menu-close decision — so triggering the dissolve here
        // guarantees exactly one overlay spawn per tap.
        onItemClick = {
            if (item.collapseOnClick) {
                onDissolveTap(item.id, item.menuText ?: item.text)
                onCollapseMenu()
            }
        },
        onHostClick = {
            val newHostState = !(hostStates[item.id] ?: false)
            hostStates[item.id] = newHostState
            scope.onExpandedChangeMap[item.id]?.invoke(newHostState)
        },
        onItemGloballyPositioned = { id, bounds ->
            scope.itemBoundsCache[id] = bounds
            scope.advancedConfig.onItemGloballyPositioned?.invoke(id, bounds)
        },
        onBoundsCleared = { id -> scope.itemBoundsCache.remove(id) },
        helpEnabled = showHelpOverlay,
        activeColor = scope.activeColor,
        kineticModifier = kinetic,
        textStyle = scope.itemTextStyle,
        dockingSide = dockingSide,
        menuItemAlignment = scope.menuItemAlignment,
        justifyMenuItems = scope.justifyMenuItems
    )

    if (item.isHost && hostStates[item.id] == true) {
        val children = allItems.filter { it.isSubItem && it.hostId == item.id }
        children.forEachIndexed { childIndex, child ->
            MenuItemNode(
                item = child,
                allItems = allItems,
                navController = navController,
                currentDestination = currentDestination,
                scope = scope,
                hostStates = hostStates,
                showHelpOverlay = showHelpOverlay,
                onToggleHelp = onToggleHelp,
                onCollapseMenu = onCollapseMenu,
                index = childIndex,
                count = children.size,
                visible = visible,
                floating = floating,
                dockingSide = dockingSide
            )
        }
    }
}
