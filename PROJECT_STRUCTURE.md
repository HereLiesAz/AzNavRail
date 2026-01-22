## Project Structure

This project is a monorepo containing the following packages:

-   `aznavrail`: The core Android library.
-   `SampleApp`: A sample Android application that demonstrates how to use the `aznavrail` library.
-   `aznavrail-web`: The web version of the `aznavrail` library.

### File Dictionary

This document provides a brief but thorough description of what each file in the project is supposed to do.

#### Root Directory

| File/Directory | Description |
| --- | --- |
| `.github/` | Contains GitHub Actions workflows for CI/CD. |
| `SampleApp/` | An Android application that demonstrates how to use the `aznavrail` library. |
| `aznavrail/` | The core `aznavrail` Android library module. |
| `aznavrail-web/` | The web version of the `aznavrail` library. |
| `gradle/` | Contains the Gradle wrapper files. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `AGENTS.md` | Provides instructions for AI agents working with the codebase. |
| `README.md` | The main README file for the project. |
| `build.gradle.kts` | The main build script for the project. |
| `gradle.properties` | Project-wide Gradle settings. |
| `gradlew` | The Gradle wrapper script for Unix-based systems. |
| `gradlew.bat` | The Gradle wrapper script for Windows. |
| `settings.gradle.kts` | The settings script for the project. |

#### .github Directory

| File/Directory | Description |
| --- | --- |
| `workflows/` | Contains the GitHub Actions workflow files. |

##### workflows

| File | Description |
| --- | --- |
| `npm-publish.yml` | This workflow builds, tests, and publishes the `aznavrail-web` package to npm. |
| `push.yml` | This workflow builds and tests the project on every push and pull request. |

#### SampleApp Directory

The `SampleApp` directory contains an Android application that demonstrates how to use the `aznavrail` library.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `SampleApp` module. |
| `src/main/AndroidManifest.xml` | The manifest file for the `SampleApp`. |
| `src/main/java/com/example/sampleapp/BubbleActivity.kt` | An activity configured to run as an Android Bubble, demonstrating overlay capabilities. |
| `src/main/java/com/example/sampleapp/MainActivity.kt` | The main activity of the `SampleApp`, which contains the sample code for the `aznavrail` library. |
| `src/main/res/` | Contains the resources for the `SampleApp`. |

#### aznavrail Directory

The `aznavrail` directory contains the core `aznavrail` Android library module.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `aznavrail` module. |
| `consumer-rules.pro` | ProGuard rules for consumers of the library. |
| `src/` | Contains the source code for the `aznavrail` library. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt` | The internal `AzNavRail` composable and logic. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavHost.kt` | Defines `AzHostActivityLayout` (mandatory top-level) and `AzNavHost` (wrapper). |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailScope.kt` | The DSL scope definition for `AzNavRail`. |
| `src/main/java/com/hereliesaz/aznavrail/AzTextBox.kt` | The `AzTextBox` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzForm.kt` | The `AzForm` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzButton.kt` | The standalone `AzButton` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailButton.kt` | The button component used within the rail. |
| `src/main/java/com/hereliesaz/aznavrail/AzLoad.kt` | The loading animation component. |
| `src/main/java/com/hereliesaz/aznavrail/AzDivider.kt` | The divider component. |

#### aznavrail-web Directory

The `aznavrail-web` directory contains the web version of the `aznavrail` library.

| File/Directory | Description |
| --- | --- |
| `public/` | Contains public assets that are served directly. |
| `src/` | Contains the source code for the `aznavrail-web` library. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `README.md` | The README file for the `aznavrail-web` library. |
| `eslint.config.js` | ESLint configuration file. |
| `index.html` | The main HTML file for the web application. |
| `package-lock.json` | Records the exact version of each dependency. |
| `package.json` | Lists the project dependencies and scripts. |
| `vite.config.js` | Vite configuration file. |

#### gradle Directory

The `gradle` directory contains the Gradle wrapper files, which allow the project to be built with a specific version of Gradle without having to install it system-wide.
