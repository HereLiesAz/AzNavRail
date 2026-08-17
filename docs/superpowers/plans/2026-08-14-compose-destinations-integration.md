# Compose Destinations Integration Plan

## Goal

Prove `compose-destinations` in the Android `SampleApp`. Preserve the existing `NavHostController`, route strings, rail selection, transitions, legacy routes, and public Android/CMP APIs. Any library adapter is later work with its own decision and scope.

## Non-goals

- Do not add `compose-destinations` to the published `aznavrail` API during the pilot.
- Do not migrate `aznavrail-cmp` until upstream Kotlin Multiplatform support is verified against every current target.
- Do not replace `AzHostActivityLayout`, the rail DSL, or `AzNavItem.route`.
- Do not change the existing AzNavRail KSP processor in the pilot.
- Do not remove legacy routes or make unrelated sample-screen changes.

## Constraints found in the repository

1. `SampleApp` already runs the AzNavRail KSP processor. Compose Destinations will be a second processor in that module.
2. `AzHostActivityLayout` and its rail scope must receive the same `NavHostController` used by the generated graph. A second controller would split navigation state from rail state.
3. Rail items and active-item matching use stable route strings. Generated destination types therefore need a narrow string bridge; they must not leak into the core rail model.
4. The sample has behavior keyed to the literal `bottom-sheet` route. Separately, its manual graph intentionally preserves a set of older route names. Generated routes must retain all of those values.
5. The Android library exposes AndroidX Navigation Compose in public signatures. Compose Destinations remains an application concern unless a later adapter proves generally useful.
6. The CMP module uses a separate navigation artifact across Android, desktop, Wasm, and iOS when built on macOS. Android success is not evidence of CMP compatibility.
7. The AzNavRail processor already generates an `AzNavHost` shell around `AzActivity.azGraphDestinations`. Changing that contract is a separate migration with consumer compatibility consequences.
8. `ARCHITECTURE.md`, required by repository instructions, is absent. Restore it with the required module boundaries, invariants, version, and decisions before implementation merges.

## Phase 0 — Verify the upstream contract

Before editing Gradle files, check the upstream README, setup guide, release notes, and compatibility table for the selected release.

- Pin one released Compose Destinations version compatible with this repository's Kotlin, KSP, Compose, and AndroidX Navigation versions. Do not use `+`, snapshots, or memory disguised as documentation.
- Confirm the correct runtime artifact and KSP processor coordinates for Android-only Compose.
- Confirm whether the selected release expects the generated graph host, generated destination specifications, or both.
- Confirm how explicit route names, arguments, deep links, transitions, and `NavHostController` interoperate with that release.
- Record the selected version and compatibility evidence in the implementation pull request.

**Exit gate:** dependency resolution succeeds and a one-screen generated graph compiles without modifying any published AzNavRail module.

## Phase 1 — Wire only `SampleApp`

1. Add a Compose Destinations version and aliases to `gradle/libs.versions.toml`.
2. Add the runtime dependency and KSP processor to `SampleApp/build.gradle.kts`; reuse the KSP plugin already applied there.
3. Keep `aznavrail/build.gradle.kts`, `aznavrail-cmp/build.gradle.kts`, and the AzNavRail processor dependencies unchanged.
4. Inspect generated sources and Gradle task output to prove both processors run without duplicate symbols, nondeterministic output, or task-order coupling.

**Exit gate:** clean debug compilation runs both KSP processors and the existing sample still launches.

## Phase 2 — Pilot a generated graph

Migrate `showcase-home`, `bottom-sheet`, and `forms` first. They cover a start destination, stateful screen wiring, and an argument-free leaf without touching every route at once.

1. Extract destination entry composables only where required by the annotation processor; keep screen implementations and state ownership unchanged.
2. Give every pilot destination an explicit route matching its current string exactly.
3. Host the generated graph inside the existing `onscreen` block.
4. Pass the `NavHostController` created by `MainApp` into the generated host. Never call `rememberNavController` for a second graph.
5. Preserve the current destination observer used by `AzHostActivityLayout` and the bottom-sheet effect.
6. Keep non-pilot destinations registered through a temporary, documented compatibility graph or generated wrappers, depending on the verified upstream API. There must be one back stack and one host.

**Exit gate:** the three pilot menu items navigate, highlight correctly, restore the bottom-sheet detent behavior, and return through the same back stack.

## Phase 3 — Establish the route bridge

Create an application-local adapter only if the generated API cannot expose the required stable string without repetition.

The bridge must:

- produce the exact legacy route string used by `azMenuItem` and `azRailItem`;
- support destinations with arguments without hand-building encoded route strings;
- keep generated destination types out of `aznavrail` public signatures;
- provide one source of truth for menu dispatch, active-route matching, effects, tests, and deep links;
- fail at compile time when a destination is removed or renamed where the upstream API permits it.

Do not generalize the adapter into the library until the full sample migration reveals a reusable contract. Three lines duplicated twice are not yet a framework. They are merely Tuesday.

**Exit gate:** no pilot route literal is independently repeated across destination declaration, rail declaration, and route-dependent behavior.

## Phase 4 — Migrate the remaining sample graph

1. Convert the remaining showcase destinations in small groups.
2. Preserve all routes currently marked as legacy.
3. Replace arbitrary string navigation callbacks with generated navigation calls where the destination set is closed and known.
4. Retain a deliberate string escape hatch only where the sample genuinely demonstrates dynamic routing.
5. Inventory arguments and deep links before converting them; add serialization tests before changing their route declarations.
6. Delete the manual `composable(...)` registration only after every route has a generated equivalent and a caller.

**Exit gate:** searching the sample finds no accidental manual duplicate graph, no orphaned route, and no second navigation controller.

## Phase 5 — Test the integration seam

Add tests at the application boundary rather than testing generated code for the generator's author.

### Required behavior tests

- Generated start destination renders `showcase-home`.
- Rail click navigates to a generated destination.
- Current-destination observation selects the correct rail item.
- System back and explicit pop return through the single shared back stack.
- `bottom-sheet` navigation still restores the expected detent.
- Every preserved legacy route resolves.
- Destination arguments and deep links round-trip using independently specified expected values.
- Process recreation restores the current destination where supported by the existing host.

### Required build checks

- Clean `SampleApp` unit tests.
- Clean Android debug assemble.
- Existing `aznavrail` unit tests.
- CMP compilation, proving the Android pilot did not leak into common code.
- KSP output inspection for both processors.

## Follow-up decision — Library API

This is not part of the integration implementation. Open a separate task after the sample migration, choose one outcome, and document why.

### Preferred default: no library dependency

Publish a recipe showing `DestinationsNavHost` inside `AzHostActivityLayout.onscreen`, sharing its controller and mapping generated destinations to rail route strings. This keeps AzNavRail navigation-library-neutral above its existing AndroidX dependency.

### Optional adapter: separate artifact

If multiple consumers need repeated glue, consider an `aznavrail-compose-destinations` artifact. It may contain host and route adapters but must not force Compose Destinations on current users.

### Last resort: core overload

Add an Android-only `AzNavHost` overload only if it preserves default AzNavRail transitions and can avoid exposing unstable generated implementation types. Treat it as public API: KDoc, binary-compatibility review, unit tests, and migration documentation are mandatory.

**Exit gate:** the chosen boundary has a stated reason, compatibility policy, and consumer example. Convenience alone is how dependencies become landlords.

## Follow-up decisions — Generators and CMP

Neither item is part of the integration implementation. Each requires a separate task.

### AzNavRail KSP processor

Evaluate whether generated Az graphs should delegate to a Compose Destinations graph only after the application integration is stable. Preserve `AzActivity.azGraphDestinations(NavGraphBuilder)` until a deprecation path and source-compatible replacement exist.

### Compose Multiplatform

Open a separate design task. Verify upstream processor/runtime support for Android, desktop, iOS, and Wasm; generated-source placement; Navigation Compose compatibility; and publication metadata. If any target is unsupported, retain the existing CMP host rather than creating target-specific API drift.

## Rollout and rollback

1. Land dependency wiring and the one-screen compile proof as one reversible change.
2. Land the three-screen pilot after the Phase 0 exit gate passes.
3. Land remaining route groups separately, each preserving explicit route strings.
4. Keep the prior manual graph recoverable until behavior tests cover the generated replacement.
5. Roll back by removing sample annotations/dependencies and restoring the manual graph; no published library ABI should change in phases 0–5.

## Definition of done

- One controller owns the graph, rail state, current destination, and back stack.
- Every current route, including legacy routes, remains reachable under the same external string.
- The sample uses generated destinations for its closed destination set.
- Route-dependent behavior and argument/deep-link serialization are tested.
- Both KSP processors run reliably from a clean checkout.
- Android library ABI and CMP source sets remain unchanged unless approved in a later, separately scoped decision.
- Documentation names the selected upstream version, compatibility evidence, integration recipe, and rollback path.
