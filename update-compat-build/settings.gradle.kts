pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "aznavrail-update-compat"
include(":aznavrail-updates-compat", ":aznavrail-updates-play-compat")
