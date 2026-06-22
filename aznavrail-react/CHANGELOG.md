# Changelog

## 0.4.1

### Fixed
- **`onExpandedChange` spurious firing.** The callback previously shared a `useEffect` with the
  width animation, causing it to fire whenever `expandedRailWidth` or `collapsedRailWidth` changed
  (e.g. from the Customization screen) rather than only on actual rail expand/collapse transitions.
  The effects are now split — the callback fires exclusively on `isExpanded` state changes.

### Added
- **`onExpandedChange` and `onInteraction` on `AzHostActivityLayoutProps`.** Both callbacks were
  already handled by the inner `AzNavRail` but were missing from the host layout's TypeScript
  interface, making them invisible to callers using the typical `<AzHostActivityLayout>` entry
  point. They are now declared on the props type and flow through the spread to `AzNavRail`
  automatically.
- **`onExpandedChange` demo in the React sample app.** `sample-pwa` now passes `onExpandedChange`
  to `AzHostActivityLayout` and displays the live expansion state on the Showcase Home screen,
  providing a working reference implementation.

## 0.4.0

### Added
- **`AzDropdownMenu` (+ `AzDropdownItem`).** A standalone, **app-icon** drop-down declared with the
  rail's opinionated surface — it accepts only what the rest of the library sanctions (no arbitrary
  icon tint/source, panel background, offsets, or `menuWidth`). The trigger is the app icon (gray
  placeholder on RN, `/app-icon.png` on web), dropped inline, with configurable `headerIconShape`/
  `headerIconSize` (mirroring the rail's `azTheme`). Configured by `design`
  (`AzDropdownDesign` RAIL/MENU → panel width) and `dockingSide` (`AzDockingSide` LEFT/RIGHT screen
  edge); the panel drops from the trigger (RN `Modal` overlay; web fixed panel). `<AzDropdownItem>`
  entries accept the sanctioned per-item knobs plus a `route` dispatched through the menu's
  `onNavigate` (AzNavHost-style routing). Controlled `expanded`/`onExpandedChange`. The `MENU` design
  renders rows at the rail's menu-item text size (16px) and carries the rail's footer
  (About / Feedback / @HereLiesAz, gated by `showFooter`, with `appRepositoryUrl` behind "About").
  Reaches parity with the Android `AzDropdownMenu` DSL.
- **Sizable header icon** (`headerIconSize`) and the **in-app About reader + "More from Az"**
  carousel (`appRepositoryUrl`, `inAppAbout`, `moreRailItem`) reach parity with Android.
- **`expandWhen` qualifier for host items.** All four host-item builders (`AzRailHostItem`,
  `AzMenuHostItem`, `AzRailSubHostItem`, `AzMenuSubHostItem`) and the `AzHostItemProps`
  interface accept an optional `expandWhen?: () => boolean` prop. When the function's return
  value transitions **false→true** the host auto-expands; **true→false** auto-collapses.
  A manual user collapse while the condition is `true` is respected — the condition fires
  again only on the next false→true edge. Evaluated after every render via a no-deps
  `useEffect`, so any parent state change that the function reads automatically propagates.
  Mirrors the Android `expandWhen: (() -> Boolean)?` DSL parameter and `snapshotFlow`
  implementation for full cross-platform parity. Wrap the lambda in `useCallback([dep])` to
  avoid unnecessary re-registrations.
- **`initiallyExpanded` prop on `AzHostItemProps`.** Previously absent from the TypeScript
  interface; now documented alongside `expandWhen` for completeness.

### Typical use — `expandWhen` + tutorial framework

```tsx
<AzRailHostItem
  id="features"
  text="Features"
  expandWhen={useCallback(() => activeTutorialId === 'onboarding', [activeTutorialId])}
/>
```

A tutorial card that spotlights a sub-item of a collapsed host would silently degrade
(sub-item not laid out → not in `itemBoundsCache` → no punch-out). `expandWhen` ensures
the host is open whenever the tutorial needs it.

### Removed
- **The rail-coupled drop-down mode.** `dropdownMenu` / `dropdownSource` / `dropdownAlignment` /
  `dropdownOffset` settings and the `AzDropdownSource` enum are gone — use the standalone
  `AzDropdownMenu` instead.
- **`AzDropdownAlignment`** (and the `parseDropdownAnchor` helper). `AzDropdownMenu` now pins to a
  screen edge via `dockingSide` (`AzDockingSide` LEFT/RIGHT) and drops from the trigger automatically,
  matching the rail; the nine-anchor enum and per-call `offset`/`iconShape`/`menuWidth`/
  `backgroundColor` styling props are removed.

---

## 0.3.0

### Added
- **Pages (Z-ordering) on the host.** `<AzOnscreen>` and `<AzBackground>` accept a
  `page?: number`, and `<AzHostActivityLayout>` accepts `pagesEnabled?: boolean` (default
  `true`). Items sharing a page are co-planar (positioned via `alignment`); items on different
  pages stack in Z — a **higher** page draws **further back**. Decimals (`1.5`) insert a layer
  between existing ones. `<AzBackground>` forms its own book of pages beneath the `<AzOnscreen>`
  book; `weight` breaks ties within a background page. Mirrors the Android `AzHostActivityLayout`
  pages system for parity.

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
