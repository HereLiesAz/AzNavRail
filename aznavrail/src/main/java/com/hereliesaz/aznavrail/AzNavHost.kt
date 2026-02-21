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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzRailLayoutHelper
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.model.AzDockingSide

val LocalAzNavHostPresent = compositionLocalOf { false }
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }
val LocalAzNavHostScope = staticCompositionLocalOf<AzNavHostScope?> { null }

interface AzNavHostScope : AzNavRailScope {
    fun background(weight: Int = 0, content: @Composable () -> Unit)
    fun onscreen(alignment: Alignment = Alignment.Center, content: @Composable () -> Unit)
}

private class AzNavHostScopeImpl(private val railScope: AzNavRailScopeImpl) : AzNavHostScope, AzNavRailScope by railScope {
    val backgrounds = mutableStateListOf<Pair<Int, @Composable () -> Unit>>()
    val onscreenContents = mutableStateListOf<Pair<Alignment, @Composable () -> Unit>>()

    override fun background(weight: Int, content: @Composable () -> Unit) {
        backgrounds.add(weight to content)
    }

    override fun onscreen(alignment: Alignment, content: @Composable () -> Unit) {
        onscreenContents.add(alignment to content)
    }
}

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
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val density = LocalDensity.current

    val screenHeight = configuration.screenHeightDp
    val isLandscapeEffective = isLandscape ?: (configuration.screenWidthDp > configuration.screenHeightDp)

    val display = view.display
    val rotation = display?.rotation ?: Surface.ROTATION_0

    val railScope = remember { AzNavRailScopeImpl() }
    val hostScope = remember(railScope) { AzNavHostScopeImpl(railScope) }

    hostScope.backgrounds.clear()
    hostScope.onscreenContents.clear()
    hostScope.apply(content)

    val layoutConfig = AzRailLayoutHelper.calculateLayout(
        logicalSide = hostScope.dockingSide,
        rotation = rotation,
        usePhysicalDocking = hostScope.usePhysicalDocking
    )

    val visualSide = layoutConfig.visualSide
    val isRightDocked = visualSide == AzVisualSide.RIGHT || visualSide == AzVisualSide.BOTTOM

    val expandedWidth = hostScope.expandedWidth
    val collapsedWidth = hostScope.collapsedWidth

    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val calculatedTop = with(density) { (screenHeight * AzLayoutConfig.ContentSafeTopPercent).toDp() }
    val calculatedBottom = with(density) { (screenHeight * AzLayoutConfig.ContentSafeBottomPercent).toDp() }

    val safeTop = max(calculatedTop, systemBars.calculateTopPadding())
    val safeBottom = max(calculatedBottom, systemBars.calculateBottomPadding())

    val safeZones = AzSafeZones(top = safeTop, bottom = safeBottom)

    CompositionLocalProvider(
        LocalAzNavHostPresent provides true,
        LocalAzSafeZones provides safeZones,
        LocalAzNavHostScope provides hostScope
    ) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val contentPadding = if (hostScope.noMenu) collapsedWidth else expandedWidth

            hostScope.backgrounds.sortedBy { it.first }.forEach { (_, bgContent) ->
                Box(modifier = Modifier.fillMaxSize()) {
                    bgContent()
                }
            }

            hostScope.onscreenContents.forEach { (alignment, onscreenContent) ->
                val adjustedAlignment = if (isRightDocked && alignment is BiasAlignment) {
                    BiasAlignment(horizontalBias = -alignment.horizontalBias, verticalBias = alignment.verticalBias)
                } else {
                    alignment
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = safeZones.top,
                            bottom = safeZones.bottom,
                            start = if (!isRightDocked) contentPadding else 0.dp,
                            end = if (isRightDocked) contentPadding else 0.dp
                        ),
                    contentAlignment = adjustedAlignment
                ) {
                    onscreenContent()
                }
            }

            AzNavRail(
                modifier = Modifier,
                providedScope = railScope,
                navController = navController,
                currentDestination = currentDestination,
                isLandscape = isLandscapeEffective,
                visualDockingSide = if (isRightDocked) AzDockingSide.RIGHT else AzDockingSide.LEFT,
                initiallyExpanded = initiallyExpanded,
                disableSwipeToOpen = disableSwipeToOpen
            )
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
    enterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = {
        val scope = LocalAzNavHostScope.current
        val isRightDocked = scope?.dockingSide == AzDockingSide.RIGHT
        val slideDirection = if (isRightDocked) -1 else 1
        slideInHorizontally(initialOffsetX = { slideDirection * 1000 }) + fadeIn()
    },
    exitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = {
        val scope = LocalAzNavHostScope.current
        val isRightDocked = scope?.dockingSide == AzDockingSide.RIGHT
        val slideDirection = if (isRightDocked) 1 else -1
        slideOutHorizontally(targetOffsetX = { slideDirection * 1000 }) + fadeOut()
    },
    popEnterTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition) = enterTransition,
    popExitTransition: (@JvmSuppressWildcards AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition) = exitTransition,
    builder: NavGraphBuilder.() -> Unit
) {
    androidx.navigation.compose.NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        contentAlignment = contentAlignment,
        route = route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        builder = builder
    )
}
