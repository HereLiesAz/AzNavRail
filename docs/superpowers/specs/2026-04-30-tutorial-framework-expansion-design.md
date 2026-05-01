# AzNavRail Tutorial Framework Expansion & Help/Info Fixes

**Date:** 2026-04-30
**Scope:** Android (Kotlin/Compose) · React Native · React Web

---

## Goals

1. Fix the help/info overlay implementation across all platforms (React Native behavior mismatch, Web missing entirely).
2. Fix and expand the tutorial framework with advance conditions, branching, new card types, and platform-native persistence.
3. Bring the React Web library to an exact feature port of the Android library (TypeScript throughout, no `.jsx` files).

---

## Section 1: Updated Tutorial Data Model

All changes are additive. Existing DSL usage is fully backward-compatible.

### 1.1 `AzAdvanceCondition`

New sealed class (Kotlin) / discriminated union (TypeScript). Added to both `AzTutorial.kt` and `types.ts`.

```kotlin
sealed class AzAdvanceCondition {
    object Button     : AzAdvanceCondition()   // tap Next button (default, existing behavior)
    object TapTarget  : AzAdvanceCondition()   // tap the highlighted item
    object TapAnywhere: AzAdvanceCondition()   // tap anywhere on the overlay
    data class Event(val name: String) : AzAdvanceCondition()  // app calls fireEvent("name")
}
```

```typescript
export type AzAdvanceCondition =
  | { type: 'Button' }
  | { type: 'TapTarget' }
  | { type: 'TapAnywhere' }
  | { type: 'Event'; name: string };
```

### 1.2 `AzCard` — new fields

| Field | Type | Default | Purpose |
|---|---|---|---|
| `advanceCondition` | `AzAdvanceCondition` | `Button` | What triggers card advance |
| `branches` | `Map<String, String>` | `emptyMap()` | For `TapTarget` only: `itemId → sceneId`; ignored for all other advance conditions |
| `mediaContent` | `(@Composable () -> Unit)?` | `null` | Optional content rendered between title and text |
| `checklistItems` | `List<String>?` | `null` | If set, all items must be checked before Next enables |

`checklistItems` and `advanceCondition` are mutually exclusive in practice: if `checklistItems` is non-null, the Next button is disabled until all items are checked regardless of `advanceCondition`.

### 1.3 `AzScene` — new fields for variable-based branching

| Field | Type | Default | Purpose |
|---|---|---|---|
| `branchVar` | `String?` | `null` | Variable name to check when this scene becomes current |
| `branches` | `Map<String, String>` | `emptyMap()` | `variableValue → sceneId`; evaluated before showing first card |

When a scene becomes current and `branchVar` is non-null: look up `variables[branchVar]`, check `branches` for a match. If found, navigate to the target scene instead of showing this scene's cards. The redirected-from scene fires no `onComplete`. This makes scenes usable as invisible branch nodes.

The `AzSceneBuilder` DSL gains a `branch()` function to set these:
```kotlin
scene(id = "branch-gate", content = { /* empty backdrop */ }) {
    branch(varName = "userLevel", mapOf("advanced" to "scene-advanced", "basic" to "scene-basic"))
    // No card() calls needed; this scene acts purely as a redirect
}
```

### 1.4 `AzTutorial` — new fields

| Field | Type | Default | Purpose |
|---|---|---|---|
| `onComplete` | `(() -> Unit)?` | `null` | Fired when the last scene completes |
| `onSkip` | `(() -> Unit)?` | `null` | Fired when user taps Skip Tutorial |

### 1.5 DSL examples

```kotlin
// Advance condition + tap-target branching
card(
    title = "Pick a path",
    text = "Tap the item you want to learn about.",
    highlight = AzHighlight.Item("nav-menu"),
    advanceCondition = AzAdvanceCondition.TapTarget,
    branches = mapOf(
        "settings-btn" to "scene-settings",
        "profile-btn"  to "scene-profile"
    )
)

// Variable-based branching (scene level)
scene(id = "branch-gate", content = { /* empty backdrop */ }) {
    branch(varName = "userLevel", mapOf("advanced" to "scene-advanced", "basic" to "scene-basic"))
    // No card() calls needed; this scene acts purely as a redirect node
}

// Checklist card
card(
    title = "Before you continue",
    text = "Confirm the following:",
    checklistItems = listOf("I have read the docs", "I have set up my account")
)

// Event-driven advance
card(
    title = "Open the menu",
    text = "Swipe right or tap the rail header to open the menu.",
    highlight = AzHighlight.Item("rail-header"),
    advanceCondition = AzAdvanceCondition.Event("menu_opened")
)

// Media card
card(
    title = "The Rail",
    text = "The rail sits on the left (or right) edge of your screen.",
    mediaContent = { Image(painter = painterResource(R.drawable.rail_diagram), contentDescription = null) }
)
```

---

## Section 2: Controller & Persistence

### 2.1 New controller methods

```kotlin
// startTutorial gains optional variables map
fun startTutorial(id: String, variables: Map<String, Any> = emptyMap())

// Signals the overlay that a named event occurred
fun fireEvent(name: String)
```

`variables` is stored internally on the controller and passed to `AzTutorialOverlay` at composition time. `fireEvent()` sets a `pendingEvent: String?` observable state; the overlay consumes and clears it when the current card's condition matches.

### 2.2 Persistence strategy

**Same key on all platforms:** `az_navrail_read_tutorials` → JSON array of tutorial ID strings.

| Platform | Storage | Notes |
|---|---|---|
| Android | `SharedPreferences` (`az_tutorial_prefs`) | No new dependency. Loaded in `rememberAzTutorialController()` via `LocalContext.current`. Written synchronously in `markTutorialRead()`. |
| React Native | `@react-native-async-storage/async-storage` | Loaded in `AzTutorialProvider` `useEffect` on mount. Written on each `markTutorialRead()`. |
| Web | `localStorage` | Synchronous. Read on controller init, written inline. |

`rememberSaveable` on Android is retained for config-change/process-death survival within a session. SharedPreferences handles cross-launch survival.

---

## Section 3: Overlay Behavior

### 3.1 Advance condition handling

**Button (default):** No change. Next button tapped → advance card or scene.

**TapTarget:** The punch-out area is wrapped in a clickable region. Item ID is sourced from `currentCard.highlight`. If highlight is `AzHighlight.Item(id)`: on tap, check `currentCard.branches[id]`; if match found → navigate to that scene and reset card index to 0; if no match → advance linearly. If highlight is anything other than `AzHighlight.Item` (Area, FullScreen, None), `TapTarget` degrades to `TapAnywhere` — any tap on the overlay advances linearly.

**TapAnywhere:** The dimmed overlay layer is clickable. Any tap → advance linearly.

**Event(name):** Overlay observes `controller.pendingEvent`. When value equals current card's event name → advance linearly and clear `pendingEvent`.

### 3.2 Variable branching (scene-level)

Evaluated each time `currentSceneIndex` changes (via `LaunchedEffect(currentSceneIndex)`):
1. Get `currentScene.branchVar`. If null, proceed normally.
2. Look up `variables[branchVar]`.
3. Check `currentScene.branches[value]`. If match → set `currentSceneIndex` to index of target scene (looked up by scene ID). No `onComplete` fired for the skipped scene.
4. If no match → show scene's cards normally.

Circular branch detection: if a branch target resolves back to a scene already visited in the current navigation step, log a warning and advance to `currentSceneIndex + 1` (the next scene in list order) to avoid an infinite loop. If `currentSceneIndex + 1` is out of bounds, end the tutorial.

### 3.3 Checklist cards

- Local `checkedIndices: Set<Int>` state per card (reset when card changes).
- Each checklist item renders as a row with a checkbox.
- Next button: `enabled = checkedIndices.size == checklistItems.size`.
- Checking/unchecking updates `checkedIndices` via `remember { mutableStateOf(...) }`.

### 3.4 Media cards

- `mediaContent` composable renders between `title` and `text` inside the card.
- Constrained to `heightIn(max = 120.dp)` with `clip(RoundedCornerShape(8.dp))`.

### 3.5 Card auto-positioning

- Default: `Alignment.BottomCenter`.
- If the highlight punch-out's center Y > 60% of screen height → card floats to `Alignment.TopCenter` instead.
- Prevents the card from covering the highlighted area.

---

## Section 4: Help/Info Fixes

### 4.1 Android

- Delete `HelpOverlay.kt.orig` (stale backup).
- No behavioral changes needed; Android is the reference implementation.

### 4.2 React Native (`HelpOverlay.tsx`)

**Current behavior (bug):** Any tap on a card that has a tutorial immediately calls `startTutorial()` and dismisses the help overlay.

**Correct behavior (matching Android):**
- Collapsed card: show `"Tutorial available"` hint text below the truncated description.
- Expanded card: show `"Start Tutorial"` button at the bottom of the card content.
- Only tapping `"Start Tutorial"` calls `tutorialController.startTutorial(id)` and dismisses the help overlay.

### 4.3 Web — full rewrite

`HelpOverlay.jsx` → `HelpOverlay.tsx`. Plain JavaScript removed; TypeScript throughout.

New props:
```typescript
tutorials?: Record<string, AzTutorial>
onTutorialLaunch?: (id: string) => void
```

Same expand/collapse behavior and `"Tutorial available"` hint as Android. Tutorial launch only on explicit `"Start Tutorial"` button tap.

**New file: `web/AzTutorialOverlay.tsx`**
- CSS highlight via `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)` on an absolutely-positioned element sized and positioned to match target bounds.
- Falls back to full-screen dim with no punch-out if bounds are unavailable.
- All four advance conditions implemented.
- Checklist cards, media content, auto-positioning — same behavior as Android.
- `getBoundingClientRect()` for item bounds lookup.

**New file: `web/AzTutorialController.tsx`**
- React Context + Provider.
- `localStorage` persistence on init and `markTutorialRead()`.
- Exports `useAzTutorialController()` hook.
- API matches React Native exactly.

---

## Section 5: React Web Exact Port — Feature Parity

### 5.1 Parity table

| Feature | Android | React Native | Web (current) | Web (after) |
|---|---|---|---|---|
| `AzHighlight` (Area/Item/FullScreen/None) | ✓ | ✓ | ✗ | ✓ |
| `AzAdvanceCondition` | ✓ | ✓ | ✗ | ✓ |
| Tap-target branching | ✓ | ✓ | ✗ | ✓ |
| Variable branching | ✓ | ✓ | ✗ | ✓ |
| Media cards | ✓ | ✓ | ✗ | ✓ |
| Checklist cards | ✓ | ✓ | ✗ | ✓ |
| Persistence | SharedPrefs | AsyncStorage | ✗ | localStorage |
| Card auto-position | ✓ | ✓ | ✗ | ✓ |
| `"Tutorial available"` hint + Start button | ✓ | fix | ✗ | ✓ |
| `fireEvent()` | ✓ | ✓ | ✗ | ✓ |
| TypeScript throughout | ✓ | ✓ | ✗ | ✓ |

### 5.2 File changes summary

| File | Action |
|---|---|
| `aznavrail/src/.../tutorial/AzTutorial.kt` | Update: add `AzAdvanceCondition`, new fields on `AzCard`/`AzScene`/`AzTutorial`, update DSL builders |
| `aznavrail/src/.../tutorial/AzTutorialController.kt` | Update: `startTutorial(variables)`, `fireEvent()`, SharedPreferences persistence |
| `aznavrail/src/.../tutorial/AzTutorialOverlay.kt` | Update: all four advance conditions, branching, checklist, media, auto-position |
| `aznavrail/src/.../internal/HelpOverlay.kt` | Delete `.orig` backup |
| `aznavrail-react/src/types.ts` | Update: `AzAdvanceCondition`, new `AzCard`/`AzScene`/`AzTutorial` fields |
| `aznavrail-react/src/tutorial/AzTutorialController.tsx` | Update: `startTutorial(variables)`, `fireEvent()`, AsyncStorage persistence |
| `aznavrail-react/src/components/AzTutorialOverlay.tsx` | Update: all four advance conditions, branching, checklist, media, auto-position |
| `aznavrail-react/src/components/HelpOverlay.tsx` | Fix: correct tutorial launch flow (expand → hint → Start button) |
| `aznavrail-react/src/web/HelpOverlay.jsx` | Rewrite as `HelpOverlay.tsx` with TypeScript + tutorial integration |
| `aznavrail-react/src/web/AzTutorialOverlay.tsx` | New: CSS box-shadow highlight, all four advance conditions |
| `aznavrail-react/src/web/AzTutorialController.tsx` | New: Context + Provider + localStorage persistence |

### 5.3 Platform rendering differences (acceptable, not bugs)

| Aspect | Android | React Native | Web |
|---|---|---|---|
| Highlight cutout | `BlendMode.Clear` on Canvas | Massive border hack | `box-shadow` spread |
| Persistence API | SharedPreferences (sync) | AsyncStorage (async) | localStorage (sync) |
| Bounds lookup | `onGloballyPositioned` | `measureInWindow` | `getBoundingClientRect()` |

Same visual output and behavior; different platform mechanisms.

---

## Out of Scope

- Animated pointer/pulse highlight styles (from original proposal) — deferred.
- `AzHighlightMethod.PULSE` / `POINTER` — deferred.
- Mock data injection into scenes (`LocalTutorialMockData`) — deferred.
- Tutorial analytics / event tracking — deferred.
