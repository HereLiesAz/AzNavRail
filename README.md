# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An expressive and highly configurable if not contemptable navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose.

This "navigrenuail" provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Plug and Play:** A self-contained component that manages its own expanded/collapsed state.
-   **Expandable & Collapsible:** A compact rail that expands into a full menu.
-   **Swipe-to-Collapse:** Intuitive swipe gesture to close the expanded menu.
-   **App Icon Header:** Optionally displays your app's launcher icon as the header, with a fallback.
-   **Stateful Cycle Buttons:** A special button type that cycles through a list of options with built-in cooldown behavior.
-   **Configurable Footer:** Easily add common menu items like "About" and "Feedback" to a fixed footer.
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
            // ...
                   maven { url 'https://jitpack.io'}
    }

```

And add the dependency to your app's `build.gradle.kts`:

```kotlin
    dependencies {
        implementation("com.github.HereLiesAz:AzNavRail:1.3") // Use the latest version
    }
```


## Usage

Here's a simple example of how to use the `AzNavRail` composable. Because it manages its own state, you can just drop it into your layout.

```kotlin
// In your screen's Composable, e.g., inside a Row
AzNavRail(
    // You can choose to use the app's icon as the header
    useAppIconAsHeader = true,
    // Or provide a custom header
    header = NavRailHeader(
        content = {
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

| Parameter           | Type                               | Description                                                                                                                              |
| ------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `header`            | `NavRailHeader`                    | The configuration for the header content. Ignored if `useAppIconAsHeader` is `true`.                                                     |
| `buttons`           | `List<NavRailItem>`                | The list of buttons to display in the collapsed rail. Can be `NavRailActionButton` or `NavRailCycleButton`.                              |
| `menuSections`      | `List<NavRailMenuSection>`         | The list of sections to display in the expanded menu.                                                                                    |
| `modifier`          | `Modifier`                         | (Optional) The modifier to be applied to the component.                                                                                  |
| `initiallyExpanded` | `Boolean`                          | (Optional) Whether the rail should be expanded when it first appears. Defaults to `false`.                                               |
| `useAppIconAsHeader`| `Boolean`                          | (Optional) If `true`, the rail will display the app's launcher icon in the header. Defaults to `false`.                                  |
| `headerIconSize`    | `Dp`                               | (Optional) The size of the header icon. Defaults to `80.dp`.                                                                             |
| `onAboutClicked`    | `(() -> Unit)?`                    | (Optional) A click handler for the 'About' button in the footer. If `null`, the button is hidden.                                        |
| `onFeedbackClicked` | `(() -> Unit)?`                    | (Optional) A click handler for the 'Feedback' button in the footer. If `null`, the button is hidden.                                     |
| `creditText`        | `String?`                          | (Optional) The text for the credit line in the footer. If `null`, the item is hidden.                                                    |
| `onCreditClicked`   | `(() -> Unit)?`                    | (Optional) A click handler for the credit line.                                                                                          |
