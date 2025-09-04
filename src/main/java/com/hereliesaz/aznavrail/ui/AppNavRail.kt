package com.hereliesaz.aznavrail.ui

import androidx.compose.runtime.Composable
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem

/**
 * A simplified and opinionated wrapper around the [AzNavRail] component.
 * This is the **recommended entry point** for developers using the library.
 * It provides a streamlined API for common use cases, making it incredibly easy to get started.
 *
 * The header of the rail is configured with a single flag, `displayAppNameInHeader`.
 * The component will automatically fetch the app's name and icon from the system.
 *
 * ### Example Usage:
 *
 * ```kotlin
 * @Composable
 * fun MyAppScreen() {
 *     val menuItems = listOf(
 *         MenuItem.MenuAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { ... }),
 *         MenuItem.MenuToggle(id = "online", text = "Online Status", icon = Icons.Default.Cloud, isChecked = true, onCheckedChange = { ... })
 *     )
 *
 *     val railItems = listOf(
 *         RailItem.RailAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { ... }),
 *         RailItem.RailToggle(id = "online", text = "Online", icon = Icons.Default.Cloud, isChecked = true, onCheckedChange = { ... })
 *     )
 *
 *     Scaffold {
 *         Row {
 *             AppNavRail(
 *                 menuItems = menuItems,
 *                 railItems = railItems
 *             )
 *             // The rest of your app's content goes here
 *             Text("Main content area")
 *         }
 *     }
 * }
 * ```
 *
 * @param menuItems The list of items to display in the expanded menu drawer. See [MenuItem].
 * @param railItems The list of items to display as circular buttons on the collapsed rail. See [RailItem].
 * @param displayAppNameInHeader If `true`, the header will display the application's name. If `false` (the default), it will display the application's launcher icon.
 * @param packRailButtons If `true`, the rail buttons will be packed together at the top. If `false` (the default), they will be spaced out to align with their menu item counterparts.
 * @param footerItems The list of items to display in the footer of the expanded menu.
 */
@Composable
fun AppNavRail(
    menuItems: List<MenuItem>,
    railItems: List<RailItem>,
    displayAppNameInHeader: Boolean = false,
    packRailButtons: Boolean = false,
    footerItems: List<MenuItem> = emptyList()
) {
    AzNavRail(
        menuItems = menuItems,
        railItems = railItems,
        displayAppNameInHeader = displayAppNameInHeader,
        packRailButtons = packRailButtons,
        footerItems = footerItems
    )
}
