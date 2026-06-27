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

## Platform parity

| Area | Android | React |
| :--- | :--- | :--- |
| Kinetic entrance/exit/tilt/title | ✅ | ✅ (tilt web-only) |
| `expandWhen` / host `initiallyExpanded` | ✅ instant + ~300 ms poll | ✅ render-eval + ~300 ms poll |
| About `.azignore` filtering | ✅ | ✅ |
| Pinned "More from Az", tap-to-open cards, real app icon | ✅ | ✅ |
| Clear Help/Tutorial while About open | ✅ | ✅ |
