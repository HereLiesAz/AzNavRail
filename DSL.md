# The AzNavRail DSL

This document outlines how to use the `AzNavRailScope` DSL to configure your navigation rail. This DSL is used within the `AzHostActivityLayout` composable.

## Basic Usage

Wrap your `AzNavHost` (or your own navigation host) with `AzHostActivityLayout`:

```kotlin
AzHostActivityLayout(
    navController = navController,
    content = {
        // Configure behavior
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true,
            displayAppName = true
        )

        // Configure theme
        azTheme(
            activeColor = MaterialTheme.colorScheme.primary,
            defaultShape = AzButtonShape.CIRCLE
        )

        // Add items
        azRailItem(
            id = "home",
            text = "Home",
            route = "home_route",
            content = Icons.Default.Home
        )

        // Add menu items
        azMenuItem(
            id = "settings",
            text = "Settings",
            route = "settings_route"
        )

        // Setup the NavHost
        onscreen {
            AzNavHost(
                startDestination = "home_route",
                navController = navController
            ) {
                composable("home_route") { HomeScreen() }
                composable("settings_route") { SettingsScreen() }
            }
        }
    }
)
```

## Configuration

### `azConfig`
Controls the behavioral and geometrical mechanics.
~~~kotlin
fun azConfig(
    dockingSide: AzDockingSide = AzDockingSide.LEFT,
    packButtons: Boolean = false,
    noMenu: Boolean = false,
    vibrate: Boolean = false,
    displayAppName: Boolean = false,
    activeClassifiers: Set<String> = emptySet(),
    usePhysicalDocking: Boolean = false,
    expandedWidth: Dp = 130.dp,
    collapsedWidth: Dp = 80.dp,
    showFooter: Boolean = true
)
~~~

### `azTheme`
Controls the physical shape and active color.
~~~kotlin
fun azTheme(
    activeColor: Color = Color.Unspecified,
    defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
    headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
)
~~~

### `azAdvanced`
Controls advanced settings, loading states, and overlay bindings.
~~~kotlin
fun azAdvanced(
    isLoading: Boolean = false,
    infoScreen: Boolean = false,
    onDismissInfoScreen: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onUndock: (() -> Unit)? = null,
    enableRailDragging: Boolean = false,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null
)
~~~

## Item Builders

* `azMenuItem(id, text, route, content, disabled, screenTitle, info, onClick)`
* `azRailItem(id, text, route, content, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `@Composable azNestedRail(id, text, route, content, alignment, disabled, screenTitle, info, classifiers, onFocus) { ... }`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azRailRelocItem(id, hostId, text, route, content, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate) { ... }`
