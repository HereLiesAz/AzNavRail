package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.model.AzDockingSide

val LocalAzNavHostPresent = compositionLocalOf { false }
val LocalAzSafeZones = compositionLocalOf { AzSafeZones() }

interface AzNavHostScope : AzNavRailScope {
    val navController: NavHostController
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

@Composable
fun AzNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
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
    val railWidth = railScope.collapsedRailWidth

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

        // Layer 2: Restricted Content
        val startPadding = if (dockingSide == AzDockingSide.LEFT) railWidth else 0.dp
        val endPadding = if (dockingSide == AzDockingSide.RIGHT) railWidth else 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeTop, bottom = safeBottom, start = startPadding, end = endPadding)
        ) {
            scope.onscreenItems.forEach { item ->
                // Flip alignment if Right Docked
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
                providedScope = railScope
            ) {}
        }
    }
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
