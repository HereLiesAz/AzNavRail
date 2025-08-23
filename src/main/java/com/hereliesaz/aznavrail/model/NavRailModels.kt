package com.hereliesaz.aznavrail.model

import androidx.compose.runtime.Composable

/**
 * Represents a set of predefined actions that the NavRail can handle internally.
 * This simplifies the API for common use cases.
 */
enum class PredefinedAction {
    ADD,
    FAVORITE,
    HOME,
    SETTINGS,
    ABOUT,
    FEEDBACK
}

/**
 * Represents the header of the navigation rail.
 *
 * @param content A composable lambda for the header content, typically an icon or logo.
 */
data class NavRailHeader(
    val content: @Composable () -> Unit
)

/**
 * A sealed class representing the specific data and logic for a NavItem.
 * This determines whether the item is a simple action, a toggle, or a cycle button.
 */
sealed class NavItemData {
    /**
     * An item that performs a single action when clicked.
     * @param onClick A custom lambda to be executed.
     * @param predefinedAction A predefined action to be executed.
     */
    data class Action(
        val onClick: (() -> Unit)? = null,
        val predefinedAction: PredefinedAction? = null
    ) : NavItemData() {
        init {
            require(onClick == null || predefinedAction == null) { "Action: Cannot provide both onClick and predefinedAction." }
        }
    }

    /**
     * An item that toggles between two states.
     * @param initialIsChecked The initial state of the toggle.
     * @param onStateChange A callback that is invoked with the new boolean state.
     */
    data class Toggle(
        val initialIsChecked: Boolean,
        val onStateChange: (Boolean) -> Unit
    ) : NavItemData()

    /**
     * An item that cycles through a list of options.
     * @param options The list of string options to cycle through.
     * @param initialOption The starting option.
     * @param onStateChange A callback that is invoked with the new state.
     */
    data class Cycle(
        val options: List<String>,
        val initialOption: String,
        val onStateChange: (String) -> Unit
    ) : NavItemData() {
        init {
            require(initialOption in options) { "Cycle: initialOption must be one of the provided options." }
        }
    }
}

/**
 * The unified model for a navigation element. It can be a menu item, a rail button, or both.
 *
 * @param text The primary text to display for the item, used in the expanded menu.
 * @param data The specific data and logic for this item, defining its type and behavior.
 * @param showOnRail If true, this item will also be displayed as a button on the collapsed rail.
 * @param railButtonText Optional text for the rail button. If null, [text] is used.
 * @param enabled Whether the item is clickable.
 */
data class NavItem(
    val text: String,
    val data: NavItemData,
    val showOnRail: Boolean = false,
    val railButtonText: String? = null,
    val enabled: Boolean = true,
    val icon: @Composable (() -> Unit)? = null
)

/**
 * Represents a section in the expanded menu, containing a title and a list of items.
 *
 * @param title The title of the section. Can be empty for a section without a title.
 * @param items The list of [NavItem]s in this section.
 */
data class NavRailMenuSection(
    val title: String,
    val items: List<NavItem>
)
