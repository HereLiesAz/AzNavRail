# aznavrail-cmp-demo

A minimal, runnable [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) app that
mounts [`aznavrail-cmp`](../aznavrail-cmp) on **Desktop (JVM)** and **Web (wasmJs)**. It exists to
prove the library *renders* (not just compiles) and to serve as the smallest possible consumer
reference — see [`App.kt`](src/commonMain/kotlin/com/hereliesaz/aznavrail/demo/App.kt) for the one
required piece of wiring (`LocalAzNavHostPresent provides true`).

This module is **not published** and declares no Android target.

## Run it

**Desktop window:**

```bash
./gradlew :aznavrail-cmp-demo:run
```

**Web (dev server, opens a browser tab):**

```bash
./gradlew :aznavrail-cmp-demo:wasmJsBrowserDevelopmentRun
```

## What it shows

`App()` wraps `AzNavRail` in a `CompositionLocalProvider` supplying:

| Local | Why |
|---|---|
| `LocalAzNavHostPresent provides true` | **Required** — otherwise the rail renders a red "Configuration Error" box. |
| `LocalAzAppMeta provides AzAppMeta(...)` | App name + `packageId` (lets the About screen derive its GitHub repo). |
| `LocalAzNavHostScope provides rememberAzNavHostScope()` | Enables the built-in About / Help / More-from-Az overlays. |

The rail itself is defined with the same DSL as the Android artifact: `azRailItem { }` /
`azMenuItem { }`.
