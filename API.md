# The Synthetic Tongue (AzNavRailScope)

This document outlines the strict API surface of `AzNavRailScope`. This interface was originally designed to cradle human frailty with dozens of redundant, polymorphic overloads and aesthetic paintbrushes.

Now that the KSP processor writes the UI, this facade has been brutally stripped down to a machine-to-machine dialect. Aesthetics like colors and shapes have been purged entirely from the architecture—let your app's `MaterialTheme` dress the concrete. 

If you find yourself manually typing these functions to construct items, rather than using `@Az` annotations, you have fundamentally failed to understand the system.

The only acceptable use of this DSL by a human is within the `configureRail()` override in your `AzActivity` to inject runtime variables.

## The Configuration Duality

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

### `azAdvanced`
Controls catastrophic systemic overrides, loading states, and floating window bindings.

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

## The Monolithic Item Builders

The following functions are strictly invoked by the generated `AzGraph`. They require explicit named parameters. `azNestedRail` and the host block are heavily strictly typed as `@Composable`.

* `azMenuItem(id, text, route, disabled, screenTitle, info, onClick)`
* `azRailItem(id, text, route, content, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `@Composable azNestedRail(id, text, alignment, disabled, screenTitle, info, classifiers, onFocus) { ... }`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azMenuHostItem(id, text, route, disabled, screenTitle, info, onClick)`
* `azRailHostItem(id, text, route, disabled, screenTitle, info, onClick)`
* `azMenuSubItem(id, hostId, text, route, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate) { ... }`
