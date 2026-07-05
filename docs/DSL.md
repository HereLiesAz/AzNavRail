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
    appRepositoryUrl: String = "",
    // Menu-drawer look-and-feel:
    dimBehindMenu: Boolean = false,                              // opt-in dim scrim behind the drawer
    dimBehindMenuAlpha: Float = 0.4f,                            // 0..1 alpha of the scrim
    menuItemAlignment: AzMenuItemAlignment = AzMenuItemAlignment.SIDE, // SIDE (default) | CENTER
    justifyMenuItems: Boolean = true,                            // full-justify labels via letter-spacing
)
~~~

The menu-drawer knobs only affect the **expanded-menu drawer** labels (the standalone
`AzDropdownMenu` mirrors them on its own `azConfig`). Small rail-button labels are unaffected.
`SIDE` alignment resolves to `TextAlign.Start` when docked LEFT and `TextAlign.End` when RIGHT.
`justifyMenuItems` runs a **hybrid kerning + font-scale solver**: it fills the row with
`letterSpacing` alone until tracking would exceed `α · fontSize` (default `α = 0.15`); once the
kerning cap is hit, the font scales up so both letter-spacing and font-size converge on the mix
that lands the label exactly on the row width. Font growth is capped at `1.5×`. Labels shorter
than 2 characters, or already at/past the row width, are skipped. `AzDivider`'s default color is
now `LocalContentColor.current` on Compose and `currentColor` on the web, so the divider matches
the surrounding font — never a muted outline.

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
    entranceStaggerMs: Int = 60,           // small stagger → items overlap heavily
    entranceDurationMs: Int = 720,         // per-item duration
    entranceEasing: Easing = AzEasing.Wp7Decelerate,
    entranceStartAngle: Float = 90f,       // pure edge-on → flat, no fade, no slide
    tiltOnPress: Boolean = false,          // off by default on the rail (drag-safe)
    maxTiltDegrees: Float = 10f,
    titleEntrance: AzEntrance = AzEntrance.Turnstile,
    titleTextStyle: TextStyle? = null
)
~~~

`AzEntrance` = `None | Fade | SlideUp | Turnstile`; `AzExit` = `None | Fade | Turnstile`;
`AzEasing.Wp7Decelerate` is the signature snappy easing. `tiltOnPress` is automatically suppressed for
draggable/relocatable items. The default **Turnstile** entrance is a pure 90° `rotationY` sweep
hinged on the docked edge — no fade, no vertical slide. Because the stagger (60 ms) is much smaller
than the duration (720 ms), items overlap heavily: the next item starts ~60 ms after the previous
begins while the previous is still animating. The footer (About / Feedback / @HereLiesAz) then
**unfolds like an accordion** from the top edge, starting the moment the last item begins
(delay = `count * staggerMs` — one tick past the last item's start, making the footer the natural
next beat in the cascade).

In React, the rail reads these from `settings` (`itemEntrance`, `itemExit`, `titleEntrance`, …).
On **native React Native** the hinge is emulated via a `translateX ±(width/2)` correction around
`rotateY` because RN ignores `transformOrigin`; on web the CSS property applies natively.

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

While a footer screen (About or More from Az) is open, visible Help cards and any guidance callouts
are hidden, and they return exactly where they were when the footer screen closes (all platforms).

`onInteraction` is called whenever any rail item is interacted with — click, toggle, cycler advance, nested rail open, reloc drag, or host expand/collapse. It fires for both leaf items (`azRailItem` / `azRailSubItem`) and host items (`azRailHostItem`), in both the compact rail and the expanded menu, so opening a host menu is observable just like tapping a leaf. It receives the item's `id` and the `AzNavItem` itself, enabling analytics and UI feedback without per-item callbacks. (Guidance advancement no longer relies on this — the status-driven engine observes status predicates directly; see the Guidance DSL below.)

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
(`onClickMap`, `onFocusMap`). Inside `AzNavRail`, a `LaunchedEffect` keyed on the **stable set of
expandWhen host ids** fans out one child coroutine per host, each observing the condition through
`snapshotFlow { cond() }` (instant when the lambda reads Compose snapshot state) **merged with a
low-rate (~300 ms) poll**. Expansion is applied only on an actual transition, and the very first
observation only ever *expands* (so it never clobbers an `initiallyExpanded` or manual state).

> Keying on the host-id set rather than `scope.navItems` is deliberate: the item list's contents
> change on any item-value update, which previously tore down and relaunched the collectors and
> swallowed the rising edge — leaving the host stuck collapsed.

**Capabilities & limitations**

- ✅ Works whether or not the condition reads Compose state. A condition backed by `mutableStateOf`
  reacts **instantly**; one backed by a non-Compose source (`StateFlow.value`, `LiveData.value`, a
  plain `var`, an external store) still works via the poll — **with up to ~300 ms latency**.
- ✅ A manual collapse while the condition stays `true` is preserved (the condition acts on
  transitions, never continuously), so the user can override it until the next `false → true` edge.
- ⚠️ Because of the poll, an `expandWhen` host keeps a tiny recurring timer alive while mounted. It is
  one cheap boolean evaluation per host every ~300 ms — negligible, but not zero.
- ⚠️ React parity: the same semantics are implemented in `aznavrail-react` (render-time evaluation
  plus a 300 ms `setInterval` fallback); see *Capabilities & Limitations* doc.

**Typical use — guidance framework**

A guidance edge that highlights a sub-item inside a collapsed host would fail because the
sub-item is not laid out (and therefore has no measured bounds). Declare `expandWhen` on
the host to auto-expand it while a guidance goal is active:

~~~kotlin
azRailHostItem(
    id = "my-host",
    text = "Features",
    expandWhen = { guidance.activeGoals.contains("onboarding") }
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

## Guidance DSL (`azStatus` / `azEdge` / `azGoal`)

The status-driven guidance framework replaces the old scripted `azTutorial` scene/card DSL. You declare
the userflow as a flowchart of **statuses** (boolean nodes), **edges** (transitions carrying an
instruction), and **goals** (target statuses), then activate goals on the `AzGuidanceController`. The
engine watches which statuses are true and always shows the instruction to reach the next status toward
each active goal, auto-advancing the moment a target becomes true. See
[`TUTORIAL_FRAMEWORK_PROPOSAL.md`](TUTORIAL_FRAMEWORK_PROPOSAL.md) for the full reference.

### Kotlin

The three DSL functions are declared inside the `AzHostActivityLayout { ... }` content lambda, which
**returns** an `AzGuidanceController`.

~~~kotlin
fun azStatus(id: String, predicate: () -> Boolean)
fun azEdge(
    from: String, to: String? = null, text: String? = null, title: String? = null,
    highlightItemId: String? = null,      // a rail item, or the AZ_ITEM_ACTIVE token
    highlightTargetId: String? = null,    // a registered arbitrary shape (azGuidanceTarget)
    highlightSelector: (() -> String?)? = null,   // a rail item id resolved each frame
    steps: List<AzInstructionStep> = emptyList(), // paged sub-pointers (tap / advanceWhen)
)
fun azGoal(id: String, target: String, label: String? = null, autoStartWhen: String? = null)

// Arbitrary on-screen highlight target (window-space px shape, recomputed each frame; null ⇒ text-only).
fun azGuidanceTarget(id: String, shape: () -> AzGuideShape?)
// Hide guidance while `predicate` is true; re-show after `settleMs` once it clears.
fun azSuppressGuide(settleMs: Long = 700, predicate: () -> Boolean)
// Draw the callout body yourself (the non-blocking outline + connector still draw).
fun azGuideRenderer(renderer: @Composable (AzGuidanceSnapshot, Rect?) -> Unit)
~~~

`AzGuideShape` is `Circle` / `Rect` / `Path` in window-space px (see §9.6 of the Complete Guide); a step
is `AzInstructionStep(text, title?, highlightItemId?, highlightTargetId?, side?, highlightSelector?,
advanceWhen?)`. An informational step (no `advanceWhen`) advances on tap; an actionable one advances
when its status flips. The React port mirrors these as `<AzGuidanceTarget>`, `<AzSuppressGuide>`,
`<AzGuideRenderer>`, and `steps` / `highlightTargetId` props on `<AzEdge>`.

~~~kotlin
import com.hereliesaz.aznavrail.tutorial.*

val guidance = AzHostActivityLayout(navController = nav, currentDestination = route) {
    azRailItem(id = "cart", text = "Cart", route = "cart")
    azRailItem(id = "checkout", text = "Checkout", route = "checkout")
    onscreen { AzNavHost(startDestination = "home") { /* … */ } }

    // A developer status: a predicate over your own state.
    azStatus("cart_open") { cart.isOpen }

    // An interactive edge into a built-in status; highlight the control used to traverse it.
    azEdge(from = "cart_open", to = "az.screen.checkout", text = "Tap Checkout", highlightItemId = "checkout")

    // A passive edge (to = null): an ambient hint shown while `from` holds.
    azEdge(from = "cart_open", text = "Review your items before checking out.")

    // A goal: drive the user to the confirmation screen. Activate it later (developer-driven).
    azGoal(id = "checkout", target = "az.screen.confirmation", label = "Check out")

    // Optional self-activating onboarding goal.
    azGoal(id = "onboarding", target = "az.rail.expanded", label = "Take the tour", autoStartWhen = "az.app.ready")
}

// Developer-driven activation.
guidance.activate("checkout")
~~~

You only hand-author edges **into your custom statuses**. The engine auto-generates edges for the rail's
own affordances: "Open the menu" (`az.rail.collapsed → az.rail.expanded`), tapping a host item
(→ `az.host.<id>.expanded`), tapping a nested-rail item (→ `az.nestedRail.<id>.open`), and tapping a
routed item (→ `az.screen.<route>`). Instruction text is localizable via `az_guide_open_menu` /
`az_guide_tap_item`.

### TypeScript (React)

Declared as JSX children of the rail (under `AzHostActivityLayout` / `AzNavRail`). Get the controller
anywhere under the rail with `useAzGuidanceController()`.

~~~tsx
<AzStatus id="cart_open" predicate={() => cart.isOpen} />
<AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
<AzGoal id="checkout" target="az.screen.confirmation" label="Check out" autoStartWhen={null} />
~~~

~~~tsx
import {
    AzHostActivityLayout, AzRailItem,
    AzStatus, AzEdge, AzGoal, useAzGuidanceController,
} from '@HereLiesAz/aznavrail-react';

function Shell() {
    return (
        <AzHostActivityLayout dockingSide={AzDockingSide.LEFT}>
            <AzRailItem id="cart" text="Cart" route="cart" />
            <AzRailItem id="checkout" text="Checkout" route="checkout" />

            <AzStatus id="cart_open" predicate={() => cart.isOpen} />
            <AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
            <AzEdge from="cart_open" text="Review your items before checking out." />{/* passive: omit `to` */}
            <AzGoal id="checkout" target="az.screen.confirmation" label="Check out" autoStartWhen={null} />
        </AzHostActivityLayout>
    );
}

function Launcher() {
    const guidance = useAzGuidanceController();
    return <button onClick={() => guidance.activate('checkout')}>Guide me</button>;
}
~~~

Guidance is presented as a **non-blocking coach**: a thin outline around each step's target and a small
callout placed near it (with a connector), never dimming the screen or intercepting input outside a
callout. The user **swipes a callout away to cancel** (persisted); a step shown + acted on is never
re-shown. **Starting is always developer-driven** — call `activate(...)` (the `autoStartWhen` status-id
self-start still exists but is discouraged, and honours completion + skip).

`AzGuidanceController` (both platforms): `enabled`, `activeGoals`, `completedGoals`, `dismissedGoals`,
`enable()`, `disable()`, `activate(goalId)`, `deactivate(goalId)`, `markReached(goalId)`,
`isCompleted(goalId)`, `isDismissed(goalId)`, `skip(goalId?)` (cancel one or, with no arg, all),
`resetGuidance(goalId?)` (replay). For paged edges it also exposes `advance(stepKey)` / `next(stepKey)`
/ `back(stepKey)` and a no-arg `advance()`, and for observation `currentInstructions` / `current` (an
`AzGuidanceSnapshot` list; Kotlin also has `currentFlow`). Completion persists under
`az_navrail_completed_goals` and skips under `az_navrail_dismissed_goals` (Android `SharedPreferences`
file `az_tutorial_prefs`; React `localStorage` / RN `AsyncStorage`).

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
