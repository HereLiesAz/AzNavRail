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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzRailLayoutHelper
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.model.AzDockingSide

val LocalAzNavHostPresent = compositionLocalOf { false }
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }
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
     * @param weight The Z-order weight. Lower weights are drawn first (further back).
     * @param content The composable content for the background.
     */
    fun background(weight: Int = 0, content: @Composable () -> Unit)

    /**
     * Adds content to the main screen area, respecting safe zones and rail padding.
     *
     * @param alignment The alignment of the content within the safe area. Note: Alignment is
     *                  automatically mirrored if the rail is docked on the right.
     * @param content The composable content.
     */
    fun onscreen(alignment: Alignment = Alignment.TopStart, content: @Composable () -> Unit)
}

data class AzBackgroundItem(val weight: Int, val content: @Composable () -> Unit)
data class AzOnscreenItem(val alignment: Alignment, val content: @Composable () -> Unit)

class AzNavHostScopeImpl(
    private val railScope: AzNavRailScopeImpl = AzNavRailScopeImpl()
) : AzNavHostScope, AzNavRailScope by railScope {
    private var _navController: NavHostController? = null
    override val navController: NavHostController get() = _navController ?: error("NavController not initialized.")
    override val dockingSide: AzDockingSide get() = railScope.dockingSide

    val backgrounds = mutableStateListOf<AzBackgroundItem>()
    val onscreenItems = mutableStateListOf<AzOnscreenItem>()

    fun setController(controller: NavHostController) {
        _navController = controller
        railScope.navController = controller
    }

    override fun background(weight: Int, content: @Composable () -> Unit) {
        backgrounds.add(AzBackgroundItem(weight, content))
    }

    override fun onscreen(alignment: Alignment, content: @Composable () -> Unit) {
        onscreenItems.add(AzOnscreenItem(alignment, content))
    }

    fun getRailScopeImpl() = railScope

    fun resetHost() {
        railScope.reset()
        backgrounds.clear()
        onscreenItems.clear()
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
    content: AzNavHostScope.() -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        var currentContext = context
        var activity: Activity? = null
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
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

    val safeTop = max(calculatedTop, systemBars.calculateTopPadding())
    val safeBottom = max(calculatedBottom, systemBars.calculateBottomPadding())

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        scope.backgrounds.sortedBy { it.weight }.forEach { item ->
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
                item.isCycler -> item.selectedOption ?: item.text
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

        CompositionLocalProvider(LocalAzNavHostScope provides scope) {
            AzHostFragmentLayout(
                safeTop = safeTop,
                safeBottom = safeBottom,
                startPadding = startPadding,
                endPadding = endPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                items = scope.onscreenItems,
                dockingSide = dockingSide
            )
        }

        CompositionLocalProvider(
            LocalAzNavHostPresent provides true,
            LocalAzSafeZones provides AzSafeZones(safeTop, safeBottom)
        ) {
            AzNavRail(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                currentDestination = effectiveCurrentDestination,
                isLandscape = effectiveIsLandscape,
                initiallyExpanded = initiallyExpanded,
                disableSwipeToOpen = disableSwipeToOpen,
                providedScope = railScope,
                orientation = orientation,
                visualDockingSide = visualDockingSideProxy,
                railAlignment = railAlignment,
                reverseLayout = reverseLayout,
                content = {}
            )
        }
    }
}

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
                BiasAlignment(horizontalBias = -(item.alignment as BiasAlignment).horizontalBias, verticalBias = (item.alignment as BiasAlignment).verticalBias)
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