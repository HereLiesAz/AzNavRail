package com.hereliesaz.aznavrail.model

/**
 * Where an **unattached host rail item** (`azUnattachedHostItem`) parks itself.
 *
 * An unattached host is a rail host item that does not live in the [com.hereliesaz.aznavrail.AzNavRail]
 * strip. It is drawn as its own free-standing button somewhere else on the screen; tapping it unfolds
 * its sub-items (declared the usual way, with `hostId` pointing at it) beneath it, exactly as they
 * would have unfolded inside the rail. Several unattached hosts sharing an anchor stack into a
 * column, again exactly as they would have stacked in the rail.
 */
enum class AzUnattachedAnchor {
    /** The bottom of the screen, on the side opposite the rail. */
    BOTTOM,

    /** The side opposite the rail, at the same height the rail's own items start. */
    OPPOSITE,

    /**
     * Free-floating and draggable. The stack's position is persisted, so it comes back where the
     * user left it on the next launch. It is clamped to the same 10%–90% vertical safe zone the
     * rail's FAB mode uses.
     */
    FLOATING,
}
