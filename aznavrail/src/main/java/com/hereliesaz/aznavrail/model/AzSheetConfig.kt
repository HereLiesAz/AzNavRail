// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/model/AzSheetConfig.kt
package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Static configuration for [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheet] and
 * [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetWindowHost].
 *
 * All values have sensible defaults derived from the LogKitty bottom-sheet shell;
 * override individually for tuning without breaking visual parity.
 *
 * @property backgroundColor Sheet background. Defaults to [Color.Unspecified], in which case the
 *   in-tree shell substitutes `MaterialTheme.colorScheme.surface`.
 * @property backgroundAlpha Alpha blended over [backgroundColor].
 * @property scrimColor Color of the dim layer painted between the sheet and content underneath
 *   when the sheet is at [AzSheetDetent.HALF] or [AzSheetDetent.FULL].
 * @property scrimAlpha Alpha applied to [scrimColor] in the half/full states.
 * @property hiddenStripDp Touch-target height of the otherwise-invisible swipe strip when at [AzSheetDetent.HIDDEN].
 * @property peekDp Height of the [AzSheetDetent.PEEK] state. Mirrors LogKitty's single-line ticker.
 * @property halfFraction Fraction of total height for [AzSheetDetent.HALF].
 * @property fullFraction Fraction of total height for [AzSheetDetent.FULL].
 * @property dragThresholdDp Cumulative vertical drag distance needed to step one detent.
 *   The drag accumulator is reset between gestures so each gesture moves the sheet exactly one step.
 * @property collapseOnBack When `true`, the system back press steps the sheet down rather than
 *   forwarding the event. Honoured by [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetWindowHost]
 *   only when its window is focusable.
 * @property horizontalSwipeEnabled When `true`, horizontal drag on the header invokes the
 *   `onSwipeLeft` / `onSwipeRight` callbacks (used by LogKitty for tab navigation).
 * @property animateInTree When `true`, the in-tree shell uses [androidx.compose.animation.core.animateDpAsState]
 *   to animate between detent heights. The system-overlay flavor always hard-jumps to match LogKitty's look.
 * @property cornerRadiusDp Top-corner radius of the sheet card.
 * @property handleVisible When `true`, draws a centered drag-handle pill at the top of the sheet.
 * @property drawBehindNavBar When `true` **and** the device uses button navigation (3-button /
 *   2-button), the sheet draws behind the system navigation bar: its exposed height above the bar
 *   is unchanged, but the navigation-bar background is forced semi-transparent / see-through so
 *   the sheet content shows through it. Has no effect in gesture navigation (the bar is already a
 *   thin pill). In-tree this makes the host Activity's navigation bar see-through; in the
 *   system-overlay flavor it makes the [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheetWindowHost]
 *   navigation-bar decoration paint semi-transparent. Defaults to `false`.
 */
data class AzSheetConfig(
    val backgroundColor: Color = Color.Unspecified,
    val backgroundAlpha: Float = 0.92f,
    val scrimColor: Color = Color.Black,
    val scrimAlpha: Float = 0.32f,
    val hiddenStripDp: Dp = 28.dp,
    // PEEK sits visibly above HIDDEN — the 56dp default was just slightly taller than the 28dp
    // HIDDEN strip and the two looked nearly identical at runtime. 120dp gives ~92dp of body
    // content area below the drag handle, so PEEK is unambiguously a step up from HIDDEN
    // without overlapping HALF (`halfFraction = 0.5`).
    val peekDp: Dp = 120.dp,
    val halfFraction: Float = 0.5f,
    val fullFraction: Float = 0.9f,
    val dragThresholdDp: Dp = 24.dp,
    val collapseOnBack: Boolean = true,
    val horizontalSwipeEnabled: Boolean = false,
    val animateInTree: Boolean = true,
    val cornerRadiusDp: Dp = 16.dp,
    val handleVisible: Boolean = true,
    val drawBehindNavBar: Boolean = false,
)
