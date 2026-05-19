# Migrating from the Android library

This guide maps the Compose DSL surface exposed by the Android `aznavrail` library to
the React/JSX surface exposed by `@HereLiesAz/aznavrail-react`.

## Rail items

| Android DSL | React JSX |
| --- | --- |
| `azRailItem(id, text, ...)` | `<AzRailItem id text ... />` |
| `azMenuItem(id, text, ...)` | `<AzMenuItem id text ... />` |
| `azRailToggle(...)` / `azMenuToggle(...)` | `<AzRailToggle ... />` / `<AzMenuToggle ... />` |
| `azRailCycler(...)` / `azMenuCycler(...)` | `<AzRailCycler ... />` / `<AzMenuCycler ... />` |
| `azRailHostItem(...)` / `azMenuHostItem(...)` | `<AzRailHostItem ... />` / `<AzMenuHostItem ... />` |
| `azRailSubItem(...)` / `azMenuSubItem(...)` | `<AzRailSubItem ... />` / `<AzMenuSubItem ... />` |
| `azNestedRail(id, text, alignment) { ... }` | `<AzNestedRail id text alignment>... </AzNestedRail>` |
| `azRailRelocItem(id, hostId, ...) { hiddenMenu }` | `<AzRailRelocItem id hostId hiddenMenu={...}>` |
| `azDivider()` | `<AzDivider />` |
| `azHelpRailItem(id)` / `azHelpSubItem(id, hostId)` | `<AzHelpRailItem id />` / `<AzHelpSubItem id hostId />` |

## Rail configuration

Android exposes `azConfig`, `azTheme`, `azAdvanced`, and `azSettings` as DSL methods. The
React `<AzNavRail>` accepts the same parameters as flat props on the component itself.

```kotlin
// Android
azConfig(packButtons = true, dockingSide = AzDockingSide.RIGHT)
azTheme(activeColor = MaterialTheme.colorScheme.primary)
```

```tsx
// React
<AzNavRail packRailButtons dockingSide={AzDockingSide.RIGHT} activeColor="#0066cc">
  ...
</AzNavRail>
```

## Bottom sheets

The Android library registers sheets via `AzNavHostScope.azBottomSheet(controller, config, ...)`
inside the layout DSL. The React equivalent is a regular component:

```tsx
import { AzBottomSheet, AzBottomSheetInsetAware, useAzSheetController, AzSheetDetent } from '@HereLiesAz/aznavrail-react';

function MyScreen() {
  const controller = useAzSheetController(AzSheetDetent.PEEK);
  return (
    <>
      <Content />
      <AzBottomSheet
        controller={controller}
        config={{ horizontalSwipeEnabled: true }}
        onSwipeLeft={() => navigateLeft()}
        onSwipeRight={() => navigateRight()}
      >
        <SheetBody />
      </AzBottomSheet>
    </>
  );
}
```

| Android | React |
| --- | --- |
| `azBottomSheet(controller, config, onSwipeLeft, onSwipeRight) { content }` | `<AzBottomSheet controller config onSwipeLeft onSwipeRight>{content}</AzBottomSheet>` |
| `AzBottomSheetInsetAware(...)` | `<AzBottomSheetInsetAware ...>` |
| `rememberAzSheetController()` | `useAzSheetController()` |
| `AzSheetController.snapTo(target)` | `controller.snapTo(target)` |
| `AzSheetController.stepUp() / stepDown()` | `controller.stepUp() / controller.stepDown()` |
| `AzSheetConfig(halfFraction = 0.6, ...)` | `{ halfFraction: 0.6, ... }` |

## Tutorials

| Android | React |
| --- | --- |
| `azTutorial { scene(id) { card(...) } }` | `{ scenes: [{ id, cards: [{ title, text, ... }] }] }` (plain objects) |
| `LocalAzTutorialController.current.startTutorial(id)` | `const ctrl = useAzTutorialController(); ctrl.startTutorial(id)` |
| `AzAdvanceCondition.Event("name")` | `{ kind: 'Event', name: 'name' }` |

The TypeScript interfaces in `src/types.ts` document the exact shapes.

## Platform-only features

See [`KNOWN_GAPS.md`](./KNOWN_GAPS.md) for capabilities that exist on Android but cannot
be expressed in React / React Native (system overlay services, `AzActivity` base class,
`secLoc` TCP server).

## What's new in 0.2.0

- `AzBottomSheet`, `AzBottomSheetInsetAware`, `useAzSheetController`, `AzSheetDetent`,
  `AzSheetConfig` — full port of the LogKitty-style four-detent sheet from Android
  commit `da9e1be feat(aznavrail): port LogKitty bottom-sheet shell as first-class library feature`.
- `AzFloatingRail` — documented stand-in for the Android system-overlay services.
- `KNOWN_GAPS.md` documenting the platform-shaped gaps.
