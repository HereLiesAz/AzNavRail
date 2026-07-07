# AzNavRail — Compose Multiplatform

`aznavrail-cmp` is the [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
port of [AzNavRail](../README.md) — the opinionated, DSL-driven navigation rail. It shares the
Android library's API and look, but runs from a single `commonMain` source set across **Android,
Desktop (JVM), Web (wasmJs), and iOS**.

> The Android-only `aznavrail` artifact remains the right choice for pure-Android apps. Use
> `aznavrail-cmp` when you target multiple platforms from shared Compose code.

## Install

JitPack builds every tagged release. Add the repo (settings.gradle.kts):

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then depend on the module from your `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.github.HereLiesAz.AzNavRail:aznavrail-cmp:VERSION") // latest release tag
        }
    }
}
```

## Supported targets

| Target | Status |
|---|---|
| Android | ✅ |
| Desktop / JVM | ✅ |
| Web / `wasmJs` | ✅ |
| iOS (`iosArm64`, `iosSimulatorArm64`) | ✅ (compiled on macOS; CI configures it on Linux) |

`iosX64` (the Intel iOS simulator) is intentionally omitted.

## Minimal usage

Unlike the Android artifact, there is **no `AzHostActivityLayout`** in the CMP module (it's built on
`Activity`/`Window`, which don't exist off-Android). Instead you provide a couple of
CompositionLocals yourself. Only **`LocalAzNavHostPresent provides true`** is strictly required —
without it the rail renders a red "Configuration Error" placeholder. Everything else has safe
defaults.

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.LocalAzNavHostPresent
import com.hereliesaz.aznavrail.LocalAzAppMeta
import com.hereliesaz.aznavrail.AzAppMeta
import com.hereliesaz.aznavrail.LocalAzNavHostScope
import com.hereliesaz.aznavrail.rememberAzNavHostScope

@Composable
fun App() = MaterialTheme {
    CompositionLocalProvider(
        LocalAzNavHostPresent provides true,                                   // required
        LocalAzAppMeta provides AzAppMeta(                                      // optional: name/icon/repo
            name = "My App",
            packageId = "com.example.myapp",                                   // enables About repo derivation
        ),
        LocalAzNavHostScope provides rememberAzNavHostScope(),                 // optional: enables About/Help/More overlays
    ) {
        AzNavRail {
            azRailItem(id = "home", text = "Home", onClick = { /* … */ })
            azMenuItem(id = "about", text = "About", onClick = { /* … */ })
        }
    }
}
```

### CompositionLocals

| Local | Required? | Purpose / default |
|---|---|---|
| `LocalAzNavHostPresent` | **Yes** — set `true` | The rail refuses to render (red error box) when `false` (default). |
| `LocalAzAppMeta` | Optional | App name / icon / package id. Default is a placeholder `"App"`. `packageId` lets the About screen auto-derive the GitHub repo. |
| `LocalAzNavHostScope` | Optional | Provide `rememberAzNavHostScope()` to make the About / Help / More-from-Az overlays function. Default `null` = overlays disabled. |
| `LocalAzSafeZones` | Optional | Safe-area insets. Has a working default. |
| `LocalAzGuideStrings` | Optional | Localized guidance strings (English defaults). |

## Differences from the Android artifact

- **No `AzNavHost` / `AzHostActivityLayout` / window-overlay Services** — Android platform glue with
  no cross-platform analogue. You mount `AzNavRail` under your own root composable and provide the
  CompositionLocals above.
- **Material icons are inlined** (see `internal/AzIcons.kt`) so the module needs no
  `material-icons-extended` dependency (which has no iOS variant). The glyphs are identical.
- **Networking** (About docs + More-from-Az carousel) uses [Ktor](https://ktor.io/) with a two-tier
  cache: an in-memory hot tier plus a best-effort persistent tier
  ([multiplatform-settings](https://github.com/russhwolf/multiplatform-settings)). Bodies over ~6 KB
  aren't persisted (storage-backend limits); everything still works within a session.
- **Guidance/tutorial completion is session-only** unless you persist it yourself (the Android
  sibling uses `SharedPreferences`).
- On **wasmJs (browser)** some cross-origin HEAD probes (repo icon/banner resolution) may be blocked
  by CORS; the code degrades gracefully (OpenGraph fallback / blank banner).

## Runnable demo

See [`aznavrail-cmp-demo`](../aznavrail-cmp-demo) for a minimal desktop + wasmJs app that mounts the
rail. Run the desktop demo with `./gradlew :aznavrail-cmp-demo:run` and the web demo with
`./gradlew :aznavrail-cmp-demo:wasmJsBrowserDevelopmentRun`.
