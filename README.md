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
- **Hierarchy**: Nested menus with host and sub-items (Inline expansion).
- **Nested Rails**: Support for nested navigation structures (`azNestedRail`) with both Vertical (anchored column) and Horizontal (scrollable row) alignments (Popup overlay).
- **Draggable (FAB Mode)**: Detach and move the rail.
- **Reorderable Items**: `AzRailRelocItem` allows user drag-and-drop reordering within clusters.
    - **Drag**: Long press (with vibration feedback) to start dragging.
    - **Hidden Menu**: Tap to focus/select. If already focused, tap again to open the hidden menu.
- **System Overlay**: System-wide overlay support with automatic resizing and activity launching.
- **Auto-sizing Text**: Text fits without wrapping (unless explicit newline).
- **Full-Fill Content**: Rail items with images or colors now completely fill the button shape (crop/bleed).
- **Toggles/Cyclers**: Simple state management.
- **Gestures**: Swipe/tap to expand, collapse, or undock.
- **`AzTextBox`**: Modern text box with autocomplete and submit button.
- **`AzRoller`**: A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
- **Info Screen**: Interactive help mode for onboarding with visual guides and coordinate display.
- **Left/Right Docking**: Position the rail on the left or right side of the screen.
- **Physical Docking**: Experimental mode (`usePhysicalDocking`) to anchor the rail to the physical side of the device, adapting to rotation.
- **No Menu Mode**: Treat all items as rail items, removing the side drawer.
- **AzHostActivityLayout**: A layout container that enforces strict safe zones and automatic alignment rules.
- **AzNavHost**: A wrapper around `androidx.navigation.compose.NavHost` for seamless integration.
- **Smart Transitions**: `AzNavHost` automatically configures directional transitions (slide in/out) based on the docking side (e.g., standard LTR or mirrored for Right dock).
- **Dynamic Content**: Rail buttons can now display solid colors, numbers, or images (via Coil) directly.

## AzNavRail for Android (Jetpack Compose)

### Setup

To use this library, add JitPack to your `settings.gradle.kts`:

~~~kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
~~~

And add the dependency to your app's `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest version
}
~~~

### Usage

**⚠️ STRICT USAGE PROTOCOL**

`AzNavRail` **MUST** be used within an `AzHostActivityLayout` container. The library enforces strict layout rules (safe zones, padding, z-ordering) and will throw a runtime error (or display a red warning screen) if `AzNavRail` is instantiated directly without a host wrapper.

Do **NOT** use `Scaffold`. Use `AzHostActivityLayout` as your root.

**The Golden Sample:**

~~~kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide

@Composable
fun SampleScreen() {
    // You must explicitly manage the controller
    val navController = rememberNavController()

    AzHostActivityLayout(
        navController = navController,
        initiallyExpanded = false
    ) {
        // SECTOR 1: VISUAL COMPLIANCE
        azTheme(
            activeColor = Color.Cyan,
            expandedWidth = 280.dp
        )

        // SECTOR 2: BEHAVIORAL PROTOCOLS
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true,
            displayAppName = true,
            usePhysicalDocking = false // Experimental: Anchor to physical device side
        )

        // SECTOR 3: SPECIAL OPERATIONS
        // azAdvanced(...) // for overlays, help screens, etc.

        // NAVIGATION ITEMS
        azRailItem(id = "home", text = "Home", route = "home")
        azRailItem(id = "settings", text = "Settings", route = "settings")

        // ONSCREEN CONTENT
        // Use 'onscreen' to define your UI. 
        // Layout rules (safe zones, padding) are enforced automatically.
        onscreen(alignment = Alignment.Center) {
            Column(modifier = Modifier.padding(16.dp)) {
                AzNavHost(startDestination = "home") {
                    composable("home") { Text("Home Screen") }
                    composable("settings") { Text("Settings Screen") }
                }
            }
        }
    }
}
~~~

### AzHostActivityLayout Layout Rules

`AzHostActivityLayout` enforces a "Strict Mode" layout system:

1.  **Rail Avoidance**: No content in the `onscreen` block will overlap the rail. Padding is automatically applied based on the docking side.
2.  **Vertical Safe Zones**: Content is restricted from the top (`80.dp`) and bottom (`48.dp`) of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

[API Reference](docs/API.md)

[Full DSL](docs/DSL.md)

[Project Structure](docs/PROJECT_STRUCTURE.md)

## License

Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0