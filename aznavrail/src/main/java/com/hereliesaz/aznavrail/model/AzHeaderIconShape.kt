package com.hereliesaz.aznavrail.model

/** Shape of the app icon displayed in the rail header when [com.hereliesaz.aznavrail.AzNavRailScopeImpl.displayAppName] is false. */
enum class AzHeaderIconShape {
    /** Clips the icon to a perfect circle. */
    CIRCLE,
    /** No clipping — the icon is rendered as-is (typically square). */
    SQUARE,
    /** Clips the icon to a rounded rectangle (12 dp corner radius). */
    ROUNDED,
    /** Alias for SQUARE; no clip applied. */
    NONE
}
