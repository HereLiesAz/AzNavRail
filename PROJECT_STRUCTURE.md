# Project Structure

This project provides the AzNavRail navigation library:

-   `aznavrail`: The core Android library containing the visual primitives and DSL.
-   `aznavrail-react-native`: The React Native port.
-   `aznavrail-web`: The Web port.
-   `SampleApp`: A sample application demonstrating manual usage of the `AzHostActivityLayout` and DSL.

### File Dictionary

#### Root Directory

| File/Directory | Description |
| --- | --- |
| `.github/` | CI/CD automation. |
| `SampleApp/` | The sample application. |
| `aznavrail/` | The core runtime library. |
| `aznavrail-react-native/` | React Native port. |
| `aznavrail-web/` | Web port. |
| `docs/` | Documentation. |
| `gradle/` | Gradle wrapper files. |

#### docs Directory

| File | Description |
| --- | --- |
| `API.md` | The technical API boundaries. |
| `DSL.md` | The DSL reference for configuring the rail. |

#### aznavrail Directory

| File/Directory | Description |
| --- | --- |
| `src/main/java/com/hereliesaz/aznavrail/AzNavHost.kt` | Contains `AzHostActivityLayout`, the main entry point. |
| `src/main/java/com/hereliesaz/aznavrail/AzNavRailScope.kt` | The DSL interface for configuring the rail items and behavior. |
