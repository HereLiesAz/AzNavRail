// The KSP processor that turns `@Az` declarations into an `AzGraph` implementing `AzGraphInterface`.
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("maven-publish")
}

group = "com.github.HereLiesAz.AzNavRail"
version = System.getenv("JITPACK_VERSION") ?: libs.versions.aznavrail.get()

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":aznavrail-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "aznavrail-processor"
        }
    }
}
