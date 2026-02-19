# AzNavRail API Reference

This document serves as the technical reference for the AzNavRail library.

* **[1. High-Inference API](#1-high-inference-api-az)**: The annotation system used to configure the app.
* **[2. The Configuration Trinity (Scope API)](#2-the-configuration-trinity-scope-api)**: The backend API used to inject runtime configurations.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

The primary interface for the library is the `@Az` annotation. It is a container that accepts specific context objects.

### `com.hereliesaz.aznavrail.annotation.Az`

**Target:** `FUNCTION`, `CLASS`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `app` | `App` | Configuration for the `MainActivity` (State). |
| `rail` | `RailItem` | Defines a standard navigation destination. |
| `railHost` | `RailHost` | Defines a parent item that expands inline. |
| `nested` | `NestedRail` | Defines a popup menu structure. |
| `toggle` | `Toggle` | Defines a state toggle button. |
| `cycler` | `Cycler` | Defines a multi-option cycle button. |
| `reloc` | `RelocItem` | Defines a draggable, reorderable item. |
| `menu` | `MenuItem` | Defines an item visible *only* in the expanded menu. |

### Context Classes

#### `App` (Class Level)
Configures the global application state.
* `dock`: `AzDockingSide` (`LEFT` or `RIGHT`). Default: `LEFT`.
* `theme`: `AzTheme` (`GlitchNoir`, etc.). Default: `GlitchNoir`.
* `secure`: `Boolean`. If `true`, strict safe zones are enforced. Default: `true`.

#### `RailItem` (Function Level)
* `id`: `String`. Unique identifier. *Inferred from function name if empty.*
* `text`: `String`. Display label. *Inferred from function name if empty.*
* `icon`: `Int` (Drawable Res ID). Default: `0` (Uses default shape).
* `parent`: `String`. ID of the host item (if this is a sub-item).
* `home`: `Boolean`. Set `true` if this is the start destination.

#### `RailHost` (Property Level)
* `id`: `String`. *Inferred from property name.*
* `text`: `String`. *Inferred from property name.*
* `icon`: `Int`.

#### `Toggle` (Function Level)
* `id`: `String`.
* `onText`: `String`. Text displayed when state is `true`.
* `offText`: `String`. Text displayed when state is `false`.
* `default`: `Boolean`. Initial state.
* `slot`: `AzSlot` (`RAIL` or `MENU`). Where the item appears.

---

## 2. The Configuration Trinity (Scope API)

When extending `AzActivity`, you may optionally override `AzNavRailScope.configureRail()` to inject dynamic or aesthetic parameters into the generated layout. `azSettings` has been deprecated and split into three strict domains.

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

---

## 3. UI Components

These components are designed to be used **inside** your `@Composable` screens. They adhere to the Az aesthetic.

### `AzTextBox`
A high-ground text input field with support for autocomplete, password masking, and multiline text.

~~~kotlin
@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String? = null, // Null = Internal State
    onValueChange: ((String) -> Unit)? = null,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false, // Masks text, shows "Reveal" icon
    isError: Boolean = false,
    historyContext: String? = null, // ID for autocomplete history
    onSubmit: (String) -> Unit
)
~~~

### `AzRoller`
A "Slot Machine" style dropdown.
* **Left Click**: Type to filter options.
* **Right Click**: Open full list to scroll (slot machine style).

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
    isLoading: Boolean = false // Replaces text with spinner
)
~~~

### `AzForm`
A container for grouping multiple `AzTextBox` fields with a single submit button.

~~~kotlin
@Composable
fun AzForm(
    formName: String, // Namespace for history
    onSubmit: (Map<String, String>) -> Unit,
    content: AzFormScope.() -> Unit
)
~~~
**Scope Usage:** `entry(entryName = "username", hint = "User")`

---

## 4. Low-Level API (Manual)

**⚠️ Warning:** These components are automatically generated by the High-Inference system. Use them directly only if you are bypassing the standard architecture.

### `AzActivity`
The base class for your Activity.
* `abstract val graph: AzGraphInterface`: You must override this to point to the generated `AzGraph`.
* `open fun AzNavRailScope.configureRail()`: Override to inject runtime configuration via `azTheme`, `azConfig`, and `azAdvanced`.

### `AzHostActivityLayout`
The strict layout container. Throws `FatalLayoutViolation` if safe zones are breached or if used incorrectly.

~~~kotlin
@Composable
fun AzHostActivityLayout(
    navController: NavHostController,
    onscreen: @Composable () -> Unit,
    content: AzNavRailScope.() -> Unit
)
~~~

### `AzNavHost`
A wrapper around Jetpack Navigation's `NavHost`.
* Automatically applies directional transitions based on the configured docking side.
* Automatically links to the parent `AzHostActivityLayout` controller.

~~~kotlin
@Composable
fun AzNavHost(
    startDestination: String,
    builder: NavGraphBuilder.() -> Unit
)
~~~

### `AzNavRailOverlayService`
Abstract `Service` class for creating system-wide floating windows.
* **`OverlayContent()`**: The `@Composable` entry point for your overlay UI.
* **`stopSelf()`**: Call this to close the overlay.
