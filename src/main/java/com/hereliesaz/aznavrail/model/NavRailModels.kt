package com.hereliesaz.aznavrail.model

import androidx.compose.runtime.Composable

/**
 * Represents the header of the navigation rail.
 *
 * @param content A composable lambda for the header content, typically an icon or logo.
 * @param onClick A lambda to be executed when the header is clicked.
 */
data class NavRailHeader(
    val content: @Composable () -> Unit,
    val onClick: () -> Unit
)

/**
 * A sealed interface representing an item in the collapsed navigation rail.
 * Can be either a simple action button or a stateful cycle button.
 */
sealed interface NavRailItem

/**
 * Represents a simple button with a single action.

 *
 * @param text The text to display on the button.
 * @param onClick A lambda to be executed when the button is clicked.
 */
data class NavRailActionButton(
    val text: String,
    val onClick: () -> Unit
) : NavRailItem

/**
 * Represents a button that cycles through a list of states, with built-in
 * cooldown and rapid-cycle behavior.
 *
 * @param options The list of string options to cycle through.
 * @param initialOption The starting option for the button. Must be one of the options.
 * @param onStateChange A callback that is invoked with the new state whenever it changes.
 */
data class NavRailCycleButton(
    val options: List<String>,
    val initialOption: String,
    val onStateChange: (String) -> Unit
) : NavRailItem


/**
 * Represents a single item in the expanded menu.
 *
 * @param text The text to display for the menu item.
 * @param onClick A lambda to be executed when the item is clicked.
 * @param enabled Whether the menu item is clickable.
 */
data class NavRailMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

/**
 * Represents a section in the expanded menu, containing a title and a list of items.
 *
 * @param title The title of the section. Can be empty for a section without a title.
 * @param items The list of menu items in this section.
 */
data class NavRailMenuSection(
    val title: String,
    val items: List<NavRailMenuItem>
)
