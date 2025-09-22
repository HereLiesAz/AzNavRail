# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Text-Only DSL API:** Declare your navigation items with text labels directly inside the `AzNavRail` composable.
-   **Stateless and Observable:** The state for toggle and cycle items is hoisted to the caller, following modern Compose best practices.
-   **Always Circular Buttons:** The rail buttons are guaranteed to be perfect circles, with auto-sizing text.
-   **Customizable Colors:** Specify a custom color for each rail button.
-   **Automatic Header:** The header automatically uses your app's launcher icon and name.
-   **Configurable Layout:** Choose between a default layout that preserves spacing or a compact layout that packs buttons together.
-   **Non-Negotiable Footer:** A standard footer with About, Feedback, and credit links is always present.

## Setup

To use this library, 

1) Sacrifice a goat. 
2) Drain its blood into a bowl.
3) Cover the bowl in saran wrap.
4) Put the bowl in the box.
5) Print out the label.
6) Ship it overnight to me with a rubber ducky for bath time.
7) Add JitPack to your `settings.gradle.kts`:

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

    implementation("com.github.HereLiesAz:AzNavRail:3.5") // Or the latest version
}
```

## Usage

Using the new DSL API is incredibly simple. You manage the state in your own composable and pass it to the `AzNavRail`.

```kotlin
import com.hereliesaz.aznavrail.AzNavRail

@Composable
fun MainScreen() {
    var isOnline by remember { mutableStateOf(true) }
    val cycleOptions = listOf("A", "B", "C")
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }

    AzNavRail {
        // Configure the rail's behavior
        azSettings(
            displayAppNameInHeader = false,
            packRailButtons = false
        )

        // Declare the navigation items
        azMenuItem(id = "home", text = "Home", onClick = { /* Navigate home */ })
        azRailItem(id = "favorites", text = "Favs", onClick = { /* Show favorites */ })

        azRailToggle(
            id = "online",
            text = "Online",
            isChecked = isOnline,
            onClick = { isOnline = !isOnline }
        )

        azMenuCycler(
            id = "cycler",
            text = "Cycle",
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

### Standalone Buttons

You can also use the `AzButton`, `AzToggle`, and `AzCycler` composables by themselves. They are styled to look just like the buttons in the `AzNavRail`.

```kotlin
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.AzCycler

@Composable
fun MyScreen() {
    var isToggled by remember { mutableStateOf(false) }

    Column {
        AzButton {
            text("Click Me")
            onClick { /* Do something */ }
        }

        AzToggle(
            isOn = isToggled,
            onToggle = { isToggled = !isToggled }
        ) {
            default(text = "Off")
            alt(text = "On")
        }

        AzCycler {
            state(text = "Option A", onClick = { /* Action for A */ })
            state(text = "Option B", onClick = { /* Action for B */ })
            state(text = "Option C", onClick = { /* Action for C */ })
        }
    }
}
```

### Standalone Buttons API

-   **`AzButton`**: A simple button.
    -   `content: AzButtonScope.() -> Unit`: The DSL for the button content.
        -   `text(text: String)`: Sets the text of the button. Throws an `IllegalArgumentException` if `text` is empty.
        -   `onClick(action: () -> Unit)`: Sets the click action.
        -   `color(color: Color)`: Sets the color of the button.

-   **`AzToggle`**: A toggle button.
    -   `isOn: Boolean`: The state of the toggle.
    -   `onToggle: () -> Unit`: The callback for when the toggle is clicked.
    -   `content: AzToggleScope.() -> Unit`: The DSL for the toggle content.
        -   `default(text: String, color: Color? = null)`: The text and color for the "off" state. Throws an `IllegalArgumentException` if `text` is empty.
        -   `alt(text: String, color: Color? = null)`: The text and color for the "on" state. Throws an `IllegalArgumentException` if `text` is empty.

-   **`AzCycler`**: A button that cycles through a list of states.
    -   `content: AzCyclerScope.() -> Unit`: The DSL for the cycler content.
        -   `state(text: String, onClick: () -> Unit, color: Color? = null)`: Defines a state for the cycler. Throws an `IllegalArgumentException` if `text` is empty.
    -   Throws an `IllegalArgumentException` if no states are defined.

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

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean)`: Configures the settings for the `AzNavRail`. Throws an `IllegalArgumentException` if `expandedRailWidth` is not greater than `collapsedRailWidth`.
-   `azMenuItem(id: String, text: String, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Throws an `IllegalArgumentException` if `text` is empty.
-   `azRailItem(id: String, text: String, color: Color? = null, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Throws an `IllegalArgumentException` if `text` is empty.
-   `azMenuToggle(id: String, text: String, isChecked: Boolean, onClick: () -> Unit)`: Adds a toggle switch item that only appears in the expanded menu. Throws an `IllegalArgumentException` if `toggleOnText` or `toggleOffText` are empty.
-   `azRailToggle(id: String, text: String, color: Color? = null, isChecked: Boolean, onClick: () -> Unit)`: Adds a toggle switch item that appears in both the collapsed rail and the expanded menu. Throws an `IllegalArgumentException` if `toggleOnText` or `toggleOffText` are empty.
-   `azMenuCycler(id: String, text: String, options: List<String>, selectedOption: String, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Throws an `IllegalArgumentException` if `selectedOption` is not in `options`.
-   `azRailCycler(id: String, text: String, color: Color? = null, options: List<String>, selectedOption: String, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. Throws an `IllegalArgumentException` if `selectedOption` is not in `options`.

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
