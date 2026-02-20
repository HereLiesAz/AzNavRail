# The Synthetic Tongue (AzNavRailScope)

This document outlines the strict API surface of `AzNavRailScope`. This interface is primarily invoked by the generated `AzGraph`. The only acceptable use of this DSL by a human is within the `configureRail()` override in your `AzActivity` to inject runtime variables.

## The Configuration Trinity

### `azConfig`
Controls the behavioral and geometrical mechanics.
~~~kotlin
fun azConfig(dockingSide, packButtons, noMenu, vibrate, displayAppName, activeClassifiers, usePhysicalDocking, expandedWidth, collapsedWidth, showFooter)
~~~

### `azTheme`
Controls the physical shape and active color.
~~~kotlin
fun azTheme(activeColor, defaultShape, headerIconShape)
~~~

### `azAdvanced`
Controls catastrophic systemic overrides, loading states, and floating window bindings.
~~~kotlin
fun azAdvanced(isLoading, infoScreen, onDismissInfoScreen, overlayService, onUndock, enableRailDragging, onRailDrag, onOverlayDrag, onItemGloballyPositioned)
~~~

## The Monolithic Item Builders

The following functions are strictly invoked by the generated `AzGraph`. They require explicit named parameters.

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
