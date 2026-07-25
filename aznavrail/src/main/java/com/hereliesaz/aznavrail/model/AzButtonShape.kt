package com.hereliesaz.aznavrail.model

/** Shape options for AzNavRail buttons. */
enum class AzButtonShape {
    /** Renders as a circle (square dimensions, circular clip). */
    CIRCLE,
    /** Renders as a square with sharp corners. */
    SQUARE,
    /** Renders as a fixed-height wide rectangle (typically for text labels). */
    RECTANGLE,
    /**
     * Renders as an upward-pointing triangle outline with rounded corners — the warning glyph a rail
     * item takes on while it owns a notice/warning [com.hereliesaz.aznavrail.AzPopup]. Available as
     * an ordinary item shape too.
     */
    TRIANGLE,
    /** No border is drawn; the button is still interactive but visually borderless. */
    NONE
}
