# Known parity gaps vs. the Android library

This file tracks places where the React port intentionally diverges from the Android
implementation because the underlying platform does not expose an equivalent primitive.

## System overlay window (`SYSTEM_ALERT_WINDOW`)

The Android library ships `AzNavRailWindowService`, `AzNavRailOverlayService`, and
`AzNavRailSimpleOverlayService` — services that use `WindowManager.addView` together with
the `SYSTEM_ALERT_WINDOW` runtime permission to draw the rail above every other app.

Neither React Native (iOS/Android JS) nor the web has an equivalent capability. iOS does
not allow apps to draw above other apps at all. Android could host such an overlay, but
it would require a native module — outside the scope of this JS-only port.

The library provides `<AzFloatingRail>` as an in-app stand-in. It draws the rail above the
rest of the React tree (transparent `Modal` on native, fixed-position container on web) and
forwards drag callbacks (`onDrag`, `onDragEnd`, `onDismiss`) that map onto the Android
`onRailDrag` / `onOverlayDrag` / `onUndock` callbacks. It cannot float above other apps.

## System navigation-bar inset (`AzBottomSheetInsetAware`)

The Android variant respects `WindowInsets.navigationBars` so the sheet sits exactly behind
the system gesture bar. React Native and the web have no first-class access to the
gesture-bar inset on every platform:

- **iOS / Android RN**: handled correctly via `SafeAreaView`.
- **Web**: the wrapper only applies `padding-bottom: env(safe-area-inset-bottom)`, which is
  honoured on iOS Safari but is a no-op on most desktop browsers. There is no underlying
  notion of a system gesture bar to honour.

If pixel-perfect parity with the Android `windowInsetsPadding(WindowInsets.navigationBars)`
result matters on web, callers should provide their own bottom padding via the `style` prop.

## Navigation-mode behaviors (`drawBehindNavBar`, gesture-nav margin)

The Android library detects the system navigation mode (3-button / 2-button / gesture) via
`Settings.Secure.navigation_mode` and uses it to (a) optionally draw the bottom sheet behind a
forced-transparent system navigation bar (`AzSheetConfig.drawBehindNavBar`) on button-nav
devices, and (b) automatically drop all bottom margin in gesture navigation.

The web has no system navigation bar and no equivalent "navigation mode", so neither behavior
has a meaningful web analog. The closest web primitive is `env(safe-area-inset-bottom)`, already
applied by `AzBottomSheetInsetAware`, which is `0` on devices without a home indicator. No
`drawBehindNavBar` flag is exposed on the React `AzSheetConfig`.

## Guidance overlay: outline + connector (non-blocking)

The status-driven guidance framework reaches behavioural parity across platforms — the same
`AzStatus` / `AzEdge` / `AzGoal` DSL, the same built-in `az.*` statuses and auto-edges, the same
auto-advancing routing, near-target collision-avoiding callout placement, swipe-to-cancel, the no-repeat
rule, and the same persistence keys (`az_navrail_completed_goals`, `az_navrail_dismissed_goals`).
**Neither platform dims the screen or blocks input.** One **minor visual** difference remains in how the
target is outlined: **Android** strokes the target's true geometry (`Circle` / `Rect` / `Path`) and
draws an **arrowhead** on the connector; **React/web** rings the target's **bounding box** and draws a
plain **connector line** (so a `Path` target is approximated by its AABB, and there is no arrowhead —
per-shape masking and arrowheads aren't portable on React Native). The resolved shape is still published
on the controller (`currentInstructions[i].resolvedShape`), so a host that wants a pixel-accurate
highlight can draw it itself via `<AzGuideRenderer>` or by observing the snapshot.

## Drop-down trigger title-row placement (`AzDropdownTriggerPlacement.TITLE`)

Android/CMP's `AzHostActivityLayout` draws a big screen-boundary title, and any `AzDropdownMenu`
declared inside its `onscreen { ... }` block can lift its trigger out of the call site to sit next
to that title (`AzDropdownTriggerPlacement.TITLE`, the effective default there). Several drop-downs
hosted this way line their triggers up beside each other in declaration order.

React has no equivalent host-activity layout with a title row to lift into. `AzDropdownMenu`
accepts `AzDropdownTriggerPlacement` for source parity, but `AUTO` and `TITLE` both currently
resolve to `INLINE` — the trigger always renders where the component is declared. Revisit this if/
when a React `AzHostActivityLayout` equivalent is built.

## Unattached hosts (`AzUnattachedHostItem`) and floating-host docking

Ported. `AzUnattachedHostItem` (`src/AzNavRailScope.tsx`) declares a rail host that lives outside
the rail strip and the drawer; `<AzUnattachedRail>` (`src/components/AzUnattachedRail.tsx`, wired
automatically inside `<AzNavRail>`, same as Android/CMP's own internal composable of the same name)
draws it wherever its `AzUnattachedAnchor` puts it. `OPPOSITE`/`BOTTOM` hosts stack into a fixed
column; `FLOATING` hosts drag via `PanResponder`, snap to a screen edge, dock flush against another
`FLOATING` host to form a shared-drag group (capped at two columns, each column capped at however
many hosts fit fully expanded — an over-capacity drop is refused, not evicted), and grow a grab bar
above a group's column-top host. A `FLOATING` host's own screen-edge dock/position/priority persist
per host id (`src/services/unattachedFloatingStore.ts`, mirroring Kotlin's `AzUnattachedStore`);
rail-to-rail attachments deliberately do not, matching Kotlin's own documented reasoning. The
drag/dock/attach geometry itself (`src/services/floatingDockMath.ts`) is a line-for-line port of the
Kotlin private functions in `AzUnattachedRail.kt` and is unit-tested directly — see
`floatingDockMath.test.ts`'s own doc comment for why (React Native's gesture responder system has no
synthetic-touch injection the way Compose's `performTouchInput` does, unlike the Kotlin test suite
this ports).

Two small, deliberate divergences, both because React's `AzNavRailContext` has no
`AzHostActivityLayout`-equivalent shared scope-wide flag for a composable to reach into (the same
reason the popup system's two divergences exist, below): an unattached item's "last tapped" focus
highlight, and an unattached nested rail's open/closed state, are tracked independently of the main
rail strip's own (Kotlin shares one scope-wide flag for each across every surface). Cosmetic only —
each surface renders a disjoint set of items, so there is nothing for the two trackers to disagree
about in practice.

## Popups (`AzPopup`, `AzPopupKind`) — two divergences from Android/CMP

`AzPopup`/`AzPopupController`/`AzPopupKind`/`AzItemAlert` are ported (`src/components/AzPopup.tsx`),
built on the existing `AzWindow`. Two things differ from Android/CMP, both because React has no
`AzHostActivityLayout`-equivalent shared scope object for a popup (a sibling of the rail, not a
descendant) to reach into:

- **No "last touched item" fallback.** `AzPopupController.show({...})` requires an explicit
  `itemId` to bind to an item; Kotlin's `itemId: null` falls back to the rail's last-tapped item.
  React's `AzNavRailScope` does not publish that id to siblings.
- **No read side on the item handle.** `AzPopupItemHandle` here is write-only (`setBadge`/
  `setLoading`/`setAlert`/`clear`) — there is no `item: AzNavItem?` to read the bound item's
  current declared state back out of, for the same sibling-scope reason above.

Additionally, the alert visual itself is an approximation: Android/CMP redraw the flagged item as a
rounded-corner **triangle** outline (`AzButtonShape.TRIANGLE`); React has no vector-path rendering
dependency to draw an arbitrary polygon stroke, so `AzItemAlert` instead overrides the item's
existing shape with the alert's accent colour. Revisit if a vector-drawing dependency (e.g.
`react-native-svg`) is ever added to the package.

## `AzActivity` / `AzGraphInterface`

The Android library exposes an `AzActivity` base class plus a KSP-generated
`AzGraphInterface` implementation for declarative graph wiring. React has no `Activity`
analogue — host integration is handled by composing `<AzNavRail>` directly in the React
tree. No replacement is planned.

## Secret-screen TCP sync (`secLoc` / `secLocPort`)

The Android library hosts a tiny TCP location-history sync server when `secLoc` is set,
listening on `secLocPort` (default 10203). The React port accepts the same flags on the
settings object for future native-module use, but no JavaScript-side server is started.

## DSL vs JSX

Compose DSL functions (`azBottomSheet { ... }`, `azNestedRail { ... }`, etc.) are
imperative. The React equivalents are declarative children — `<AzBottomSheet>` is a sibling
of the rail rather than a function call on the host scope. The behaviour is identical; only
the call site differs. See `MIGRATION_FROM_ANDROID.md` for the full mapping.
