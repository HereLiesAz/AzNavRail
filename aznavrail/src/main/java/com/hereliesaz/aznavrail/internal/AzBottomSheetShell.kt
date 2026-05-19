// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/AzBottomSheetShell.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.bottomsheet.AzSheetController
import com.hereliesaz.aznavrail.model.AzSheetConfig
import com.hereliesaz.aznavrail.model.AzSheetDetent

/**
 * Shared visual shell used by both the in-tree `AzBottomSheet` composable and the
 * system-overlay `AzBottomSheetWindowHost`. Renders:
 *
 *  1. A transparent scrim above the sheet (catching taps to [AzSheetController.stepDown]) when
 *     the detent is HALF or FULL.
 *  2. A rounded-top card sized via [heightForDetent], anchored at the bottom of the available space.
 *  3. A drag-handle pill (when [AzSheetConfig.handleVisible]) plus the always-present hidden swipe
 *     strip; both attach the accumulated-delta vertical-drag modifier.
 *  4. The caller-supplied [content] inside the card.
 *
 * @param controller Controller backing the sheet's state.
 * @param config Static configuration.
 * @param parentHeight The container height used to resolve fraction-based detents.
 * @param animate When `true`, animates between detent heights. The in-tree path passes
 *   `config.animateInTree`; the system-overlay path passes `false` since the window itself jumps.
 * @param onSwipeLeft / [onSwipeRight] Optional horizontal-swipe callbacks gated by
 *   [AzSheetConfig.horizontalSwipeEnabled].
 * @param content Caller-provided sheet content.
 */
@Composable
internal fun AzBottomSheetShell(
    controller: AzSheetController,
    config: AzSheetConfig,
    parentHeight: Dp,
    animate: Boolean,
    onSwipeLeft: (() -> Unit)?,
    onSwipeRight: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val targetHeight = heightForDetent(controller.detent, parentHeight, config)
    val animatedHeight by animateDpAsState(
        targetValue = if (animate) targetHeight else targetHeight,
        label = "az-sheet-height",
    )
    val effectiveHeight = if (animate) animatedHeight else targetHeight

    val resolvedBackground = if (config.backgroundColor.isUnspecified()) {
        MaterialTheme.colorScheme.surface
    } else {
        config.backgroundColor
    }
    val sheetColor = resolvedBackground.copy(alpha = config.backgroundAlpha)

    val showScrim = controller.detent == AzSheetDetent.HALF || controller.detent == AzSheetDetent.FULL

    Box(modifier = modifier.fillMaxSize()) {
        if (showScrim) {
            // Transparent dim layer; tap steps the sheet down one detent (mirrors LogKitty).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(config.scrimColor.copy(alpha = config.scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { controller.stepDown() }
            )
        }

        val swipeMod = if (config.horizontalSwipeEnabled && onSwipeLeft != null && onSwipeRight != null) {
            Modifier.azSheetHorizontalSwipe(density, 48.dp, onSwipeLeft, onSwipeRight)
        } else {
            Modifier
        }

        // At HIDDEN, tap on the strip reveals PEEK. This makes the affordance work for users
        // whose first instinct is to tap rather than drag.
        val hiddenTapMod = if (controller.detent == AzSheetDetent.HIDDEN) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controller.stepUp() }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(effectiveHeight)
                .clip(RoundedCornerShape(topStart = config.cornerRadiusDp, topEnd = config.cornerRadiusDp))
                .background(sheetColor)
                .azSheetVerticalDrag(controller, density, config.dragThresholdDp)
                .then(swipeMod)
                .then(hiddenTapMod)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (config.handleVisible) {
                    // Always render the handle when configured visible — including at HIDDEN —
                    // so the swipe-up affordance is discoverable. The handle dims at HIDDEN to
                    // match the "near-invisible strip" intent without becoming untouchable.
                    val isHidden = controller.detent == AzSheetDetent.HIDDEN
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isHidden) 14.dp else 20.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (isHidden) 0.20f else 0.32f
                                    )
                                )
                        )
                    }
                }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                ) { content() }
            }
        }
    }
}

private fun Color.isUnspecified(): Boolean = this.value == Color.Unspecified.value
