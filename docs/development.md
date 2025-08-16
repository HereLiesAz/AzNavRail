# Local Development

If you are working on the library itself, you may want to use it as a local module in a sample app for testing.

1.  In your `settings.gradle.kts` file, add the module:
    ```kotlin
    include(":AzNavRail")
    ```
2.  In your sample app's `build.gradle.kts` file, add the dependency:
    ```kotlin
    implementation(project(":AzNavRail"))
    ```
