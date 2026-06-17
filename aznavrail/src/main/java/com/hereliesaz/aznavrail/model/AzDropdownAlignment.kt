package com.hereliesaz.aznavrail.model

import androidx.compose.ui.Alignment

/**
 * Where the drop-down menu's trigger icon is anchored on screen
 * (see [com.hereliesaz.aznavrail.AzNavRailScope.azConfig]'s `dropdownMenu` flag).
 *
 * In drop-down mode the rail is **not** a docked side-strip — it is a single hamburger-style icon
 * the developer can place anywhere, exactly like a plain menu button. This enum names the nine
 * standard anchor points; a fine `dropdownOffset` (a [androidx.compose.ui.unit.DpOffset]) nudges
 * the icon from that anchor. The unfolded panel opens **downward** for top/centre anchors and
 * **upward** for the bottom anchors, so it always grows away from the nearest screen edge.
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
