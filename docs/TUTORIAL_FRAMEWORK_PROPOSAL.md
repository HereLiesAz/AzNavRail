# Status-Driven Guidance Framework Reference

This document is the complete reference for the AzNavRail **status-driven guidance framework** as
shipped. It replaces the old scripted scene/card tutorial framework. Instead of authoring a linear
script of scenes and cards, the developer describes the app's userflow as a **flowchart of statuses
(nodes)** and **edges (transitions)**, declares one or more **goals**, and lets a reactive engine
figure out — moment to moment — which instruction to surface to reach each goal.

> **Migration note (old → new).** The scripted `AzTutorial` / scenes / cards model and the
> `AzTutorialController` are gone. See *Migration from the scripted tutorial framework* at the bottom
> of this document for the symbol-by-symbol mapping.

---

## Concept

The app (and the rail, automatically) describe the userflow as a **flowchart**:

- A **status** is a node — a named boolean condition that is currently either true or false.
- An **edge** is a transition from one status to another; it carries the **instruction** the user must
  follow to traverse it (e.g. "Tap Checkout").
- A **goal** is a target status the developer wants the user to reach.

The developer **activates** one or more goals. The engine observes which statuses are currently true
and, for each active goal, always shows the single instruction needed to reach the *next* status on a
shortest path toward that goal. It **auto-advances the instant a target status becomes true** — there
is no "Next" button — and it **re-routes live** if the user wanders off the path.

**Multiple goals can be active at once.** Every active goal's current instruction is shown
simultaneously, each rendered as a **callout placed adjacent to the control used to accomplish it**.
Instructions shared by more than one goal are de-duplicated.

---

## Status vocabulary

A "status" is a string id resolved uniformly from three sources:

1. A **developer predicate** registered with `azStatus(id) { ... }` — true while the lambda returns true.
2. An **active classifier name** — a status is true while that classifier is active on the rail.
3. A **built-in `az.*` id** — published automatically from live rail / host / route / help / onscreen
   state.

### Built-in `az.*` status ids

These are published automatically; you never declare them, you only reference them as edge/goal targets.

| Status id | True when |
| :--- | :--- |
| `az.app.ready` | Always true. It is the root, so navigation auto-edges always have a reachable `from`. |
| `az.rail.expanded` | The rail menu is expanded. |
| `az.rail.collapsed` | The rail is collapsed. (Mutually exclusive with `az.rail.expanded`.) |
| `az.rail.floating` | The rail is in FAB / floating mode. |
| `az.host.<id>.expanded` | The host item `<id>` is expanded. |
| `az.screen.<route>` | The current route is `<route>`. |
| `az.item.<id>.active` | The item `<id>` is the active/selected item. |
| `az.nestedRail.<id>.open` | The nested rail `<id>` popup is open. |
| `az.help.open` | The help overlay is open. |
| `az.onscreen.<id>.visible` | The on-screen element `<id>` is visible. |

---

## Edges

An edge declares how to get from one status to another and what instruction to show while you're at the
`from` status heading toward the `to` status.

- **Interactive edge** — `from` → `to` with instruction text. When the engine routes through this edge,
  it shows the text; when `to` becomes true the engine advances.
- **Passive edge** — `to = null`. It shows informational text while `from` holds, without targeting a
  next status. Use it for ambient hints.

### Auto-edges (generated for the rail's own affordances)

The engine **automatically generates edges for the rail's own controls**, so you never hand-author the
mechanics of opening the menu or tapping a rail item. You only hand-author edges *into your custom
statuses*.

| Auto-edge | Generated transition |
| :--- | :--- |
| "Open the menu" | `az.rail.collapsed → az.rail.expanded` |
| Tap a host item | → `az.host.<id>.expanded` |
| Tap a nested-rail item | → `az.nestedRail.<id>.open` |
| Tap a routed item | → `az.screen.<route>` |

Rail items are tappable from `az.app.ready`; **menu-only items require `az.rail.expanded`** first (so
the engine first instructs "Open the menu" if needed). Instruction text is **localizable** — on Android
via the string resources `az_guide_open_menu` / `az_guide_tap_item` (defaults "Open the menu" /
"Tap <label>").

---

## Goals

A **goal** names a target status the engine should drive the user toward.

- `azGoal(id, target, label?, autoStartWhen?)` declares a goal with a stable `id`, the `target` status
  it aims for, an optional human `label`, and an optional `autoStartWhen` status.
- **Goal activation is developer-driven.** Call `activate(goalId)` / `deactivate(goalId)` on the
  controller. There is **no built-in end-user goal picker**.
- `autoStartWhen` is optional onboarding-style **self-activation**: when the named status becomes true,
  the goal activates itself (e.g. activate a "first run" goal when `az.app.ready`). Pass `null` to opt
  out.

---

## The guidance controller

The controller is the imperative handle for enabling guidance and activating goals.

### Obtaining it

**Android.** `AzHostActivityLayout(...) { ... }` now **returns** an `AzGuidanceController`. Capture it:

```kotlin
val guidance = AzHostActivityLayout(navController = nav, currentDestination = route) {
    // ... DSL: items, azStatus, azEdge, azGoal ...
}
```

Inside any composable under the host you can also read it from the CompositionLocal:

```kotlin
val guidance = LocalAzGuidanceController.current
// or, at a composable root:
val guidance = rememberAzGuidanceController()
```

**React.** Get the controller anywhere under the rail with the hook:

```tsx
const guidance = useAzGuidanceController();
```

### Controller surface

| Member | Android | React | Description |
| :--- | :--- | :--- | :--- |
| `enabled` | `Boolean` | `boolean` | Whether guidance is on. |
| `activeGoals` | `List<String>` | `string[]` | Currently active goal ids. |
| `completedGoals` | `List<String>` | `string[]` | Goal ids reached at least once (persisted). |
| `enable()` / `disable()` | ✓ | ✓ | Turn guidance on/off. |
| `activate(goalId)` / `deactivate(goalId)` | ✓ | ✓ | Activate / deactivate a goal. |
| `markReached(goalId)` | ✓ | ✓ | Mark a goal reached (persists it as completed). |
| `isCompleted(goalId)` | ✓ | ✓ | Whether a goal has been completed. |

**Persistence.** Completed goals persist:

- **Android** — `SharedPreferences` file `az_tutorial_prefs`, key `az_navrail_completed_goals`.
- **React** — `localStorage` (and `AsyncStorage` on React Native) under key `az_navrail_completed_goals`.

---

## The engine

- **BFS routing.** The engine runs a breadth-first search over the status/edge graph and picks the
  **first edge on a shortest path** from the set of currently-true statuses to a goal's target. It
  re-routes live when the user wanders, and **auto-advances** (no Next button) the instant the target
  status becomes true.
- **Multiple simultaneous goals.** Every active goal contributes its current instruction; the engine
  renders them all at once as callouts adjacent to the relevant controls, and **de-duplicates**
  instructions shared by multiple goals.
- **Observation latency.** Predicates that read Compose snapshot state (Android) or React state are
  observed **instantly**. Predicates backed by a non-snapshot / non-React source — a `StateFlow.value`,
  a plain `var`, a mutable ref, or an external store — are observed within a **~300 ms poll**.

### Overlay rendering note (Android vs React)

Where **Android** punches a true spotlight hole per target, **React** draws an **accent ring** around
each target over a light dim (multi-hole masking isn't portable across React Native primitives).

---

## Android (Kotlin) DSL

Declared inside the `AzHostActivityLayout { ... }` content lambda, alongside your item builders.

```kotlin
fun azStatus(id: String, predicate: () -> Boolean)
fun azEdge(from: String, to: String? = null, text: String, title: String? = null, highlightItemId: String? = null)
fun azGoal(id: String, target: String, label: String? = null, autoStartWhen: String? = null)
```

### Example

```kotlin
import com.hereliesaz.aznavrail.tutorial.*

val guidance = AzHostActivityLayout(navController = nav, currentDestination = route) {
    azConfig(dockingSide = AzDockingSide.LEFT)

    azRailItem(id = "cart", text = "Cart", route = "cart")
    azRailItem(id = "checkout", text = "Checkout", route = "checkout")

    onscreen { AzNavHost(startDestination = "home") { /* … */ } }

    // 1. A custom developer status (predicate over your own state).
    azStatus("cart_open") { cart.isOpen }

    // 2. A hand-authored edge INTO a custom status, and one toward a built-in status.
    azEdge(from = "cart_open", to = "az.screen.checkout", text = "Tap Checkout", highlightItemId = "checkout")

    // 3. A passive edge (to = null) — ambient hint while the cart is open.
    azEdge(from = "cart_open", text = "Review your items before checking out.")

    // 4. A goal that drives the user to the confirmation screen.
    azGoal(id = "checkout", target = "az.screen.confirmation", label = "Check out")
}

// Activate the goal when you decide to (developer-driven).
guidance.activate("checkout")
```

Self-activating onboarding goal:

```kotlin
azGoal(
    id = "onboarding",
    target = "az.rail.expanded",
    label = "Take the tour",
    autoStartWhen = "az.app.ready"   // activates itself on first ready
)
```

---

## React (TypeScript) DSL

Declared as JSX children of the rail (under `AzHostActivityLayout` / `AzNavRail`).

```tsx
<AzStatus id="cart_open" predicate={() => cart.isOpen} />
<AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
<AzGoal id="checkout" target="az.screen.confirmation" label="Check out" autoStartWhen={null} />
```

`to` on `<AzEdge>` is optional — omit it for a passive edge.

### Example

```tsx
import {
    AzHostActivityLayout,
    AzRailItem,
    AzStatus,
    AzEdge,
    AzGoal,
    useAzGuidanceController,
} from '@HereLiesAz/aznavrail-react';

function Shell() {
    return (
        <AzHostActivityLayout dockingSide={AzDockingSide.LEFT}>
            <AzRailItem id="cart" text="Cart" route="cart" />
            <AzRailItem id="checkout" text="Checkout" route="checkout" />

            {/* custom status */}
            <AzStatus id="cart_open" predicate={() => cart.isOpen} />

            {/* interactive edge + passive edge */}
            <AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
            <AzEdge from="cart_open" text="Review your items before checking out." />

            {/* goal */}
            <AzGoal id="checkout" target="az.screen.confirmation" label="Check out" autoStartWhen={null} />
        </AzHostActivityLayout>
    );
}

function CheckoutLauncher() {
    const guidance = useAzGuidanceController();
    return <button onClick={() => guidance.activate('checkout')}>Guide me through checkout</button>;
}
```

Beyond the DSL components and the hook, the React package also exports `AzGuidanceProvider`,
`AzInstructionOverlay`, `useActiveStatuses`, `computeBuiltinStatuses`, `nextHop`, `routeInstructions`,
`computeAutoEdges`, and the types `AzGuideHighlight`, `AzCalloutSide`, `AzInstruction`, `AzGoalDef`,
`AzEdgeDef`, `AzStatusPredicate`, `AzStatusProps`, `AzEdgeProps`, `AzGoalProps`.

---

## Model types

Package `com.hereliesaz.aznavrail.tutorial` (Android); the React equivalents are exported from
`@HereLiesAz/aznavrail-react`.

| Type | Shape |
| :--- | :--- |
| `AzGuideHighlight` | Sealed: `None`, `FullScreen`, `Item(id)`, `Area(left, top, width, height)`. |
| `AzInstruction` | `(text, title?, highlight, side, media?)`. |
| `AzCalloutSide` | `Auto` / `Above` / `Below` / `Start` / `End`. |
| `AzEdge` | `(from, to?, instruction)`. |
| `AzGoal` | `(id, target, label?, autoStartWhen?)`. |

---

## Migration from the scripted tutorial framework

The old scripted scene/card framework has been **removed**. Map old concepts to new ones as follows:

| Old (removed) | New |
| :--- | :--- |
| `AzTutorial` / `AzScene` / `AzCard` | `azStatus` / `azEdge` / `azGoal` + the reactive engine |
| `AzTutorialController.startTutorial(id, …)` | `AzGuidanceController.activate(goalId)` |
| `AzTutorialController.markTutorialRead(id)` | `AzGuidanceController.markReached(goalId)` |
| `azAdvanced(tutorials = …)` | **removed** — declare statuses/edges/goals in the host content lambda |
| Help-overlay **"Start Tutorial"** launch | **removed** — guidance is developer-activated, never launched from help |
| Persistence key `az_navrail_read_tutorials` | `az_navrail_completed_goals` |

The advance conditions (`Button` / `TapTarget` / `TapAnywhere` / `Event`), variable/scene branching,
checklist and media cards, and the `AzTutorialOverlay` are all gone — their roles are subsumed by the
status graph and the auto-advancing engine.
