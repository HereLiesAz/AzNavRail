pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AzNavRail"
include(":SampleApp")
include(":aznavrail")
include(":aznavrail-cmp")
// The demo is a runnable app, not a published artifact. Its wasmJs executable pulls in the Binaryen
// setup task (and node/yarn tooling) during `assemble`, needlessly building an app during a library
// release — and that setup registers project-level repositories. JitPack sets JITPACK=true, so skip
// the demo there entirely; it stays included for CI (which type-checks it) and local dev (where it
// runs). (repositoriesMode is PREFER_SETTINGS above, so those tool repos are tolerated when the demo
// IS built off-JitPack.)
if (System.getenv("JITPACK") == null) {
    include(":aznavrail-cmp-demo")
}
