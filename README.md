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
- **Standalone Components**: `AzButton`, `AzToggle`, `AzCycler`, `AzDivider`.
- **Navigation**: seamless Jetpack Navigation integration.
- **Hierarchy**: Nested menus with host and sub-items.
- **Draggable (FAB Mode)**: Detach and move the rail.
- **System Overlay**: System-wide overlay support.
- **Auto-sizing Text**: Text fits without wrapping (unless explicit newline).
- **Toggles/Cyclers**: Simple state management.
- **Gestures**: Swipe/tap to expand, collapse, or undock.
- **`AzTextBox`**: Modern text box with autocomplete and submit button.

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

Here is a comprehensive example demonstrating the various features of `AzNavRail`, including Jetpack Navigation integration. You can find this code in the `SampleApp`.

```kotlin
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzHeaderIconShape

@Composable
fun SampleScreen() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    var isOnline by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val railCycleOptions = remember { listOf("A", "B", "C", "D") }
    var railSelectedOption by remember { mutableStateOf(railCycleOptions.first()) }
    val menuCycleOptions = remember { listOf("X", "Y", "Z") }
    var menuSelectedOption by remember { mutableStateOf(menuCycleOptions.first()) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Row {
        AzNavRail(
            navController = navController,
            currentDestination = currentDestination?.destination?.route,
            isLandscape = isLandscape
        ) {
            azSettings(
                // displayAppNameInHeader = true, // Set to true to display the app name instead of the icon
                packRailButtons = false,
                isLoading = isLoading,
                defaultShape = AzButtonShape.RECTANGLE, // Set a default shape for all rail items
                enableRailDragging = true, // Enable the draggable rail feature
                headerIconShape = AzHeaderIconShape.ROUNDED // Set the header icon shape to ROUNDED
            )

            // A standard menu item - only appears in the expanded menu
            azMenuItem(id = "home", text = "Home", route = "home")

            // A menu item with multi-line text
            azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line")

            // A rail item with the default shape (RECTANGLE)
            azRailItem(id = "favorites", text = "Favorites", route = "favorites")

            // A disabled rail item that overrides the default shape
            azRailItem(
                id = "profile",
                text = "Profile",
                shape = AzButtonShape.CIRCLE,
                disabled = true,
                route = "profile"
            )

            azDivider()

            // A rail toggle item with the SQUARE shape
            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                shape = AzButtonShape.SQUARE,
                route = "online",
                onClick = { isOnline = !isOnline }
            )

            // A menu toggle item
            azMenuToggle(
                id = "dark-mode",
                isChecked = isDarkMode,
                toggleOnText = "Dark Mode",
                toggleOffText = "Light Mode",
                route = "dark-mode",
                onClick = { isDarkMode = !isDarkMode }
            )

            azDivider()

            // A rail cycler with a disabled option
            azRailCycler(
                id = "rail-cycler",
                options = railCycleOptions,
                selectedOption = railSelectedOption,
                disabledOptions = listOf("C"),
                route = "rail-cycler",
                onClick = {
                    val currentIndex = railCycleOptions.indexOf(railSelectedOption)
                    val nextIndex = (currentIndex + 1) % railCycleOptions.size
                    railSelectedOption = railCycleOptions[nextIndex]
                }
            )

            // A menu cycler
            azMenuCycler(
                id = "menu-cycler",
                options = menuCycleOptions,
                selectedOption = menuSelectedOption,
                route = "menu-cycler",
                onClick = {
                    val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                    val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                    menuSelectedOption = menuCycleOptions[nextIndex]
                }
            )


            // A button to demonstrate the loading state
            azRailItem(id = "loading", text = "Load", route = "loading", onClick = { isLoading = !isLoading })

            azDivider()

            azMenuHostItem(id = "menu-host", text = "Menu Host", route = "menu-host")
            azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1", route = "menu-sub-1")
            azMenuSubItem(id = "menu-sub-2", hostId = "menu-host", text = "Menu Sub 2", route = "menu-sub-2")

            azRailHostItem(id = "rail-host", text = "Rail Host", route = "rail-host")
            azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1", route = "rail-sub-1")
            azMenuSubItem(id = "rail-sub-2", hostId = "rail-host", text = "Menu Sub 2", route = "rail-sub-2")

            azMenuSubToggle(
                id = "sub-toggle",
                hostId = "menu-host",
                isChecked = isDarkMode,
                toggleOnText = "Sub Toggle On",
                toggleOffText = "Sub Toggle Off",
                route = "sub-toggle",
                onClick = { isDarkMode = !isDarkMode }
            )

            azRailSubCycler(
                id = "sub-cycler",
                hostId = "rail-host",
                options = menuCycleOptions,
                selectedOption = menuSelectedOption,
                route = "sub-cycler",
                onClick = {
                    val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                    val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                    menuSelectedOption = menuCycleOptions[nextIndex]
                }
            )
        }

        // Your app's main content goes here
        NavHost(navController = navController, startDestination = "home") {
            composable("home") { Text("Home Screen") }
            composable("multi-line") { Text("Multi-line Screen") }
            composable("favorites") { Text("Favorites Screen") }
            composable("profile") { Text("Profile Screen") }
            composable("online") { Text("Online Screen") }
            composable("dark-mode") { Text("Dark Mode Screen") }
            composable("rail-cycler") { Text("Rail Cycler Screen") }
            composable("menu-cycler") { Text("Menu Cycler Screen") }
            composable("loading") { Text("Loading Screen") }
            composable("menu-host") { Text("Menu Host Screen") }
            composable("menu-sub-1") { Text("Menu Sub 1 Screen") }
            composable("menu-sub-2") { Text("Menu Sub 2 Screen") }
            composable("rail-host") { Text("Rail Host Screen") }
            composable("rail-sub-1") { Text("Rail Sub 1 Screen") }
            composable("rail-sub-2") { Text("Rail Sub 2 Screen") }
            composable("sub-toggle") { Text("Sub Toggle Screen") }
            composable("sub-cycler") { Text("Sub Cycler Screen") }
        }

        // Your app's main content goes here
        Column(modifier = Modifier.padding(16.dp)) {
            AzTextBox(
                modifier = Modifier.padding(bottom = 16.dp),
                hint = "Enter text...",
                onSubmit = { text ->
                    Log.d(TAG, "Submitted text: $text")
                },
                submitButtonContent = {
                    Text("Go")
                }
            )

            NavHost(navController = navController, startDestination = "home") {
                composable("home") { Text("Home Screen") }
                // ... other composable destinations
            }
        }
    }
}
```

### `AzTextBox` and `AzForm`

`AzTextBox` is a text input field. `AzForm` is a container that groups multiple `AzTextBox` fields, managing them as a single entity with one submit button.

#### Features

-   **Multiline Support**: `AzTextBox` can be configured as a multiline input, which will automatically expand vertically as the user types. The clear and submit buttons remain anchored to the bottom right.
-   **Secret / Password Fields**: Text boxes can be set to `secret` mode, which masks the input. In this mode, the clear button is replaced by a reveal icon to temporarily show the password.
-   **Mutual Exclusivity**: A field cannot be `multiline` and `secret` at the same time.
-   **Unified Styling**:
    -   The input text, outline, and all icons (clear, reveal, submit) share the same color, which can be customized.
    -   The background color and opacity for all text boxes and forms can be set globally.
    -   The submit button's background always matches the text box's background.
-   **Intelligent Autocomplete**:
    -   Suggestions appear in a dropdown as the user types.
    -   The dropdown's style is clean: no outlines or separators, with a background that alternates between 90% and 80% opacity for each suggestion.
    -   Suggestions are sorted by recency, showing the most recently used matching entries first.
-   **`AzForm` Component**:
    -   Group multiple text fields into a single form with a shared submit button.
    -   Each field within the form has its own clear or reveal button.
    -   Styling (outline, background) is applied consistently to all fields within the form.
-   **Disabled State**: Both `AzTextBox` and `AzForm` entries support an `enabled` parameter. When disabled, the input is non-interactive and visual elements are dimmed.

#### Usage

Here is an example of how to use the standalone `AzTextBox` for multiline and secret inputs:

```kotlin
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzTextBoxDefaults

// In your main Activity or a central setup location:
AzTextBoxDefaults.setSuggestionLimit(3) // Show up to 3 suggestions
AzTextBoxDefaults.setBackgroundColor(Color.LightGray) // Set a global background color
AzTextBoxDefaults.setBackgroundOpacity(0.5f) // Set a global background opacity

// Uncontrolled (internal state management)
AzTextBox(
    modifier = Modifier.padding(16.dp),
    hint = "Enter text...",
    onSubmit = { text ->
        // Handle the submitted text
    },
    submitButtonContent = {
        Text("Go")
    }
)

// Controlled (hoisted state management)
var text by remember { mutableStateOf("") }
AzTextBox(
    modifier = Modifier.padding(16.dp),
    value = text,
    onValueChange = { text = it },
    hint = "Enter text...",
    onSubmit = {
        // Handle the submitted text
    },
    submitButtonContent = {
        Text("Go")
    }
)

// Multiline Text Box
AzTextBox(
    modifier = Modifier.padding(16.dp),
    hint = "Enter multiple lines of text...",
    multiline = true,
    onSubmit = { text ->
        // Handle the submitted text
    },
    submitButtonContent = {
        Text("Submit")
    }
)

// Secret Text Box
AzTextBox(
    modifier = Modifier.padding(16.dp),
    hint = "Enter password...",
    secret = true,
    onSubmit = { password ->
        // Handle the submitted password
    },
    submitButtonContent = {
        Text("Go")
    }
)

// Disabled Text Box
AzTextBox(
    modifier = Modifier.padding(16.dp),
    hint = "Cannot edit...",
    enabled = false,
    onSubmit = { /* No-op */ }
)
```

Here is an example of the `AzForm` component:

```kotlin
import com.hereliesaz.aznavrail.AzForm

AzForm(
    formName = "loginForm",
    onSubmit = { formData ->
        // formData is a map of entryName to value
        val username = formData["username"]
        val password = formData["password"]
    }
) {
    entry(entryName = "username", hint = "Username")
    entry(entryName = "password", hint = "Password", secret = true)
    entry(entryName = "bio", hint = "Biography", multiline = true)
    entry(entryName = "readonly", hint = "Read only", enabled = false)
}
```

The `onSubmit` callback provides a map where keys are the entry names defined in `entry()`, and values are the user's input.

### Standalone Buttons

The `AzButton` component (and `AzToggle`, `AzCycler`) can be used independently of the rail.

```kotlin
AzButton(
    onClick = { /* ... */ },
    text = "Save",
    shape = AzButtonShape.RECTANGLE,
    enabled = true, // Can be disabled
    isLoading = false, // Shows loading spinner without resizing button
    contentPadding = PaddingValues(16.dp) // Custom padding
)
```

- **`enabled`**: Disables interaction and dims the button.
- **`isLoading`**: Replaces the text with a loading animation (`AzLoad`). The button maintains its original size, and the animation is allowed to overflow if necessary.
- **`contentPadding`**: Allows customizing the internal padding of the button (defaults to 8dp for rail buttons, but can be overridden for standalone use).

### Hierarchical Navigation

`AzNavRail` supports hierarchical navigation with host and sub-items. This allows you to create nested menus that are easy to navigate.

-   **Host Items**: These are top-level items that can contain sub-items. They can be placed in the rail or the menu.
-   **Sub-Items**: These are nested items that are only visible when their host item is expanded. They can also be placed in the rail or the menu.

### Draggable Rail (FAB Mode)

The rail can be detached and moved around the screen by long-pressing the header icon, which activates "FAB Mode". To enable this feature, set `enableRailDragging = true` in the `azSettings` block.

- **Activation**: Long-press the header (app icon or name) to undock the rail and enter FAB mode. A vertical swipe on the rail will also activate it. Haptic feedback confirms activation/deactivation.
- **Appearance**: In FAB mode, the rail collapses into a floating action button (FAB) displaying the app icon. If the app name was displayed, it transforms into the icon.
- **Interaction**:
    - **Tap**: Tapping the FAB unfolds the rail items downwards. Tapping it again folds them back up. The menu is not available in FAB mode.
    - **Drag**: The FAB can be dragged anywhere on the screen, but is constrained to stay within the top and bottom 10% of the screen. If the rail items are unfolded, they will automatically fold up when a drag begins and unfold when it ends.
- **Deactivation**:
    - **Snapping**: Drag the FAB close to its original docked position to snap it back into place, exiting FAB mode.
    - **Long Press**: Long-pressing the FAB will also immediately re-dock the rail.

### System Overlay

AzNavRail can function as a system-wide overlay (using `SYSTEM_ALERT_WINDOW`). This allows users to access the navigation menu from anywhere on their device.

#### 1. Create an Overlay Service

Extend `AzNavRailOverlayService` to create a service that renders the overlay content.

**OverlayService.kt:**
```kotlin
class OverlayService : AzNavRailOverlayService() {

    override fun getNotification(): Notification {
        // Create and return a persistent notification for the foreground service
        // ...
    }

    @Composable
    override fun OverlayContent() {
        // Wrap content in your theme
        MyApplicationTheme {
             AzNavRail(
                 // ...
             ) {
                 azSettings(
                     enableRailDragging = true,
                     onUndock = { stopSelf() }, // Close overlay on undock
                     onOverlayDrag = { x, y -> updatePosition(x, y) } // Handle window movement
                 )
                 // ... add items
             }
        }
    }
}
```

#### 2. Configure Manifest

Declare the service and required permissions in `AndroidManifest.xml`.

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<!-- For Android 14+ -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>

<application ...>
    <service
        android:name=".OverlayService"
        android:foregroundServiceType="specialUse">
        <property android:name="android.app.property.FOREGROUND_SERVICE_TYPE_SPECIAL_USE_DESCRIPTION"
                  android:value="Overlay for navigation"/>
    </service>
</application>
```

#### 3. Launch the Overlay

You can launch the overlay automatically by providing the service class to `azSettings`, or handle it manually via `onUndock`.

**Option A: Automatic Launch**
```kotlin
AzNavRail {
    azSettings(
        overlayService = OverlayService::class.java
    )
    // ...
}
```
*Note: The library will attempt to launch the service. You must ensure `Settings.canDrawOverlays(context)` is true before this happens, or the launch will fail.*

**Option B: Manual Launch (Recommended)**
Use `onUndock` to handle permission checks and service launching.

```kotlin
AzNavRail {
    azSettings(
        onUndock = {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, OverlayService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } else {
                // Request permission
            }
        }
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
