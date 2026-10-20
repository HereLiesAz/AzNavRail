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

val hostIsMac = System.getProperty("os.name")?.startsWith("Mac", ignoreCase = true) == true

kotlin {
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm("desktop")
    if (hostIsMac) {
        iosArm64()
        iosSimulatorArm64()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            // Android-only. The host application MUST depend on this artifact through
            // playImplementation, never implementation or githubImplementation.
            implementation("com.android.billingclient:billing:9.1.0")
        }
    }
}

android {
    namespace = "com.hereliesaz.aznavrail.subscriptions"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("AzNavRail subscriptions (Compose Multiplatform)")
            description.set("Opt-in Compose subscription gate; Google Play Billing on Android only.")
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
