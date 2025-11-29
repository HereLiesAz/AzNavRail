# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

It is NOT highly configurable. It is exactly what I want it to be. Its only benefits are the simplicity and extreme conciseness of the implementation code.

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
- **Bubbles**: System-wide overlay support.
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

### System Overlay (Bubbles)

AzNavRail can function as a system-wide overlay using the Android Bubble API. This allows users to access the navigation menu from anywhere on their device.

#### 1. Setup Bubble Activity

Create an Activity that will serve as the content of the bubble. This activity should display your `AzNavRail`. You can simply set `bubbleMode = true` in `azSettings` to configure it correctly (`initiallyExpanded = true` and `enableRailDragging = false`).

**AndroidManifest.xml:**
```xml
<activity
    android:name=".BubbleActivity"
    android:label="My App Bubble"
    android:allowEmbedded="true"
    android:documentLaunchMode="always"
    android:resizeableActivity="true" />
```

**BubbleActivity.kt:**
```kotlin
class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                // Reuse your screen content, ensuring it's configured for bubble mode
                SampleScreen(
                    bubbleMode = true,              // Enable bubble mode
                    onUndockOverride = { finish() } // Undock in bubble closes it
                )
            }
        }
    }
}
```

#### 2. Launch the Bubble

Use the `bubbleTargetActivity` parameter in your main activity's `AzNavRail` settings to trigger the bubble notification automatically. This overrides the default internal floating mode.

```kotlin
AzNavRail {
    azSettings(
        // Enable rail dragging so the Undock button appears
        enableRailDragging = true,
        // Provide the class of the Activity to launch as a bubble
        bubbleTargetActivity = BubbleActivity::class.java
    )
    // ...
}
```

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
-   **`initiallyExpanded`**: Whether the navigation rail is expanded by default. Useful for Bubble activities.
-   **`disableSwipeToOpen`**: Whether to disable the swipe-to-open gesture.
-   **`content`**: The DSL content for the navigation rail.

#### `AzNavRailScope`

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

-   `azSettings(displayAppNameInHeader: Boolean, packRailButtons: Boolean, expandedRailWidth: Dp, collapsedRailWidth: Dp, showFooter: Boolean, isLoading: Boolean, defaultShape: AzButtonShape, enableRailDragging: Boolean, headerIconShape: AzHeaderIconShape, onUndock: (() -> Unit)?, bubbleMode: Boolean, bubbleTargetActivity: Class<*>?)`: Configures the settings for the `AzNavRail`. `headerIconShape` can be `CIRCLE` (default), `ROUNDED`, or `NONE` (no clipping). `onUndock` allows overriding the default undock behavior. `bubbleMode` (default `false`) simplifies bubble configuration by setting `enableRailDragging` to `false` and ensuring the rail is initially expanded. `bubbleTargetActivity` allows specifying the Activity to launch as a bubble when undocked.
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

#### `AzTextBox`

A text input field with autocomplete.

```kotlin
@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    historyContext: String? = null,
    submitButtonContent: (@Composable () -> Unit)? = null,
    onSubmit: (String) -> Unit
)
```

-   **`modifier`**: The modifier to be applied to the text box.
-   **`value`**: The input text to be shown in the text field. Use `null` for an uncontrolled component.
-   **`onValueChange`**: The callback that is triggered when the input value updates. Use `null` for an uncontrolled component.
-   **`hint`**: The hint text to display when the input is empty.
-   **`outlined`**: Whether the text box has an outline. The submit button's outline will be the inverse of this value.
-   **`multiline`**: Enables multiline input, which expands the text box vertically.
-   **`secret`**: Masks the input for password fields and replaces the clear button with a reveal icon.
-   **`outlineColor`**: Sets the color for the outline, input text, and all icons.
-   **`historyContext`**: An optional string to provide a unique context for the autocomplete history. If provided, suggestions will be drawn only from entries saved with the same context.
-   **`submitButtonContent`**: A composable lambda for the content of the submit button.
-   **`onSubmit`**: A callback that is invoked when the submit button is clicked, providing the current text.

#### `AzForm`

A composable for creating a form with multiple `AzTextBox` fields.

```kotlin
@Composable
fun AzForm(
    formName: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    onSubmit: (Map<String, String>) -> Unit,
    submitButtonContent: @Composable () -> Unit = { Text("Submit") },
    content: AzFormScope.() -> Unit
)
```

-   **`formName`**: A unique name for the form. This name is used as the `historyContext` for all text fields within the form, ensuring that autocomplete suggestions are namespaced and relevant to this form only.
-   **`modifier`**: The modifier to be applied to the form.
-   **`outlined`**: Whether the text fields in the form have an outline. The submit button's outline will be the inverse of this value.
-   **`outlineColor`**: Sets the color for the outline, input text, and all icons for all fields in the form.
-   **`onSubmit`**: A callback that is invoked when the form's submit button is clicked, providing a map of the form data.
-   **`submitButtonContent`**: A composable lambda for the content of the submit button.
-   **`content`**: The DSL content for the form, where you define the `entry` fields.

#### `AzFormScope`

-   `entry(entryName: String, hint: String, multiline: Boolean, secret: Boolean)`: Adds a text field to the form.

#### `AzTextBoxDefaults`

An object for configuring global `AzTextBox` and `AzForm` settings.

-   `setSuggestionLimit(limit: Int)`: Sets the maximum number of autocomplete suggestions to display (0-5) and the corresponding history storage limit in kilobytes.
-   `setBackgroundColor(color: Color)`: Sets the global background color for all text boxes and forms.
-   `setBackgroundOpacity(opacity: Float)`: Sets the global background opacity for all text boxes and forms.

#### `AzButton`

A circular, text-only button with auto-sizing text.

```kotlin
@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`onClick`**: A lambda to be executed when the button is clicked.
-   **`text`**: The text to display on the button.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`shape`**: The shape of the button.

#### `AzToggle`

A toggle button that displays different text for its on and off states.

```kotlin
@Composable
fun AzToggle(
    isChecked: Boolean,
    onToggle: () -> Unit,
    toggleOnText: String,
    toggleOffText: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`isChecked`**: Whether the toggle is in the "on" state.
-   **`onToggle`**: The callback to be invoked when the button is toggled.
-   **`toggleOnText`**: The text to display when the toggle is on.
-   **`toggleOffText`**: The text to display when the toggle is off.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`shape`**: The shape of the button.

#### `AzCycler`

A button that cycles through a list of options when clicked, with a delayed action.

```kotlin
@Composable
fun AzCycler(
    options: List<String>,
    selectedOption: String,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    shape: AzButtonShape = AzButtonShape.CIRCLE
)
```

-   **`options`**: The list of options to cycle through.
-   **`selectedOption`**: The currently selected option from the view model.
-   **`onCycle`**: The callback to be invoked for the final selected option after a 1-second delay.
-   **`modifier`**: The modifier to be applied to the button.
-   **`color`**: The color of the button's border and text.
-   **`shape`**: The shape of the button.

#### `AzNavRailScope`

For the full DSL reference, see [DSL.md](DSL.md).

## AzNavRail for Web (React)

`aznavrail-web` is a React component that provides a Material Design-style navigation rail.

### Features

All item functions are overloaded to support `route`-based navigation.

## AzNavRail for React Native

`aznavrail-react-native` provides the navigation rail for React Native applications.

### Installation

```bash
npm install aznavrail-react-native
# or
yarn add aznavrail-react-native
```

### Usage

```tsx
import { AzNavRail } from 'aznavrail-react-native';
import { AzRailItem, AzMenuItem } from 'aznavrail-react-native';

<AzNavRail>
  <AzRailItem id="home" text="Home" onClick={() => {}} />
  <AzMenuItem id="settings" text="Settings" onClick={() => {}} />
</AzNavRail>
```

## Project Structure

This project is a monorepo containing the following packages:

-   `aznavrail`: The core Android library.
-   `SampleApp`: A sample Android application that demonstrates how to use the `aznavrail` library.
-   `aznavrail-web`: The web version of the `aznavrail` library.

### File Dictionary

This document provides a brief but thorough description of what each file in the project is supposed to do.

#### Root Directory

| File/Directory | Description |
| --- | --- |
| `.github/` | Contains GitHub Actions workflows for CI/CD. |
| `SampleApp/` | An Android application that demonstrates how to use the `aznavrail` library. |
| `aznavrail/` | The core `aznavrail` Android library module. |
| `aznavrail-web/` | The web version of the `aznavrail` library. |
| `gradle/` | Contains the Gradle wrapper files. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `AGENTS.md` | Provides instructions for AI agents working with the codebase. |
| `README.md` | The main README file for the project. |
| `build.gradle.kts` | The main build script for the project. |
| `gradle.properties` | Project-wide Gradle settings. |
| `gradlew` | The Gradle wrapper script for Unix-based systems. |
| `gradlew.bat` | The Gradle wrapper script for Windows. |
| `settings.gradle.kts` | The settings script for the project. |

#### .github Directory

| File/Directory | Description |
| --- | --- |
| `workflows/` | Contains the GitHub Actions workflow files. |

##### workflows

| File | Description |
| --- | --- |
| `npm-publish.yml` | This workflow builds, tests, and publishes the `aznavrail-web` package to npm. |
| `push.yml` | This workflow builds and tests the project on every push and pull request. |

#### SampleApp Directory

The `SampleApp` directory contains an Android application that demonstrates how to use the `aznavrail` library.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `SampleApp` module. |
| `src/main/AndroidManifest.xml` | The manifest file for the `SampleApp`. |
| `src/main/java/com/example/sampleapp/BubbleActivity.kt` | An activity configured to run as an Android Bubble, demonstrating overlay capabilities. |
| `src/main/java/com/example/sampleapp/MainActivity.kt` | The main activity of the `SampleApp`, which contains the sample code for the `aznavrail` library. |
| `src/main/res/` | Contains the resources for the `SampleApp`. |

#### aznavrail Directory

The `aznavrail` directory contains the core `aznavrail` Android library module.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `aznavrail` module. |
| `consumer-rules.pro` | ProGuard rules for consumers of the library. |
| `src/` | Contains the source code for the `aznavrail` library. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt` | The main `AzNavRail` composable and logic. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailScope.kt` | The DSL scope definition for `AzNavRail`. |
| `src/main/java/com/hereliesaz/aznavrail/AzTextBox.kt` | The `AzTextBox` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzForm.kt` | The `AzForm` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzButton.kt` | The standalone `AzButton` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailButton.kt` | The button component used within the rail. |
| `src/main/java/com/hereliesaz/aznavrail/AzLoad.kt` | The loading animation component. |
| `src/main/java/com/hereliesaz/aznavrail/AzDivider.kt` | The divider component. |

#### aznavrail-web Directory

The `aznavrail-web` directory contains the web version of the `aznavrail` library.

| File/Directory | Description |
| --- | --- |
| `public/` | Contains public assets that are served directly. |
| `src/` | Contains the source code for the `aznavrail-web` library. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `README.md` | The README file for the `aznavrail-web` library. |
| `eslint.config.js` | ESLint configuration file. |
| `index.html` | The main HTML file for the web application. |
| `package-lock.json` | Records the exact version of each dependency. |
| `package.json` | Lists the project dependencies and scripts. |
| `vite.config.js` | Vite configuration file. |

#### gradle Directory

The `gradle` directory contains the Gradle wrapper files, which allow the project to be built with a specific version of Gradle without having to install it system-wide.

## Contributing

Contributions are NOT welcome. They are at least not asked for.

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
