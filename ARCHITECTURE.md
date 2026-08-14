# Architecture

## Module boundaries

- `aznavrail` is the Android library. It owns the Android host, rail DSL, and AndroidX Navigation integration.
- `aznavrail-cmp` is the Compose Multiplatform library. Common code owns shared UI and navigation behavior; platform source sets contain platform glue.
- `aznavrail-annotations` contains source-retained declarations and has no UI runtime.
- `aznavrail-processor` turns `aznavrail-annotations` declarations into Android graph code.
- `SampleApp` is the Android integration consumer. Application-only navigation processors belong here unless a reusable library contract is proven.
- `aznavrail-cmp-demo` is the desktop and Wasm consumer. It is runnable, not published.

## Invariants

1. A host, its rail, and its navigation graph share one `NavHostController` and therefore one back stack.
2. Rail and menu routes remain stable strings; generated navigation types do not replace public route values.
3. `aznavrail-annotations` does not depend on Compose, AndroidX Navigation, or a generated-navigation runtime.
4. `aznavrail-processor` generates readable calls to the public AzNavRail DSL and adds no runtime behavior.
5. Compose Destinations remains consumer wiring until an adapter demonstrates a stable, reusable contract.
6. Android navigation integration does not enter `aznavrail-cmp` common source unless every published CMP target can compile it.
7. Existing KSP-generated `AzGraphInterface` consumers retain their source contract while Compose Destinations is introduced.
8. Generated destination graphs are checked before the Android build; UX errors block CI, while UX warnings remain visible in annotations, summaries, SARIF, and artifacts.

## Current version

AzNavRail `11.0`, as declared by `gradle/libs.versions.toml`.

## Decisions

### Keep navigation state singular

`AzHostActivityLayout` and the graph receive the same controller because two controllers split rail selection from navigation history.

### Pilot generated navigation in consumers

Compose Destinations starts in `SampleApp`, alongside the existing AzNavRail processor. Generated implementation types are volatile; putting them in a published signature would turn an integration detail into an ABI promise.

### Keep the CMP library navigation-runtime neutral

The CMP library exposes JetBrains Navigation Compose because its host signatures use those types. Compose Destinations is not added until its runtime and generated metadata compile for Android, desktop, iOS, and Wasm. One green Android build does not grant the other targets citizenship.

### Preserve the AzNavRail processor contract

The processor continues generating `AzGraphInterface` implementations that invoke the public DSL. Delegating generation to another processor would couple processing rounds and remove the current readable-output guarantee.

### Analyze user flows before KSP

The Compose Destinations UX analyzer reads Kotlin source before generated code exists because generated graphs prove that routes compile, not that users can escape them. It treats sealed-state variants, enum decisions, and navigation events as the behavioral graph. Errors fail CI; warnings remain non-blocking because conservative source analysis cannot see every dynamic route, but silence would be the more expensive lie.
