# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Scrollable**: Both rail and menu are scrollable.
- **DSL API**: Simple, declarative API.
- **Multi-line Items**: Supports multi-line text.
- **Stateless**: Hoist and manage state yourself.
- **Shapes**: `CIRCLE`, `SQUARE`, `RECTANGLE`, or `NONE`. `RECTANGLE`/`NONE` auto-size width (fixed 36dp height).
- **Smart Collapse**: Items collapse the rail after interaction.
- **Delayed Cycler**: Built-in delay prevents accidental triggers.
- **Custom Colors**: Apply custom colors to buttons.
- **Dividers**: Add menu dividers.
- **Automatic Header**: Displays app icon or name.
- **Layout**: Pack buttons or preserve spacing.
- **Disabled State**: Disable items or options.
- **Loading State**: Built-in loading animation.
- **Standalone Components**: `AzButton`, `AzToggle`, `AzCycler`, `AzDivider`, `AzRoller`.
- **Navigation**: seamless Jetpack Navigation integration.
- **Hierarchy**: Nested menus with host and sub-items.
- **Draggable (FAB Mode)**: Detach and move the rail.
-   **Reorderable Items**: `AzRailRelocItem` allows user drag-and-drop reordering within clusters.
    -   **Drag**: Long press (with vibration feedback) to start dragging.
    -   **Hidden Menu**: Tap to focus/select. If already focused, tap again to open the hidden menu.
- **System Overlay**: System-wide overlay support with automatic resizing and activity launching.
- **Auto-sizing Text**: Text fits without wrapping (unless explicit newline).
- **Toggles/Cyclers**: Simple state management.
- **Gestures**: Swipe/tap to expand, collapse, or undock.
- **`AzTextBox`**: Modern text box with autocomplete and submit button.
- **`AzRoller`**: A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
- **Info Screen**: Interactive help mode for onboarding with visual guides and coordinate display.
- **Left/Right Docking**: Position the rail on the left or right side of the screen.
- **No Menu Mode**: Treat all items as rail items, removing the side drawer.
- **AzNavHost**: A layout container that enforces strict safe zones and automatic alignment rules.

## AzNavRail for Android (Jetpack Compose)

### Setup

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
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest version
}
```

### Usage

The recommended way to use `AzNavRail` is via the `AzNavHost` wrapper, which enforces layout best practices.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide

@Composable
fun SampleScreen() {
    val navController = rememberNavController()

    AzNavHost(navController = navController) {
        // Configure Rail Settings
        azSettings(
            dockingSide = AzDockingSide.LEFT,
            enableRailDragging = true
        )

        // Define Rail Items
        azRailItem(id = "home", text = "Home", route = "home")

        // Define Background Content (Weighted, ignores safe zones)
        background(weight = 0) {
            Box(Modifier.fillMaxSize().background(Color.White))
        }

        // Define On-Screen Content (Restricted to safe zones)
        onscreen(alignment = Alignment.TopStart) {
            Text("This aligns TopStart if docked Left, and flips to TopEnd if docked Right.")
        }

        onscreen(alignment = Alignment.Center) {
             NavHost(navController = navController, startDestination = "home") {
                composable("home") { Text("Home Screen") }
            }
        }
    }
}
```

### AzNavHost Layout Rules

`AzNavHost` enforces a "Strict Mode" layout system:

1.  **Rail Avoidance**: No content in the `onscreen` block will overlap the rail. Padding is automatically applied based on the docking side.
2.  **Vertical Safe Zones**: Content is restricted from the top 20% and bottom 10% of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

### Info Screen (Help Mode)

`AzNavRail` includes an interactive "Info Screen" mode, ideal for onboarding or help sections.

- **Activation**: Set `infoScreen = true` in `azSettings`.
- **Behavior**:
    - **Visual Guides**: Drawn arrows connect description text to the corresponding rail items.
    - **Coordinates**: The overlay displays the on-screen coordinates of each item in the description, aiding in debugging and layout verification.
    - **Independent Scrolling**: Both the description list and the rail are independently scrollable. Arrows update dynamically to maintain the connection.
    - **Interactivity**: Normal navigation items are disabled and greyed out. However, **Host Items** remain interactive, allowing users to expand and collapse sub-menus to view help for nested items.
    - **Content**: If an item has an `info` string, it is displayed in the scrollable list.
- **Exit**: A Floating Action Button (FAB) appears in the bottom-right corner to exit the mode. You must handle the `onDismissInfoScreen` callback in `azSettings` to set `infoScreen = false`.

```kotlin
var showHelp by remember { mutableStateOf(false) }

AzNavRail(...) {
    azSettings(
        infoScreen = showHelp,
        onDismissInfoScreen = { showHelp = false }
    )

    azRailItem(
        id = "home",
        text = "Home",
        info = "Go to the home screen." // Text displayed in help mode
    )
    // ...
}
```

[API Reference](/API.md)

[Full DSL](/DSL.md)

[Project Structure](/PROJECT_STRUCTURE.md)

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
