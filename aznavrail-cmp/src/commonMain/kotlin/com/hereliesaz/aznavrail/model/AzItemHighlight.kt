package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * The four highlights an item can wear.
 *
 * They answer four different questions, which is why there are four of them and not one:
 *
 *  - [ACTIVE] — *where you are.* The item whose route is the current destination, or whose
 *    classifier is in `azConfig(activeClassifiers = …)`. At most one place in an app is the place
 *    you are, so this highlight is the app telling you where it has put you.
 *  - [FOCUS] — *what you are touching.* The item under the finger, and the last item tapped when it
 *    carries no route of its own (a toggle, a cycler, an action). It is about the gesture, not the
 *    destination, and it moves as fast as the user does.
 *  - [SECONDARY] — *whatever you say it is.* The library never sets this one. It is for the state
 *    only the app knows: the item that is syncing, the layer that is locked, the tool that is armed.
 *    Turn it on with `azItemState(id, secondary = true)` or `azConfig(secondaryClassifiers = …)`.
 *  - [TERTIARY] — *a fourth developer-defined highlight tier.* Like secondary, the library never
 *    sets this one. It slots below secondary in precedence (focus > active > secondary > tertiary).
 *    Turn it on with `azItemState(id, tertiary = true)` or `azConfig(tertiaryClassifiers = …)`.
 *
 * All four take their colour from [com.hereliesaz.aznavrail.AzNavRailScope.azTheme] by default and
 * can be overridden per item with [com.hereliesaz.aznavrail.AzNavRailScope.azHighlight].
 */
enum class AzHighlight {
    ACTIVE,
    FOCUS,
    SECONDARY,
    TERTIARY,
}

/**
 * Per-item highlight colours, declared with [com.hereliesaz.aznavrail.AzNavRailScope.azHighlight].
 *
 * A null field means "use the rail's colour for that highlight" — the one set in `azTheme`, and
 * failing that the rail's own accent.
 */
data class AzItemHighlight(
    val active: Color? = null,
    val focus: Color? = null,
    val secondary: Color? = null,
    val tertiary: Color? = null,
)
