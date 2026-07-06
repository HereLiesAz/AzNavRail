package com.hereliesaz.aznavrail.model

/** Shape options for AzNavRail buttons. */
enum class AzButtonShape {
    /** Renders as a circle (square dimensions, circular clip). */
    CIRCLE,
    /** Renders as a square with sharp corners. */
    SQUARE,
    /** Renders as a fixed-height wide rectangle (typically for text labels). */
    RECTANGLE,
    /** No border is drawn; the button is still interactive but visually borderless. */
    NONE
}
