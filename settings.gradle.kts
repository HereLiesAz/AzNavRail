pluginManagement {
    plugins {
        id("com.android.application").version("8.6.0")
        id("com.android.library").version("8.6.0")
        id("org.jetbrains.kotlin.android").version("2.0.0")
        id("org.jetbrains.kotlin.plugin.compose").version("2.0.0")
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

    }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
      maven { url = uri("https://jitpack.io") }

  }
}

rootProject.name = "AzNavRail"
include(":SampleApp")
include(":aznavrail")
