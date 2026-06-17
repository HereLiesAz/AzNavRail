package com.hereliesaz.aznavrail.model

/**
 * Selects which set of items the rail reveals when it is used as a drop-down menu
 * (see [com.hereliesaz.aznavrail.AzNavRailScope.azConfig]'s `dropdownMenu` flag).
 *
 * In drop-down mode there is exactly **one** set of items — the app icon acts as the menu
 * trigger and tapping it unfolds whichever set this enum names. There is no rail-to-menu
 * expansion; the developer commits to one of the two renderings.
 */
enum class AzDropdownSource {
    /**
     * Unfold the **rail** items (everything declared via `azRail*`), rendered as the packed
     * rail buttons exactly as they would appear on the collapsed rail strip.
     */
    RAIL,

    /**
     * Unfold the **menu** items (the full expandable drawer, including `azRail*` and `azMenu*`
     * entries), rendered as the menu rows exactly as they would appear in the expanded drawer.
     */
    MENU
}
