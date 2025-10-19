# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

AzNavRail is a highly customizable and opinionated navigation rail component for Jetpack Compose, designed with a streamlined, DSL-style API. It provides a powerful yet easy-to-use solution for app navigation, ensuring a consistent and polished user experience.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Introducing AzNavRail for Web
**[➡️ View the AzNavRail for Web Documentation](./aznavrail-web/README.md)**


## Features

-   **Responsive Layout:** Automatically adjusts to orientation changes.
-   **Scrollable Content:** Both the rail and the expanded menu are scrollable.
-   **Text-Only DSL API:** A simple, declarative API for building your navigation.
-   **Multi-line Menu Items:** Supports multi-line text with automatic indentation.
-   **Stateless and Observable:** Hoist and manage state in your own composables.
-   **Customizable Shapes:** Choose from `CIRCLE`, `SQUARE`, or `RECTANGLE` for rail buttons.
-   **Uniform Rectangle Width:** All rectangular buttons automatically share the same width.
-   **Smart Collapse Behavior:** Menu items intelligently collapse the rail after interactions.
-   **Delayed Cycler Action:** Cycler items have a built-in delay to prevent accidental triggers.
-   **Customizable Colors:** Apply custom colors to individual rail buttons.
-   **Dividers:** Easily add dividers to your menu.
-   **Automatic Header:** Displays your app's icon or name by default.
-   **Configurable Layout:** Pack buttons together or preserve spacing.
-   **Disabled State:** Disable any item, including individual cycler options.
-   **Loading State:** Show a loading animation over the rail.
-   **Standalone Components:** Use `AzButton`, `AzToggle`, `AzCycler`, and `AzDivider` on their own.

## Setup

To use this library, add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

And add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {

    implementation("com.github.HereLiesAz:AzNavRail:3.14") // Or the latest version
}
```

## Usage

Here is a comprehensive example demonstrating the various features of `AzNavRail`. You can find this code in the `SampleApp`.

```kotlin
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape

@Composable
fun SampleScreen() {
    var isOnline by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val cycleOptions = remember { listOf("A", "B", "C", "D") }
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }
    val context = LocalContext.current

    Row {
        AzNavRail {
            azSettings(
                displayAppNameInHeader = true,
                packRailButtons = false,
                isLoading = isLoading,
                defaultShape = AzButtonShape.RECTANGLE // Set a default shape for all rail items
            )

            // A standard menu item
            azMenuItem(id = "home", text = "Home", onClick = { /* ... */ })

            // A rail item with the default shape (RECTANGLE)
            azRailItem(id = "favorites", text = "Favorites", onClick = { /* ... */ })

            // A disabled rail item that overrides the default shape
            azRailItem(
                id = "profile",
                text = "Profile",
                shape = AzButtonShape.CIRCLE,
                disabled = true,
                onClick = { /* This will not be triggered */ }
            )

            azDivider()

            // A toggle item with the SQUARE shape
            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                shape = AzButtonShape.SQUARE,
                onClick = { isOnline = !isOnline }
            )

            // A cycler with a disabled option
            azRailCycler(
                id = "cycler",
                options = cycleOptions,
                selectedOption = selectedOption,
                disabledOptions = listOf("C"),
                onClick = {
                    val currentIndex = cycleOptions.indexOf(selectedOption)
                    selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
                }
            )

            // A button to demonstrate the loading state
            azRailItem(id = "loading", text = "Load", onClick = { isLoading = !isLoading })
        }

        // Your app's main content goes here
        Column(modifier = Modifier.padding(16.dp)) {
            Text("isOnline: $isOnline")
            Text("selectedOption: $selectedOption")
            Text("isLoading: $isLoading")
        }
    }
}
```

## API Reference

### `AzNavRail`

The main composable for the navigation rail.

```kotlin
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
)
```

### `AzNavRailScope`

The DSL for configuring the `AzNavRail`.

-   `azSettings(...)`: Configures global settings for the rail.
-   `azMenuItem(...)`: Adds an item that only appears in the expanded menu.
-   `azRailItem(...)`: Adds an item to the rail and the expanded menu.
-   `azMenuToggle(...)`: Adds a toggle item to the expanded menu.
-   `azRailToggle(...)`: Adds a toggle item to the rail and the expanded menu.
-   `azMenuCycler(...)`: Adds a cycler item to the expanded menu.
-   `azRailCycler(...)`: Adds a cycler item to the rail and the expanded menu.
-   `azDivider()`: Adds a divider to the menu.

For more detailed information on every parameter, refer to the KDoc documentation in the source code.

## License

```
Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
