package com.hereliesaz.aznavrail

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.model.AzDockingSide

// --- Composition Locals ---
val LocalAzNavHostPresent = compositionLocalOf { false }
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScope?> { null }

// --- Scopes & Models ---
interface AzNavHostScope : AzNavRailScope {
    val navController: NavHostController
    val dockingSide: AzDockingSide
    fun background(weight: Int = 0, content: @Composable () -> Unit)
    fun onscreen(alignment: Alignment = Alignment.TopStart, content: @Composable () -> Unit)
}

data class AzBackgroundItem(val weight: Int, val content: @Composable () -> Unit)
data class AzOnscreenItem(val alignment: Alignment, val content: @Composable () -> Unit)

class AzNavHostScopeImpl(
    private val railScope: AzNavRailScopeImpl = AzNavRailScopeImpl()
) : AzNavHostScope, AzNavRailScope by railScope {

    private var _navController: NavHostController? = null

    override val navController: NavHostController
        get() = _navController ?: error("NavController not initialized. Ensure this scope is used within AzNavHost.")

    override val dockingSide: AzDockingSide
        get() = railScope.dockingSide

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

    // Expose railScope for AzNavRail consumption
    fun getRailScopeImpl() = railScope

    fun resetHost() {
        railScope.reset()
        backgrounds.clear()
        onscreenItems.clear()
    }
}

private enum class AzVisualSide { LEFT, RIGHT, TOP, BOTTOM }

// --- Layouts ---

// AUTHORIZED: This layout is the designated wrapper for the strict AzNavRail.
@OptIn(AzStrictLayout::class)
@Composable
fun AzHostActivityLayout(
    navController: NavHostController, // Mandatory
    modifier: Modifier = Modifier,
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavHostScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val effectiveIsLandscape = isLandscape ?: (configuration.screenWidthDp > configuration.screenHeightDp)

    val effectiveCurrentDestination = if (currentDestination != null) {
        currentDestination
    } else {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        navBackStackEntry?.destination?.route
    }

    val scope = remember { AzNavHostScopeImpl() }
    scope.resetHost()
    scope.setController(navController)

    scope.apply(content)

    // Determine rail settings
    val railScope = scope.getRailScopeImpl()
    val dockingSide = railScope.dockingSide
    val railWidth = railScope.collapsedWidth
    val usePhysicalDocking = railScope.usePhysicalDocking

    // Rotation Logic
    val rotation = LocalView.current.display?.rotation ?: Surface.ROTATION_0
    val visualSide = if (usePhysicalDocking) {
        when (dockingSide) {
            AzDockingSide.LEFT -> when (rotation) {
                Surface.ROTATION_0 -> AzVisualSide.LEFT
                Surface.ROTATION_90 -> AzVisualSide.TOP
                Surface.ROTATION_180 -> AzVisualSide.RIGHT
                Surface.ROTATION_270 -> AzVisualSide.BOTTOM
                else -> AzVisualSide.LEFT
            }
            AzDockingSide.RIGHT -> when (rotation) {
                Surface.ROTATION_0 -> AzVisualSide.RIGHT
                Surface.ROTATION_90 -> AzVisualSide.BOTTOM
                Surface.ROTATION_180 -> AzVisualSide.LEFT
                Surface.ROTATION_270 -> AzVisualSide.TOP
                else -> AzVisualSide.RIGHT
            }
        }
    } else {
        // Stick to view side (Classic behavior)
        if (dockingSide == AzDockingSide.LEFT) AzVisualSide.LEFT else AzVisualSide.RIGHT
    }

    val orientation = if (visualSide == AzVisualSide.TOP || visualSide == AzVisualSide.BOTTOM) AzOrientation.Horizontal else AzOrientation.Vertical
    val railAlignment = when (visualSide) {
        AzVisualSide.LEFT -> Alignment.TopStart
        AzVisualSide.RIGHT -> Alignment.TopEnd
        AzVisualSide.TOP -> Alignment.TopStart
        AzVisualSide.BOTTOM -> Alignment.BottomStart // BottomStart aligns to Bottom-Left, but fills width so creates Bottom Bar
    }

    // Proxy visual docking side for AzNavRail internals (swipe direction)
    val visualDockingSideProxy = if (visualSide == AzVisualSide.BOTTOM || visualSide == AzVisualSide.RIGHT) AzDockingSide.RIGHT else AzDockingSide.LEFT

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = maxHeight
        val safeTop = maxHeight * AzLayoutConfig.SafeTopPercent
        val safeBottom = maxHeight * AzLayoutConfig.SafeBottomPercent

        // Layer 1: Backgrounds
        scope.backgrounds.sortedBy { it.weight }.forEach { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                item.content()
            }
        }

        // Layer 2: Restricted Content (AzHostFragmentLayout)
        val startPadding = if (visualSide == AzVisualSide.LEFT) railWidth else 0.dp
        val endPadding = if (visualSide == AzVisualSide.RIGHT) railWidth else 0.dp
        val topPadding = if (visualSide == AzVisualSide.TOP) railWidth else 0.dp
        val bottomPadding = if (visualSide == AzVisualSide.BOTTOM) railWidth else 0.dp

        CompositionLocalProvider(LocalAzNavHostScope provides scope) {
            AzHostFragmentLayout(
                safeTop = safeTop,
                safeBottom = safeBottom,
                startPadding = startPadding,
                endPadding = endPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                items = scope.onscreenItems,
                dockingSide = dockingSide // Original docking side for logical flipping if needed
            )
        }

        // Layer 3: AzNavRail
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
                railAlignment = railAlignment
            ) {}
        }
    }
}

@Composable
fun AzHostFragmentLayout(
    safeTop: Dp,
    safeBottom: Dp,
    startPadding: Dp,
    endPadding: Dp,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp,
    items: List<AzOnscreenItem>,
    dockingSide: AzDockingSide
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = safeTop + topPadding,
                bottom = safeBottom + bottomPadding,
                start = startPadding,
                end = endPadding
            )
    ) {
        items.forEach { item ->
            // Flip alignment if Right Docked (Physical)
            val finalAlignment = if (dockingSide == AzDockingSide.RIGHT) {
                flipAlignment(item.alignment)
            } else {
                item.alignment
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = finalAlignment
            ) {
                item.content()
            }
        }
    }
}

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
        if (dockingSide == AzDockingSide.LEFT) {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        } else {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
        }
    }

    val defaultExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (dockingSide == AzDockingSide.LEFT) {
            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        } else {
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        }
    }

    val defaultPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (dockingSide == AzDockingSide.LEFT) {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
        } else {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        }
    }

    val defaultPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (dockingSide == AzDockingSide.LEFT) {
            slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        } else {
            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        }
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

private fun flipAlignment(alignment: Alignment): Alignment {
    return when (alignment) {
        is BiasAlignment -> BiasAlignment(
            horizontalBias = -alignment.horizontalBias,
            verticalBias = alignment.verticalBias
        )
        else -> alignment
    }
}
