# Changelog

## 0.2.0

### Added
- `AzBottomSheet` — cross-platform port of the Android `AzBottomSheet`, including the
  four-detent (HIDDEN/PEEK/HALF/FULL) state machine, drag handle, scrim, optional
  horizontal-swipe callbacks, and animated detent transitions. Built on the React Native
  core `Animated` + `PanResponder` APIs so no new peer dependencies are introduced.
- `AzBottomSheetInsetAware` — sibling that wraps the sheet in a `SafeAreaView` on native
  and applies `padding-bottom: env(safe-area-inset-bottom)` on web.
- `useAzSheetController` hook + `AzSheetController` type — imperative controller with
  `detent`, `isEnabled`, `setDetent`, `setEnabled`, `stepUp`, `stepDown`, and `snapTo`.
- `AzSheetDetent` enum and `AzSheetConfig` interface mirroring the Android model types.
- `AzFloatingRail` — documented stand-in for the Android system-overlay services.
  Web: `position: fixed` host with `PanResponder` drag. Native: transparent fullscreen
  `Modal`. See `KNOWN_GAPS.md` for the parity caveats.
- `MIGRATION_FROM_ANDROID.md` mapping the Compose DSL surface to the React JSX surface.
- `KNOWN_GAPS.md` documenting platform-shaped gaps (`SYSTEM_ALERT_WINDOW`, `AzActivity`,
  the secret-screen TCP sync, the inset-aware variant on web).
- `AzRailSubHostItem` / `AzMenuSubHostItem` — a sub-item that is itself a host. Hosts now
  nest to any depth: opening a sub-host reveals its children inline while sibling sub-items
  stay visible. Children attach to their host by `hostId` reference. Rail rendering is a
  single recursive path with cycle detection so a self-referential or cyclic `hostId`
  cannot loop.

### Changed
- `onInteraction` callback now passes `AzNavItem` as 3rd argument for item-level analytics.
- Bottom sheet swipe-down snaps directly to `HIDDEN` instead of stepping one detent.
- Bottom sheet adds transparent tap overlay at `PEEK` detent for tap-to-dismiss.

### Notes
Catches the React port up with Android commit `da9e1be feat(aznavrail): port LogKitty
bottom-sheet shell as first-class library feature`.

## 0.1.0

Initial pre-release.
