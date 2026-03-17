# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

---

## 📚 Documentation

-   **[Complete Guide (The Bible)](/docs/AZNAVRAIL_COMPLETE_GUIDE.md)**: Comprehensive, encyclopedic reference for every feature.
-   **[API Reference](/docs/API.md)**: Javadoc-style listing.
-   **[DSL Reference](/docs/DSL.md)**: Quick look at DSL functions.

---

## 🚀 Setup

Add JitPack to your `settings.gradle.kts`:
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
- **Reorderable Items**: `AzRailRelocItem` allows user drag-and-drop reordering within clusters.
    -   **Drag**: Long press (with vibration feedback) to start dragging.
    -   **Hidden Menu**: Tap to focus/select. If already focused, tap again to open the hidden menu.
- **System Overlay**: System-wide overlay support with automatic resizing and activity launching.
- **Auto-sizing Text**: Text fits without wrapping (unless explicit newline).
- **Toggles/Cyclers**: Simple state management.
- **Gestures**: Swipe/tap to expand, collapse, or undock.
- **`AzTextBox`**: Modern text box with autocomplete and submit button.
- **`AzForm`**: Group multiple text boxes into a single form with a shared submit button.
- **`AzRoller`**: A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
- **Info Screen**: Interactive help mode for onboarding with visual guides and coordinate display.
- **Left/Right Docking**: Position the rail on the left or right side of the screen.
- **No Menu Mode**: Treat all items as rail items, removing the side drawer.
- **AzHostActivityLayout**: A layout container that enforces strict safe zones and automatic alignment rules.
- **AzNavHost**: A wrapper around `androidx.navigation.compose.NavHost` for seamless integration.
- **Smart Transitions**: `AzNavHost` automatically configures directional transitions (slide in/out) based on the docking side (e.g., standard LTR or mirrored for Right dock).
- **Nested Rails**: `azNestedRail` allows for secondary popup rails (Vertical or Horizontal) triggered from a rail item.

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

Add the dependency to your app's `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest release
}
~~~

---

## 🛠️ The Golden Sample

This is the standard, validated way to initialize `AzNavRail`. It **must** be wrapped in `AzHostActivityLayout`.

~~~kotlin
import com.hereliesaz.aznavrail.*

@Composable
fun SampleScreen() {
    val navController = rememberNavController()

    AzHostActivityLayout(
        navController = navController,
        initiallyExpanded = false
    ) {
        // 1. CONFIGURATION
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true, // Tightly pack rail items
            displayAppName = true
        )

        azTheme(activeColor = Color.Cyan)

        // 2. NAVIGATION ITEMS
        azRailItem(id = "home", text = "Home", route = "home", content = Icons.Default.Home)
        azRailItem(id = "profile", text = "Profile", route = "profile")

        // 3. MENU ONLY ITEMS
        azMenuItem(id = "settings", text = "Settings", route = "settings")

        // 4. ONSCREEN CONTENT
        // Use 'onscreen' to define your UI. 
        // Layout rules (safe zones, padding) are enforced automatically.
        onscreen(alignment = Alignment.Center) {
            AzNavHost(startDestination = "home") {
                composable("home") { Text("Home Screen") }
                composable("profile") { Text("Profile Screen") }
                composable("settings") { Text("Settings Screen") }
            }
        }
    }
}
~~~

---

## ✨ Features & Basics

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
2.  **Vertical Safe Zones**: Content is restricted from the top 10% and bottom 10% of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

### Help Overlay (Help Mode)

`AzNavRail` includes an interactive "Help Mode" (formerly Info Screen), ideal for onboarding or help sections.

- **Activation**: Explicitly enable via `helpEnabled = true` in `azAdvanced`.
- **Triggering**:
    - **Auto Menu Item**: A "Help" item is automatically added to the bottom of the drawer menu.
    - **Explicit Trigger**: Use `azHelpRailItem(id, text)` to place a dedicated help button in the rail.
- **Behavior**:
    - **Visual Guides**: Dynamic lines connect interactive info cards to their corresponding rail items.
    - **Interactive Info Cards**: Cards are displayed in a scrollable list. Tap any card to expand it for more detail.
    - **Line Persistence**: Connection lines update in real-time as you scroll through the help cards or the rail.
    - **Contextual Interactivity**: Main navigation items are disabled while the overlay is active. However, **Host Items** remain interactive, allowing users to navigate hierarchical structures within the help context.
- **Exit**: Tap anywhere on the dark background overlay to dismiss the mode.

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

### AzLoad Animation

### 1. Strict Layout System
`AzHostActivityLayout` enforces a "Constitution" for your UI to ensure consistency and usability:
*   **Safe Zones:** Top 10% and Bottom 10% are reserved. Interactive content is pushed to the center 80%.
*   **Automatic Padding:** Content in `onscreen` is automatically padded to avoid the rail, regardless of docking side or rotation.
*   **Backgrounds:** Use `background(weight)` to place content *behind* the rail (e.g., maps).

### 2. Navigation Items
*   **`azRailItem`**: Always visible.
*   **`azMenuItem`**: Visible only in the drawer.
*   **`azNestedRail`**: Opens a secondary popup rail (Vertical/Horizontal).
*   **`azRailRelocItem`**: Draggable items for user reordering.

### 3. Interactive Components
Manage state directly in the rail without leaving the context.
*   **Toggles:** `azRailToggle` / `azMenuToggle`.
*   **Cyclers:** `azRailCycler` (multi-state buttons).

~~~kotlin
AzNavRail(...) {
    azAdvanced(
        isLoading = true // Shows the AzLoad animation in the center of the screen
        // ...
    )
}
~~~

#### Standalone Usage

You can also use `AzLoad` directly in your composables.

### Standalone Buttons

The `AzButton` component (and `AzToggle`, `AzCycler`) can be used independently of the rail.

~~~kotlin
AzButton(
    onClick = { /* ... */ },
    text = "Save",
    modifier = Modifier.fillMaxWidth(), // Now supports modifiers
    shape = AzButtonShape.RECTANGLE,
    enabled = true, // Can be disabled
    isLoading = false, // Shows loading spinner without resizing button
    contentPadding = PaddingValues(16.dp) // Custom padding
)
~~~

### AzRoller

The `AzRoller` component is a versatile dropdown that behaves like a slot machine but also supports typing and filtering. It extends the functionality of `AzTextBox` with a unique split-click interaction model.

~~~kotlin
AzRoller(
    options = listOf("Cherry", "Bell", "Bar"),
    selectedOption = "Cherry",
    onOptionSelected = { /* handle selection (String) */ },
    hint = "Select Item",
    enabled = true,
    isError = false
)
~~~

- **Split Interaction**:
    - **Left Click**: Activates text edit mode for typing and filtering.
    - **Right Click**: Opens the dropdown in "Slot Machine" mode for browsing.
- **Slot Machine Experience**: The dropdown list visually overlaps the input field, allowing users to "scroll" items into the selection slot. Items snap into place.
- **Typing Support**: Users can type to filter or find options, or enter a value not present in the list. As you type, the dropdown automatically filters to show only matching options. The list automatically manages transparency to ensure the input is visible while typing.
- **Dropdown Reset**: Clicking the dropdown arrow while typing exits "Text Mode" and re-opens the full list in "Slot Machine" mode.

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

~~~kotlin
azRailRelocItem(
    id = "reloc-1",
    hostId = "host-1",
    text = "Item 1",
    onRelocate = { from, to, newOrder ->
        // Handle new order (List<String>)
    }
) {
    // Hidden Menu (Tap to select -> Long Press to open)
    listItem("Action 1") { /* ... */ }
    inputItem("Rename") { newName -> /* ... */ }
}
~~~

- **Drag-and-Drop**: Long-press (triggers a vibration) and drag an item to move it. Other items will animate to create an empty slot at the potential drop target.
- **Cluster Constraints**: Items can only be moved within their "cluster" — a contiguous group of relocation items under the same host. They cannot jump over standard items or move to a different host.
- **Hidden Menu**: Tapping the item brings it into focus (selects it). Long-pressing the item *without dragging* opens the contextual menu. This menu supports list items and input fields.

### System Overlay

AzNavRail can function as a system-wide overlay (using `SYSTEM_ALERT_WINDOW`). This allows users to access the navigation menu from anywhere on their device.

#### Features

---
### Theming and Customization

#### Menu Font Size
The expanded menu text font size (and the footer items text size) is strictly controlled by your app's `MaterialTheme.typography.titleLarge`. To adjust the text size inside the side menu drawer, simply customize the `titleLarge` attribute in your app's typography theme!

#### Customizing Item Text and Colors
Navigation items support overriding their display text and colors when shown in the menu versus the rail using `menuText`, `menuToggleOnText`, `menuToggleOffText`, `menuOptions`, `textColor`, and `fillColor` properties!

### Documentation

The library includes a comprehensive **Complete Guide** (`docs/AZNAVRAIL_COMPLETE_GUIDE.md`) containing:
* Full Getting Started instructions.
* Complete API and DSL references.
* Layout rules and best practices.
* Complete Sample App source code.

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
