package com.hereliesaz.aznavrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem

/**
 * A simplified and opinionated wrapper around the [AzNavRail] component.
 * This is the recommended entry point for developers using the library.
 * It provides a streamlined API for common use cases.
 *
 * @param headerText The text to display in the header, typically the app name.
 * @param headerIcon The icon to display in the header.
 * @param menuItems The list of items to display in the expanded menu.
 * @param railItems The list of items to display on the collapsed rail.
 * @param footerItems The list of items to display in the footer of the expanded menu.
 */
@Composable
fun AppNavRail(
    headerText: String,
    headerIcon: ImageVector?,
    menuItems: List<MenuItem>,
    railItems: List<RailItem>,
    footerItems: List<MenuItem> = emptyList()
) {
    AzNavRail(
        headerText = headerText,
        headerIcon = headerIcon,
        menuItems = menuItems,
        railItems = railItems,
        footerItems = footerItems
    )
}
