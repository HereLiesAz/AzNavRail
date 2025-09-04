package com.hereliesaz.aznavrail.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem

/**
 * A simplified and opinionated wrapper around the [AzNavRail] component.
 * This is the **recommended entry point** for developers using the library.
 * It provides a streamlined API for common use cases, making it incredibly easy to get started.
 *
 * Simply create your lists of `MenuItem`s and `RailItem`s and pass them to this composable.
 *
 * ### Example Usage:
 *
 * ```kotlin
 * @Composable
 * fun MyAppScreen() {
 *     val menuItems = listOf(
 *         MenuItem.MenuAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { ... }),
 *         MenuItem.MenuToggle(id = "online", text = "Online Status", icon = Icons.Default.Cloud, isChecked = true, onCheckedChange = { ... }),
 *         MenuItem.MenuAction(id = "settings", text = "Settings", icon = Icons.Default.Settings, onClick = { ... })
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
 *                 headerText = "My Awesome App",
 *                 headerIcon = Icons.Default.AcUnit,
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
 * @param headerText The text to display in the header, typically the app name.
 * @param headerIcon The icon to display in the header. If null, a default menu icon will be used.
 * @param menuItems The list of items to display in the expanded menu drawer. See [MenuItem].
 * @param railItems The list of items to display as circular buttons on the collapsed rail. See [RailItem].

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
