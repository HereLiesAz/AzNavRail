package com.hereliesaz.aznavrail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import com.hereliesaz.aznavrail.model.AzNavItem

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
 * The accent every AzNavRail composable should draw itself in: the host rail's accent when there is
 * one, otherwise [fallback] (the app theme's primary).
 */
@Composable
internal fun azAccent(fallback: Color = MaterialTheme.colorScheme.primary): Color =
    LocalAzRailPalette.current.accent.takeOrElse { fallback }

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
