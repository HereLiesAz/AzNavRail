// The compose plugin's `compose.runtime` / `compose.material3` / etc. accessors are marked
// deprecated ("Specify dependency directly") but remain the ONLY correct way to reference the CMP
// artifacts: each artifact (material3, runtime, ui…) versions independently of the compose plugin
// version, so hardcoding a coordinate + a single version guesses wrong (e.g.
// `org.jetbrains.compose.material3:material3` tops out at a different version than the plugin). The
// accessors resolve the right per-artifact version automatically. `@file:Suppress("DEPRECATION")`
// keeps that deprecation from being promoted to a script-compilation error.
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

val coil3Version = libs.versions.coil3.get()
val navigationComposeCmpVersion = libs.versions.navigationComposeCmp.get()
val activityComposeVersion = libs.versions.activityCompose.get()

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    // iOS targets are intentionally omitted for now. The ported UI uses Material icons
    // (`androidx.compose.material.icons.*`) pervasively — hamburger, password-visibility toggle,
    // dropdown arrow, back, check — and `org.jetbrains.compose.material:material-icons-extended`
    // publishes android/desktop/wasmJs but NOT iOS (the androidx material-icons libraries are
    // deprecated and frozen without iOS support). Supporting iOS would require replacing every
    // `Icons.*` usage with an inlined `ImageVector` or adopting a maintained multiplatform icon
    // library — a dedicated follow-up. Android + Desktop + wasmJs all resolve every dependency and
    // share the same source, which is the multiplatform win this port delivers today.

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                // Coil 3 — multiplatform image loader (replaces Android-only Coil 2 in the CMP
                // port). Consumers can add `coil-network-ktor3`/`coil-network-okhttp` for URL
                // loading; the base `coil-compose` handles model→painter without an engine.
                implementation("io.coil-kt.coil3:coil-compose:$coil3Version")
                // JetBrains multiplatform fork of AndroidX Navigation — the API is
                // package-compatible with `androidx.navigation`, so files ported from the Android
                // sibling generally need no import changes.
                implementation("org.jetbrains.androidx.navigation:navigation-compose:$navigationComposeCmpVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                // BackHandler's `actual` on Android delegates to androidx.activity's BackHandler.
                // This is the standard Android artifact (not the JetBrains multiplatform fork,
                // which doesn't publish a common BackHandler) so it only belongs in androidMain.
                implementation("androidx.activity:activity-compose:$activityComposeVersion")
            }
        }
    }
}

// This project pins `android.newDsl=false` in `gradle.properties`, so the AGP 9 `androidLibrary`
// extension isn't registered — only the deprecated `android { }` DSL is available. The DSL
// generates a `w:` deprecation warning; with no `e:` errors left in this script that warning is
// tolerated and the script compiles.
android {
    namespace = "com.hereliesaz.aznavrail.cmp"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
