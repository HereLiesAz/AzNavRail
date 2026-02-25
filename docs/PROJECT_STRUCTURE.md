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
| `aznavrail-react-native/` | The React Native version of the `aznavrail` library. |
| `aznavrail-web/` | The web version of the `aznavrail` library. |
| `docs/` | Contains detailed documentation guides. |
| `gradle/` | Contains the Gradle wrapper files. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `README.md` | The main README file for the project. |
| `build.gradle.kts` | The main build script for the project. |
| `gradle.properties` | Project-wide Gradle settings. |
| `settings.gradle.kts` | The settings script for the project. |
| `setup_android.sh` | Shell script for setting up the Android SDK environment. |

#### docs Directory

| File | Description |
| --- | --- |
| `AZNAVRAIL_COMPLETE_GUIDE.md` | A comprehensive guide extracted from the library assets. |
| `API.md` | Direct documentation of the public-facing API. |
| `DSL.md` | Direct documentation covering the builder Domain Specific Language parameters. |
| `PROJECT_STRUCTURE.md` | Explains the layout of the repository. |
| `DIMENSIONAL_COMPARISON.md` | Documents differences between major versions (v6.99 vs v7.25). |
| `SECURITY.md` | Security policy and vulnerability reporting. |
| `MIGRATION_GUIDE.md` | Guide for migrating to newer versions. |
| `AZ_HIGH_INFERENCE.md` | Documentation for the annotation-based High-Inference system. |

#### .github Directory

| File/Directory | Description |
| --- | --- |
| `workflows/` | Contains the GitHub Actions workflow files. |

##### workflows

| File | Description |
| --- | --- |
| `android-sample-build.yml` | Validates Android builds on push. |
| `codeql.yml` | Code security scanning action. |
| `jekyll-gh-pages.yml` | Documentation deployment pipeline. |

#### SampleApp Directory

The `SampleApp` directory contains an Android application that demonstrates how to use the `aznavrail` library.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `SampleApp` module. |
| `src/main/AndroidManifest.xml` | The manifest file for the `SampleApp`. |
| `src/main/java/com/example/sampleapp/MainActivity.kt` | The main activity of the `SampleApp`. |
| `src/main/java/com/example/sampleapp/SampleScreen.kt` | Demonstrates all features of the DSL inside `AzHostActivityLayout`. |
| `src/main/java/com/example/sampleapp/SampleOverlayService.kt` | Foreground service overlay demonstration. |
| `src/main/java/com/example/sampleapp/SampleBasicOverlayService.kt` | Basic `SYSTEM_ALERT_WINDOW` overlay demonstration. |
| `src/main/res/` | Contains the resources for the `SampleApp`. |

#### aznavrail Directory

The `aznavrail` directory contains the core `aznavrail` Android library module.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `aznavrail` module. |
| `consumer-rules.pro` | ProGuard rules for consumers of the library. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt` | The internal `AzNavRail` composable and logic. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavHost.kt` | Defines `AzHostActivityLayout` (mandatory top-level) and `AzNavHost` (wrapper). |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailScope.kt` | The DSL scope definition for `AzNavRail`. |
| `src/main/java/com/hereliesaz/aznavrail/AzTextBox.kt` | The `AzTextBox` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzForm.kt` | The `AzForm` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzRoller.kt` | The slot-machine dropdown dropdown picker. |
| `src/main/java/com/hereliesaz/aznavrail/AzButton.kt` | The standalone `AzButton` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailButton.kt` | The core button component powering the rail itself. |
| `src/main/java/com/hereliesaz/aznavrail/AzLoad.kt` | The loading animation component. |
| `src/main/java/com/hereliesaz/aznavrail/AzDivider.kt` | The divider component. |
| `src/main/java/com/hereliesaz/aznavrail/internal/NestedRail.kt` | Rendering engine for nested rail popup overlays. |
| `src/main/java/com/hereliesaz/aznavrail/internal/AzRailLayoutHelper.kt` | Helper logic for calculating rail layout and physical vs view side mappings. |
| `src/main/java/com/hereliesaz/aznavrail/internal/RelocItemHandler.kt` | Handles drag, drop, bounds, and cluster bounding for Reorderable items. |
| `src/main/java/com/hereliesaz/aznavrail/internal/SecretScreens.kt` | Location logger utility unlocked via PIN check on the footer. |

#### aznavrail-react-native Directory

Contains the React Native translation of the framework. Built using Bob.

#### aznavrail-web Directory

The `aznavrail-web` directory contains the web version of the `aznavrail` library built with React & Vite.

| File/Directory | Description |
| --- | --- |
| `src/` | Contains the source code for the `aznavrail-web` library. |
| `package.json` | Lists the project dependencies and scripts. |
| `vite.config.js` | Vite configuration file for UMD and ES builds. |