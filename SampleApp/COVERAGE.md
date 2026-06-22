# SampleApp coverage of the AzNavRail public API

Each row links a public symbol to the screen or rail item that exercises it. If a symbol
appears in the public surface but has no entry here, the demo is incomplete and the row
should be added before shipping.

## Top-level composables

| Symbol | Exercised by |
| --- | --- |
| `AzHostActivityLayout` | `MainApp.kt` |
| `AzNavHost` | `MainApp.kt` |
| `AzNavRail` (transitively via `AzHostActivityLayout`) | `MainApp.kt` |

## Rail DSL (`AzNavRailScope` / `AzNavHostScope`)

| DSL | Exercised by |
| --- | --- |
| `azConfig` (all params: `vibrate`, `displayAppName`, `activeClassifiers`, `expandedWidth`, `collapsedWidth`, `showFooter`, `appRepositoryUrl`, `packButtons`, `noMenu`, `usePhysicalDocking`, `dockingSide`) | `MainApp.kt`, driven by `CustomizationDemoScreen` + legacy toggles |
| `azTheme` (`activeColor`, `defaultShape`, `headerIconShape`, `translucentBackground`, `helpLineColors`) | `MainApp.kt`, driven by `CustomizationDemoScreen` |
| `azAdvanced` (`isLoading`, `helpEnabled`, `onDismissHelp`, `enableRailDragging`, `onRailDrag`, `onOverlayDrag`, `onUndock`, `helpList`, `tutorials`) | `MainApp.kt`, driven by `FabOverlayDemoScreen`, `HelpSystemDemoScreen`, `TutorialDemoScreen` |
| `azMenuItem` | `MainApp.kt` (10× showcase menu items) |
| `azRailItem` (Color/ResourceId/ImageVector/AzComposableContent content variants, shape, disabled, classifiers, info) | `MainApp.kt` rail items (`color-item`, `icon-item`, `vector-item`) |
| `azRailToggle` / `azMenuToggle` | `MainApp.kt` (pack-rail, online, dark-mode, docking-side, no-menu, physical-docking) |
| `azRailCycler` / `azMenuCycler` (`disabledOptions`) | `MainApp.kt` (rail-cycler with disabled "C", menu-cycler) |
| `azRailHostItem` / `azMenuHostItem` (`initiallyExpanded`, `expandWhen`) | `MainApp.kt` (`expandWhen` driven by `expand-when-demo` `azMenuToggle`) |
| `azRailSubItem` / `azMenuSubItem` | `MainApp.kt` |
| `azRailSubHostItem` / `azMenuSubHostItem` (nested host under a host) | `MainApp.kt` (`rail-subhost`, `menu-subhost`) |
| `azRailSubToggle` / `azMenuSubToggle` | `MainApp.kt` (sub-toggle under menu-host) |
| `azRailSubCycler` / `azMenuSubCycler` | `MainApp.kt` (sub-cycler under rail-host) |
| `azHelpRailItem` | `MainApp.kt` (toggle-help) |
| `azHelpSubItem` | `MainApp.kt` (menu-host-help) |
| `azDivider` (DSL) | `MainApp.kt` (×3) |
| `azNestedRail` (VERTICAL + HORIZONTAL, custom `AzComposableContent` child) | `MainApp.kt` (nested-rail vertical, nested-horizontal) |
| `azRailRelocItem` (with `HiddenMenuScope`, `onRelocate`, `nestedContent`) | `MainApp.kt` (reloc-1, reloc-2, reloc-nested-parent) |
| `HiddenMenuScope.listItem(text, onClick)` | reloc-1 |
| `HiddenMenuScope.listItem(text, route)` | reloc-1 ("Open standalone widgets") |
| `HiddenMenuScope.inputItem(hint, onValueChange)` | reloc-2 ("Nickname") |
| `HiddenMenuScope.inputItem(hint, initialValue, onValueChange)` | reloc-2 ("Tag", initial "foo") |
| `background(weight)` | `MainApp.kt` (weights 0 + 10) |
| `onscreen(alignment)` | `MainApp.kt` (TopStart, TopEnd, Center) |
| `azBottomSheet { ... }` DSL | `BottomSheetDemoScreen.kt` (calls `AzBottomSheet` directly inside the screen — equivalent behaviour, demonstrated standalone) |

## Standalone composables

| Symbol | Exercised by |
| --- | --- |
| `AzButton` (all `AzButtonShape` values, `isLoading`, `enabled`, `contentPadding`) | `StandaloneWidgetsScreen`, `LegacyRailDemoScreen`, `BottomSheetDemoScreen`, others |
| `AzToggle` (all shapes) | `StandaloneWidgetsScreen`, `BottomSheetDemoScreen`, others |
| `AzCycler` | `StandaloneWidgetsScreen`, `LegacyRailDemoScreen` |
| `AzRoller` | `StandaloneWidgetsScreen`, `LegacyRailDemoScreen` |
| `AzTextBox` (controlled/uncontrolled, history, password toggle, clear button, custom fill, outlined/un-outlined, isError, leadingIcon) | `FormShowcaseScreen`, `LegacyRailDemoScreen` |
| `AzForm` + `AzFormScope.entry` (every param: `multiline`, `secret`, `leadingIcon`, `isError`, `enabled`, `keyboardOptions`, `keyboardActions`, `initialValue`) | `FormShowcaseScreen` |
| `AzLoad` | `StandaloneWidgetsScreen` |
| `AzDivider` (both orientations) | `StandaloneWidgetsScreen` |
| `AzBottomSheet` | `BottomSheetDemoScreen` |
| `AzBottomSheetInsetAware` | `BottomSheetDemoScreen` (toggleable) |
| `AzTutorialOverlay` (transitive via `AzTutorialController.startTutorial`) | `TutorialDemoScreen` |
| `EqualWidthLayout` | `StandaloneWidgetsScreen` |
| `AutoSizeText` | `StandaloneWidgetsScreen` |

## State holders / controllers

| Symbol | Exercised by |
| --- | --- |
| `rememberAzSheetController` | `BottomSheetDemoScreen` |
| `AzSheetController` (`detent`, `isEnabled`, `stepUp`, `stepDown`, `snapTo`) | `BottomSheetDemoScreen` |
| `rememberAzTutorialController` (via `AzHostActivityLayout`) | `TutorialDemoScreen` |
| `AzTutorialController.startTutorial`, `endTutorial`, `fireEvent`, `markTutorialRead` | `TutorialDemoScreen` |
| `LocalAzTutorialController` | `TutorialDemoScreen` |
| `AzTextBoxDefaults.setSuggestionLimit`, `setBackgroundColor`, `setBackgroundOpacity` | `FormShowcaseScreen` |

## Tutorial DSL & models

| Symbol | Exercised by |
| --- | --- |
| `azTutorial` builder | `TutorialDemoScreen` (`SampleTutorials`) |
| `AzTutorialBuilder.scene` / `AzSceneBuilder.card` / `AzSceneBuilder.branch` | `SampleTutorials` |
| `AzAdvanceCondition.Button` / `.TapTarget` / `.TapAnywhere` / `.Event` | `SampleTutorials` |
| `AzHighlight.Area` / `.Item` / `.FullScreen` / `.None` | `SampleTutorials` (Item, FullScreen, None — Area is also reachable via Item internally) |
| `AzCard.checklistItems` / `mediaContent` | `SampleTutorials` |
| `AzScene.branchVar` + `branches` | `SampleTutorials.branching-demo` |

## Services

| Symbol | Exercised by |
| --- | --- |
| `AzNavRailSimpleOverlayService` | `SampleOverlayService` (extends it) |
| `AzNavRailOverlayController` | Implicit via `AzNavRailWindowService` |

## Enums / models

Every enum (`AzButtonShape`, `AzDockingSide`, `AzOrientation`, `AzHeaderIconShape`,
`AzNestedRailAlignment`, `AzSheetDetent`) is reached either through a DSL parameter,
a demo screen control, or a standalone widget showcase.

## Known gaps in coverage

- `azRailHostItem` / `azMenuHostItem` `expandWhen` parameter — demoed via the
  `expand-when-demo` `azMenuToggle` (covered). A tutorial-integrated version (reading
  `activeTutorialId` from `LocalAzTutorialController`) is a stretch goal if desired.
  The toggle demo already exercises both edges (false→true expand, true→false collapse) and
  the user-wins rule (manual collapse while condition is `true`).
- `AzNavRailOverlayService` (foreground variant with `getNotification()`) is not
  instantiated — the demo extends `AzNavRailSimpleOverlayService` instead. The foreground
  variant is functionally equivalent for the purposes of this showcase.
- `AzActivity` / `AzGraphInterface` (KSP-generated) — not migrated. The existing
  `MainActivity` continues to work.
- `secLoc` / `secLocPort` — not configured in the showcase since wiring a real TCP server
  from a demo would be misleading.
