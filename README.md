# AzNavRail

An expressive and highly configurable navigation rail component for Jetpack Compose, extracted from the Cue D'etat project.

This component provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Expandable & Collapsible:** A compact rail that expands into a full menu.
-   **Swipe-to-Collapse:** Intuitive swipe gesture to close the expanded menu.
-   **Stateful Cycle Buttons:** A special button type that cycles through a list of options with built-in cooldown and rapid-cycle behavior.
-   **Configurable Footer:** Easily add common menu items like "About" and "Feedback" to a fixed footer.
-   **Customizable:** Fully data-driven API allows for complete control over the content of the rail and menu.
-   **Theming:** Adapts to the `MaterialTheme` of the consuming application.

## Setup

To use this library, first create a release on your GitHub repository to get a version tag (e.g., `v1.0.0`). Then, add JitPack to your project.

1.  In your root `settings.gradle.kts` file, add the JitPack repository:
    ```kotlin
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven { url 'https://jitpack.io' }
        }
    }
    ```

2.  In your app-level `build.gradle.kts` file, add the dependency, replacing `Tag` with your release tag:
    ```kotlin
    dependencies {
        implementation("com.github.HereLiesAz:AzNavRail:Tag")
    }
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
