# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An M3 Expressive and highly configurable navigation rail/menu for Jetpack Compose.

This component provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Separated Data Models:** Define your menu items (`MenuItem`) and rail buttons (`RailItem`) separately for maximum flexibility.
-   **Rich Item Types:** Both menu and rail items can be simple actions, toggles, or cycle buttons, with their state managed automatically.
-   **Always Circular Buttons:** The rail buttons are guaranteed to be perfect circles, regardless of screen orientation.
-   **Auto-Sizing Text:** Text inside the rail buttons automatically shrinks to fit, preventing ugly text wrapping.
-   **Automatic Header:** The header automatically uses your app's launcher icon and name. The only choice is which one to display.
-   **Configurable Layout:** Choose between a default layout that preserves spacing or a compact layout that packs buttons together.
-   **Intuitive Gestures:** Swipe-to-open and swipe-to-close gestures are enabled by default for a fluid user experience.
-   **Simplified Setup:** A new `AppNavRail` composable provides a streamlined API for easy implementation.

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

2.  Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:2.0") // Or the latest version
}
```

## Usage

Using the new `AppNavRail` component is incredibly simple. Here’s a basic example:

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem
import com.hereliesaz.aznavrail.ui.AppNavRail

@Composable
fun MyAwesomeScreen() {
    // 1. Define the items for the expanded menu
    val menuItems = listOf(
        MenuItem.MenuAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { /* ... */ }),
        MenuItem.MenuAction(id = "favorites", text = "Favorites", icon = Icons.Default.Favorite, onClick = { /* ... */ }),
        MenuItem.MenuAction(id = "settings", text = "Settings", icon = Icons.Default.Settings, onClick = { /* ... */ })
    )

    // 2. Define which menu items should also appear on the rail
    val railItems = listOf(
        RailItem.RailAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { /* ... */ }),
        RailItem.RailAction(id = "favorites", text = "Favs", icon = Icons.Default.Favorite, onClick = { /* ... */ })
    )

    // 3. Add the AppNavRail to your layout
    Row {
        AppNavRail(
            menuItems = menuItems,
            railItems = railItems
            // displayAppNameInHeader = true, // Uncomment to show app name instead of icon
            // packRailButtons = true,      // Uncomment to remove gaps between rail buttons
        )
        // The rest of your app's content goes here
        Text("Main content area")
    }
}
```

## API Reference

### `AppNavRail` Composable

This is the recommended entry point for using the library.

| Parameter                | Type                  | Description                                                                                                                                                             |
|--------------------------|-----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `menuItems`              | `List<MenuItem>`      | The list of items to display in the expanded menu drawer. This list also defines the layout order for the rail buttons. See [MenuItem].                                     |
| `railItems`              | `List<RailItem>`      | The list of items to display as circular buttons on the collapsed rail. See [RailItem].                                                                                |
| `displayAppNameInHeader` | `Boolean`             | (Optional) If `true`, the header will display the application's name. If `false` (the default), it will display the application's launcher icon.                             |
| `packRailButtons`        | `Boolean`             | (Optional) If `true`, the rail buttons will be packed together at the top. If `false` (the default), they will be spaced out to align with their menu item counterparts. |

### Data Models

-   **`MenuItem`**: Represents an item in the expanded menu. Can be a `MenuAction`, `MenuToggle`, or `MenuCycle`.
-   **`RailItem`**: Represents a button on the collapsed rail. Can be a `RailAction`, `RailToggle`, or `RailCycle`.

For more detailed information on every parameter and component, refer to the KDoc documentation in the source code.
