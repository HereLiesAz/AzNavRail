# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An M3 Expressive and highly configurable if not contemptable navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose.

This "navigrenuail" provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Single Source of Truth:** Define your navigation items once. The rail buttons are automatically generated from your menu items.
-   **Unified API:** A single `NavItem` model for all items, whether they are simple actions, toggles, or cycle buttons.
-   **Customizable Buttons:** Provide your own Composable to completely customize the look and feel of the rail buttons.
-   **Expandable & Collapsible:** A compact rail that expands into a full menu.
-   **Simplified Actions:** Use `PredefinedAction` for common tasks like Home, Settings, and About, or provide your own custom lambdas.
-   **Stateful Buttons:** `Toggle` and `Cycle` items automatically manage their state, which is shared between the rail button and the menu item.
-   **Swipe-to-Collapse:** Intuitive swipe gesture to close the expanded menu.
-   **Theming:** Adapts to the `MaterialTheme` of the consuming application.

## Setup

To use this library, 

1) Sacrifice a goat. 
2) Drain its blood into a bowl.
3) Cover the bowl in saran wrap.
4) Put the bowl in the box.
5) Print out the label.
6) Ship it overnight to me with a rubber ducky for bath time.
7) Add JitPack to your settings.gradle.kts:

```kotlin
    dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```

And add the dependency to your app's `build.gradle.kts`. The library now includes `coil-compose` as a transitive dependency, so you don't need to add it separately.

```kotlin
    dependencies {
        implementation("com.github.HereLiesAz:AzNavRail:1.8")
    }
```


## Usage

Here's an example of how to use the new unified API. You define your navigation structure once using `NavItem`, and `AzNavRail` handles the rest.

```kotlin
// In your screen's Composable, e.g., inside a Row
AzNavRail(
    header = NavRailHeader { /* ... */ },
    menuSections = listOf(
        NavRailMenuSection(
            title = "Main",
            items = listOf(
                NavItem(
                    text = "Home",
                    data = NavItemData.Action(predefinedAction = PredefinedAction.HOME),
                    showOnRail = true
                ),
                NavItem(
                    text = "Online",
                    data = NavItemData.Toggle(
                        initialIsChecked = true,
                        onStateChange = { isOnline -> /* ... */ }
                    ),
                    showOnRail = true,
                    railButtonText = "On"
                ),
            )
        )
    ),
    onPredefinedAction = { action ->
        when (action) {
            PredefinedAction.HOME -> { /* Navigate to Home */ }
            else -> {}
        }
    }
)
```

### Customizing Button Content

You can provide a custom composable for the rail buttons using the `buttonContent` parameter. This gives you full control over the appearance, including different styles for active/inactive states.

```kotlin
AzNavRail(
    // ... other parameters
    buttonContent = { item, state ->
        // Get the current state for toggle/cycle buttons
        val isChecked = (state?.value as? Boolean) ?: false
        val color = if (isChecked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

        // Your custom button implementation
        OutlinedButton(
            onClick = { /* The internal onClick logic is handled by the library */ },
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, color.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
        ) {
            Text(text = item.railButtonText ?: item.text)
        }
    }
)
```

### API Reference

#### `AzNavRail` Composable

| Parameter            | Type                                                    | Description                                                                                                                              |
| -------------------- | ------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `header`             | `NavRailHeader`                                         | The configuration for the header content. Ignored if `useAppIconAsHeader` is `true`.                                                     |
| `menuSections`       | `List<NavRailMenuSection>`                              | The list of sections that defines the entire navigation structure.                                                                       |
| `onPredefinedAction` | `(PredefinedAction) -> Unit`                            | A handler for actions that use the `PredefinedAction` enum.                                                                              |
| `buttonContent`      | `@Composable (item: NavItem, state: MutableState<Any>?) -> Unit` | (Optional) A composable lambda to customize the appearance of the rail buttons.                                                        |
| `allowCyclersOnRail` | `Boolean`                                               | (Optional) If `true`, items of type `NavItemData.Cycle` will be shown on the rail. Defaults to `false`.                                  |
| `modifier`           | `Modifier`                                              | (Optional) The modifier to be applied to the component.                                                                                  |
| `initiallyExpanded`  | `Boolean`                                               | (Optional) Whether the rail should be expanded when it first appears. Defaults to `false`.                                               |
| `useAppIconAsHeader` | `Boolean`                                               | (Optional) If `true`, the rail will display the app's launcher icon in the header. Defaults to `false`.                                  |
| `headerIconSize`     | `Dp`                                                    | (Optional) The size of the header icon. Defaults to `80.dp`.                                                                             |
| `creditText`         | `String?`                                               | (Optional) The text for the credit line in the footer.                                                                                   |
| `onCreditClicked`    | `(() -> Unit)?`                                         | (Optional) A click handler for the credit line.                                                                                          |

#### `NavItem` Model

The `NavItem` is the core data model for defining a navigation element.

`data class NavItem(text: String, data: NavItemData, showOnRail: Boolean = false, railButtonText: String? = null, enabled: Boolean = true)`

#### `NavItemData` Sealed Class

This class defines the behavior of a `NavItem`.

-   `NavItemData.Action(onClick: (() -> Unit)?, predefinedAction: PredefinedAction?)`: A simple clickable item.
-   `NavItemData.Toggle(initialIsChecked: Boolean, onStateChange: (Boolean) -> Unit)`: An item that toggles between two states.
-   `NavItemData.Cycle(options: List<String>, initialOption: String, onStateChange: (String) -> Unit)`: An item that cycles through a list of options.
