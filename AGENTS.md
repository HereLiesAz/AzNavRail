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

- **Nested Rails (`azNestedRail`)**: This is a distinct feature from Host Items. A Nested Rail opens a separate **popup overlay** adjacent to the parent item instead of expanding inline. It supports `VERTICAL` (column) and `HORIZONTAL` (row) alignment.

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

The only haptic feedback should be when fab mode is activated and deactivated, not at the start and
end of every drag event.

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

Drop-down menu mode: the rail can be used as a drop-down menu via `dropdownMenu = true` in
`azConfig`/`azSettings` (a `dropdownMenu` boolean in React settings). In this mode:

- `onscreen()` content is allowed the entire width of the screen (the rail reserves no horizontal
  band). The rail renders as a floating top-anchored trigger above the content.
- The app icon takes the place of the hamburger menu icon. The bleeding app-name header is not used
  here; it is always the icon.
- Tapping the icon causes either the menu items or the rail items to unfold like an accordion
  downward, reusing the exact fold/unfold mechanism already programmed for the floating-rail (FAB)
  feature. Tapping the icon again, tapping an item, or tapping outside folds them back up.
- The developer selects which set unfolds via `dropdownSource` (`RAIL` or `MENU`). There is no
  collapsing the rail or expanding the menu; whichever they choose is the one and only set the
  drop-down shows.
- This mode works at the explicit exclusion of: FAB mode/draggable rail and the overlay service,
  rail↔menu expansion, `noMenu`, all swipe gestures, physical docking/rotate-in-place, the footer,
  nested-rail popups, the rail width settings, the bleeding app-name header, the rail safe-zone
  padding, and the help overlay/tutorials. Host items still expand inline as an accordion.

In-app About reader + "More from Az": the footer "About" item opens a built-in, full-screen, themed
markdown reader (an overlay drawn over the live UI, like the help overlay) instead of opening the repo
URL in a browser. It auto-discovers the consuming app's docs by listing the `.md` files in the
repository root and the `docs/` folder of `appRepositoryUrl` via the GitHub contents API, builds a
table of contents, and renders each doc inline. Fetches are cached (ETag + TTL) to respect GitHub's
unauthenticated rate limit; offline/limited shows the last cached copy. Public repos only. Configured
via `azAbout(inAppAbout, moreFromAzEnabled, moreFromAzJsonUrl, moreRailItem)`; `inAppAbout = false`
restores the browser behavior. A repo-root `.azignore` (one pattern per line; `#` comments; exact
paths, `dir/` prefixes, or `*` globs) excludes listed docs from the About TOC — implemented in
`GithubDocsRepository.parseIgnore`/`isIgnored` (Android) and `githubDocs.ts` (React).

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

INVARIANT — do not break: the bake commit is made as `github-actions[bot]` with a `[skip ci]`
message. The `[skip ci]` is load-bearing: it stops the bake commit from re-triggering both that
workflow and `android-sample-build.yml` (which also has `paths-ignore` for `more-from-az.json` and
`**/*.md`). Do not hand-edit `version`.

As an option, I am changing how the AzNavRail switches from portrait to landscape mode. Instead of maintaining its position on the side of the screen, it maintains its position on the side of the device, and all elements of the rail each rotate in place. This may take some careful consideration for whatever logic is needed in different circumstances, like how RailHostItems are expanded, or the difference between the rail being docked on the right or left in portrait mode. Also--PAY ATTENTION--if the rail is docked to the left in portrait mode, rotating the device clockwise means it will be at the top of the screen. But if I rotate counter-clockwise, it should be at the bottom of the screen. And if I turned the device upside down, the rail should be on the left side.
