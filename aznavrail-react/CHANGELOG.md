# Changelog

## Unreleased — three highlights, one About, windows that move

### Added
- **Three highlights, not one.** An item can be lit three ways, and each answers a different
  question: **active** (`activeColor`) is *where you are* — the current destination or an
  `activeClassifiers` match; **focus** (`focusColor`) is *what you are touching* — the item just
  tapped when it carries no route of its own; and **secondary** (`secondaryColor`) is *whatever the
  app decides*, lit only by the app via an item's `secondary` prop or `secondaryClassifiers`. Focus
  beats active beats secondary. Any item can override any of the three with its own `activeColor` /
  `focusColor` / `secondaryColor`. `focusColor` left unset reuses `activeColor`, so nothing changes
  for an app that doesn't ask for it.
- **`<AzWindow>`** — the library's floating window, and the surface every panel it puts in front of
  the user is now drawn in. It **moves** (drag its bar; clamped so it can never be lost off-screen)
  and **folds** to that bar, keeping its position. The **hidden menu** is one, which matters most
  for the panel that can hold a text field.
- **`dedupeAbout`** (default true) draws the About affordance in exactly one surface even when
  several could offer it. A declared `<AzAboutRailItem>` outranks the rail's menu footer, which
  outranks a drop-down's footer, which outranks the automatic `?`.

### Changed
- **The About reader is loaded before it is opened.** Its docs listing, first document and
  More-from-Az manifest are warmed in the background as the rail mounts, so the page opens populated
  instead of spinning. A reader opened mid-flight fills in the moment the fetch lands.
- **The About page ends with the author** — `@HereLiesAz`, Feedback and `hereliesaz.com`.
- **The More-from-Az carousel sticks.** `disableIntervalMomentum` plus a settle pass means a flick
  hands focus to the next app or two and lands ON a card rather than coasting between them.
- **Footer labels auto-size.** Every footer row shrinks to one line rather than wrapping
  `@HereLiesAz` onto a second and making the footer taller than the strip it is pinned to.
- **The automatic `?` rail item now stands down** when a menu footer is already carrying About. A
  `noMenu` rail (no footer) still gets it; `dedupeAbout={false}` restores the old behaviour.

## Unreleased — borderless shapes, About on the rail, and one palette for every surface

### Added
- **Borderless shapes now have a base shape.** `AzButtonShape.NONE` kept the wide `RECTANGLE`
  footprint and there was no way to ask for anything else, so a borderless item never lined up with
  the bordered ones beside it. `NONE_SQUARE` and `NONE_CIRCLE` join it: each keeps the footprint of
  the base shape it names, so dropping the border no longer reflows the rail.
- **`<AzAboutRailItem>`** declares the rail's About (`?`) item explicitly, wherever you render it,
  replacing the automatic one. It takes the same per-item props as any other item, `info` included.
- **`useAzAccent()` / `useAzRailSurface()` / `AzRailPaletteContext`** expose the rail's own colours to
  your components. A rail also *publishes* its palette outside the tree, so a second floating rail —
  a sibling, somewhere React context cannot reach — inherits from the rail on screen rather than
  falling back to the library default.

### Changed
- **Full parity across both React builds.** Everything below now applies to the React Native build
  (`src/`) *and* the web build (`src/web/`), which previously had none of it: the About (`?`) rail
  item, the shared rail palette, the borderless shape family, and the reader's escape hatches. The
  web reader also adopted the dark ground the other ports already had, and is inset by the rail's
  gutter so the app icon stays reachable behind it.
- **The drawer collapses on an outside tap whether or not `dimBehindMenu` is on.** The scrim used to
  exist only when the developer opted into dimming, which meant the documented "tap outside to
  collapse" behaviour silently did not exist for anyone who left dimming off. It is now always
  present while the drawer is open; `dimBehindMenu` decides only whether that area is darkened.
- **About lives on the rail.** The rail now ends with an About (`?`) rail item in every mode, not
  only in the footer of a drawer the rail may not even have. It persists but it is not fixed:
  declare your own `<AzAboutRailItem>` to move or restyle it, or pass `aboutRailItem: false` to drop
  it. Tapping it toggles the reader.
- **The About reader is easy to leave.** Tapping any other rail item, any menu item, or the app icon
  closes it — only the About item itself toggles.
- **Every AzNavRail surface takes its colour from the rail, not the app's theme.** `AzNavRail`
  publishes `AzRailPaletteContext`; a floating rail, a drop-down menu, the About reader and the
  More-from-Az carousel resolve their accent through `useAzAccent()`. The accent is `activeColor`
  when set, otherwise the colour most of the rail's own items are drawn in (`resolveRailAccent`) —
  a rail whose every button is white is a white rail, whatever the theme's primary says.

## Earlier unreleased — motion, the rail slider, the help item, and the About reader

### Changed
- **One motion scale (`AzMotion`), and everything got faster.** Every transition in the library now
  reads its timing from one object. The values it replaced — a 60 ms stagger and a 720 ms item
  duration, copy-pasted as literals into six places — meant an eight-item drawer took **1.2 seconds**
  to finish arriving, and because the turnstile entrance starts each item edge-on (and therefore
  invisible) the panel sat there empty for the first stretch of it. Items are now 280 ms with a 22 ms
  stagger, containers 200 ms, layout settles 240 ms. `AzMotionTest` holds a budget so they cannot
  drift back: a twelve-item cascade must settle inside 650 ms, and a panel must never outlast its own
  contents.
- **The About reader is dark in every theme**, with light ink. It is a full-screen surface the user
  stepped aside into for long-form reading; taking `MaterialTheme.colorScheme.surface` meant a
  screenful of white in a light-themed host. The host's accent still carries headings, links, and the
  close affordance, and a host-supplied `translucentBackground` still wins.
- **The About reader has three ways out** instead of one 24 dp icon: drag down anywhere (with a grab
  handle announcing it), a 48 dp close target, and system back. The in-reader back arrow got the same
  48 dp target.
- **The help `?` uses the ordinary rail-item colour.** It drew in `scope.activeColor` — the
  *selected* accent — so it stood out from the rail it belongs to, and rendered as `Unspecified`
  whenever the host had not set an active colour at all.

### Added
- **`AzSlider`** — the rail's slider, drawn on Material 3 Expressive lines: a thick track with fully
  rounded ends, an inset gap where the thumb sits, a pill thumb standing across the track, and stop
  indicators wherever the value is quantised. One composable covers all four variants — `CONTINUOUS`,
  `STEPPED`, `CENTERED`, `RANGE` — across five sizes and both orientations, because they differ only
  in where the active track begins and how many thumbs ride it.
- **`azRailSlider(...)`** / **`<AzRailSlider>`** — a rail item that unfolds into that slider **in
  its own slot**. Folded it is an ordinary rail button; tapped, the slot grows along the rail and
  the button becomes the track, with the value underneath as the way back. Nothing opens over the
  rail, so the control arrives where the user was already looking.

### React port
All of the above lands here too: `AzMotion` in `AzNavRailDefaults`, `AzSlider`, `<AzRailSlider>`, and
the dark About reader with drag-to-dismiss, a grab handle and 48dp header targets. Markdown prose now
takes an explicit `ink` colour — it carried none of its own and would have inherited React Native's
near-black, invisible on the new ground. The help `?` recolour has no React counterpart: the port has
no synthesised `noMenu` footer glyph to recolour.


## Unreleased — toolchain: React Native 0.86 / React 19

### Changed
- **React Native 0.72.4 → 0.86.0, React 18.2 → 19.2.** With it: `@react-native/babel-preset` and
  `@react-native/jest-preset` (replacing the retired `metro-react-native-babel-preset`),
  `@react-native/typescript-config`, TypeScript 5.9, Jest 30, ESLint 9 on a flat
  `eslint.config.js`, and `react-native-builder-bob` 0.43. `@types/react-native` and
  `react-test-renderer` are gone — RN ships its own types, and React 19 deprecated the test
  renderer. `npm ci` now resolves without `--legacy-peer-deps`.
- **`@testing-library/react-native` 11.5 → 14.** Its rendering API is asynchronous in v14
  (`render`, `renderHook`, `fireEvent`, `rerender`, `unmount`), and the whole suite was migrated.
- **Tests run against the real React Native tree.** A hand-rolled `jest.setup.js` that mocked the
  entire `react-native` module has been deleted in favour of the official preset. It was hiding
  real defects (see Fixed). `jest.config.js` declares two projects, `native` and `web`; the web
  components in `src/web` are now covered by Jest + jsdom + `react-dom` rather than by two
  `vitest`-authored suites that no runner had ever executed.
- **`StyleSheet.absoluteFillObject` → `StyleSheet.absoluteFill`**, which RN 0.86 collapsed into one.

### Fixed
- **Stray `" "` text node inside a `<View>`** in `AzNavRail`. Two `{flag && <View />} {/* … */}`
  lines existed only to silence unused-variable warnings, and the space before the comment rendered
  a bare text child — an invariant violation that crashes a real React Native app.
- **The dropdown menu could fail to open at all.** `openMenu` called `setOpen(true)` *inside* the
  `measureInWindow` callback, so if the trigger had not been laid out yet — or the host did not
  implement `measureInWindow` — the menu never appeared. It now opens immediately and lets the
  measurement refine the anchor when it lands.
- **`MenuItem` (web) called `useRef` after an early return**, so a row switching between divider and
  item changed the component's hook count between renders.
- **Menu labels were announced twice.** Each `MENU`-design row renders an invisible copy of its
  label to measure natural width; that copy is now hidden from assistive tech.
- **`humanize('MIGRATION_GUIDE.md')` returned `MIGRATION GUIDE`.** It title-cases multi-word names
  now, while leaving a single all-caps name (`README`, `API`, `DSL`) as written.
- **`key` was being passed through a props spread**, which React 19 rejects.
- **`bob build` was broken** by a `brace-expansion: ^5.0.8` security override: `minimatch` 3.x calls
  it as a function and v5 is a named export. Because the Pages workflow ran `bob build || true`,
  this failed silently and left a stale committed `lib/` in place, which in turn failed the
  `sample-pwa` build with unresolved imports. `lib/` is no longer tracked by git, the workflow no
  longer swallows build failures, and the override is gone (see `docs/SECURITY.md`).

### Added
- **Rail items carry their declared `id` as a `testID`**, so any item can be addressed by the id it
  was given. `AzLoad` and the button fill-content graphic have stable test ids too.

## Unreleased — cosmetic (Compose only: dissolve overlay on close)

### Added
- **Compose drawer + dropdown**: tapping a menu item / dropdown entry whose click closes the panel
  now spawns a `DissolveOverlay` — a full-screen `Popup` that renders the item's label at its
  captured window-space bounds, slides toward the middle of the screen, and fades to zero — while
  the OTHER items run their normal bottom-up exit turnstile. The tapped item is skipped from the
  exit render so its label doesn't animate in two places at once. Overlay duration + easing match
  the drawer's own kinetic config, so the effect reads as one instrument in the same phrase as
  the rest of the exit.

## Unreleased — polish (footer animation, divider color, @HereLiesAz alpha)

### Fixed
- **Footer accordion now actually plays on open.** The initial `Animatable`/`Animated.Value` seeded
  itself from `visible ? 1 : 0`, so first-mount always started at "already open" and no animation
  played. Always start collapsed and let the `LaunchedEffect`/`useEffect` run the fold-in.
- **Footer fold-UP on close.** The `visible=false` branch used to call `snapTo(0)` / `setValue(0)`,
  hiding the footer instantly. Animate to 0 with the same duration/easing so the footer visibly
  collapses first. The rail keeps it composed via `rememberAzClosingState(count=1)` (Compose) /
  same lifetime as the item cascade (RN), and the plain web tracks a `footerClosing` flag with a
  matching `--closing` CSS keyframe. The footer is now visibly the FIRST thing to go on close.
- **Item exit cascade shifted one stagger tick.** Previous `(count - 1 - index) * staggerMs` had
  the last item exit at t=0 simultaneously with the footer. New `(count - index) * staggerMs`
  puts the last item at t=staggerMs, so the footer folds first and the items follow bottom-up —
  a symmetric mirror of the open sequence (item[0] at t=0, footer at t=count·staggerMs).
  `rememberAzClosingState` / `useAzClosing` bumped by the same tick so nothing tears down early.
- **`@HereLiesAz` footer link no longer 50%-alpha.** Renders in the same accent color as
  About / Feedback so the whole footer looks like one visual family. Applied across all four
  surfaces (Compose rail + dropdown, RN rail + dropdown, plain web rail + dropdown).
- **In-drawer `azDivider()` items now render as real dividers.** `MenuItemNode` used to fall
  through to the empty-text `MenuItem` for `isDivider = true` items, producing a blank clickable
  row. Short-circuit and render `AzDivider(color = accent)` instead — same accent as every
  other divider (footer, About-page split, dropdown-menu footer top).

## Unreleased — bug fix (shrink oversized drawer labels; explicit-only line breaks)

### Fixed
- **Menu-drawer labels no longer auto-wrap mid-word.** The previous hybrid-justify solver bailed
  out (returning `{scale: 1, letterSpacing: 0}`) whenever the label's natural width already met
  or exceeded the row width, and the `Text`/`<Text>`/`<span>` then wrapped — producing ugly
  "Generat / e", "Projec / t", "Setting / s" overhangs on narrow rails. The solver now has a
  **shrink** branch: `scale = rowWidth / naturalWidth` (clamped `≥ 0.5×`) so oversized labels scale
  down to fit on one line.
- **Explicit `\n` line breaks now survive on React Native and web.** Drawer labels are split on
  `\n` up-front and each line is rendered by its own inner component (`JustifiedRNLine`,
  `JustifiedDropdownLine`, `JustifiedWebLine`, `JustifiedWebDropdownLine`) with its own
  natural-width measurement and its own solver call. Otherwise the newline character would inflate
  `charCount` and fold both lines' widths into one `naturalWidth`, skewing the per-line justify
  math — matches Compose's `internal/MenuItem.kt` `lines.forEach { line -> Text(line, …) }`.
  Combined with `softWrap = false` + `maxLines = 1` on Compose, `numberOfLines={1}` on the RN
  per-line `<Text>`, and `white-space: nowrap` + `overflow: hidden` on the web per-line `<span>`,
  **line breaks in drawer labels are now explicit-only**.

## Unreleased — follow-up (hybrid justify + AzDivider color)

### Changed
- **`justifyMenuItems` now uses a hybrid kerning + font-scale solver** (`src/util/AzJustify.ts` on
  React; `internal/AzJustify.kt` on Compose). Kerning fills the row up to `α · fontSize` of
  tracking (`α = 0.15`); when saturated, the font scales up so both letter-spacing and font-size
  converge on the mix that lands the label exactly on the row width. Font growth capped at `1.5×`.
  Fixes the previous pure-letter-spacing pass over-spreading short labels.
- **`AzDivider` inherits the font color.** New default is `LocalContentColor.current` on Compose,
  `currentColor` on the web (React and plain-JSX builds). The rail's expanded-menu divider, the
  About-page split divider, and the standalone `AzDropdownMenu`'s footer divider all pass their
  accent color explicitly so the divider belongs to the same visual family as the labels next to
  it — never a muted outline.

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
  **last** menu item starts its own kinetic entrance — one extra stagger tick past it (delay =
  `count * staggerMs`), so the footer is the natural next beat in the cascade rhythm. Same
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
