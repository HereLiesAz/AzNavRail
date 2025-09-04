package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single item in the expanded navigation menu.
 *
 * This is a sealed class, which means a `MenuItem` can be one of the following types:
 * - [MenuAction]: A simple clickable item that performs an action.
 * - [MenuToggle]: An item with an on/off state, like a switch or checkbox.
 * - [MenuCycle]: An item that cycles through a list of options.
 *
 * @property id A unique identifier for this menu item.
 * @property text The text to display for this item in the menu.
 * @property icon The icon to display next to the text. Can be null if no icon is desired.
 */

sealed class MenuItem(
    open val id: String,
    open val text: String,
    open val icon: ImageVector?
) {
    /**
     * A menu item that performs a single, immediate action when clicked.
     * Think of this as a standard button.
     *
     * @param id A unique identifier for this action item.
     * @param text The text to display for this item.
     * @param icon The icon to display next to the text.
     * @param onClick The lambda function to be executed when the user clicks on this item.
     */

    data class MenuAction(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val onClick: () -> Unit
    ) : MenuItem(id, text, icon)

    /**
     * A menu item that represents a toggleable state (on/off).
     * It maintains a boolean state and provides a callback for when that state changes.
     *
     * @param id A unique identifier for this toggle item.
     * @param text The text to display for this item.
     * @param icon The icon to display next to the text.
     * @param isChecked The initial checked state of the toggle.
     * @param onCheckedChange A callback that is invoked with the new state whenever the user toggles the item.
     */

    data class MenuToggle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : MenuItem(id, text, icon)

    /**
     * A menu item that allows the user to cycle through a predefined list of options.
     * Each click advances to the next option in the list, looping back to the beginning.
     *
     * @param id A unique identifier for this cycle item.
     * @param text The text to display for this item.
     * @param icon The icon to display next to the text.
     * @param options The list of strings that the user can cycle through.
     * @param selectedOption The initial option that is selected from the `options` list.
     * @param onOptionSelected A callback that is invoked with the newly selected option string each time the user clicks the item.
     */

    data class MenuCycle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : MenuItem(id, text, icon)
}
