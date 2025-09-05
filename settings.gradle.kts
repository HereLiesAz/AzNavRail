pluginManagement {
    plugins {
        id("com.android.application").version("8.12.2")
        id("com.android.library").version("8.12.2")
        id("org.jetbrains.kotlin.android").version("2.2.10")
        id("org.jetbrains.kotlin.plugin.compose").version("2.2.10")
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
