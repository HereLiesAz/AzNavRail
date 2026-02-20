# AzNavRail Complete Guide

Welcome to the comprehensive guide for **AzNavRail**. This document details the **High-Inference System**, which is the absolute and only supported method for configuring the rail architecture. 

**Dictatorial Design:** You do not build the graph. You do not configure the layout. You do not pick the colors. You annotate your intentions, and the Compiler enforces the strict, brutalist rules of the AzNavRail system. 

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Az Protocol (Architecture)](#the-az-protocol-architecture)
3.  [State & Action Binding](#state--action-binding)
4.  [Strict Layout Rules](#strict-layout-rules)
5.  [Annotation Reference (@Az)](#annotation-reference-az)

---

## Getting Started

### 1. Installation

Add the **KSP Plugin** and dependencies to your app's `build.gradle.kts`.

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
            // Dynamic overrides for expandedWidth, etc., go here
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

---

## State & Action Binding: The Shape of the Function

The system dictates whether an item navigates to a new screen, toggles a state, or triggers a silent background process by examining the *shape* of the symbol you annotate.

### 1. Screen Binding (Navigable Destinations)
If the annotated target is a **`@Composable` function**, the processor assumes it possesses visual mass. It wires the item into the `AzNavHost` graph and assigns it a routing destination. Clicking the rail item opens the screen.

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_profile))
@Composable
fun UserProfile() {
    Text("Profile Screen")
}
~~~

### 2. Action Binding (Transient Executions)
If the annotated target is a **standard function** (without `@Composable`), the processor deduces it is a transient action. It does **not** add it to the navigation graph. Instead, it wires the function call directly to the rail item's `onClick` listener. 

Clicking the item will simply execute your code and collapse the rail.

~~~kotlin
@Az(rail = RailItem(icon = R.drawable.ic_logout, text = "Log Out"))
fun TerminateSession() {
    authManager.clearTokens()
    // No screen is opened. The rail just collapses.
}
~~~

### 3. State Binding (Puppeteering)
If you annotate a `var` property with `@Az(toggle = ...)` or `@Az(cycler = ...)`, the compiler assumes this is a source of UI truth. It automatically reads the variable to draw the UI state, and mutates the variable when the user clicks it. 

*Note:* You must use Kotlin property delegation (`by mutableStateOf()`) for the UI to observe it.

~~~kotlin
@Az(toggle = Toggle(toggleOnText = "DARK", toggleOffText = "LIGHT", isMenu = true))
var isDarkMode by mutableStateOf(false)
~~~

---

## Strict Layout Rules

The generated `AzGraph` automatically wraps your content in `AzHostActivityLayout`. This enforces:

1.  **Safe Zones**: Your UI content is strictly forbidden from the **Top 20%** and **Bottom 10%** of the screen. The rail itself is restricted to the **Top 10%** and **Bottom 10%**. Under no circumstances will your content obscure system bars.
2.  **Rail Avoidance**: Content is automatically padded to avoid the rail.
3.  **Smart Transitions**: Animations automatically reverse based on the docking side.
4.  **Aesthetic Purge**: The layout handles geometry. Your app's `MaterialTheme` handles the paint. 

### Backgrounds
To place content (like Maps) behind the safe zones, annotate a composable with `@Az(background = Background(weight = 0))`. Backgrounds ignore all padding and extend completely behind the navigation bars, notification bars, and the rail itself.
