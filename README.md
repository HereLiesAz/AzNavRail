# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a "navigrenuail"--for Jetpack Compose with a streamlined, zero-boilerplate API via KSP code generation.

This navigrenuail provides a vertical navigation rail that expands to a full menu drawer. It forces a brutalist architecture to ensure consistent layout boundaries, safe-zone obedience, and zero aesthetic drifting.

## 🚀 Quick Setup Guide

### 1. Add JitPack
In your `settings.gradle.kts` (or root `build.gradle.kts`), add the JitPack repository:

~~~kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
~~~

### 2. Configure Your Module
In your app module's `build.gradle.kts`, apply the KSP plugin and add dependencies.

**Plugins Block:**
~~~kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" // Use version compatible with your Kotlin version
}
~~~

**Dependencies Block:**
~~~kotlin
dependencies {
    // Core Library
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail:<latest_version>")
    
    // Annotation Processor (Required for High-Inference System)
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:<latest_version>")
}
~~~

---

## 🛠️ Usage

AzNavRail relies entirely on the High-Inference annotation system. The library generates your entire navigation graph and enforces strict layout laws automatically.

1.  **Annotate your Activity**: Use `@Az` on your main activity to configure the app and theme.
2.  **Annotate your Composables & States**: Use `@Az` on any `@Composable` function, transient action, or state variable you want in the rail.
3.  **Connect the Graph**: Override `graph` in your activity.
4.  **Configure Rail**: Override `configureRail()` for dynamic runtime values.

**MainActivity.kt:**
~~~kotlin
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.annotation.*
import com.hereliesaz.aznavrail.model.*

@Az(
    app = App(dock = AzDockingSide.LEFT),
    theme = Theme(activeColorHex = "#FF00FF", defaultShape = AzButtonShape.ROUNDED_RECTANGLE)
)
class MainActivity : AzActivity() {
    // The processor generates 'AzGraph' in your package automatically.
    override val graph = AzGraph

    // Bind state directly. The KSP processor knows how to find this instance variable.
    @Az(toggle = Toggle(toggleOnText = "WIFI: ON", toggleOffText = "WIFI: OFF"))
    var isWifiOn by mutableStateOf(false)

    // Override to inject dynamic runtime configurations
    override fun AzNavRailScope.configureRail() {
        azConfig(
            vibrate = true,
            displayAppName = true,
            expandedWidth = 240.dp,
            collapsedWidth = 80.dp
        )
    }
}
~~~

**Screens.kt:**
~~~kotlin
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.RailItem

@Az(rail = RailItem(id = "home", text = "Home", home = true, icon = R.drawable.ic_home))
@Composable
fun Home() {
    Text("Welcome Home")
}

@Az(rail = RailItem(id = "secret", text = "Answer", iconText = "42"))
@Composable
fun SecretScreen() {
    Text("Typographic Icons Supported")
}
~~~

---

## 🌟 Key Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Strict Safe Zones**: Your UI content is violently crushed between the Top 20% and Bottom 10% marks. The Rail is imprisoned between Top 10% and Bottom 10%. Safe zones strictly guarantee no overlaps with system bars.
- **Scrollable**: Both rail and menu are vertically/horizontally scrollable.
- **Absolute Backgrounds**: Background layers render fully behind the system navigation and notification bars.
- **State Puppeteering**: Bind state toggles, transient actions, and multi-option cyclers directly to functions and `var` properties inside your Activity via KSP.
- **Nested Rails**: Support for nested navigation structures (`azNestedRail`) with both Vertical and Horizontal popups.
- **Draggable (FAB Mode)**: Detach and move the rail by long-pressing the header.
- **Reorderable Items**: `RelocItem` allows user drag-and-drop reordering with hidden floating actions.
- **System Overlay**: System-wide floating overlay window support built-in.

[API Reference](API.md) | [Full DSL](DSL.md) | [Complete Guide](AZNAVRAIL_COMPLETE_GUIDE.md)
