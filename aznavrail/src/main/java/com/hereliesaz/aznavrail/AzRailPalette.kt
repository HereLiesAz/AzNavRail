package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlin.math.max
import kotlin.math.min

/**
 * The colours the rail on screen is actually wearing, published so that everything else the library
 * draws can wear them too.
 *
 * A second (floating / unattached) rail, a drop-down menu, the About reader, the Help overlay and a
 * popup are all part of the *same* piece of chrome as the rail the user is already looking at. When
 * they fall back to `MaterialTheme.colorScheme` instead, they announce themselves as somebody else's
 * UI — which is exactly what a navigation system must never do. An unset field here means "the rail
 * had no opinion", and the consumer falls back to the theme as before.
 *
 * @property accent The rail's accent — its `activeColor` when set, otherwise the colour its own
 *   items are drawn in (see [azResolveRailAccent]).
 * @property surface The rail's `translucentBackground`, for panels drawn over the app.
 */
@Immutable
data class AzRailPalette(
    val accent: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
)

/**
 * The palette of the rail hosting the current composition. Provided by [AzHostActivityLayout] and by
 * a standalone [AzNavRail]; empty (theme fallback) when no rail is present.
 */
val LocalAzRailPalette = compositionLocalOf { AzRailPalette() }

/**
 * The last palette a rail published, held outside the composition.
 *
 * A composition local only reaches the rail's own subtree, and not every piece of the library's
 * chrome composes inside it: a drop-down's trigger is lifted into the screen-title row, its panel is
 * a [androidx.compose.ui.window.Popup], and a drop-down declared beside `AzHostActivityLayout`
 * rather than inside its content never sees the provider at all. Those are the exact places the
 * drop-down came out in the app theme's purple beside a white rail. Consulting the last rail to
 * compose is a far better guess than the app's `colorScheme`, which belongs to somebody else's UI.
 *
 * It is deliberately never cleared: after the host leaves the composition its colours are still the
 * closest thing to the truth anything drawn afterwards has.
 */
internal object AzRailPaletteRegistry {
    var palette: AzRailPalette by mutableStateOf(AzRailPalette())
}

/**
 * The library's own panel colours, for chrome that must stay legible whatever the host theme is
 * doing. Same ground and ink the About reader uses, so every surface the library puts over the app
 * reads as one piece.
 */
internal object AzChromeColors {
    /** Near-black, very slightly cool, so an accent reads warm against it. */
    val Ground = Color(0xFF101014)

    /** Primary ink on a dark ground. Not pure white — pure white on near-black glares. */
    val Ink = Color(0xFFECECF0)

    /** Primary ink on a light ground. */
    val InkInverse = Color(0xFF101014)
}

/**
 * The accent every AzNavRail composable should draw itself in: the host rail's accent when there is
 * one, the last rail to compose when this composition is outside the host's subtree, otherwise
 * [fallback] (the app theme's primary).
 */
@Composable
internal fun azAccent(fallback: Color = MaterialTheme.colorScheme.primary): Color =
    LocalAzRailPalette.current.accent
        .takeOrElse { AzRailPaletteRegistry.palette.accent }
        .takeOrElse { fallback }

/**
 * The colour of a panel the library drops over the app — a drop-down's dropped menu, and anything
 * else presented as a slice of the rail.
 *
 * The rail's `translucentBackground` supplies the hue; **its alpha is not honoured.** A rail is a
 * strip of buttons over the app and can afford to be see-through; a panel of words cannot. A
 * translucent menu puts the app's own artwork behind its labels, and no accent survives that.
 *
 * With no rail colour to borrow it lands on [AzChromeColors.Ground] rather than
 * `MaterialTheme.colorScheme.surface`, for the same reason the About reader does: the panel belongs
 * to the rail, not to the host theme, and a host theme is free to make its surface translucent.
 */
@Composable
internal fun azPanelColor(): Color =
    LocalAzRailPalette.current.surface
        .takeOrElse { AzRailPaletteRegistry.palette.surface }
        .takeOrElse { AzChromeColors.Ground }
        .copy(alpha = 1f)

/** Plain ink — light or dark — guaranteed to read on [background]. */
internal fun azInkOn(background: Color): Color =
    if (background.luminance() > 0.5f) AzChromeColors.InkInverse else AzChromeColors.Ink

/** WCAG contrast ratio between two opaque colours, from 1.0 (identical) to 21.0 (black on white). */
internal fun azContrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}

/**
 * [preferred] when it can actually be read on [background], plain ink otherwise.
 *
 * Only ever applied to a colour the library *chose* (the rail's accent, or the theme fallback behind
 * it) — never to one a developer named on an item. If you pick the colour you get the colour; if you
 * leave it to the library, the library owes you a legible one. The threshold is WCAG's 3:1 for large
 * text, which is what every label on these panels is.
 */
internal fun azReadableOn(background: Color, preferred: Color): Color {
    if (!preferred.isSpecified) return azInkOn(background)
    // A partly transparent accent is judged as it will actually be seen: over the panel.
    val flattened = preferred.compositeOver(background)
    return if (azContrastRatio(flattened, background) >= 3f) preferred else azInkOn(background)
}

/**
 * The accent a rail reads as.
 *
 * [activeColor] wins when the developer set one. Otherwise it is derived from the rail's own items —
 * the colour most of them are drawn in — because a rail whose every button is white is a white rail,
 * whatever the app's `colorScheme.primary` happens to be. Returns [Color.Unspecified] when the rail
 * expressed no colour at all, leaving the theme in charge.
 */
internal fun azResolveRailAccent(activeColor: Color, items: List<AzNavItem>): Color =
    activeColor.takeOrElse {
        items.asSequence()
            .mapNotNull { it.color }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: Color.Unspecified
    }
