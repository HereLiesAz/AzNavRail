package com.hereliesaz.aznavrail.model

/**
 * The visual design of an [com.hereliesaz.aznavrail.AzDropdownMenu]'s dropped panel.
 *
 * The panel is presented as a slice of the AzNavRail itself: pick whether it looks like the
 * collapsed **rail** or the expanded **menu**. The choice drives both the item rendering and the
 * panel's width (so it matches the rail/menu it imitates).
 */
enum class AzDropdownDesign {
    /**
     * The collapsed-rail look: compact rail buttons (icon/auto-sized text), constrained to the
     * collapsed rail width (matching `azConfig`'s `collapsedWidth`, 100dp by default).
     */
    RAIL,

    /**
     * The expanded-menu look: full-width labeled rows, constrained to the expanded menu width
     * (matching `azConfig`'s `expandedWidth`, 160dp by default).
     */
    MENU
}
