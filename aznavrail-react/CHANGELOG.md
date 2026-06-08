# Changelog

## 0.3.0

### Changed
- **Bottom sheet drag-to-collapse is now gentler.** A downward drag steps the sheet **down
  one detent** (FULL → HALF → PEEK → HIDDEN) instead of snapping straight to `HIDDEN`,
  mirroring the up-drag's one-step `stepUp()`. Tap-to-dismiss overlays (the scrim at
  HALF/FULL and the tap layer at PEEK) still call `stepDown()` as before.

### Notes
- Runtime resizing already works on the React port: `<AzBottomSheet config={...}>` recomputes
  the detent heights whenever the `config` prop (`peekDp` / `hiddenStripDp` / fractions)
  changes, so no `updateConfig()` method is needed — re-render with a new `config`. This is
  the React analog of the Android `AzBottomSheetWindowHost.updateConfig()` live-resize fix.
- Window insets continue to be delivered via `AzBottomSheetInsetAware` (`SafeAreaView` on
  native, `env(safe-area-inset-bottom)` on web). See `KNOWN_GAPS.md` for the web caveat.
- Tracks the Android `9.2` release (live overlay-window resize on `updateConfig()`, window
  insets delivered to sheet content, and the matching gentler drag-to-collapse).
- The Android `9.2` navigation-mode behaviors — `AzSheetConfig.drawBehindNavBar` and the
  automatic gesture-nav zero-bottom-margin — have **no web analog** (no system navigation bar /
  navigation mode on web). See `KNOWN_GAPS.md`.

## 0.2.0

### Added
- Rail-item `content` now accepts an **image source** (`require()` id or `{ uri }`) in addition
  to a React node, and any graphic content (image source, `<Image>`, or a `react-native-svg`
  `<Svg>`) **fills the item's shape** — scaled to cover and clipped — without changing the
  item's dimensions. `<Image>` elements are coerced to `resizeMode="cover"`; other elements
  are stretched to 100% × 100%. (Mirrors the Android `ImageVector`/`Painter` fill behavior.)
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
