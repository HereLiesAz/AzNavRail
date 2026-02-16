# Az High-Inference KSP System

The Az High-Inference system is a powerful compile-time code generator that eliminates the boilerplate traditionally associated with setting up `AzNavRail`. By using the `@Az` annotation and a smart KSP processor, you can define your entire navigation graph, rail hierarchy, and application configuration with minimal code.

## Key Features

*   **Zero-Talk Inference**: Automatically infers IDs and titles from your function and property names.
    *   `fun WifiSettings()` -> ID: `wifi_settings`, Text: "Wifi Settings"
*   **Unified Annotation**: A single `@Az` annotation handles Apps, Items, Hosts, and Nested Rails.
*   **Base Class Automation**: `AzActivity` handles all the `onCreate`, `setContent`, and `AzHostActivityLayout` wiring for you.
*   **Validation**: Enforces strict rules at compile time (e.g., ensuring navigation items are `@Composable`).

## Setup

### 1. Dependencies

Add the processor and annotation modules to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" // Use appropriate version
}

dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:5.1")
    implementation("com.github.HereLiesAz:AzNavRail:aznavrail-annotation:5.1")
    ksp("com.github.HereLiesAz:AzNavRail:aznavrail-processor:5.1")
}
```

### 2. The Application Entry Point

Extend `AzActivity` and annotate it with `@Az(app = ...)` to configure your app globally. You only need to override the `graph` property to point to the generated `AzGraph`.

```kotlin
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.App
import com.hereliesaz.aznavrail.model.AzDockingSide

@Az(
    app = App(
        dock = AzDockingSide.LEFT,
        displayAppName = true
    )
)
class MainActivity : AzActivity() {
    override val graph = AzGraph // Generated automatically
}
```

### 3. Defining Features (Items)

Annotate your `@Composable` functions with `@Az`.

#### Standard Rail Item
Infers ID and Text from the function name.

```kotlin
@Az // ID: "home", Text: "Home"
@Composable
fun Home() {
    Text("Home Screen")
}
```

#### Customized Rail Item
Override default values using `RailItem`.

```kotlin
@Az(rail = RailItem(icon = R.drawable.ic_profile, text = "My Profile"))
@Composable
fun Profile() {
    // ...
}
```

#### Rail Host (Expander)
Define a Host item using a property. This item expands to show nested items but has no screen content itself.

```kotlin
// ID: "settings", Text: "Settings"
@Az(host = RailHost(icon = R.drawable.ic_settings))
val Settings = null
```

#### Nested Rail Item
Link an item to a parent host.

```kotlin
// Parent: "settings", ID: "wifi", Text: "Wifi"
@Az(nested = NestedRail(parent = "settings"))
@Composable
fun Wifi() {
    Text("WiFi Settings")
}
```

## Inference Rules

| Code Symbol | Inferred ID | Inferred Text |
| :--- | :--- | :--- |
| `fun UserProfile()` | `user_profile` | "User Profile" |
| `fun FAQ()` | `f_a_q` (Standard Snake) | "F A Q" |
| `val AdvancedSettings` | `advanced_settings` | "Advanced Settings" |

## Validation

The processor enforces "The Tax":
*   Any function annotated with `@Az` (as a Rail or Nested item) **MUST** be annotated with `@Composable`.
*   Nested items must refer to a valid, existing Parent ID.

## Generated Code

The processor generates a file `AzGraph.kt` in your package containing:
1.  An `AzGraph` object implementing `AzGraphInterface`.
2.  A `Run` function that sets up `AzHostActivityLayout`, injects all `azRailItem`/`azNestedRail` calls, and builds the `AzNavHost` with all your composables.
