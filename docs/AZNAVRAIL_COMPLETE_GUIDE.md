# AzNavRail Complete Guide

Welcome to the comprehensive guide for **AzNavRail**. This document details the **High-Inference System**, which is the absolute and only supported method for configuring the rail architecture. 

**Dictatorial Design:** You do not build the graph. You do not configure the layout. You annotate your intentions, and the Compiler enforces the strict, brutalist rules of the AzNavRail system. 

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Az Protocol (Architecture)](#the-az-protocol-architecture)
3.  [State & Action Binding](#state--action-binding)
4.  [Strict Layout Rules](#strict-layout-rules)

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
// 1. Define Global Rules and Theme
@Az(
    app = App(dock = AzDockingSide.LEFT),
    theme = Theme(activeColorHex = "#FF00FF", defaultShape = AzButtonShape.ROUNDED_RECTANGLE)
)
class MainActivity : AzActivity() {
    // 2. Link the Generated Graph
    override val graph = AzGraph 

    // 3. Bind properties (The Processor will find them here)
    @Az(toggle = Toggle(toggleOnText = "WIFI ON", toggleOffText = "WIFI OFF"))
    var wifiState by mutableStateOf(false)

    // 4. The Runtime Escape Hatch (Optional)
    override fun AzNavRailScope.configureRail() {
        azConfig(vibrate = true, displayAppName = true)
    }
}
~~~

### 3. The Stations (Screens)

You do not manually add items to a rail. You annotate `@Composable` functions. The machine infers the ID and the display text from your function name. You can use standard drawables (`icon`) or strings (`iconText`).

~~~kotlin
@Az(rail = RailItem(iconText = "42"))
@Composable
fun TheAnswer() {
    Text("Strict Mode Active")
}
~~~

---

## State & Action Binding: The Shape of the Function

The system dictates whether an item navigates to a new screen, toggles a state, or triggers a silent background process by examining the *shape* of the symbol you annotate.

### 1. Screen Binding (Navigable Destinations)
If the annotated target is a **`@Composable` function**, the processor assumes it possesses visual mass. It wires the item into the `AzNavHost` graph and assigns it a routing destination. Clicking the rail item opens the screen.

### 2. Action Binding (Transient Executions)
If the annotated target is a **standard function** (without `@Composable`), the processor deduces it is a transient action. It does **not** add it to the navigation graph. Instead, it wires the function call directly to the rail item's `onClick` listener. 

~~~kotlin
@Az(rail = RailItem(iconText = "X", text = "Log Out"))
fun TerminateSession() {
    // No screen is opened. The rail just collapses.
}
~~~

### 3. State Binding (Puppeteering)
If you annotate a `var` property with `@Az(toggle = ...)` or `@Az(cycler = ...)` inside your Activity, the compiler assumes this is a source of UI truth. It automatically reads the variable to draw the UI state, and mutates the variable when the user clicks it. 

---

## Strict Layout Rules

The generated `AzGraph` automatically wraps your content in `AzHostActivityLayout`. This enforces strict boundaries that mathematically guarantee the system UI will never be obscured.

1.  **Content Safe Zones**: Your UI content is strictly forbidden from the **Top 20%** and **Bottom 10%** of the screen. 
2.  **Rail Safe Zones**: The rail itself is restricted to the **Top 10%** and **Bottom 10%**. 
3.  **The System Guarantee**: The safe zones dynamically calculate `max(SystemBars, SafePercentage)`. Under no circumstances will your content or popups obscure the notification or navigation bars.
4.  **Rail Avoidance**: Content is automatically padded to avoid the rail.

### Background Exile
To place content behind everything (e.g., a map or ambient animation), annotate a composable with `@Az(background = Background(weight = 0))`. Backgrounds ignore all padding and are exiled to `fillMaxSize()`, extending completely behind the navigation bars, notification bars, and the rail itself.
