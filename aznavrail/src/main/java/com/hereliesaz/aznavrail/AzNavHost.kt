package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.model.AzDockingSide

val LocalAzNavHostPresent = compositionLocalOf { false }

interface AzNavHostScope : AzNavRailScope {
    fun background(weight: Int = 0, content: @Composable () -> Unit)
    fun onscreen(alignment: Alignment = Alignment.TopStart, content: @Composable () -> Unit)
}

data class AzBackgroundItem(val weight: Int, val content: @Composable () -> Unit)
data class AzOnscreenItem(val alignment: Alignment, val content: @Composable () -> Unit)

class AzNavHostScopeImpl(
    private val railScope: AzNavRailScopeImpl = AzNavRailScopeImpl()
) : AzNavHostScope, AzNavRailScope by railScope {

    val backgrounds = mutableStateListOf<AzBackgroundItem>()
    val onscreenItems = mutableStateListOf<AzOnscreenItem>()

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
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavHostScope.() -> Unit
) {
    val scope = remember { AzNavHostScopeImpl() }
    scope.resetHost()

    scope.apply(content)

    // Determine rail settings
    val railScope = scope.getRailScopeImpl()
    val dockingSide = railScope.dockingSide
    val railWidth = railScope.collapsedRailWidth

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = maxHeight
        val safeTop = maxHeight * 0.2f // TODO: Replace with SAFE_AREA_TOP_RATIO
        val safeBottom = maxHeight * 0.1f // TODO: Replace with SAFE_AREA_BOTTOM_RATIO

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
        CompositionLocalProvider(LocalAzNavHostPresent provides true) {
            AzNavRail(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                currentDestination = currentDestination,
                isLandscape = isLandscape,
                initiallyExpanded = initiallyExpanded,
                disableSwipeToOpen = disableSwipeToOpen,
                providedScope = railScope
            ) {}
        }
    }
}

private fun flipAlignment(alignment: Alignment): Alignment {
    return when (alignment) {
        Alignment.TopStart -> Alignment.TopEnd
        Alignment.TopEnd -> Alignment.TopStart
        Alignment.TopCenter -> Alignment.TopCenter
        Alignment.CenterStart -> Alignment.CenterEnd
        Alignment.CenterEnd -> Alignment.CenterStart
        Alignment.Center -> Alignment.Center
        Alignment.BottomStart -> Alignment.BottomEnd
        Alignment.BottomEnd -> Alignment.BottomStart
        Alignment.BottomCenter -> Alignment.BottomCenter
        else -> alignment
    }
}
