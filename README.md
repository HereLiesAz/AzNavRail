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

-   **`modifier`**: The modifier to be applied to the navigation rail.
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

### `AzNavRailScope`

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean, expandedRailWidth: Dp, collapsedRailWidth: Dp, showFooter: Boolean, isLoading: Boolean, defaultShape: AzButtonShape)`: Configures the settings for the `AzNavRail`.
-   `azMenuItem(id: String, text: String, disabled: Boolean, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azDivider()`: Adds a horizontal divider to the expanded menu.

## Other Components

### Loading State

When your app is loading data, you can display an unobtrusive animation over the rail by setting the `isLoading` parameter to `true` in the `azSettings` function. This will disable all buttons and show a loading indicator until `isLoading` is set back to `false`.

This feature is powered by the `AzLoad` composable, which is integrated directly into the `AzNavRail` and does not require any separate setup.

```kotlin
@Composable
fun MainScreen(myViewModel: MyViewModel = viewModel()) {
    val uiState by myViewModel.uiState.collectAsState()

    AzNavRail {
        azSettings(
            isLoading = uiState.isLoading
        )

        // ... your rail and menu items
    }
}
```

### Standalone Components

You can also use the `AzButton`, `AzToggle`, `AzCycler`, and `AzDivider` composables by themselves. They are styled to look just like the components in the `AzNavRail`.

```kotlin
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzDivider

@Composable
fun MyScreen() {
    var isToggled by remember { mutableStateOf(false) }
    val cyclerOptions = listOf("A", "B", "C")
    var selectedOption by remember { mutableStateOf(cyclerOptions.first()) }

    Column {
        AzButton(
            text = "Click Me",
            onClick = { /* Do something */ }
        )

        AzDivider()

        AzToggle(
            isChecked = isToggled,
            onToggle = { isToggled = !isToggled },
            toggleOnText = "On",
            toggleOffText = "Off"
        )

        AzCycler(
            options = cyclerOptions,
            selectedOption = selectedOption,
            onCycle = {
                val currentIndex = cyclerOptions.indexOf(selectedOption)
                selectedOption = cyclerOptions[(currentIndex + 1) % cyclerOptions.size]
            }
        )
    }
}
```

#### `AzButton`
A simple, circular button.
-   `onClick: () -> Unit`: The callback to be invoked when the button is clicked.
-   `text: String`: The text to display on the button.
-   `modifier: Modifier`: The modifier to be applied to the button.
-   `color: Color`: The color of the button's border and text.

#### `AzToggle`
A toggle button.
-   `isChecked: Boolean`: Whether the toggle is in the "on" state.
-   `onToggle: () -> Unit`: The callback to be invoked when the button is toggled.
-   `toggleOnText: String`: The text to display when the toggle is on.
-   `toggleOffText: String`: The text to display when the toggle is off.
-   `modifier: Modifier`: The modifier to be applied to the button.
-   `color: Color`: The color of the button's border and text.

#### `AzCycler`
A button that cycles through a list of options when clicked.
-   `options: List<String>`: The list of options to cycle through.
-   `selectedOption: String`: The currently selected option.
-   `onCycle: () -> Unit`: The callback to be invoked after a 1-second delay on the final selected option.
-   `modifier: Modifier`: The modifier to be applied to the button.
-   `color: Color`: The color of the button's border and text.

#### `AzDivider`
A divider that automatically adjusts its orientation based on the available space.
-   `modifier: Modifier`: The modifier to be applied to the divider.
-   `thickness: Dp`: The thickness of the divider line.
-   `color: Color`: The color of the divider line.
-   `horizontalPadding: Dp`: The padding applied to the left and right of the divider.
-   `verticalPadding: Dp`: The padding applied to the top and bottom of the divider.

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
