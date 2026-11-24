plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}
group = "com.github.HereLiesAz.AzNavRail"
version = providers.gradleProperty("updateVersion").get()
android {
    namespace = "com.hereliesaz.aznavrail.updates.play"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    publishing { singleVariant("release") { withSourcesJar() } }
}
dependencies {
    api(project(":aznavrail-updates-compat"))
    implementation("com.google.android.play:app-update:2.1.0")
}
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                pom {
                    name.set("AzNavRail Android Play update adapter (compatibility build)")
                    licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
                }
            }
        }
    }
}
