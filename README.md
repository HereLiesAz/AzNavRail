# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be highly configurable while providing sensible defaults that work out-of-the-box.

## Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Scrollable Content**: Both the rail and the expanded menu are scrollable.
- **Text-Only DSL API**: A simple, declarative API for building your navigation.
- **Multi-line Menu Items**: Supports multi-line text with automatic indentation.
- **Stateless and Observable**: Hoist and manage state in your own composables.
-   **Customizable Shapes**: Choose from `CIRCLE`, `SQUARE`, `RECTANGLE`, or `NONE` for rail buttons.
-   **Uniform Rectangle Width**: All rectangular buttons automatically share the same width, determined by the widest rectangle.
- **Smart Collapse Behavior**: Menu items intelligently collapse the rail after interactions.
- **Delayed Cycler Action**: Cycler items have a built-in delay to prevent accidental triggers.
- **Customizable Colors**: Apply custom colors to individual rail buttons.
- **Dividers**: Easily add dividers to your menu.
- **Automatic Header**: Displays your app's icon or name by default.
- **Configurable Layout**: Pack buttons together or preserve spacing.
- **Disabled State**: Disable any item, including individual cycler options.
- **Loading State**: Show a loading animation over the rail.
- **Standalone Components**: Use `AzButton`, `AzToggle`, `AzCycler`, and `AzDivider` on their own.
- **Jetpack Navigation Integration**: Seamlessly integrate with Jetpack Navigation using the `navController`.
- **Hierarchical Navigation**: Create nested menus with host and sub-items.
- **Draggable Rail (FAB Mode)**: The rail can be detached and moved around the screen.
- **Auto-sizing Text**: Text within rail buttons automatically resizes to fit without wrapping, unless a newline character is explicitly used.
- **Toggle and Cycler Items**: `azRailToggle` and `azMenuToggle` provide a simple way to manage boolean states, while `azRailCycler` and `azMenuCycler` allow cycling through a list of options.
- **Gesture Control**: Intuitive swipe and tap gestures for expanding, collapsing, and activating FAB mode.

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
                enableRailDragging = true // Enable the draggable rail feature
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
    }
}
```

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

### API Reference

#### `AzNavRail`

The main composable for the navigation rail.

```kotlin
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the navigation rail.
-   **`navController`**: An optional `NavController` to enable integration with Jetpack Navigation.
-   **`currentDestination`**: The route of the current destination, used to highlight the active item.
-   **`isLandscape`**: A boolean to indicate if the device is in landscape mode.
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

#### `AzNavRailScope`

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean, expandedRailWidth: Dp, collapsedRailWidth: Dp, showFooter: Boolean, isLoading: Boolean, defaultShape: AzButtonShape, enableRailDragging: Boolean)`: Configures the settings for the `AzNavRail`.
-   `azMenuItem(id: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tpping it executes the action and collapses the rail.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azDivider()`: Adds a horizontal divider to the expanded menu.
-   **`screenTitle`**: An optional parameter for all `azMenuItem`, `azRailItem`, `azMenuToggle`, `azRailToggle`, `azMenuCycler`, and `azRailCycler` functions that displays a title on the screen when the item is selected. The title will be **force-aligned to the right** of the screen. If no `screenTitle` is provided, the item's `text` will be used instead. To prevent a title from being displayed, use `screenTitle = AzNavRail.noTitle`.
-    **`route`**: An optional parameter for all `azMenuItem`, `azRailItem`, `azMenuToggle`, `azRailToggle`, `azMenuCycler`, and `azRailCycler` functions that specifies the route to navigate to when the item is clicked. This is used for integration with Jetpack Navigation.
-   `azMenuHostItem(id: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a host item that only appears in the expanded menu.
-   `azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a host item that only appears in the expanded menu.
-   `azRailHostItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a host item that appears in both the collapsed rail and the expanded menu.
-   `azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a host item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a sub item that only appears in the expanded menu.
-   `azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a sub item that only appears in the expanded menu.
-   `azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a sub item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a sub item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle sub-item that only appears in the expanded menu.
-   `azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle sub-item that only appears in the expanded menu.
-   `azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, onClick: () -> Unit)`: Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler sub-item that only appears in the expanded menu.
-   `azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler sub-item that only appears in the expanded menu.
-   `azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.

## AzNavRail for Web (React)

`aznavrail-web` is a React component that provides a Material Design-style navigation rail, inspired by its counterpart in the Android ecosystem. It is designed to be a "batteries-included" solution, offering a highly configurable and easy-to-use navigation component for web applications.

### Features

All item functions are overloaded to support `route`-based navigation. The `route` parameter is a `String` that represents the destination in the navigation graph.

-   **`screenTitle`**: An optional parameter for all item functions that displays a title on the screen when the item is selected. The title will be **force-aligned to the right** of the screen. If no `screenTitle` is provided, the item's `text` will be used instead. To prevent a title from being displayed, use `screenTitle = AzNavRail.noTitle`.

## AzNavRail for Web (React)

(This section remains unchanged as no modifications were made to the React component)

## Project Structure

This project is a monorepo containing the following packages:

-   `aznavrail`: The core Android library.
-   `SampleApp`: A sample Android application that demonstrates how to use the `aznavrail` library.
-   `aznavrail-web`: The web version of the `aznavrail` library.

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.

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
