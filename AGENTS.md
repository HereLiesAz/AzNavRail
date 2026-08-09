This document serves as a detailed specification and behavior guide for the AzNavRail component. All changes must be backward-compatible.

**AGENT DIRECTIVE: YOU ARE EXPRESSLY FORBIDDEN from executing `setup_android.sh` or any other environment setup or initialization scripts. The environment is already correctly provisioned and snapshotted. Assume all dependencies, JDKs, and SDKs are installed and functioning.**

- The rail buttons must be a transparent shape with a colored stroke.

- Make sure the app icon is displayed at the top by default. It's the app icon up there by default,
  or it's the
  app name.

- And I don't give a shit how "unstable" you think it is, RESIZE THE TEXT IN THE BUTTONS. The item
  text MUST MUST MUST fit inside the shape, no wrapping allowed without the developer explicitly
  deciding to do so with a newline character.

- **Content Filling**: If a Rail Item's content is a `Color`, an Image Resource ID (`Int`), a Compose `ImageVector` or `Painter` (vector graphics), or an Image URL/Model (`Any`), it MUST fill the button shape completely (Fill/Crop) with 0 padding, clipping as needed. `ImageVector` content is tinted with the item color. Text and Numbers retain default padding.

- To be clear, if the developer has a multi-word string for a rail item, they DO need the ability to
  put the words on separate lines.

- For a toggle option, there should NOT be a toggle component. There should be just the menu item's
  text,which switches to the other text when it changes states. So, for example, The menu option
  would be Power On when the power is on. Tap it, and it then displays Power Off and also changes
  the state, accordingly.

- The cycler must work similarly. It displays Option A and that's what's enabled. Tap it and you see
  Option B take its place. Tap it again and you see Option C. Leave it at Option C for 1 second, and
  Option C enables.
-
- So, to be clear, for a toggle, you need to collect TWO strings. For a cycler, you need to collect
  at least THREE strings. The developer should be able to use whatever text they want for each state
  of both items.

- When any item in the menu is tapped, this should execute whatever action it is for AND collapse
  the rail.

Support hierarchical navigation with host and sub-items. This allows you to create nested menus that
are easy to navigate.

- **Host Items**: These are top-level items that can contain sub-items. They can be placed in the
  rail or the menu. They expand **inline** when clicked.

- **Sub-Items**: These are nested items, toggles, and cyclers that are only visible when their host
  item is expanded. They can also be placed in the rail or the menu. A rail Sub item must be the
  child of a rail host, but menu sub items may be the child of a rail host or menu host.

- **Sub-Hosts (`azRailSubHostItem` / `azMenuSubHostItem`)**: A sub-item that is itself a host.
  Hosts nest to **any depth**: opening a sub-host reveals its children inline while sibling
  sub-items stay visible (accordion behavior at every level). Children attach to their host by
  `hostId` reference, not by position.

- **Reactive Expansion (`expandWhen`)**: All host-item builders accept
  `expandWhen: (() -> Boolean)?` (Android) / `expandWhen?: () => boolean` (React). When the
  condition transitions false→true the host auto-expands; true→false auto-collapses. The
  "user wins" rule applies: a manual collapse while the condition is `true` is honoured — the
  condition acts again only on the next false→true edge. Lambdas are stored in
  `expandWhenMap` (not on `AzNavItem`) and tracked via `snapshotFlow` on Android and via a
  no-deps `useEffect` on React. Use `initiallyExpanded` for a one-shot expand on first
  appearance; use `expandWhen` for ongoing reactive control. `azNestedRail` also accepts
  `expandWhen`, reusing this exact mechanism and the same `expandWhenMap`/edge-detection effect:
  the only difference is that a nested-rail id writes `nestedRailOpenId` (rising edge sets it to
  the item's own id, falling edge clears it if it is still the open one) instead of `hostStates`,
  so a nested rail's popup can be driven open/closed programmatically with the identical
  rising/falling-edge, first-observation, and "user wins" semantics documented above. On React
  (`aznavrail-react`), `expandWhen` lives directly on the registered item (`AzNavItem.expandWhen`
  in `types.ts`, spread through by every DSL builder including `AzNestedRail`) rather than a side
  map, and is evaluated in `AzNavRail.tsx`'s/`AzNavRail.jsx`'s existing edge-detection effect
  (post-render + a 300ms poll fallback for non-React-state conditions); a nested-rail item's
  `expandWhen` there drives RN's single `nestedRailVisible` state / web's `nestedRailVisibleId`
  state the same way a host's drives `hostStates`, sharing the same edge-tracking ref so a manual
  popup dismissal is not fought until the next false→true transition.

- **Nested Rails (`azNestedRail`)**: This is a distinct feature from Host Items. A Nested Rail opens a separate **popup overlay** adjacent to the parent item instead of expanding inline. It supports `VERTICAL` (column) and `HORIZONTAL` (row) alignment. Optional `reflectSelectionInParent` (default `false`) makes the parent button itself stand in for whichever (non-host) child was last selected: the parent's text/content mirror that child's (its own color/shape/fillColor are untouched), a plain tap fires the selected child's action directly instead of opening the popup, and a long-press opens the popup instead (the plain-tap behavior when `reflectSelectionInParent` is `false`). The selection is tracked by `AzNavItem.selectedChildId` / a `hostId → selectedChildId` scope map that survives DSL re-declaration, seeded once from a developer-supplied initial value the same way `initiallyExpanded` seeds a host. `azNestedRail` also accepts `expandWhen` for programmatic open/close of the popup itself — see "Reactive Expansion" above. On React (`aznavrail-react`, both the RN and web builds), tapping a
  (non-host) nested-rail child now also closes the popup afterward — unless the host's
  `keepNestedRailOpen` is set — closing a gap where only a backdrop tap ever dismissed it; the
  `selectedChildId` map and the close-on-select logic live in the rail's own state
  (`AzNavRail.tsx`'s `selectedChildByHost` / the equivalent in `AzNavRail.jsx`), not on the DSL
  item itself, so they survive re-registration the same way `hostStates` does.

- **Orientation Handling**: The rail supports two modes:
    1.  **Default**: Anchored to the screen/view side (e.g., Left side of the window).
    2.  **Physical Docking (Experimental)**: Anchored to the physical side of the device, adapting to rotation (e.g., Left in Portrait -> Right in Reverse Portrait).

Long press the app icon/name to activate fab mode for dragging around the screen.

Haptic feedback should notify the user when FAB mode is activated and deactivated. If the developer
has activated the App Name instead of app icon at the top, that text should NOT be resized. It
should NOT be constrained to a shape, nor to the width of the AzNavRail. That text should NOT be
wrapped, and it should NOT be clipped. It should be allowed whatever width the developer wants,
extending across the screen. But, when that text is tapped and held to activate dragging around the
screen, it MUST transform into the app icon. The AzNavRail can only be dragged around the screen as
an App icon. So it transforms into app icon when dragging is enabled, and transforms back to the app
name when docked back into place.

When I tap the app icon/name, it should expand or collapse the rail, revealing or hiding the menu.
When activating the drag and place function, otherwise called FAB mode, if the app name was enabled,
it turns into an app icon. All of the rail items
fold up into the app icon, and the icon can be moved anywhere on the screen as a fab. In this mode,
tapping the app icon causes all of the rail items to unfold downward. If the app icon is tapped
again, the rail items fold back up. If the app icon is dragged while the rail items are unfolded,
then they immediately fold back up until the app icon is released. It's important that while the app
icon is draggable,

To disable dragging, the user drags the app icon to its home location, which is where it was when in
docked mode.
If the user brings the app icon within half the app icon's width of its home location, the app icon
should snap back into place, activating the original docked mode.

I need the sample app to show in the logcat every function it actively performs and every user
interaction.

Haptics (superseded): the old rule was "the only haptic feedback should be when fab mode is
activated and deactivated". That left every interaction people actually perform — tapping an item,
flipping a toggle, landing a cycler — with no physical acknowledgement at all, which is the cheapest
conveyance there is to withhold. The rule now: haptics run through `internal/AzHaptics.kt`, gated by
`vibrate`, with two voices — `commit()` for a tap that did something, `modeChange()` for FAB in/out
and other mode flips. Still nothing per drag frame, and a cycler speaks once when it *commits*, not
on each tap through its options.

The menu SHOULD expand and collapse on single tap of the app icon/name.

the area for swipe to collapse SHOULD be a little wider than the expanded menu.

Tapping outside of the menu should also collapse the menu. 

Also, when in FAB mode, the app icon should snap back into place when brought near its original
docked position in non-fab mode

in FAB mode, if the app icon is long pressed, this should immediately disable FAB mode and redock
the rail.

the MENU is never supposed to be present when in FAB mode. If the app icon is long pressed while the
menu is expanded, it should fold up into the app icon, and when in fab mode and the app icon is
tapped, this should unfold the RAIL, not the menu. The menu should NEVER be available in FAB mode.

both a tap and a long press are defined not by when the touch begins but when it ends. So the logic
that makes a long press shouldn't be interfering with the logic that makes a tap. The gesture
listener hears the touch begin, and then, if it ends before what is considered a long press, then
it's considered a tap.

let's have two kinds of swipes. Horizontal swipes expand and collapse the rail. But a vertical swipe
immediately initiates FAB mode and undocks the rail.
swipe up causes all the rail/menu items to fold up into the app icon. This means the rail is in FAB
mode, in a resting state.
A swipe down when docked immediately initiates FAB mode and causes the app icon to be dragged, so
all the items fold up and the app icon is already being dragged around.
The vertical swipe logic should apply to the entire rail. A swipe up might start at the bottom or
the middle of the rail. A swipe down will always start near the app icon/name.
In fab mode, dragging must not be mistaken for a long press.
In FAB mode, the app icon must NOT be allowed above the top 10% or the bottom 10% of the screen.
Also, in FAB mode, a packed rail must be forced at all times. And, if the rail items are displayed,
when a drag begins, the rail items must immediately fold up into the app icon. When the rail items
are visible when a drag begins, they must unfold downward when the drag ends
In FAB mode, the rail items must also not be allowed above the top 10% of the screen nor the bottom
10% of the screen. This means that the rail should unfold downward, and push the location of the app
icon upward if necessary.

The rail can also be used as a system-wide overlay using a System Alert Window. This is activated by
providing the `overlayService` class in `azSettings`, which overrides the default internal FAB mode.
The Service should extend `AzNavRailOverlayService` and call `AzNavRail` within its content.
When undocked, the library will request the `SYSTEM_ALERT_WINDOW` permission if needed and start the service.

**Note:** The app must declare the `SYSTEM_ALERT_WINDOW` and `FOREGROUND_SERVICE` (and `FOREGROUND_SERVICE_SPECIAL_USE` if targeting API 34+) permissions in `AndroidManifest.xml`.
The Service extending `AzNavRailOverlayService` must also be declared in the manifest.
The subclass must implement `getNotification()` to return a notification for the foreground service, and may override `getNotificationId()`.

Add the ability to use a solid color, a number value, or an image (which may require fitting), specified on the fly, as the content of a RailItem. This will require a text-based alternative to be displayed as the equivalent MenuItem. 

Not to be confused with hosted items which contain inline rails of sub items, NestedRails unfold next to parent items, displaying a rail of child items either vertically or horizontally aligned. If horizontally aligned, then it should expand out from the Parent in a scrollable row, anchored next to the parent item. If vertically aligned, the NestedRail should appear as a column with its centermost item vertically centered on the screen. NestedRails must also be allowed to scroll if they take up 80% of the width or height of the screen, with the visible boundaries being the same as the AzNavRail itself. NestedRails are also able to contain AzRailHostItems, which expand to reveal AzRailSubItems. If vertically aligned, this manifests the same way it does on the main AzNavRail. However, if horizontally aligned, the RailSubItems should expand downward, vertically--not horizontally. 

The hidden menu width should be half what it is now. 

In landscape mode, the RailItems are still way too small, and should be the same size as they are in portrait mode. Force the width and height of each item in landscape to be the same as it is in portrait. 

There's a quirky bug with the generated screen title. When I click an AzRailToggle or an AzRailCycler, it displays the text on the button that was present when clicked. It SHOULD display the text of the option that is active.

The app icon in the header must be sizable to a specific diameter. Provide `headerIconSize`
(a `Dp` on Android via `azTheme`/`azSettings`, a pixel number on React). When unset, the icon keeps
its legacy behavior of sizing to the rail width. When set, the header icon is rendered at exactly
that width and height.

Drop-down menu (`AzDropdownMenu`): a standalone widget declared with the **same opinionated DSL as the
rail**. In AzNavRail tradition it accepts only sanctioned config — it does **not** expose arbitrary
icon tint/source, panel background, offsets, `menuWidth`, or a free `azCustom` escape hatch. The
trigger is the **app icon** (auto-drawn exactly like the rail's header — `getApplicationIcon` on
Android, gray placeholder on RN, `/app-icon.png` on web), placed inline; its shape/size are
configurable (mirroring the rail's `azTheme`). Only the dropped list is an overlay.

- File: `aznavrail/src/main/java/com/hereliesaz/aznavrail/AzDropdownMenu.kt` (Android),
  `aznavrail-react/src/components/AzDropdownMenu.tsx` (RN) and `src/web/AzDropdownMenu.jsx` (web).
- DSL like the rail (collect-then-render): the `content` is a plain `AzDropdownMenuScope.() -> Unit`
  builder. `azConfig(design, dockingSide, vibrate, expandedWidth, collapsedWidth, headerIconShape,
  headerIconSize, showFooter, inAppAbout = true, appRepositoryUrl = "")` mirrors the rail's
  `azConfig`/`azTheme` (RN/web take the same as props); items are `azItem`/`azToggle`/`azCycler`/`azDivider`
  accepting only the rail's per-item knobs (`color`/`textColor`/`fillColor`/`shape`/`enabled`/`closeOnClick`)
  plus a `route`. `appRepositoryUrl` is an optional override — blank auto-derives the repo from the app
  namespace (`com.<owner>.<repo>` → `github.com/<owner>/<repo>`), never the AzNavRail library repo.
- The `MENU` design renders rows at the rail's menu-item text size (Android `titleLarge`; RN/web 16px,
  matching `RailMenuItem`/`.az-menu-item-text`) and — like the rail's expanded menu — appends the
  footer (About, Feedback → mailto, @HereLiesAz → Instagram) when `showFooter`, mirroring
  `internal/Footer.kt` / the rail's `renderFooter`. The dropdown has no onscreen/host area, so tapping
  About opens a **full-screen** in-app reader drawn as its own layer when `inAppAbout = true` (the
  default); `inAppAbout = false` opens the resolved repo in a browser.
- `design` (`AzDropdownDesign { RAIL, MENU }`, default `MENU`) styles the panel as the collapsed rail
  (compact buttons, `collapsedWidth` ≈100dp) or the expanded menu (full-width labeled rows,
  `expandedWidth` ≈160dp). `dockingSide` (`AzDockingSide { LEFT, RIGHT }`) **pins the panel to that
  physical screen edge**; the vertical drop direction is derived automatically from the trigger
  (downward when it fits, else upward) via a custom window-edge `PopupPositionProvider` (Android) /
  measured-rect math (RN/web). The old `AzDropdownAlignment` + `parseDropdownAnchor` are removed.
- Routing: the composable takes `navController: NavController? = LocalAzNavHostScope.current?.navController`
  (auto-wires inside an `AzNavHost`); an item's `route` navigates it (then the callback, then dismiss),
  exactly like `MenuItem.kt`. RN/web use an `onNavigate(route)` prop + `route?` on `AzDropdownItem`.
- Controlled `expanded`/`onExpandedChange` remain. Tapping outside, back, or an item folds it up.

Every AzNavRail surface takes its colour from the RAIL, not from the app's theme. On React the same
contract is `AzRailPaletteContext` + `useAzAccent()` + `resolveRailAccent()`, in BOTH builds (`src/`
and `src/web/`); because a second floating rail there is a *sibling* rather than a descendant, the
rail also publishes its palette outside the tree (`usePublishRailPalette`) and `useAzAccent` falls
back to that when no provider is above. `AzNavRail`
publishes `LocalAzRailPalette` (`AzRailPalette(accent, surface)`); a second unattached/floating rail,
a drop-down menu, the About reader, the Help overlay, nested rails, popups and the drawer all resolve
their accent through `azAccent()`, which falls back to `MaterialTheme.colorScheme` only when there is
no rail. The accent itself is `AzNavRailScopeImpl.railAccent`: `activeColor` when the developer set
one, otherwise the colour most of the rail's own items are drawn in (`azResolveRailAccent`) — a rail
whose every button is white is a white rail, whatever `colorScheme.primary` says. The About reader
additionally forces its ground opaque: `translucentBackground` supplies the hue, never the alpha,
because a see-through full-screen reader is an unreadable one.

The rail must not be greedy with gestures. It is laid out over the whole window, so it may only
install pointer handlers it will actually answer, and may only consume the events it acts on:
- no window-wide tap listener — the expanded menu's scrim (inset to exclude the rail) collapses on
  outside taps. That scrim exists whenever the drawer is open on EVERY platform; `dimBehindMenu`
  decides only whether the area is darkened, never whether the tap-to-collapse affordance exists
  (React and web used to render it only when dimming was on, which silently deleted the documented
  behaviour for anyone who left dimming off);
- the drag detector is attached only when the rail is floating, draggable, or swipe-openable, and
  `change.consume()` is called only on the branch that actually undocks or moves the menu;
- the rail Surface swallows stray taps only while it is expanded or floating; collapsed and docked it
  is a mostly-empty full-height strip whose gaps belong to the app underneath;
- the "tap to dismiss the open nested rail" listener exists only while a nested rail is open.

Drop-down trigger: `AzDropdownMenu`'s trigger is chosen with `azConfig(trigger = …)` from the sanctioned
`AzDropdownTrigger` set — `MoreVert` (three vertical dots, **the default**), `Hamburger`, `AppIcon` (the
launcher icon, the pre-trigger default), `Text("…")`, or `Icon(model)` (ImageVector/Painter/URL/any Coil
model). Size and clip shape still come from `headerIconSize`/`headerIconShape`. `azConfig(triggerPlacement = …)`
takes `AzDropdownTriggerPlacement { AUTO, TITLE, INLINE }`: `AUTO` (default) lifts the trigger out of its call
site and places it **next to the big screen title**, above the onscreen area, whenever the drop-down is declared
inside an `AzHostActivityLayout`; a standalone drop-down stays inline. Several title-hosted drop-downs line their
triggers up beside each other in registration (= declaration) order, on the side opposite the rail. The dropped
panel stays composed at the call site and anchors to the real trigger via window-space bounds the trigger reports
back (`AzTitleTriggerSlot` in `internal/AzTitleTriggers.kt`; the slot publishes a comparable
`AzDropdownTriggerSpec` as snapshot state and keeps its tap/bounds callbacks as plain fields, which is what stops
the host's composition from looping).

Unattached host rail items (`azUnattachedHostItem`): a rail host that does **not** live in the rail strip. It is
drawn on its own at an `AzUnattachedAnchor` — `OPPOSITE` (side opposite the rail, level with the rail's items),
`BOTTOM` (bottom of the screen, opposite side), or `FLOATING` (draggable, position persisted) — and tapping it
unfolds its sub-items inline beneath it, exactly as they would have unfolded in the rail. Sub-items attach by
`hostId` as usual and sub-hosts nest to any depth. The host and its **whole subtree** are filtered out of both the
rail strip and the drawer (`azUnattachedSubtreeIds`). Several hosts sharing an anchor stack into a column with the
rail's own spacing/packing. The `FLOATING` stack drags as one unit, is clamped to the 10%–90% vertical safe zone
FAB mode uses, and persists its position **as a fraction of the window** (`AzUnattachedStore` —
multiplatform-settings on CMP, SharedPreferences on Android) so it survives rotation and different screen sizes.
Rendered by `internal/AzUnattachedRail.kt`, which owns its own `hostStates` (the rail's map does not cover it) and
re-implements the same rising-edge `initiallyExpanded` / `expandWhen` contract.

Per-item badges + loading: every item builder accepts `badge` / `persistentBadge` / `isLoading`, and **any**
already-declared item can be decorated by id with `azItemState(id, badge, persistentBadge, isLoading, alert)` —
applied after the whole DSL runs (alongside `applyRelocReorders`), so declaration order is irrelevant and null
fields leave the item's existing value alone. Loading is **per item**: the button hides its content and spins an
`AzLoad` scaled to the button and tinted to the item's colour (`AzLoad` now takes `size`/`color`/`showLabel`); a
menu row keeps its label and spins a small ring beside it. Badges now render in nested rails too — previously
`NestedRail.kt` silently dropped them.

Popups (`AzPopup`): a window **bound to a rail item**, registered with `azPopup(controller)` on the host DSL and
driven by an `AzPopupController` from `rememberAzPopupController()`. `show(itemId = …)` names the source item;
`show()` with no id binds to the **last touched** rail item (`AzNavRailScopeImpl.lastTouchedItemId`). The body runs
in an `AzPopupScope` exposing `kind`/`title`/`message`/`payload`, `dismiss()`, and `item` — an
`AzPopupItemHandle` that can read the live `AzNavItem` and write back to it (`setBadge`, `setLoading`, `setAlert`,
`clear`). Those writes land in `AzNavRailScopeImpl.itemOverrides`, which deliberately survives `reset()` (the DSL
re-runs every recomposition and would otherwise wipe them next frame) and wins over `azItemState`. A
`AzPopupKind.NOTICE` / `WARNING` popup redraws its bound item as a **yellow, rounded-corner triangle outline**
(`AzButtonShape.TRIANGLE` + `AzRoundedTriangleShape`, corners cut back and bridged with a quadratic through the
vertex) for exactly as long as the popup is up — raised and dropped by the popup's own `DisposableEffect`, never
left behind. In the drawer, where a row is type rather than a button, the flagged item takes the same yellow.

INVARIANT — every `AzButtonShape` -> Compose `Shape` conversion goes through the single
`AzButtonShape.toComposeShape()` in `internal/AzShapes.kt`, so adding a member can never be silently missed by one
call site.

INVARIANT — no dead config. `azAdvanced`/`azSettings` knobs must be **read** by something, not merely
stored. `isLoading`, `onRailDrag`, `onOverlayDrag` and `overlayService` were collected into
`AzAdvancedConfig` and documented for a long time while nothing consumed them; they are now wired:
`isLoading` draws a screen-centred `AzLoad` above everything (and swallows input) from
`AzHostActivityLayout`; `onRailDrag` fires on every FAB-mode drag delta; `onOverlayDrag` fires the
same way when an `overlayService` is configured; and supplying `overlayService` makes undocking hand
off to that service via `OverlayHelper.launch` (`rememberAzOverlayLauncher` — Android actual, no-op
on Desktop/Web/iOS). Do not reintroduce a config field with no reader.

INVARIANT — `navigation-compose` is an **`api`** dependency in both modules, not `implementation`:
`AzHostActivityLayout`/`AzNavHost` name `NavHostController` in their public signatures, so consumers
must resolve those types transitively.

Per-item state applies in a second pass. `applyItemStates()` runs right after `applyRelocReorders()`
in both `AzNavRail` (standalone) and `AzHostActivityLayout`, stamping `declaredItemStates` (from
`azItemState`, cleared by `reset()`) and `itemOverrides` (pushed by an `AzPopupItemHandle`,
deliberately NOT cleared by `reset()`) onto the freshly-rebuilt `navItems`, nested-rail children
included. Overrides win over declarations; null fields leave the item's existing value alone.

Toggle/cycler `AzNavItem.text` carries the **live label** (the checked/selected string) in both
modules. The rail and drawer derive their own label from `isChecked`/`selectedOption`, but guidance
(`computeAutoEdges` → "Tap Dark") and the help overlay read `text` and fall back to the raw item id
when it is blank. The CMP module used to store `""` here, which is why its callouts named ids.

Physical-docking rotation mapping is specified at the bottom of this file and implemented in
`AzRailLayoutHelper`: docked LEFT, `ROTATION_90` → TOP, `ROTATION_180` → LEFT, `ROTATION_270` →
BOTTOM (mirrored for RIGHT). `AzRailLayoutHelperTest` encodes exactly that. Note the 180° rule is a
deliberate product choice, not a physical consequence — a device turned upside down puts its
physical-left edge on the screen's right, but the spec says the rail stays LEFT.

The `@Az` annotation / KSP code-generation system **exists** and lives in two modules:
`:aznavrail-annotations` (a plain JVM jar — no Android or Compose types, so anything can depend on
it) and `:aznavrail-processor` (KSP + KotlinPoet), registered via `META-INF/services`. It generates
`<ActivityName>AzGraph : AzGraphInterface`, which `AzActivity.graph` points at.

INVARIANT — the generator emits **plain DSL**, the same calls a developer would write by hand, and
carries no runtime of its own. Keep it that way: the generated file has to stay readable and
debuggable, and every annotation must map onto an existing DSL call rather than a private hook.
Anything the annotations can't express belongs in `AzActivity.configureRail()` (called inside the
same DSL block) or `azGraphDestinations` (the generated `AzNavHost`'s destinations). The end-to-end
proof is `SampleApp`'s `AzGraphDemoActivity` — it compiles the generator on every SampleApp build, so
a broken generator breaks CI rather than rotting quietly.

CONVEYANCE — the library is measured against the Conveyance manifesto
(github.com/HereLiesAz/Conveyance): guide by example, resourceful minimalism, compassionate design.
Concretely, for this codebase:

- **Guidance and help are last resorts.** `azAdvanced(autoGuidanceEdges = …)` is **off by default**;
  the rail no longer auto-generates "Open the menu" / "Tap Settings" captions for its own
  affordances. A rail that has to caption its own buttons has already failed to convey them.
  Developer-authored `azEdge`s (for transitions into an app's own domain statuses) always apply.
- **No captions on affordances.** The guidance callout's "Tap to continue ▸" and the help card's
  "Tap to collapse" are gone — a breathing chevron and the revealed text carry those. Do not add
  copy that explains a tap target.
- **Every element earns its place.** `AzLoad` is a shape that morphs through rounded polygons rather
  than a ring plus the word "loading..." — the refusal to settle *is* the message, and it localises
  for free. Toggles/cyclers remain label-is-state-is-control. Per-item loading (`azItemState`) is
  preferred over the screen-blanking global `isLoading`.
- **Transformations, not swaps.** An item flagged by a notice/warning popup *morphs* into the
  warning triangle (`AzAlertMorphShape`) and morphs back; it never cuts. New surfaces get motion —
  the popup rises in, unattached sub-items unfold on the rail's accordion.
- **The dissolve must be hosted at the window root.** `DissolveOverlay` is rendered by
  `AzHostActivityLayout`, not by the rail or the dropdown, and state lives on `AzNavHostScopeImpl`.
  It used to render itself in a `Popup`, whose window is sized to its own content, so the travelling
  label was clipped the instant it left the rail — which looked exactly like it vanishing. Do not
  move it back inside the rail's layer.

In-app About reader + "More from Az": the footer "About" item opens a built-in, themed markdown reader
instead of opening the repo URL in a browser. On the rail (Android/web) About + More-from-Az flow
through the host's `onscreen()` layout path (rail stays docked beside them); the standalone
`AzDropdownMenu` has no onscreen area, so its About is drawn as its own **full-screen** layer. It
auto-discovers the consuming app's docs by listing the `.md` files in the repository root and the
`docs/` folder of the resolved repo via the GitHub contents API, builds a table of contents, and
renders each doc inline. Fetches are cached (ETag + TTL) to respect GitHub's unauthenticated rate
limit; offline/limited shows the last cached copy. Public repos only.

Repo resolution: on **Android** the repo is auto-derived from the app **namespace** — `com.<owner>.<repo>`
→ `https://github.com/<owner>/<repo>` (owner = 2nd segment, repo = last segment; a trailing build
suffix like `.debug` is stripped), via `GithubDocsRepository.repoUrlFromPackage`. `appRepositoryUrl`
(on `azConfig` and `AzDropdownMenu`'s `azConfig`, default `""`) is an **optional** override and the
derivation **never** falls back to the AzNavRail library repo. On **web** there is no namespace, so
`appRepositoryUrl` is **required** there (no auto-derivation); when unset the About entry is hidden.

About lives on the RAIL, not only in the drawer. The rail always ends with an About ("?") rail
item — an ordinary `AzNavItem` (`isAboutItem`, id `AzNavRailDefaults.AUTO_ABOUT_ID`) appended to the
strip, in every mode, not the hand-drawn footer glyph that used to appear only for `noMenu` rails.
It persists but it is NOT fixed: `azAboutRailItem(id, text, color, shape, …)` declares your own in
whatever position you call it from and suppresses the automatic one, and `azAbout(aboutRailItem =
false)` drops it. Tapping it toggles the reader.

The About reader must be easy to LEAVE. Tapping any other rail item, any menu item, or the app icon
closes it (only the About item itself toggles); back and drag-down still work. And it never covers
the rail's own gutter — `AzHostActivityLayout` insets the About / More-from-Az layer by the rail
width whether or not the rail is folded up, because an overlay drawn edge-to-edge over a folded rail
covers the very app icon you tap to bring the rail back.

Configured via `azAbout(inAppAbout, moreFromAzEnabled, moreFromAzJsonUrl, moreRailItem, aboutRailItem)`;
`inAppAbout = false` restores the browser behavior. A repo-root `.azignore` (one pattern per line;
`#` comments; exact paths, `dir/` prefixes, or `*` globs) excludes listed docs from the About TOC —
implemented in `GithubDocsRepository.parseIgnore`/`isIgnored` (Android) and `githubDocs.ts` (React).

Guides hidden over footer screens (all platforms): while a footer screen (About or More-from-Az) is
open, any visible Help cards and any in-progress tutorial are hidden but kept mounted, so they return
exactly where they were when the footer screen closes.

"More from Az" is a carousel of the author's other apps reachable from the About screen and/or a
pinned "More" rail item (`moreRailItem`). The maintainer **pastes GitHub repo links, one per line,
any order** into `more-from-az.json`. ALL resolution happens in CI, not the app:
`.github/scripts/bake_more_from_az.py` (run by `.github/workflows/bump-more-from-az.yml`, server-side
with the authenticated `GITHUB_TOKEN`) resolves each repo and **bakes a finished manifest**
(`{ version, apps:[{ name, iconUrl, description, github?, play?, web?, isPwa? }] }`):
- groups by repository (one repo = one app; never by URL-string matching),
- constructs+verifies the Play link from `com.<owner>.<repo>` (kept only if the listing resolves),
- reads website/PWA from the repo's GitHub homepage (PWA detected via `rel="manifest"`),
- excludes apps whose README first line contains the whole word `WIP`,
- sorts apps with a Play link first, fills name/icon/description, bumps `version`.
The rail (`service/MoreFromAzRepository.kt`, `services/moreFromAz.ts`) is a **thin renderer** that
just parses the baked apps (with a lenient fallback rendering degraded cards from raw link/string
entries before CI bakes). Do NOT reintroduce per-app runtime resolution in the rail — keep it in CI
(avoids the unauthenticated GitHub rate limit and web CORS). The carousel is built from the rail's
own components (`AzButton`, `AzLoad`, `AzDivider`, `AutoSizeText`) and tokens so it matches the rail.

Below the More-from-Az carousel, the About page ends with three sections, each rendered by
`AboutOverlay.kt` (mirrored on Android and CMP): `AzTipJar` (the free-forever pitch, verbatim: "Every
single one of my apps is available on Github in full, without ads or conditions of any kind, for free
and forever. But I never say no to just a the-tip." — plus a "Leave a Tip" button opening
`AZ_DONATE_URL`, `Footer.kt`'s `https://paypal.me/HereLiesAz`); `AzAuthorHeader` (a circular GitHub
avatar, the fixed name "Az", and a bio — avatar and bio are fetched live from
`https://api.github.com/users/HereLiesAz` via `GithubDocsRepository.fetchAuthorProfile` /
`AzAboutPrefetch.warmAuthorProfile`, never baked in); and `AzAboutPageFooter` (the @HereLiesAz /
Feedback / hereliesaz.com links, stacked vertically in large type — not the compact side-by-side row
the drawer's own `Footer.kt` uses for the same three destinations). This whole bottom stretch scrolls
independently of the docs TOC and the carousel above it, so it always has room for all three sections
regardless of screen size.

INVARIANT — do not break: the bake commit is made as `github-actions[bot]` with a `[skip ci]`
message. The `[skip ci]` is load-bearing: it stops the bake commit from re-triggering both that
workflow and `android-sample-build.yml` (which also has `paths-ignore` for `more-from-az.json` and
`**/*.md`). Do not hand-edit `version`.

Embedded guide sync: the Complete Guide has a single canonical copy at
`docs/AZNAVRAIL_COMPLETE_GUIDE.md`. The library also ships two bundled copies
(`aznavrail/src/main/assets/AZNAVRAIL_COMPLETE_GUIDE.md` and `aznavrail/src/main/resources/AZNAVRAIL_COMPLETE_GUIDE.md`)
packaged into the AAR. Edit ONLY the canonical `docs/` copy — `.github/workflows/sync-embedded-guide.yml`
copies it into both bundled paths and commits back (as `github-actions[bot]`, `[skip ci]`, same
loop-safety pattern as the bake workflow). Never hand-edit the bundled copies; they must stay
byte-identical to `docs/`.

As an option, I am changing how the AzNavRail switches from portrait to landscape mode. Instead of maintaining its position on the side of the screen, it maintains its position on the side of the device, and all elements of the rail each rotate in place. This may take some careful consideration for whatever logic is needed in different circumstances, like how RailHostItems are expanded, or the difference between the rail being docked on the right or left in portrait mode. Also--PAY ATTENTION--if the rail is docked to the left in portrait mode, rotating the device clockwise means it will be at the top of the screen. But if I rotate counter-clockwise, it should be at the bottom of the screen. And if I turned the device upside down, the rail should be on the left side.
