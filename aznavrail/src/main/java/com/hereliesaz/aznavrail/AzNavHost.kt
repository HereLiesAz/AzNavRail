// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavHost.kt
package com.hereliesaz.aznavrail

import android.app.Activity
import android.content.ContextWrapper
import android.view.Surface
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.bottomsheet.AzBottomSheet
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.internal.AboutOverlay
import com.hereliesaz.aznavrail.internal.AzBottomSheetItem
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzNavMode
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzRailLayoutHelper
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.MoreFromAzOverlay
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.azResolveSafeBottom
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.tutorial.LocalAzTutorialController
import com.hereliesaz.aznavrail.tutorial.rememberAzTutorialController
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzSheetConfig
import androidx.compose.foundation.layout.BoxScope

/** CompositionLocal that signals whether [AzNavRail] is correctly nested inside [AzHostActivityLayout]. */
val LocalAzNavHostPresent = compositionLocalOf { false }

/** CompositionLocal providing the computed safe-zone insets for the current layout. */
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }

/** CompositionLocal giving composables access to the active [AzNavHostScope] from [AzHostActivityLayout]. */
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScope?> { null }

/**
 * Extended scope for configuring both the AzNavRail and the hosted content.
 *
 * This scope inherits from [AzNavRailScope], allowing you to define rail items and settings,
 * while also providing methods to define onscreen content and background layers.
 */
interface AzNavHostScope : AzNavRailScope {
    /** The active [NavHostController]. */
    val navController: NavHostController
    /** The current docking side of the rail. */
    val dockingSide: AzDockingSide

    /**
     * Adds a background layer behind the main content.
     *
     * Backgrounds form their own "book" of pages that sits entirely beneath the onscreen book
     * (and therefore beneath the rail and nav bar). See [onscreen] for how [page] orders layers.
     *
     * @param weight Tie-breaker Z-order within a single background [page]. Lower weights are drawn
     *   first (further back).
     * @param page The page this background belongs to. Higher page numbers are drawn further back;
     *   decimals (e.g. `1.5f`) insert a page between existing ones without renumbering. Honoured
     *   only when the host's `pagesEnabled` is `true` (the default).
     * @param content The composable content for the background.
     */
    fun background(weight: Int = 0, page: Float = 0f, content: @Composable () -> Unit)

    /**
     * Adds content to the main screen area, respecting safe zones and rail padding.
     *
     * **Pages.** Items sharing a [page] render on one co-planar layer and are positioned with
     * standard Compose [alignment] (give same-page items distinct alignments — or compose your own
     * `Row`/`Column` inside [content] — so they tile without overlapping). Items on *different*
     * pages are stacked in Z and may overlap: a **higher** page number is drawn **further back**.
     * Decimal page numbers (e.g. `1.5f`) insert a page between existing ones without renumbering.
     * Paging is honoured only when the host's `pagesEnabled` is `true` (the default); when on it is
     * forced, so unlabelled items all share the default page `0f`.
     *
     * @param alignment The alignment of the content within the safe area. Note: Alignment is
     *                  automatically mirrored if the rail is docked on the right.
     * @param page The page this content belongs to (Z-layer); see above. Defaults to `0f`.
     * @param content The composable content.
     */
    fun onscreen(alignment: Alignment = Alignment.TopStart, page: Float = 0f, content: @Composable () -> Unit)

    /**
     * Registers a bottom sheet that draws above the rail, the menu, and the onscreen area, and
     * extends all the way to the bottom of the screen so the HIDDEN-detent strip is reachable
     * from the system-navigation-bar edge — i.e., a swipe-up from the gesture/nav-bar area
     * lands on the strip and reveals the sheet. If your sheet body needs to clear the system nav
     * bar visually, pad inside [content] or use [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetInsetAware]
     * directly outside the DSL.
     *
     * The sheet is rendered as the top z-layer in [AzHostActivityLayout] and is *not* a background
     * (see [background]). Multiple calls register multiple sheets, stacked in the order they were
     * declared.
     *
     * Use [com.hereliesaz.aznavrail.bottomsheet.rememberAzSheetController] to create [controller]
     * outside the DSL block so its detent state survives recomposition.
     *
     * @param controller State holder driving the sheet's [com.hereliesaz.aznavrail.model.AzSheetDetent].
     * @param config Static configuration (heights, colors, drag threshold, animation).
     * @param onSwipeLeft Optional horizontal-swipe-left callback (gated by [AzSheetConfig.horizontalSwipeEnabled]).
     * @param onSwipeRight Optional horizontal-swipe-right callback (gated by [AzSheetConfig.horizontalSwipeEnabled]).
     * @param content Caller-provided sheet content.
     */
    fun azBottomSheet(
        controller: AzSheetController,
        config: AzSheetConfig = AzSheetConfig(),
        onSwipeLeft: (() -> Unit)? = null,
        onSwipeRight: (() -> Unit)? = null,
        content: @Composable BoxScope.() -> Unit,
    )
}

/** Holds a sorted-background layer registered via [AzNavHostScope.background]. */
data class AzBackgroundItem(val weight: Int, val page: Float = 0f, val content: @Composable () -> Unit)

/** Holds an onscreen content item registered via [AzNavHostScope.onscreen]. */
data class AzOnscreenItem(val alignment: Alignment, val page: Float = 0f, val content: @Composable () -> Unit)

/**
 * Orders the background "book" back-to-front for rendering (the first element is drawn first, i.e.
 * is the backmost). When [pagesEnabled], a **higher** page draws further back; `weight` breaks ties
 * within a page (lower weight further back). When disabled, falls back to the legacy weight sort.
 */
internal fun azOrderBackgrounds(items: List<AzBackgroundItem>, pagesEnabled: Boolean): List<AzBackgroundItem> =
    if (pagesEnabled) items.sortedWith(compareByDescending<AzBackgroundItem> { it.page }.thenBy { it.weight })
    else items.sortedBy { it.weight }

/**
 * Orders the onscreen "book" back-to-front for rendering (the first element is drawn first, i.e. is
 * the backmost). When [pagesEnabled], a **higher** page draws further back; items sharing a page keep
 * their declaration order (stable sort). When disabled, declaration order is preserved unchanged.
 */
internal fun azOrderOnscreen(items: List<AzOnscreenItem>, pagesEnabled: Boolean): List<AzOnscreenItem> =
    if (pagesEnabled) items.sortedByDescending { it.page } else items

/**
 * Visually hides and input-disables a guide overlay (Help cards / interactive tutorial) while a
 * footer screen (About / More-from-Az) is open, WITHOUT removing it from composition. Keeping the
 * overlay mounted preserves its local progress state (tutorial scene/card/checklist, help
 * expanded-card/scroll), so it reappears exactly where the user left it once the footer screen is
 * dismissed. When [suppressed] it draws at `alpha = 0` and swallows all pointer input (so a stray
 * tap on the still-mounted full-screen background can't dismiss it).
 */
internal fun Modifier.azSuppressGuide(suppressed: Boolean): Modifier =
    if (!suppressed) this
    else this
        .graphicsLayer { alpha = 0f }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }

/** Concrete implementation of [AzNavHostScope] used internally by [AzHostActivityLayout]. */
class AzNavHostScopeImpl(
    private val railScope: AzNavRailScopeImpl = AzNavRailScopeImpl()
) : AzNavHostScope, AzNavRailScope by railScope {
    private var _navController: NavHostController? = null
    override val navController: NavHostController get() = _navController ?: error("AzNavHostScope.navController was read before the host's NavController was attached. This getter is only safe inside composables that run AFTER `AzHostActivityLayout` has built its `NavHost` (eg. inside `onscreen { ... }`, `background { ... }`, or button `onClick` lambdas). Fix: do not access `navController` from inside the `azRailContent { ... }` declaration block itself, nor from `init`/eager code -- defer the call into a composable body or an `onClick`; or, if you are writing a custom host, call `setController(yourNavController)` on this scope before reading `navController`.")
    override val dockingSide: AzDockingSide get() = railScope.dockingSide

    val backgrounds = mutableStateListOf<AzBackgroundItem>()
    val onscreenItems = mutableStateListOf<AzOnscreenItem>()
    internal val bottomSheets = mutableStateListOf<AzBottomSheetItem>()

    // --- Built-in overlay visibility ---
    // The rail (which always runs inside this host) flips these on user action; the host renders the
    // About reader and "More from Az" carousel through the onscreen() layout path, and the Help
    // overlay full-screen (it draws connector lines to rail items that live outside the onscreen safe
    // area). State lives here, not on the rail, so the host can render the overlays like any other
    // onscreen content. It is deliberately NOT cleared by resetHost(), so visibility survives the DSL
    // being re-applied on every recomposition.
    var aboutVisible by mutableStateOf(false)
        private set
    var helpVisible by mutableStateOf(false)
        private set
    /** When non-null, the Help overlay is scoped to that nested rail's child items. */
    var helpScopeId by mutableStateOf<String?>(null)
        private set
    var moreFromAzVisible by mutableStateOf(false)
        private set

    fun showAbout() { aboutVisible = true }
    fun hideAbout() { aboutVisible = false }
    fun showHelp(scopeId: String?) { helpScopeId = scopeId; helpVisible = true }
    fun hideHelp() { helpVisible = false; helpScopeId = null }
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

    fun getRailScopeImpl() = railScope

    fun resetHost() {
        railScope.reset()
        backgrounds.clear()
        onscreenItems.clear()
        bottomSheets.clear()
    }
}

/**
 * The mandatory top-level container for applications using AzNavRail.
 *
 * `AzHostActivityLayout` acts as the root of your screen hierarchy. It manages the layout of the
 * navigation rail and the main content, enforcing strict safe zones (top 10%, bottom 10%) and
 * applying correct padding based on the rail's position and device orientation.
 *
 * It provides an [AzNavHostScope] to its content lambda, allowing you to configure the rail
 * and add onscreen content simultaneously.
 *
 * @param modifier The modifier for the layout.
 * @param navController The [NavHostController] for navigation.
 * @param currentDestination The current route. Auto-detected if null.
 * @param isLandscape Explicitly set landscape mode. Auto-detected if null.
 * @param initiallyExpanded Whether the rail is initially expanded.
 * @param disableSwipeToOpen Disable swipe-to-open gesture for the rail menu.
 * @param onExpandedChange Called whenever the rail transitions between collapsed and expanded states.
 *   Receives `true` when the rail expands and `false` when it collapses.
 * @param pagesEnabled Whether the pages Z-ordering system is active (default `true`). When on, the
 *   `page` of every [AzNavHostScope.onscreen] / [AzNavHostScope.background] item is honoured and
 *   forced — items with no explicit page share the default page `0f`. When off, items render in
 *   declaration order (backgrounds by `weight`) and `page` is ignored.
 * @param content The configuration block for rail items and onscreen content.
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
    content: AzNavHostScope.() -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext: android.content.Context = context
        var found: Activity? = null
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                found = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        found
    }
    LaunchedEffect(activity) {
        activity?.let {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
            // Deprecated direct property access replaced with WindowCompat or ignored if handled by themes/edge-to-edge
            // it.window.statusBarColor = android.graphics.Color.TRANSPARENT
            // it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val effectiveIsLandscape = isLandscape ?: (configuration.screenWidthDp > configuration.screenHeightDp)

    val effectiveCurrentDestination = currentDestination ?: run {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        navBackStackEntry?.destination?.route
    }

    val scope = remember { AzNavHostScopeImpl() }
    scope.resetHost()
    scope.setController(navController)
    scope.apply(content)
    // Re-apply persisted reloc-item reorders so drag-and-drop sticks across recomposition.
    scope.getRailScopeImpl().applyRelocReorders()

    val railScope = scope.getRailScopeImpl()
    val dockingSide = railScope.dockingSide
    val railWidth = railScope.collapsedWidth
    val usePhysicalDocking = railScope.usePhysicalDocking
    val rotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0

    val layoutConfig = AzRailLayoutHelper.calculateLayout(
        dockingSide = dockingSide,
        rotation = rotation,
        usePhysicalDocking = usePhysicalDocking
    )

    val visualSide = layoutConfig.visualSide
    val orientation = layoutConfig.orientation
    val reverseLayout = layoutConfig.reverseLayout
    val railAlignment = layoutConfig.alignment

    val visualDockingSideProxy = if (visualSide == AzVisualSide.BOTTOM || visualSide == AzVisualSide.RIGHT) AzDockingSide.RIGHT else AzDockingSide.LEFT

    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val screenHeight = configuration.screenHeightDp

    val calculatedTop = with(density) { (screenHeight * AzLayoutConfig.ContentSafeTopPercent).toDp() }
    val calculatedBottom = with(density) { (screenHeight * AzLayoutConfig.ContentSafeBottomPercent).toDp() }

    // In gesture navigation the library imposes no bottom margin — on-screen content runs fully
    // edge-to-edge at the bottom (there is no button bar to clear). In button navigation we keep
    // the larger of the 10% content safe-zone and the system navigation-bar inset.
    val gestureNav = AzNavMode.isGestureNav(context)
    val safeTop = max(calculatedTop, systemBars.calculateTopPadding())
    val safeBottom = azResolveSafeBottom(gestureNav, calculatedBottom, systemBars.calculateBottomPadding())

    // Feature 1 (in-tree): when any registered sheet opts into `drawBehindNavBar` and the device
    // uses button navigation, force the Activity's navigation bar see-through so the sheet — which
    // already draws to the bottom edge — is visible behind it. The exposed height above the bar is
    // unchanged. No-op in gesture navigation. Prior window values are restored on dispose.
    val wantDrawBehindNavBar = scope.bottomSheets.any { it.config.drawBehindNavBar }
    DisposableEffect(activity, wantDrawBehindNavBar, gestureNav) {
        val window = activity?.window
        if (window != null && wantDrawBehindNavBar && !gestureNav) {
            @Suppress("DEPRECATION")
            val previousColor = window.navigationBarColor
            val previousContrast = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced
            } else {
                null
            }
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            onDispose {
                @Suppress("DEPRECATION")
                window.navigationBarColor = previousColor
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && previousContrast != null) {
                    window.isNavigationBarContrastEnforced = previousContrast
                }
            }
        } else {
            onDispose { }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        azOrderBackgrounds(scope.backgrounds, pagesEnabled).forEach { item ->
            Box(modifier = Modifier.fillMaxSize()) { item.content() }
        }

        val startPadding = if (visualSide == AzVisualSide.LEFT) railWidth else 0.dp
        val endPadding = if (visualSide == AzVisualSide.RIGHT) railWidth else 0.dp
        val topPadding = if (visualSide == AzVisualSide.TOP) railWidth else 0.dp
        val bottomPadding = if (visualSide == AzVisualSide.BOTTOM) railWidth else 0.dp

        // Identify active item and pull actual transient states
        val railScopeImpl = scope.getRailScopeImpl()
        val currentActiveItem = railScopeImpl.navItems.find { item ->
            (item.route != null && item.route == effectiveCurrentDestination) ||
                    item.classifiers.any { railScopeImpl.activeClassifiers.contains(it) }
        }

        val currentTitle = currentActiveItem?.let { item ->
            when {
                item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
                item.isCycler -> railScopeImpl.transientCyclerOptions[item.id] ?: item.selectedOption ?: item.text
                else -> if (item.screenTitle != null && item.screenTitle != AzNavRailDefaults.NO_TITLE) item.screenTitle else item.text
            }
        }

        // Setup BIG 10%-20% boundary title on opposite side
        val titleTop = with(density) { (screenHeight * 0.1f).toDp() }
        val titleHeight = with(density) { (screenHeight * 0.1f).toDp() }
        val titleAlignment = if (visualDockingSideProxy == AzDockingSide.LEFT) Alignment.CenterEnd else Alignment.CenterStart
        val titlePaddingSide = if (visualDockingSideProxy == AzDockingSide.LEFT) Modifier.padding(end = 32.dp) else Modifier.padding(start = 32.dp)

        if (currentTitle != null && currentTitle != AzNavRailDefaults.NO_TITLE && currentTitle.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = titleTop)
                    .height(titleHeight)
                    .then(titlePaddingSide),
                contentAlignment = titleAlignment
            ) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.displayMedium, // Much bigger requirement
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        val tutorialController = rememberAzTutorialController()

        CompositionLocalProvider(
            LocalAzNavHostScope provides scope,
            LocalAzTutorialController provides tutorialController
        ) {
            AzHostFragmentLayout(
                safeTop = safeTop,
                safeBottom = safeBottom,
                startPadding = startPadding,
                endPadding = endPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                items = azOrderOnscreen(scope.onscreenItems, pagesEnabled),
                dockingSide = dockingSide
            )
        }

        CompositionLocalProvider(
            LocalAzNavHostPresent provides true,
            LocalAzNavHostScope provides scope,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom),
            LocalAzTutorialController provides tutorialController
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
                content = {}
            )
        }

        // Built-in overlays, host-managed and rendered like the rest of the screen. The rail flips
        // the visibility flags on this host scope; the host renders the overlays here so they are no
        // longer bespoke full-screen layers bolted onto the rail.
        //
        // About + More-from-Az are full content surfaces, so they flow through the SAME safe-zone /
        // rail-padding / docking treatment as onscreen() content — the rail stays docked and visible
        // beside them. Help stays full-screen: it draws connector lines from rail items (which sit in
        // the rail strip, outside the onscreen safe area) to their cards, so it must span the window.
        val effectiveRepoUrl = remember(railScope.appRepositoryUrl, context.packageName) {
            railScope.appRepositoryUrl.ifBlank {
                GithubDocsRepository.repoUrlFromPackage(context.packageName) ?: railScope.appRepositoryUrl
            }
        }

        CompositionLocalProvider(
            LocalAzNavHostScope provides scope,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom),
            LocalAzTutorialController provides tutorialController,
        ) {
            if (scope.aboutVisible || scope.moreFromAzVisible) {
                // Pad only the rail offset; the overlays apply top/bottom safe-zone insets themselves
                // from the LocalAzSafeZones provided above (so the same composables work standalone
                // under the dropdown, which has no host wrapper).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, bottom = bottomPadding, start = startPadding, end = endPadding)
                ) {
                    if (scope.aboutVisible) {
                        AboutOverlay(
                            repoUrl = effectiveRepoUrl,
                            scope = railScope,
                            onOpenMoreFromAz = if (railScope.advancedConfig.moreFromAzEnabled) {
                                { scope.showMoreFromAz() }
                            } else null,
                            onDismiss = { scope.hideAbout() },
                        )
                    }
                    // More-from-Az draws above About (later in the Box), so opening it from the About
                    // screen covers it and dismissing returns to About underneath.
                    if (scope.moreFromAzVisible) {
                        MoreFromAzOverlay(
                            jsonUrl = railScope.advancedConfig.moreFromAzJsonUrl,
                            scope = railScope,
                            onDismiss = { scope.hideMoreFromAz() },
                        )
                    }
                }
            }

            // While a footer screen (About / More-from-Az) is open, the Help overlay is hidden but
            // kept mounted so its expanded-card/scroll state restores untouched on close.
            if (scope.helpVisible) {
                Box(Modifier.azSuppressGuide(scope.aboutVisible || scope.moreFromAzVisible)) {
                    HelpOverlay(
                        items = railScope.navItems,
                        helpLineColors = railScope.helpLineColors,
                        onDismiss = { scope.hideHelp(); railScope.advancedConfig.onDismissHelp?.invoke() },
                        itemBoundsCache = railScope.itemBoundsCache,
                        helpList = railScope.advancedConfig.helpList,
                        nestedRailOpenId = scope.helpScopeId,
                        tutorials = railScope.advancedConfig.tutorials,
                        onTutorialLaunch = { id -> tutorialController.startTutorial(id); scope.hideHelp() },
                    )
                }
            }
        }

        // Bottom sheets registered via azBottomSheet { ... } draw above EVERYTHING — including
        // the rail, the menu, and the onscreen content — and span the full screen width.
        //
        // - No `windowInsetsPadding(WindowInsets.navigationBars)` so the HIDDEN strip reaches the
        //   bottom edge and a swipe-up from the system-nav-bar area reveals the sheet.
        // - No rail-offset padding (those `startPadding` / `endPadding` values above feed
        //   `AzHostFragmentLayout` only). The sheet card uses `fillMaxWidth()` against this
        //   fullscreen modifier, so it spans edge-to-edge and is not constrained like an onscreen
        //   composable.
        // - Explicit `zIndex` guarantees the sheet visually stacks above the rail's Surface even
        //   if Compose's tonal-elevation shading would otherwise win.
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
    }
}

/**
 * Renders the main content area of the screen, applying safe-zone and rail-offset padding
 * and placing each [AzOnscreenItem] at its requested alignment.
 *
 * Horizontal alignment is mirrored automatically when [dockingSide] is [AzDockingSide.RIGHT].
 *
 * @param safeTop Top inset computed from the 20 % / system-bar rule.
 * @param safeBottom Bottom inset computed from the 10 % / system-bar rule.
 * @param startPadding Padding added at the start edge (left) when the rail is docked there.
 * @param endPadding Padding added at the end edge (right) when the rail is docked there.
 * @param topPadding Padding added at the top when the rail is docked horizontally.
 * @param bottomPadding Padding added at the bottom when the rail is docked horizontally.
 * @param items The onscreen content items to render.
 * @param dockingSide Used to mirror [BiasAlignment] for right-docked layouts.
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
    dockingSide: AzDockingSide
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = safeTop + topPadding, bottom = safeBottom + bottomPadding, start = startPadding, end = endPadding)
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
 * A convenience wrapper around [androidx.navigation.compose.NavHost] that integrates seamlessly with [AzHostActivityLayout].
 *
 * It automatically uses the [NavHostController] from the parent [AzHostActivityLayout] and configures
 * default directional transitions based on the rail's docking side.
 *
 * @param startDestination The route for the start destination.
 * @param modifier The modifier for the NavHost.
 * @param navController The controller (defaults to the one provided by [AzHostActivityLayout]).
 * @param contentAlignment The alignment of the content.
 * @param route The route for the graph.
 * @param enterTransition Callback to define enter transition.
 * @param exitTransition Callback to define exit transition.
 * @param popEnterTransition Callback to define pop enter transition.
 * @param popExitTransition Callback to define pop exit transition.
 * @param builder The builder closure to define the navigation graph.
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
    builder: NavGraphBuilder.() -> Unit
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

    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        contentAlignment = contentAlignment,
        route = route,
        enterTransition = enterTransition ?: defaultEnter,
        exitTransition = exitTransition ?: defaultExit,
        popEnterTransition = popEnterTransition ?: defaultPopEnter,
        popExitTransition = popExitTransition ?: defaultPopExit,
        builder = builder
    )
}