// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt
package com.hereliesaz.aznavrail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.isSpecified
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzAboutSurface
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.rememberAzAboutOwnership
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.MenuItem
import com.hereliesaz.aznavrail.internal.RailItems
import com.hereliesaz.aznavrail.internal.rememberAzHaptics
import com.hereliesaz.aznavrail.internal.azUnattachedSubtreeIds
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.internal.rememberAzClosingState
import com.hereliesaz.aznavrail.internal.rememberAzKineticModifier
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzExit
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.service.AzAboutWarmup
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.tutorial.AzEscortOverlay
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
    /**
     * Optional controlled open-state for the rail's menu drawer. When null (the default) the rail
     * manages its own state, seeded by [initiallyExpanded] — today's exact behaviour. When non-null,
     * the caller owns whether the drawer is open; every internal path that would otherwise toggle it
     * (app-icon tap, swipe, outside-tap-to-dismiss, item `collapseOnClick`, undock, etc.) still calls
     * [onExpandedChange] so a controlling caller can react, but the drawer's actual open/closed state
     * always reflects this value on the next recomposition. Mirrors [AzDropdownMenu]'s `expanded`
     * parameter.
     */
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
        val errorMessage = "CRITICAL ERROR: AzNavRail invoked without AzHostActivityLayout!"
        AzNavRailLogger.e("AzNavRail", errorMessage)
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)) {
            Text(errorMessage, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }
    if (providedScope == null) {
        scope.reset()
        scope.apply(content)
        scope.applyRelocReorders()
        scope.applyItemStates()
    }

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
    val effectiveRepoUrl = remember(scope.appRepositoryUrl, packageName) {
        scope.appRepositoryUrl.ifBlank {
            GithubDocsRepository.repoUrlFromPackage(packageName) ?: scope.appRepositoryUrl
        }
    }

    // One haptic handle per rail, built once the scope (and its `vibrate` setting) exists.
    val azHaptics = rememberAzHaptics(scope.vibrate)

    // Ids of the unattached hosts and their whole subtrees. They are declared on this scope like any
    // other item, but they render at their own anchor (see `AzUnattachedRail`), so the rail strip
    // and the drawer both have to skip them.
    val unattachedIds = remember(scope.navItems.toList()) { azUnattachedSubtreeIds(scope.navItems) }

    var internalExpanded by remember { mutableStateOf(if (scope.noMenu) false else initiallyExpanded) }
    // Controlled/uncontrolled pattern mirroring `AzDropdownMenu`'s `expanded ?: internalOpen`: when
    // the caller passes a non-null `expanded`, it always wins on every recomposition; `setIsExpanded`
    // is the single path every internal gesture below routes through, so it only ever mutates the
    // internal fallback while the rail is uncontrolled — a controlled rail's actual state can only
    // change by the caller changing `expanded`. `onExpandedChange` fires either way, via the
    // `LaunchedEffect(isExpanded)` below, since that observes the resulting value regardless of who
    // changed it.
    val isExpanded = expanded ?: internalExpanded
    val setIsExpanded: (Boolean) -> Unit = { value -> if (expanded == null) internalExpanded = value }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var railContentHeight by remember { mutableStateOf(0f) }
    val hostScope = LocalAzNavHostScope.current as? AzNavHostScopeImpl
    val showHelpOverlay = hostScope?.helpVisible == true

    // The About ("?") affordance is a rail item like any other, appended last — unless the developer
    // declared their own with `azAboutRailItem`, in which case theirs stands where they put it. It
    // persists; it is not fixed.
    // Warm the About reader's content now, in the background, so opening it shows the page rather
    // than a spinner. Cheap: cached, conditional requests that usually come back 304.
    if (scope.advancedConfig.inAppAbout) {
        AzAboutWarmup(
            repoUrl = effectiveRepoUrl,
            moreFromAzEnabled = scope.advancedConfig.moreFromAzEnabled,
            moreFromAzJsonUrl = scope.advancedConfig.moreFromAzJsonUrl,
        )
    }

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
    var wasFloatingOpenBeforeDrag by remember { mutableStateOf(false) }
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    val onSecretClick = SecretScreens(secLoc = scope.advancedConfig.secLoc, secLocPort = scope.advancedConfig.secLocPort)

    LaunchedEffect(showFloatingButtons, railContentHeight) {
        if (isFloating) {
            val minY = screenHeightPx * 0.1f
            val maxY = maxOf(minY, (screenHeightPx * 0.9f) - railContentHeight)
            if (offsetY > maxY) offsetY = maxY
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

    val baseButtonSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth
    val calculatedShrunkSize = baseButtonSize * 0.75f
    val actualShrunkSize = maxOf(calculatedShrunkSize, 35.dp)
    val targetShrunkSize = minOf(baseButtonSize, actualShrunkSize)
    val activeButtonSize = if (isVerticalNestedRailOpen) targetShrunkSize else baseButtonSize

    val targetRailWidth = if (isExpanded) scope.expandedWidth
    else if (isVerticalNestedRailOpen) 60.dp
    else scope.collapsedWidth

    val railWidth by animateDpAsState(targetValue = targetRailWidth)

    val effectiveNavController = navController ?: rememberNavController()
    val navBackStackEntry by effectiveNavController.currentBackStackEntryAsState()
    val actualCurrentDestination = currentDestination ?: navBackStackEntry?.destination?.route
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val initiallyExpandedSeen = remember { mutableMapOf<String, Boolean>() }
    val expandWhenSeen = remember { mutableMapOf<String, Boolean>() }

    val toggleHelpOverlay = { itemId: String? ->
        if (hostScope?.helpVisible == true) {
            hostScope.hideHelp()
            scope.advancedConfig.onDismissHelp?.invoke()
        } else {
            val scopeId = itemId?.let { id ->
                scope.navItems.firstOrNull { parent ->
                    parent.isNestedRail && parent.nestedRailItems?.any { it.id == id } == true
                }?.id
            }
            hostScope?.showHelp(scopeId)
        }
    }

    LaunchedEffect(scope.navItems) {
        scope.navItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
                scope.transientCyclerOptions.putIfAbsent(item.id, item.selectedOption ?: "")
            }
            if (item.isHost) {
                if (item.initiallyExpanded && initiallyExpandedSeen[item.id] != true) hostStates[item.id] =
                    true
                initiallyExpandedSeen[item.id] = item.initiallyExpanded
            }
        }
    }

    LaunchedEffect(scope.expandWhenMap.keys.sorted().joinToString(",")) {
        scope.expandWhenMap.toMap().forEach { (id, cond) ->
            launch {
                merge(
                    snapshotFlow { cond() },
                    flow { while (true) { emit(cond()); delay(300) } },
                ).distinctUntilChanged().collect { conditionNow ->
                    val before = expandWhenSeen[id]
                    // `expandWhen` is shared, rising/falling-edge machinery for two different kinds
                    // of "open": a host item's inline sub-items (`hostStates`) and a nested rail's
                    // popup (`scope.nestedRailOpenId`, the single source of truth for which nested
                    // rail is open). Which one this `id` drives depends on what kind of item declared
                    // the `expandWhen` — a nested rail's own id, or an ordinary host's.
                    val isNestedRailTarget = scope.navItems.any { it.id == id && it.isNestedRail }
                    if (before == null) {
                        if (conditionNow) {
                            if (isNestedRailTarget) scope.nestedRailOpenId = id else hostStates[id] = true
                        }
                    } else if (before != conditionNow) {
                        if (isNestedRailTarget) {
                            if (conditionNow) {
                                scope.nestedRailOpenId = id
                            } else if (scope.nestedRailOpenId == id) {
                                scope.nestedRailOpenId = null
                            }
                        } else {
                            hostStates[id] = conditionNow
                        }
                    }
                    expandWhenSeen[id] = conditionNow
                }
            }
        }
    }


    // The documented "rail as a system-wide overlay" mode: when the developer supplied an
    // `overlayService`, undocking hands off to that service instead of only floating in-app. The
    // config has always been collected; this is the edge that finally acts on it.
    val launchOverlay = run {
        val overlayContext = androidx.compose.ui.platform.LocalContext.current
        { serviceClass: Any? ->
            if (serviceClass is Class<*>) {
                runCatching { com.hereliesaz.aznavrail.internal.OverlayHelper.launch(overlayContext, serviceClass) }
                    .onFailure { AzNavRailLogger.e("AzNavRail", "Failed to start overlay service: $it") }
            }
        }
    }
    LaunchedEffect(isFloating) {
        if (isFloating) scope.advancedConfig.overlayService?.let { launchOverlay(it) }
    }

    LaunchedEffect(isExpanded) {
        onExpandedChange?.invoke(isExpanded)
        if (!isExpanded) {
            cyclerStates.forEach { (id, state) ->
                if (state.job != null) {
                    state.job.cancel()
                    scope.navItems.find { it.id == id }?.let { item ->
                        coroutineScope.launch {
                            val options = item.options ?: emptyList()
                            val currentIndexInVm = options.indexOf(item.selectedOption)
                            val targetIndex = options.indexOf(state.displayedOption)
                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                scope.onClickMap[item.id]?.let { onClick -> repeat(clicksToCatchUp) { onClick() } }
                            }
                            cyclerStates[id] = state.copy(job = null)
                        }
                    }
                }
            }
        }
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

    fun toggleExpanded() {
        dismissFooterScreens()
        if (!showHelpOverlay) {
            if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else if (scope.noMenu) {
                scope.isFoldedUp = !scope.isFoldedUp
            } else {
                setIsExpanded(!isExpanded)
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val isHorizontal = orientation == AzOrientation.Horizontal

    // Which pointer gestures this rail can actually act on. Everything else must fall through to the
    // app: the rail is drawn over the host's content, and a listener that answers no gesture is
    // simply a hole in the app's own input.
    val canSwipeMenu = !disableSwipeToOpen && !scope.noMenu
    val railHandlesDrags = isFloating || scope.advancedConfig.enableRailDragging || canSwipeMenu

    val sizeModifier = if (isFloating || (scope.noMenu && scope.isFoldedUp)) {
        val maxFabSize = (configuration.screenHeightDp * 0.8f).dp
        if (!isHorizontal) Modifier
            .width(railWidth)
            .heightIn(max = maxFabSize)
            .wrapContentHeight()
        else Modifier
            .height(railWidth)
            .widthIn(max = maxFabSize)
            .wrapContentWidth()
    } else {
        if (!isHorizontal) Modifier
            .width(railWidth)
            .fillMaxHeight()
        else Modifier
            .height(railWidth)
            .fillMaxWidth()
    }

    val safeTopDp = (configuration.screenHeightDp * 0.1f).dp
    val safeBottomDp = (configuration.screenHeightDp * 0.1f).dp
    val safeZoneModifier = if (!isFloating && !isHorizontal) Modifier.padding(
        top = safeTopDp,
        bottom = safeBottomDp
    ) else Modifier

    // Deliberately no window-wide tap listener on this Box. The rail is laid out over the entire
    // window, so a `detectTapGestures` here eats every tap meant for the app beneath it. Collapsing
    // on an outside tap is the scrim's job below — it is inset to exclude the rail, and it only
    // exists while the menu is actually open.
    Box(
        modifier = modifier,
        contentAlignment = if (isFloating) Alignment.TopStart else railAlignment
    ) {
        val swipeWidthIncrease = if (isExpanded) 40.dp else 0.dp
        val snapBackRadius = with(density) { 36.dp.toPx() }

        if (isExpanded && !isFloating) {
            val scrimPadding = when {
                !isHorizontal && visualDockingSide == AzDockingSide.LEFT -> Modifier.padding(start = railWidth)
                !isHorizontal && visualDockingSide == AzDockingSide.RIGHT -> Modifier.padding(end = railWidth)
                railAlignment == Alignment.BottomStart || railAlignment == Alignment.BottomCenter || railAlignment == Alignment.BottomEnd -> Modifier.padding(
                    bottom = railWidth
                )
                else -> Modifier.padding(top = railWidth)
            }
            val scrimColor = if (scope.dimBehindMenu) Color.Black.copy(
                alpha = scope.dimBehindMenuAlpha.coerceIn(
                    0f,
                    1f
                )
            ) else Color.Transparent
            Box(
                Modifier
                    .matchParentSize()
                    .then(scrimPadding)
                    .background(scrimColor)
                    .pointerInput(Unit) { detectTapGestures(onTap = { setIsExpanded(false) }) })
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(safeZoneModifier)
                .then(if (isHorizontal) Modifier.height(railWidth) else Modifier.width(railWidth + swipeWidthIncrease))
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
                                    wasFloatingOpenBeforeDrag =
                                        showFloatingButtons; showFloatingButtons = false
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
                                    val maxX = screenWidthPx - railWidth.toPx()
                                    offsetX = offsetX.coerceIn(minX, maxX)
                                } else {
                                    val absX = kotlin.math.abs(dragAmount.x);
                                    val absY = kotlin.math.abs(dragAmount.y)
                                    if (scope.advancedConfig.enableRailDragging && absY > 20 && absY > absX) {
                                        change.consume()
                                        isFloating = true; setIsExpanded(false); offsetX = 0f; offsetY =
                                            screenHeightPx * 0.1f; showFloatingButtons =
                                            false; wasFloatingOpenBeforeDrag = false
                                        if (scope.vibrate) haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    } else if (canSwipeMenu) {
                                        val opening = if (visualDockingSide == AzDockingSide.LEFT) {
                                            dragAmount.x > 20 && !isExpanded
                                        } else {
                                            dragAmount.x < -20 && !isExpanded
                                        }
                                        val closing = if (visualDockingSide == AzDockingSide.LEFT) {
                                            dragAmount.x < -20 && isExpanded
                                        } else {
                                            dragAmount.x > 20 && isExpanded
                                        }
                                        if (opening || closing) {
                                            change.consume()
                                            setIsExpanded(opening)
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isFloating) {
                                    if (wasFloatingOpenBeforeDrag) showFloatingButtons = true
                                    if (offsetX * offsetX + offsetY * offsetY < snapBackRadius * snapBackRadius) {
                                        isFloating = false; offsetX = 0f; offsetY = 0f
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
                color = Color.Transparent,
                tonalElevation = if (isExpanded && !isFloating) 2.dp else 0.dp
            ) {
                @Composable
                fun Header() {
                    val headerModifier =
                        if (isHorizontal) Modifier
                            .padding(AzNavRailDefaults.HeaderPadding)
                            .width(AzNavRailDefaults.HeaderHeightDp)
                            .fillMaxHeight()
                        else Modifier
                            .padding(AzNavRailDefaults.HeaderPadding)
                            .height(AzNavRailDefaults.HeaderHeightDp)
                            .fillMaxWidth()
                    Box(
                        modifier = headerModifier.pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { toggleExpanded() },
                                onLongPress = {
                                    if (scope.advancedConfig.enableRailDragging) {
                                        isFloating = !isFloating
                                        if (isFloating) {
                                            setIsExpanded(false); offsetY = screenHeightPx * 0.1f
                                        } else {
                                            offsetX = 0f; offsetY = 0f
                                        }
                                        if (scope.vibrate) haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                    }
                                }
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (scope.displayAppName && !isFloating) {
                            Text(
                                appName,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.requiredWidth(1000.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            val headerIconDiameter =
                                if (scope.headerIconSize != androidx.compose.ui.unit.Dp.Unspecified) scope.headerIconSize else minOf(
                                    100.dp,
                                    targetRailWidth
                                )
                            Box(
                                modifier = Modifier.size(headerIconDiameter),
                                contentAlignment = Alignment.Center
                            ) {
                                if (appIcon != null) {
                                    val clipModifier = when (scope.headerIconShape) {
                                        AzHeaderIconShape.CIRCLE -> Modifier.clip(CircleShape)
                                        AzHeaderIconShape.ROUNDED -> Modifier.clip(
                                            RoundedCornerShape(12.dp)
                                        )

                                        else -> Modifier
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(appIcon),
                                        contentDescription = "App Icon",
                                        modifier = clipModifier.fillMaxSize()
                                    )
                                } else Icon(Icons.Default.Menu, "Menu")
                            }
                        }
                    }
                }

                @Composable
                fun MainContent(isRailOpen: Boolean, railItemsCount: Int) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val hasExplicitHelpItem =
                            scope.navItems.any { it.isHelpItem || it.id == AzNavRailDefaults.AUTO_HELP_ID }
                        val attachedItems =
                            if (unattachedIds.isEmpty()) scope.navItems
                            else scope.navItems.filterNot { it.id in unattachedIds }
                        val displayItems =
                            if (scope.advancedConfig.helpEnabled && !hasExplicitHelpItem) {
                                attachedItems + AzNavItem.Help(
                                    id = AzNavRailDefaults.AUTO_HELP_ID,
                                    isRailItem = false
                                )
                            } else attachedItems
                        // The rail strip ends with the About ("?") button. The drawer does not
                        // need one — its footer already carries About — so this is appended for the
                        // strip only, and `reverseLayout` still decides which end "last" is.
                        val railStripItems =
                            if (autoAboutItem != null) attachedItems + autoAboutItem else attachedItems
                        val orderedItems =
                            if (reverseLayout) displayItems.reversed() else displayItems
                        val orderedRailItems =
                            if (reverseLayout) railStripItems.reversed() else railStripItems
                        val topLevelItems = orderedItems.filter { !it.isSubItem }
                        val menuRendered = rememberAzClosingState(
                            open = isExpanded,
                            exit = scope.itemExit,
                            count = topLevelItems.size,
                            staggerMs = scope.entranceStaggerMs,
                            durationMs = scope.entranceDurationMs
                        )

                        if (menuRendered) {
                            val scrollState = rememberScrollState()
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)) {
                                topLevelItems.forEachIndexed { index, item ->
                                    // The tapped entry's slot is held open (empty) while its label
                                    // travels out of the menu, so the label never animates in two
                                    // places at once.
                                    hostScope?.dissolving?.takeIf { it.itemId == item.id }?.let {
                                        Spacer(Modifier.height(with(density) { it.bounds.height.toDp() }))
                                    } ?: MenuItemNode(
                                        item = item,
                                        allItems = orderedItems,
                                        navController = effectiveNavController,
                                        currentDestination = actualCurrentDestination,
                                        scope = scope,
                                        hostStates = hostStates,
                                        showHelpOverlay = showHelpOverlay,
                                        onToggleHelp = { toggleHelpOverlay(it) },
                                        onCollapseMenu = { setIsExpanded(false) },
                                        onDissolveTap = { itemId, text ->
                                            scope.itemBoundsCache[itemId]?.let {
                                                // Hand it to the host: by the time this animates,
                                                // this menu is gone, and the label has to cross the
                                                // whole window.
                                                hostScope?.dissolve(
                                                    com.hereliesaz.aznavrail.internal.DissolveState(
                                                        itemId,
                                                        text,
                                                        it
                                                    )
                                                )
                                            }
                                        },
                                        index = index,
                                        count = topLevelItems.size,
                                        visible = isExpanded,
                                        floating = isFloating,
                                        dockingSide = scope.dockingSide,
                                        haptics = azHaptics
                                    )
                                }
                            }
                        } else {
                            val isItemVisible = { item: AzNavItem ->
                                if (!item.isRailItem) false else if (!item.isSubItem) true else {
                                    var visible = true;
                                    var hId: String? = item.hostId;
                                    val seen = HashSet<String>()
                                    while (hId != null && seen.add(hId)) {
                                        val host = scope.navItems.find { it.id == hId }
                                        if (host == null || hostStates[hId] != true || host.isNestedRail) {
                                            visible = false; break
                                        }
                                        hId = host.hostId
                                    }
                                    visible
                                }
                            }
                            val totalItemSize = orderedRailItems.filter(isItemVisible)
                                .sumOf { (activeButtonSize.value + (if (scope.packButtons || isFloating) 0f else AzNavRailDefaults.RailContentVerticalArrangement.value)).toDouble() }.dp
                            val availableSize = if (isHorizontal) maxWidth else maxHeight
                            val isScrollable = totalItemSize > (availableSize * 0.65f)
                            val scrollModifier = if (isScrollable) {
                                if (isHorizontal) Modifier.horizontalScroll(rememberScrollState()) else Modifier.verticalScroll(
                                    rememberScrollState()
                                )
                            } else Modifier

                            if (isHorizontal) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .then(scrollModifier),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RailItems(
                                        items = orderedRailItems,
                                        scope = scope,
                                        navController = effectiveNavController,
                                        currentDestination = actualCurrentDestination,
                                        buttonSize = activeButtonSize,
                                        onRailCyclerClick = { item ->
                                            handleRailCyclerClick(
                                                item,
                                                cyclerStates,
                                                scope,
                                                coroutineScope
                                            )
                                        },
                                        onItemSelected = { item ->
                                            // Remember who was touched last: a notice/warning popup
                                            // raised without an explicit source claims this item.
                                            scope.lastTouchedItemId = item.id
                                            azHaptics.commit()
                                            if (item.isHelpItem) toggleHelpOverlay(item.id)
                                            // Reaching for any other rail item is the user leaving
                                            // the About reader; only About itself toggles it.
                                            if (item.isAboutItem) toggleAbout() else dismissFooterScreens()
                                            if (item.collapseOnClick && !scope.noMenu) setIsExpanded(false)
                                        },
                                        hostStates = hostStates,
                                        packRailButtons = isFloating || scope.packButtons,
                                        visualDockingSide = visualDockingSide,
                                        onItemGloballyPositioned = scope.advancedConfig.onItemGloballyPositioned,
                                        helpEnabled = showHelpOverlay,
                                        rotationDegrees = rotationDegrees,
                                        orientation = orientation,
                                        isRailOpen = isRailOpen,
                                        railItemsCount = railItemsCount
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(scrollModifier),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RailItems(
                                        items = orderedRailItems,
                                        scope = scope,
                                        navController = effectiveNavController,
                                        currentDestination = actualCurrentDestination,
                                        buttonSize = activeButtonSize,
                                        onRailCyclerClick = { item ->
                                            handleRailCyclerClick(
                                                item,
                                                cyclerStates,
                                                scope,
                                                coroutineScope
                                            )
                                        },
                                        onItemSelected = { item ->
                                            // Remember who was touched last: a notice/warning popup
                                            // raised without an explicit source claims this item.
                                            scope.lastTouchedItemId = item.id
                                            if (item.isHelpItem) toggleHelpOverlay(item.id)
                                            // Reaching for any other rail item is the user leaving
                                            // the About reader; only About itself toggles it.
                                            if (item.isAboutItem) toggleAbout() else dismissFooterScreens()
                                            if (item.collapseOnClick && !scope.noMenu) setIsExpanded(false)
                                        },
                                        hostStates = hostStates,
                                        packRailButtons = isFloating || scope.packButtons,
                                        visualDockingSide = visualDockingSide,
                                        onItemGloballyPositioned = scope.advancedConfig.onItemGloballyPositioned,
                                        helpEnabled = showHelpOverlay,
                                        rotationDegrees = rotationDegrees,
                                        orientation = orientation,
                                        isRailOpen = isRailOpen,
                                        railItemsCount = railItemsCount
                                    )

                                    // Optional pinned "More" rail item that opens the More-from-Az
                                    // carousel. The CMP module has always drawn this; `moreRailItem`
                                    // was accepted here but never rendered.
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
                            }
                        }
                    }
                }

                @Composable
                fun Foot() {
                    // The About ("?") button used to live here, hand-drawn and reachable only in
                    // `noMenu` rails; it is now an ordinary rail item (see `autoAboutItem`), so it
                    // is on the rail in every mode and the developer can move, restyle or replace it.
                    val footerRendered = rememberAzClosingState(
                        open = isExpanded,
                        exit = AzExit.Turnstile,
                        count = 1,
                        staggerMs = scope.entranceStaggerMs,
                        durationMs = scope.entranceDurationMs
                    )
                    if (scope.showFooter && footerRendered) {
                        val footerMenuCount = scope.navItems.count { !it.isSubItem }
                        val divColor =
                            scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
                        if (isHorizontal) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(divColor))
                                Footer(
                                    appName = appName,
                                    onToggle = { toggleExpanded() },
                                    onUndock = {
                                        isFloating = true; setIsExpanded(false); offsetY =
                                        screenHeightPx * 0.1f; scope.advancedConfig.onUndock?.invoke()
                                    },
                                    onSecretClick = onSecretClick,
                                    scope = scope,
                                    repoUrl = effectiveRepoUrl,
                                    footerColor = scope.railAccent,
                                    onAboutClick = if (scope.advancedConfig.inAppAbout) {
                                        { setIsExpanded(false); hostScope?.showAbout() }
                                    } else null,
                                    showAbout = footerOwnsAbout,
                                    visible = isExpanded,
                                    menuItemCount = footerMenuCount,
                                    staggerMs = scope.entranceStaggerMs,
                                    durationMs = scope.entranceDurationMs,
                                    easing = scope.entranceEasing)
                            }
                        } else {
                            Column {
                                com.hereliesaz.aznavrail.AzDivider(color = divColor)
                                Footer(
                                    appName = appName,
                                    onToggle = { toggleExpanded() },
                                    onUndock = {
                                        isFloating = true; setIsExpanded(false); offsetY =
                                        screenHeightPx * 0.1f; scope.advancedConfig.onUndock?.invoke()
                                    },
                                    onSecretClick = onSecretClick,
                                    scope = scope,
                                    repoUrl = effectiveRepoUrl,
                                    footerColor = scope.railAccent,
                                    onAboutClick = if (scope.advancedConfig.inAppAbout) {
                                        { setIsExpanded(false); hostScope?.showAbout() }
                                    } else null,
                                    showAbout = footerOwnsAbout,
                                    visible = isExpanded,
                                    menuItemCount = footerMenuCount,
                                    staggerMs = scope.entranceStaggerMs,
                                    durationMs = scope.entranceDurationMs,
                                    easing = scope.entranceEasing)
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

                if (isHorizontal) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Header()
                        if (railItemsRendered) {
                            Box(modifier = Modifier.weight(1f)) { MainContent(isRailOpen, railItemsCount) }
                            Foot()
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Header()
                        if (railItemsRendered) {
                            Box(modifier = Modifier.weight(1f)) { MainContent(isRailOpen, railItemsCount) }
                            Foot()
                        }
                    }
                }

            }
        }
    }

    // Guidance + Dissolve Overlay
    val guidanceFallback = rememberAzGuidanceController()
    val guidanceController = LocalAzGuidanceController.current ?: guidanceFallback
    val guidanceActiveItemId = scope.navItems.firstOrNull { item ->
        (item.route != null && item.route == actualCurrentDestination) || item.classifiers.any {
            scope.activeClassifiers.contains(it)
        }
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
                helpOpen = showHelpOverlay
            )
        })
    val guidanceGoalsMap =
        remember(scope.guidanceGoals.toList()) { scope.guidanceGoals.associateBy { it.id } }

    LaunchedEffect(activeStatuses, guidanceGoalsMap, guidanceController) {
        guidanceGoalsMap.values.forEach { goal ->
            if (goal.autoStartWhen != null && goal.autoStartWhen in activeStatuses && !guidanceController.isCompleted(
                    goal.id
                ) && !guidanceController.isDismissed(goal.id) && goal.id !in guidanceController.activeGoals
            ) {
                guidanceController.activate(goal.id)
            }
        }
    }

    val guidanceSuppressed by rememberGuidanceSuppressed(scope.guidanceSuppressors)

    if (guidanceController.enabled && hostScope?.aboutVisible != true && hostScope?.moreFromAzVisible != true) {
        val openMenuLabel = stringResource(R.string.az_guide_open_menu)
        val tapTemplate = stringResource(R.string.az_guide_tap_item)
        val guidanceEdges = remember(
            scope.guidanceEdges.toList(),
            scope.navItems.toList(),
            openMenuLabel,
            tapTemplate,
            scope.advancedConfig.autoGuidanceEdges,
        ) {
            // Auto-edges are opt-in: the rail describing its own buttons back to the user is the
            // failure mode, not the feature. Developer-authored edges always apply.
            if (!scope.advancedConfig.autoGuidanceEdges) scope.guidanceEdges.toList()
            else scope.guidanceEdges + computeAutoEdges(
                items = scope.navItems,
                openMenuLabel = openMenuLabel,
                tapLabel = { label -> String.format(tapTemplate, label) })
        }
        val frame = remember(
            guidanceEdges,
            guidanceGoalsMap,
            activeStatuses
        ) {
            derivedStateOf {
                routeInstructions(
                    edges = guidanceEdges,
                    goals = guidanceGoalsMap,
                    activeGoalIds = guidanceController.activeGoals,
                    activeStatuses = activeStatuses,
                    stepIndexOf = { guidanceController.stepIndex(it) },
                    consumedStatuses = guidanceController.consumedStatuses
                )
            }
        }.value
        LaunchedEffect(
            frame.reachedGoals,
            guidanceController
        ) { frame.reachedGoals.forEach { guidanceController.markReached(it) } }
        val pendingConsume = remember(guidanceController) { mutableSetOf<String>() }
        LaunchedEffect(frame.resolved, activeStatuses, guidanceController) {
            frame.resolved.forEach { r -> r.edge.to?.let { if (it !in pendingConsume) pendingConsume.add(it) } }
            pendingConsume.forEach { if (it in activeStatuses) guidanceController.consume(it) }
        }
        LaunchedEffect(
            frame.resolved,
            guidanceController
        ) {
            frame.resolved.forEach { r ->
                if (r.stepTotal > 1 && guidanceController.stepIndex(r.edge.stepKey()) < r.stepIndex) guidanceController.setStep(
                    r.edge.stepKey(),
                    r.stepIndex
                )
            }
        }
        val snapshots = remember(
            frame.resolved,
            guidanceActiveItemId,
            scope.itemBoundsCache.toMap()
        ) {
            frame.resolved.map { r ->
                val h = r.instruction.highlight;
                val shape = if (h is AzGuideHighlight.Target) null else h.resolveShape(
                    scope.itemBoundsCache,
                    guidanceActiveItemId,
                    emptyMap()
                ); r.toSnapshot(shape, guidanceActiveItemId)
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
                    activeItemId = guidanceActiveItemId,
                    targets = scope.guidanceTargets,
                    controller = guidanceController,
                )
                com.hereliesaz.aznavrail.tutorial.AzGuidanceStyle.Callout -> AzInstructionOverlay(
                    resolved = frame.resolved,
                    itemBoundsCache = scope.itemBoundsCache,
                    accent = accent,
                    activeItemId = guidanceActiveItemId,
                    targets = scope.guidanceTargets,
                    controller = guidanceController,
                    renderSlot = scope.guidanceRenderer
                )
            }
        }
    } else {
        LaunchedEffect(guidanceController) { guidanceController.publishCurrent(emptyList()) }
    }

}

private fun handleRailCyclerClick(
    item: AzNavItem,
    cyclerStates: MutableMap<String, CyclerTransientState>,
    scope: AzNavRailScopeImpl,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val state = cyclerStates[item.id]
    if (state != null && !item.disabled) {
        state.job?.cancel()
        val options = item.options ?: emptyList()
        val enabledOptions = options.filterNot { it in (item.disabledOptions ?: emptyList()) }
        if (enabledOptions.isNotEmpty()) {
            val currentIndex = enabledOptions.indexOf(state.displayedOption)
            val nextOption = enabledOptions[(currentIndex + 1) % enabledOptions.size]
            scope.transientCyclerOptions[item.id] = nextOption
            cyclerStates[item.id] =
                state.copy(displayedOption = nextOption, job = coroutineScope.launch {
                    delay(1000L)
                    scope.onClickMap[item.id]?.invoke()
                    cyclerStates[item.id] = cyclerStates[item.id]?.copy(job = null) ?: state
                })
        }
        scope.advancedConfig.onInteraction?.invoke(item.id, item)
    }
}

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
    onDissolveTap: (itemId: String, text: String) -> Unit = { _, _ -> },
    index: Int,
    count: Int,
    visible: Boolean,
    floating: Boolean,
    dockingSide: AzDockingSide,
    haptics: com.hereliesaz.aznavrail.internal.AzHaptics
) {
    if (item.isDivider) {
        val dividerAccent = scope.railAccent.takeOrElse { MaterialTheme.colorScheme.primary }
        com.hereliesaz.aznavrail.AzDivider(color = dividerAccent)
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
        tiltOnPress = scope.tiltOnPress && !item.isRelocItem,
        maxTiltDegrees = scope.maxTiltDegrees,
        dockingSide = dockingSide,
        floating = floating
    )
    val readerHost = LocalAzNavHostScope.current as? AzNavHostScopeImpl
    MenuItem(
        item = item,
        navController = navController,
        isSelected = (item.route != null && item.route == currentDestination) || item.classifiers.any {
            scope.activeClassifiers.contains(it)
        },
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
            // Same contract as the rail strip: acting on the menu means the user is done with the
            // About / More-from-Az reader, so it gets out of the way. Only About itself toggles it.
            if (item.isAboutItem) {
                if (readerHost?.aboutVisible == true) readerHost.hideAbout() else readerHost?.showAbout()
            } else {
                readerHost?.hideAbout()
                readerHost?.hideMoreFromAz()
            }
            when {
                item.isHelpItem -> onToggleHelp(item.id)
                item.isAboutItem -> Unit
                else -> scope.onClickMap[item.id]?.invoke()
            }
            scope.advancedConfig.onInteraction?.invoke(item.id, item)
            if (item.collapseOnClick) onCollapseMenu()
        },
        onCyclerClick = {
            scope.onClickMap[item.id]?.invoke(); scope.advancedConfig.onInteraction?.invoke(
            item.id,
            item
        )
        },
        onToggle = {
            scope.advancedConfig.onInteraction?.invoke(
                item.id,
                item
            ); if (item.collapseOnClick) onCollapseMenu()
        },
        onItemClick = {
            if (item.collapseOnClick) {
                onDissolveTap(item.id, item.menuText ?: item.text); onCollapseMenu()
            }
        },
        onHostClick = {
            val newHostState = !(hostStates[item.id] ?: false); hostStates[item.id] =
            newHostState; scope.onExpandedChangeMap[item.id]?.invoke(newHostState)
        },
        onItemGloballyPositioned = { id, bounds ->
            scope.itemBoundsCache[id] =
                bounds; scope.advancedConfig.onItemGloballyPositioned?.invoke(id, bounds)
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
