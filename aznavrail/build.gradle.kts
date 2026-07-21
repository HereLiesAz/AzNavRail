plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    alias(libs.plugins.parcelize)
}

// Unifying version across all modules via the version catalog
group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

android {
    namespace = "com.hereliesaz.aznavrail"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.material.icons.extended)

    api(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)


    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Documentation management
tasks.register<Copy>("extractDocs") {
    description = "Extracts the AzNavRail Complete Guide to the project's docs directory."
    group = "documentation"
    from("src/main/resources/AZNAVRAIL_COMPLETE_GUIDE.md")
    into("${project.rootDir}/docs")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                pom {
                    name.set("AzNavRail")
                    description.set("An opinionated, DSL-driven navigation rail for Jetpack Compose.")
                    url.set("https://github.com/HereLiesAz/AzNavRail")
                    // jitpack only emits a <licenses> block in the generated POM when the
                    // publication declares one; without it licensee/scanners report "no license".
                    // The name is the SPDX id and the url is the canonical license URL so licensee
                    // resolves it cleanly.
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
    }
}
