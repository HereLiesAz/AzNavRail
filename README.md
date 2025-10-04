# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Introducing AzNavRail for Web
**[➡️ View the AzNavRail for Web Documentation](./aznavrail-web/README.md)**


## Features

-   **Responsive Layout:** The rail automatically adjusts to landscape orientation, ensuring a great user experience on all devices.
-   **Scrollable Content:** Both the expanded menu and the collapsed rail are scrollable, allowing for a large number of navigation items.
-   **Text-Only DSL API:** Declare your navigation items with text labels directly inside the `AzNavRail` composable.
-   **Multi-line Menu Items:** The expanded menu supports multi-line text, with automatic indentation for readability.
-   **Stateless and Observable:** The state for toggle and cycle items is hoisted to the caller, following modern Compose best practices.
-   **Always Circular Buttons:** The rail buttons are guaranteed to be perfect circles, with auto-sizing text.
-   **Smart Collapse Behavior:** Toggle and standard menu items collapse the rail on tap. Cycler items wait for the user to settle on an option before acting and collapsing the rail.
-   **Delayed Cycler Action:** Cycler items update their displayed option instantly but wait for a 1-second delay before triggering the associated action, allowing users to rapid-cycle without unintended consequences.
-   **Customizable Colors:** Specify a custom color for each rail button.
-   **Automatic Header:** The header automatically uses your app's launcher icon by default. You can configure it to display the app name instead.
-   **Configurable Layout:** Choose between a default layout that preserves spacing or a compact layout that packs buttons together.
-   **Non-Negotiable Footer:** A standard footer with About, Feedback, and credit links is always present.
-   **Loading State:** Display a loading animation over the rail while content is loading.

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

    implementation("com.github.HereLiesAz:AzNavRail:3.10") // Or the latest version
}
```

## Usage

Using the new DSL API is incredibly simple. You manage the state in your own composable and pass it to the `AzNavRail`.

```kotlin
import com.hereliesaz.aznavrail.AzNavRail

@Composable
fun MainScreen() {
    var isOnline by remember { mutableStateOf(true) }
    val cycleOptions = listOf("Option A", "Option B", "Option C")
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }

    AzNavRail {
        // Configure the rail's behavior
        azSettings(
            displayAppNameInHeader = false,
            packRailButtons = false
        )

        // Declare the navigation items
        azMenuItem(id = "home", text = "Home\nSweet Home", onClick = { /* Navigate home */ })
        azRailItem(id = "favorites", text = "Favs", onClick = { /* Show favorites */ })

        azRailToggle(
            id = "online",
            isChecked = isOnline,
            toggleOnText = "Online",
            toggleOffText = "Offline",
            onClick = { isOnline = !isOnline }
        )

        azMenuCycler(
            id = "cycler",
            options = cycleOptions,
            selectedOption = selectedOption,
            onClick = {
                val currentIndex = cycleOptions.indexOf(selectedOption)
                selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
            }
        )
    }
}
```

### Loading State

When your app is loading data, you can display an unobtrusive animation over the rail by setting the `isLoading` parameter to `true` in the `azSettings` function. This will disable all buttons and show a loading indicator until the `isLoading` is set back to `false`.

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

### Standalone Buttons

You can also use the `AzButton`, `AzToggle`, and `AzCycler` composables by themselves. They are styled to look just like the buttons in the `AzNavRail`.

```kotlin
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.AzCycler

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

## API Reference

The main entry point is the `AzNavRail` composable.

### `AzNavRail`

```kotlin
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
)
```

An M3-style navigation rail that expands into a full menu drawer.

-   **`modifier`**: The modifier to be applied to the navigation rail.
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

### `AzNavRailScope`

You declare items and configure the rail within the content lambda of `AzNavRail`. The available functions are:

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean, expandedRailWidth: Dp, collapsedRailWidth: Dp, showFooter: Boolean, isLoading: Boolean)`: Configures the settings for the `AzNavRail`.
    -   **`isLoading`**: Whether to show the loading animation. Defaults to `false`.
-   `azMenuItem(id: String, text: String, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azRailItem(id: String, text: String, color: Color? = null, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color? = null, isChecked: Boolean, toggleOnText: String, toggleOffText: String, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color? = null, options: List<String>, selectedOption: String, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.

### Standalone Buttons API

-   **`AzButton`**: A simple button.
    -   `onClick: () -> Unit`: The callback to be invoked when the button is clicked.
    -   `text: String`: The text to display on the button.
    -   `modifier: Modifier`: The modifier to be applied to the button.
    -   `color: Color`: The color of the button's border and text.

-   **`AzToggle`**: A toggle button.
    -   `isChecked: Boolean`: Whether the toggle is in the "on" state.
    -   `onToggle: () -> Unit`: The callback to be invoked when the button is toggled.
    -   `toggleOnText: String`: The text to display when the toggle is on.
    -   `toggleOffText: String`: The text to display when the toggle is off.
    -   `modifier: Modifier`: The modifier to be applied to the button.
    -   `color: Color`: The color of the button's border and text.

-   **`AzCycler`**: A button that cycles through a list of options when clicked.
    -   `options: List<String>`: The list of options to cycle through.
    -   `selectedOption: String`: The currently selected option.
    -   `onCycle: () -> Unit`: The callback to be invoked after a 1-second delay on the final selected option.
    -   `modifier: Modifier`: The modifier to be applied to the button.
    -   `color: Color`: The color of the button's border and text.

### Standalone Components

-   `AzButton(onClick: () -> Unit, text: String, color: Color? = null)`: A standalone circular button.
-   `AzToggle(isChecked: Boolean, onToggle: () -> Unit, toggleOnText: String, toggleOffText: String, color: Color? = null)`: A standalone toggle button.
-   `AzCycler(options: List<String>, selectedOption: String, onCycle: () -> Unit, color: Color? = null)`: A standalone cycler button with delayed action.

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
