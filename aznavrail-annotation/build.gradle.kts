plugins {
    // Put your Kotlin version back here (adjust if your root project uses a different version)
    id("org.jetbrains.kotlin.jvm") version "2.0.21" 
    id("maven-publish") 
}

// EXPLICITLY declare group and version here so the parent POM resolves the dependency
group = "com.github.HereLiesAz.AzNavRail"
version = "1.0" // JitPack overrides this dynamically

dependencies {
    implementation(kotlin("stdlib"))
}

// JITPACK PUBLISHING SCRIPT
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
