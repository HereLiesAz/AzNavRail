# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An expressive and highly configurable if not contemptable navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose.

This "navigrenuail" provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Single Source of Truth:** Define your navigation items once. The rail buttons are automatically generated from your menu items.
-   **Unified API:** A single `NavItem` model for all items, whether they are simple actions, toggles, or cycle buttons.
-   **Expandable & Collapsible:** A compact rail that expands into a full menu.
-   **Simplified Actions:** Use `PredefinedAction` for common tasks like Home, Settings, and About, or provide your own custom lambdas.
-   **Stateful Buttons:** `Toggle` and `Cycle` items automatically manage their state, which is shared between the rail button and the menu item.
-   **Swipe-to-Collapse:** Intuitive swipe gesture to close the expanded menu.
-   **Theming:** Adapts to the `MaterialTheme` of the consuming application.

## Setup

To use this library, 

1) Sacrifice a goat. 
2) Drain its blood into a bowl.
3. Cover the bowl in saran wrap.
4) Put the bowl in the box.
5) Print out the label.
6) Ship it overnight to me with a rubber ducky for bath time.
7) Add JitPack to your settings.gradle.kts:

```kotlin
    dependencyResolutionManagement {
            // ...
                   maven { url 'https://jitpack.io'}
    }
```

And add the dependency to your app's `build.gradle.kts`. The library now includes `coil-compose` as a transitive dependency, so you don't need to add it separately.

```kotlin
    dependencies {
        implementation("com.github.HereLiesAz:AzNavRail:1.7") // Or latest version
    }
```


## Usage

Here's an example of how to use the new unified API. You define your navigation structure once using `NavItem`, and `AzNavRail` handles the rest.

```kotlin
// In your screen's Composable, e.g., inside a Row
AzNavRail(
    header = NavRailHeader { /* ... */ },
    // Define all your navigation items in sections.
    // The rail buttons will be generated from this list.
    menuSections = listOf(
        NavRailMenuSection(
            title = "Main",
            items = listOf(
                // This item is in the menu AND on the rail.
                NavItem(
                    text = "Home",
                    data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                    showOnRail = true
                ),
                // This is a toggle button on the rail and in the menu.
                NavItem(
                    text = "Online",
                    data = NavItemData.Toggle(
                        initialIsChecked = true,
                        onStateChange = { isOnline -> /* ... */ }
                    ),
                    showOnRail = true,
                    railButtonText = "On" // Optional different text for the rail
                ),
                // This item is only in the menu.
                NavItem(
                    text = "Profile",
                    data = NavItemData.Action(onClick = { /* ... */ })
                )
            )
        ),
        NavRailMenuSection(
            title = "Settings",
            items = listOf(
                // A cycle button, which can optionally appear on the rail.
                NavItem(
                    text = "Theme",
                    data = NavItemData.Cycle(
                        options = listOf("Light", "Dark", "System"),
                        initialOption = "System",
                        onStateChange = { theme -> /* ... */ }
                    ),
                    showOnRail = true
                ),
                NavItem(
                    text = "App Settings",
                    data = NavItemData.Action(predefinedAction = PredefinedAction.SETTINGS)
                )
            )
        )
    ),
    // A handler for all predefined actions.
    onPredefinedAction = { action ->
        when (action) {
            PredefinedAction.HOME -> { /* Navigate to Home */ }
            PredefinedAction.SETTINGS -> { /* Navigate to Settings */ }
            else -> {}
        }
    },
    // Optionally allow cycle buttons on the rail.
    allowCyclersOnRail = true
)
```

### API Reference

#### `AzNavRail` Composable

| Parameter            | Type                               | Description                                                                                                                              |
| -------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `header`             | `NavRailHeader`                    | The configuration for the header content. Ignored if `useAppIconAsHeader` is `true`.                                                     |
| `menuSections`       | `List<NavRailMenuSection>`         | The list of sections that defines the entire navigation structure.                                                                       |
| `onPredefinedAction` | `(PredefinedAction) -> Unit`       | A handler for actions that use the `PredefinedAction` enum.                                                                              |
| `allowCyclersOnRail` | `Boolean`                          | (Optional) If `true`, items of type `NavItemData.Cycle` will be shown on the rail. Defaults to `false`.                                  |
| `modifier`           | `Modifier`                         | (Optional) The modifier to be applied to the component.                                                                                  |
| `initiallyExpanded`  | `Boolean`                          | (Optional) Whether the rail should be expanded when it first appears. Defaults to `false`.                                               |
| `useAppIconAsHeader` | `Boolean`                          | (Optional) If `true`, the rail will display the app's launcher icon in the header. Defaults to `false`.                                  |
| `headerIconSize`     | `Dp`                               | (Optional) The size of the header icon. Defaults to `80.dp`.                                                                             |
| `creditText`         | `String?`                          | (Optional) The text for the credit line in the footer.                                                                                   |
| `onCreditClicked`    | `(() -> Unit)?`                    | (Optional) A click handler for the credit line.                                                                                          |

#### `NavItem` Model

The `NavItem` is the core data model for defining a navigation element.

`data class NavItem(text: String, data: NavItemData, showOnRail: Boolean = false, railButtonText: String? = null, enabled: Boolean = true)`

#### `NavItemData` Sealed Class

This class defines the behavior of a `NavItem`.

-   `NavItemData.Action(onClick: (() -> Unit)?, predefinedAction: PredefinedAction?)`: A simple clickable item.
-   `NavItemData.Toggle(initialIsChecked: Boolean, onStateChange: (Boolean) -> Unit)`: An item that toggles between two states.
-   `NavItemData.Cycle(options: List<String>, initialOption: String, onStateChange: (String) -> Unit)`: An item that cycles through a list of options.
