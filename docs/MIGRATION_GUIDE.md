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

---

## Migrating to the Expanded Tutorial Framework

The tutorial framework now ships with four advance conditions, two branching mechanisms, checklist cards, media cards, cross-platform persistence, and a Web port.

### Existing cards remain valid

All new fields on `AzCard` — `advanceCondition`, `branches`, `mediaContent`, `checklistItems` — have safe defaults and are optional. Existing `card(title, text, highlight, actionText, onAction)` call sites continue to compile and behave identically. No changes are required unless you want to use the new features.

### One breaking change: `AzTutorialController.Saver`

Previously `AzTutorialController.Saver` could be referenced as a plain `val`. It is now a function that requires a `Context` argument:

**Old:**
~~~kotlin
rememberSaveable(saver = AzTutorialController.Saver) { AzTutorialController() }
~~~

**New:**
~~~kotlin
rememberSaveable(saver = AzTutorialController.Saver(context)) { AzTutorialController(context) }
~~~

**Who is affected:** Only callers who explicitly used `AzTutorialController.Saver` in their own `rememberSaveable` calls. The standard entrypoint `rememberAzTutorialController()` handles this internally and is unaffected.

### `startTutorial` now accepts variables

The signature of `startTutorial` gains an optional second parameter:

~~~kotlin
// Old (still valid — variables defaults to emptyMap())
controller.startTutorial("tut-1")

// New — pass variables to drive scene-level branching
controller.startTutorial("tut-1", variables = mapOf("userLevel" to "advanced"))
~~~

TypeScript:
~~~typescript
// Old (still valid)
controller.startTutorial('tut-1');

// New
controller.startTutorial('tut-1', { userLevel: 'advanced' });
~~~

### Help/Info Overlay behavior change

The old behavior — any tap on a collapsed help card immediately started the tutorial — is removed. The new flow is:

1. **Collapsed card:** Shows a "Tutorial available" hint when a tutorial exists for that item. No action on tap.
2. **Expanded card:** Shows a "Start Tutorial" button. Tapping it calls `startTutorial` and dismisses the overlay.

If your app relied on the old tap-to-launch behavior you will need to update any UI that expected tapping a collapsed card to start the tutorial.

### New Web provider (distinct from React Native)

The Web port introduces `AzWebTutorialProvider` and `useAzWebTutorialController` — separate exports from the React Native `AzTutorialProvider` / `useAzTutorialController`. Do not import the RN provider in a web build or vice versa.

~~~typescript
// React Native
import { AzTutorialProvider, useAzTutorialController } from '@HereLiesAz/aznavrail-react';

// Web
import { AzWebTutorialProvider, useAzWebTutorialController } from '@HereLiesAz/aznavrail-web';
~~~
