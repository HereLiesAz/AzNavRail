plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("maven-publish") 
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.2.21-2.0.5")
    implementation("com.squareup:kotlinpoet:2.1.0")
    implementation("com.squareup:kotlinpoet-ksp:2.1.0")
    implementation(project(":aznavrail-annotation"))
}

kotlin {
    jvmToolchain(17)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["java"])
                groupId = project.group.toString()
                artifactId = "aznavrail-processor"
                version = project.version.toString()
            }
        }
    }
}
