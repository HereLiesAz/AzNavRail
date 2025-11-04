pluginManagement {
    plugins {
        id("com.android.application").version("8.13.0")
        id("com.android.library").version("8.13.0")
        id("org.jetbrains.kotlin.android").version("2.2.21")
        id("org.jetbrains.kotlin.plugin.compose").version("2.2.21")
        id("org.jetbrains.kotlin.plugin.parcelize").version("2.2.21")
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
