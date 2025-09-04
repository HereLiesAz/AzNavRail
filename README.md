# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An M3 Expressive and highly configurable navigation rail/menu for Jetpack Compose.

This component provides a vertical navigation rail that can be expanded to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

## Features

-   **Separated Data Models:** Define your menu items (`MenuItem`) and rail buttons (`RailItem`) separately for maximum flexibility.
-   **Rich Item Types:** Both menu and rail items can be simple actions, toggles, or cycle buttons, with their state managed automatically.
-   **Always Circular Buttons:** The rail buttons are guaranteed to be perfect circles, regardless of screen orientation.
-   **Auto-Sizing Text:** Text inside the rail buttons automatically shrinks to fit, preventing ugly text wrapping.
-   **Expandable & Collapsible:** A compact rail that expands into a full menu with a simple click or swipe.
-   **Intuitive Gestures:** Swipe-to-open and swipe-to-close gestures are enabled by default for a fluid user experience.
-   **Simplified Setup:** A new `AppNavRail` composable provides a streamlined API for easy implementation.

## Setup

1.  Add JitPack to your `settings.gradle.kts`:

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
        MenuItem.MenuToggle(id = "online", text = "Online Status", icon = Icons.Default.Cloud, isChecked = true, onCheckedChange = { /* ... */ }),
        MenuItem.MenuAction(id = "settings", text = "Settings", icon = Icons.Default.Settings, onClick = { /* ... */ })
    )

    // 2. Define the items for the collapsed rail
    val railItems = listOf(
        RailItem.RailAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = { /* ... */ }),
        RailItem.RailToggle(id = "online", text = "Online", icon = Icons.Default.Cloud, isChecked = true, onCheckedChange = { /* ... */ })
    )

    // 3. Add the AppNavRail to your layout
    Row {
        AppNavRail(
            headerText = "My Awesome App",
            headerIcon = Icons.Default.AcUnit,
            menuItems = menuItems,
            railItems = railItems
        )
        // The rest of your app's content goes here
        Text("Main content area")
    }
}
```

## API Reference

### `AppNavRail` Composable

This is the recommended entry point for using the library.

| Parameter     | Type                  | Description                                                                          |
|---------------|-----------------------|--------------------------------------------------------------------------------------|
| `headerText`  | `String`              | The text to display in the header, typically the app name.                           |
| `headerIcon`  | `ImageVector?`        | The icon to display in the header. If null, a default menu icon will be used.        |
| `menuItems`   | `List<MenuItem>`      | The list of items to display in the expanded menu drawer. See [MenuItem].            |
| `railItems`   | `List<RailItem>`      | The list of items to display as circular buttons on the collapsed rail. See [RailItem].|
| `footerItems` | `List<MenuItem>`      | (Optional) A list of items to display in the footer of the expanded menu.            |

### Data Models

-   **`MenuItem`**: Represents an item in the expanded menu. Can be a `MenuAction`, `MenuToggle`, or `MenuCycle`.
-   **`RailItem`**: Represents a button on the collapsed rail. Can be a `RailAction`, `RailToggle`, or `RailCycle`.

For more detailed information on every parameter and component, refer to the KDoc documentation in the source code.
