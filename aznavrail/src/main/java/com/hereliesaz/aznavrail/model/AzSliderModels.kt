package com.hereliesaz.aznavrail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Which of Material 3 Expressive's slider shapes an [com.hereliesaz.aznavrail.AzSlider] takes.
 *
 * The variants differ only in where the active track starts and how many thumbs there are — the
 * track, the inset gap, the thumb and the stop indicators are the same instrument throughout, which
 * is why they are one composable and not four.
 */
enum class AzSliderVariant {
    /** Free movement across the range. The active track runs from the range start to the thumb. */
    CONTINUOUS,

    /**
     * Movement snaps to evenly-spaced stops. The inactive track carries a stop indicator at every
     * step, which is the only cue a user gets that the value is quantised — so it is not optional.
     */
    STEPPED,

    /**
     * The value has a natural zero in the middle — balance, trim, an EQ band. The active track
     * grows *out of the centre* toward the thumb in whichever direction the value went, so the sign
     * of the value is legible without reading the number.
     */
    CENTERED,

    /** Two thumbs bounding a span. The active track is the region between them. */
    RANGE,
}

/**
 * The size ladder from Material 3 Expressive. Bigger is not merely larger: the track thickens
 * faster than the thumb grows, so a large slider reads as a substantial physical control rather
 * than a scaled-up small one.
 *
 * [trackThicknessDp] is the track's cross-axis size; [thumbThicknessDp] is the thumb's along-axis
 * width (the M3 Expressive thumb is a pill standing across the track, not a circle sitting on it).
 */
enum class AzSliderSize(val trackThicknessDp: Int, val thumbThicknessDp: Int, val thumbLengthDp: Int) {
    XSMALL(16, 4, 24),
    SMALL(24, 4, 34),
    MEDIUM(40, 4, 52),
    LARGE(56, 8, 68),
    XLARGE(96, 8, 108),
}

/** Which way an [com.hereliesaz.aznavrail.AzSlider] runs. */
enum class AzSliderOrientation { HORIZONTAL, VERTICAL }

/**
 * Everything about a slider except its live value: the shape it takes, how big it is, which way it
 * runs, and where its stops are.
 *
 * Held as one object so a rail slider item and a standalone [com.hereliesaz.aznavrail.AzSlider] are
 * configured identically — the rail item is the same control, parked in the rail.
 */
@Parcelize
data class AzSliderConfig(
    val variant: AzSliderVariant = AzSliderVariant.CONTINUOUS,
    val size: AzSliderSize = AzSliderSize.SMALL,
    val orientation: AzSliderOrientation = AzSliderOrientation.VERTICAL,
    /** Inclusive bounds. */
    val valueFrom: Float = 0f,
    val valueTo: Float = 1f,
    /**
     * Number of interior stops for [AzSliderVariant.STEPPED]. `steps = 3` over 0..1 gives stops at
     * 0, 0.25, 0.5, 0.75 and 1 — the two ends are always stops and are not counted here, matching
     * Material's own convention.
     */
    val steps: Int = 0,
    /**
     * The origin a [AzSliderVariant.CENTERED] track grows out of. Null means the arithmetic middle
     * of the range, which is what "centred" means in the ordinary case.
     */
    val origin: Float? = null,
    /** Whether the value label appears while the thumb is being dragged. */
    val showValueWhileDragging: Boolean = true,
) : Parcelable
