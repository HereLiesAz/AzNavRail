// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/bottomsheet/AzBottomSheet.kt
package com.hereliesaz.aznavrail.bottomsheet

import com.hereliesaz.aznavrail.internal.AzBackHandler
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.aznavrail.internal.AzBottomSheetShell
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * An in-tree bottom-sheet shell. Renders the LogKitty-style four-detent sheet
 * (HIDDEN / PEEK / HALF / FULL) anchored to the bottom of the parent, with a drag handle,
 * accumulated-delta vertical drag gesture, a scrim in the HALF/FULL states, and optional
 * horizontal-swipe callbacks.
 *
 * When used inside [com.hereliesaz.aznavrail.AzHostActivityLayout] via the [com.hereliesaz.aznavrail.AzNavHostScope.azBottomSheet]
 * DSL, the sheet draws on top of the rail, the menu, and the onscreen content area, and respects
 * [WindowInsets.navigationBars] so it sits behind the system navigation bar rather than over it.
 *
 * The sheet is *not* a background; do not register it via [com.hereliesaz.aznavrail.AzNavHostScope.background].
 *
 * @param controller State holder (use [rememberAzSheetController]).
 * @param modifier Modifier applied to the sheet's root box. The DSL form already applies
 *   `Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)`.
 * @param config Static configuration; see [AzSheetConfig] for tunables.
 * @param onSwipeLeft Optional horizontal-swipe-left callback, gated by [AzSheetConfig.horizontalSwipeEnabled].
 * @param onSwipeRight Optional horizontal-swipe-right callback, gated by [AzSheetConfig.horizontalSwipeEnabled].
 * @param content The caller-provided sheet content. Fill it with whatever you like — the shell
 *   provides only the chrome.
 */
@Composable
fun AzBottomSheet(
    controller: AzSheetController,
    modifier: Modifier = Modifier,
    config: AzSheetConfig = AzSheetConfig(),
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (config.collapseOnBack) {
        AzBackHandler(enabled = controller.detent != AzSheetDetent.HIDDEN) {
            controller.stepDown()
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        AzBottomSheetShell(
            controller = controller,
            config = config,
            parentHeight = maxHeight,
            animate = config.animateInTree,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            content = content,
        )
    }
}

/**
 * Convenience overload that defaults the modifier to one which honours the system navigation-bar
 * inset, so the sheet sits behind the system nav bar without callers needing to apply it.
 */
@Composable
fun AzBottomSheetInsetAware(
    controller: AzSheetController,
    config: AzSheetConfig = AzSheetConfig(),
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    AzBottomSheet(
        controller = controller,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
        config = config,
        onSwipeLeft = onSwipeLeft,
        onSwipeRight = onSwipeRight,
        content = content,
    )
}
