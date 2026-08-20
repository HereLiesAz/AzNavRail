# AzNavRail — Capabilities & Limitations

A single reference for what the recent feature set can and can't do, on both the Android
(Jetpack Compose) and React (`aznavrail-react`, React Native / Web) libraries. Behaviour is kept at
parity between the two unless noted.

---

## Requirements (what's needed)

**Nothing new was added to the dependency surface for the recent features.**

**Android (`aznavrail`)**
- `minSdk 26`, Java 17, Jetpack Compose (the project's Compose BOM).
- `androidx.compose.animation:animation-core` — already a declared dependency; powers the kinetic
  typography (`CubicBezierEasing`, `Animatable`, `tween`, `spring`).
- `kotlinx-coroutines-core` (already on the classpath via Compose; already used across the library) —
  the host-expansion fix uses `merge` / `flow` / `distinctUntilChanged` from it. No new artifact.

**React (`aznavrail-react`)**
- Peer deps only: `react`, `react-native`, `react-native-web`. The kinetic typography uses RN's
  built-in `Animated` / `Easing`; host auto-expansion uses a built-in `setInterval`. No new packages.

---

## Kinetic typography (WP7-style)

Config-driven motion for menu words — preset enums, no free-composable escape hatch.

**Capabilities**
- **Entrance** (`AzEntrance`: `None | Fade | SlideUp | Turnstile`) and **exit** (`AzExit`:
  `None | Fade | Turnstile`), staggered per item.
- **Tilt-on-press** — 3D tilt toward the press point; the item's own click still fires.
- **`itemTextStyle`** — override merged over the label so menu words can be big/light/wide Metro type.
- **Screen title** — the big `AzNavHost` boundary title sweeps in each time the active screen changes.
- **Surfaces**: the expanded rail menu, the standalone `AzDropdownMenu`, and the screen title. The
  dropdown's app-icon trigger also gets an automatic margin.
- **Defaults are ON** (the library's signature look). Opt a surface out with `AzEntrance.None` /
  `AzExit.None` (rail: `azKinetics(...)`, dropdown: `azConfig(...)`, React: `settings` / props).
- **FAB / floating mode**: with no docked edge to hinge a turnstile on, the cascade degrades to a
  vertical up/down slide while keeping the stagger.
- **Exit**: items are held mounted through a "closing" state so they can animate out before teardown.

**Limitations**
- The turnstile hinges on the **docked edge**; on the screen title it hinges on the leading edge.
- Tilt-on-press is **auto-suppressed on draggable / relocatable rail items** so it can't fight the
  drag gesture.
- The collapsed icon strip is **not** animated (icons, not typography). Only labeled surfaces animate.
- The rail **exit overlaps the collapse-width animation** (items turnstile out as the rail narrows) —
  intended, but a busy combination; tune `entranceDurationMs` if it feels heavy.
- React tilt is **web-only** (pointer events); on native RN it's a no-op. `transformOrigin` is honored
  on web and ignored on native.

---

## Host auto-expansion (`expandWhen` / `initiallyExpanded`)

A host item can auto-expand its sub-items reactively.

**Capabilities**
- **`expandWhen: () -> Boolean`** — rising edge (`false → true`) expands, falling edge collapses.
- **`initiallyExpanded`** — expands once on first appearance.
- **Works regardless of how the condition is backed.** A Compose-state (`mutableStateOf`) / React-state
  condition reacts **instantly**; a non-reactive source (`StateFlow.value`, `LiveData.value`, a plain
  `var`, an external store) still works via a low-rate poll.
- A **manual collapse while the condition stays true is preserved** — the condition acts on
  transitions, never continuously.
- `expandWhen` and `initiallyExpanded` may coexist; the first observation only ever *expands*, so it
  never clobbers an `initiallyExpanded`/manual state.

**Limitations**
- Non-reactive conditions update only on the poll tick — **up to ~300 ms latency**. For instant
  response, read reactive state inside the lambda.
- Each `expandWhen` host keeps a small recurring timer alive while mounted (one boolean eval per host
  per ~300 ms). Negligible, but not zero. Hosts without `expandWhen` start no timer.
- `onExpandedChange` fires only for **manual** expand/collapse — never for `expandWhen` /
  `initiallyExpanded`-driven changes.

> Historical note: this previously appeared "broken" because the watcher was keyed on the whole item
> list, so any item-value change relaunched the watchers and swallowed the rising edge. It is now keyed
> on the stable host-id set. See `docs/DSL.md` → *Reactive Host Expansion*.

---

## About screen / docs reader / "More from Az"

**Capabilities**
- **Auto-discovers** the host app's markdown docs (repo root + `docs/`) from its GitHub repo, derived
  from the app namespace (`com.<owner>.<repo>` → `github.com/<owner>/<repo>`); `appRepositoryUrl`
  overrides it.
- **`.azignore` (and `.aiexclude`)** in the repo root excludes listed docs from the table of contents
  (gitignore-style patterns; `*` globs and trailing-slash dir prefixes).
- In-app, themed markdown reader; offline/rate-limited falls back to the last cached copy.
- **"More from Az"** is pinned at the bottom of the About screen (never scrolls away). Its cards are
  **not a selection model** — tapping a card opens that app directly (website/PWA → Play → GitHub).
- Each card shows **that app's own icon** (Play / website `og:image`); a blank or GitHub-avatar URL
  falls back to the app's initials — the owner's GitHub avatar is never shown as an app icon.
- While the About / More-from-Az reader is open, the **Help and Tutorial overlays are fully cleared**
  (not composed), and only one reader shows at a time (no bleed-through on a translucent surface).
- **About is a rail item**, appended to the end of the strip in every mode — including `noMenu` rails
  that have no drawer footer. It persists but is not fixed: `azAboutRailItem(...)` /
  `<AzAboutRailItem>` places and styles your own, `azAbout(aboutRailItem = false)` drops it.
- **Five ways out** of the reader: the About item again, any other rail or menu item, the app icon,
  drag-down, the 48dp close target, or system back.
- The reader is **inset by the rail's gutter**, folded or not, so the app icon behind it stays
  tappable — a reader can never become an app you cannot leave.
- The reader wears the **rail's** accent, not the app theme's, and always draws on an **opaque**
  ground (`translucentBackground` supplies the hue, never the alpha). The drop-down's dropped panel
  does the same, and additionally replaces a library-chosen accent that fails WCAG 3:1 against that
  panel with plain ink.

**Limitations**
- Discovery and fetching use the **public GitHub API** (unauthenticated) — subject to rate limits;
  the reader degrades to cached/offline content when limited.
- Requires the repo to be **derivable from the namespace** or supplied via `appRepositoryUrl`; the
  library never falls back to its own docs in a consuming app.
- `.azignore` is read from the repo's **contents listing** (so it resolves on any default branch); a
  repo unreachable over the API means no filtering that session.
- Per-app icons in "More from Az" come from the **CI-baked manifest**; until CI re-bakes, an app with
  no Play/website icon shows initials rather than a bespoke icon.

---

## Gestures the rail claims

**Capabilities**
- The rail installs pointer handlers **only when it can answer them**: the drag detector exists only
  while the rail is floating, draggable (`enableRailDragging`), or swipe-openable, and it consumes
  the pointer only on the branch that actually undocks the rail or moves the menu.
- Collapsed and docked, only the **buttons** take taps. The gaps between them, and the empty strip
  above and below, pass through to the app underneath — you can draw, scroll or drag there.
- Expanded or floating, the rail is a panel in its own right and does swallow stray taps.
- An outside tap collapses the drawer whether or not `dimBehindMenu` is on; the scrim is inset to
  exclude the rail, so taps on the rail still reach the rail.
- The nested-rail tap-to-dismiss listener exists **only while a nested rail is open**.

**Limitations**
- The rail is laid out over the whole window. Within the rail's own strip, its buttons win — an app
  gesture that must start exactly on a rail button will not reach the app.
- A drag the rail *does* act on (undock, swipe-open/close) is consumed; the app will not also see it.
- On Compose the rail deliberately has **no window-wide tap listener**. If it appears to eat a
  gesture your app wanted, that is a bug, not a design constraint.

---

## Platform parity

| Area | Android | React |
| :--- | :--- | :--- |
| Kinetic entrance/exit/tilt/title | ✅ | ✅ (tilt web-only) |
| `expandWhen` / host `initiallyExpanded` | ✅ instant + ~300 ms poll | ✅ render-eval + ~300 ms poll |
| About `.azignore` filtering | ✅ | ✅ |
| Pinned "More from Az", tap-to-open cards, real app icon | ✅ | ✅ |
| Clear Help/Tutorial while About open | ✅ | ✅ |
| About (`?`) rail item, auto-appended + overridable | ✅ | ✅ |
| Borderless shapes with a base footprint (`NONE_SQUARE`/`NONE_CIRCLE`) | ✅ | ✅ |
| Shared rail palette for every AzNavRail surface | ✅ `LocalAzRailPalette` | ✅ `AzRailPaletteContext` |
| Outside-tap collapses the drawer without `dimBehindMenu` | ✅ | ✅ |

---

## Feature parity (as of this release)

| Feature | Android (`aznavrail`) | CMP (`aznavrail-cmp`) | React (`aznavrail-react`) |
| :--- | :--- | :--- | :--- |
| Drop-down trigger set + title-row placement | ✅ | ✅ | ❌ not ported |
| Unattached hosts (`azUnattachedHostItem`) | ✅ | ✅ | ❌ not ported |
| Per-item badge / loading (`azItemState`) | ✅ | ✅ | ❌ not ported |
| Popups (`azPopup`) + warning triangle | ✅ | ✅ | ❌ not ported |
| Pinned "More from Az" rail item | ✅ | ✅ | ✅ |
| Three highlights (active / focus / secondary) | ✅ | ✅ | ✅ |
| Per-item highlight colours | ✅ `azHighlight` | ✅ `azHighlight` | ✅ item props |
| About de-duplication across surfaces | ✅ | ✅ | ✅ |
| About content warmed in the background | ✅ | ✅ | ✅ |
| Auto-sizing footer labels | ✅ | ✅ | ✅ |
| Floating windows (`AzWindow`) — movable + minimizable | ✅ | ✅ | ✅ |
| Hidden menu drawn in a window | ✅ | ✅ | ✅ |
| System overlay (`overlayService`) | ✅ | Android target only | ❌ n/a |
| Dissolve overlay on item tap | ✅ | ❌ not ported | ❌ |
| Unit tests | ✅ | ✅ (DSL-level only) | ✅ |

## Known gaps

- **The React port lags the Kotlin modules** on the features marked "not ported" above. The features
  added in this cycle — the three highlights, About de-duplication and warm-up, the auto-sizing
  footer, and `AzWindow` — landed on all three at once, but the older gaps remain.
  `aznavrail-react` is versioned separately (`package.json`) from the Gradle artifact, so the two do
  not move together.
- **The `@Az` annotations and KSP processor now exist** (`:aznavrail-annotations`,
  `:aznavrail-processor`) and generate a working `AzGraph`; see `docs/API.md` §1 and
  `SampleApp/.../AzGraphDemoActivity.kt`. They are Android-only — the generated graph calls
  `setContent` on a `ComponentActivity`, which has no Compose Multiplatform analogue. CMP consumers
  use the DSL directly.
- **CMP test coverage is DSL-level only.** The common test source set exercises the scope/DSL logic
  (unattached subtrees, per-item state). Compose rendering — placement, drag, the triangle glyph —
  is not covered by an automated test on any platform.
- **The physical-docking 180° rule is a product choice, not physics.** Docked LEFT and turned upside
  down, the rail stays on the LEFT even though the device's physical-left edge is then on the
  screen's right. `AzRailLayoutHelper` and its test both encode the documented rule.
- **Relocatable items under an unattached host don't support drag-to-reorder.** `azRailRelocItem`
  attaches to an `azUnattachedHostItem`'s `hostId` the same way it attaches to a rail host, and gets
  full tap-to-activate and long-press-to-open-hidden-menu support there (`aznavrail` and
  `aznavrail-cmp`; previously such an item was completely unclickable — tapping it did nothing at
  all). Drag-to-reorder is only wired for the rail strip, though: under an unattached host,
  `onRelocate` never fires, since that stack's linear layout has no reorder gesture of its own. The
  React port has no unattached-host feature at all yet (see the parity table above), so this does not
  apply there.

---

## Conveyance posture

The library is measured against the [Conveyance manifesto](https://github.com/HereLiesAz/Conveyance).
What that means in practice, and what changed to honour it:

| Principle | How the rail expresses it |
| :--- | :--- |
| Guide by example | Toggles/cyclers are label-is-state-is-control. An alerted item **morphs** into the warning triangle and back. Menus unfold; taps dissolve outward across the screen. |
| Resourceful minimalism | `AzLoad` is a morphing shape, not a ring plus the word "loading...". Per-item loading rather than a screen-blanking overlay. The rail is also the FAB, the drag handle and the app identity. |
| Eradicate explicit instruction | Auto-generated guidance ("Open the menu", "Tap Settings") is **off by default** (`azAdvanced(autoGuidanceEdges = true)` opts in). Affordance captions ("Tap to continue", "Tap to collapse") are gone. |
| Physics and motion | Haptics now answer every commit, not only FAB activation. New surfaces animate in rather than appearing. |

### Behaviour changes to be aware of

- **`AzLoad` no longer renders the text "loading..."** and no longer takes `showLabel`. It draws a
  filled shape morphing through rounded polygons. If you asserted on that string, assert on
  `AzLoadContentDescription` instead.
- **Guidance auto-edges are opt-in.** Apps that relied on the rail captioning its own affordances
  must set `azAdvanced(autoGuidanceEdges = true)`.
- **Haptics fire on item commits** when `vibrate` is on, where previously only FAB activation did.
- **The dissolve effect now works** (and works on every platform). It was rendering inside a `Popup`
  whose window is sized to its content, so the travelling label was clipped at the rail's edge and
  appeared to vanish. It is now drawn by the host at the window root.
