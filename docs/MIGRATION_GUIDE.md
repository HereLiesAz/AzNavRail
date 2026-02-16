# Migration Guide

## Upgrading to the latest version

### Visual Changes
- **Rail Item Fill**: `azRailItem` content that is a `Color`, an Image Resource ID (`Int`), or an Image URL/Model (`Any`) now fills the entire button shape (Fill/Crop) with 0 padding. Previously, these items had default padding and fit inside the button. If you relied on the padding, wrap your content in a Composable that adds padding (if using custom content) or accept the new full-bleed style.

### API Changes
- **Nested Rail**: A new `azNestedRail` DSL function is available for creating hierarchical menus that open in a popup overlay. This is distinct from the existing `azRailHostItem`/`azMenuHostItem` which expand inline.
- **Orientation Handling**: A new experimental setting `usePhysicalDocking` has been added to `azConfig`.
    - `false` (Default): The rail anchors to the side of the *screen* (View) specified by `dockingSide`.
    - `true`: The rail anchors to the physical side of the *device*. For example, if docked LEFT in Portrait, it will appear on the RIGHT in Reverse Portrait (upside down).

### Fixes
- **Nested Rail Rendering**: Fixed an issue where Nested Rails were being composed multiple times.
