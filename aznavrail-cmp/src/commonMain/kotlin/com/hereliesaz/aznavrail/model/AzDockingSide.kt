package com.hereliesaz.aznavrail.model

/**
 * The logical docking side of the AzNavRail.
 *
 * When [com.hereliesaz.aznavrail.AzNavRailScopeImpl.usePhysicalDocking] is true, the actual
 * visual side adapts to device rotation; otherwise the rail sticks to the named screen edge.
 */
enum class AzDockingSide {
    /** Rail anchored to the left edge of the screen. */
    LEFT,
    /** Rail anchored to the right edge of the screen. */
    RIGHT
}
