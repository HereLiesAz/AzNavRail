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
- **AzHostActivityLayout**: A layout container that enforces strict safe zones and automatic alignment rules.
- **AzNavHost**: A wrapper around `androidx.navigation.compose.NavHost` for seamless integration.
- **Smart Transitions**: `AzNavHost` automatically configures directional transitions (slide in/out) based on the docking side (e.g., standard LTR or mirrored for Right dock).

## AzNavRail for Android (Jetpack Compose)

### Setup

To use this library, add JitPack to your `settings.gradle.kts`:


    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            mavenCentral()
            maven { url = uri("https://jitpack.io") }
        }
    }
    


And add the dependency to your app's `build.gradle.kts`:


    dependencies {
        implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest version
    }
    

### Usage

**⚠️ STRICT USAGE PROTOCOL**

`AzNavRail` **MUST** be used within an `AzHostActivityLayout` container. The library enforces strict layout rules (safe zones, padding, z-ordering) and will throw a runtime error (or display a red warning screen) if `AzNavRail` is instantiated directly without a host wrapper.

Do **NOT** use `Scaffold`. Use `AzHostActivityLayout` as your root.

**The Golden Sample:**


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
                displayAppName = true
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


### AzHostActivityLayout Configuration

`AzHostActivityLayout` accepts several parameters to customize its behavior:

* **`navController`**: The `NavHostController` to use. **Required.**
* **`currentDestination`**: Explicitly set the current route. If null, it is automatically derived from the `navController`.
* **`isLandscape`**: Explicitly set the orientation. If null, it is automatically derived from the screen configuration.
* **`initiallyExpanded`**: Set to `true` to have the rail expanded by default (e.g., for bubble activities).
* **`disableSwipeToOpen`**: Set to `true` to disable the swipe gesture that opens the menu.

### AzHostActivityLayout Layout Rules

`AzHostActivityLayout` enforces a "Strict Mode" layout system:

1.  **Rail Avoidance**: No content in the `onscreen` block will overlap the rail. Padding is automatically applied based on the docking side.
2.  **Vertical Safe Zones**: Content is restricted from the top 20% and bottom 10% of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

**Example: Setting a Background**



    AzHostActivityLayout(navController = navController) {
        // This map will fill the entire screen, ignoring safe zones.
        background(weight = 0) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )
        }
    
        // Your UI content goes here, respecting safe zones.
        onscreen(Alignment.TopEnd) {
            Text("Map Overlay")
        }
    
        // ... rail items ...
    }
    


### Smart Transitions with AzNavHost

The `AzNavHost` wrapper provides seamless integration with the `AzHostActivityLayout`:

1.  **Automatic Navigation Controller**: It automatically retrieves the `navController` provided to `AzHostActivityLayout`, eliminating the need to pass it again.
2.  **Directional Transitions**: It automatically configures entry and exit animations based on the rail's docking side:
    * **Left Dock**: New screens slide in from the **Right**; old screens slide out to the **Left** (towards the rail).
    * **Right Dock**: New screens slide in from the **Left**; old screens slide out to the **Right** (towards the rail).

### Info Screen (Help Mode)

`AzNavRail` includes an interactive "Info Screen" mode, ideal for onboarding or help sections.

- **Activation**: Set `infoScreen = true` in `azAdvanced`.
- **Behavior**:
    - **Visual Guides**: Drawn arrows connect description text to the corresponding rail items.
    - **Coordinates**: The overlay displays the on-screen coordinates of each item in the description, aiding in debugging and layout verification.
    - **Independent Scrolling**: Both the description list and the rail are independently scrollable. Arrows update dynamically to maintain the connection.
    - **Interactivity**: Normal navigation items are disabled and greyed out. However, **Host Items** remain interactive, allowing users to expand and collapse sub-menus to view help for nested items.
    - **Content**: If an item has an `info` string, it is displayed in the scrollable list.
- **Exit**: A Floating Action Button (FAB) appears in the bottom-right corner to exit the mode. You must handle the `onDismissInfoScreen` callback in `azAdvanced` to set `infoScreen = false`.



    var showHelp by remember { mutableStateOf(false) }
    
    AzNavRail(...) {
        azAdvanced(
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


Here is an example of the `AzForm` component:



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


The `onSubmit` callback provides a map where keys are the entry names defined in `entry()`, and values are the user's input.

### AzLoad Animation

The `AzLoad` component provides a loading animation. It can be used as a full-screen overlay managed by `AzNavRail` or as a standalone component.

#### Full-Screen Overlay

To show a loading animation in the middle of the screen (overlaying the rail and content), use the `isLoading` parameter in `azAdvanced`.



    AzNavRail(...) {
        azAdvanced(
            isLoading = true // Shows the AzLoad animation in the center of the screen
            // ...
        )
    }


This renders the animation in a non-focusable `Popup`, ensuring it appears on top of other UI elements.

#### Standalone Usage

You can also use `AzLoad` directly in your composables. It is a composable function that renders the animation. To center it, place it within a container with appropriate alignment (e.g., `Box` with `contentAlignment = Alignment.Center`).



    // In any Composable
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AzLoad()
    }


### Standalone Buttons

The `AzButton` component (and `AzToggle`, `AzCycler`) can be used independently of the rail.



    AzButton(
        onClick = { /* ... */ },
        text = "Save",
        modifier = Modifier.fillMaxWidth(), // Now supports modifiers
        shape = AzButtonShape.RECTANGLE,
        enabled = true, // Can be disabled
        isLoading = false, // Shows loading spinner without resizing button
        contentPadding = PaddingValues(16.dp) // Custom padding
    )
    


- **`modifier`**: Supports `Modifier` for layout customization (e.g., `weight`, `fillMaxWidth`).
- **`enabled`**: Disables interaction and dims the button.
- **`isLoading`**: Replaces the text with a loading animation (`AzLoad`). The button maintains its original size, and the animation is allowed to overflow if necessary.
- **`contentPadding`**: Allows customizing the internal padding of the button (defaults to 8dp for rail buttons, but can be overridden for standalone use).

### AzRoller

The `AzRoller` component is a versatile dropdown that behaves like a slot machine but also supports typing and filtering. It extends the functionality of `AzTextBox` with a unique split-click interaction model.



    AzRoller(
        options = listOf("Cherry", "Bell", "Bar"),
        selectedOption = "Cherry",
        // Or use selectedIndex for index-based selection
        // selectedIndex = 0,
        onOptionSelected = { /* handle selection (String) */ },
        hint = "Select Item",
        enabled = true,
        isError = false
    )
    


- **Split Interaction**:
    - **Left Click**: Activates text edit mode for typing and filtering.
    - **Right Click**: Opens the dropdown in "Slot Machine" mode for browsing.
- **Slot Machine Experience**: The dropdown list visually overlaps the input field, allowing users to "scroll" items into the selection slot. Items snap into place.
- **Typing Support**: Users can type to filter or find options, or enter a value not present in the list. As you type, the dropdown automatically filters to show only matching options. The list automatically manages transparency to ensure the input is visible while typing.
- **Dropdown Reset**: Clicking the dropdown arrow while typing exits "Text Mode" and re-opens the full list in "Slot Machine" mode.
- **Index Support**: Gracefully handles `selectedIndex` in addition to `selectedOption`.
- **Styling**: Uses `AzTextBox` as its core, ensuring consistent styling (outlines, colors, errors).
- **Validation**: Propagates `isError` state to the underlying `AzTextBox` for visual feedback.

### Hierarchical Navigation

`AzNavRail` supports hierarchical navigation with host and sub-items. This allows you to create nested menus that are easy to navigate.

-   **Host Items**: These are top-level items that can contain sub-items. They can be placed in the rail or the menu.
-   **Sub-Items**: These are nested items that are only visible when their host item is expanded. They can also be placed in the rail or the menu.
-   **Exclusive Expansion**: Only one host item can be expanded at a time. Expanding a host item automatically collapses any other open host items.

### Draggable Rail (FAB Mode)

The rail can be detached and moved around the screen by long-pressing the header icon, which activates "FAB Mode". To enable this feature, set `enableRailDragging = true` in the `azAdvanced` block.

- **Activation**: Long-press the header (app icon or name) to undock the rail and enter FAB mode. A vertical swipe on the rail will also activate it. Haptic feedback confirms activation/deactivation.
- **Appearance**: In FAB mode, the rail collapses into a floating action button (FAB) displaying the app icon. If the app name was displayed, it transforms into the icon.
- **Interaction**:
    - **Tap**: Tapping the FAB unfolds the rail items downwards. Tapping it again folds them back up. The menu is not available in FAB mode.
    - **Drag**: The FAB can be dragged anywhere on the screen, but is constrained to stay within the top and bottom 10% of the screen. If the rail items are unfolded, they will automatically fold up when a drag begins and unfold when it ends.
- **Deactivation**:
    - **Snapping**: Drag the FAB close to its original docked position to snap it back into place, exiting FAB mode.
    - **Long Press**: Long-pressing the FAB will also immediately re-dock the rail.

### Reorderable Items (AzRailRelocItem)

`AzRailRelocItem` is a specialized sub-item that users can reorder via drag-and-drop. This feature is supported on Android, Web, and React Native.



    azRailRelocItem(
        id = "reloc-1",
        hostId = "host-1",
        text = "Item 1",
        onRelocate = { from, to, newOrder ->
            // Handle new order (List<String>)
        }
    ) {
        // Hidden Menu (Tap to select -> Tap again to open)
        listItem("Action 1") { /* ... */ }
        inputItem("Rename") { newName -> /* ... */ }
    }


- **Drag-and-Drop**: Long-press (triggers a vibration) and drag an item to move it. Other items will animate to create an empty slot at the potential drop target.
- **Cluster Constraints**: Items can only be moved within their "cluster" — a contiguous group of relocation items under the same host. They cannot jump over standard items or move to a different host.
- **Hidden Menu**: Tapping the item brings it into focus (selects it). Tapping the item *again* while it is already focused opens the contextual menu. This menu supports:
    - **List Items**: Standard clickable actions.
    - **Input Items**: Text input fields (e.g., for renaming).
- **Callbacks**:
    - `onClick`: Triggered on the first tap (selection).
    - `onRelocate`: Called when a drag operation completes. Provides the `fromIndex`, `toIndex`, and the complete `newOrder` list of item IDs.

### System Overlay

AzNavRail can function as a system-wide overlay (using `SYSTEM_ALERT_WINDOW`). This allows users to access the navigation menu from anywhere on their device.

#### Features

* **Dynamic Resizing**: The overlay window automatically expands to fill the screen during drag operations (for smooth movement) and shrinks to wrap its content when stationary (to unblock the underlying screen).
* **Automatic Expansion**: When dropped, the rail items automatically expand.
* **Automatic Activity Launching**: Clicking a navigation item in the overlay automatically brings the main application to the foreground and navigates to the associated route.
* **Exclusive Host Expansion**: Expanding a host item collapses all other host items, ensuring the overlay size remains manageable.
* **Footer Color Enforcement**: The footer text color matches the primary or first item color for consistency.

#### 1. Create an Overlay Service

You have two options for creating an overlay service:

**Option A: Foreground Service (Recommended for persistence)**

Extend `AzNavRailOverlayService` to create a foreground service that renders the overlay content. This is more resilient to being killed by the system.

**OverlayService.kt:**


    class OverlayService : AzNavRailOverlayService() {
    
        override fun getNotification(): Notification {
            // Create and return a persistent notification for the foreground service
            // ...
        }
    
        @Composable
        override fun OverlayContent() {
            // Wrap content in your theme
            MyApplicationTheme {
                 // Simply render AzNavRail. The service automatically manages
                 // window resizing and drag events via an internal controller.
                 AzNavRail(
                     // ...
                 ) {
                     azAdvanced(
                         enableRailDragging = true,
                         onUndock = { stopSelf() } // Close overlay on undock
                     )
                     azTheme(...)
                     azConfig(...)
                     // ... add items
                 }
            }
        }
    }


**Option B: Basic Service (Simpler setup)**

`AzHostActivityLayout` enforces a "Strict Mode" layout system:

1.  **Rail Avoidance**: No content in the `onscreen` block will overlap the rail. Padding is automatically applied based on the docking side.
2.  **Vertical Safe Zones**: Content is restricted from the top 20% and bottom 10% of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

[API Reference](/API.md)

[Full DSL](/DSL.md)

[Project Structure](/PROJECT_STRUCTURE.md)

## Documentation

The library includes a comprehensive **Complete Guide** (`AZNAVRAIL_COMPLETE_GUIDE.md`) containing:
* Full Getting Started instructions.
* Complete API and DSL references.
* Layout rules and best practices.
* Complete Sample App source code.

### Auto-Generate Documentation

To automatically extract this guide into your project's `docs/` folder whenever you build, add the following task to your app's `build.gradle.kts`:



    // In app/build.gradle.kts
    
    tasks.register("updateAzNavDocs") {
        group = "documentation"
        description = "Extracts AzNavRail documentation from the dependency."
    
        doLast {
            // Find the AzNavRail AAR in the runtime classpath
            val artifact = configurations.getByName("debugRuntimeClasspath").files
                .find { it.name.contains("AzNavRail") && it.extension == "aar" }
    
            if (artifact != null) {
                copy {
                    from(zipTree(artifact))
                    include("assets/AZNAVRAIL_COMPLETE_GUIDE.md")
                    into(layout.projectDirectory.dir("docs"))
                    // Remove the 'assets/' prefix from the output file
                    eachFile {
                        path = name
                    }
                    includeEmptyDirs = false
                }
                println("AzNavRail documentation updated: docs/AZNAVRAIL_COMPLETE_GUIDE.md")
            } else {
                println("AzNavRail AAR not found. Make sure the dependency is added.")
            }
        }
    }
    
    // Optional: Run this task automatically before every build
    // tasks.named("preBuild") { dependsOn("updateAzNavDocs") }


Once added, run `./gradlew updateAzNavDocs` (or just build your app if you uncommented the last line) to generate the documentation.

## License


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
