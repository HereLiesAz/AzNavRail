# The Synthetic Tongue (AzNavRailScope)

This document outlines the API surface of `AzNavRailScope`. This interface defines the DSL used to configure the rail, whether manually or via the generated code.

## Reactive Property Binding

The **KSP Processor** injects reactive logic. When using `@Az` annotations with property bindings (e.g., `textProperty = "myTitle"`), the generated code no longer passes hardcoded literals. Instead, it accesses your `AzActivity` instance directly.

Because these properties are ideally `mutableStateOf`, the entire `AzGraph` will **recompose** automatically when your Activity state changes.

## Configuration & Theme

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

`appRepositoryUrl` is an **optional** override for the About reader's repo. Blank (the default)
auto-derives the repo from the app **namespace** on Android: `com.<owner>.<repo>` →
`https://github.com/<owner>/<repo>` (owner = 2nd segment, repo = last segment; trailing build suffixes
like `.debug` are stripped). It **never** falls back to the AzNavRail library repo. On **web** there is
no namespace, so `appRepositoryUrl` is **required** (no auto-derivation); when unset the About entry is
hidden.

> A hamburger drop-down menu is **not** a rail mode. Use the standalone `AzDropdownMenu` composable —
> placed inline like `AzButton`, with no `AzNavHost`, no scope, and no safe zones. See
> "`AzDropdownMenu`" below.

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

`headerIconSize` pins the header app-icon to an exact diameter. When left `Dp.Unspecified` the icon
sizes itself to the rail width (legacy behavior).

### `azKinetics`
Configures the WP7-style **kinetic typography**: the staggered turnstile entrance/exit on the
expanded menu items, the press-tilt, and the big screen-boundary title's sweep. Config-driven (preset
enums, no free-composable escape hatch). Defaults animate — pass `AzEntrance.None`/`AzExit.None` to
opt a surface out. In FAB/floating mode the cascade degrades to a vertical up/down slide.

~~~kotlin
fun azKinetics(
    itemEntrance: AzEntrance = AzEntrance.Turnstile,
    itemExit: AzExit = AzExit.Turnstile,
    itemTextStyle: TextStyle? = null,
    entranceStaggerMs: Int = 55,
    entranceDurationMs: Int = 360,
    entranceEasing: Easing = AzEasing.Wp7Decelerate,
    entranceStartAngle: Float = 70f,
    tiltOnPress: Boolean = false,   // off by default on the rail (drag-safe)
    maxTiltDegrees: Float = 10f,
    titleEntrance: AzEntrance = AzEntrance.Turnstile,
    titleTextStyle: TextStyle? = null
)
~~~

`AzEntrance` = `None | Fade | SlideUp | Turnstile`; `AzExit` = `None | Fade | Turnstile`;
`AzEasing.Wp7Decelerate` is the signature snappy easing. `tiltOnPress` is automatically suppressed for
draggable/relocatable items. In React, the rail reads these from `settings` (`itemEntrance`,
`itemExit`, `titleEntrance`, …).

### `azAdvanced`
Controls system overrides, loading states, and floating window bindings.

~~~kotlin
fun azAdvanced(
    isLoading: Boolean = false,
    helpEnabled: Boolean = false,
    onDismissHelp: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onUndock: (() -> Unit)? = null,
    enableRailDragging: Boolean = false,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    helpList: Map<String, Any> = emptyMap(),
    tutorials: Map<String, AzTutorial> = emptyMap(),
    onInteraction: ((String, AzNavItem) -> Unit)? = null
)
~~~

### `azAbout`
Configures the built-in **About** reader and the **"More from Az"** carousel.

~~~kotlin
fun azAbout(
    inAppAbout: Boolean = true,        // footer "About" opens the in-app reader vs a browser
    moreFromAzEnabled: Boolean = true, // show the "More from Az" entry in the About screen
    moreFromAzJsonUrl: String = "https://raw.githubusercontent.com/HereLiesAz/AzNavRail/main/more-from-az.json",
    moreRailItem: Boolean = false      // also pin a "More" item at the bottom of the rail
)
~~~

The About reader auto-discovers the markdown docs (root + `docs/`) of the resolved repo via the GitHub
API (cached; public repos only) and renders them inline. On **Android** the repo is auto-derived from
the app namespace (`azConfig`'s `appRepositoryUrl` is an optional override, never the AzNavRail library
repo); on **web** `appRepositoryUrl` is required (the About entry is hidden when unset). "More from Az"
is a link-only, CI-versioned carousel — see the README's "In-App About Reader" and "More from Az"
sections.

While a footer screen (About or More from Az) is open, visible Help cards and any in-progress tutorial
are hidden, and they return exactly where they were when the footer screen closes (all platforms).

`onInteraction` is called whenever any rail item is interacted with — click, toggle, cycler advance, nested rail open, reloc drag, or host expand/collapse. It fires for both leaf items (`azRailItem` / `azRailSubItem`) and host items (`azRailHostItem`), in both the compact rail and the expanded menu, so opening a host menu is observable just like tapping a leaf. It receives the item's `id` and the `AzNavItem` itself, enabling analytics, UI feedback, and tutorial advancement (pair it with `controller.fireEvent(...)` and an `AzAdvanceCondition.Event` card) without per-item callbacks.

## `AzDropdownMenu` (standalone app-icon drop-down)

A drop-down menu is a standalone widget declared with the **same opinionated DSL as the rail**: it
accepts only the configuration the rest of the library sanctions (no arbitrary panel background,
offsets, icon tint/source, or free composable escape hatch). Its trigger is the **app icon**
(auto-drawn like the rail's header; shape/size set via `azConfig`), dropped inline like any widget.
Tapping it unfolds an **overlay panel** (a `Popup`) of the items you declare; tapping outside or
pressing back folds it up.

~~~kotlin
@Composable
fun AzDropdownMenu(
    modifier: Modifier = Modifier,
    navController: NavController? = LocalAzNavHostScope.current?.navController,  // for item routes
    expanded: Boolean? = null,             // optional controlled state
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: AzDropdownMenuScope.() -> Unit // a plain builder, like the rail's
)

// Inside the builder:
interface AzDropdownMenuScope {
    fun azConfig(
        design: AzDropdownDesign = AzDropdownDesign.MENU,   // RAIL = rail width, MENU = menu width
        dockingSide: AzDockingSide = AzDockingSide.LEFT,    // which screen edge the panel pins to
        vibrate: Boolean = false,
        expandedWidth: Dp = 160.dp,
        collapsedWidth: Dp = 100.dp,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,  // app-icon clip, like azTheme
        headerIconSize: Dp = 48.dp,                         // app-icon diameter, like azTheme
        showFooter: Boolean = true,                         // MENU footer (About/Feedback/@HereLiesAz)
        inAppAbout: Boolean = true,                          // "About" opens a full-screen in-app reader
        appRepositoryUrl: String = "",                       // optional override; else derived from namespace
        // — kinetic typography (on by default; pass None to opt out) —
        itemTextStyle: TextStyle? = null,                    // merged over each MENU row label
        itemEntrance: AzEntrance = AzEntrance.Turnstile,     // None | Fade | SlideUp | Turnstile
        entranceStaggerMs: Int = 55,
        entranceDurationMs: Int = 360,
        entranceEasing: Easing = AzEasing.Wp7Decelerate,
        entranceStartAngle: Float = 70f,
        tiltOnPress: Boolean = false,                        // WP7 3D tilt toward the press
        maxTiltDegrees: Float = 10f,
        itemExit: AzExit = AzExit.Turnstile                  // None | Fade | Turnstile
    )
    fun azItem(text, route = null, color, textColor, fillColor, shape, enabled, closeOnClick = true, onClick)
    fun azToggle(isChecked, toggleOnText, toggleOffText, route = null, …, onToggle)
    fun azCycler(options, selectedOption, route = null, …, onCycle)
    fun azDivider()
}
~~~

`azConfig` mirrors the rail's `azConfig`/`azTheme`: `design` sets the look + width
(`AzDropdownDesign.RAIL` = compact rail buttons at `collapsedWidth` ≈100dp; `AzDropdownDesign.MENU`
= full-width labeled rows at `expandedWidth` ≈160dp), `dockingSide` pins the panel to the `LEFT`
or `RIGHT` screen edge, and `headerIconShape`/`headerIconSize` set the app-icon trigger's clip and
diameter (the same knobs the rail exposes via `azTheme`). The `MENU` design renders rows at the
rail's menu-item text size and — like the rail's expanded menu — carries the footer
(About / Feedback / @HereLiesAz, gated by `showFooter`). Because the dropdown has no onscreen/host
area, tapping **About** opens a **full-screen in-app reader** drawn as its own layer when
`inAppAbout = true` (the default); `inAppAbout = false` opens the repo in a browser. The repo is
auto-derived from the app namespace on Android, with `appRepositoryUrl` as an optional override (never
the AzNavRail library repo). The panel drops from the trigger automatically (downward when it fits,
else upward). Items accept only the
rail's sanctioned per-item knobs, plus a navigation `route`: when set,
tapping navigates the `navController` (so the drop-down can drive an `AzNavHost`), then runs the
callback, then folds up if `closeOnClick`.

~~~kotlin
AzDropdownMenu(navController = navController) {
    azConfig(design = AzDropdownDesign.MENU, dockingSide = AzDockingSide.LEFT)
    azItem("Home", route = "home") { }
    azToggle(isChecked = dark, toggleOnText = "Dark", toggleOffText = "Light") { dark = it }
    azDivider()
    azItem("Sign out") { signOut() }
}
~~~

On React the equivalent is `<AzDropdownMenu design dockingSide onNavigate>` with `<AzDropdownItem>`
children (each accepting an optional `route`); props mirror the Kotlin `azConfig` names.

## Hidden Menu Builders (for `azRailRelocItem`)
* `listItem(text, route)`
* `listItem(text, onClick)`
* `inputItem(hint, onValueChange)`
* `inputItem(hint, initialValue, onValueChange)`

## Item Builders

The following functions are used to define the rail structure.

> **`content`** accepts a `String`, a `Color`, a drawable/vector resource id (`Int`), a Compose
> `ImageVector` or `Painter`, or any image model Coil can load (`Bitmap`, URL, `File`, `Uri`).
> Non-text graphics fill the item's shape (cover) and are clipped to it; `ImageVector` content is
> tinted with the item color.

* `azMenuItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azHelpRailItem(id, text, content, color, shape)`
* `azRailItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `@Composable azNestedRail(id, text, route, content, color, shape, alignment, disabled, screenTitle, info, classifiers, onFocus) { ... }`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azMenuHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, initiallyExpanded, expandWhen, onExpandedChange, onClick)`
* `azRailHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, initiallyExpanded, expandWhen, onExpandedChange, onClick)`
* `azMenuSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubHostItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, initiallyExpanded, expandWhen, onExpandedChange, onClick)` — a sub-item that is itself a host; nests to any depth.
* `azRailSubHostItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, initiallyExpanded, expandWhen, onExpandedChange, onClick)` — a sub-item that is itself a host; nests to any depth.
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, forceHiddenMenuOpen, onHiddenMenuDismiss) { ... }`

---

## Reactive Host Expansion (`expandWhen`)

All four host-item builders (`azMenuHostItem`, `azRailHostItem`, `azMenuSubHostItem`,
`azRailSubHostItem`) accept an optional `expandWhen: (() -> Boolean)?` parameter.

**Behaviour**

| Edge | Effect |
| :--- | :--- |
| `false → true` | Host auto-expands |
| `true → false` | Host auto-collapses |
| User collapse while `true` | Respected; condition acts again only on the next `false → true` edge |

**How it works**

`expandWhen` is a lambda — it cannot live on `AzNavItem` (a `@Parcelize` class). Instead it
is stored in `AzNavRailScopeImpl.expandWhenMap` alongside the other callback maps
(`onClickMap`, `onFocusMap`). Inside `AzNavRail`, a `LaunchedEffect` keyed on
`scope.navItems` fans out one child coroutine per host item, each running
`snapshotFlow { cond() }.collect { … }` so the runtime re-evaluates the condition whenever
any Compose snapshot state it reads changes.

**Typical use — tutorial framework**

A tutorial card that highlights a sub-item inside a collapsed host would fail because the
sub-item is not laid out (and therefore not in `itemBoundsCache`). Declare `expandWhen` on
the host to auto-expand it when the tutorial is active:

~~~kotlin
azRailHostItem(
    id = "my-host",
    text = "Features",
    expandWhen = { tutorialController.activeTutorialId.value == "onboarding" }
)
~~~

Both the `expandWhen` and `initiallyExpanded` qualifiers can coexist. `initiallyExpanded`
fires once on first appearance; `expandWhen` fires on every edge transition thereafter.

---

## Observing Host Expansion (`onExpandedChange`)

All four host-item builders accept an optional `onExpandedChange` callback that fires whenever the user manually expands or collapses that specific host item. It does NOT fire for programmatic changes driven by `expandWhen` or `initiallyExpanded`.

**Callback signature**

| Platform | Type |
| :--- | :--- |
| Android / Kotlin | `((Boolean) -> Unit)?` |
| React / TypeScript | `(expanded: boolean) => void` |

**Typical use — persist expansion per host**

~~~kotlin
// Android
azRailHostItem(
    id = "settings",
    text = "Settings",
    initiallyExpanded = savedExpandedIds.contains("settings"),
    onExpandedChange = { isExpanded ->
        if (isExpanded) savedExpandedIds.add("settings")
        else savedExpandedIds.remove("settings")
    }
)
~~~

~~~tsx
// React
const [expanded, setExpanded] = useLocalStorage<Record<string, boolean>>('hostExpanded', {})

<AzRailHostItem
  id="settings"
  text="Settings"
  initiallyExpanded={expanded['settings'] ?? false}
  onExpandedChange={(isExpanded) => setExpanded(prev => ({ ...prev, settings: isExpanded }))}
/>
~~~

The callback covers all four builder types: `azMenuHostItem`, `azRailHostItem`, `azMenuSubHostItem`, and `azRailSubHostItem`, including hosts nested inside a nested-rail popup.

---

## Tutorial DSL (`azTutorial`)

Tutorials are defined with `azTutorial { ... }` and registered via `azAdvanced(tutorials = mapOf("id" to tutorial))`.

### Kotlin

~~~kotlin
import com.hereliesaz.aznavrail.tutorial.*

val tutorial = azTutorial {
    // Lifecycle callbacks (both optional)
    onComplete { /* fired when last scene completes */ }
    onSkip    { /* fired when Skip Tutorial tapped */ }

    // ── Variable branch gate (invisible redirect node) ──────────────────
    scene(id = "gate", content = { /* empty or backdrop composable */ }) {
        branch(varName = "plan", mapOf(
            "pro"  to "scene-pro",
            "free" to "scene-free"
        ))
    }

    // ── Scene with all card types ────────────────────────────────────────
    scene(id = "scene-pro", content = { ProScreen() }) {

        // 1. Default advance condition: Button (Next button shown)
        card(
            title  = "Welcome",
            text   = "This is the pro experience.",
            highlight = AzHighlight.FullScreen
        )

        // 2. TapAnywhere
        card(
            title  = "Tap anywhere to continue",
            text   = "Really, anywhere.",
            highlight = AzHighlight.None,
            advanceCondition = AzAdvanceCondition.TapAnywhere
        )

        // 3. TapTarget — with per-item branching
        card(
            title  = "Pick a path",
            text   = "Tap the item you want to explore.",
            highlight = AzHighlight.Item("nav-menu"),
            advanceCondition = AzAdvanceCondition.TapTarget,
            branches = mapOf(
                "settings-btn" to "scene-settings",
                "profile-btn"  to "scene-profile"
            )
        )

        // 4. Event-driven advance
        card(
            title  = "Open the menu",
            text   = "Swipe right or tap the rail header.",
            highlight = AzHighlight.Item("rail-header"),
            advanceCondition = AzAdvanceCondition.Event("menu_opened")
        )

        // 5. Checklist card — Next disabled until all items checked
        card(
            title  = "Before you continue",
            text   = "Confirm the following:",
            checklistItems = listOf("I read the docs", "I set up my account")
        )

        // 6. Media card — content shown between title and text
        card(
            title  = "The Rail",
            text   = "Sits on the left or right edge.",
            mediaContent = { Image(painterResource(R.drawable.rail), contentDescription = null) }
        )
    }
}

// Register
azAdvanced(
    helpEnabled = true,
    onItemGloballyPositioned = { id, rect -> boundsMap[id] = rect },
    tutorials = mapOf("tut-1" to tutorial)
)

// Start with variables
controller.startTutorial("tut-1", variables = mapOf("plan" to "pro"))

// Fire an event from app logic
controller.fireEvent("menu_opened")
~~~

### TypeScript (React Native / Web)

The TypeScript shape is a plain discriminated-union object — no builder DSL.

~~~typescript
import { AzTutorial, AzScene, AzCard } from '@HereLiesAz/aznavrail-react'; // or aznavrail-web

const tutorial: AzTutorial = {
    onComplete: () => {},
    onSkip:     () => {},
    scenes: [
        // Variable branch gate
        {
            id: 'gate',
            content: () => null,
            cards: [],
            branchVar: 'plan',
            branches: { pro: 'scene-pro', free: 'scene-free' },
        },
        {
            id: 'scene-pro',
            content: () => <ProScreen />,
            cards: [
                // 1. Button (default)
                {
                    title: 'Welcome',
                    text: 'This is the pro experience.',
                    highlight: { type: 'FullScreen' },
                },
                // 2. TapAnywhere
                {
                    title: 'Tap anywhere to continue',
                    text: 'Really, anywhere.',
                    highlight: { type: 'None' },
                    advanceCondition: { type: 'TapAnywhere' },
                },
                // 3. TapTarget with branching
                {
                    title: 'Pick a path',
                    text: 'Tap the item you want to explore.',
                    highlight: { type: 'Item', id: 'nav-menu' },
                    advanceCondition: { type: 'TapTarget' },
                    branches: {
                        'settings-btn': 'scene-settings',
                        'profile-btn':  'scene-profile',
                    },
                },
                // 4. Event-driven
                {
                    title: 'Open the menu',
                    text: 'Swipe right or tap the rail header.',
                    highlight: { type: 'Item', id: 'rail-header' },
                    advanceCondition: { type: 'Event', name: 'menu_opened' },
                },
                // 5. Checklist
                {
                    title: 'Before you continue',
                    text: 'Confirm the following:',
                    checklistItems: ['I read the docs', 'I set up my account'],
                },
                // 6. Media
                {
                    title: 'The Rail',
                    text: 'Sits on the left or right edge.',
                    mediaContent: () => <img src={railImg} style={{ maxHeight: 120 }} alt="" />,
                },
            ],
        },
    ],
};

// React Native provider + hook
// <AzTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzTutorialProvider>
// const controller = useAzTutorialController();

// Web provider + hook
// <AzWebTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzWebTutorialProvider>
// const ctrl = useAzWebTutorialController();

controller.startTutorial('tut-1', { plan: 'pro' });
controller.fireEvent('menu_opened');
~~~

---

## Bottom Sheet

The `azBottomSheet` DSL registers a four-detent bottom sheet that draws above the rail, the menu, and the `onscreen` area, spans the full screen width, and extends all the way to the bottom of the screen — so the HIDDEN-detent strip is reachable from the system-navigation-bar edge for a swipe-up reveal. It is **not** a `background()`; it sits at the top z-layer of `AzHostActivityLayout` (via `Modifier.zIndex(2f)`). If your sheet body needs to clear the system nav bar visually, pad inside your `content` lambda or use `AzBottomSheetInsetAware` directly outside the DSL.

The shell was ported from [LogKitty](https://github.com/HereLiesAz/LogKitty); its detents, drag accumulator, and controller API are preserved verbatim so LogKitty can swap its custom sheet for `AzBottomSheetWindowHost` with no observable behavior change.

### Detents
- `AzSheetDetent.HIDDEN` — thin swipe strip (configurable, default **28dp**). Stays visible with a dimmed drag-handle pill and accepts a **tap** that steps up to PEEK in addition to the swipe-up gesture, so the affordance is discoverable.
- `AzSheetDetent.PEEK` — single-line ticker (configurable, default 56dp).
- `AzSheetDetent.HALF` — ~50% of the available height (configurable via `halfFraction`).
- `AzSheetDetent.FULL` — ~90% of the available height (configurable via `fullFraction`).

### In-tree usage

~~~kotlin
val sheetController = rememberAzSheetController()

AzHostActivityLayout(navController = nav, currentDestination = currentRoute) {
    azConfig(dockingSide = AzDockingSide.LEFT)
    azMenuItem(id = "home", text = "Home", route = "home", onClick = { /* … */ })
    onscreen(alignment = Alignment.Center) {
        AzNavHost(startDestination = "home") { /* … */ }
    }

    // One DSL line. Draws above rail/menu/onscreen, behind the system nav bar.
    azBottomSheet(
        controller = sheetController,
        config = AzSheetConfig(peekDp = 48.dp, halfFraction = 0.5f, fullFraction = 0.9f),
    ) {
        // Any composable content; the shell provides chrome (drag handle, scrim, gestures).
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Sheet body")
            Button(onClick = { sheetController.stepUp() }) { Text("Expand") }
            Button(onClick = { sheetController.stepDown() }) { Text("Collapse") }
        }
    }
}
~~~

### Controller

`AzSheetController` exposes:

- `var detent: AzSheetDetent` — read/write current state.
- `var isEnabled: Boolean` — disabling forces `HIDDEN` and blocks `stepUp`/`stepDown`.
- `val detentFlow: StateFlow<AzSheetDetent>` — observed by the system-overlay flavor.
- `val enabledFlow: StateFlow<Boolean>`.
- `stepUp()` / `stepDown()` — one detent at a time, clamped at the ends.
- `snapTo(target: AzSheetDetent)` — jump directly.

Use `rememberAzSheetController(initial)` inside Compose so detent state survives recomposition and configuration changes.

### Gestures
- **Vertical drag up** on the sheet card or hidden strip accumulates per-frame delta and fires exactly one `stepUp()` per gesture when the threshold (`config.dragThresholdDp`, default 24dp) is crossed.
- **Vertical drag down** (swipe-down) calls `snapTo(HIDDEN)`, dismissing the sheet entirely in one gesture rather than stepping down one detent at a time.
- **Scrim tap** in `HALF` / `FULL` calls `stepDown()` (dim overlay visible).
- **Transparent tap overlay** at `PEEK` — a non-dimmed, full-screen tap catcher that calls `stepDown()`, transitioning to HIDDEN. Makes the dismiss affordance discoverable for users who tap rather than swipe.
- **Back press** when `config.collapseOnBack = true` calls `stepDown()` while the sheet is non-HIDDEN.
- **Horizontal swipe** is opt-in via `config.horizontalSwipeEnabled = true` and the `onSwipeLeft` / `onSwipeRight` callbacks (used by LogKitty for tab navigation).

### System-overlay flavor

For Services that float a sheet over the active foreground app (LogKitty's use case), use `AzBottomSheetWindowHost`:

~~~kotlin
val sheetController = AzSheetController(initial = AzSheetDetent.HIDDEN)
val host = AzBottomSheetWindowHost(
    context = this,
    controller = sheetController,
    config = AzSheetConfig(backgroundColor = userBg, backgroundAlpha = userAlpha),
    lifecycleOwner = this,
    viewModelStoreOwner = this,
    savedStateRegistryOwner = this,
    navBarHeightPx = navBarPx,
) { /* same content slot as AzBottomSheet */ }

host.attach()
host.attachNavBarDecor()   // call from your accessibility service's onServiceConnected
// onDestroy: host.detach()
~~~

The library ships no `Service`; consumers declare the `SYSTEM_ALERT_WINDOW` (and, for the nav-bar decoration, `BIND_ACCESSIBILITY_SERVICE`) permissions in their own manifest.
