## Project Structure

This project is a monorepo containing the following packages:

-   `aznavrail`: The core Android library (Runtime).
-   `aznavrail-annotation`: The KSP annotations module (Compile-time).
-   `aznavrail-processor`: The KSP symbol processor (Code Generator).
-   `aznavrail-react-native`: The React Native version of the library.
-   `aznavrail-web`: The web version of the library.
-   `SampleApp`: A sample Android application demonstrating the High-Inference system.

### File Dictionary

This document provides a brief but thorough description of what each file in the project is supposed to do.

#### Root Directory

| File/Directory | Description |
| --- | --- |
| `.github/` | Contains GitHub Actions workflows for CI/CD. |
| `SampleApp/` | An Android application that demonstrates how to use the `aznavrail` library. |
| `aznavrail/` | The core `aznavrail` Android runtime library. |
| `aznavrail-annotation/` | Contains the `@Az` annotation and shared data models. |
| `aznavrail-processor/` | Contains the KSP processor that generates the navigation graph. |
| `aznavrail-react-native/` | The React Native version of the library. |
| `aznavrail-web/` | The web version of the library. |
| `docs/` | Contains detailed documentation guides. |
| `gradle/` | Contains the Gradle wrapper files. |
| `.gitignore` | Specifies intentionally untracked files to ignore. |
| `AGENTS.md` | Provides instructions for AI agents working with the codebase. |
| `README.md` | The main README file for the project. |
| `build.gradle.kts` | The main build script for the project. |
| `gradle.properties` | Project-wide Gradle settings. |
| `gradlew` | The Gradle wrapper script for Unix-based systems. |
| `gradlew.bat` | The Gradle wrapper script for Windows. |
| `settings.gradle.kts` | The settings script for the project (defines modules). |
| `setup_android.sh` | A script to verify and set up the Android development environment. |

#### docs Directory

| File | Description |
| --- | --- |
| `AZNAVRAIL_COMPLETE_GUIDE.md` | The master manual for the library. |
| `AZ_HIGH_INFERENCE.md` | Documentation specific to the KSP High-Inference system. |
| `MIGRATION_GUIDE.md` | Guide for upgrading from legacy DSL to `@Az` annotations. |
| `API.md` | Technical API reference. |
| `DSL.md` | Legacy DSL reference. |

#### aznavrail-annotation Directory

This module contains the annotations used by the developer to configure the app. It has no Android dependencies.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | Build script for the annotation module. |
| `src/main/java/com/hereliesaz/aznavrail/annotation/Az.kt` | Defines the `@Az` annotation and its context classes (`App`, `RailItem`, `RailHost`, etc.). |
| `src/main/java/com/hereliesaz/aznavrail/model/` | Shared models used by both the processor and the runtime (e.g., `AzDockingSide`, `AzOrientation`). |

#### aznavrail-processor Directory

This module contains the KSP logic that scans the code and generates the `AzGraph`.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | Build script for the processor module. |
| `src/main/java/com/hereliesaz/aznavrail/processor/AzProcessor.kt` | The core logic that scans symbols, validates rules, and generates Kotlin code. |
| `src/main/java/com/hereliesaz/aznavrail/processor/AzProcessorProvider.kt` | The provider class that instantiates the processor. |
| `src/main/resources/META-INF/services/` | Registers the processor provider for KSP. |

#### aznavrail Directory (Core Runtime)

The `aznavrail` directory contains the core Android library logic.

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the runtime module. |
| `src/main/java/com/hereliesaz/aznavrail/AzActivity.kt` | The base Activity class that users extend to link the generated graph. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRail.kt` | The internal `AzNavRail` composable and logic. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavHost.kt` | Defines `AzHostActivityLayout` (mandatory top-level) and `AzNavHost` (wrapper). |
| `src/main/java/com/hereliesaz/aznavrail/AzTextBox.kt` | The `AzTextBox` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzRoller.kt` | The `AzRoller` composable. |
| `src/main/java/com/hereliesaz/aznavrail/AzButton.kt` | The standalone `AzButton` composable. |
| `src/main/java/com/hereliesaz/aznavrail/internal/` | Internal implementation details (`RailItems.kt`, `NestedRail.kt`, `RailContent.kt`). |

#### aznavrail-react-native Directory

The React Native port of the library.

| File/Directory | Description |
| --- | --- |
| `package.json` | Node package configuration. |
| `src/AzNavRail.tsx` | The main React Native component. |
| `src/AzNavRailScope.tsx` | Context definitions for the rail. |
| `src/components/` | Platform-specific components (`AzButton.tsx`, `AzTextBox.tsx`). |
| `src/util/` | Utilities including `HistoryManager` and `RelocItemHandler`. |

#### aznavrail-web Directory

The Web (React) port of the library.

| File/Directory | Description |
| --- | --- |
| `package.json` | Node package configuration. |
| `vite.config.js` | Vite build configuration. |
| `src/components/` | Web components (`AzNavRail.jsx`, `AzButton.jsx`). |
| `src/App.jsx` | Example usage for the web. |

#### SampleApp Directory

The `SampleApp` directory demonstrates the usage of the library (specifically the High-Inference annotation system).

| File/Directory | Description |
| --- | --- |
| `build.gradle.kts` | The build script for the `SampleApp` module. |
| `src/main/java/com/example/sampleapp/MainActivity.kt` | The main activity annotated with `@Az(app = ...)` that initializes the generated graph. |
