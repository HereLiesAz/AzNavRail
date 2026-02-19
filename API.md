# AzNavRail API Reference

This document serves as the technical reference for the AzNavRail library. The facade has been stripped. The machine expects strict obedience to the high-inference architecture.

* **[1. High-Inference API](#1-high-inference-api-az)**: The annotation system used to dictate structure to the compiler.
* **[2. The Configuration Duality (Scope API)](#2-the-configuration-duality-scope-api)**: The backend API used to inject runtime configurations.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

The primary interface for the library is the `@Az` annotation. It is a monolithic container that accepts specific context objects. The processor interprets your intent based on whether the annotated target is a `@Composable` function (Screen), a standard function (Action), or a property (State).

### `com.hereliesaz.aznavrail.annotation.Az`

**Target:** `FUNCTION`, `CLASS`, `PROPERTY`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `app` | `App` | Configuration for the `MainActivity` (Global State). |
| `advanced`| `Advanced` | Binds catastrophic system overlays and physical dimensions. |
| `rail` | `RailItem` | Defines a standard navigation destination or transient action. |
| `menu` | `MenuItem` | Defines an item visible *only* in the expanded footer menu. |
| `host` | `RailHost` | Defines a parent item that expands inline. |
| `nested` | `NestedRail` | Defines a popup menu structure. |
| `toggle` | `Toggle` | Defines a state toggle button. |
| `cycler` | `Cycler` | Defines a multi-option cycle button. |
| `reloc` | `RelocItem` | Defines a draggable, reorderable item with hidden menus. |
| `background`| `Background`| Plunges content beneath the safe zones based on weight. |
| `divider` | `Divider` | Injects visual silence into the list. |

### Context Classes

#### `App` (Class Level)
Configures the global application state. Aesthetics like colors have been abolished.
* `dock`: `AzDockingSide` (`LEFT` or `RIGHT`).
* `packButtons`: `Boolean`.
* `noMenu`: `Boolean`.
* `vibrate`: `Boolean`.
* `displayAppName`: `Boolean`.
* `usePhysicalDocking`: `Boolean`.
* `showFooter`: `Boolean`.
* `expandedWidth` / `collapsedWidth`: `Int`. (Injected as `Dp` by the compiler).
* `activeClassifiers`: `Array<String>`.

#### `Advanced` (Class Level)
* `isLoading`, `infoScreen`, `enableRailDragging`: `Boolean`.
* `overlayServiceClass`: `String` (Fully qualified class name).
* Callback Binders (`String`): `onUndock`, `onRailDrag`, `onOverlayDrag`, `onItemGloballyPositioned`. (Provide the name of the function to bind).

#### `RailItem` / `MenuItem` / `NestedRail`
* `id`, `text`, `parent`: Structural routing.
* `icon`: `Int` (Drawable Res ID).
* `disabled`: `Boolean`.
* `screenTitle`, `info`: `String`.
* `classifiers`: `Array<String>`.
* `onFocus`: `String` (Name of the function to bind).

#### `Toggle` & `Cycler`
* `toggleOnText`, `toggleOffText`: `String`.
* `options`, `disabledOptions`: `Array<String>`.
* **State Binding**: If you annotate a `var` property with these, the generated code will automatically read and mutate the state.

---

## 2. The Configuration Duality (Scope API)

When extending `AzActivity`, you may optionally override `AzNavRailScope.configureRail()` to inject dynamic parameters. `azSettings` was executed for treason. `azTheme` was purged for masquerading as a structural component. Use your app's `MaterialTheme`.

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
    usePhysicalDocking: Boolean = false,
    expandedWidth: Dp = 130.dp,
    collapsedWidth: Dp = 80.dp,
    showFooter: Boolean = true
)
~~~

### `azAdvanced`
Controls systemic overrides and overlays.

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

---

## 3. UI Components

Standalone components designed to be used **inside** your `@Composable` screens.

### `AzTextBox`
A high-ground text input field with support for autocomplete, password masking, and multiline text.

~~~kotlin
@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false,
    isError: Boolean = false,
    historyContext: String? = null,
    onSubmit: (String) -> Unit
)
~~~

### `AzRoller`
A "Slot Machine" style dropdown. Left click to type/filter, right click to spin the barrel.

~~~kotlin
@Composable
fun AzRoller(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    hint: String = "Select...",
    enabled: Boolean = true
)
~~~

### `AzButton`
A circular or shaped button that automatically resizes text to fit.

~~~kotlin
@Composable
fun AzButton(
    onClick: () -> Unit,
    text: String,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    isLoading: Boolean = false
)
~~~

### `AzForm`
A container for grouping multiple `AzTextBox` fields with a single submit button.

~~~kotlin
@Composable
fun AzForm(
    formName: String,
    onSubmit: (Map<String, String>) -> Unit,
    content: AzFormScope.() -> Unit
)
~~~

---

## 4. Low-Level API (Manual)

**⚠️ Warning:** The KSP processor writes this code. You are observing the matrix. Do not touch it unless you are prepared for reality to collapse.

### `AzActivity`
The base class for your Activity.
* `abstract val graph: AzGraphInterface`: Point this to the generated `AzGraph`.
* `open fun AzNavRailScope.configureRail()`: Override to inject runtime configuration via `azConfig`, and `azAdvanced`.

### `AzHostActivityLayout`
The strict layout container. Throws `FatalLayoutViolation` if safe zones are breached.

### `AzNavHost`
A wrapper around Jetpack Navigation's `NavHost` that automatically applies directional transitions based on the configured docking side.
