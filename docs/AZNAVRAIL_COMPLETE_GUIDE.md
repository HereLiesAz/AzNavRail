# AzNavRail Complete Guide (v7.25)

Welcome to the comprehensive guide for **AzNavRail**. This document details the **High-Inference System**, which is the absolute and only supported method for configuring the rail architecture. 

**Version 7.25: The Live Dictatorship.** You no longer just annotate static structures. You bind your rail items directly to the reactive state of your Activity. When your state changes, the rail reacts instantly.

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Az Protocol (Architecture)](#the-az-protocol-architecture)
3.  [Live Dictatorship: Dynamic Binding](#live-dictatorship-dynamic-binding)
4.  [State & Action Binding](#state--action-binding)
5.  [Strict Layout Rules](#strict-layout-rules)
6.  [Info & Help Screen](#info--help-screen)
7.  [Annotation Reference (@Az)](#annotation-reference-az)

---

## Getting Started

### 1. Installation

Add the **KSP Plugin** and dependencies to your app's `build.gradle.kts`.

~~~kotlin
plugins {
    id("com.google.devtools.ksp") version "2.2.21-2.0.5"
}

dependencies {
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail:7.25")
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail-annotation:7.25")
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:7.25")
}
~~~

### 2. The Constitution (App Setup)

Extend `AzActivity` and point it to the generated graph. In v7.25, annotation parameters like `dock` use **String literals** to prevent dependency cycles.

~~~kotlin
// 1. Define Global Rules (Note: dock is now a String "LEFT" or "RIGHT")
@Az(app = App(dock = "LEFT", expandedWidth = 240, collapsedWidth = 80))
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

---

## The Az Protocol (Architecture)

The KSP Processor enforces the following architecture:

1.  **Zero-Talk Inference**: If you do not provide an `id` or `text` in the annotation, it is derived from the symbol name (e.g., `fun WiFiSettings` -> ID: `wifi_settings`, Text: "WiFi Settings").
2.  **Type Safety via Casting**: The generated `AzGraph` casts the generic `activity` to your specific `MainActivity`. This allows the rail to read and mutate your properties directly.

---

## Live Dictatorship: Dynamic Binding

This is the flagship feature of v7.25. You can bind UI properties to `mutableStateOf` variables in your Activity.

### How it works:
1.  Define a property in your `AzActivity`.
2.  Reference that property by name (as a String) in the `@Az` annotation.

~~~kotlin
class MainActivity : AzActivity() {
    override val graph = AzGraph
    
    // 1. Define reactive state
    var isProMode by mutableStateOf(false)
    var cartCount by mutableStateOf("0")
}

// 2. Bind in the Composable
@Az(rail = RailItem(
    icon = R.drawable.ic_premium,
    visibleProperty = "isProMode",   // Only shows if isProMode is true
    iconTextProperty = "cartCount"   // Shows the count as a badge
))
@Composable
fun PremiumDashboard() { ... }
~~~

### Supported Dynamic Fields:
- `visibleProperty` (Boolean): Controls item visibility.
- `disabledProperty` (Boolean): Controls whether the item is clickable/grayed out.
- `textProperty` (String): Dynamically updates the label text.
- `iconTextProperty` (String): Dynamically updates the badge/icon overlay text.
- `isLoadingProperty` (Boolean): (Advanced) Controls the global loading spinner.

---

## State & Action Binding

The system dictates how an item behaves by examining the *shape* of the symbol you annotate.

### 1. Screen Binding (Navigable Destinations)
Annotate a **`@Composable` function**. Clicking the rail item opens the screen.

### 2. Action Binding (Transient Executions)
Annotate a **standard function**. Clicking the item executes the code and collapses the rail.

### 3. State Puppeteering (Toggles & Cyclers)
Annotate a `var` property with `@Az(toggle = ...)` or `@Az(cycler = ...)`. 

**Important:** For `Toggle`, you **must** provide `isCheckedProperty` matching the variable name. The generated code will handle the mutation (e.g., `instance.isDarkMode = !instance.isDarkMode`).

~~~kotlin
@Az(toggle = Toggle(isCheckedProperty = "isDarkMode", toggleOnText = "DARK", toggleOffText = "LIGHT"))
var isDarkMode by mutableStateOf(false)
~~~

---

## Strict Layout Rules

The generated `AzGraph` automatically wraps your content in `AzHostActivityLayout`. This enforces:

1.  **Safe Zones**: Your UI content is strictly forbidden from the **Top 20%** and **Bottom 10%** of the screen.
2.  **Rail Avoidance**: Content is automatically padded to avoid the rail.
3.  **Aesthetic Purge**: The layout handles geometry. Your app's `MaterialTheme` handles the paint. 

---

## Info & Help Screen

AzNavRail includes an interactive Info Screen that overlays the UI to explain navigation items.

### Activation
To activate the Info Screen, you must bind a boolean property to the `infoScreen` parameter in the `@Az(advanced = ...)` annotation.

~~~kotlin
// In MainActivity
var showHelp by mutableStateOf(false)

@Az(advanced = Advanced(
    infoScreen = true, // Enables the binding
    // Ideally this property name matches the variable in MainActivity
    // but currently the KSP processor might infer it or require explicit naming depending on implementation.
    // Check generated code or use manual config if KSP binding is limited.
))
~~~

**Manual Activation (Fallback):**
Override `configureRail` and use `azAdvanced`:

~~~kotlin
override fun AzNavRailScope.configureRail() {
    azAdvanced(
        infoScreen = showHelp,
        onDismissInfoScreen = { showHelp = false }
    )
}
~~~

### Adding Info to Items
To display information for a specific item, simply add the `info` string to its annotation or builder function.

~~~kotlin
@Az(rail = RailItem(info = "Navigates to the home dashboard."))
@Composable
fun Home() { ... }
~~~

When the Info Screen is active:
1.  **Visual Connection**: A yellow line connects the rail item to its description card.
2.  **Strict Square Aesthetics**: The description cards and the close button use `RECTANGLE` shapes (no rounded corners).
3.  **Safe Zones**: Lines and cards respect the rail safe zones.

---

## Annotation Reference (@Az)

| Annotation | Key Parameters (v7.25) |
| :--- | :--- |
| `@App` | `dock` (String), `expandedWidth` (Int), `collapsedWidth` (Int), `vibrate` (Boolean), `usePhysicalDocking` (Boolean) |
| `@RailItem` | `textProperty`, `iconTextProperty`, `visibleProperty`, `disabledProperty`, `info` (String) |
| `@Toggle` | `isCheckedProperty` (Required for dynamic binding), `info` (String) |
| `@Cycler` | `optionsProperty`, `selectedOptionProperty`, `disabledOptionsProperty`, `info` (String) |
| `@Advanced`| `isLoadingProperty`, `infoScreen` (Boolean) |
