import java.util.Random

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.parcelize)
    id("maven-publish")
}

val generatedPin = (100000 + Random().nextInt(900000)).toString()

android {
    namespace = "com.hereliesaz.aznavrail"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "GENERATED_SEC_LOC_PIN", "\"$generatedPin\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

dependencies {
    api(project(":aznavrail-annotation"))

    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.material.icons.extended)
    api(libs.coil.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)

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

tasks.register("sendPinEmail") {
    doLast {
        println("--------------------------------------------------")
        println("Sending email to hereliesaz@gmail.com")
        println("Subject: Build PIN for AzNavRail")
        println("Body: The random PIN for this build is: $generatedPin")
        println("--------------------------------------------------")
    }
}

afterEvaluate {
    val debugTask = tasks.findByName("assembleDebug")
    if (debugTask != null) {
        debugTask.finalizedBy("sendPinEmail")
    }

    val releaseTask = tasks.findByName("assembleRelease")
    if (releaseTask != null) {
        releaseTask.finalizedBy("sendPinEmail")
    }
    
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.HereLiesAz"
                artifactId = "AzNavRail"
                version = "5.1"
            }
        }
    }
}

tasks.register<Copy>("extractDocs") {
    description = "Extracts the AzNavRail Complete Guide to the project's docs directory."
    group = "documentation"
    from("src/main/resources/AZNAVRAIL_COMPLETE_GUIDE.md")
    into("${project.rootDir}/docs")
}
