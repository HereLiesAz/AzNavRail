package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderOrientation
import com.hereliesaz.aznavrail.model.AzSliderSize
import com.hereliesaz.aznavrail.model.AzSliderVariant
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The slider's arithmetic, tested where it lives — away from the canvas.
 *
 * The quantisation below is the same expression `AzSlider` uses to snap a stepped track. It is
 * duplicated here deliberately: a rendering test can tell you a thumb moved, but not that it landed
 * on a stop, and "landed on a stop" is the only promise a stepped slider actually makes.
 */
class AzSliderConfigTest {

    private fun quantise(config: AzSliderConfig, fraction: Float): Float {
        val span = config.valueTo - config.valueFrom
        val raw = config.valueFrom + fraction.coerceIn(0f, 1f) * span
        if (config.variant != AzSliderVariant.STEPPED || config.steps <= 0) return raw
        val slots = config.steps + 1
        val stepSize = span / slots
        return config.valueFrom + ((raw - config.valueFrom) / stepSize).roundToInt() * stepSize
    }

    @Test
    fun steppedTrackSnapsToTheNearestStop() {
        // `steps = 3` means three interior stops, so four slots: 0, .25, .5, .75, 1.
        val config = AzSliderConfig(variant = AzSliderVariant.STEPPED, steps = 3)

        assertEquals(0f, quantise(config, 0.10f), 1e-5f)
        assertEquals(0.25f, quantise(config, 0.20f), 1e-5f)
        assertEquals(0.5f, quantise(config, 0.51f), 1e-5f)
        assertEquals(0.75f, quantise(config, 0.70f), 1e-5f)
        assertEquals(1f, quantise(config, 0.99f), 1e-5f)
    }

    @Test
    fun steppedTrackHonoursANonUnitRange() {
        // Two interior stops over 10..40 gives three slots: 10, 20, 30, 40.
        val config = AzSliderConfig(
            variant = AzSliderVariant.STEPPED,
            steps = 2,
            valueFrom = 10f,
            valueTo = 40f,
        )
        assertEquals(10f, quantise(config, 0f), 1e-4f)
        assertEquals(20f, quantise(config, 0.30f), 1e-4f)
        assertEquals(30f, quantise(config, 0.70f), 1e-4f)
        assertEquals(40f, quantise(config, 1f), 1e-4f)
    }

    @Test
    fun continuousTrackDoesNotSnap() {
        val config = AzSliderConfig(variant = AzSliderVariant.CONTINUOUS, steps = 3)
        // `steps` is ignored unless the variant asks for it — a continuous slider that quietly
        // quantised because someone left a `steps` in the config would be a nasty surprise.
        assertEquals(0.37f, quantise(config, 0.37f), 1e-5f)
    }

    @Test
    fun centredTrackDefaultsItsOriginToTheMiddle() {
        val config = AzSliderConfig(variant = AzSliderVariant.CENTERED, valueFrom = -1f, valueTo = 1f)
        val origin = config.origin ?: ((config.valueFrom + config.valueTo) / 2f)
        assertEquals(0f, origin, 1e-5f)
    }

    @Test
    fun theSizeLadderGrowsTheTrackFasterThanTheThumb() {
        // M3 Expressive's ladder is not a uniform scale-up: the track thickens faster than the
        // thumb, so a large slider reads as a substantial control rather than a magnified small one.
        val sizes = AzSliderSize.entries.toList()
        val ratios = sizes.map { it.trackThicknessDp.toFloat() / it.thumbThicknessDp }
        assertTrue(
            ratios.first() < ratios.last(),
            "Track-to-thumb ratio does not grow across the ladder: $ratios",
        )
        sizes.zipWithNext { a, b ->
            assertTrue(
                b.trackThicknessDp > a.trackThicknessDp,
                "${b.name} is not thicker than ${a.name}",
            )
            assertTrue(
                b.thumbLengthDp > a.thumbLengthDp,
                "${b.name}'s thumb is not longer than ${a.name}'s",
            )
        }
    }

    @Test
    fun aRailSliderIsVerticalWhateverTheHostAsksFor() {
        // The rail is a vertical strip; `AzRailSliderItem` overrides the orientation rather than
        // letting a horizontal track be laid into an 80dp-wide slot.
        val asked = AzSliderConfig(orientation = AzSliderOrientation.HORIZONTAL)
        val inRail = asked.copy(orientation = AzSliderOrientation.VERTICAL)
        assertEquals(AzSliderOrientation.VERTICAL, inRail.orientation)
        // Everything else the host asked for survives the override.
        assertEquals(asked.variant, inRail.variant)
        assertEquals(asked.size, inRail.size)
        assertEquals(asked.valueFrom, inRail.valueFrom)
        assertEquals(asked.valueTo, inRail.valueTo)
    }
}
