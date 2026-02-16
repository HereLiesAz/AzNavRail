plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("maven-publish") // REQUIRED FOR JITPACK
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.2.21-2.0.5")
    implementation("com.squareup:kotlinpoet:2.1.0")
    implementation("com.squareup:kotlinpoet-ksp:2.1.0")
    implementation(project(":aznavrail-annotation"))
}

// JITPACK PUBLISHING SCRIPT
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
                groupId = "com.github.HereLiesAz.AzNavRail"
                artifactId = "aznavrail-processor"
                version = "1.0"
            }
        }
    }
}
