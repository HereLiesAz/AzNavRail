// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/model/AzSheetDetent.kt
package com.hereliesaz.aznavrail.model

/**
 * Discrete heights an [com.hereliesaz.aznavrail.bottomsheet.AzBottomSheet] can settle at.
 *
 * The four-step model mirrors the bottom-sheet shell ported from
 * [LogKitty](https://github.com/HereLiesAz/LogKitty); consumers can step
 * up/down with [com.hereliesaz.aznavrail.bottomsheet.AzSheetController.stepUp]
 * and [com.hereliesaz.aznavrail.bottomsheet.AzSheetController.stepDown], or
 * jump directly via [com.hereliesaz.aznavrail.bottomsheet.AzSheetController.snapTo].
 */
enum class AzSheetDetent {
    /** A thin, near-invisible strip along the bottom edge that swallows a drag-up gesture but otherwise lets touches pass through. */
    HIDDEN,

    /** A short ticker-height strip that surfaces a single line of content. */
    PEEK,

    /** Roughly half the screen height (configurable via [AzSheetConfig.halfFraction]). */
    HALF,

    /** Nearly full screen height (configurable via [AzSheetConfig.fullFraction]). */
    FULL,
}
