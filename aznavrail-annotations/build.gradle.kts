// The annotations are a plain JVM jar: they are read by the KSP processor at compile time and never
// ship any Android or Compose types, so a consumer can depend on them from any module.
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "aznavrail-annotations"
        }
    }
}
