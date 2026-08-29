// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.rememberDeviceRotationDegrees
import com.hereliesaz.aznavrail.internal.rememberEffectiveAppMeta
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzAboutSurface
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.rememberAzAboutOwnership
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.rememberAzKineticModifier
import com.hereliesaz.aznavrail.internal.rememberAzClosingState
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.internal.rememberAzHaptics
import com.hereliesaz.aznavrail.internal.rememberAzOverlayLauncher
import com.hereliesaz.aznavrail.model.AzExit
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.service.AzAboutWarmup
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.tutorial.AzGuideHighlight
import com.hereliesaz.aznavrail.tutorial.AzEscortOverlay
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
import com.hereliesaz.aznavrail.internal.AzIcons

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
 * @param initiallyExpanded Whether the rail should be expanded (show menu) by default. Only seeds
 *   the *uncontrolled* case ([expanded] left null); ignored while [expanded] is non-null.
 * @param disableSwipeToOpen Whether the swipe gesture to open the menu is disabled.
 * @param providedScope An optional pre-configured [AzNavRailScopeImpl]. Used internally by [AzHostActivityLayout].
 * @param orientation The orientation of the rail (Vertical or Horizontal). Default is Vertical.
 * @param visualDockingSide The side of the screen where the rail is visually docked (Left or Right).
 * @param railAlignment The alignment of the rail within its container.
 * @param reverseLayout Whether to reverse the layout direction (e.g., for Right-to-Left languages or right docking).
 * @param onExpandedChange Called whenever the rail transitions between collapsed and expanded states.
 *   Receives `true` when the rail expands and `false` when it collapses. Every internal toggle path
 *   (header tap, swipe, outside-tap-to-dismiss, an item's `collapseOnClick`, undock, etc.) calls this,
 *   whether or not [expanded] is supplied — the same controlled/uncontrolled contract [AzDropdownMenu]
 *   already uses.
 * @param expanded Optional controlled expand/collapse state, mirroring [AzDropdownMenu]'s `expanded`
 *   param. Null (the default) leaves the rail fully uncontrolled — [initiallyExpanded] seeds it and
 *   [onExpandedChange] merely observes, exactly like today. Non-null makes the caller the source of
 *   truth: every internal toggle path still fires [onExpandedChange] (so the caller can react and
 *   flip its own state), but the rail's visible expand/collapse state is driven by this value alone.
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
    expanded: Boolean? = null,
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

    val uriHandler = LocalUriHandler.current
    // Effective app metadata: consumer-provided LocalAzAppMeta merged over the platform-resolved
    // name/icon/packageId, so the launcher icon shows automatically (Android/web) without manual
    // wiring — matching the Android library — while still honoring explicit overrides.
    val appMeta = rememberEffectiveAppMeta()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Window size in px + a Dp height, both multiplatform-safe (LocalConfiguration is Android-only).
    val containerSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = containerSize.width.toFloat()
    val screenHeightPx = containerSize.height.toFloat()
    val screenHeightDp = with(density) { containerSize.height.toDp() }
    val coroutineScope = rememberCoroutineScope()

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) {
        scope.reset()
        scope.apply(content)
        scope.applyRelocReorders()
        scope.applyItemStates()
    }
    // When providedScope is non-null, AzHostActivityLayout already applied the DSL + reorders.

    // App name + icon + repo derivation: pulled from LocalAzAppMeta (Android's
    // AzHostActivityLayout populates it from `packageManager.getApplicationLabel/Icon`; non-Android
    // consumers pass whatever their app already has).
    val packageName = appMeta.packageId
    val appName = appMeta.name
    val appIcon = appMeta.icon
    // The repo backing the About screen / footer link: the explicit override if set, otherwise
    // auto-derived from the app namespace. Never the AzNavRail library repo.
    val effectiveRepoUrl = remember(scope.appRepositoryUrl, packageName) {
        scope.appRepositoryUrl.ifBlank {
            packageName?.let { GithubDocsRepository.repoUrlFromPackage(it) } ?: scope.appRepositoryUrl
        }
    }

    // One haptic handle per rail, built once the scope (and its `vibrate` setting) exists.
    val azHaptics = rememberAzHaptics(scope.vibrate)

    // Ids of the unattached hosts and their whole subtrees. They are declared on this scope like any
    // other item, but they render at their own anchor (see `AzUnattachedRail`), so the rail strip
    // and the drawer both have to skip them.
    val unattachedIds = remember(scope.navItems.toList()) {
        com.hereliesaz.aznavrail.internal.azUnattachedSubtreeIds(scope.navItems)
    }

    // Controlled/uncontrolled expand state, mirroring AzDropdownMenu's `expanded ?: internalOpen`
    // pattern: `expanded == null` (the default) is fully uncontrolled — `internalExpanded` is the
    // only source of truth, seeded by `initiallyExpanded`, and behavior is identical to before this
    // param existed. `expanded != null` hands control to the caller: `internalExpanded` still gets
    // written by every internal toggle path (so nothing here has to special-case controlled mode),
    // but it is ignored by `isExpanded` in that case; `setExpanded` calls `onExpandedChange` directly
    // for the controlled case, since the reactive effect below (keyed on `isExpanded`) cannot see a
    // change that `expanded` itself didn't cause.
    var internalExpanded by remember { mutableStateOf(if (scope.noMenu) false else initiallyExpanded) }
    val isExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { value ->
        if (expanded == null) {
            internalExpanded = value
        } else {
            onExpandedChange?.invoke(value)
        }
    }
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

    // The About ("?") affordance is a rail item like any other, appended last — unless the developer
    // declared their own with `azAboutRailItem`, in which case theirs stands where they put it. It
    // persists; it is not fixed.
    val hasExplicitAboutItem = scope.navItems.any { it.isAboutItem }
    val dedupeAbout = scope.advancedConfig.dedupeAbout

    // Who draws About. Every surface that could offer one registers itself; only the highest-ranked
    // registration actually draws, so an app with a rail AND a drop-down doesn't show About three
    // times over. A developer-declared `?` outranks everything and is never suppressed — it only
    // suppresses others — which is why its ownership result is ignored.
    rememberAzAboutOwnership(
        surface = AzAboutSurface.RAIL_ITEM_EXPLICIT,
        offered = hasExplicitAboutItem,
        dedupe = dedupeAbout,
    )
    val footerOwnsAbout = rememberAzAboutOwnership(
        surface = AzAboutSurface.RAIL_MENU_FOOTER,
        offered = scope.showFooter && !scope.noMenu,
        dedupe = dedupeAbout,
    )
    val autoAboutItemOwned = rememberAzAboutOwnership(
        surface = AzAboutSurface.RAIL_ITEM_AUTO,
        offered = scope.advancedConfig.aboutRailItem && !hasExplicitAboutItem,
        dedupe = dedupeAbout,
    )
    val autoAboutItem = if (autoAboutItemOwned) {
        AzNavItem.About(id = AzNavRailDefaults.AUTO_ABOUT_ID, shape = scope.defaultShape)
    } else null

    // Warm the About reader's content now, in the background, so opening it shows the page rather
    // than a spinner. Cheap: cached, conditional requests that usually come back 304.
    if (scope.advancedConfig.inAppAbout) {
        AzAboutWarmup(
            repoUrl = effectiveRepoUrl,
            moreFromAzEnabled = scope.advancedConfig.moreFromAzEnabled,
            moreFromAzJsonUrl = scope.advancedConfig.moreFromAzJsonUrl,
        )
    }

    /** Opens the About reader (or the repo in a browser), and closes it again on a second tap. */
    val toggleAbout: () -> Unit = {
        if (scope.advancedConfig.inAppAbout) {
            if (hostScope?.aboutVisible == true) hostScope.hideAbout() else hostScope?.showAbout()
        } else if (effectiveRepoUrl.isNotBlank()) {
            try {
                uriHandler.openUri(effectiveRepoUrl)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Leaves the About / More-from-Az reader. Any other interaction with the rail means the user is
     * done reading — a full-screen reader you can only escape through its own close button is a
     * room with a keyhole for a door.
     */
    val dismissFooterScreens: () -> Unit = {
        hostScope?.hideAbout()
        hostScope?.hideMoreFromAz()
    }
    var wasFloatingOpenBeforeDrag by remember { mutableStateOf(false) }
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val onSecretClick = SecretScreens(secLoc = scope.advancedConfig.secLoc, secLocPort = scope.advancedConfig.secLocPort)

    LaunchedEffect(showFloatingButtons, railContentHeight) {
        if (isFloating) {
            val minY = screenHeightPx * 0.1f
            val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
            if (offsetY > maxY) {
                offsetY = maxY
                AzNavRailLogger.e("AzNavRail", "FAB mode: Adjusted position to stay within 10-90% safe zone.")
            }
        }
    }

    // Device rotation (0/90/180/270 degrees) via the platform expect fun — Android reads it from
    // LocalView.current.display?.rotation; non-Android targets return 0f.
    val rotationDegrees = rememberDeviceRotationDegrees()

    val isVerticalNestedRailOpen by remember {
        derivedStateOf {
            scope.nestedRailOpenId?.let { id ->
                scope.navItems.any { it.id == id && it.nestedRailAlignment == AzNestedRailAlignment.VERTICAL }
            } ?: false
        }
    }

    // Shrink button size and rail width further when a vertical nested rail is open. Otherwise honor
    // a developer-defined uniform rail-item size (scope.railItemWidth), falling back to the default.
    val baseButtonSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth
    val calculatedShrunkSize = baseButtonSize * 0.75f
    val actualShrunkSize = maxOf(calculatedShrunkSize, 35.dp)
    val targetShrunkSize = minOf(baseButtonSize, actualShrunkSize)
    val activeButtonSize = if (isVerticalNestedRailOpen) targetShrunkSize else baseButtonSize

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
                // putIfAbsent is a JVM-only Map default method; use containsKey guards for KMP.
                if (!cyclerStates.containsKey(item.id)) {
                    cyclerStates[item.id] = CyclerTransientState(item.selectedOption ?: "")
                }
                if (!scope.transientCyclerOptions.containsKey(item.id)) {
                    scope.transientCyclerOptions[item.id] = item.selectedOption ?: ""
                }
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
    // The same map/effect also drives a nested rail's popup when its `azNestedRail(expandWhen = ...)`
    // is set: `scope.expandWhenMap` is populated by both host items and nested-rail items (see
    // `AzNavRailScopeImpl.azNestedRail`), and this effect tells them apart by checking `isNestedRail`
    // on the target item, writing `scope.nestedRailOpenId` instead of `hostStates` for those — the
    // exact same rising/falling-edge, first-observation, and "user wins" semantics apply to both.
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
                    val isNestedRailTarget = scope.navItems.find { it.id == id }?.isNestedRail == true
                    fun applyRisingEdge() {
                        if (isNestedRailTarget) scope.nestedRailOpenId = id else hostStates[id] = true
                    }
                    fun applyTransition() {
                        if (isNestedRailTarget) {
                            if (conditionNow) scope.nestedRailOpenId = id
                            else if (scope.nestedRailOpenId == id) scope.nestedRailOpenId = null
                        } else {
                            hostStates[id] = conditionNow
                        }
                    }
                    when {
                        // First observation: only auto-expand on a true condition; never clobber an
                        // initiallyExpanded/manual state with a collapse here.
                        before == null -> if (conditionNow) applyRisingEdge()
                        // A real transition: rising edge expands, falling edge collapses.
                        before != conditionNow -> applyTransition()
                    }
                    expandWhenSeen[id] = conditionNow
                }
            }
        }
    }


    // The documented "rail as a system-wide overlay" mode: when the developer supplied an
    // `overlayService`, undocking hands off to that service instead of only floating in-app. The
    // config has always been collected; this is the edge that finally acts on it.
    val launchOverlay = rememberAzOverlayLauncher()
    LaunchedEffect(isFloating) {
        if (isFloating) scope.advancedConfig.overlayService?.let { launchOverlay(it) }
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
        dismissFooterScreens()
        if (!showHelpOverlay) {
            if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else if (scope.noMenu) {
                scope.isFoldedUp = !scope.isFoldedUp
            } else {
                setExpanded(!isExpanded)
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Which pointer gestures this rail can actually act on. Everything else must fall through to the
    // app: the rail is drawn over the host's content, and a listener that answers no gesture is
    // simply a hole in the app's own input.
    val canSwipeMenu = !disableSwipeToOpen && !scope.noMenu
    val railHandlesDrags = isFloating || scope.advancedConfig.enableRailDragging || canSwipeMenu

    val isHorizontal = orientation == AzOrientation.Horizontal

    val sizeModifier = if (isFloating || (scope.noMenu && scope.isFoldedUp)) {
        // Enforce max height/width in FAB mode to ensure it fits within 10-90% safe zone
        val maxFabSize = screenHeightDp * 0.8f
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
    val safeTopDp = screenHeightDp * 0.1f
    val safeBottomDp = screenHeightDp * 0.1f
    val safeZoneModifier = if (!isFloating && orientation == AzOrientation.Vertical) {
        Modifier.padding(top = safeTopDp, bottom = safeBottomDp)
    } else { Modifier }

    // No background shape when collapsed. Drawer visible only when expanded.
    val surfaceColor = Color.Transparent
    val surfaceElevation = if (isExpanded && !isFloating) 2.dp else 0.dp

    // Deliberately no window-wide tap listener on this Box. The rail is laid out over the entire
    // window, so a `detectTapGestures` here eats every tap meant for the app beneath it. Collapsing
    // on an outside tap is the scrim's job below — it is inset to exclude the rail, and it only
    // exists while the menu is actually open.
    Box(
        modifier = modifier,
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
                        detectTapGestures(onTap = { setExpanded(false) })
                    }
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(safeZoneModifier)
                .width(railWidth + swipeWidthIncrease)
                // Listen for drags only when the rail could act on one. A rail that can neither be
                // undocked nor swiped open has no business installing a pointer handler over the
                // app's content — and even when it does listen, it consumes only the drags it
                // actually uses, so a scroll that starts under the rail still reaches the app.
                .then(
                    if (!railHandlesDrags) Modifier
                    else Modifier.pointerInput(isFloating, disableSwipeToOpen, visualDockingSide) {
                        detectDragGestures(
                            onDragStart = {
                                if (isFloating) {
                                    wasFloatingOpenBeforeDrag = showFloatingButtons
                                    showFloatingButtons = false
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isFloating) {
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                    // Report the drag to the consumer. `onOverlayDrag` is the
                                    // system-overlay flavour, `onRailDrag` the in-app one; both were
                                    // collected by azAdvanced/azSettings but never invoked.
                                    scope.advancedConfig.onRailDrag?.invoke(dragAmount.x, dragAmount.y)
                                    if (scope.advancedConfig.overlayService != null) {
                                        scope.advancedConfig.onOverlayDrag?.invoke(dragAmount.x, dragAmount.y)
                                    }
                                    val minY = screenHeightPx * 0.1f
                                    val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
                                    offsetY = offsetY.coerceIn(minY, maxY)

                                    val minX = 0f
                                    val maxX = screenWidthPx - with(density) { railWidth.toPx() }
                                    offsetX = offsetX.coerceIn(minX, maxX)
                                } else {
                                    val absX = kotlin.math.abs(dragAmount.x)
                                    val absY = kotlin.math.abs(dragAmount.y)

                                    if (scope.advancedConfig.enableRailDragging && absY > 20 && absY > absX) {
                                        change.consume()
                                        isFloating = true
                                        setExpanded(false)
                                        offsetX = 0f
                                        offsetY = screenHeightPx * 0.1f

                                        if (dragAmount.y < 0) {
                                            showFloatingButtons = false
                                            wasFloatingOpenBeforeDrag = false
                                            AzNavRailLogger.e(
                                                "AzNavRail",
                                                "Vertical swipe UP: FAB mode activated, items folded."
                                            )
                                        } else {
                                            showFloatingButtons = false
                                            wasFloatingOpenBeforeDrag = false
                                            AzNavRailLogger.e(
                                                "AzNavRail",
                                                "Vertical swipe DOWN: FAB mode activated, dragging initiated."
                                            )
                                        }

                                        if (scope.vibrate) haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    } else if (canSwipeMenu) {
                                        if (visualDockingSide == AzDockingSide.LEFT) {
                                            if (dragAmount.x > 20 && !isExpanded) {
                                                change.consume()
                                                setExpanded(true)
                                                AzNavRailLogger.e(
                                                    "AzNavRail",
                                                    "Horizontal swipe RIGHT: Menu expanded."
                                                )
                                            } else if (dragAmount.x < -20 && isExpanded) {
                                                change.consume()
                                                setExpanded(false)
                                                AzNavRailLogger.e(
                                                    "AzNavRail",
                                                    "Horizontal swipe LEFT: Menu collapsed."
                                                )
                                            }
                                        } else {
                                            if (dragAmount.x < -20 && !isExpanded) {
                                                change.consume()
                                                setExpanded(true)
                                                AzNavRailLogger.e(
                                                    "AzNavRail",
                                                    "Horizontal swipe LEFT: Menu expanded."
                                                )
                                            } else if (dragAmount.x > 20 && isExpanded) {
                                                change.consume()
                                                setExpanded(false)
                                                AzNavRailLogger.e(
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
                                        AzNavRailLogger.e("AzNavRail", "FAB drag end: items unfolded.")
                                    }

                                    if (offsetX * offsetX + offsetY * offsetY < snapBackRadius * snapBackRadius) {
                                        isFloating = false
                                        offsetX = 0f
                                        offsetY = 0f
                                        AzNavRailLogger.e("AzNavRail", "FAB docked via snapping.")
                                    }
                                }
                            }
                        )
                    }
                )
        ) {
            Surface(
                modifier = sizeModifier
                    .onGloballyPositioned {
                        if (isFloating) railContentHeight = it.size.height.toFloat()
                    }
                    // Swallow stray taps only while the rail is a panel in its own right — expanded,
                    // or floating over the app. Collapsed and docked it is a full-height strip that
                    // is mostly empty space, its buttons take their own taps, and the gaps between
                    // them belong to whatever the app drew underneath.
                    .then(
                        if (isExpanded || isFloating) Modifier.pointerInput(Unit) {
                            detectTapGestures { /* Consume */ }
                        } else Modifier
                    )
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
                                        AzNavRailLogger.e("AzNavRail", "Header Tapped: toggling expansion.")
                                        toggleExpanded()
                                    },
                                    onLongPress = {
                                        if (scope.advancedConfig.enableRailDragging) {
                                            isFloating = !isFloating
                                            if (isFloating) {
                                                setExpanded(false)
                                                offsetY = screenHeightPx * 0.1f
                                                AzNavRailLogger.e(
                                                    "AzNavRail",
                                                    "Header Long Pressed: FAB mode activated."
                                                )
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                                AzNavRailLogger.e(
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
                                Icon(AzIcons.Menu, "Menu")
                            }
                        }
                    }
                }
                val isRailOpen = !(isFloating && !showFloatingButtons) && !(scope.noMenu && scope.isFoldedUp)
                val railItemsCount =
                    scope.navItems.filter { it.isRailItem && !it.isSubItem && it.id !in unattachedIds }.size +
                        (if (autoAboutItem != null) 1 else 0)
                val railItemsRendered = rememberAzClosingState(
                    open = isRailOpen,
                    exit = AzExit.Turnstile,
                    count = railItemsCount,
                    staggerMs = scope.entranceStaggerMs,
                    durationMs = scope.entranceDurationMs
                )

                if (railItemsRendered) {
                    // MAIN CONTENT and MENU separation
                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val hasExplicitHelpItem = scope.navItems.any { it.isHelpItem || it.id == AzNavRailDefaults.AUTO_HELP_ID }
                        // Unattached hosts and everything hanging off them have left the rail: they
                        // draw themselves at their own anchor, so neither the strip nor the drawer
                        // may render them.
                        val attachedItems = if (unattachedIds.isEmpty()) scope.navItems
                        else scope.navItems.filterNot { it.id in unattachedIds }
                        // The rail strip ends with the About ("?") button. The drawer does not need
                        // one — its footer already carries About — so this is appended here only.
                        val railStripItems =
                            if (autoAboutItem != null) attachedItems + autoAboutItem else attachedItems
                        val displayItems = if (scope.advancedConfig.helpEnabled && !hasExplicitHelpItem) {
                            attachedItems + AzNavItem.Help(
                                id = AzNavRailDefaults.AUTO_HELP_ID,
                                isRailItem = false
                            )
                        } else {
                            attachedItems
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
                                    MenuItemNode(
                                        item = item,
                                        allItems = displayItems,
                                        navController = effectiveNavController,
                                        currentDestination = actualCurrentDestination,
                                        scope = scope,
                                        hostStates = hostStates,
                                        showHelpOverlay = showHelpOverlay,
                                        onToggleHelp = { toggleHelpOverlay(it) },
                                        onCollapseMenu = { setExpanded(false) },
                                        index = index,
                                        count = topLevelItems.size,
                                        visible = isExpanded,
                                        floating = isFloating,
                                        dockingSide = scope.dockingSide,
                                        haptics = azHaptics,
                                    )
                                }
                            }
                        } else {
                            // COLLAPSED (Rail) view.
                            val isItemVisible = { item: AzNavItem ->
                                if (!item.isRailItem) false else if (!item.isSubItem) true else {
                                    var visible = true
                                    var hId: String? = item.hostId
                                    val seen = HashSet<String>()
                                    while (hId != null && seen.add(hId)) {
                                        val host = scope.navItems.find { it.id == hId }
                                        if (host == null || hostStates[hId] != true || host.isNestedRail) {
                                            visible = false
                                            break
                                        }
                                        hId = host.hostId
                                    }
                                    visible
                                }
                            }
                            val totalItemSize = displayItems.filter(isItemVisible)
                                .sumOf { (activeButtonSize.value + (if (scope.packButtons || isFloating) 0f else AzNavRailDefaults.RailContentVerticalArrangement.value)).toDouble() }.dp
                            val availableSize = if (isHorizontal) maxWidth else maxHeight
                            val isScrollable = totalItemSize > (availableSize * 0.65f)
                            val scrollModifier = if (isScrollable) {
                                if (isHorizontal) Modifier.horizontalScroll(rememberScrollState())
                                else Modifier.verticalScroll(rememberScrollState())
                            } else Modifier
                            val railStripContent: @Composable () -> Unit = {
                                RailItems(
                                    items = railStripItems,
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
                                        // Remember who was touched last: a notice/warning popup
                                        // raised without an explicit source claims this item.
                                        scope.lastTouchedItemId = item.id
                                        azHaptics.commit()
                                        if (item.isHelpItem) {
                                            toggleHelpOverlay(item.id)
                                        }
                                        // Reaching for any other rail item is the user leaving the
                                        // About reader; only the About item itself toggles it.
                                        if (item.isAboutItem) toggleAbout() else dismissFooterScreens()
                                        if (item.collapseOnClick && !scope.noMenu) setExpanded(false)
                                    },
                                    hostStates = hostStates,
                                    packRailButtons = isFloating || scope.packButtons, // Forced pack in FAB mode
                                    visualDockingSide = visualDockingSide,
                                    onItemGloballyPositioned = scope.advancedConfig.onItemGloballyPositioned,
                                    helpEnabled = showHelpOverlay,
                                    rotationDegrees = rotationDegrees,
                                    orientation = orientation,
                                    isRailOpen = isRailOpen,
                                    railItemsCount = railItemsCount
                                )

                                // Optional pinned "More" rail item that opens the More-from-Az carousel.
                                if (scope.advancedConfig.moreFromAzRailItem &&
                                    scope.advancedConfig.moreFromAzEnabled && !isFloating
                                ) {
                                    val moreColor = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
                                    AzButton(
                                        onClick = { hostScope?.showMoreFromAz() },
                                        text = "More",
                                        color = moreColor,
                                        activeColor = moreColor,
                                        shape = scope.defaultShape
                                    )
                                }
                            }
                            if (isHorizontal) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .then(scrollModifier),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    railStripContent()
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(scrollModifier),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    railStripContent()
                                }
                            }
                        }
                    }

                    // FIXED FOOTER (Does not scroll, pinned below menu). The About ("?") button
                    // used to live here, hand-drawn and reachable only in `noMenu` rails; it is now
                    // an ordinary rail item (see `autoAboutItem`), so it is on the rail in every
                    // mode and the developer can move, restyle or replace it.
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
                        val dividerColor = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
                        Column {
                            AzDivider(color = dividerColor)
                            Footer(
                                appName = appName,
                                onToggle = { toggleExpanded() },
                                onUndock = {
                                    isFloating = true
                                    setExpanded(false)
                                    offsetY = screenHeightPx * 0.1f
                                    scope.advancedConfig.onUndock?.invoke()
                                },
                                onSecretClick = onSecretClick,
                                scope = scope,
                                repoUrl = effectiveRepoUrl,
                                footerColor = scope.railAccent,
                                onAboutClick = if (scope.advancedConfig.inAppAbout) {
                                    { setExpanded(false); hostScope?.showAbout() }
                                } else null,
                                showAbout = footerOwnsAbout,
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
        // Auto-edge instruction text comes from LocalAzGuideStrings — CMP consumers override that
        // CompositionLocal to localize; English defaults ship with the module.
        val guideStrings = LocalAzGuideStrings.current
        val openMenuLabel = guideStrings.openMenu
        val tapTemplate = guideStrings.tapItem
        val guidanceEdges = remember(scope.guidanceEdges.toList(), scope.navItems.toList(), openMenuLabel, tapTemplate, scope.advancedConfig.autoGuidanceEdges) {
            // Auto-edges are opt-in: the rail describing its own buttons back to the user is the
            // failure mode, not the feature. Developer-authored edges always apply.
            if (!scope.advancedConfig.autoGuidanceEdges) scope.guidanceEdges.toList()
            else scope.guidanceEdges + computeAutoEdges(
                items = scope.navItems,
                openMenuLabel = openMenuLabel,
                tapLabel = { label -> tapTemplate.replace("%s", label) },
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
            val accent = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
            when (scope.advancedConfig.guidanceStyle) {
                com.hereliesaz.aznavrail.tutorial.AzGuidanceStyle.Escort -> AzEscortOverlay(
                    resolved = frame.resolved,
                    itemBoundsCache = scope.itemBoundsCache,
                    accent = accent,
                    activeItemId = activeItemId,
                    targets = scope.guidanceTargets,
                    controller = guidanceController,
                )
                com.hereliesaz.aznavrail.tutorial.AzGuidanceStyle.Callout -> AzInstructionOverlay(
                    resolved = frame.resolved,
                    itemBoundsCache = scope.itemBoundsCache,
                    accent = accent,
                    activeItemId = activeItemId,
                    targets = scope.guidanceTargets,
                    controller = guidanceController,
                    renderSlot = scope.guidanceRenderer,
                )
            }
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
    index: Int,
    count: Int,
    visible: Boolean,
    floating: Boolean,
    dockingSide: AzDockingSide,
    haptics: com.hereliesaz.aznavrail.internal.AzHaptics
) {
    // DSL `azDivider()` calls declare a synthetic item with `isDivider = true`. Render it as an
    // actual AzDivider — same accent color as the surrounding labels — instead of falling through
    // to the empty-MenuItem code path (which used to render as a blank clickable row).
    if (item.isDivider) {
        val dividerAccent = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
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
    // Same contract as the rail strip: acting on the menu means the user is done with the About /
    // More-from-Az reader, so it gets out of the way.
    val readerHost = LocalAzNavHostScope.current as? AzNavHostScopeImpl
    MenuItem(
        item = item,
        navController = navController,
        isSelected = (item.route != null && item.route == currentDestination) ||
                item.classifiers.any { scope.activeClassifiers.contains(it) },
        isSecondaryActive = item.isSecondaryActive ||
                item.classifiers.any { scope.secondaryClassifiers.contains(it) },
        isTertiaryActive = item.isTertiaryActive ||
                item.classifiers.any { scope.tertiaryClassifiers.contains(it) },
        focusColor = scope.focusColor,
        secondaryColor = scope.secondaryColor,
        tertiaryColor = scope.tertiaryColor,
        onClick = {
            scope.lastTouchedItemId = item.id
            haptics.commit()
            if (item.isAboutItem) {
                if (readerHost?.aboutVisible == true) readerHost.hideAbout() else readerHost?.showAbout()
            } else {
                readerHost?.hideAbout()
                readerHost?.hideMoreFromAz()
            }
            if (item.isHelpItem) {
                onToggleHelp(item.id)
            } else if (!item.isAboutItem) {
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
        // place where the tap becomes a menu-close decision.
        onItemClick = {
            if (item.collapseOnClick) {
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
        activeColor = scope.railAccent,
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
                dockingSide = dockingSide,
                haptics = haptics
            )
        }
    }
}
