package com.hereliesaz.aznavrail.model

import androidx.compose.ui.Alignment

/**
 * Where an [com.hereliesaz.aznavrail.AzDropdownMenu] panel anchors to its trigger icon and which
 * way it unfolds.
 *
 * The drop-down menu is a standalone, hamburger-style widget placed inline like
 * [com.hereliesaz.aznavrail.AzButton]. This enum names the nine standard anchor points; the panel
 * opens **downward** for top/centre anchors and **upward** for the bottom anchors, so it always
 * grows away from the nearest screen edge. A fine offset nudges the panel from that anchor.
 */
enum class AzDropdownAlignment {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END;

    /** The Compose [Alignment] this anchor maps to within the full-screen drop-down container. */
    fun toAlignment(): Alignment = when (this) {
        TOP_START -> Alignment.TopStart
        TOP_CENTER -> Alignment.TopCenter
        TOP_END -> Alignment.TopEnd
        CENTER_START -> Alignment.CenterStart
        CENTER -> Alignment.Center
        CENTER_END -> Alignment.CenterEnd
        BOTTOM_START -> Alignment.BottomStart
        BOTTOM_CENTER -> Alignment.BottomCenter
        BOTTOM_END -> Alignment.BottomEnd
    }

    /** Horizontal alignment for the trigger/panel column derived from the anchor. */
    fun toHorizontalAlignment(): Alignment.Horizontal = when (this) {
        TOP_START, CENTER_START, BOTTOM_START -> Alignment.Start
        TOP_CENTER, CENTER, BOTTOM_CENTER -> Alignment.CenterHorizontally
        TOP_END, CENTER_END, BOTTOM_END -> Alignment.End
    }

    /**
     * True when the icon is anchored to the bottom of the screen, in which case the unfolded panel
     * is rendered **above** the icon so it opens upward (away from the bottom edge).
     */
    val isBottom: Boolean
        get() = this == BOTTOM_START || this == BOTTOM_CENTER || this == BOTTOM_END
}
