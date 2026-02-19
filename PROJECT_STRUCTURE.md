# Project Structure

This project is a monorepo enforcing the AzNavRail automated architecture:

-   `aznavrail`: The core Android library (The Runtime Environment).
-   `aznavrail-annotation`: The KSP annotations module (The Static Law).
-   `aznavrail-processor`: The KSP symbol processor (The Dictator).
-   `aznavrail-react-native`: The React Native port.
-   `aznavrail-web`: The Web port.
-   `SampleApp`: A sample application demonstrating complete submission to the High-Inference system.

### File Dictionary

#### Root Directory

| File/Directory | Description |
| --- | --- |
| `.github/` | CI/CD automation. |
| `SampleApp/` | The reference implementation of an automated graph. |
| `aznavrail/` | The core runtime library containing the visual primitives. |
| `aznavrail-annotation/` | The `@Az` annotation and immutable data models. |
| `aznavrail-processor/` | The KSP processor that generates `AzGraph`. |
| `aznavrail-react-native/` | React Native port. |
| `aznavrail-web/` | Web port. |
| `docs/` | The sacred texts defining the architecture. |
| `gradle/` | Gradle wrapper files. |

#### docs Directory

| File | Description |
| --- | --- |
| `AZNAVRAIL_COMPLETE_GUIDE.md` | The master manual outlining the annotation protocol. |
| `MIGRATION_GUIDE.md` | Instructions for dismantling obsolete manual DSL setups. |
| `API.md` | The technical API boundaries. |
| `DSL.md` | The Synthetic Tongue reference. |

#### aznavrail-annotation Directory (The Law)

| File/Directory | Description |
| --- | --- |
| `src/main/java/com/hereliesaz/aznavrail/annotation/Az.kt` | Defines `@Az` and its rigid context classes. |
| `src/main/java/com/hereliesaz/aznavrail/model/` | Shared compile-time and runtime models (`AzDockingSide`, `AzOrientation`). |

#### aznavrail-processor Directory (The Dictator)

| File/Directory | Description |
| --- | --- |
| `src/main/java/com/hereliesaz/aznavrail/processor/AzProcessor.kt` | The compiler plugin that strips the developer of structural control and generates the UI. |

#### aznavrail Directory (The Prison)

| File/Directory | Description |
| --- | --- |
| `src/main/java/com/hereliesaz/aznavrail/AzActivity.kt` | The base class enforcing KSP compliance. Contains the `configureRail()` escape hatch. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailScope.kt` | The stripped-down Synthetic Tongue used for machine-to-machine UI injection. |
