# Changelog

## Unreleased

Windows-Phone-7 fidelity pass: the signature kinetic-typography entrance is redesigned end-to-end,
the About page becomes a docs-TOC + focused-hero-carousel split, and three long-missing menu-drawer
knobs (dim, side-alignment, kerning-justify) land as first-class options.

### Added
- **Menu-drawer look-and-feel options** on `AzNavRailSettings` (and mirrored on `AzDropdownMenu`):
  - **`dimBehindMenu`** (default `false`) plus **`dimBehindMenuAlpha`** (default `0.4`) — draws a
    dim scrim behind the expanded drawer; tap-to-collapse preserved.
  - **`menuItemAlignment`** (`'center' | 'side'`, default **`'side'`**) — labels hug the docked
    edge (`Start` when docked LEFT, `End` when RIGHT) instead of the legacy center-align.
  - **`justifyMenuItems`** (default **`true`**) — measures the natural width of each label and
    applies computed `letterSpacing` so the label fills the row edge-to-edge (Word-style justify).
- **`AzMoreFromApp.bannerUrl`.** When the app's repo has `docs/banner.png` / `.webp` / `.jpg`
  (or `docs/hero.*`), it is displayed at the top of that app's info panel under the About-page
  hero carousel. Resolved both at CI-bake time and at runtime.

### Changed
- **Kinetic typography redesigned.** Item entrance/exit is now a pure 90° `rotateY` sweep hinged on
  the docked edge — no fade, no vertical slide. New defaults:
  `entranceStartAngle = 90` (was 70), `entranceDurationMs = 720` (was 360),
  `entranceStaggerMs = 60` (was 55). Items now overlap heavily — the next item starts ~60 ms
  after the previous begins while the previous is still animating.
- **On native React Native**, `AzKineticItem` now applies a `translateX ±(width/2)` pivot correction
  around the `rotateY` so the hinge visibly sits on the docked side (was center-pivoted because
  React Native ignores `transformOrigin`). No change on web (CSS `transform-origin` was already
  correct).
- **Footer unfolds like an accordion.** The rail and dropdown footer (About/Feedback/@HereLiesAz)
  now animate in with `scaleY 0→1` + `opacity 0→1` hinged at the top edge, starting when the
  **last** menu item starts its own kinetic entrance (delay = `(count - 1) * staggerMs`). Same
  Wp7Decelerate easing; the whole footer unfolds as one unit.
- **About page split into two halves.** The top half is the existing docs TOC (unchanged internals),
  and the bottom half is a **focused-hero More-from-Az carousel** with a size pattern
  `small · medium · LARGE · medium · small` — the LARGE (center) item is the active one, and its
  banner (when present), name, description, and link buttons render below the carousel. The old
  pinned "More from Az" and "View on GitHub" buttons at the bottom of About are removed.
- **`fetchMoreFromAz` now enriches missing icons at runtime.** When the manifest's `iconUrl` is
  blank (or points at the owner's GH avatar), the runtime walks standard Android launcher-icon
  paths on `raw.githubusercontent.com` (`mipmap-xxxhdpi/ic_launcher.webp` / `.png`, then xxhdpi,
  xhdpi, hdpi) and falls back to the repo's OpenGraph social preview.

### CI
- **`.github/scripts/bake_more_from_az.py` walks the app's launcher icons and banner.** When
  Play/website `og:image` resolvers leave `iconUrl` blank, the bake now looks for
  `app/src/main/res/mipmap-*hdpi/ic_launcher.{png,webp}` (with a Contents-API tree walk fallback and
  adaptive-icon XML parse) and fills the manifest. It also probes each repo for `docs/banner.*` /
  `docs/hero.*` and stores it as `bannerUrl`.

## 0.6.0

Guidance is now a **non-blocking coach** instead of a modal tutorial.

### Changed
- **No dimming, no blocking.** The overlay no longer draws a full-screen dim. It outlines each step's
  target and places a small callout *near* (never on) it with a connector line; nothing outside a
  callout consumes input, so the app stays fully interactive while guidance is up.
- **Smart placement.** Callouts are positioned near their own target and kept off the target, other
  known UI (rail items / registered targets), each other, and the screen edges — they no longer stack at
  the bottom or clip. New exports: `placeCallout`, `overlapArea`.
- **Developer-driven start.** `autoStartWhen` still works but is discouraged; it now also honours the
  user's skip. Prefer `controller.activate(...)`.

### Added
- **Swipe-to-cancel.** Swiping a callout away cancels tutorial mode. New controller surface:
  `skip(goalId?)`, `dismissedGoals`, `isDismissed(id)`, `resetGuidance(goalId?)`. Skips persist to
  `localStorage` / `AsyncStorage` under `az_navrail_dismissed_goals`; a skipped or completed goal is not
  shown again until `resetGuidance(...)`.
- **No repeats.** A step that has been shown and acted on is consumed for the session and never re-shown,
  even if the user undoes the action and the router would otherwise re-route to it.

### Notes
- Parity: React rings the target's bounding box and draws a plain connector (Android strokes the true
  shape + an arrowhead) — see `KNOWN_GAPS.md`.

## 0.5.0

### Added
- **Arbitrary moving highlight targets.** Register a window-space shape with **`<AzGuidanceTarget id
  shape={() => AzGuideShape | null} />`** and point an edge/step at it via `highlightTargetId`. The shape
  (`{ type: 'Circle' | 'Rect' | 'Path', … }`, recomputed each frame) lets the spotlight track an
  on-screen object drawn over a canvas; returning `null` degrades to text-only. New exports:
  `AzGuidanceTarget`, `AzGuideShape`, `AzPathCmd`, `shapeBounds`, `resolveShape`.
- **Paged steps & manual advance.** `<AzEdge steps={[…]} />` reveals one `AzInstructionStep` at a time:
  an informational step (no `advanceWhen`) advances on tap; a step with `advanceWhen="<statusId>"`
  auto-advances when that status flips (reactive wins). One goal can mix “read this” and “now do this”
  steps. The controller gains `advance(stepKey)` / `next(stepKey)` / `back(stepKey)` and no-arg
  `advance()`.
- **Dynamic rail-item highlights.** The `AZ_ITEM_ACTIVE` (`'az.item.active'`) token and a
  `highlightSelector={() => string | null}` prop resolve the highlight to the active or a runtime rail
  item each frame.
- **Observable current instruction.** The controller exposes `currentInstructions: AzGuidanceSnapshot[]`
  and `current`, so a host can mirror what's showing (text/title/target/step) with bespoke rendering.
- **Gesture-time suppression.** **`<AzSuppressGuide predicate settleMs />`** hides guidance while the
  predicate is true and re-shows after a settle delay once it clears.
- **Custom callout rendering.** **`<AzGuideRenderer render={(snapshot, bounds) => …} />`** replaces the
  built-in callout body (the dim/ring still draw).

### Fixed
- The guidance docs incorrectly showed `autoStartWhen` as a function; it is a **status id** string.

### Notes
- All additions are backward-compatible: existing `<AzStatus>` / `<AzEdge>` / `<AzGoal>` usage is
  unchanged. **Parity:** a `Path` target is ringed by its bounding box (RN can't multi-hole / path-mask).

## Unreleased

### Removed
- **The scripted scene/card tutorial framework.** `AzTutorial`, `AzTutorialProvider` /
  `AzWebTutorialProvider`, `useAzTutorialController` / `useAzWebTutorialController`, the scene/card
  model, the four advance conditions, variable/scene branching, checklist/media cards, and the
  `AzTutorialOverlay` are all gone, along with the help-overlay "Start Tutorial" launch affordance.

### Added
- **The status-driven guidance framework** replaces it. Describe the userflow as a flowchart of
  **statuses** (string-id nodes) joined by **edges** (transitions carrying an instruction), declare
  **goals** (target statuses), and activate them on the controller. The engine shows the instruction
  to reach the next status toward each active goal, **auto-advances the instant a target status
  becomes true** (no Next button), re-routes live, and shows every active goal's callout next to its
  control. New exports: **`AzStatus`**, **`AzEdge`**, **`AzGoal`**, **`AzGuidanceProvider`**,
  **`useAzGuidanceController`**, **`AzInstructionOverlay`**, **`useActiveStatuses`**,
  **`computeBuiltinStatuses`**, **`nextHop`**, **`routeInstructions`**, and **`computeAutoEdges`**.
  The controller exposes `enabled`, `activeGoals`, `completedGoals`, `enable()`, `disable()`,
  `activate(id)`, `deactivate(id)`, `markReached(id)`, `isCompleted(id)`. Built-in `az.*` statuses and
  auto-edges for rail affordances are published automatically; you hand-author `<AzEdge>` only into
  your own custom statuses. Completed goals persist to `localStorage` (and `AsyncStorage` on RN) under
  key `az_navrail_completed_goals` (replacing `az_navrail_read_tutorials`). **Parity note:** the
  React/web overlay draws an **accent ring** around each target over a light dim, rather than a true
  punch-out spotlight; routing and advancement are identical to Android.

### Changed
- **About reader docs clarified for the repo-resolution split.** Android auto-derives the repo from
  the app namespace (`com.<owner>.<repo>` → `github.com/<owner>/<repo>`), so `appRepositoryUrl` is an
  optional override there and never falls back to the AzNavRail library repo. On **web** there is no
  package namespace, so `appRepositoryUrl` remains **required** (no auto-derivation); when it is unset
  the About entry is hidden. Also documented: the standalone `AzDropdownMenu`'s full-screen in-app
  About reader, and that visible Help cards and any guidance callouts hide while a footer screen
  (About / More from Az) is open and restore exactly where they were on close. Behavior on web is
  unchanged — docs/migration notes only.

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

### Typical use — `expandWhen` + the guidance framework

```tsx
<AzRailHostItem
  id="features"
  text="Features"
  expandWhen={useCallback(() => guidance.activeGoals.includes('onboarding'), [guidance.activeGoals])}
/>
```

A guidance edge whose callout anchors to (`highlightItemId`) a sub-item of a collapsed host would
silently degrade (sub-item not laid out → bounds unknown → no callout anchor). `expandWhen` ensures
the host is open whenever guidance needs to point at the item.

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
