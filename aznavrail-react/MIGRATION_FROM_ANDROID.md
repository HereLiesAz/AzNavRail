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
| `azRailSubHostItem(...)` / `azMenuSubHostItem(...)` | `<AzRailSubHostItem ... />` / `<AzMenuSubHostItem ... />` |
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

## Interaction callback

The `onInteraction` callback fires whenever a user interacts with any rail item.

| Android | React |
| --- | --- |
| `azAdvanced(onInteraction = { id, item -> ... })` | `<AzNavRail onInteraction={(action, details, item) => ...}>` |

The Android callback receives `(itemId: String, item: AzNavItem)`. The React callback receives `(action: string, details?: string, item?: AzNavItem)` — the third parameter was added for parity and may be `undefined` for non-item actions (header tap, drag start, etc.).

## What's new in 0.3.0

- Bottom sheet **drag-to-collapse is gentler**: a downward drag now steps **down one detent**
  (`FULL → HALF → PEEK → HIDDEN`) instead of snapping straight to HIDDEN, mirroring the
  up-drag's one-step expand. Matches the Android `9.2` `AzSheetGestures` change.
- Parity note: `<AzBottomSheet config={...}>` already recomputes detent heights when the
  `config` prop changes, which is the React analog of the Android `AzBottomSheetWindowHost.updateConfig()` live-resize fix in `9.2`. Window insets remain handled by
  `AzBottomSheetInsetAware` (see `KNOWN_GAPS.md`).
- No web analog: the Android `9.2` `AzSheetConfig.drawBehindNavBar` (draw the sheet behind a
  forced-transparent system navigation bar on button-nav devices) and the automatic
  gesture-navigation zero-bottom-margin have no equivalent on the web (no system navigation bar
  or navigation mode). See `KNOWN_GAPS.md`.

## What's new in 0.2.0

- `AzBottomSheet`, `AzBottomSheetInsetAware`, `useAzSheetController`, `AzSheetDetent`,
  `AzSheetConfig` — full port of the LogKitty-style four-detent sheet from Android
  commit `da9e1be feat(aznavrail): port LogKitty bottom-sheet shell as first-class library feature`.
- `AzFloatingRail` — documented stand-in for the Android system-overlay services.
- `KNOWN_GAPS.md` documenting the platform-shaped gaps.
- `onInteraction` callback now passes `AzNavItem` as 3rd argument for item-level analytics.
- Bottom sheet swipe-down snaps directly to HIDDEN for quick dismissal.
- Bottom sheet adds transparent tap overlay at PEEK for improved discoverability.
