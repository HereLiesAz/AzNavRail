package com.hereliesaz.aznavrail

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.bottomsheet.AzBottomSheet
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.internal.AboutOverlay
import com.hereliesaz.aznavrail.internal.AzBottomSheetItem
import com.hereliesaz.aznavrail.internal.AzDropdownTriggerButton
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzTitleTriggerSlot
import com.hereliesaz.aznavrail.internal.AzUnattachedRail
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzRailLayoutHelper
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MoreFromAzOverlay
import com.hereliesaz.aznavrail.internal.PlatformHostChrome
import com.hereliesaz.aznavrail.internal.azResolveSafeBottom
import com.hereliesaz.aznavrail.internal.rememberAzKineticModifier
import com.hereliesaz.aznavrail.internal.rememberDeviceRotationDegrees
import com.hereliesaz.aznavrail.internal.rememberEffectiveAppMeta
import com.hereliesaz.aznavrail.internal.rememberIsGestureNav
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzEasing
import com.hereliesaz.aznavrail.model.AzExit
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.tutorial.AzGuidanceController
import com.hereliesaz.aznavrail.tutorial.LocalAzGuidanceController
import com.hereliesaz.aznavrail.tutorial.rememberAzGuidanceController

/**
 * Extended scope for configuring both the rail and the hosted content. Inherits [AzNavRailScope]
 * (rail items + config) and adds onscreen/background layers and bottom sheets.
 *
 * Port note vs the Android sibling: [navController] is nullable here — the CMP rail/dropdown tolerate
 * a null controller when used standalone (they skip route-based navigation). [AzHostActivityLayout]
 * always supplies a real controller.
 */
interface AzNavHostScope : AzNavRailScope {
    /** The active [NavHostController], or null when none has been wired (standalone use). */
    val navController: NavHostController?

    /** The current docking side of the rail. */
    val dockingSide: AzDockingSide

    /**
     * Adds a background layer behind the main content. Backgrounds form their own "book" of pages
     * beneath the onscreen book. [weight] breaks Z-ties within a [page]; a **higher** page draws
     * further back. Honoured only when the host's `pagesEnabled` is `true`.
     */
    fun background(weight: Int = 0, page: Float = 0f, content: @Composable () -> Unit)

    /**
     * Adds content to the main screen area, respecting safe zones and rail padding. Items sharing a
     * [page] are co-planar (positioned by [alignment]); a **higher** page draws further back.
     * [alignment] is mirrored automatically when the rail docks on the right.
     */
    fun onscreen(alignment: Alignment = Alignment.TopStart, page: Float = 0f, content: @Composable () -> Unit)

    /**
     * Registers a popup window driven by [controller], drawn over everything else in the host.
     *
     * A popup is **bound to a rail item**: `controller.show(itemId = "sync")` names one explicitly,
     * and `controller.show()` binds to whichever rail item the user touched last. The body receives
     * that item as an [AzPopupItemHandle] through its [AzPopupScope], so the two share state in both
     * directions — the popup can read the item and write back to it (badge, its own loading
     * animation, an alert flag), and those writes outlive the DSL re-running until the popup clears
     * them.
     *
     * A [AzPopupKind.NOTICE] or [AzPopupKind.WARNING] popup additionally redraws its bound item as a
     * **yellow, rounded-corner triangle outline** for as long as it is up, and restores the item
     * when it closes.
     *
     * @param controller Created with [rememberAzPopupController] outside the DSL so its state
     *   survives recomposition.
     * @param dismissOnOutsideTap Whether tapping the scrim closes the popup.
     * @param content The popup's body. Defaults to the built-in title/message/OK panel.
     */
    fun azPopup(
        controller: AzPopupController,
        dismissOnOutsideTap: Boolean = true,
        content: @Composable AzPopupScope.() -> Unit = { AzPopupBody() },
    )

    /**
     * Registers a bottom sheet that draws above the rail/menu/onscreen area and spans to the bottom
     * edge (so the HIDDEN detent is reachable from the nav-bar edge). Use
     * [com.hereliesaz.aznavrail.bottomsheet.rememberAzSheetController] to create [controller] outside
     * the DSL so its detent survives recomposition.
     */
    fun azBottomSheet(
        controller: AzSheetController,
        config: AzSheetConfig = AzSheetConfig(),
        onSwipeLeft: (() -> Unit)? = null,
        onSwipeRight: (() -> Unit)? = null,
        content: @Composable BoxScope.() -> Unit,
    )
}

/** Holds a background layer registered via [AzNavHostScope.background]. */
data class AzBackgroundItem(val weight: Int, val page: Float = 0f, val content: @Composable () -> Unit)

/** Holds an onscreen content item registered via [AzNavHostScope.onscreen]. */
data class AzOnscreenItem(val alignment: Alignment, val page: Float = 0f, val content: @Composable () -> Unit)

/** Orders the background "book" back-to-front. Higher page draws further back; weight breaks ties. */
internal fun azOrderBackgrounds(items: List<AzBackgroundItem>, pagesEnabled: Boolean): List<AzBackgroundItem> =
    if (pagesEnabled) items.sortedWith(compareByDescending<AzBackgroundItem> { it.page }.thenBy { it.weight })
    else items.sortedBy { it.weight }

/** Orders the onscreen "book" back-to-front. Higher page draws further back; declaration order kept within a page. */
internal fun azOrderOnscreen(items: List<AzOnscreenItem>, pagesEnabled: Boolean): List<AzOnscreenItem> =
    if (pagesEnabled) items.sortedByDescending { it.page } else items

/** Concrete [AzNavHostScope] used by [AzHostActivityLayout]. */
class AzNavHostScopeImpl(
    private val railScope: AzNavRailScopeImpl = AzNavRailScopeImpl()
) : AzNavHostScope, AzNavRailScope by railScope {
    private var _navController: NavHostController? = null
    override val navController: NavHostController? get() = _navController
    override val dockingSide: AzDockingSide get() = railScope.dockingSide

    val backgrounds = mutableListOf<AzBackgroundItem>()
    val onscreenItems = mutableListOf<AzOnscreenItem>()
    internal val bottomSheets = mutableListOf<AzBottomSheetItem>()
    internal val popups = mutableListOf<AzPopupItem>()

    /**
     * Drop-down triggers lifted out of their call sites and hosted next to the screen title, in
     * registration order (which is declaration order, since `onscreen` blocks compose in order).
     *
     * Registered from `AzDropdownMenu`'s `DisposableEffect`, so — unlike [onscreenItems] — this is
     * NOT cleared by [resetHost]: it is keyed to composition lifetime, not to the DSL pass.
     */
    internal val titleTriggers = mutableStateListOf<AzTitleTriggerSlot>()

    internal fun registerTitleTrigger(slot: AzTitleTriggerSlot) {
        if (!titleTriggers.contains(slot)) titleTriggers.add(slot)
    }

    internal fun unregisterTitleTrigger(slot: AzTitleTriggerSlot) {
        titleTriggers.remove(slot)
    }

    // Built-in overlay visibility. The rail flips these on user action; the host renders the overlays.
    // Deliberately NOT cleared by resetHost() so visibility survives the DSL re-applying each recomposition.
    var aboutVisible by mutableStateOf(false)
        private set
    var helpVisible by mutableStateOf(false)
        private set
    /** When non-null, the Help overlay is scoped to that nested rail's child items. */
    var helpOwnerId by mutableStateOf<String?>(null)
        private set
    var moreFromAzVisible by mutableStateOf(false)
        private set

    fun showAbout() { aboutVisible = true }
    fun hideAbout() { aboutVisible = false }
    fun showHelp(id: String?) { helpOwnerId = id; helpVisible = true }
    fun hideHelp() { helpVisible = false; helpOwnerId = null }
    fun showMoreFromAz() { moreFromAzVisible = true }
    fun hideMoreFromAz() { moreFromAzVisible = false }

    fun setController(controller: NavHostController) {
        _navController = controller
        railScope.navController = controller
    }

    override fun background(weight: Int, page: Float, content: @Composable () -> Unit) {
        backgrounds.add(AzBackgroundItem(weight, page, content))
    }

    override fun onscreen(alignment: Alignment, page: Float, content: @Composable () -> Unit) {
        onscreenItems.add(AzOnscreenItem(alignment, page, content))
    }

    override fun azBottomSheet(
        controller: AzSheetController,
        config: AzSheetConfig,
        onSwipeLeft: (() -> Unit)?,
        onSwipeRight: (() -> Unit)?,
        content: @Composable BoxScope.() -> Unit,
    ) {
        bottomSheets.add(AzBottomSheetItem(controller, config, onSwipeLeft, onSwipeRight, content))
    }

    override fun azPopup(
        controller: AzPopupController,
        dismissOnOutsideTap: Boolean,
        content: @Composable AzPopupScope.() -> Unit,
    ) {
        popups.add(AzPopupItem(controller, dismissOnOutsideTap, content))
    }

    fun getRailScopeImpl() = railScope

    fun resetHost() {
        railScope.reset()
        backgrounds.clear()
        onscreenItems.clear()
        bottomSheets.clear()
        popups.clear()
    }
}

/**
 * The top-level container for applications using AzNavRail — the cross-platform analogue of the
 * Android sibling's `AzHostActivityLayout`. It computes safe zones (top 20% / bottom 10%, clamped to
 * system bars, gesture-nav aware), resolves docking from the device rotation, provides the
 * CompositionLocals the rail relies on, renders backgrounds → the big screen title → onscreen content
 * → the rail → the built-in overlays → bottom sheets, and returns the [AzGuidanceController].
 *
 * Platform-specific window chrome (edge-to-edge, nav-bar transparency) is applied on Android and is a
 * no-op elsewhere (see `PlatformHostChrome`).
 *
 * @return the [AzGuidanceController] so callers can `activate`/`reset` guidance goals.
 */
@OptIn(AzStrictLayout::class)
@Composable
fun AzHostActivityLayout(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    pagesEnabled: Boolean = true,
    content: AzNavHostScope.() -> Unit,
): AzGuidanceController {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val screenHeightPx = containerSize.height.toFloat()
    val effectiveIsLandscape = isLandscape ?: (containerSize.width > containerSize.height)

    val effectiveCurrentDestination = currentDestination ?: run {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        navBackStackEntry?.destination?.route
    }

    val scope = remember { AzNavHostScopeImpl() }
    scope.resetHost()
    scope.setController(navController)
    scope.apply(content)
    // Re-apply persisted reloc-item reorders so drag-and-drop sticks across recomposition, then
    // stamp on the per-item badge/loading/alert state the DSL and any live popup declared.
    scope.getRailScopeImpl().applyRelocReorders()
    scope.getRailScopeImpl().applyItemStates()

    // Status-driven guidance controller: provided to the rail (which routes/renders it) and returned.
    val guidanceController = rememberAzGuidanceController()

    val railScope = scope.getRailScopeImpl()
    val dockingSide = railScope.dockingSide
    val railWidth = railScope.collapsedWidth
    val usePhysicalDocking = railScope.usePhysicalDocking
    val rotation = rememberDeviceRotationDegrees()

    val layoutConfig = AzRailLayoutHelper.calculateLayout(
        dockingSide = dockingSide,
        rotation = rotation,
        usePhysicalDocking = usePhysicalDocking,
    )
    val visualSide = layoutConfig.visualSide
    val orientation = layoutConfig.orientation
    val reverseLayout = layoutConfig.reverseLayout
    val railAlignment = layoutConfig.alignment
    val visualDockingSideProxy =
        if (visualSide == AzVisualSide.BOTTOM || visualSide == AzVisualSide.RIGHT) AzDockingSide.RIGHT else AzDockingSide.LEFT

    val systemBars = WindowInsets.systemBars.asPaddingValues()

    val calculatedTop = with(density) { (screenHeightPx * AzLayoutConfig.ContentSafeTopPercent).toDp() }
    val calculatedBottom = with(density) { (screenHeightPx * AzLayoutConfig.ContentSafeBottomPercent).toDp() }

    val gestureNav = rememberIsGestureNav()
    val safeTop = max(calculatedTop, systemBars.calculateTopPadding())
    val safeBottom = azResolveSafeBottom(gestureNav, calculatedBottom, systemBars.calculateBottomPadding())

    // Android window chrome: edge-to-edge + optional see-through nav bar for drawBehindNavBar sheets.
    val wantDrawBehindNavBar = scope.bottomSheets.any { it.config.drawBehindNavBar }
    PlatformHostChrome(wantDrawBehindNavBar)

    // App metadata (icon/name/packageId) resolved from the platform, merged over any consumer override.
    val appMeta = rememberEffectiveAppMeta()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        azOrderBackgrounds(scope.backgrounds, pagesEnabled).forEach { item ->
            Box(modifier = Modifier.fillMaxSize()) { item.content() }
        }

        val railScopeImpl = scope.getRailScopeImpl()
        val isStationaryFolded = railScopeImpl.noMenu && railScopeImpl.isFoldedUp

        val startPadding = if (visualSide == AzVisualSide.LEFT && !isStationaryFolded) railWidth else 0.dp
        val endPadding = if (visualSide == AzVisualSide.RIGHT && !isStationaryFolded) railWidth else 0.dp
        val topPadding = if (visualSide == AzVisualSide.TOP && !isStationaryFolded) railWidth else 0.dp
        val bottomPadding = if (visualSide == AzVisualSide.BOTTOM && !isStationaryFolded) railWidth else 0.dp

        val currentActiveItem = railScopeImpl.navItems.find { item ->
            (item.route != null && item.route == effectiveCurrentDestination) ||
                item.classifiers.any { railScopeImpl.activeClassifiers.contains(it) }
        }

        val currentTitle = currentActiveItem?.let { item ->
            val explicitTitle =
                if (item.screenTitle != null && item.screenTitle != AzNavRailDefaults.NO_TITLE) item.screenTitle else null
            explicitTitle ?: when {
                item.isToggle -> if (item.isChecked == true) (item.menuToggleOnText ?: item.toggleOnText)
                    else (item.menuToggleOffText ?: item.toggleOffText)

                item.isCycler -> {
                    val currentOption = railScopeImpl.transientCyclerOptions[item.id] ?: item.selectedOption
                    val index = item.options?.indexOf(currentOption) ?: -1
                    if (index != -1 && item.menuOptions != null && index in item.menuOptions.indices) {
                        item.menuOptions[index]
                    } else {
                        currentOption ?: item.text
                    }
                }

                else -> item.text
            }
        }

        val titleTop = with(density) { (screenHeightPx * 0.1f).toDp() }
        val titleHeight = with(density) { (screenHeightPx * 0.1f).toDp() }
        val titleAlignment = if (visualDockingSideProxy == AzDockingSide.LEFT) Alignment.CenterEnd else Alignment.CenterStart
        val titlePaddingSide =
            if (visualDockingSideProxy == AzDockingSide.LEFT) Modifier.padding(end = 32.dp) else Modifier.padding(start = 32.dp)

        val hasTitle = currentTitle != null && currentTitle != AzNavRailDefaults.NO_TITLE && currentTitle.isNotBlank()
        // The title row is also where hosted drop-down triggers live, so it is drawn whenever there
        // is a title OR at least one trigger to place.
        if (hasTitle || scope.titleTriggers.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = titleTop)
                    .height(titleHeight)
                    .then(titlePaddingSide),
                contentAlignment = titleAlignment,
            ) {
                // Title and triggers read like an app bar mirrored onto the rail's docking side: the
                // triggers sit hard against the edge the title hugs, with the title just inboard of
                // them. Several triggers line up beside each other in registration order.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (visualDockingSideProxy != AzDockingSide.LEFT) {
                        scope.titleTriggers.forEach { slot -> AzDropdownTriggerButton(slot) }
                    }
                    if (hasTitle) {
                        // Keying on the title restarts the kinetic entrance every time the active
                        // screen changes.
                        key(currentTitle) {
                            val titleKinetic = rememberAzKineticModifier(
                                index = 0,
                                count = 1,
                                visible = true,
                                entrance = railScopeImpl.titleEntrance,
                                exit = AzExit.None,
                                staggerMs = 0,
                                durationMs = 420,
                                easing = AzEasing.Wp7Decelerate,
                                startAngle = 70f,
                                tiltOnPress = false,
                                maxTiltDegrees = 0f,
                                dockingSide = visualDockingSideProxy,
                            )
                            Text(
                                text = currentTitle!!,
                                modifier = titleKinetic,
                                style = MaterialTheme.typography.displayMedium
                                    .copy(fontWeight = FontWeight.Bold)
                                    .merge(railScopeImpl.titleTextStyle),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                    if (visualDockingSideProxy == AzDockingSide.LEFT) {
                        scope.titleTriggers.forEach { slot -> AzDropdownTriggerButton(slot) }
                    }
                }
            }
        }

        CompositionLocalProvider(LocalAzNavHostScope provides scope) {
            AzHostFragmentLayout(
                safeTop = safeTop,
                safeBottom = safeBottom,
                startPadding = startPadding,
                endPadding = endPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                items = azOrderOnscreen(scope.onscreenItems, pagesEnabled),
                dockingSide = dockingSide,
            )
        }

        CompositionLocalProvider(
            LocalAzNavHostPresent provides true,
            LocalAzNavHostScope provides scope,
            LocalAzAppMeta provides appMeta,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom),
            LocalAzGuidanceController provides guidanceController,
        ) {
            AzNavRail(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                currentDestination = effectiveCurrentDestination,
                isLandscape = effectiveIsLandscape,
                initiallyExpanded = initiallyExpanded,
                disableSwipeToOpen = disableSwipeToOpen,
                onExpandedChange = onExpandedChange,
                providedScope = railScope,
                orientation = orientation,
                visualDockingSide = visualDockingSideProxy,
                railAlignment = railAlignment,
                reverseLayout = reverseLayout,
                content = {},
            )
        }

        // Unattached host items — rail hosts that live outside the rail strip. Drawn after the rail
        // so a floating stack can be dragged over it, and before the built-in overlays so About /
        // Help / More-from-Az still cover everything.
        CompositionLocalProvider(
            LocalAzNavHostScope provides scope,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom),
        ) {
            AzUnattachedRail(
                scope = railScope,
                navController = navController,
                currentDestination = effectiveCurrentDestination,
                visualDockingSide = visualDockingSideProxy,
            )
        }

        // Built-in overlays, host-managed. About + More-from-Az flow through the same safe-zone /
        // rail-padding treatment as onscreen content; Help spans the full window (it draws connector
        // lines from rail items outside the onscreen safe area).
        val effectiveRepoUrl = remember(railScope.appRepositoryUrl, appMeta.packageId) {
            railScope.appRepositoryUrl.ifBlank {
                appMeta.packageId?.let { GithubDocsRepository.repoUrlFromPackage(it) } ?: railScope.appRepositoryUrl
            }
        }

        CompositionLocalProvider(
            LocalAzNavHostScope provides scope,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom),
        ) {
            if (scope.aboutVisible || scope.moreFromAzVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, bottom = bottomPadding, start = startPadding, end = endPadding),
                ) {
                    if (scope.aboutVisible && !scope.moreFromAzVisible) {
                        AboutOverlay(
                            repoUrl = effectiveRepoUrl,
                            scope = railScope,
                            onOpenMoreFromAz = if (railScope.advancedConfig.moreFromAzEnabled) {
                                { scope.showMoreFromAz() }
                            } else null,
                            onDismiss = { scope.hideAbout() },
                        )
                    }
                    if (scope.moreFromAzVisible) {
                        MoreFromAzOverlay(
                            jsonUrl = railScope.advancedConfig.moreFromAzJsonUrl,
                            scope = railScope,
                            onDismiss = { scope.hideMoreFromAz() },
                        )
                    }
                }
            }

            if (scope.helpVisible && !scope.aboutVisible && !scope.moreFromAzVisible) {
                HelpOverlay(
                    items = railScope.navItems,
                    helpLineColors = railScope.helpLineColors,
                    onDismiss = { scope.hideHelp(); railScope.advancedConfig.onDismissHelp?.invoke() },
                    itemBoundsCache = railScope.itemBoundsCache,
                    helpList = railScope.advancedConfig.helpList,
                    nestedRailOpenId = scope.helpOwnerId,
                )
            }
        }

        // Bottom sheets draw above everything and span the full width, edge-to-edge to the bottom.
        scope.bottomSheets.forEach { item ->
            AzBottomSheet(
                controller = item.controller,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                config = item.config,
                onSwipeLeft = item.onSwipeLeft,
                onSwipeRight = item.onSwipeRight,
                content = item.content,
            )
        }

        // Global loading state (`azAdvanced(isLoading = …)` / `azSettings(isLoading = …)`). Documented
        // since forever as "a full-screen AzLoad spinner"; it had never been read by anything. It
        // draws above the rail and swallows input, because a screen that is loading is not a screen
        // you can act on.
        if (railScope.advancedConfig.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(4f)
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center,
            ) {
                AzLoad()
            }
        }

        // Popups sit above the sheets: they are the layer that interrupts, and a warning raised
        // while a sheet is open still has to be the thing the user answers.
        scope.popups.forEach { popup ->
            AzPopupHost(
                popup = popup,
                railScope = railScope,
                modifier = Modifier.zIndex(3f),
            )
        }
    }

    return guidanceController
}

/**
 * Renders the main content area, applying safe-zone + rail-offset padding and placing each
 * [AzOnscreenItem] at its requested alignment (mirrored automatically when [dockingSide] is RIGHT).
 */
@Composable
fun AzHostFragmentLayout(
    safeTop: Dp,
    safeBottom: Dp,
    startPadding: Dp,
    endPadding: Dp,
    topPadding: Dp,
    bottomPadding: Dp,
    items: List<AzOnscreenItem>,
    dockingSide: AzDockingSide,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = safeTop + topPadding, bottom = safeBottom + bottomPadding, start = startPadding, end = endPadding),
    ) {
        items.forEach { item ->
            val finalAlignment = if (dockingSide == AzDockingSide.RIGHT && item.alignment is BiasAlignment) {
                BiasAlignment(horizontalBias = -item.alignment.horizontalBias, verticalBias = item.alignment.verticalBias)
            } else {
                item.alignment
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = finalAlignment) {
                item.content()
            }
        }
    }
}

/**
 * A convenience wrapper around the multiplatform navigation `NavHost` that integrates with
 * [AzHostActivityLayout]: it uses the host's [NavHostController] and configures default directional
 * transitions from the rail's docking side.
 */
@Composable
fun AzNavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = LocalAzNavHostScope.current?.navController ?: rememberNavController(),
    contentAlignment: Alignment = Alignment.Center,
    route: String? = null,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    builder: NavGraphBuilder.() -> Unit,
) {
    val scope = LocalAzNavHostScope.current
    val dockingSide = scope?.dockingSide ?: AzDockingSide.LEFT

    val defaultEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (dockingSide == AzDockingSide.LEFT) slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        else slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
    }
    val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (dockingSide == AzDockingSide.LEFT) slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        else slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    }
    val defaultPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (dockingSide == AzDockingSide.LEFT) slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
        else slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    }
    val defaultPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (dockingSide == AzDockingSide.LEFT) slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        else slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        contentAlignment = contentAlignment,
        route = route,
        enterTransition = enterTransition ?: defaultEnter,
        exitTransition = exitTransition ?: defaultExit,
        popEnterTransition = popEnterTransition ?: defaultPopEnter,
        popExitTransition = popExitTransition ?: defaultPopExit,
        builder = builder,
    )
}
