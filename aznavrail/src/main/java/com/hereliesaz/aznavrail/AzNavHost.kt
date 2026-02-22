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
import com.hereliesaz.aznavrail.internal.AzRailLayoutHelper
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.model.AzDockingSide

val LocalAzNavHostPresent = compositionLocalOf { false }
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScope?> { null }

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
            it.window.statusBarColor = android.graphics.Color.TRANSPARENT
            it.window.navigationBarColor = android.graphics.Color.TRANSPARENT
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

        // Identify current active item for title display
        val currentActiveItem = scope.navItems.find { item ->
            (item.route != null && item.route == effectiveCurrentDestination) ||
            item.classifiers.any { scope.activeClassifiers.contains(it) }
        }
        val currentTitle = currentActiveItem?.screenTitle ?: currentActiveItem?.text

        CompositionLocalProvider(LocalAzNavHostScope provides scope) {
            AzHostFragmentLayout(
                safeTop = safeTop,
                safeBottom = safeBottom,
                startPadding = startPadding,
                endPadding = endPadding,
                topPadding = topPadding,
                bottomPadding = bottomPadding,
                items = scope.onscreenItems,
                dockingSide = dockingSide,
                currentTitle = currentTitle
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
    dockingSide: AzDockingSide,
    currentTitle: String? = null
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

        // Display Screen Title if available
        if (currentTitle != null && currentTitle != com.hereliesaz.aznavrail.internal.AzNavRailDefaults.NO_TITLE && currentTitle.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
