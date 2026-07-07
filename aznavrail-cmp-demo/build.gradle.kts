// See the sibling `aznavrail-cmp/build.gradle.kts` for why the `compose.*` accessors are used
// despite their deprecation warning — they resolve the correct per-artifact CMP versions.
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Minimal runnable demo for the `aznavrail-cmp` library — proves `AzNavRail` actually renders (not
// just compiles) on Desktop (JVM) and Web (wasmJs), and doubles as the consumer reference. It is NOT
// published and declares no Android target (keeping it off the Android build path and Linux-clean).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // `binaries.executable()` (an app, unlike the library) emits the `.js`/`.wasm` bundle and
        // registers the `wasmJsBrowserDevelopmentRun` task.
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // The library under demonstration, consumed as an in-build project.
                implementation(project(":aznavrail-cmp"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
        val desktopMain by getting {
            dependencies {
                // Compose Desktop runtime + Swing/Skiko window host for the current OS.
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Desktop `application { }` block — provides the `run` task and names the entry point generated from
// `desktopMain/kotlin/main.kt` (top-level `fun main()` → `…demo.MainKt`).
compose.desktop {
    application {
        mainClass = "com.hereliesaz.aznavrail.demo.MainKt"
    }
}
