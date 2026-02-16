plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("maven-publish") // REQUIRED FOR JITPACK
}

dependencies {
    implementation(kotlin("stdlib"))
}

// JITPACK PUBLISHING SCRIPT
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
                groupId = "com.github.HereLiesAz.AzNavRail"
                artifactId = "aznavrail-annotation"
                version = "1.0" // JitPack overrides this, but Gradle needs a value
            }
        }
    }
}
