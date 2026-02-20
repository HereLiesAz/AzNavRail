# AzNavRail API Reference

This document serves as the technical reference for the AzNavRail library. The facade has been stripped. The machine expects strict obedience to the high-inference architecture.

* **[1. High-Inference API](#1-high-inference-api-az)**: The annotation system used to dictate structure to the compiler.
* **[2. The Configuration Duality (Scope API)](#2-the-configuration-duality-scope-api)**: The backend API used to inject runtime configurations.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

The primary interface for the library is the `@Az` annotation. It is a monolithic container that accepts specific context objects. The processor interprets your intent based on whether the annotated target is a `@Composable` function (Screen), a standard function (Action), or a property (State). It is fully instance-aware.

### Context Classes

#### `App` (Class Level)
Configures the global application state.
* `dock`: `AzDockingSide` (`LEFT` or `RIGHT`).
* `packButtons`, `noMenu`, `vibrate`, `displayAppName`, `usePhysicalDocking`, `showFooter`, `initiallyExpanded`, `disableSwipeToOpen`: `Boolean`.
* `expandedWidth` / `collapsedWidth`: `Int`. (Injected as `Dp` by the compiler).
* `activeClassifiers`: `Array<String>`.

#### `Theme` (Class Level)
Configures the geometry and color of the structural components.
* `activeColorHex`: `String` (e.g., `"#FF00FF"`).
* `defaultShape`: `AzButtonShape` (`CIRCLE`, `RECTANGLE`, `ROUNDED_RECTANGLE`).
* `headerIconShape`: `AzHeaderIconShape` (`CIRCLE`, `SQUARE`, `ROUNDED_SQUARE`).

#### `Advanced` (Class Level)
* `isLoading`, `infoScreen`, `enableRailDragging`: `Boolean`.
* `overlayServiceClass`: `String` (Fully qualified class name).
* Callback Binders (`String`): `onUndock`, `onRailDrag`, `onOverlayDrag`, `onItemGloballyPositioned`. (Provide the name of the function to bind).

#### `RailItem` / `MenuItem` / `NestedRail` / `RailHost`
* `id`, `text`, `parent`: Structural routing.
* `icon`: `Int` (Drawable Res ID).
* `iconText`: `String` (Typographic icon, e.g., "42").
* `disabled`: `Boolean`.
* `screenTitle`, `info`: `String`.
* `classifiers`: `Array<String>`.
* `onFocus`: `String` (Name of the function to bind).
* `alignment` (NestedRail only): `AzNestedRailAlignment`.

#### `Toggle` & `Cycler`
* `toggleOnText`, `toggleOffText`: `String`.
* `options`, `disabledOptions`: `Array<String>`.

#### `RelocItem`
* `hiddenMenuRoutes`: `Array<String>` (Routes to navigate to).
* `hiddenMenuActions`: `Array<String>` (Functions to call).
* `hiddenMenuInputs`: `Array<String>` (Functions to receive string inputs).

---

## 2. The Configuration Duality (Scope API)

When extending `AzActivity`, you may optionally override `AzNavRailScope.configureRail()` to inject dynamic parameters. 

### `azConfig`
Controls the behavioral and geometrical mechanics.

~~~kotlin
fun azConfig(dockingSide, packButtons, noMenu, vibrate, displayAppName, activeClassifiers, usePhysicalDocking, expandedWidth, collapsedWidth, showFooter)
~~~

### `azTheme`
Controls dynamic aesthetic adjustments at runtime.

~~~kotlin
fun azTheme(activeColor: Color, defaultShape: AzButtonShape, headerIconShape: AzHeaderIconShape)
~~~

### `azAdvanced`
Controls systemic overrides and overlays.

~~~kotlin
fun azAdvanced(isLoading, infoScreen, onDismissInfoScreen, overlayService, onUndock, enableRailDragging, onRailDrag, onOverlayDrag, onItemGloballyPositioned)
~~~
