# The Synthetic Tongue (AzNavRailScope)

This document outlines the API surface of `AzNavRailScope`. This interface defines the DSL used to configure the rail, whether manually or via the generated code.

## The Live Dictatorship (v7.25)

The **v7.25 KSP Processor** injects reactive logic. When using `@Az` annotations with property bindings (e.g., `textProperty = "myTitle"`), the generated code no longer passes hardcoded literals. Instead, it accesses your `AzActivity` instance directly.

Because these properties are ideally `mutableStateOf`, the entire `AzGraph` will **recompose** automatically when your Activity state changes.

## Configuration & Theme

### `azConfig`
Controls behavioral and geometrical mechanics.

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
Controls the visual style of the rail.

~~~kotlin
fun azTheme(
    activeColor: Color = Color.Unspecified,
    defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
    headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
)
~~~

### `azAdvanced`
Controls system overrides, loading states, and floating window bindings.

~~~kotlin
fun azAdvanced(
    isLoading: Boolean = false,
    helpEnabled: Boolean = false,
    onDismissHelp: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onUndock: (() -> Unit)? = null,
    enableRailDragging: Boolean = false,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null
)
~~~

## Item Builders

The following functions are used to define the rail structure.

* `azMenuItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azHelpRailItem(id, text, content, color, shape)`
* `azRailItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `@Composable azNestedRail(id, text, route, content, color, shape, alignment, disabled, screenTitle, info, classifiers, onFocus) { ... }`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azMenuHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate) { ... }`
