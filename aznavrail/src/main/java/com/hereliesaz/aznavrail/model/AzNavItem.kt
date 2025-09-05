package com.hereliesaz.aznavrail.model

import androidx.compose.ui.graphics.Color

/**
 * The unified, stateless data model for any item in the navigation rail or menu.
 * This is a text-only component; icons are not supported.
 *
 * @param id A unique identifier for this item.
 * @param text The text to display for this item.
 * @param isRailItem If `true`, this item will be displayed on the collapsed rail. All items are displayed in the expanded menu.
 * @param color The color for the rail button's text and border. Only applies if `isRailItem` is `true`.
 * @param isToggle If `true`, this item behaves like a toggle.
 * @param isChecked The current checked state of the toggle.
 * @param isCycler If `true`, this item behaves like a cycler.
 * @param options The list of options for a cycler.
 * @param selectedOption The currently selected option for a cycler.
 * @param onClick The lambda to be executed when the item is clicked. For toggles and cyclers, this is where you should update your state.
 */
data class AzNavItem(
    val id: String,
    val text: String,
    val isRailItem: Boolean,
    val color: Color? = null,
    val isToggle: Boolean = false,
    val isChecked: Boolean? = null,
    val toggleOnText: String? = null,
    val toggleOffText: String? = null,
    val isCycler: Boolean = false,
    val options: List<String>? = null,
    val selectedOption: String? = null,
    val onClick: () -> Unit
)
