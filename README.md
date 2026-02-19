# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a "navigrenuail"--for Jetpack Compose with a streamlined, DSL-style API.

This navigrenuail provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

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
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" // Use version compatible with your Kotlin version
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

### Option A: High-Inference (Recommended)
Let AzNavRail generate your entire navigation graph for you.

1.  **Annotate your Activity**: Use `@Az` on your main activity to configure the app.
2.  **Annotate your Composables**: Use `@Az` on any `@Composable` function you want in the rail.
3.  **Connect the Graph**: Override `graph` in your activity.
4.  **Configure Rail**: Override `configureRail()` to adjust runtime aesthetic preferences.

**MainActivity.kt:**
~~~kotlin
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.AzApp
import com.hereliesaz.aznavrail.model.AzDockingSide

@Az(app = AzApp(dock = AzDockingSide.LEFT))
class MainActivity : AzActivity() {
    // The processor generates 'AzGraph' in your package automatically.
    override val graph = AzGraph

    // Override to inject dynamic or runtime aesthetic configurations
    override fun AzNavRailScope.configureRail() {
        azTheme(
            expandedWidth = 240.dp,
            collapsedWidth = 80.dp
        )
        azConfig(
            vibrate = true,
            displayAppName = true
        )
    }
}
~~~

**Screens.kt:**
~~~kotlin
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.AzRail

@Az(rail = AzRail(id = "home", text = "Home", home = true, icon = R.drawable.ic_home))
@Composable
fun Home() {
    Text("Welcome Home")
}

@Az(rail = AzRail(id = "settings", text = "Settings", icon = R.drawable.ic_settings))
@Composable
fun Settings() {
    Text("App Settings")
}
~~~

### Option B: Manual Setup (Classic Mode)
If you prefer manual control, you must use the `AzHostActivityLayout` wrapper.

**MainActivity.kt:**
~~~kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // ⚠️ MANDATORY WRAPPER
            AzHostActivityLayout(
                navController = navController,
                onscreen = {
                    AzNavHost(startDestination = "home") {
                        composable("home") { Home() }
                        composable("settings") { Settings() }
                    }
                }
            ) {
                // 1. Theme
                azTheme(activeColor = Color.Cyan)
                
                // 2. Config
                azConfig(dockingSide = AzDockingSide.LEFT)

                // 3. Define Items
                azRailItem(id = "home", text = "Home", route = "home")
                azRailItem(id = "settings", text = "Settings", route = "settings")
            }
        }
    }
}
~~~

---

## 🌟 Key Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Scrollable**: Both rail and menu are scrollable.
- **DSL API**: Simple, declarative API.
- **Shapes**: `CIRCLE`, `SQUARE`, `RECTANGLE`, or `NONE`. `RECTANGLE`/`NONE` auto-size width (fixed 36dp height).
- **Smart Collapse**: Items collapse the rail after interaction.
- **Delayed Cycler**: Built-in delay prevents accidental triggers.
- **Automatic Header**: Displays app icon or name.
- **Loading State**: Built-in loading animation.
- **Standalone Components**: `AzButton`, `AzToggle`, `AzCycler`, `AzDivider`, `AzRoller`, `AzTextBox`, `AzForm`.
- **Nested Rails**: Support for nested navigation structures (`azNestedRail`) with both Vertical and Horizontal alignments.
- **Draggable (FAB Mode)**: Detach and move the rail by long-pressing the header.
- **Reorderable Items**: `AzRailRelocItem` allows user drag-and-drop reordering.
- **System Overlay**: System-wide overlay support.
- **Info Screen**: Interactive help mode for onboarding.
- **AzHostActivityLayout**: A layout container that enforces strict safe zones and automatic alignment rules.

[API Reference](/API.md) | [Full DSL](/DSL.md) | [Project Structure](/PROJECT_STRUCTURE.md)

## Documentation

The library includes a comprehensive **Complete Guide** (`AZNAVRAIL_COMPLETE_GUIDE.md`) in the `docs/` folder containing detailed instructions, API references, and best practices.

## License

Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
See the License for the specific language governing permissions and
limitations under the License.
