# Migration Guide: Surviving the Automation Purge

If you are reading this, you are attempting to upgrade AzNavRail from its chaotic, manual-DSL era to the totalitarian KSP architecture.

## The Live Dictatorship Migration

The annotation parameters changed to support dynamic binding and resolve cyclic dependencies.

### 1. Enum to String Literal Conversion
To prevent the `annotation` module from depending on the `core` library, all Enum-based parameters in annotations have been converted to `String`.

*   **@App**: `dock = AzDockingSide.LEFT` → `dock = "LEFT"`
*   **@NestedRail**: `alignment = AzNestedRailAlignment.VERTICAL` → `alignment = "VERTICAL"`
*   **@Theme**: `defaultShape = AzButtonShape.CIRCLE` → `defaultShape = "CIRCLE"`

### 2. Reactive Property Binding
If you want your rail items to react to Activity state, you must now use the `*Property` fields in the annotations.

**Old (Static):**
~~~kotlin
@Az(rail = RailItem(text = "Status", disabled = true))
~~~

**New (Reactive):**
~~~kotlin
@Az(rail = RailItem(textProperty = "currentStatus", disabledProperty = "isOffline"))
~~~

---

## Phase 1: Eradicate the Manual Graph

You previously built your UI inside `setContent { ... }` using a sprawling DSL block. 

**Action:** Delete it. Delete all of it. The `AzHostActivityLayout`, the `AzNavHost`, the `composable` routes. KSP generates this now.

## Phase 2: Surrender the Activity

Your `MainActivity` must now bow to the generated structural authority.

**The New Mandate:**
Extend `AzActivity` and declare the generated `AzGraph`.

~~~kotlin
@Az(app = App(dock = "LEFT"))
class MainActivity : AzActivity() {
    override val graph = AzGraph
}
~~~

## Phase 3: The Annihilation of `azSettings` and `azTheme`

The bloated `azSettings` function has been destroyed. **Furthermore, `azTheme` has been entirely eradicated.** Your app's `MaterialTheme` now controls the colors. 

Parameters like `expandedWidth`, `collapsedWidth`, and `showFooter` have been absorbed into `azConfig` and the `@App` annotation.

## Phase 4: The Help/Info System Overhaul

The help system has been promoted to a first-class citizen with explicit triggers and scrollable, interactive info cards.

### 1. Explicit Help triggers
The "automatic" FAB to exit help mode has been removed. Help mode must now be explicitly enabled in `azAdvanced` or `azSettings`.

*   **DSL:** `infoScreen` → `helpEnabled`, `onDismissInfoScreen` → `onDismissHelp`.
*   **Automatic Menu Item:** If `helpEnabled = true`, a "Help" item is automatically appended to your side menu.
*   **Dedicated Rail Item:** Use `azHelpRailItem(id, text)` to add an explicit help trigger to the rail.

### 2. Interactive Info Cards
Info cards in the Help Overlay are now scrollable and support tap-to-expand. To dismiss the overlay, tap anywhere on the dark background.

## Phase 5: Submit to the Synthetic Tongue

The `AzNavRailScope` has been stripped of its human conveniences. If you are dynamically creating items that the annotations cannot reach, you must use the explicit, monolithic function signatures. Look at `DSL.md` for the exact parameters.

### New: Interactive Callback

`azAdvanced()` and `azSettings()` now accept an optional `onInteraction` parameter:

```kotlin
azAdvanced(
    onInteraction = { itemId, navItem ->
        Log.d("Rail", "User interacted with $itemId: ${navItem.text}")
    }
)
```

This fires whenever a user interacts with any rail item (tap, toggle, cycler advance, nested rail open, reloc drag), enabling analytics integration without per-item callbacks.

### Bottom Sheet Behavior Changes

- **Swipe-down** now calls `snapTo(HIDDEN)`, dismissing the sheet entirely instead of stepping one detent.
- **PEEK detent** now has a transparent tap overlay that calls `stepDown()` — tapping above the sheet at PEEK dismisses it to HIDDEN.
- Scrim tap at HALF/FULL, back press, and swipe-up behavior are unchanged.

---

## Migrating from the scripted tutorial framework to status-driven guidance

The scripted scene/card **tutorial** framework has been **removed** and replaced by the reactive
**status-driven guidance** framework. Instead of authoring a linear script of scenes and cards, you
describe the app's userflow as a flowchart of **statuses** (string-id nodes defined by reactive
predicates) joined by **edges** (transitions that each carry an instruction). You declare **goals**
(target statuses) and activate them on the `AzGuidanceController`; the engine shows the instruction to
reach the next status toward each active goal, **auto-advances the instant a target status becomes
true** (no Next button), re-routes live, and shows **every active goal's** callout next to its control.

### Old → new mapping

| Old (scripted tutorial) | New (status-driven guidance) |
| :--- | :--- |
| `AzTutorial` / `scene(...)` / `card(...)` | `azStatus` / `azEdge` / `azGoal` + the guidance engine |
| `AzTutorialController.startTutorial(id, …)` | `AzGuidanceController.activate(id)` |
| `AzTutorialController.markTutorialRead(id)` | `AzGuidanceController.markReached(id)` |
| `azAdvanced(tutorials = …)` config | **removed** — declare `azStatus` / `azEdge` / `azGoal` in the host content lambda |
| Help-overlay **"Start Tutorial"** launch | **removed** — guidance is developer-activated, never launched from help |
| Advance conditions (`Button` / `TapTarget` / `TapAnywhere` / `Event`), variable/scene branching, checklist/media cards, `AzTutorialOverlay` | subsumed by the status graph + auto-advancing engine |
| Persistence key `az_navrail_read_tutorials` | `az_navrail_completed_goals` |

### The new DSL

The guidance DSL lives inside `AzHostActivityLayout { ... }` on Android (which now **returns** an
`AzGuidanceController`, also readable via `LocalAzGuidanceController.current` /
`rememberAzGuidanceController()`); on React/web it is expressed as JSX children of the rail with the
`useAzGuidanceController()` hook.

- `azStatus(id) { predicate }` / `<AzStatus id predicate />` — declare a custom flowchart node.
- `azEdge(from, to = null, text, title = null, highlightItemId = null)` /
  `<AzEdge from to? text title? highlightItemId? />` — a transition carrying instruction `text`. You
  hand-author edges only into your **custom** statuses; rail affordances are auto-edged.
- `azGoal(id, target, label = null, autoStartWhen = null)` /
  `<AzGoal id target label? autoStartWhen? />` — a target status; inert until activated.
  `autoStartWhen` self-activates onboarding goals.

The controller surface (identical on both platforms): `enabled`, `activeGoals`, `completedGoals`,
`enable()`, `disable()`, `activate(id)`, `deactivate(id)`, `markReached(id)`, `isCompleted(id)`.

~~~kotlin
// Android — AzHostActivityLayout returns the controller
val controller = AzHostActivityLayout(navController = nav, currentDestination = route) {
    azStatus("profileComplete") { viewModel.profile.isComplete }
    azEdge(from = "az.screen.feature-a", to = "profileComplete",
           text = "Finish setup.", highlightItemId = "feature-a")
    azGoal(id = "onboarding", target = "profileComplete",
           autoStartWhen = { !guidance.isCompleted("onboarding") })
}
controller.activate("onboarding")
~~~

~~~tsx
// React Native / Web
const ctrl = useAzGuidanceController();
ctrl.activate('onboarding');
// <AzStatus id="profileComplete" predicate={() => viewModel.profile.isComplete} />
// <AzEdge from="az.screen.feature-a" to="profileComplete" text="Finish setup." highlightItemId="feature-a" />
// <AzGoal id="onboarding" target="profileComplete" autoStartWhen={() => !done.includes('onboarding')} />
~~~

### Built-in statuses, auto-edges, and predicate timing

The rail auto-publishes `az.*` statuses (`az.app.ready`, `az.rail.expanded` / `az.rail.collapsed`,
`az.rail.floating`, `az.host.<id>.expanded`, `az.screen.<route>`, `az.item.<id>.active`,
`az.nestedRail.<id>.open`, `az.help.open`, `az.onscreen.<id>.visible`) and generates auto-edges for
rail affordances ("Open the menu" for `az.rail.collapsed → az.rail.expanded`, tap host →
`az.host.<id>.expanded`, tap nested-rail → `az.nestedRail.<id>.open`, tap routed item →
`az.screen.<route>`). Predicates over Compose/React state are observed instantly; predicates over
non-reactive sources (a `StateFlow.value`, a plain `var`, a `ref`, an external store) are observed
within a ~300 ms poll. Goal activation is developer-driven — there is no built-in end-user picker.

### Persistence

Completed goals persist under key `az_navrail_completed_goals` (`SharedPreferences` on Android,
`localStorage` / `AsyncStorage` on React/RN), replacing the old `az_navrail_read_tutorials` read-state
key.

### React/web overlay parity note

The React/web overlay draws an **accent ring** around each target over a light dim, rather than a true
punch-out spotlight. Routing and advancement are identical to Android.

---

## Replacing a custom bottom sheet with `AzBottomSheetWindowHost`

AzNavRail's bottom-sheet shell is a verbatim port of LogKitty's: four detents (`HIDDEN` / `PEEK` / `HALF` / `FULL`), accumulated-delta vertical drag, scrim-tap to step down, optional horizontal-swipe callbacks, `TYPE_APPLICATION_OVERLAY` for the sheet window, and a separate `TYPE_ACCESSIBILITY_OVERLAY` for the nav-bar color sync. Migrating a custom sheet preserves the exact look frame-for-frame.

### Before (custom shell inside a Service)

```kotlin
class LogKittyOverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    private val controller = SheetController()
    private lateinit var composeView: ComposeView
    private lateinit var sheetParams: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        composeView = ComposeView(this).apply {
            setContent { LogKittyTheme { LogBottomSheet(controller, viewModel, /* … */) } }
        }
        sheetParams = WindowManager.LayoutParams(
            MATCH_PARENT, sheetHeight, TYPE_APPLICATION_OVERLAY,
            FLAG_LAYOUT_NO_LIMITS or FLAG_LAYOUT_IN_SCREEN or FLAG_NOT_TOUCH_MODAL or FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM }
        windowManager.addView(composeView, sheetParams)
        setupNavBarDecoration(viewModel, navBarHeightPx)

        serviceScope.launch {
            controller.detentFlow.collect { detent ->
                sheetParams.height = heightForDetent(detent, /* … */)
                windowManager.updateViewLayout(composeView, sheetParams)
            }
        }
    }
}
```

### After (AzNavRail shell)

```kotlin
class LogKittyOverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    private val controller = AzSheetController(initial = AzSheetDetent.HIDDEN)
    private lateinit var sheetHost: AzBottomSheetWindowHost

    override fun onCreate() {
        super.onCreate()
        sheetHost = AzBottomSheetWindowHost(
            context = this,
            controller = controller,
            config = AzSheetConfig(
                backgroundColor = Color(viewModel.backgroundColor.value),
                backgroundAlpha = viewModel.overlayOpacity.value,
                peekDp = with(resources.displayMetrics) { (peekHeightPx / density).dp },
                halfFraction = 0.5f,
                fullFraction = 0.9f,
            ),
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this,
            navBarHeightPx = resources.getDimensionPixelSize(
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            ),
        ) { LogKittyTabs(viewModel, onSave = { /* … */ }, onSettings = { /* … */ }) }
        sheetHost.attach()
    }

    fun onAccessibilityServiceBound() {
        sheetHost.attachNavBarDecor()
    }

    override fun onDestroy() {
        sheetHost.detach()
        super.onDestroy()
    }
}
```

### Files LogKitty can delete after migration

- `ui/LogBottomSheet.kt`
- `ui/SheetController.kt`
- `services/SheetGestures.kt` (or wherever the accumulated-delta drag lived)
- The inline `setupNavBarDecoration(...)` block in `services/LogKittyOverlayService.kt`
- The `controller.detentFlow.collect { ... }` window-resize bridge

LogKitty keeps its Service, theme, ViewModel, and accessibility service, and the log-rendering composable it now passes as the `content` lambda.

### Behavior parity checklist

- Drag accumulator threshold: tune via `AzSheetConfig.dragThresholdDp` if your test gestures behave differently.
- WindowManager flag set: the defaults in `AzBottomSheetWindowHost` copy LogKitty's flag set exactly. If you previously set extra flags, file an issue describing them — we may add a config hook.
- Animation: the system-overlay flavor hard-jumps detent heights (no `animateDpAsState`) to match LogKitty's existing look. The in-tree flavor animates by default.
- Back press: `config.collapseOnBack = true` (default) wires a `setOnKeyListener` for `KEYCODE_BACK` that steps down only while the window is focusable (HALF/FULL).
