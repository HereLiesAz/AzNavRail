# AzNavRail Complete Guide (v7.25)

Welcome to the comprehensive guide for **AzNavRail**. This library provides a robust, opinionated navigation rail and menu system for Jetpack Compose, featuring a streamlined DSL, strict layout enforcement, and advanced capabilities like FAB mode, nested rails, and system overlays.

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Manual DSL (Recommended)](#the-manual-dsl-recommended)
    *   [Layout & Hosting](#layout--hosting)
    *   [Navigation Items](#navigation-items)
    *   [Toggles, Cyclers, and Rollers](#toggles-cyclers-and-rollers)
    *   [Nested Rails](#nested-rails)
    *   [Reorderable Items](#reorderable-items)
    *   [Configuration & Theme](#configuration--theme)
    *   [Advanced Features](#advanced-features)
3.  [UI Components](#ui-components)
    *   [AzTextBox & AzForm](#aztextbox--azform)
    *   [AzButton, AzToggle, AzCycler](#azbutton-aztoggle-azcycler)
    *   [AzRoller](#azroller)
    *   [AzLoad](#azload)
4.  [Strict Layout Rules](#strict-layout-rules)
5.  [Info & Help Screen](#info--help-screen)

---

## Getting Started

### Installation

Add the dependency to your app's `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace with latest version
}
~~~

---

## The Manual DSL (Recommended)

The manual DSL offers full control over the rail's content and behavior directly within your Composable code.

### Layout & Hosting

**Strict Usage Protocol:** `AzNavRail` **MUST** be used within an `AzHostActivityLayout` container. This layout enforces safe zones, handles padding automatically, and manages the rail's Z-ordering.

~~~kotlin
AzHostActivityLayout(
    navController = rememberNavController(),
    initiallyExpanded = false
) {
    // 1. Configure the Rail
    azConfig(dockingSide = AzDockingSide.LEFT)

    // 2. Add Items
    azRailItem(id = "home", text = "Home", route = "home")

    // 3. Define Content
    onscreen(alignment = Alignment.Center) {
        // Your UI goes here. It is automatically padded to avoid the rail.
        AzNavHost(startDestination = "home") {
            composable("home") { Text("Home Screen") }
        }
    }
}
~~~

### Navigation Items

*   **`azRailItem`**: Adds an item to the always-visible rail.
*   **`azMenuItem`**: Adds an item to the expandable menu drawer only.
*   **`azRailHostItem` / `azMenuHostItem`**: Adds a parent item that can contain sub-items.
*   **`azRailSubItem` / `azMenuSubItem`**: Adds a child item to a host.

**Parameters:**
*   `id`: Unique String ID.
*   `text`: Display label.
*   `route`: Navigation destination (optional).
*   `content`: Custom icon/content (Color, Drawable Res ID, or specific types).
*   `info`: Help text for the Info Screen.
*   `disabled`: Boolean to disable interaction.
*   `classifiers`: Set of strings for programmatic highlighting.

~~~kotlin
azRailItem(
    id = "dashboard",
    text = "Dashboard",
    route = "dashboard",
    content = R.drawable.ic_dashboard,
    info = "View your main stats."
)
~~~

### Toggles, Cyclers, and Rollers

Manage state directly within the rail.

*   **`azRailToggle` / `azMenuToggle`**: A switch with two states.
*   **`azRailCycler` / `azMenuCycler`**: A button that cycles through a list of options.

~~~kotlin
azRailToggle(
    id = "theme_toggle",
    isChecked = isDarkTheme,
    toggleOnText = "Dark",
    toggleOffText = "Light",
    onClick = { isDarkTheme = !isDarkTheme }
)

azRailCycler(
    id = "filter_cycler",
    options = listOf("All", "Active", "Completed"),
    selectedOption = currentFilter,
    onClick = { /* Logic to cycle to next option */ }
)
~~~

### Nested Rails

`azNestedRail` allows an item to open a secondary popup rail. This is useful for complex hierarchies without cluttering the main rail.

*   **Vertical**: Expands downwards from the item.
*   **Horizontal**: Expands sideways from the item.

~~~kotlin
azNestedRail(
    id = "nested_tools",
    text = "Tools",
    alignment = AzNestedRailAlignment.HORIZONTAL
) {
    azRailItem("tool_1", "Hammer")
    azRailItem("tool_2", "Wrench")
}
~~~

### Reorderable Items

`azRailRelocItem` creates items that users can drag and drop to reorder.

*   **Long Press**: Starts dragging.
*   **Tap**: Selects/Focuses.
*   **Tap (Focused)**: Opens a hidden context menu (defined via `hiddenMenu`).

~~~kotlin
azRailRelocItem(
    id = "fav_1",
    hostId = "favorites",
    text = "Favorite 1",
    onRelocate = { from, to, newOrder -> /* Save new order */ }
) {
    listItem("Remove") { /* ... */ }
    inputItem("Rename") { newName -> /* ... */ }
}
~~~

### Configuration & Theme

*   **`azConfig`**:
    *   `dockingSide`: `LEFT` or `RIGHT`.
    *   `packButtons`: If `true`, rail items are packed tightly at the top/center. If `false`, they are spaced out.
    *   `noMenu`: Disables the drawer; all items effectively become rail items.
    *   `displayAppName`: Shows app name in header instead of icon.
*   **`azTheme`**:
    *   `activeColor`: Tint color for active items.
    *   `defaultShape`: `CIRCLE`, `SQUARE`, `RECTANGLE`, `NONE`.
    *   `headerIconShape`: Shape of the top header icon.

### Advanced Features

*   **`azAdvanced`**:
    *   `isLoading`: Shows a global loading spinner (`AzLoad`).
    *   `infoScreen`: Activates the interactive Help Overlay.
    *   `enableRailDragging`: Enables FAB Mode (detach and drag the rail).
    *   `overlayService`: Class reference for System Overlay service.

---

## UI Components

### AzTextBox & AzForm

Modern text input components with autocomplete, history, and validation.

*   **`AzTextBox`**:
    *   `historyContext`: Namespaces autocomplete history.
    *   `multiline`: Expands vertically.
    *   `secret`: Masks input (password).
    *   `onSubmit`: Callback for the built-in submit button.
*   **`AzForm`**: Groups `AzTextBox` fields into a single submission unit.

~~~kotlin
AzForm(
    formName = "login",
    onSubmit = { data -> login(data["user"], data["pass"]) }
) {
    entry("user", "Username")
    entry("pass", "Password", secret = true)
}
~~~

### AzButton, AzToggle, AzCycler

Standalone versions of the rail components for use in your `onscreen` content.

~~~kotlin
AzButton(
    text = "Submit",
    onClick = { submit() },
    isLoading = isSubmitting
)
~~~

### AzRoller

A "Slot Machine" style dropdown. Left-click to type/filter, Right-click to scroll options like a roller.

~~~kotlin
AzRoller(
    options = listOf("Apple", "Banana", "Cherry"),
    selectedOption = currentFruit,
    onOptionSelected = { currentFruit = it }
)
~~~

### AzLoad

A polished loading spinner. Use `AzLoad()` composable or `azAdvanced(isLoading = true)` for a full-screen overlay.

---

## Strict Layout Rules

`AzHostActivityLayout` enforces a "Constitution" for your UI:

1.  **Safe Zones**: Content is forbidden in the **Top 10%** (Header) and **Bottom 10%** (Footer/Nav) areas.
2.  **Rail Avoidance**: Padding is automatically applied to `onscreen` content to prevent overlap with the rail.
3.  **Mirrored Alignment**: If you dock the rail to the `RIGHT`, `Alignment.TopStart` in `onscreen` effectively becomes `TopEnd` relative to the safe area (visual flip).

---

## Info & Help Screen

A built-in tutorial mode.

1.  **Activate**: Set `infoScreen = true` in `azAdvanced` (or via Live Dictatorship binding).
2.  **Annotate**: Add `info = "Description..."` to your items.
3.  **Experience**:
    *   The UI dims.
    *   Lines connect items to their descriptions.
    *   Coordinates are visualized for debugging.
    *   Host items remain interactive to explore nested help.

To exit, the user clicks the FAB in the bottom right, which triggers `onDismissInfoScreen`.
