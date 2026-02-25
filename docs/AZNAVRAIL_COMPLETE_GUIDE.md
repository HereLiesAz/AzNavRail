# AzNavRail Complete Guide (v7.25)

Welcome to the definitive "Encyclopedic" guide for **AzNavRail**. This document details every single feature, its abilities, its limitations, and strict usage protocols.

---

## Table of Contents

1.  [Core Architecture](#core-architecture)
    *   [Host Activity Layout](#host-activity-layout)
    *   [Safe Zones & Layout Logic](#safe-zones--layout-logic)
2.  [Rail Configuration](#rail-configuration)
    *   [Docking & Orientation](#docking--orientation)
    *   [FAB Mode (Draggable Rail)](#fab-mode-draggable-rail)
    *   [Info Screen (Help Mode)](#info-screen-help-mode)
3.  [Navigation Items](#navigation-items)
    *   [Standard Items](#standard-items)
    *   [Nested Rails](#nested-rails)
    *   [Reorderable Items (Drag & Drop)](#reorderable-items-drag--drop)
    *   [Host & Sub-Items](#host--sub-items)
4.  [Interactive Components](#interactive-components)
    *   [Toggles](#toggles)
    *   [Cyclers](#cyclers)
    *   [Rollers (Slot Machine)](#rollers-slot-machine)
5.  [Input Components](#input-components)
    *   [AzTextBox](#aztextbox)
    *   [AzForm](#azform)
6.  [Advanced Features](#advanced-features)
    *   [AzLoad (Loading)](#azload-loading)
    *   [System Overlay](#system-overlay)

---

## Core Architecture

### Host Activity Layout

**Description:** The mandatory top-level container. It manages the Z-ordering of the rail, background layers, and main content.

**Abilities:**
*   **Automatic Padding:** Calculates the width of the rail (collapsed or floating) and applies padding to your content so it never overlaps.
*   **Rotation Handling:** Adapts layout to device rotation (0°, 90°, 270°).
*   **Background Layering:** Allows placing content *behind* the rail (e.g., full-screen maps) using `background(weight)`.

**Limitations:**
*   **Strict Usage:** Must be the root composable. Throwing an error if `AzNavRail` is used outside it.
*   **Single Instance:** Designed for one rail per screen.

**Usage:**
~~~kotlin
AzHostActivityLayout(navController = navController) {
    background(weight = 0) { GoogleMap(...) } // Behind UI
    onscreen(Alignment.TopStart) { Text("Main UI") } // Safe UI
}
~~~

### Safe Zones & Layout Logic

**Description:** A strict system that reserves screen real estate for system bars and navigation elements.

**Abilities:**
*   **Top 10%:** Reserved for Header/Status Bar. No interactive content allowed here.
*   **Bottom 10%:** Reserved for Footer/Nav Bar. No interactive content allowed here.
*   **Mirrored Alignment:** If docked `RIGHT`, `Alignment.TopStart` acts visually as `TopEnd` relative to the safe area.

**Limitations:**
*   **Unavoidable:** You cannot disable safe zones. They are "Constitutionally" enforced.

---

## Rail Configuration

### Docking & Orientation

**Description:** Controls where the rail sits.

**Abilities:**
*   **`dockingSide`**: `LEFT` or `RIGHT`.
*   **`usePhysicalDocking`**: If `true`, ties the rail to the physical hardware edge. If you rotate the device, the rail rotates with it (staying on the physical "left"), rather than jumping to the new visual left.

**Usage:**
~~~kotlin
azConfig(
    dockingSide = AzDockingSide.LEFT,
    usePhysicalDocking = true
)
~~~

### FAB Mode (Draggable Rail)

**Description:** Allows the rail to detach from the edge and become a Floating Action Button (FAB).

**Abilities:**
*   **Activation:** Long-press the header icon OR swipe vertically on the rail. Triggers haptic feedback.
*   **Drag Constraints:** Can be dragged anywhere vertically between the 10% and 90% safe zones.
*   **Auto-Fold:** If expanded, items fold up into the FAB while dragging and unfold when dropped.
*   **Snapping:** Dragging close to the docking edge snaps it back to Rail Mode.

**Limitations:**
*   **No Menu:** The expandable drawer is disabled in FAB mode.
*   **Size Cap:** Max size is capped at 80% of screen height/width to ensure safe zone compliance.

**Usage:**
~~~kotlin
azAdvanced(enableRailDragging = true)
~~~

### Info Screen (Help Mode)

**Description:** An interactive overlay for onboarding.

**Abilities:**
*   **Visual Guides:** Draws lines connecting description cards to specific rail items.
*   **Auto-Wiring:** Automatically calculates item positions; no manual coordinates needed.
*   **Live Debugging:** Displays X/Y coordinates of items.
*   **Host Interaction:** Users can expand Host items to see help for sub-items.

**Limitations:**
*   **Modal:** Blocks interaction with standard items (except Hosts) until dismissed.

**Usage:**
~~~kotlin
azAdvanced(infoScreen = true)
azRailItem(..., info = "This goes to Home")
~~~

---

## Navigation Items

### Standard Items

**Description:** The basic clickable unit.

**Abilities:**
*   **Dynamic Content:** Accepts `Color`, `Int` (Resource ID), or `String` (Text).
*   **Shapes:** `CIRCLE`, `SQUARE` (Fixed size), `RECTANGLE` (Auto-width), `NONE` (Text only).
*   **Classifiers:** Tags for programmatic highlighting (e.g., "shared_route").

**Limitations:**
*   **ImageVectors:** Explicitly NOT supported (use Resource ID instead) due to Coil crashes.

**Usage:**
~~~kotlin
azRailItem(
    id = "home",
    text = "Home",
    content = R.drawable.ic_home, // Resource ID
    shape = AzButtonShape.SQUARE
)
~~~

### Nested Rails

**Description:** A secondary popup rail triggered by an item.

**Abilities:**
*   **Alignment:** `VERTICAL` (drops down) or `HORIZONTAL` (slides out).
*   **Positioning:** Automatically calculated relative to the anchor item.
*   **Offset:** Horizontal rails have a configurable margin (default 8dp).

**Limitations:**
*   **Single Level:** Nested rails cannot contain other nested rails.

**Usage:**
~~~kotlin
azNestedRail(id = "tools", text = "Tools", alignment = AzNestedRailAlignment.HORIZONTAL) {
    azRailItem("hammer", "Hammer")
}
~~~

### Reorderable Items (Drag & Drop)

**Description:** Items that can be rearranged by the user.

**Abilities:**
*   **Cluster Logic:** Items can only be moved within their contiguous "cluster" (neighbors sharing the same `hostId` and type).
*   **Interaction:**
    *   **Tap:** Selects/Focuses the item.
    *   **Long Press + Drag:** Moves the item within its cluster (vibration confirmation on grab).
    *   **Long Press (No Drag):** Opens the Hidden Context Menu or Nested Rail.
*   **Context Menu:** Supports `listItem` (actions) and `inputItem` (renaming).

**Limitations:**
*   **Minimum 2:** You need at least 2 items to form a cluster.
*   **Overlap Threshold:** Requires 40% overlap to trigger a swap.

**Usage:**
~~~kotlin
azRailRelocItem(id = "1", hostId = "favs", text = "A", onRelocate = { f, t, list -> }) {
    listItem("Delete") { }
}
~~~

### Host & Sub-Items

**Description:** Accordion-style hierarchy.

**Abilities:**
*   **Exclusive Expansion:** Only one Host can be open at a time. Opening another auto-collapses the first.
*   **Location:** Hosts can be in the Rail (always visible) or Menu (drawer only).

**Usage:**
~~~kotlin
azRailHostItem(id = "settings", text = "Settings")
azRailSubItem(id = "wifi", hostId = "settings", text = "WiFi")
~~~

---

## Interactive Components

### Toggles

**Description:** Binary state switch.

**Abilities:**
*   **Visuals:** Updates text/icon based on state.
*   **Feedback:** Haptic feedback on toggle.

**Usage:**
~~~kotlin
azRailToggle(id = "dark", isChecked = isDark, toggleOnText = "Dark", toggleOffText = "Light")
~~~

### Cyclers

**Description:** Multi-state button.

**Abilities:**
*   **Delay:** Built-in 1000ms delay before confirming selection to prevent accidental rapid cycling.
*   **Visuals:** Shows pending selection during delay.

**Limitations:**
*   **Validation:** Throws error if `selectedOption` is not in `options` list.

**Usage:**
~~~kotlin
azRailCycler(id = "mode", options = listOf("A", "B", "C"), selectedOption = "A")
~~~

### Rollers (Slot Machine)

**Description:** A dropdown that behaves like a physical roller.

**Abilities:**
*   **Split Interaction:** Left-click = Type/Filter. Right-click = Slot Machine Roll.
*   **Filtering:** Typing filters the list in real-time.
*   **Snapping:** Items snap into place when scrolling.

**Usage:**
~~~kotlin
AzRoller(options = listOf("1", "2", "3"), selectedOption = "1", onOptionSelected = {})
~~~

---

## Input Components

### AzTextBox

**Description:** Advanced text input.

**Abilities:**
*   **History:** Namespaced autocomplete history (LRU, max 5 items). Persists to file.
*   **Modes:** `multiline` (auto-expand) OR `secret` (password mask). Mutual exclusive.
*   **Controls:** Integrated Clear/Reveal and Submit buttons.

**Limitations:**
*   **Exclusivity:** Cannot be both `multiline` and `secret`. throws `IllegalArgumentException`.

**Usage:**
~~~kotlin
AzTextBox(hint = "Pass", secret = true, onSubmit = {})
~~~

### AzForm

**Description:** Group container for text boxes.

**Abilities:**
*   **Aggregation:** Collects values from all children into a Map.
*   **Traversal:** `Next` key moves focus. `Send` key on last item submits form.
*   **Unified Style:** Applies outline/color settings to all children.

**Usage:**
~~~kotlin
AzForm(formName = "login", onSubmit = { map -> }) {
    entry("user", "User")
    entry("pass", "Pass", secret = true)
}
~~~

---

## Advanced Features

### AzLoad (Loading)

**Description:** Global or local loading spinner.

**Abilities:**
*   **Overlay:** `azAdvanced(isLoading = true)` blocks entire UI with spinner.
*   **Standalone:** `AzLoad()` composable for local use.

### System Overlay

**Description:** Run the rail over other apps.

**Abilities:**
*   **Dynamic Resize:** Expands to screen size during drag; shrinks to wrap content when idle.
*   **Auto-Launch:** Clicking an item brings the app to foreground.

**Requirements:**
*   Must extend `AzNavRailOverlayService`.
*   Requires `SYSTEM_ALERT_WINDOW` permission.
