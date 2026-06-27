# AzNavRail API Reference

This document serves as the technical reference for the AzNavRail library. **Dynamic Reactive Binding** lets the annotation-driven architecture react to runtime state changes in the host Activity.

* **[1. High-Inference API](#1-high-inference-api-az)**: The annotation system used to dictate structure and reactive bindings.
* **[2. The Configuration Duality (Scope API)](#2-the-configuration-duality-scope-api)**: The backend API used to inject runtime configurations.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

The primary interface for the library is the `@Az` annotation. Annotations support `*Property` strings which bind directly to properties in your `AzActivity` (ideally `mutableStateOf`).

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
* `helpEnabled`: When `true`, auto-injects a Help item into the menu drawer if no explicit `azHelpRailItem` / `azHelpSubItem` is registered. The overlay itself is toggled exclusively by tapping a help item (there is no public API to open it externally).
* `helpList`: An optional mapping of `RailItem` IDs to help texts to be shown in the help overlay alongside `info`.
* `onInteraction`: Optional callback `((String, AzNavItem) -> Unit)?` invoked whenever any rail item is interacted with (click, toggle, cycler advance, nested rail open, reloc drag, **and host expand/collapse**). Receives the item's ID and the `AzNavItem` itself. Fires for both leaf items (`azRailItem` / `azRailSubItem`) and host items (`azRailHostItem`), in both the compact rail and the expanded menu — so a host tap that opens a sub-menu is observable exactly like a leaf tap. This is the supported way to react to "the user used the rail" (e.g. to update your own state that a guidance `azStatus` predicate reads, or to `activate`/`deactivate` a guidance goal); it does not require — and is not affected by — the rail consuming its own taps.

#### `AzNavRail` / `AzHostActivityLayout` parameters — expansion observability

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `onExpandedChange` | `((Boolean) -> Unit)?` | Called whenever the **rail** transitions between collapsed and expanded states. Receives `true` on expand, `false` on collapse. Also fires on initial composition with the starting state. Available on both `AzNavRail` (direct) and `AzHostActivityLayout` (forwarded). |

### Help overlay scoping & rendering

* **Scope follows the trigger.** Tapping `azHelpRailItem` in the main rail shows cards for the main-rail items. Tapping `azHelpRailItem`/`azHelpSubItem` inside an `azNestedRail` block shows cards only for that nested rail's children. Internally the library tracks `helpScopeId = parent.id` (or `null` for main rail) at toggle time.
* **Offscreen items are suppressed.** Cards (and their connector lines) are filtered out when the item's last-known bounds don't overlap the overlay viewport — so a card never renders for a rail item that's currently scrolled off, in a collapsed menu, or in a closed nested popup.
* **Cards/lines update during scroll.** `itemBoundsCache` is a `SnapshotStateMap` populated by every rail item's `onGloballyPositioned`; the overlay's filter and `drawBehind` subscribe to it, so scrolling the rail re-runs the filter and the lines follow item positions in real time.
* **Bounds eviction.** Each rail/menu item attaches a `DisposableEffect` that removes its entry from `itemBoundsCache` on dispose. Without this, items that had bounds cached when the menu was expanded would keep stale positions after the menu collapsed, and the overlay would draw cards pointing at phantom locations.

### Reorderable items — persistence

`azRailRelocItem` reorders survive recomposition. The library writes the new per-host sibling order into `AzNavRailScopeImpl.savedRelocOrders` on drag-end, and re-applies it after every DSL re-evaluation via `applyRelocReorders()`. Without this layer the DSL block — which is re-applied on every recomposition — would always reinstate the original declaration order, snapping a freshly-dropped item back to where the user moved it from. The `onRelocate(from, to, newOrder)` callback still fires on drop so you can mirror the order into your own persistent store and seed the DSL with it on next launch.

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
    expandedWidth: Dp = 160.dp,
    collapsedWidth: Dp = 100.dp,
    showFooter: Boolean = true,
    appRepositoryUrl: String = ""
)
~~~

* `appRepositoryUrl` — **optional** override for the host app's GitHub repo used by the About reader.
  Blank (the default) auto-derives the repo from the app **namespace**: `com.<owner>.<repo>` →
  `https://github.com/<owner>/<repo>` (owner = 2nd segment, repo = last segment; a trailing build
  suffix like `.debug` is stripped). It **never** falls back to the AzNavRail library repo. (Helper:
  `GithubDocsRepository.repoUrlFromPackage`.) On **web** there is no package namespace, so
  `appRepositoryUrl` is **required** there (no auto-derivation); when unset the About entry is hidden.

> A hamburger drop-down menu is a standalone composable, **`AzDropdownMenu`** — not a rail mode. Its
> `azConfig` also takes `inAppAbout = true` and `appRepositoryUrl = ""`; because the dropdown has no
> onscreen area, "About" opens a **full-screen** in-app reader (`inAppAbout = false` reverts to a
> browser link). Same namespace derivation as the rail. See the README's "`AzDropdownMenu`" section
> and `docs/DSL.md`.

### `azTheme`
Controls the visual style of the rail.

~~~kotlin
fun azTheme(
    activeColor: Color = Color.Unspecified,
    defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
    headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
    translucentBackground: Color = Color.Unspecified,
    helpLineColors: List<Color> = emptyList(),
    headerIconSize: Dp = Dp.Unspecified
)
~~~

* `headerIconSize`: `Dp` — exact diameter of the header app-icon. `Dp.Unspecified` (default) sizes
  the icon to the rail width (legacy behavior).

### `azKinetics`
Configures the WP7-style kinetic typography (entrance/exit on the expanded menu items, press-tilt, and
the big screen title's sweep). Defaults animate; pass `AzEntrance.None`/`AzExit.None` to opt out. In
FAB/floating mode the cascade becomes a vertical up/down slide.

~~~kotlin
fun azKinetics(
    itemEntrance: AzEntrance = AzEntrance.Turnstile,
    itemExit: AzExit = AzExit.Turnstile,
    itemTextStyle: TextStyle? = null,
    entranceStaggerMs: Int = 55,
    entranceDurationMs: Int = 360,
    entranceEasing: Easing = AzEasing.Wp7Decelerate,
    entranceStartAngle: Float = 70f,
    tiltOnPress: Boolean = false,
    maxTiltDegrees: Float = 10f,
    titleEntrance: AzEntrance = AzEntrance.Turnstile,
    titleTextStyle: TextStyle? = null
)
~~~

* `AzEntrance`: `None | Fade | SlideUp | Turnstile`. `AzExit`: `None | Fade | Turnstile`.
* `AzEasing.Wp7Decelerate`: the signature fast-out/gentle-settle bezier (the default easing).
* `tiltOnPress` is auto-suppressed for draggable/relocatable items.
* The standalone `AzDropdownMenu` exposes the same item knobs on its own `azConfig` (also on by
  default). React: the rail reads these from `settings`, the dropdown takes matching props.

### `azAbout`
Configures the built-in About reader and the "More from Az" carousel.

~~~kotlin
fun azAbout(
    inAppAbout: Boolean = true,
    moreFromAzEnabled: Boolean = true,
    moreFromAzJsonUrl: String = "https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json",
    moreRailItem: Boolean = false
)
~~~

* `inAppAbout` — footer "About" opens the in-app markdown reader (auto-generated from the repo's docs)
  instead of opening the resolved repo in a browser. The repo is auto-derived from the app namespace
  on Android (`azConfig`'s `appRepositoryUrl` is an optional override); on web `appRepositoryUrl` is
  required (the About entry is hidden when it is unset).
* `moreFromAzEnabled` — show the "More from Az" entry inside the About screen.
* `moreFromAzJsonUrl` — raw URL of the link-only, CI-versioned `more-from-az.json` manifest.
* `moreRailItem` — also pin a "More" item at the bottom of the collapsed rail that opens the carousel.

> **Guides hidden over footer screens (all platforms):** while a footer screen (About or More from Az)
> is open, visible Help cards and any guidance callouts are hidden, and they return exactly where
> they were when the footer screen closes.

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

### Pages (Z-ordering)

`onscreen(alignment, page = 0f) { }` and `background(weight, page = 0f) { }` accept a `page: Float`. Pages add Z-ordering over standard Compose positioning:

| Concept | Behavior |
| :--- | :--- |
| Same page | One co-planar layer; position items with normal Compose `alignment` (or `Row`/`Column` inside the content) so they tile without overlapping. |
| Different pages | Stacked in Z and may overlap. **Higher `page` → further back**; lowest page is on top. |
| Decimals | `page = 1.5f` inserts a layer between `1f` and `2f` without renumbering. |
| Two books | `background()` pages form a book entirely beneath the `onscreen` book (which is beneath the rail/nav bar). `onscreen` pages respect safe zones; backgrounds fill the screen. `weight` breaks ties within a background page. |
| `AzHostActivityLayout(pagesEnabled = true)` | Default. Forced when on — unlabelled items share page `0f`. Set `false` to render in declaration order (backgrounds by `weight`) and ignore `page`. |

---

## 5. Status-Driven Guidance API

The guidance framework drives the user toward developer-declared **goals** by reactively surfacing the
instruction needed to reach the next **status** along a shortest path. You describe the userflow as a
graph of statuses (nodes) and edges (transitions carrying an instruction); the engine routes with BFS,
auto-advances the instant a target status becomes true, re-routes when the user wanders, and renders
every active goal's instruction simultaneously as a callout adjacent to its control. See
[`TUTORIAL_FRAMEWORK_PROPOSAL.md`](TUTORIAL_FRAMEWORK_PROPOSAL.md) for the conceptual reference.

> This section replaces the old scripted multi-scene **Tutorial Framework API**. See the migration note
> at the end of the section.

---

### DSL — Android (Kotlin)

Declared inside the `AzHostActivityLayout { ... }` content lambda, which now **returns** an
`AzGuidanceController`.

~~~kotlin
fun azStatus(id: String, predicate: () -> Boolean)
fun azEdge(from: String, to: String? = null, text: String, title: String? = null, highlightItemId: String? = null)
fun azGoal(id: String, target: String, label: String? = null, autoStartWhen: String? = null)
~~~

- `azStatus` registers a developer status whose truth is the predicate's return value.
- `azEdge` declares a transition from `from` to `to` carrying instruction `text` (with optional `title`
  and `highlightItemId`). A **passive edge** has `to = null` — it just shows info while `from` holds.
- `azGoal` declares a target status with a stable `id`, optional human `label`, and optional
  `autoStartWhen` status for self-activation.

You only hand-author edges into custom statuses; the engine auto-generates edges for the rail's own
affordances (see *Built-in statuses & auto-edges* below).

### DSL — React (TypeScript)

Declared as JSX children of the rail (under `AzHostActivityLayout` / `AzNavRail`).

~~~tsx
<AzStatus id="cart_open" predicate={() => cart.isOpen} />
<AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
<AzGoal id="checkout" target="az.screen.confirmation" label="Check out" autoStartWhen={null} />
~~~

`to` on `<AzEdge>` is optional (omit for a passive edge). The React package also exports
`AzGuidanceProvider`, `AzInstructionOverlay`, `useActiveStatuses`, `computeBuiltinStatuses`, `nextHop`,
`routeInstructions`, `computeAutoEdges`, and types `AzGuideHighlight`, `AzCalloutSide`, `AzInstruction`,
`AzGoalDef`, `AzEdgeDef`, `AzStatusPredicate`, `AzStatusProps`, `AzEdgeProps`, `AzGoalProps`.

---

### `AzGuidanceController`

Package `com.hereliesaz.aznavrail.tutorial` (Android); exported from `@HereLiesAz/aznavrail-react`.

| Member | Android type | React type | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | `Boolean` | `boolean` | Whether guidance is on. |
| `activeGoals` | `List<String>` | `string[]` | Currently active goal ids. |
| `completedGoals` | `List<String>` | `string[]` | Goal ids reached at least once (persisted). |
| `enable()` / `disable()` | — | — | Turn guidance on/off. |
| `activate(goalId)` / `deactivate(goalId)` | — | — | Activate / deactivate a goal. |
| `markReached(goalId)` | — | — | Mark a goal reached (persists it as completed). |
| `isCompleted(goalId)` | `Boolean` | `boolean` | Whether a goal has been completed. |

Goal activation is **developer-driven**. There is no built-in end-user goal picker; use `autoStartWhen`
for onboarding-style self-activation.

**Obtaining the controller — Android:**
~~~kotlin
// AzHostActivityLayout returns it:
val guidance = AzHostActivityLayout(navController = nav, currentDestination = route) { /* … */ }

// Or from the CompositionLocal under the host:
val guidance = LocalAzGuidanceController.current        // CompositionLocal<AzGuidanceController?>
val guidance = rememberAzGuidanceController()           // @Composable
~~~

**Obtaining the controller — React:**
~~~tsx
const guidance = useAzGuidanceController();
~~~

**Persistence:** completed goals persist under key `az_navrail_completed_goals` — Android
`SharedPreferences` file `az_tutorial_prefs`; React `localStorage` (and `AsyncStorage` on React Native).

---

### Model types

Package `com.hereliesaz.aznavrail.tutorial`; React equivalents exported from `@HereLiesAz/aznavrail-react`.

| Type | Shape |
| :--- | :--- |
| `AzGuideHighlight` | Sealed: `None`, `FullScreen`, `Item(id)`, `Area(left, top, width, height)`. |
| `AzInstruction` | `(text, title?, highlight, side, media?)`. |
| `AzCalloutSide` | `Auto` / `Above` / `Below` / `Start` / `End`. |
| `AzEdge` | `(from, to?, instruction)`. |
| `AzGoal` | `(id, target, label?, autoStartWhen?)`. |

**Overlay rendering note.** Where Android punches a true spotlight hole per target, React draws an
**accent ring** around each target over a light dim (multi-hole masking isn't portable across React
Native primitives).

---

### Built-in statuses & auto-edges

These `az.*` statuses are published automatically from live rail / host / route / help / onscreen state;
reference them as edge or goal targets.

| Status id | True when |
| :--- | :--- |
| `az.app.ready` | Always true (root; navigation auto-edges always have a reachable `from`). |
| `az.rail.expanded` | The rail menu is expanded. |
| `az.rail.collapsed` | The rail is collapsed (mutually exclusive with `az.rail.expanded`). |
| `az.rail.floating` | The rail is in FAB / floating mode. |
| `az.host.<id>.expanded` | Host item `<id>` is expanded. |
| `az.screen.<route>` | The current route is `<route>`. |
| `az.item.<id>.active` | Item `<id>` is the active/selected item. |
| `az.nestedRail.<id>.open` | Nested rail `<id>` popup is open. |
| `az.help.open` | The help overlay is open. |
| `az.onscreen.<id>.visible` | On-screen element `<id>` is visible. |

**Auto-edges** (you don't author these): "Open the menu" (`az.rail.collapsed → az.rail.expanded`),
tap a host item (→ `az.host.<id>.expanded`), tap a nested-rail item (→ `az.nestedRail.<id>.open`), tap a
routed item (→ `az.screen.<route>`). Rail items are tappable from `az.app.ready`; menu-only items require
`az.rail.expanded`. Instruction text is localizable on Android via `az_guide_open_menu` /
`az_guide_tap_item` (defaults "Open the menu" / "Tap <label>").

**Observation latency.** Predicates that read Compose snapshot state (Android) or React state are
observed instantly; predicates backed by a non-snapshot / non-React source (`StateFlow.value`, a plain
`var`, a mutable ref, an external store) are observed within a **~300 ms poll**.

---

### Migration from the scripted tutorial framework

| Old (removed) | New |
| :--- | :--- |
| `AzTutorial` / `AzScene` / `AzCard`, advance conditions, branching, checklist/media cards, `AzTutorialOverlay` | `azStatus` / `azEdge` / `azGoal` + the reactive engine |
| `AzTutorialController.startTutorial` / `markTutorialRead` | `AzGuidanceController.activate` / `markReached` |
| `azAdvanced(tutorials = …)` | removed — declare statuses/edges/goals in the host content lambda |
| Help-overlay "Start Tutorial" launch | removed — guidance is developer-activated, never launched from help |
| `LocalAzTutorialController` / `useAzTutorialController` | `LocalAzGuidanceController` / `useAzGuidanceController` |
| Persistence key `az_navrail_read_tutorials` | `az_navrail_completed_goals` |

---

## Bottom Sheet

The bottom-sheet shell is ported from [LogKitty](https://github.com/HereLiesAz/LogKitty) so AzNavRail consumers get the same four-detent, accumulated-delta-drag sheet, and LogKitty itself can replace its hand-rolled version with `AzBottomSheetWindowHost` with no visual change.

### Composables

| Composable | Description |
| :--- | :--- |
| `AzBottomSheet(controller, modifier, config, onSwipeLeft?, onSwipeRight?) { content }` | In-tree sheet. Anchored at the bottom of its parent and spans full width. The HIDDEN strip stays visible (dimmed handle) and accepts a **tap** that steps up to PEEK in addition to swipe-up. Apply `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` yourself if your body content needs to clear the system nav bar. |
| `AzBottomSheetInsetAware(controller, config, onSwipeLeft?, onSwipeRight?) { content }` | Same as `AzBottomSheet` but the modifier already applies `fillMaxSize() + windowInsetsPadding(navigationBars)`. |
| DSL form via `AzNavHostScope.azBottomSheet` | Registered above rail/menu/onscreen with `zIndex(2f)`, full-width, edge-to-edge (no `windowInsetsPadding` applied by the DSL). |

### DSL

| DSL | Scope | Description |
| :--- | :--- | :--- |
| `azBottomSheet(controller, config, onSwipeLeft?, onSwipeRight?) { content }` | `AzNavHostScope` | Registers a sheet rendered above rail/menu/onscreen in `AzHostActivityLayout` with `zIndex(2f)`, spans full screen width, extends to the bottom edge (no inset padding) so the HIDDEN strip is reachable from the system-nav-bar area. |

### State

| Class | Description |
| :--- | :--- |
| `enum AzSheetDetent { HIDDEN, PEEK, HALF, FULL }` | Discrete heights. |
| `AzSheetController(initial)` | State holder with dual `mutableState` + `StateFlow` channels. |
| `rememberAzSheetController(initial)` | Composable factory backed by `rememberSaveable`. |

`AzSheetController` properties / methods:

| Member | Type | Notes |
| :--- | :--- | :--- |
| `detent` | `AzSheetDetent` (mutable) | Setter also pushes to `detentFlow`. |
| `isEnabled` | `Boolean` (mutable) | `false` forces `HIDDEN` and blocks step calls. |
| `detentFlow` | `StateFlow<AzSheetDetent>` | Read-only. |
| `enabledFlow` | `StateFlow<Boolean>` | Read-only. |
| `stepUp()` / `stepDown()` | – | One detent per call; clamps at the ends. The swipe-down gesture calls `stepDown()` (descends one detent at a time, mirroring swipe-up). |
| `snapTo(target)` | – | Direct jump; blocked when disabled and target ≠ HIDDEN. |

### Configuration

`AzSheetConfig(...)` — all fields optional, sensible defaults match the LogKitty look.

| Field | Type | Default | Purpose |
| :--- | :--- | :--- | :--- |
| `backgroundColor` | `Color` | `Color.Unspecified` (→ `MaterialTheme.colorScheme.surface`) | Sheet fill. |
| `backgroundAlpha` | `Float` | `0.92f` | Alpha applied to fill. |
| `scrimColor` | `Color` | `Color.Black` | Dim layer above sheet in HALF/FULL. At PEEK, a transparent (no-dim) tap overlay catches taps instead. |
| `scrimAlpha` | `Float` | `0.32f` | Scrim alpha. |
| `hiddenStripDp` | `Dp` | `28.dp` | Swipe-target height in HIDDEN. Bumped from 14dp so it's reliably touchable on gesture-nav devices; the handle stays visible at this detent (dimmed). |
| `peekDp` | `Dp` | `56.dp` | Height in PEEK. |
| `halfFraction` | `Float` | `0.5f` | Fraction of parent height in HALF. |
| `fullFraction` | `Float` | `0.9f` | Fraction of parent height in FULL. |
| `dragThresholdDp` | `Dp` | `24.dp` | Cumulative drag needed per step. Swipe up steps one detent up; swipe down steps one detent down. |
| `collapseOnBack` | `Boolean` | `true` | Back press steps down. |
| `horizontalSwipeEnabled` | `Boolean` | `false` | Enables `onSwipeLeft` / `onSwipeRight`. |
| `animateInTree` | `Boolean` | `true` | In-tree shell animates between heights; system-overlay always hard-jumps. |
| `cornerRadiusDp` | `Dp` | `16.dp` | Top-corner radius. |
| `handleVisible` | `Boolean` | `true` | Centered drag-handle pill. |
| `drawBehindNavBar` | `Boolean` | `false` | When `true` **and** the device uses button navigation, the sheet draws behind the system nav bar (exposed height unchanged) and the bar is forced see-through so content shows through. No-op in gesture navigation. |

> **Automatic gesture-nav margin:** independent of any config, `AzHostActivityLayout` imposes **zero** bottom margin on on-screen content when the device is in gesture navigation (detected via `Settings.Secure` `navigation_mode`); button-navigation devices keep the `max(10% safe-zone, nav-bar inset)` bottom margin.

### System-overlay flavor

`class AzBottomSheetWindowHost(context, controller, config, lifecycleOwner, viewModelStoreOwner, savedStateRegistryOwner, navBarHeightPx, content)`

| Method | Description |
| :--- | :--- |
| `attach()` | Adds the sheet's `TYPE_APPLICATION_OVERLAY` window. Idempotent. |
| `attachNavBarDecor()` | Adds the secondary `TYPE_ACCESSIBILITY_OVERLAY` that tints the system nav bar. Call after your accessibility service binds. No-op when `navBarHeightPx <= 0`. |
| `detach()` | Removes both windows. |
| `updateConfig(config)` | Replaces the live config and, while attached at `HIDDEN`/`PEEK`, **immediately resizes the overlay window** to the new `hiddenStripDp`/`peekDp` (`HALF`/`FULL` stay `MATCH_PARENT`). |

The overlay window also delivers real window insets to its content: `WindowInsets.navigationBars` / `Modifier.navigationBarsPadding()` resolve to the actual system nav-bar inset inside the `content` slot (insets are forwarded un-consumed, so the app below still receives them).

Consumer manifest: declares `SYSTEM_ALERT_WINDOW` (and `BIND_ACCESSIBILITY_SERVICE` for the nav-bar decoration). The library itself ships no permissions or services.
