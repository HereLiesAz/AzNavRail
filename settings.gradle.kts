pluginManagement {
    plugins {
        id("com.android.application").version("8.6.0")
        id("com.android.library").version("8.6.0")
        id("org.jetbrains.kotlin.android").version("1.9.0")
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
include(":", ":SampleApp")
