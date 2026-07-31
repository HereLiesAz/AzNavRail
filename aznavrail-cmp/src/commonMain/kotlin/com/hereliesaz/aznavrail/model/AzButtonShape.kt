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
    /**
     * No border is drawn; the button is still interactive but visually borderless. It keeps the
     * [RECTANGLE] footprint — the wide, fixed-height slot a text label wants.
     *
     * Use [NONE_SQUARE] or [NONE_CIRCLE] for a borderless button on a square footprint instead, so
     * it still lines up with the bordered items around it.
     */
    NONE,
    /** Borderless, on the [SQUARE] footprint. */
    NONE_SQUARE,
    /** Borderless, on the [CIRCLE] footprint (square box, circular clip). */
    NONE_CIRCLE;

    /** True when the button draws no border — the whole `NONE*` family. */
    val isBorderless: Boolean
        get() = this == NONE || this == NONE_SQUARE || this == NONE_CIRCLE

    /**
     * The shape whose footprint and outline this one adopts. Identity for every bordered shape; for
     * the borderless family, the base shape the developer picked.
     */
    val baseShape: AzButtonShape
        get() = when (this) {
            NONE -> RECTANGLE
            NONE_SQUARE -> SQUARE
            NONE_CIRCLE -> CIRCLE
            else -> this
        }
}
