# AzNavRail API Reference

This document serves as the technical reference for the AzNavRail library. The library is driven by a **Kotlin DSL** declared inside `AzHostActivityLayout`; the DSL block is re-applied on every recomposition, so items simply read your state and stay in sync. The `@Az` annotations in section 1 are sugar that generates exactly that DSL.

* **[1. High-Inference API](#1-high-inference-api-az)**: The `@Az` annotation system and its KSP processor.
* **[2. The Configuration Duality (Scope API)](#2-the-configuration-duality-scope-api)**: The DSL — the primary way to configure the rail.
* **[3. UI Components](#3-ui-components)**: Standalone components (`AzTextBox`, `AzRoller`, etc.) for building screens.
* **[4. Low-Level API](#4-low-level-api-manual)**: The underlying layout engine (`AzHostActivityLayout`).

---

## 1. High-Inference API (`@Az`)

Declare the rail with annotations and let a KSP processor generate the graph. This is **sugar over
the DSL**, not a replacement: the generated file is the same DSL you would have written by hand, so
it is readable and debuggable, and anything the annotations cannot express goes in
`AzActivity.configureRail()`, which the generated graph calls inside the same block.

### Setup

```kotlin
plugins { id("com.google.devtools.ksp") }

dependencies {
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail-annotations:<version>")
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:<version>")
}
```

### Usage

Annotate the Activity to configure the rail, and its **functions and properties** to declare items.
The processor emits `<ActivityName>AzGraph`; point `AzActivity.graph` at it.

```kotlin
@Az(
    app = App(displayAppName = true, vibrate = true, startDestination = "home"),
    advanced = Advanced(enableRailDragging = true),
)
class MainActivity : AzActivity() {
    override val graph = MainActivityAzGraph

    var unread by mutableIntStateOf(2)
    var dark by mutableStateOf(true)

    @Az(rail = RailItem(id = "home", text = "Home", route = "home", badgeProperty = "unread"))
    fun goHome() { unread = 0 }

    @Az(toggle = Toggle(id = "theme", toggleOnText = "Dark", toggleOffText = "Light", isCheckedProperty = "dark"))
    fun toggleTheme() = Unit

    override fun azGraphDestinations(builder: NavGraphBuilder) {
        builder.composable("home") { HomeScreen() }
    }
}
```

A working example lives in `SampleApp/src/main/java/com/hereliesaz/SampleApp/AzGraphDemoActivity.kt`.

### Members of `@Az`

Each is "declared" when its `id` is non-blank (or, for `Divider`, when `enabled` is true), so one
`@Az` carries exactly the one kind of thing you filled in.

| Member | Declares |
| :--- | :--- |
| `app` | `App(displayAppName, packRailButtons, vibrate, dockingSide, startDestination)` — class level. |
| `advanced` | `Advanced(isLoadingProperty, helpEnabled, enableRailDragging, autoGuidanceEdges)` — class level. |
| `rail` / `menu` | `RailItem` / `MenuItem` — an item on the rail, or in the drawer only. |
| `host` / `sub` | `RailHost` and the `SubItem`s that name its `id` as their `hostId`. |
| `toggle` | `Toggle(isCheckedProperty)` — the generated `onClick` flips that property. |
| `cycler` | `Cycler(options` or `optionsProperty`, `selectedOptionProperty)` — the generated `onClick` advances it. |
| `divider` | `Divider(enabled = true)`. |

### Reactive bindings

The `*Property` parameters name a property on the Activity. The DSL block re-runs on every
recomposition, so a `mutableStateOf` property keeps the item in sync with no further wiring:

* `textProperty` — the item's label.
* `badgeProperty` — its badge (emitted as `azItemState`).
* `loadingProperty` — its own loading animation (also `azItemState`).
* `disabledProperty` — whether it is inert.
* `isCheckedProperty` — **required** on `Toggle`.
* `selectedOptionProperty` / `optionsProperty` — **`selectedOptionProperty` required** on `Cycler`.
* `isLoadingProperty` on `Advanced` — the global overlay. Prefer per-item `loadingProperty`.

On a **function**, the function is the item's `onClick`. On a **property**, the property supplies the
item's text when no `text`/`textProperty` is given.


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
    appRepositoryUrl: String = "",
    // Menu-drawer look-and-feel:
    dimBehindMenu: Boolean = false,
    dimBehindMenuAlpha: Float = 0.4f,
    menuItemAlignment: AzMenuItemAlignment = AzMenuItemAlignment.SIDE,
    justifyMenuItems: Boolean = true,
)
~~~

* `dimBehindMenu` / `dimBehindMenuAlpha` — opt-in dim scrim behind the expanded drawer. When off,
  the drawer's tap-catcher is still full-size but fully transparent (existing behaviour).
* `menuItemAlignment: AzMenuItemAlignment` (`CENTER` | `SIDE`) — alignment of the drawer's labels
  within their rows. Default `SIDE`: `TextAlign.Start` when docked LEFT, `TextAlign.End` when
  docked RIGHT. Small rail-button labels are unaffected.
* `justifyMenuItems: Boolean` — when true, each label runs through a **hybrid kerning + font-scale
  solver** (`internal/AzJustify.kt`): fill the row with `letterSpacing` alone until tracking would
  exceed `α · fontSize` (`α = 0.15`), then grow the font past that limit so both letter-spacing and
  font-size converge on the mix that lands the label exactly on the row width. Font growth capped
  at `1.5×`. When the natural width **overflows** the row the solver **shrinks** the font
  (`scale = rowWidth / naturalWidth`, clamped to `≥ 0.5×`); combined with `softWrap = false` +
  `maxLines = 1` on the drawer's `Text`, line breaks are explicit-only. Single-character labels
  are skipped. Default `true`.
* `AzDivider` now defaults its color to `LocalContentColor.current` (Compose) / `currentColor`
  (web) so the divider inherits the surrounding font color and belongs to the same visual family
  as the labels next to it. The rail and dropdown call sites pass their accent explicitly.

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
    entranceStaggerMs: Int = AzMotion.ItemStaggerMs,   // 22ms — items overlap heavily
    entranceDurationMs: Int = AzMotion.ItemDurationMs, // 280ms per item
    entranceEasing: Easing = AzEasing.Wp7Decelerate,
    entranceStartAngle: Float = 90f, // pure edge-on → flat, no fade, no slide
    tiltOnPress: Boolean = false,
    maxTiltDegrees: Float = 10f,
    titleEntrance: AzEntrance = AzEntrance.Turnstile,
    titleTextStyle: TextStyle? = null
)
~~~

#### The motion scale (`AzMotion`)

Every transition in the library reads its timing from one object, `AzMotion`. Nothing hard-codes a
duration any more.

| Constant | Value | What it times |
| --- | ---: | --- |
| `ItemDurationMs` | 280 ms | One item's own entrance or exit |
| `ItemStaggerMs` | 22 ms | The gap between one item starting and the next |
| `PanelDurationMs` | 200 ms | A container arriving or leaving — panel, scrim, popup |
| `SettleDurationMs` | 240 ms | A layout settling into a new size |
| `IndicatorStepMs` | 420 ms | One step of a continuous indicator (`AzLoad`'s morph) |

These replaced a 60 ms stagger and a 720 ms item duration that had been copy-pasted as literals into
six places. At those values an eight-item drawer took **1.2 seconds** to finish arriving, and because
the turnstile entrance starts each item edge-on — and therefore invisible — the panel sat there empty
for the first stretch of it. Motion is a guide, not a toll: it should say where a thing came from and
then get out of the way.

The scale is proportional, so a host wanting a different tempo scales the whole thing through
`azKinetics(entranceDurationMs = …, entranceStaggerMs = …)` rather than hunting literals. A unit test
(`AzMotionTest`) holds the budget: a twelve-item cascade must settle inside 650 ms, the stagger must
stay well under the duration it offsets, and a panel must never outlast its own contents.

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

**The reader is dark, in every theme.** It is a full-screen surface the user has stepped aside into —
long-form prose, a document list, a carousel — and long-form reading on a bright white field is the
wrong call regardless of what the surrounding app is doing. It used to take
`MaterialTheme.colorScheme.surface`, which in a light-themed host meant a screenful of white. The
host's accent still comes through on headings, links, and the close affordance, and a host-supplied
`translucentBackground` still wins outright.

**Three ways out**, because a full-screen reader that can only be left through one 24 dp icon is a
room with a keyhole for a door:

* **Drag down anywhere.** The sheet follows the finger, springs back if the pull was not committed,
  and leaves if it was. A grab handle at the top announces the gesture.
* **The close icon**, now a 48 dp target rather than a bare glyph. So is the in-reader back arrow.
* **System back**, which steps out of an open document first and closes the reader from the contents.

---

## 3. UI Components

Standalone components designed to be used **inside** your `@Composable` screens.

### `AzTextBox`
A high-ground text input field with support for autocomplete, password masking, multiline text, and pre-filled initial values.

### `AzForm`
A form builder component for managing and grouping multiple `AzTextBox` fields together. Form entries now strictly enforce and securely support initial pre-filled values (`initialValue`) across platform variants.

### `AzToggle`
A standalone toggle switch matching the rail's aesthetic.

### `AzSlider`
The rail's slider, drawn on Material 3 Expressive lines. One instrument in four shapes — they differ
only in where the active track begins and how many thumbs ride it, which is why they are one
composable and not four.

~~~kotlin
@Composable
fun AzSlider(
    value: Float = 0f,
    onValueChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    config: AzSliderConfig = AzSliderConfig(),
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    rangeValue: ClosedFloatingPointRange<Float> = 0f..1f,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit = {},
    length: Dp? = null,
    label: String? = null,
)
~~~

* `AzSliderVariant`: `CONTINUOUS | STEPPED | CENTERED | RANGE`.
  * `STEPPED` snaps to evenly-spaced stops and always draws stop indicators — they are the only cue
    a user gets that the value is quantised, so they are not optional.
  * `CENTERED` grows the active track *out of* its origin (the middle of the range unless `origin`
    says otherwise), so the sign of the value is legible without reading the number.
  * `RANGE` carries two thumbs; the one nearest the touch takes the drag and keeps it for the whole
    gesture, so the thumbs cannot swap under the finger mid-drag.
* `AzSliderSize`: `XSMALL | SMALL | MEDIUM | LARGE | XLARGE`. The track thickens faster than the
  thumb grows, so a large slider reads as a substantial control rather than a magnified small one.
* `AzSliderOrientation`: `HORIZONTAL | VERTICAL`. A vertical track runs bottom-up — down is less,
  the direction every physical fader moves.

It is drawn rather than composed out of Material's own `Slider` because the rail has to render
identically on Android and in Compose Multiplatform, and has to stand in an 80 dp-wide rail slot
running vertically — which Material's slider will not do.

On React the same control ships as `<AzSlider>` plus `<AzRailSlider>`, with `AzSliderSizeMetrics`
carrying the size ladder and `AzMotion` (in `AzNavRailDefaults`) carrying the timings.

**In the rail:** `azRailSlider(...)` puts the same control in a rail item that unfolds *in its own
slot*. Folded it is an ordinary rail button; tapped, the slot grows along the rail and the button
becomes the track, with the value underneath as the way back. Nothing opens over the rail and nothing
moves the user elsewhere — the control arrives where their attention already was. The rail forces the
orientation vertical, because the rail is; everything else the host asked for is honoured.

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
