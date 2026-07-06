import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

// Direct Maven coordinates for the JetBrains Compose Multiplatform artifacts. The `compose.runtime`
// / `compose.foundation` / etc. plugin accessors are deprecated in CMP 1.11+ ("Specify dependency
// directly") and Gradle Kotlin DSL script compilation on the KMP variant treats deprecation
// warnings as script errors, so declaring the coordinates explicitly avoids the whole class of
// script-compilation failures.
val cmpVersion = libs.versions.composeMultiplatform.get()

kotlin {
    jvmToolchain(17)

    androidTarget()

    jvm("desktop")

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:$cmpVersion")
                implementation("org.jetbrains.compose.foundation:foundation:$cmpVersion")
                implementation("org.jetbrains.compose.material3:material3:$cmpVersion")
                implementation("org.jetbrains.compose.ui:ui:$cmpVersion")
                implementation("org.jetbrains.compose.components:components-resources:$cmpVersion")
            }
        }
    }
}

// AGP 9 exposes the LibraryExtension via the `androidLibrary` name (new DSL) instead of the
// deprecated `android` extension. Under `-Werror`/strict-deprecation script compilation used by the
// KMP plugin, referencing `android { ... }` becomes an error; `androidLibrary { ... }` is the
// clean path.
androidLibrary {
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
