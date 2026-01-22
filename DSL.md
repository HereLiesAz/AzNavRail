# AzNavRail DSL Reference

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view. Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

## AzHostActivityLayout Scope

**Note:** The `AzHostActivityLayout` is the mandatory entry point for the library. It establishes the `AzNavHostScope` which extends `AzNavRailScope`.

When using `AzHostActivityLayout`, the following additional DSL functions are available:

-   `background(weight: Int, content: @Composable () -> Unit)`: Adds a background layer. Backgrounds ignore strict layout rules (safe zones) and fill the screen. Layers are stacked based on `weight` (lower weights are drawn first).
-   `onscreen(alignment: Alignment, content: @Composable () -> Unit)`: Adds content to the safe area.
    -   **Safe Zones**: Content is restricted from the top 20% and bottom 10% of the screen.
    -   **Rail Avoidance**: Content is padded to avoid the rail.
    -   **Alignment Flipping**: If the rail is docked to the Right, horizontal alignments are flipped (e.g., `Start` becomes `End`).

## AzNavRail Scope

The configuration is split into three distinct sectors: `azTheme` (Visuals), `azConfig` (Behavior), and `azAdvanced` (Special Ops).

### azTheme (Visuals)

Configures the visual appearance of the rail.

```kotlin
azTheme(
    expandedRailWidth: Dp,
    collapsedRailWidth: Dp,
    defaultShape: AzButtonShape,
    headerIconShape: AzHeaderIconShape,
    activeColor: Color?,
    showFooter: Boolean
)
```

-   **`expandedRailWidth`**: Width of the rail when expanded.
-   **`collapsedRailWidth`**: Width of the rail when collapsed.
-   **`defaultShape`**: Default shape for buttons (`CIRCLE`, `ROUNDED`, `RECTANGLE`).
-   **`headerIconShape`**: Shape of the header icon (`CIRCLE`, `ROUNDED`, `NONE`).
-   **`activeColor`**: Optional secondary color for the selected item.
-   **`showFooter`**: Shows or hides the footer.

### azConfig (Behavior)

Configures the behavior and features of the rail.

```kotlin
azConfig(
    displayAppName: Boolean,
    packButtons: Boolean,
    dockingSide: AzDockingSide,
    noMenu: Boolean,
    vibrate: Boolean,
    activeClassifiers: Set<String>
)
```

-   **`displayAppName`**: Whether to display the app name in the header.
-   **`packButtons`**: Reduces vertical spacing between rail items.
-   **`dockingSide`**: Side of the screen to dock the rail (`LEFT` or `RIGHT`).
-   **`noMenu`**: Disables the expanded menu.
-   **`vibrate`**: Enables tactile feedback for gestures.
-   **`activeClassifiers`**: Set of classifiers to mark items as active aside from route/selection.

### azAdvanced (Special Ops)

Configures advanced operations, overlays, and state.

```kotlin
azAdvanced(
    isLoading: Boolean,
    enableRailDragging: Boolean,
    onUndock: (() -> Unit)?,
    overlayService: Class<out android.app.Service>?,
    onOverlayDrag: ((Float, Float) -> Unit)?,
    onItemGloballyPositioned: ((String, Rect) -> Unit)?,
    infoScreen: Boolean,
    onDismissInfoScreen: (() -> Unit)?
)
```

-   **`isLoading`**: Shows a loading indicator on the rail.
-   **`enableRailDragging`**: Allows the rail to be dragged (typically for overlays).
-   **`onUndock`**: Callback for when the undock button is clicked.
-   **`overlayService`**: The Service class to launch as a system overlay when undocked.
-   **`onOverlayDrag`**: Callback to handle drag events when running in an overlay.
-   **`onItemGloballyPositioned`**: Reports the global position of rail items (useful for help overlays).
-   **`infoScreen`**: Enables the interactive help mode overlay.
-   **`onDismissInfoScreen`**: Callback when the help mode exit FAB is clicked.

### Items

-   `azMenuItem(id: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azMenuItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a menu item that only appears in the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text with the `\n` character.
-   `azRailItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azRailItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a rail item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail. Supports multi-line text in the expanded menu.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle item that only appears in the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azRailToggle(id: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle item that appears in both the collapsed rail and the expanded menu. Tapping it executes the action and collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler item that only appears in the expanded menu. Tapping it cycles through options and executes the `onClick` action for the final selection after a 1-second delay, then collapses the rail.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azRailCycler(id: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler item that appears in both the collapsed rail and the expanded menu. The behavior is the same as `azMenuCycler`.
-   `azDivider()`: Adds a horizontal divider to the expanded menu.
-   **`screenTitle`**: An optional parameter for all item functions that displays a title on the screen when the item is selected. The title will be **force-aligned to the right** of the screen. If no `screenTitle` is provided, the item's `text` will be used instead. To prevent a title from being displayed, use `screenTitle = AzNavRail.noTitle`.
-   **`route`**: An optional parameter for all item functions that specifies the route to navigate to when the item is clicked. This is used for integration with Jetpack Navigation.
-   **`info`**: An optional parameter for all item functions that specifies the text to display in the interactive help screen mode.
-   `azMenuHostItem(id: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a host item that only appears in the expanded menu.
-   `azMenuHostItem(id: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a host item that only appears in the expanded menu.
-   `azRailHostItem(id: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a host item that appears in both the collapsed rail and the expanded menu.
-   `azRailHostItem(id: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a host item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a sub item that only appears in the expanded menu.
-   `azMenuSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a sub item that only appears in the expanded menu.
-   `azRailSubItem(id: String, hostId: String, text: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a sub item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubItem(id: String, hostId: String, text: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a sub item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item that only appears in the expanded menu.
-   `azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item that only appears in the expanded menu.
-   `azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubToggle(id: String, hostId: String, color: Color?, isChecked: Boolean, toggleOnText: String, toggleOffText: String, shape: AzButtonShape?, route: String, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item that appears in both the collapsed rail and the expanded menu.
-   `azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that only appears in the expanded menu.
-   `azMenuSubCycler(id: String, hostId: String, options: List<String>, selectedOption: String, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that only appears in the expanded menu.
-   `azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
-   `azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
-   `azRailRelocItem(id: String, hostId: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit)`: Adds a draggable sub-item that can be reordered within its cluster.
-   `azRailRelocItem(id: String, hostId: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit)`: Adds a draggable sub-item with a navigation route.
