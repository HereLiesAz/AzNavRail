plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")

}

android {
    namespace = "com.hereliesaz.aznavrail"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
}

dependencies {
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
}
publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"]) // For a standard JVM library
                // artifact(tasks.jar) // If you need a specific JAR
                // artifact(tasks.sourcesJar) // If you want to publish sources
            }
        }
        repositories {
            maven {
                // For Maven Central, you'll configure Sonatype OSSRH
                // For a local Maven repository:
                // url = uri("${layout.buildDirectory.get()}/repo")
            }
        }
    }
