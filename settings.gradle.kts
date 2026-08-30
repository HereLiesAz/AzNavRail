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
include(":aznavrail")
include(":aznavrail-annotations")
include(":aznavrail-processor")
include(":aznavrail-cmp")
// SampleApp and the CMP demo are runnable apps, not published artifacts — neither appears in any
// module's `publishing {}` block. Building either one during a release is pure waste: SampleApp's
// release variant alone runs a full R8/multidex `mergeDexRelease`, which is heavy enough to exhaust
// JitPack's build-VM disk quota outright (`No space left on device` mid-dex). The demo's wasmJs
// executable additionally pulls in the Binaryen setup task (and node/yarn tooling) during `assemble`,
// registering project-level repositories in the process. JitPack sets JITPACK=true, so skip both
// there entirely; they stay included for CI (which builds/type-checks them) and local dev (where they
// run). (repositoriesMode is PREFER_SETTINGS above, so those tool repos are tolerated when the demo
// IS built off-JitPack.)
if (System.getenv("JITPACK") == null) {
    include(":SampleApp")
    include(":aznavrail-cmp-demo")
}
