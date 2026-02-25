# AzNavRail Complete Guide (v7.25)

Welcome to the comprehensive guide for **AzNavRail**. This library provides a robust, opinionated navigation rail and menu system for Jetpack Compose, featuring a streamlined DSL, strict layout enforcement, and advanced capabilities like FAB mode, nested rails, and system overlays.

---

## Table of Contents

1.  [Getting Started](#getting-started)
2.  [The Manual DSL](#the-manual-dsl)
    *   [Host Activity Layout](#host-activity-layout)
    *   [Configuration](#configuration-azconfig)
    *   [Theming](#theming-aztheme)
    *   [Navigation Items](#navigation-items)
    *   [Toggles & Cyclers](#toggles--cyclers)
    *   [Hierarchical Navigation](#hierarchical-navigation)
    *   [Nested Rails](#nested-rails)
    *   [Reorderable Items](#reorderable-items)
    *   [Advanced Features](#advanced-features-azadvanced)
3.  [UI Components](#ui-components)
    *   [AzTextBox](#aztextbox)
    *   [AzForm](#azform)
    *   [AzButton](#azbutton)
    *   [AzToggle](#aztoggle)
    *   [AzCycler](#azcycler)
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

## The Manual DSL

The manual DSL offers full control over the rail's content and behavior directly within your Composable code.

### Host Activity Layout

**Strict Usage Protocol:** `AzNavRail` **MUST** be used within an `AzHostActivityLayout` container. This layout enforces safe zones, handles padding automatically, and manages the rail's Z-ordering.

~~~kotlin
AzHostActivityLayout(
    navController = rememberNavController(),
    initiallyExpanded = false
) {
    // Rail Configuration & Items...

    onscreen(alignment = Alignment.Center) {
        // Your Content
    }
}
~~~

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `navController` | `NavHostController` | **Required** | The navigation controller for the app. |
| `currentDestination` | `String?` | `null` | Explicitly overrides the current route. If null, it's auto-detected. |
| `isLandscape` | `Boolean?` | `null` | Explicitly sets orientation. If null, auto-detected from configuration. |
| `initiallyExpanded` | `Boolean` | `false` | If true, the rail menu is expanded on launch (e.g., for tablets). |
| `disableSwipeToOpen` | `Boolean` | `false` | Disables the edge swipe gesture to open the menu. |
| `content` | `AzNavHostScope.() -> Unit` | **Required** | The DSL block for configuring the rail and adding onscreen content. |

### Configuration (`azConfig`)

Configures the behavioral and structural properties of the rail.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `dockingSide` | `AzDockingSide` | `LEFT` | `LEFT` or `RIGHT`. Determines which side the rail is pinned to. |
| `packButtons` | `Boolean` | `false` | If `true`, items are packed tightly at the top/center. If `false`, they are spaced out vertically. |
| `noMenu` | `Boolean` | `false` | If `true`, the expandable menu drawer is disabled. All items effectively become rail items. |
| `vibrate` | `Boolean` | `false` | Enables haptic feedback on interactions. |
| `displayAppName` | `Boolean` | `false` | If `true`, displays the app name (from resources) in the header instead of the icon. |
| `activeClassifiers` | `Set<String>` | `emptySet()` | A set of tags used to programmatically highlight items (see `azRailItem`). |
| `usePhysicalDocking` | `Boolean` | `false` | If `true`, anchors the rail to the physical side of the device, rotating with it. |
| `expandedWidth` | `Dp` | `130.dp` | Width of the rail when the menu is open. |
| `collapsedWidth` | `Dp` | `80.dp` | Width of the rail when collapsed. |
| `showFooter` | `Boolean` | `true` | Shows the footer (Privacy, Terms, Help) in the menu. |

### Theming (`azTheme`)

Configures the visual appearance of the rail.

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `activeColor` | `Color` | `Unspecified` | The tint color for active/selected items. |
| `defaultShape` | `AzButtonShape` | `CIRCLE` | `CIRCLE`, `SQUARE`, `RECTANGLE`, `NONE`. Defaults shape for all items. |
| `headerIconShape` | `AzHeaderIconShape` | `CIRCLE` | `CIRCLE`, `ROUNDED`, `NONE`. Shape of the top header icon. |

### Navigation Items

Items are the core building blocks.

*   **`azRailItem`**: Always visible in the rail.
*   **`azMenuItem`**: Visible only when the rail expands into a menu.

| Parameter | Description |
| :--- | :--- |
| `id` | **Required**. Unique String ID. |
| `text` | **Required**. Display label. |
| `route` | Navigation destination. |
| `content` | Custom content (`Color`, `Int` (Drawable Res), or `String` (URL - future)). |
| `color` | Overrides the default active color for this item. |
| `shape` | Overrides the default shape for this item. |
| `disabled` | If `true`, the item is non-interactive and dimmed. |
| `info` | Help text for the Info Screen. |
| `classifiers` | Set of strings. If any match `azConfig.activeClassifiers`, the item is highlighted. |
| `onFocus` | Callback when the item receives focus. |
| `onClick` | Callback when the item is clicked. |

### Toggles & Cyclers

Manage state directly within the rail.

*   **`azRailToggle` / `azMenuToggle`**: A binary switch.
*   **`azRailCycler` / `azMenuCycler`**: Cycles through a list of options.

**Toggle Parameters:**
*   `isChecked`: Boolean state.
*   `toggleOnText` / `toggleOffText`: Labels for each state.

**Cycler Parameters:**
*   `options`: List of strings.
*   `selectedOption`: Currently selected string.
*   `disabledOptions`: List of strings that cannot be selected.

### Hierarchical Navigation

Create nested menu structures. Note: Only one host can be expanded at a time.

*   **`azRailHostItem` / `azMenuHostItem`**: Parent item.
*   **`azRailSubItem` / `azMenuSubItem`**: Child item. Requires `hostId`.

**Sub-Item Parameters:**
*   `hostId`: The `id` of the parent Host item.
*   All other standard item parameters apply.

### Nested Rails

`azNestedRail` creates a secondary popup rail that appears when the item is clicked.

| Parameter | Description |
| :--- | :--- |
| `alignment` | `VERTICAL` (expands down) or `HORIZONTAL` (expands sideways). |
| `nestedContent` | DSL block to add items (`azRailItem`) to the popup rail. |

### Reorderable Items

`azRailRelocItem` enables drag-and-drop reordering.

*   **Long Press**: Drag to reorder.
*   **Tap**: Focus.
*   **Tap (Focused)**: Open context menu.

**Parameters:**
*   `onRelocate`: Callback `(fromIndex, toIndex, newOrderList) -> Unit`.
*   `hiddenMenu`: DSL block to define context menu actions (`listItem`, `inputItem`).

### Advanced Features (`azAdvanced`)

| Parameter | Description |
| :--- | :--- |
| `isLoading` | If `true`, shows a full-screen `AzLoad` spinner overlay. |
| `infoScreen` | If `true`, activates the interactive Help/Tutorial overlay. |
| `onDismissInfoScreen` | Callback to exit help mode. |
| `enableRailDragging` | If `true`, enables **FAB Mode**. Long-press header to detach rail. |
| `overlayService` | Class of the service extending `AzNavRailOverlayService` for system-wide usage. |
| `onUndock` | Callback when rail is detached. |

---

## UI Components

### AzTextBox

A feature-rich text input field.

| Parameter | Description |
| :--- | :--- |
| `value` / `onValueChange` | For controlled usage. |
| `hint` | Placeholder text. |
| `historyContext` | Key for namespacing autocomplete history. |
| `multiline` | If `true`, allows multiple lines. |
| `secret` | If `true`, masks input (password). |
| `isError` | Visual error state. |
| `submitButtonContent` | Composable for the built-in submit button. |
| `onSubmit` | Callback when submit button is clicked. |

### AzForm

Groups multiple `AzTextBox` entries.

~~~kotlin
AzForm(
    formName = "login",
    onSubmit = { data -> /* data["username"] */ }
) {
    entry("username", "User")
    entry("password", "Pass", secret = true)
}
~~~

### AzButton

Standalone button with loading state support.

| Parameter | Description |
| :--- | :--- |
| `text` | Label text. |
| `onClick` | Click handler. |
| `isLoading` | If `true`, replaces text with a spinner (maintains size). |
| `shape` | `AzButtonShape`. |

### AzToggle

Standalone binary switch.

~~~kotlin
AzToggle(
    isChecked = state,
    onToggle = { state = !state },
    toggleOnText = "ON",
    toggleOffText = "OFF"
)
~~~

### AzCycler

Standalone option cycler.

~~~kotlin
AzCycler(
    options = listOf("A", "B", "C"),
    selectedOption = current,
    onCycle = { /* update current */ }
)
~~~

### AzRoller

A "Slot Machine" dropdown.
*   **Left Click**: Type to filter.
*   **Right Click**: Roll to select.

~~~kotlin
AzRoller(
    options = listOf("A", "B", "C"),
    selectedOption = "A",
    onOptionSelected = { it }
)
~~~

### AzLoad

Polished loading spinner.

~~~kotlin
AzLoad() // Renders the spinner
~~~

---

## Strict Layout Rules

1.  **Top 10%**: Reserved for Header.
2.  **Bottom 10%**: Reserved for Footer.
3.  **Rail Padding**: `onscreen` content is padded by `collapsedWidth` on the docking side.
4.  **Mirroring**: Alignments (e.g., `TopStart`) flip automatically if the rail is on the Right.

---

## Info & Help Screen

A built-in onboarding tool.
1.  Set `infoScreen = true` in `azAdvanced`.
2.  Add `info` text to any item.
3.  The UI highlights items and draws connecting lines to their descriptions.
4.  Displays coordinates for debugging.
