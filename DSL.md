# The Synthetic Tongue (AzNavRailScope)

This document outlines the strict API surface of `AzNavRailScope`. This interface was originally designed to cradle human frailty with dozens of redundant, polymorphic overloads and aesthetic paintbrushes.

Now that the KSP processor writes the UI, this facade has been brutally stripped down to a machine-to-machine dialect. **Aesthetics like colors and shapes have been purged entirely from the architecture**—let your app's `MaterialTheme` dress the concrete. 

## The Live Dictatorship (v7.25)

While the `AzNavRailScope` remains a rigid series of instructions, the **v7.25 KSP Processor** now injects reactive logic. When using `@Az` annotations with property bindings (e.g., `textProperty = "myTitle"`), the generated code no longer passes hardcoded literals. Instead, it accesses your `AzActivity` instance directly.

Because these properties are ideally `mutableStateOf`, the entire `AzGraph` will **recompose** automatically when your Activity state changes. The "Dictatorship" is no longer static; it is alive.

## The Configuration Duality

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

The following functions are strictly invoked by the generated `AzGraph`.

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
