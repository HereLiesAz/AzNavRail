# The Synthetic Tongue (AzNavRailScope)

This document outlines the strict API surface of `AzNavRailScope`. This interface was originally designed to cradle human frailty with dozens of redundant, polymorphic overloads.

Now that the KSP processor writes the UI, this facade has been brutally stripped down to a machine-to-machine dialect. If you find yourself manually typing these functions to construct items, rather than using `@Az` annotations, you have fundamentally failed to understand the architecture.

The only acceptable use of this DSL by a human is within the `configureRail()` override in your `AzActivity` to inject runtime aesthetics via `azTheme`, `azConfig`, and `azAdvanced`.

## The Configuration Trinity

### `azTheme`
Controls the visual rendering constraints of the rail.

~~~kotlin
fun azTheme(
    activeColor: Color? = null,
    defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
    headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
    expandedWidth: Dp = 130.dp,
    collapsedWidth: Dp = 80.dp,
    showFooter: Boolean = true
)
~~~

### `azConfig`
Controls the behavioral and interactive mechanics.

~~~kotlin
fun azConfig(
    dockingSide: AzDockingSide = AzDockingSide.LEFT,
    packButtons: Boolean = false,
    noMenu: Boolean = false,
    vibrate: Boolean = false,
    displayAppName: Boolean = false,
    activeClassifiers: Set<String> = emptySet(),
    usePhysicalDocking: Boolean = false
)
~~~

### `azAdvanced`
Controls catastrophic systemic overrides and overlays.

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

The following functions are primarily invoked by the generated `AzGraph`. They require explicit named parameters. Overloads have been eradicated.

* `azMenuItem(id, text, route, disabled, screenTitle, info, onClick)`
* `azRailItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azNestedRail(id, text, alignment, color, shape, disabled, screenTitle, info, classifiers, onFocus, content)`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azMenuHostItem(id, text, route, disabled, screenTitle, info, onClick)`
* `azRailHostItem(id, text, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubItem(id, hostId, text, route, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, color, isChecked, toggleOnText, toggleOffText, shape, route, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, color, options, selectedOption, shape, route, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, hiddenMenu)`
