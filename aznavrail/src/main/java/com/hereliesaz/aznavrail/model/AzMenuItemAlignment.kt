package com.hereliesaz.aznavrail.model

/**
 * How the text of a menu-drawer label is aligned inside its row.
 *
 * - [CENTER] — legacy behavior: center-aligned regardless of docking side.
 * - [SIDE]   — aligned to the docked side (`Start` when docked LEFT, `End` when docked RIGHT).
 *              This is the WP7-style default: labels hug the same edge the rail itself is anchored to.
 */
enum class AzMenuItemAlignment { CENTER, SIDE }
