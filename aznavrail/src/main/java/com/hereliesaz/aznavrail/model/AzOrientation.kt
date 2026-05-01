package com.hereliesaz.aznavrail.model

/**
 * Physical orientation of the rail strip itself.
 *
 * Derived automatically from [com.hereliesaz.aznavrail.internal.AzRailLayoutHelper] based on
 * the docking side and device rotation; not usually set directly by consumers.
 */
enum class AzOrientation {
    /** Rail runs along a vertical (left or right) edge — the default portrait configuration. */
    Vertical,
    /** Rail runs along a horizontal (top or bottom) edge — used in landscape physical docking. */
    Horizontal
}
