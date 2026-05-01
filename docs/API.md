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
* `helpList`: An optional mapping of `RailItem` IDs to help texts to be shown in the help overlay alongside `info`.

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
A high-ground text input field with support for autocomplete, password masking, multiline text, and pre-filled initial values.

### `AzForm`
A form builder component for managing and grouping multiple `AzTextBox` fields together. Form entries now strictly enforce and securely support initial pre-filled values (`initialValue`) across platform variants.

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

---

## 5. Tutorial Framework API

The tutorial framework provides scripted multi-scene tutorials with a dimmed overlay, item spotlights, four advance conditions, variable-driven scene branching, TapTarget item branching, checklist cards, media cards, and cross-platform read-state persistence.

---

### `AzAdvanceCondition`

Controls how the tutorial advances from one card to the next.

**Android (sealed class):**

| Variant | Description |
| :--- | :--- |
| `AzAdvanceCondition.Button` | Default. A "Next" button is shown; user taps it to advance. |
| `AzAdvanceCondition.TapTarget` | User must tap the highlighted item. Degrades to TapAnywhere if highlight is not `AzHighlight.Item`. |
| `AzAdvanceCondition.TapAnywhere` | User taps anywhere on screen to advance. |
| `AzAdvanceCondition.Event(name: String)` | Advances when the app calls `controller.fireEvent(name)`. |

**TypeScript (discriminated union):**

| Literal form | Description |
| :--- | :--- |
| `{ type: 'Button' }` | Next button (default). |
| `{ type: 'TapTarget' }` | Tap the highlighted item. |
| `{ type: 'TapAnywhere' }` | Tap anywhere. |
| `{ type: 'Event', name: string }` | App fires the named event. |

---

### `AzHighlight`

Describes what the overlay should spotlight.

**Android (sealed class):**

| Variant | Description |
| :--- | :--- |
| `AzHighlight.None` | No spotlight; overlay dims the whole screen uniformly. |
| `AzHighlight.FullScreen` | Spotlight covers the entire screen (no punch-out). |
| `AzHighlight.Item(id: String)` | Spotlights the rail item with the given ID using its measured bounds. |
| `AzHighlight.Area(rect: Rect)` | Spotlights an arbitrary rect (dp coordinates). |

**TypeScript:**

| Literal form | Description |
| :--- | :--- |
| `{ type: 'None' }` | No spotlight. |
| `{ type: 'FullScreen' }` | Full-screen spotlight. |
| `{ type: 'Item', id: string }` | Spotlights the named item. |
| `{ type: 'Area', rect: DpRect }` | Spotlights an arbitrary rect. |

Card auto-positioning: defaults to bottom. Flips to top when the spotlight center Y exceeds 60% of screen height.

Web implementation: `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)` on the highlighted element — the CSS equivalent of Android's `BlendMode.Clear` punch-out.

---

### `AzCard`

A single instructional step within a scene.

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `title` | `String` | — | Card heading. |
| `text` | `String` | — | Body text displayed below the title (and below media, if present). |
| `highlight` | `AzHighlight` | `AzHighlight.None` | What to spotlight. |
| `actionText` | `String?` | `null` | Label for the action button (overrides default "Next"). |
| `onAction` | `(() -> Unit)?` | `null` | Additional callback when the action button is tapped. |
| `advanceCondition` | `AzAdvanceCondition` | `Button` | How the card advances. |
| `branches` | `Map<String, String>` | `emptyMap()` | Used with `TapTarget` only: maps item ID → scene ID to branch on tap. |
| `mediaContent` | `(@Composable () -> Unit)?` / `(() => ReactNode)?` | `null` | Content rendered between title and text. Max height 120dp/120px, 8dp/8px corner clip. |
| `checklistItems` | `List<String>` | `emptyList()` | If non-empty, renders a checklist. Next button is disabled until all items are checked. |

---

### `AzScene`

A scripted screen state within a tutorial.

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | — | Unique identifier for this scene. |
| `content` | `@Composable () -> Unit` / `() => ReactNode` | — | The UI rendered behind the overlay during this scene. |
| `cards` | `List<AzCard>` | — | Ordered list of cards to display. |
| `branchVar` | `String?` | `null` | Variable name to evaluate on scene entry for invisible redirect branching. |
| `branches` | `Map<String, String>` | `emptyMap()` | Maps variable value → scene ID. Evaluated on scene entry when `branchVar` is set. Circular branch detection logs a warning and advances to the next scene index (or ends the tutorial if out of bounds). |

---

### `AzTutorial`

A complete tutorial definition.

| Field | Type | Description |
| :--- | :--- | :--- |
| `scenes` | `List<AzScene>` | Ordered list of scenes. |
| `onComplete` | `(() -> Unit)?` | Fired when the last card of the last scene is completed. |
| `onSkip` | `(() -> Unit)?` | Fired when the user taps "Skip Tutorial". |

**Android DSL builder:** `azTutorial { ... }` inside `azAdvanced(tutorials = mapOf(...))`.

---

### `AzTutorialController` (Android)

Manages tutorial state. Created via `rememberAzTutorialController()` and provided via `LocalAzTutorialController`.

**Accessing:**
~~~kotlin
val controller = LocalAzTutorialController.current
// or at the root:
val controller = rememberAzTutorialController()
CompositionLocalProvider(LocalAzTutorialController provides controller) { ... }
~~~

**Persistence:** `SharedPreferences` file `az_tutorial_prefs`, key `az_navrail_read_tutorials`. Read on `rememberAzTutorialController()`, written on each `markTutorialRead()`.

| Member | Description |
| :--- | :--- |
| `val activeTutorialId: State<String?>` | ID of the currently running tutorial, or `null`. |
| `val readTutorials: List<String>` | Tutorial IDs that have been marked as read. |
| `fun startTutorial(id: String, variables: Map<String, String> = emptyMap())` | Starts the tutorial. `variables` drives scene-level branching. |
| `fun endTutorial()` | Ends the active tutorial without triggering `onComplete`. |
| `fun markTutorialRead(id: String)` | Persists the tutorial as read. |
| `fun isTutorialRead(id: String): Boolean` | Returns true if the tutorial has been read. |
| `fun fireEvent(name: String)` | Fires a named event consumed by the next `Event` advance condition. |
| `fun consumeEvent(): String?` | Returns and clears the pending event. Called internally by the overlay. |

**Note on `Saver`:** `AzTutorialController.Saver` is a function that requires a `Context` argument: `AzTutorialController.Saver(context)`. It is not a plain `val`. Most users are unaffected because `rememberAzTutorialController()` handles this internally.

---

### `AzTutorialProvider` / `useAzTutorialController` (React Native)

**Provider:** Wrap your app root with `AzTutorialProvider` (from `@HereLiesAz/aznavrail-react`).

~~~tsx
import { AzTutorialProvider } from '@HereLiesAz/aznavrail-react';

<AzTutorialProvider>
    <App />
</AzTutorialProvider>
~~~

**Hook:**
~~~tsx
const controller = useAzTutorialController();
~~~

**Persistence:** `@react-native-async-storage/async-storage` (optional peer dependency). Falls back to in-memory if the package is not installed. Key: `az_navrail_read_tutorials`.

| Method | Description |
| :--- | :--- |
| `startTutorial(id, variables?)` | Starts the tutorial. `variables` is `Record<string, string>`. |
| `endTutorial()` | Ends the active tutorial. |
| `markTutorialRead(id)` | Persists the tutorial as read. |
| `isTutorialRead(id)` | Returns `true` if read. |
| `fireEvent(name)` | Fires a named event. |
| `consumeEvent()` | Returns and clears the pending event. |
| `activeTutorialId` | `string \| null` — currently active tutorial ID. |
| `pendingEvent` | `string \| null` — event pending consumption. |

---

### `AzWebTutorialProvider` / `useAzWebTutorialController` (Web)

Distinct from the React Native provider. Import from the web library.

**Provider:**
~~~tsx
import { AzWebTutorialProvider } from '@HereLiesAz/aznavrail-web';

<AzWebTutorialProvider>
    <App />
</AzWebTutorialProvider>
~~~

**Hook:**
~~~tsx
const ctrl = useAzWebTutorialController();
~~~

**Persistence:** `localStorage`. Key: `az_navrail_read_tutorials`.

Methods and properties are identical to the React Native controller above.

---

### `AzTutorialOverlay` (Android) / `AzWebTutorialOverlay` (Web)

Renders the active tutorial overlay.

**Android props:**

| Prop | Type | Description |
| :--- | :--- | :--- |
| `tutorialId` | `String` | ID of the tutorial being rendered. |
| `tutorial` | `AzTutorial` | The tutorial definition. |
| `onDismiss` | `() -> Unit` | Called when the tutorial ends (skip or complete). |
| `itemBoundsCache` | `Map<String, Rect>` | Bounds of each rail item collected via `onItemGloballyPositioned`. Required for `AzHighlight.Item`. |

**Web props** (TypeScript): same shape with React/TS types.

The overlay is shown/hidden by checking `controller.activeTutorialId`. Mount it conditionally:
~~~kotlin
if (controller.activeTutorialId.value == "tut-1") {
    AzTutorialOverlay(
        tutorialId = "tut-1",
        tutorial = tutorial,
        onDismiss = { controller.endTutorial() },
        itemBoundsCache = boundsMap
    )
}
~~~

---

### Persistence reference

| Platform | Storage mechanism | File / store | Key |
| :--- | :--- | :--- | :--- |
| Android | `SharedPreferences` | `az_tutorial_prefs` | `az_navrail_read_tutorials` |
| React Native | `AsyncStorage` (or in-memory fallback) | N/A | `az_navrail_read_tutorials` |
| Web | `localStorage` | N/A | `az_navrail_read_tutorials` |
