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
    id("maven-publish")
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

val coil3Version = libs.versions.coil3.get()
val navigationComposeCmpVersion = libs.versions.navigationComposeCmp.get()
val activityComposeVersion = libs.versions.activityCompose.get()
val ktorVersion = libs.versions.ktor.get()
val kotlinxSerializationVersion = libs.versions.kotlinxSerialization.get()
val multiplatformSettingsVersion = libs.versions.multiplatformSettings.get()

// Apple/iOS Kotlin/Native targets can only be COMPILED on a macOS host (they need the Xcode
// toolchain). Linux CI runners and JitPack (which builds on Linux) therefore can't build or publish
// them — `publishToMavenLocal` would fail trying to compile the iOS klibs. So the iOS targets (and
// their `iosMain` source set) are declared only on macOS: off-Mac, the module still builds and
// publishes Android + Desktop + Web + metadata, and iOS is built when compiling on a Mac. Publishing
// iOS artifacts to a repository needs a macOS-based pipeline, which is out of scope for the JitPack
// (Linux) release.
val hostIsMac = System.getProperty("os.name")?.startsWith("Mac", ignoreCase = true) == true

kotlin {
    jvmToolchain(17)

    androidTarget {
        // Publish the release variant so the JitPack/Maven `android` publication resolves.
        publishLibraryVariants("release")
    }

    jvm("desktop")

    // iOS: real devices (arm64) + Apple-Silicon simulators (simulatorArm64). iosX64 (the Intel
    // simulator) is omitted — every dependency ships arm64/sim variants and Intel simulators are
    // legacy. The Material icons the UI needs are inlined (see internal/AzIcons.kt) so no
    // iOS-incompatible `material-icons-extended` dependency is required. Declared only on macOS
    // (see `hostIsMac` above) so the Linux JitPack/CI publish doesn't try to compile Apple klibs.
    if (hostIsMac) {
        iosArm64()
        iosSimulatorArm64()
    }

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
                // Network layer (About docs + More-from-Az carousel). Ktor's no-arg `HttpClient()`
                // auto-selects whichever engine artifact is on each target's classpath, so
                // commonMain needs only the core + the JSON runtime; the per-target engines live in
                // the platform source sets below. JSON is parsed via the `JsonElement` runtime API
                // (no `@Serializable` DTOs), so the kotlinx-serialization compiler plugin isn't
                // needed.
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                // Cross-platform key-value store backing AzHttpCache's persistence layer. The
                // `-no-arg` variant provides a commonMain `Settings()` factory that resolves each
                // platform's store automatically (java.util.prefs on Desktop, SharedPreferences via
                // androidx-startup auto-context on Android, localStorage on wasmJs, NSUserDefaults on
                // iOS) — so no `Context` and no per-platform factory are needed.
                implementation("com.russhwolf:multiplatform-settings-no-arg:$multiplatformSettingsVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                // BackHandler's `actual` on Android delegates to androidx.activity's BackHandler.
                // This is the standard Android artifact (not the JetBrains multiplatform fork,
                // which doesn't publish a common BackHandler) so it only belongs in androidMain.
                implementation("androidx.activity:activity-compose:$activityComposeVersion")
                // Ktor engine for Android (wraps HttpURLConnection; no extra transitive deps).
                implementation("io.ktor:ktor-client-android:$ktorVersion")
            }
        }
        val desktopMain by getting {
            dependencies {
                // Ktor CIO engine — pure-Kotlin, works on the JVM desktop target.
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
        }
        val wasmJsMain by getting {
            dependencies {
                // Ktor JS engine (publishes a wasm-js variant) — routes through the browser fetch API.
                implementation("io.ktor:ktor-client-js:$ktorVersion")
            }
        }
        // `iosMain` is the intermediate source set the default hierarchy template creates for the
        // two iOS targets. It only exists when those targets are declared (macOS only, above), and
        // it's materialized lazily — so `val iosMain by getting` fails with "KotlinSourceSet with
        // name 'iosMain' not found". Gate on `hostIsMac` (matching the target declaration) and use
        // the generated `iosMain` accessor, which triggers creation.
        if (hostIsMac) {
            iosMain.dependencies {
                // Ktor Darwin engine (NSURLSession) for the iOS targets.
                implementation("io.ktor:ktor-client-darwin:$ktorVersion")
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
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Publishing. The KMP plugin auto-registers a MavenPublication per target
// (`kotlinMultiplatform`, `android`, `desktop`, `wasmJs`, and the iOS targets once added), so unlike
// the Android module's single `register<MavenPublication>("release")` we attach shared POM metadata
// across all of them. JitPack picks these up on the next release tag; consumers depend on
// `com.github.HereLiesAz.AzNavRail:aznavrail-cmp:<version>`. Sources jars are emitted by the KMP
// plugin by default.
publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("AzNavRail (Compose Multiplatform)")
            description.set("Compose Multiplatform port of the DSL-driven AzNavRail navigation rail.")
            url.set("https://github.com/HereLiesAz/AzNavRail")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
        }
    }
}
