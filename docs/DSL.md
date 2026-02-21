# AzNavRail DSL Reference

The DSL for configuring the `AzNavRail`.

**Note:** Functions prefixed with `azMenu` will only appear in the expanded menu view.
Functions prefixed with `azRail` will appear on the collapsed rail, and their text will be used as the label in the expanded menu.

## AzHostActivityLayout Scope
**Note:** The `AzHostActivityLayout` is the mandatory entry point for the library.
It establishes the `AzNavHostScope` which extends `AzNavRailScope`. When using `AzHostActivityLayout`, the following additional DSL functions are available:

- `background(weight: Int, content: @Composable () -> Unit)`: Adds a background layer.
  Backgrounds ignore strict layout rules (safe zones) and fill the screen.
  Layers are stacked based on `weight` (lower weights are drawn first).
- `onscreen(alignment: Alignment, content: @Composable () -> Unit)`: Adds content to the safe area.
    - **Safe Zones**: Content is restricted from the **Top 20%** and **Bottom 10%** of the screen.
    - **Rail Avoidance**: Content is padded to avoid the rail.
    - **Alignment Flipping**: If the rail is docked to the Right, horizontal alignments are flipped (e.g., `Start` becomes `End`).

## AzNavRail Scope

The configuration is split into three distinct sectors: `azTheme` (Visuals), `azConfig` (Behavior), and `azAdvanced` (Special Ops).

### azTheme (Visuals)
Configures the visual appearance of the rail.

~~~kotlin
azTheme(
    defaultShape: AzButtonShape,
    headerIconShape: AzHeaderIconShape,
    activeColor: Color?
)
~~~
- **`defaultShape`**: Default shape for buttons (`CIRCLE`, `ROUNDED`, `RECTANGLE`).
- **`headerIconShape`**: Shape of the header icon (`CIRCLE`, `ROUNDED`, `NONE`).
- **`activeColor`**: Optional secondary color for the selected item.

### azConfig (Behavior)
Configures the behavior and features of the rail.

~~~kotlin
azConfig(
    displayAppName: Boolean,
    packButtons: Boolean,
    dockingSide: AzDockingSide,
    noMenu: Boolean,
    vibrate: Boolean,
    activeClassifiers: Set<String>,
    usePhysicalDocking: Boolean,
    expandedWidth: Dp,
    collapsedWidth: Dp,
    showFooter: Boolean
)
~~~
- **`displayAppName`**: Whether to display the app name in the header.
- **`packButtons`**: Reduces vertical spacing between rail items.
- **`dockingSide`**: Side of the screen to dock the rail (`LEFT` or `RIGHT`).
- **`noMenu`**: Disables the expanded menu.
- **`vibrate`**: Enables tactile feedback for gestures.
- **`activeClassifiers`**: Set of classifiers to mark items as active aside from route/selection.
- **`usePhysicalDocking`**: If `true`, the rail anchors to the physical side of the device, adapting to rotation (e.g., LEFT in Portrait becomes RIGHT in Upside Down). If `false` (default), it anchors to the screen side (View) regardless of rotation.
- **`expandedWidth`**: Width of the rail when expanded.
- **`collapsedWidth`**: Width of the rail when collapsed.
- **`showFooter`**: Shows or hides the footer.

### azAdvanced (Special Ops)
Configures advanced operations, overlays, and state.

~~~kotlin
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
~~~
- **`isLoading`**: Shows a loading indicator on the rail.
- **`enableRailDragging`**: Allows the rail to be dragged (typically for overlays).
- **`onUndock`**: Callback for when the undock button is clicked.
- **`overlayService`**: The Service class to launch as a system overlay when undocked.
- **`onOverlayDrag`**: Callback to handle drag events when running in an overlay.
- **`onItemGloballyPositioned`**: Callback fired when an item updates its bounds.
- **`infoScreen`**: Toggles the interactive help mode.
- **`onDismissInfoScreen`**: Callback when the help screen is dismissed.

### Items

- `azMenuItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds an item that only appears in the expanded menu.
- `azRailItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit)`: Adds an item that appears in both the collapsed rail and the expanded menu.
- `azMenuToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle button to the expanded menu.
- `azRailToggle(id: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle button to both the collapsed rail and the expanded menu.
- `azMenuCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler button to the expanded menu.
- `azRailCycler(id: String, options: List<String>, selectedOption: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler button to both the collapsed rail and the expanded menu.
- `azDivider()`: Adds a visual divider to the menu.
- `azMenuHostItem(id: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a parent host item to the menu.
- `azRailHostItem(id: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a parent host item to the rail.
- `azMenuSubItem(id: String, hostId: String, text: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a child item to a host in the menu.
- `azRailSubItem(id: String, hostId: String, text: String, route: String?, content: Any?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, classifiers: Set<String>, onFocus: (() -> Unit)?, onClick: () -> Unit)`: Adds a child item to a host in the rail.
- `azMenuSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item to the expanded menu.
- `azRailSubToggle(id: String, hostId: String, isChecked: Boolean, toggleOnText: String, toggleOffText: String, route: String?, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a toggle sub-item to both the collapsed rail and the expanded menu.
- `azMenuSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
- `azRailSubCycler(id: String, hostId: String, color: Color?, options: List<String>, selectedOption: String, shape: AzButtonShape?, route: String, disabled: Boolean, disabledOptions: List<String>?, screenTitle: String?, info: String?, onClick: () -> Unit)`: Adds a cycler sub-item that appears in both the collapsed rail and the expanded menu.
- `azRailRelocItem(id: String, hostId: String, text: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit)`: Adds a draggable sub-item that can be reordered within its cluster.
- `azRailRelocItem(id: String, hostId: String, text: String, route: String, color: Color?, shape: AzButtonShape?, disabled: Boolean, screenTitle: String?, info: String?, onClick: (() -> Unit)?, onRelocate: ((Int, Int, List<String>) -> Unit)?, hiddenMenu: HiddenMenuScope.() -> Unit)`: Adds a draggable sub-item with a navigation route.