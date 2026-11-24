import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.library")
    id("maven-publish")
}
group = "com.github.HereLiesAz.AzNavRail"
version = providers.gradleProperty("reviewVersion")
    .orElse(providers.environmentVariable("JITPACK_VERSION"))
    .orNull ?: rootProject.extensions.findByType<VersionCatalogsExtension>()
        ?.find("libs")?.orElse(null)?.findVersion("aznavrail")?.orElse(null)?.requiredVersion
        ?: "development"

android {
    namespace = "com.hereliesaz.aznavrail.reviews.play"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    publishing { singleVariant("release") { withSourcesJar() } }
}
dependencies {
    implementation("com.google.android.play:review:2.0.2")
    testImplementation("junit:junit:4.13.2")
}
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                pom {
                    name.set("AzNavRail Play review requester")
                    licenses { license { name.set("Apache-2.0"); url.set("https://www.apache.org/licenses/LICENSE-2.0.txt") } }
                }
            }
        }
    }
}
