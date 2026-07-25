package com.hereliesaz.aznavrail.model

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Windows-Phone-7-style entrance for a drop-down item. Items animate in when the panel opens,
 * cascaded by their position (see `azConfig(entranceStaggerMs = …)`).
 *
 * - [None] — items appear immediately (default; no animation).
 * - [Fade] — items fade up from transparent.
 * - [SlideUp] — items rise into place (translation) while fading.
 * - [Turnstile] — the signature WP7 sweep: each item swings in around its docked edge like a
 *   turnstile (`rotationY` from `azConfig(entranceStartAngle = …)` back to flat).
 */
enum class AzEntrance { None, Fade, SlideUp, Turnstile }

/**
 * Optional exit for a drop-down item when the panel dismisses.
 *
 * Accepted by `azConfig` for API stability, but only [None] is honored today: the drop-down
 * unmounts its items the instant it closes, so a true exit animation would require holding the
 * items mounted through a "closing" state. Non-[None] values are reserved for a future release and
 * currently behave like [None].
 */
enum class AzExit { None, Fade, Turnstile }

/** Reusable easings for AzNavRail's kinetic typography. */
object AzEasing {
    /** WP7's signature fast-out / gentle-settle curve. Snappy. */
    val Wp7Decelerate: Easing = CubicBezierEasing(0.1f, 0.9f, 0.2f, 1f)
}

/**
 * The library's one motion scale.
 *
 * Every transition in AzNavRail reads its timing from here. Before this existed the same two
 * numbers — a 60 ms stagger and a 720 ms item duration — were written out as literal defaults in
 * six separate places, which is how they drifted past the point of being useful: an eight-item
 * drawer took **1.2 seconds** to finish arriving, and because the turnstile entrance starts each
 * item edge-on (and therefore invisible), the panel sat there empty for the first stretch of it.
 *
 * Motion here is a guide, not a toll. It tells the user where a thing came from and then gets out
 * of the way. The durations are drawn from Material 3 Expressive's scale and deliberately sit at
 * its quick end: a cascade the user has to wait out has stopped being a cue and become a delay.
 *
 * The whole scale is proportional, so a host that wants a different tempo can scale every value at
 * once via `azConfig(entranceDurationMs = …, entranceStaggerMs = …)` rather than hunting literals.
 */
object AzMotion {
    /** One item's own entrance or exit. M3 Expressive `medium` — spatial, but on the fast side. */
    const val ItemDurationMs: Int = 280

    /**
     * The gap between one item starting and the next. Small enough that the cascade reads as a
     * single gesture rather than a queue: eight items are fully settled inside ~440 ms, where the
     * old 60 ms tick alone spent 480 ms before the last item had begun.
     */
    const val ItemStaggerMs: Int = 22

    /** A container arriving or leaving — a panel, a scrim, a popup. M3 Expressive `effects`. */
    const val PanelDurationMs: Int = 200

    /** A layout settling into a new size, e.g. the rail's collapsed/expanded width. */
    const val SettleDurationMs: Int = 240

    /** One step of a continuous, non-blocking indicator (the [com.hereliesaz.aznavrail.AzLoad] morph). */
    const val IndicatorStepMs: Int = 420
}
