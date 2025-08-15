# AzNavRail
[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)


An expressive and highly configurable if not contemptable navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose.

This "navigrenuail" provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Expandable & Collapsible:** A compact rail that expands into a full menu.
-   **Swipe-to-Collapse:** Intuitive swipe gesture to close the expanded menu.
-   **Stateful Cycle Buttons:** A special button type that cycles through a list of options with built-in cooldown and rapid-cycle behavior.
-   **Configurable Footer:** Easily add common menu items like "About" and "Feedback" to a fixed footer.
-   **Customizable:** Fully data-driven API allows for complete control over the content of the rail and menu.
-   **Theming:** Adapts to the `MaterialTheme` of the consuming application.

## Setup

To use this library in your project, sacrifice a goat, drain it's blood into a bowl, cover it in saran wrap, put the bowl in the box, print out the label, ship it overnight to me with a rubber ducky for bath time, and then clone the library to your repository as a local module.

1.  Copy the `AzNavRail` library directory into your project's root.
2.  In your `settings.gradle.kts` file, add the module:
    ```kotlin
    include(":AzNavRail")
    ```
3.  In your app's `build.gradle.kts` file, add the dependency:
    ```kotlin
    implementation(project(":AzNavRail"))
    ```

## Usage

Here is a complete example of how to use the `AzNavRail` composable in your app.

```kotlin
// In your screen's Composable

var isExpanded by remember { mutableStateOf(false) }

AzNavRail(
    isExpanded = isExpanded,
    header = NavRailHeader(
        onClick = { isExpanded = !isExpanded },
        content = {
            // Your header icon, e.g., an Image or Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_your_logo),
                contentDescription = "Logo"
            )
        }
    ),
    buttons = listOf(
        NavRailActionButton(
            text = "Reset",
            onClick = { /* Handle reset action */ }
        ),
        NavRailCycleButton(
            options = listOf("Low", "Medium", "High"),
            initialOption = "Low",
            onStateChange = { newState -> /* Handle new state */ }
        )
    ),
    menuSections = listOf(
        NavRailMenuSection(
            title = "Main",
            items = listOf(
                NavRailMenuItem(text = "Profile", onClick = { /* ... */ }),
                NavRailMenuItem(text = "Settings", onClick = { /* ... */ })
            )
        )
    ),
    // Optional Footer Items
    onAboutClicked = { /* Open about screen */ },
    onFeedbackClicked = { /* Open feedback form */ },
    creditText = "Made by You",
    onCreditClicked = { /* ... */ }
)
```

### API Reference

#### `AzNavRail` Composable

| Parameter         | Type                               | Description                                                                                                                              |
| ----------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `isExpanded`      | `Boolean`                          | Controls whether the rail is in its expanded (menu) or collapsed state.                                                                  |
| `header`          | `NavRailHeader`                    | The configuration for the header icon and its click action.                                                                              |
| `buttons`         | `List<NavRailItem>`                | The list of buttons to display in the collapsed rail. Can be `NavRailActionButton` or `NavRailCycleButton`.                              |
| `menuSections`    | `List<NavRailMenuSection>`         | The list of sections to display in the expanded menu.                                                                                    |
| `modifier`        | `Modifier`                         | (Optional) The modifier to be applied to the component.                                                                                  |
| `headerIconSize`  | `Dp`                               | (Optional) The size of the header icon. Defaults to `80.dp`.                                                                             |
| `onAboutClicked`  | `(() -> Unit)?`                    | (Optional) A click handler for the 'About' button in the footer. If `null`, the button is hidden.                                        |
| `onFeedbackClicked`| `(() -> Unit)?`                    | (Optional) A click handler for the 'Feedback' button in the footer. If `null`, the button is hidden.                                     |
| `creditText`      | `String?`                          | (Optional) The text for the credit line in the footer. If `null`, the item is hidden.                                                    |
| `onCreditClicked` | `(() -> Unit)?`                    | (Optional) A click handler for the credit line.                                                                                          |
