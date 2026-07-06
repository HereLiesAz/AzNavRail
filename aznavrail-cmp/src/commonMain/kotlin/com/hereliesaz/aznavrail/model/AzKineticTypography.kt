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
