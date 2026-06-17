# The Synthetic Tongue (AzNavRailScope)

This document outlines the API surface of `AzNavRailScope`. This interface defines the DSL used to configure the rail, whether manually or via the generated code.

## The Live Dictatorship (v7.25)

The **v7.25 KSP Processor** injects reactive logic. When using `@Az` annotations with property bindings (e.g., `textProperty = "myTitle"`), the generated code no longer passes hardcoded literals. Instead, it accesses your `AzActivity` instance directly.

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
    expandedWidth: Dp = 130.dp,
    collapsedWidth: Dp = 80.dp,
    showFooter: Boolean = true,
    appRepositoryUrl: String = "https://github.com/HereLiesAz/AzNavRail",
    dropdownMenu: Boolean = false,
    dropdownSource: AzDropdownSource = AzDropdownSource.RAIL,
    dropdownAlignment: AzDropdownAlignment = AzDropdownAlignment.TOP_START,
    dropdownOffset: DpOffset = DpOffset.Zero
)
~~~

`dropdownMenu` turns the rail into a drop-down whose trigger behaves like a plain hamburger button:
the app icon replaces the hamburger and tapping it unfolds `dropdownSource` (`RAIL` = packed rail
buttons, `MENU` = full drawer rows) like an accordion. `onscreen` content spans the **full screen** —
drop-down mode reserves **no** top/bottom content safe zones, so content runs edge-to-edge and the
trigger floats over it. This mode excludes FAB/dragging, rail↔menu expansion, `noMenu`, swipe
gestures, physical docking, the footer, nested-rail popups, the bleeding app-name header, and the
help overlay.

`dropdownAlignment` places the trigger icon at one of nine standard anchors (`TOP_START` … `BOTTOM_END`);
`dropdownOffset` is a fine `DpOffset` nudge from that anchor. The panel unfolds downward for
top/centre anchors and upward for bottom anchors, so it always grows away from the nearest edge. See
the README's "Drop-down Menu Mode" section.

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

The About reader auto-discovers the markdown docs (root + `docs/`) of `azConfig`'s `appRepositoryUrl`
via the GitHub API (cached; public repos only) and renders them inline. "More from Az" is a link-only,
CI-versioned carousel — see the README's "In-App About Reader" and "More from Az" sections.

`onInteraction` is called whenever any rail item is interacted with — click, toggle, cycler advance, nested rail open, reloc drag, or host expand/collapse. It fires for both leaf items (`azRailItem` / `azRailSubItem`) and host items (`azRailHostItem`), in both the compact rail and the expanded menu, so opening a host menu is observable just like tapping a leaf. It receives the item's `id` and the `AzNavItem` itself, enabling analytics, UI feedback, and tutorial advancement (pair it with `controller.fireEvent(...)` and an `AzAdvanceCondition.Event` card) without per-item callbacks.

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
* `azMenuHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubHostItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, onClick)` — a sub-item that is itself a host; nests to any depth.
* `azRailSubHostItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, menuText, textColor, fillColor, onClick)` — a sub-item that is itself a host; nests to any depth.
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, forceHiddenMenuOpen, onHiddenMenuDismiss) { ... }`

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
