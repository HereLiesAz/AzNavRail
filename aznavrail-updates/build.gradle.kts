plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}
group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()
android {
    namespace = "com.hereliesaz.aznavrail.updates"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}
kotlin { jvmToolchain(17) }
dependencies { testImplementation(libs.junit) }
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                pom {
                    name.set("AzNavRail Android updates")
                    licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
                }
            }
        }
    }
}
