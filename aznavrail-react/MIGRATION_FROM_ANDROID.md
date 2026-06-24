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

### Host item qualifiers — `initiallyExpanded` and `expandWhen`

Both qualifiers are supported identically on both platforms:

| Android DSL | React prop |
| --- | --- |
| `azRailHostItem(..., initiallyExpanded = true)` | `<AzRailHostItem ... initiallyExpanded />` |
| `azRailHostItem(..., expandWhen = { cond })` | `<AzRailHostItem ... expandWhen={() => cond} />` |

`expandWhen` accepts a `() => boolean` function. On Android the lambda is tracked via
`snapshotFlow`; on React the function is re-evaluated after every render (any parent state
change triggers a re-render, which re-evaluates the condition). Edge detection (false→true /
true→false) drives `hostStates` on both platforms. The "user wins" rule applies everywhere:
a manual collapse while the condition is `true` is respected; the condition acts again only
on the next false→true edge.

```tsx
// React example — expand the host while a tutorial is active
<AzRailHostItem
  id="features"
  text="Features"
  expandWhen={() => tutorialController.activeTutorialId === 'onboarding'}
/>
```

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

### Drop-down menu (`AzDropdownMenu`) & header icon size

| Android | React |
| --- | --- |
| `AzDropdownMenu { azItem("Settings") { … } }` | `<AzDropdownMenu><AzDropdownItem text="Settings" onClick={…} /></AzDropdownMenu>` |
| `AzDropdownMenu { azConfig(design = MENU, dockingSide = LEFT); … }` | `<AzDropdownMenu design={AzDropdownDesign.MENU} dockingSide={AzDockingSide.LEFT}>` |
| `azItem("Home", route = "home") { }` (navigates the `NavController`) | `<AzDropdownItem text="Home" route="home" … />` (calls `onNavigate`) |
| `azTheme(headerIconSize = 48.dp)` / `azSettings(headerIconSize = 48.dp)` | `headerIconSize={48}` |

Drop-down mode collapses the rail to a top-anchored app-icon trigger (the icon replaces the
hamburger) that unfolds either the rail items or the menu items like an accordion, giving the screen
content the full width. It excludes FAB/dragging, rail↔menu expansion, `noMenu`, swipe gestures,
physical docking, the footer, nested-rail popups, the bleeding app-name header, and the help overlay
— matching the Android behaviour. `headerIconSize` is a pixel `number` on React vs a `Dp` on Android.

### In-app About reader & "More from Az"

| Android | React |
| --- | --- |
| `azAbout(inAppAbout = true)` | `<AzNavRail inAppAbout />` / `settings.inAppAbout` |
| `azAbout(moreFromAzEnabled = true)` | `moreFromAzEnabled` |
| `azAbout(moreRailItem = true)` | `moreRailItem` |
| `azAbout(moreFromAzJsonUrl = "…")` | `moreFromAzJsonUrl` |
| `azConfig(appRepositoryUrl = "…")` — **optional** override (auto-derived from namespace) | `appRepositoryUrl` — **required** (no namespace to derive from) |

The footer "About" opens an in-app markdown reader that auto-discovers the repo's docs (root +
`docs/`) via the GitHub API. **Repo resolution differs by platform:** on **Android** the repo is
auto-derived from the app **namespace** (`com.<owner>.<repo>` → `https://github.com/<owner>/<repo>`),
so `appRepositoryUrl` is an optional override and never falls back to the AzNavRail library repo.
**Web has no package namespace, so `appRepositoryUrl` is required there** (no auto-derivation); when
it is unset the About entry is hidden. The standalone `AzDropdownMenu` has no onscreen area, so its
About is drawn as its own full-screen layer (Android); set `inAppAbout` false to open the repo in a
browser instead. While a footer screen (About or More from Az) is open, visible Help cards and any
in-progress tutorial are hidden and restore exactly where they were on close.

"More from Az" is a carousel whose `more-from-az.json` you fill by
pasting GitHub repo links (one per line); a GitHub Action resolves each repo and **bakes** the
finished manifest (name/icon/description, a verified Play link, the homepage website/PWA, WIP
filtering, Play-first sort). Both screens are themed from `activeColor`/`translucentBackground` and
built from the library's own components; markdown uses a small self-contained renderer (no
`react-markdown` dependency). Because resolution is done in CI, "More from Az" metadata is identical
on web and native — no CORS limitation.

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

Both callbacks are also accepted directly on `<AzHostActivityLayout>` and forwarded to the inner rail.

## Observing rail expansion (`onExpandedChange`)

To react to the rail's own expand/collapse transitions, pass `onExpandedChange` directly to `AzNavRail` or `AzHostActivityLayout`.

| Android | React |
| --- | --- |
| `AzNavRail(onExpandedChange = { expanded -> ... })` | `<AzHostActivityLayout onExpandedChange={(expanded) => ...}>` |

The callback receives `true` when the rail expands into its menu and `false` when it collapses to icon-only. It fires on initial composition/mount with the starting state, then once per transition.

## What's new in 0.3.0

- The standalone **`AzDropdownMenu`** (with `AzDropdownItem`) and a **sizable header icon**
  (`headerIconSize`) reach parity with Android. See "Drop-down menu (`AzDropdownMenu`) & header icon
  size" above.
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
- **Pages (Z-ordering)** reach full parity: the Android `onscreen(page)` / `background(page)` and
  `AzHostActivityLayout(pagesEnabled)` map directly to `<AzOnscreen page>` / `<AzBackground page>`
  and the `pagesEnabled` prop. Higher page numbers render further back; decimals insert layers;
  `<AzBackground>` is its own page-book beneath the `<AzOnscreen>` book.

## What's new in 0.2.0

- `AzBottomSheet`, `AzBottomSheetInsetAware`, `useAzSheetController`, `AzSheetDetent`,
  `AzSheetConfig` — full port of the LogKitty-style four-detent sheet from Android
  commit `da9e1be feat(aznavrail): port LogKitty bottom-sheet shell as first-class library feature`.
- `AzFloatingRail` — documented stand-in for the Android system-overlay services.
- `KNOWN_GAPS.md` documenting the platform-shaped gaps.
- `onInteraction` callback now passes `AzNavItem` as 3rd argument for item-level analytics.
- Bottom sheet swipe-down snaps directly to HIDDEN for quick dismissal.
- Bottom sheet adds transparent tap overlay at PEEK for improved discoverability.
