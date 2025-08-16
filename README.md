# Module AzNavRail

An expressive and highly configurable navigation rail component for Jetpack Compose, extracted from the Cue D'etat project.

This component provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Stateful:** Manages its own expanded/collapsed state. No need for external state management.
-   **Auto App Icon:** Automatically displays the app's launcher icon in the header.
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

Using the `AzNavRail` component is designed to be as simple as possible. Just drop it into your UI. It manages its own state.

```kotlin
// In your screen's Composable

// Just call the component. It's that easy!
AzNavRail(
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
    onFeedbackClicked = { /* Open feedback form */ }
)
```

### API Reference

#### `AzNavRail` Composable

| Parameter         | Type                               | Description                                                                                                                              |
| ----------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `buttons`         | `List<NavRailItem>`                | The list of buttons to display in the collapsed rail. Can be `NavRailActionButton` or `NavRailCycleButton`.                              |
| `menuSections`    | `List<NavRailMenuSection>`         | The list of sections to display in the expanded menu.                                                                                    |
| `modifier`        | `Modifier`                         | (Optional) The modifier to be applied to the component.                                                                                  |
| `headerIconSize`  | `Dp`                               | (Optional) The size of the header icon. Defaults to `80.dp`.                                                                             |
| `onAboutClicked`  | `(() -> Unit)?`                    | (Optional) A click handler for the 'About' button in the footer. If `null`, the button is hidden.                                        |
| `onFeedbackClicked`| `(() -> Unit)?`                    | (Optional) A click handler for the 'Feedback' button in the footer. If `null`, the button is hidden.                                     |
| `creditText`      | `String?`                          | (Optional) The text for the credit line in the footer. If `null`, the item is hidden. Defaults to "@HereLiesAz".                          |
| `onCreditClicked` | `(() -> Unit)?`                    | (Optional) A click handler for the credit line.                                                                                          |

---

## Local Development

If you are working on the library itself, you may want to use it as a local module in a sample app for testing.

1.  In your `settings.gradle.kts` file, add the module:
    ```kotlin
    include(":AzNavRail")
    ```
2.  In your sample app's `build.gradle.kts` file, add the dependency:
    ```kotlin
    implementation(project(":AzNavRail"))
    ```
