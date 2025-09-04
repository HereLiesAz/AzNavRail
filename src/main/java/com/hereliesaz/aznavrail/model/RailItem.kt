package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single item on the collapsed navigation rail.
 * These are the circular buttons that are always visible.
 *
 * This is a sealed class, which means a `RailItem` can be one of the following types:
 * - [RailAction]: A simple clickable button that performs an action.
 * - [RailToggle]: A button with an on/off state.
 * - [RailCycle]: A button that cycles through a list of options.
 *
 * @property id A unique identifier for this rail item.
 * @property text The text to display on the button. The text will auto-size to fit.
 * @property icon The icon to display on the button.
 */

sealed class RailItem(
    open val id: String,
    open val text: String,
    open val icon: ImageVector?
) {
    /**
     * A rail item that performs a single, immediate action when clicked.
     *
     * @param id A unique identifier for this action item.
     * @param text The text to display on the button.
     * @param icon The icon to display on the button.
     * @param onClick The lambda function to be executed when the user clicks on this button.
     */

    data class RailAction(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val onClick: () -> Unit
    ) : RailItem(id, text, icon)

    /**
     * A rail item that represents a toggleable state (on/off).
     *
     * @param id A unique identifier for this toggle item.
     * @param text The text to display on the button.
     * @param icon The icon to display on the button.
     * @param isChecked The initial checked state of the toggle.
     * @param onCheckedChange A callback that is invoked with the new state whenever the user toggles the button.
     */

    data class RailToggle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : RailItem(id, text, icon)

    /**
     * A rail item that allows the user to cycle through a predefined list of options.
     * Each click advances to the next option in the list.
     *
     * @param id A unique identifier for this cycle item.
     * @param text The text to display on the button.
     * @param icon The icon to display on the button.
     * @param options The list of strings that the user can cycle through.
     * @param selectedOption The initial option that is selected from the `options` list.
     * @param onOptionSelected A callback that is invoked with the newly selected option string each time the user clicks the button.
     */

    data class RailCycle(
        override val id: String,
        override val text: String,
        override val icon: ImageVector?,
        val options: List<String>,
        val selectedOption: String,
        val onOptionSelected: (String) -> Unit
    ) : RailItem(id, text, icon)
}
