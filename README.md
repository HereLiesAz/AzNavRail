# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a "navigrenuail"--for Jetpack Compose with a streamlined, zero-boilerplate API via KSP code generation.

This navigrenuail provides a vertical navigation rail that expands to a full menu drawer. It forces a brutalist architecture to ensure consistent layout boundaries, safe-zone obedience, and zero aesthetic drifting.

## 🚀 Quick Setup Guide (v7.25 - Live Dictatorship)

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
    id("com.google.devtools.ksp") version "2.2.21-2.0.5" 
}
~~~

**Dependencies Block:**
~~~kotlin
dependencies {
    // Core Library
    implementation("com.github.HereLiesAz.AzNavRail:aznavrail:7.25")
    
    // Annotation Processor (Required for High-Inference System)
    ksp("com.github.HereLiesAz.AzNavRail:aznavrail-processor:7.25")
}
~~~

---

## 🛠️ Usage: The Live Dictatorship

AzNavRail v7.25 introduces **Reactive Property Binding**. You can now bind rail item properties (visibility, labels, badges) directly to `mutableStateOf` properties in your `AzActivity`.

1.  **Annotate your Activity**: Use `@Az` on your main activity.
2.  **Define Reactive State**: Use `mutableStateOf` for properties you want to control dynamically.
3.  **Annotate your Composables**: Use `@Az` with `*Property` fields to bind to your Activity state.

**MainActivity.kt:**
~~~kotlin
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.App
import androidx.compose.runtime.*

@Az(app = App(dock = "LEFT")) // Enums are now passed as Strings
class MainActivity : AzActivity() {
    override val graph = AzGraph

    // Reactive state for the "Live Dictatorship"
    var isSettingsVisible by mutableStateOf(true)
    var notificationCount by mutableStateOf("5")

    override fun AzNavRailScope.configureRail() {
        azConfig(vibrate = true, displayAppName = true)
    }
}
~~~

**Screens.kt:**
~~~kotlin
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.RailItem

@Az(rail = RailItem(home = true, icon = R.drawable.ic_home))
@Composable
fun Home() { ... }

@Az(rail = RailItem(
    icon = R.drawable.ic_settings,
    visibleProperty = "isSettingsVisible" // Binds to MainActivity.isSettingsVisible
))
@Composable
fun Settings() { ... }

@Az(rail = RailItem(
    icon = R.drawable.ic_notifications,
    iconTextProperty = "notificationCount" // Binds to MainActivity.notificationCount
))
@Composable
fun Alerts() { ... }
~~~

---

## 🌟 Key Features

- **Live Dictatorship (v7.25)**: Entirely reactive navigation. Hide, show, or rename rail items by simply changing state in your Activity.
- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Strict Safe Zones**: Your UI is banned from the top 20% and bottom 10% of the screen.
- **Aesthetic Purge**: The library provides the concrete, your `MaterialTheme` provides the paint.
- **State Puppeteering**: Bind state toggles and multi-option cyclers directly to `var` properties.
- **Draggable (FAB Mode)**: Detach and move the rail by long-pressing the header.

[API Reference](/API.md) | [Full DSL](/DSL.md) | [Complete Guide](/docs/AZNAVRAIL_COMPLETE_GUIDE.md)

## License

Copyright 2024 The AzNavRail Authors
Licensed under the Apache License, Version 2.0.
