# AzNavRail API Reference (v7.25: The Live Dictatorship)

This document serves as the technical reference for the AzNavRail library. Version 7.25 introduces **Dynamic Reactive Binding**, allowing the annotation-driven architecture to react to runtime state changes in the host Activity.

* **[1. High-Inference API](#1-high-inference-api-az)**: The annotation system used to dictate structure and reactive bindings.
* **[2. The Configuration Duality (Scope API)](#2-the-configuration-duality-scope-api)**: The backend API used to inject runtime configurations.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

The primary interface for the library is the `@Az` annotation. In v7.25, annotations now support `*Property` strings which bind directly to properties in your `AzActivity` (ideally `mutableStateOf`).

### `com.hereliesaz.aznavrail.annotation.Az`

**Target:** `FUNCTION`, `CLASS`, `PROPERTY`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `app` | `App` | Configuration for the `MainActivity` (Global State). |
| `advanced`| `Advanced` | Binds system overlays and loading states. |
| `rail` | `RailItem` | Defines a navigation destination or action. |
| `menu` | `MenuItem` | Defines an item visible only in the expanded footer menu. |
| `host` | `RailHost` | Defines a parent item that expands inline. |
| `nested` | `NestedRail` | Defines a popup menu structure. |
| `toggle` | `Toggle` | Defines a state toggle button. |
| `cycler` | `Cycler` | Defines a multi-option cycle button. |
| `reloc` | `RelocItem` | Defines a draggable, reorderable item with hidden menus. |
| `background`| `Background`| Plunges content beneath the safe zones based on weight. |
| `divider` | `Divider` | Injects visual silence into the list. |

### Context Classes & Reactive Bindings

All items now support the following **Reactive Binding Fields**. Provide the name of a property in your Activity as a `String`.

#### `RailItem` / `MenuItem` / `NestedRail` / `RelocItem`
* `textProperty`: Binds the item's label to a property.
* `iconTextProperty`: Binds the badge/icon text (e.g., notification count).
* `visibleProperty`: Binds visibility to a `Boolean` property. Generated code uses `if (instance.property)`.
* `disabledProperty`: Binds the disabled state to a `Boolean` property.
* `onFocus`: Bind a function name to be called when the item is focused.

#### `Toggle`
* `isCheckedProperty`: **Mandatory for reactive toggles.** Binds to a `Boolean` property. The generated `onClick` will automatically toggle this property.

#### `Cycler`
* `optionsProperty`: Binds to a `List<String>` property for dynamic options.
* `selectedOptionProperty`: Binds to a `String` property. The generated `onClick` will cycle through `optionsProperty`.
* `disabledOptionsProperty`: Binds to a `List<String>` property to disable specific options.

#### `Advanced` (Class Level)
* `isLoadingProperty`: Binds the global loading spinner to a `Boolean` property.
* `helpEnabled`: If true, binds to a property of the same name to show/hide the help overlay.

#### `App` (Class Level)
* `dock`: `String` ("LEFT" or "RIGHT").
* `packButtons`, `noMenu`, `vibrate`, `displayAppName`, `usePhysicalDocking`, `showFooter`: `Boolean`.
* `expandedWidth` / `collapsedWidth`: `Int`.
* `activeClassifiers`: `Array<String>`.

---

## 2. The Configuration Duality (Scope API)

When extending `AzActivity`, you may optionally override `AzNavRailScope.configureRail()` to inject dynamic parameters.

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

---

## 3. UI Components

Standalone components designed to be used **inside** your `@Composable` screens.

### `AzTextBox`
A high-ground text input field with support for autocomplete, password masking, and multiline text.

### `AzToggle`
A standalone toggle switch matching the rail's aesthetic.

### `AzButton`
A circular or shaped button that automatically resizes text to fit.

---

## 4. Low-Level API (Manual)

### `AzActivity`
The base class for your Activity.
* `abstract val graph: AzGraphInterface`: Point this to the generated `AzGraph`.
* `open fun AzNavRailScope.configureRail()`: Override to inject runtime configuration.

### `AzGraph` (Generated)
The KSP processor generates this object. It casts the `activity` to your specific instance type to access the bound properties, ensuring **Recomposition** occurs whenever your `mutableStateOf` properties change.
