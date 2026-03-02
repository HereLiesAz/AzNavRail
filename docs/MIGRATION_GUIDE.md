# Migration Guide: Surviving the Automation Purge

If you are reading this, you are attempting to upgrade AzNavRail from its chaotic, manual-DSL era to the totalitarian KSP architecture.

## v7.25: The Live Dictatorship Migration

Version 7.25 introduces breaking changes to the annotation parameters to support dynamic binding and resolve cyclic dependencies.

### 1. Enum to String Literal Conversion
To prevent the `annotation` module from depending on the `core` library, all Enum-based parameters in annotations have been converted to `String`.

*   **@App**: `dock = AzDockingSide.LEFT` → `dock = "LEFT"`
*   **@NestedRail**: `alignment = AzNestedRailAlignment.VERTICAL` → `alignment = "VERTICAL"`
*   **@Theme**: `defaultShape = AzButtonShape.CIRCLE` → `defaultShape = "CIRCLE"`

### 2. Reactive Property Binding
If you want your rail items to react to Activity state, you must now use the `*Property` fields in the annotations.

**Old (Static):**
~~~kotlin
@Az(rail = RailItem(text = "Status", disabled = true))
~~~

**New (Reactive):**
~~~kotlin
@Az(rail = RailItem(textProperty = "currentStatus", disabledProperty = "isOffline"))
~~~

---

## Phase 1: Eradicate the Manual Graph

You previously built your UI inside `setContent { ... }` using a sprawling DSL block. 

**Action:** Delete it. Delete all of it. The `AzHostActivityLayout`, the `AzNavHost`, the `composable` routes. KSP generates this now.

## Phase 2: Surrender the Activity

Your `MainActivity` must now bow to the generated structural authority.

**The New Mandate:**
Extend `AzActivity` and declare the generated `AzGraph`.

~~~kotlin
@Az(app = App(dock = "LEFT"))
class MainActivity : AzActivity() {
    override val graph = AzGraph
}
~~~

## Phase 3: The Annihilation of `azSettings` and `azTheme`

The bloated `azSettings` function has been destroyed. **Furthermore, `azTheme` has been entirely eradicated.** Your app's `MaterialTheme` now controls the colors. 

Parameters like `expandedWidth`, `collapsedWidth`, and `showFooter` have been absorbed into `azConfig` and the `@App` annotation.

## Phase 4: The Help/Info System Overhaul (v8.00)

The help system has been promoted to a first-class citizen with explicit triggers and scrollable, interactive info cards.

### 1. Explicit Help triggers
The "automatic" FAB to exit help mode has been removed. Help mode must now be explicitly enabled in `azAdvanced` or `azSettings`.

*   **DSL:** `infoScreen` → `helpEnabled`, `onDismissInfoScreen` → `onDismissHelp`.
*   **Automatic Menu Item:** If `helpEnabled = true`, a "Help" item is automatically appended to your side menu.
*   **Dedicated Rail Item:** Use `azHelpRailItem(id, text)` to add an explicit help trigger to the rail.

### 2. Interactive Info Cards
Info cards in the Help Overlay are now scrollable and support tap-to-expand. To dismiss the overlay, tap anywhere on the dark background.

## Phase 5: Submit to the Synthetic Tongue

The `AzNavRailScope` has been stripped of its human conveniences. If you are dynamically creating items that the annotations cannot reach, you must use the explicit, monolithic function signatures. Look at `DSL.md` for the exact parameters.
