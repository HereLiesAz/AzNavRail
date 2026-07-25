package com.hereliesaz.aznavrail.model

/**
 * The alert styling applied to the rail item that raised an [com.hereliesaz.aznavrail.AzPopup].
 *
 * Both levels redraw the item as a **yellow, rounded-corner triangle outline** — the universal
 * "something wants your attention" glyph — and both revert the moment the popup is dismissed.
 * They differ only in shade, so a warning reads louder than a notice.
 */
enum class AzItemAlert {
    /** Informational. Drawn in a softer amber. */
    NOTICE,

    /** Something is wrong. Drawn in a full, saturated warning yellow. */
    WARNING,
}
