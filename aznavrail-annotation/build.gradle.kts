plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("maven-publish") 
}

// FORCE UNIFIED POM VERSIONING
group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: "1.0"

dependencies {
    implementation(kotlin("stdlib"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
                artifactId = "aznavrail-annotation"
            }
        }
    }
}
