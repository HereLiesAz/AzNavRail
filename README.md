# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

---

## 📚 Documentation

-   **[Complete Guide (The Bible)](/docs/AZNAVRAIL_COMPLETE_GUIDE.md)**: Comprehensive, encyclopedic reference for every feature.
-   **[API Reference](/docs/API.md)**: Javadoc-style listing.
-   **[DSL Reference](/docs/DSL.md)**: Quick look at DSL functions.

---

## 🚀 Setup

Add JitPack to your `settings.gradle.kts`:

~~~kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
~~~

Add the dependency to your app's `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest release
}
~~~

---

## 🛠️ The Golden Sample

This is the standard, validated way to initialize `AzNavRail`. It **must** be wrapped in `AzHostActivityLayout`.

~~~kotlin
import com.hereliesaz.aznavrail.*

@Composable
fun SampleScreen() {
    val navController = rememberNavController()

    AzHostActivityLayout(
        navController = navController,
        initiallyExpanded = false
    ) {
        // 1. CONFIGURATION
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true, // Tightly pack rail items
            displayAppName = true
        )

        azTheme(activeColor = Color.Cyan)

        // 2. NAVIGATION ITEMS
        azRailItem(id = "home", text = "Home", route = "home", content = Icons.Default.Home)
        azRailItem(id = "profile", text = "Profile", route = "profile")

        // 3. MENU ONLY ITEMS
        azMenuItem(id = "settings", text = "Settings", route = "settings")

        // 4. ONSCREEN CONTENT
        // Use 'onscreen' to define your UI. 
        // Layout rules (safe zones, padding) are enforced automatically.
        onscreen(alignment = Alignment.Center) {
            AzNavHost(startDestination = "home") {
                composable("home") { Text("Home Screen") }
                composable("profile") { Text("Profile Screen") }
                composable("settings") { Text("Settings Screen") }
            }
        }
    }
}
~~~

---

## ✨ Features & Basics

### 1. Strict Layout System
`AzHostActivityLayout` enforces a "Constitution" for your UI to ensure consistency and usability:
*   **Safe Zones:** Top 10% and Bottom 10% are reserved. Interactive content is pushed to the center 80%.
*   **Automatic Padding:** Content in `onscreen` is automatically padded to avoid the rail, regardless of docking side or rotation.
*   **Backgrounds:** Use `background(weight)` to place content *behind* the rail (e.g., maps).

### 2. Navigation Items
*   **`azRailItem`**: Always visible.
*   **`azMenuItem`**: Visible only in the drawer.
*   **`azNestedRail`**: Opens a secondary popup rail (Vertical/Horizontal).
*   **`azRailRelocItem`**: Draggable items for user reordering.

### 3. Interactive Components
Manage state directly in the rail without leaving the context.
*   **Toggles:** `azRailToggle` / `azMenuToggle`.
*   **Cyclers:** `azRailCycler` (multi-state buttons).

### 4. UI Components (Standalone)
AzNavRail includes a suite of polished UI components that match the rail's aesthetic.

**AzTextBox & AzForm:**
Modern input fields with history, autocomplete, and validation.
~~~kotlin
AzForm(formName = "login", onSubmit = { ... }) {
    entry("user", "Username")
    entry("pass", "Password", secret = true)
}
~~~

**AzRoller:**
A "Slot Machine" style dropdown. Left-click to type, Right-click to roll.
~~~kotlin
AzRoller(
    options = listOf("A", "B", "C"),
    selectedOption = "A",
    onOptionSelected = { ... }
)
~~~

### 5. Advanced Modes
*   **FAB Mode:** Long-press the header to detach the rail into a floating action button. Drag it anywhere!
*   **Info Screen:** Set `infoScreen = true` in `azAdvanced` to overlay a tutorial mode with visual guides connecting items to their descriptions.

---

## License

Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
