# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

An M3 Expressive and dictatorially confined if not contemptable navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

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
    implementation("com.github.HereLiesAz:AzNavRail:4.0") // Or the latest version
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

## API Reference

The main entry point is the `AzNavRail` composable.

`@Composable fun AzNavRail(content: @Composable AzNavRailScope.() -> Unit)`

You declare items and configure the rail within its content lambda. The available functions are:

-   `azSettings(displayAppNameInHeader, packRailButtons)`
-   `azMenuItem(id, text, onClick)`
-   `azRailItem(id, text, color, onClick)`
-   `azMenuToggle(id, text, isChecked, onClick)`
-   `azRailToggle(id, text, color, isChecked, onClick)`
-   `azMenuCycler(id, text, options, selectedOption, onClick)`
-   `azRailCycler(id, text, color, options, selectedOption, onClick)`

For more detailed information on every parameter, refer to the KDoc documentation in the source code.
