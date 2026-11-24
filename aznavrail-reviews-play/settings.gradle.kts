// Standalone publication build for legacy and modern Android consumers.
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
    plugins { id("com.android.library") version "8.7.3" }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "aznavrail-reviews-play"
