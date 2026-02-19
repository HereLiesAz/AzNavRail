# AzNavRail Complete Guide

Welcome to the comprehensive guide for **AzNavRail**. This document details the **High-Inference System**, which is the only supported method for configuring the rail architecture. 

**Dictatorial Design:** You do not build the graph. You do not configure the layout. You do not pick the colors. You annotate your intentions, and the Compiler enforces the strict, brutalist rules of the AzNavRail system. 

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Az Protocol (Architecture)](#the-az-protocol-architecture)
3.  [State & Action Binding](#state--action-binding)
4.  [Strict Layout Rules](#strict-layout-rules)
5.  [Annotation Reference (@Az)](#annotation-reference-az)
6.  [Component Reference](#component-reference)

---

## Getting Started

### 1. Installation

Add JitPack to your project's `settings.gradle.kts`:

~~~kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
~~~

Add the **KSP Plugin** and dependencies to your app's `build.gradle.kts`.
**Crucial:** The KSP version must match your Kotlin version.

~~~kotlin
plugins {
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

dependencies {
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail:VERSION")
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail-annotation:VERSION")
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:VERSION")
}
~~~

### 2. The Constitution (App Setup)

You do not write `onCreate`. You do not write `setContent`. You extend `AzActivity` and point it to the generated graph.

~~~kotlin
// 1. Define Global Rules
@Az(app = App(dock = AzDockingSide.LEFT, expandedWidth = 240, collapsedWidth = 80))
class MainActivity : AzActivity() {
    // 2. Link the Generated Graph
    override val graph = AzGraph 

    // 3. The Runtime Escape Hatch (Optional)
    override fun AzNavRailScope.configureRail() {
        azConfig(
            vibrate = true,
            displayAppName = true
        )
    }
}
~~~

### 3. The Stations (Screens)

You do not manually add items to a rail. You annotate `@Composable` functions. The machine infers the ID and the display text from your function name.

~~~kotlin
// Inferred ID: "home", Title: "Home"
@Az(rail = RailItem(home = true))
@Composable
fun Home() {
    Text("Strict Mode Active")
}
~~~

---

## The Az Protocol (Architecture)

The KSP Processor enforces the following architecture:

1.  **Zero-Talk Inference**: If you do not provide an `id` or `text` in the annotation, it is derived from the symbol name (e.g., `fun WiFiSettings` -> ID: `wifi_settings`, Text: "WiFi Settings").
2.  **Hierarchy**: To create sub-menus, define a **Host** (using a property) and link children to it using `parent`.

~~~kotlin
// Define a Host (Expands in Rail)
@Az(host = RailHost(icon = R.drawable.ic_settings))
val System = null // Placeholder property

// Link a Child to the Host
@Az(rail = RailItem(parent = "system"))
@Composable 
fun Wifi() { ... }
~~~

---

## State & Action Binding

The system differentiates between screens, transient actions, and state toggles based on the *shape* of the symbol you annotate.

### Action Binding (Transient Execution)
If you annotate a function that is **NOT** a `@Composable`, the system wires it as an `onClick` transient action. It will execute and the rail will immediately collapse.

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_logout))
fun Logout() {
    authSystem.terminate()
}
~~~

### State Binding (Puppeteering)
If you annotate a `var` property with `@Az(toggle = ...)` or `@Az(cycler = ...)`, the compiler assumes this is the source of truth. It will inject property delegation into the UI to automatically read and mutate your variable. 

*Note:* You must use Kotlin property delegation (`by mutableStateOf()`) for the UI to observe it.

~~~kotlin
@Az(toggle = Toggle(toggleOnText = "DARK", toggleOffText = "LIGHT", isMenu = true))
var isDarkMode by mutableStateOf(false)
~~~

---

## Strict Layout Rules

The generated `AzGraph` automatically wraps your content in `AzHostActivityLayout`. This enforces:

1.  **Safe Zones**: Your UI content is strictly forbidden from the **Top 20%** and **Bottom 10%** of the screen.
2.  **Rail Avoidance**: Content is automatically padded to avoid the rail.
3.  **Smart Transitions**: Animations automatically reverse based on the docking side.
4.  **Aesthetic Purge**: The layout handles geometry. Your app's `MaterialTheme` handles the paint. 

### Backgrounds
To place content (like Maps) behind the safe zones, annotate a composable with `@Az(background = Background(weight = 0))`. 

---

## Annotation Reference (@Az)

### `app = App(...)`
Used on `MainActivity`. Defines global geometry and behavior.
* `dock`, `expandedWidth`, `collapsedWidth`, `packButtons`, `usePhysicalDocking`.

### `advanced = Advanced(...)`
Used on `MainActivity`. Binds system overlays and drags.
* `infoScreen`, `isLoading`, `overlayServiceClass`, `onUndock`, `onRailDrag`.

### `rail = RailItem(...)` / `menu = MenuItem(...)`
Defines a standard screen or action. `menu` items appear only in the footer.
* `id`, `text`, `icon`, `parent`, `home`, `disabled`, `classifiers`.

### `host = RailHost(...)`
Defines a parent item that expands to show children.

### `nested = NestedRail(...)`
Defines a popup menu structure.

### `toggle = Toggle(...)` / `cycler = Cycler(...)`
Defines state-bound interactive elements.

### `reloc = RelocItem(...)`
Defines an item that can be dragged and reordered by the user, revealing hidden menus.

---

## Component Reference

### `AzTextBox`
A versatile text input with autocomplete, multiline, and "Secret" modes.

### `AzRoller`
A slot-machine style dropdown with split-click interaction.
