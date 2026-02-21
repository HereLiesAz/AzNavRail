## FILE: ./aznavrail/src/main/assets/AZNAVRAIL_COMPLETE_GUIDE.md
# AzNavRail Complete Guide

Welcome to the comprehensive guide for **AzNavRail**.
This document contains everything you need to know to use the library, including setup instructions, a full API and DSL reference, layout rules, and complete sample code.

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [AzNavHost Layout Rules](#aznavhost-layout-rules)
3.  [DSL Reference](#dsl-reference)
4.  [API Reference](#api-reference)
5.  [Sample Application Source Code](#sample-application-source-code)

---

## Getting Started

### Installation

To use AzNavRail, add JitPack to your project's `settings.gradle.kts`:

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

### Basic Usage

**IMPORTANT:** `AzNavRail` **MUST** be used within an `AzNavHost` container.

~~~kotlin
AzNavHost(navController = navController) {
    azConfig(
        displayAppName = true,
        dockingSide = AzDockingSide.LEFT
    )

    // Define Rail Items (Visible on collapsed rail)
    azRailItem(id = "home", text = "Home", route = "home", onClick = { /* navigate */ })

    // Define Menu Items (Visible only when expanded)
    azMenuItem(id = "settings", text = "Settings", route = "settings", onClick = { /* navigate */ })

    // Define Content
    onscreen(Alignment.Center) {
        Text("My Content")
    }
}
~~~

---

## AzNavHost Layout Rules

`AzNavHost` enforces a "Strict Mode" layout system to ensure consistent UX and prevent overlap.

1.  **Rail Avoidance**: Content in the `onscreen` block is automatically padded to avoid the rail.
2.  **Safe Zones**: Content is restricted from the **Top 20%** and **Bottom 10%** of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are mirrored if the rail is docked to the Right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content (e.g., maps) behind the UI. Backgrounds **ignore safe zones**.

**Example:**

~~~kotlin
AzNavHost(navController = navController) {
    // Full screen background
    background(weight = 0) {
        GoogleMap(...)
    }

    // Safe UI content
    onscreen(Alignment.TopEnd) {
        Text("Overlay")
    }
}
~~~

---

## DSL Reference

The DSL is used inside `AzNavHost` to configure the rail and items.

### AzNavHost Scope

-   `background(weight: Int, content: @Composable () -> Unit)`: Adds a background layer ignoring safe zones.
-   `onscreen(alignment: Alignment, content: @Composable () -> Unit)`: Adds content to the safe area.

### AzNavRail Scope

**Settings:**
-   `azConfig(...)`: Configures global behavior. Parameters:
    - `displayAppName`: Boolean
    - `packButtons`: Boolean
    - `expandedWidth`: Dp
    - `collapsedWidth`: Dp
    - `showFooter`: Boolean
    - `vibrate`: Boolean
    - `dockingSide`: AzDockingSide (LEFT/RIGHT)
    - `noMenu`: Boolean
    - `usePhysicalDocking`: Boolean
-   `azTheme(...)`: Configures visual appearance. Parameters:
    - `activeColor`: Color?
    - `defaultShape`: AzButtonShape
    - `headerIconShape`: AzHeaderIconShape
-   `azAdvanced(...)`: Configures advanced operations. Parameters:
    - `isLoading`: Boolean
    - `enableRailDragging`: Boolean
    - `onUndock`: (() -> Unit)?
    - `overlayService`: Class<out Service>?
    - `onOverlayDrag`: ((Float, Float) -> Unit)?
    - `onItemGloballyPositioned`: ((String, Rect) -> Unit)?
    - `infoScreen`: Boolean
    - `onDismissInfoScreen`: (() -> Unit)?

**Items:**
-   `azMenuItem(...)`: Item visible only in expanded menu.
-   `azRailItem(...)`: Item visible in rail and menu.
-   `azMenuToggle(...)` / `azRailToggle(...)`: Toggle buttons.
-   `azMenuCycler(...)` / `azRailCycler(...)`: Cycle through options.
-   `azDivider()`: Horizontal divider.
-   `azMenuHostItem(...)` / `azRailHostItem(...)`: Parent items for nested menus.
-   `azMenuSubItem(...)` / `azRailSubItem(...)`: Child items.
-   `azRailRelocItem(...)`: Reorderable drag-and-drop items.

**Common Parameters:**
-   `id`: Unique identifier.
-   `text`: Display label.
-   `route`: Navigation route (optional).
-   `icon`: (Implicitly handled by shapes/text in this library).
-   `disabled`: Boolean state.
-   `info`: Help text for Info Screen mode.
-   `onClick`: Lambda action.

---

## API Reference

### `AzNavHost`
~~~kotlin
@Composable
fun AzNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavHostScope.() -> Unit
)
~~~

### `AzTextBox`
A versatile text input component.

~~~kotlin
@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false,
    isError: Boolean = false,
    historyContext: String? = null,
    submitButtonContent: (@Composable () -> Unit)? = null,
    onSubmit: (String) -> Unit
)
~~~

### `AzForm`
Groups `AzTextBox` fields.

~~~kotlin
@Composable
fun AzForm(
    formName: String,
    onSubmit: (Map<String, String>) -> Unit,
    content: AzFormScope.() -> Unit
)
~~~

### `AzButton`, `AzToggle`, `AzCycler`
Standalone versions of the rail components are available for general UI use.