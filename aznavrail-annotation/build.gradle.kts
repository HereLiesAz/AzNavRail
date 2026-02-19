plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("maven-publish") 
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

dependencies {
    implementation(kotlin("stdlib"))
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
                artifactId = "aznavrail-annotation"
                version = project.version.toString()
            }
        }
    }
}
