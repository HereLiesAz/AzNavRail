
# AzNavRail Complete Guide (Sample App Edition)

This guide documents the complete configuration and usage of the AzNavRail library as demonstrated in the official **Sample App**. It serves as the definitive reference for setting up layouts, configuring the rail, and implementing all supported components.

---

## 1. Top-Level Setup: Host Activity Layout

Every AzNavRail implementation **must** start with `AzHostActivityLayout`. This container manages safe zones, device rotation (0°, 90°, 270°), and z-ordering.

**Sample App Implementation:**
```kotlin
AzHostActivityLayout(
    navController = navController,
    modifier = Modifier.fillMaxSize(),
    currentDestination = currentDestination?.destination?.route,
    isLandscape = isLandscape, // derived from LocalConfiguration
    initiallyExpanded = false
) {
    // 1. Configure the Rail here (DSL)
    // 2. Define Background layers here (DSL)
    // 3. Define Onscreen content here (DSL)
}
```


**React (React Native / react-native-web) Equivalent:**
While Android uses `AzHostActivityLayout` and a DSL to manage positioning and Safe Zones automatically, React projects explicitly construct their layout and pass properties and arrays of objects. The React version enforces the same visual rules via standard flex layouts.

```tsx
import { AzNavRail, AzNavItem, AzNavRailSettings, AzDockingSide, AzButtonShape } from '@HereLiesAz/aznavrail-react';
import { View } from 'react-native';

const settings: AzNavRailSettings = {
    dockingSide: AzDockingSide.LEFT,
    packRailButtons: false,
    usePhysicalDocking: false,
    defaultShape: AzButtonShape.RECTANGLE,
    activeColor: '#6200EE',
    translucentBackground: 'rgba(0,0,0,0.5)',
    enableRailDragging: true,
    isLoading: false,
    helpList: { "home": "Home screen" },
    infoScreen: false,
    onDismissInfoScreen: () => {},
};

const items: AzNavItem[] = [
    // Define items array here
];

export default function AppLayout() {
    return (
        <View style={{ flex: 1, flexDirection: 'row' }}>
            <AzNavRail
                appName="My App"
                items={items}
                expanded={false}
                settings={settings}
                onToggleExpand={() => {}}
            />
            {/* Background and Onscreen Content */}
        </View>
    );
}
```

### 1.1 Edge-to-edge and window insets (Android 15 / SDK 35)

From Android 15, an app targeting SDK 35 is laid out **edge-to-edge whether it asks or not**, and
`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` is in force. `AzHostActivityLayout` handles that for you —
there is nothing to opt into:

- **The Activity is put edge-to-edge for you.** The host calls `enableEdgeToEdge()` on the enclosing
  `ComponentActivity` (falling back to `WindowCompat.setDecorFitsSystemWindows(window, false)` for a
  host that isn't one). Calling `enableEdgeToEdge()` yourself in `onCreate` is still fine and is what
  `AzActivity` does — the two are idempotent.
- **Safe zones clear the display cutout, not just the system bars.** The host resolves each edge as
  `max(systemBars, displayCutout)`. The cutout is not redundant: under enforced edge-to-edge a camera
  cutout can occlude an edge that carries no system bar at all — most visibly the short edge in
  landscape, where the rail docks. `ime` is deliberately **not** folded in (as
  `WindowInsets.safeDrawing` would), so the layout doesn't jump when the keyboard opens.
- **Horizontal safe zones exist now.** `AzSafeZones` carries `start` / `end` alongside `top` /
  `bottom`; both are `0.dp` in portrait and non-zero only where the system reports something to clear
  (a landscape button navigation bar, or a cutout on that edge). They are applied to the `onscreen`
  content area, the title row, and the built-in About / Help / More-from-Az overlays. In the
  `onscreen` area they are combined with the rail gutter by **`max`, not addition** — the rail hugs
  that same physical edge, so a rail wider than the system inset already clears it. Where that holds
  (the usual case) the content area is positioned exactly as before; it shifts only when the system
  inset is genuinely wider than the rail.
- **Every window the library adds sets the non-deprecated cutout mode.** Android 15 deprecated
  `LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT`, `…_SHORT_EDGES` and `…_NEVER`, and a window that never
  assigns the field inherits the deprecated `DEFAULT`. The bottom-sheet overlay, the nav-bar
  decoration window, and `AzNavRailWindowService`'s default `windowParams` all assign `…_ALWAYS`
  explicitly. **If you override `windowParams`, keep that assignment** — the Play Console flags the
  inherited value.
- **The deprecated bar-colour properties are no longer touched on Android 15+.**
  `navigationBarColor` / `isNavigationBarContrastEnforced` (used by `AzSheetConfig.drawBehindNavBar`
  under button navigation) are deprecated in API 35 and ignored for apps targeting SDK 35, so the
  library only writes them below API 35 — above it, the bar is already transparent and there is
  nothing to force.

Your own content inside `onscreen { … }` is already inside the safe area. Content you draw yourself
outside the host — a full-screen `background()`, for instance — is intentionally *not* inset; use
`LocalAzSafeZones.current` if you want the same treatment.

---

## 2. Rail Configuration (DSL)

Inside the `AzHostActivityLayout` content block, you configure the rail using three primary functions: `azConfig`, `azTheme`, and `azAdvanced`.

### A. General Configuration (`azConfig`)
Controls layout behavior and docking logic.

```kotlin
azConfig(
    packButtons = packRailButtons,       // Boolean: Pack items tightly vs spaced
    dockingSide = AzDockingSide.LEFT,    // Enum: LEFT or RIGHT
    noMenu = noMenu,                     // Boolean: Disable the side drawer entirely
    usePhysicalDocking = usePhysicalDocking, // Boolean: Anchor to physical hardware edge vs visual left
    railItemWidth = 64.dp                // Dp: Configure the base width of all rail items
    // appRepositoryUrl = ""                // Optional override for the About reader's repo (see below)
)
```

`azConfig` also takes `appRepositoryUrl` (default `""`), the repo the in-app **About** reader uses.
On **Android** the repo is auto-derived from the app **namespace** — `com.<owner>.<repo>` →
`https://github.com/<owner>/<repo>` (owner = 2nd segment, repo = last segment; a trailing build
suffix like `.debug` is stripped) — so `appRepositoryUrl` is an **optional** override and it **never**
falls back to the AzNavRail library repo. On **web** there is no package namespace, so
`appRepositoryUrl` is **required** there (no auto-derivation); when unset the About entry is hidden.
While a footer screen (About or More from Az) is open, visible Help cards and any in-progress tutorial
are hidden and restore exactly where they were on close (all platforms).


### B. Theming (`azTheme`)
Controls visual style defaults.

```kotlin
azTheme(
    defaultShape = AzButtonShape.RECTANGLE, // Default shape for all items
    activeColor = MaterialTheme.colorScheme.primary, // The ACTIVE highlight
    focusColor = Color.White,               // The FOCUS highlight (unset = same as activeColor)
    secondaryColor = Color(0xFFFFB300),     // The SECONDARY highlight (unset = the rail accent)
    translucentBackground = Color.Black.copy(alpha = 0.5f), // Set the background color for menus/overlays!
    headerIconSize = 48.dp                  // Exact app-icon diameter (Dp.Unspecified = size to rail width)
)
```

#### The three highlights

An item can be lit three ways. They are three separate colours because they answer three separate
questions, and an app that answers all three with one colour has told the user nothing:

| Highlight | The question | What lights it |
| --- | --- | --- |
| **Active** (`activeColor`) | *Where am I?* | the item's `route` **is** the current destination, or one of its `classifiers` is in `azConfig(activeClassifiers = …)` |
| **Focus** (`focusColor`) | *What am I touching?* | the item is pressed, or it was the last one tapped **and carries no route of its own** — a toggle, a cycler, an action |
| **Secondary** (`secondaryColor`) | *Whatever you decide* | only you: `azItemState(id, secondary = true)`, or `azConfig(secondaryClassifiers = …)` |

**Choosing them.** `activeColor` is the loudest thing on the rail, because "where you are" is the one
fact the rail exists to tell you — give it the app's accent. `focusColor` is a transient: it is on
screen only while a finger is, so it wants contrast against the rail's own colour rather than a
second hue competing with the accent; leaving it `Unspecified` reuses `activeColor`, which is how the
rail has always looked and is a perfectly good answer. `secondaryColor` should read as *a condition*
rather than *a place* — an amber for "armed", a green for "synced" — because the user will meet it on
an item that is emphatically not where they are.

When more than one applies at once: **focus** beats **active** beats **secondary**. A press is the
most immediate thing happening, and it lasts only as long as the press.

Any single item can disagree with the rail about any of the three:

```kotlin
azRailItem(id = "record", text = "Rec")
azHighlight(id = "record", active = Color.Red)   // red when it is the live screen
azItemState(id = "record", secondary = armed)    // amber while merely armed
```

`azHighlight` is applied after the whole DSL block runs (like `azItemState`), so declaration order is
irrelevant, `null` fields leave the rail's colour in place, and unknown ids are ignored. Nested-rail
children inherit the rail's palette, and `secondaryClassifiers` reaches them too.

**React Implementation:**
```tsx
const settings: AzNavRailSettings = {
    defaultShape: AzButtonShape.RECTANGLE,
    activeColor: '#6200EE',
    translucentBackground: 'rgba(0,0,0,0.5)',
};
// Pass this object to the settings prop on AzNavRail
```

#### Shapes

`defaultShape` (and every item's `shape`) accepts:

| Shape | Border | Footprint |
| --- | --- | --- |
| `CIRCLE` | yes | square box, circular clip |
| `SQUARE` | yes | square |
| `RECTANGLE` | yes | wide, fixed height |
| `TRIANGLE` | yes | square box, rounded-corner triangle |
| `NONE` | no | wide, fixed height (same as `RECTANGLE`) |
| `NONE_SQUARE` | no | square |
| `NONE_CIRCLE` | no | square box, circular clip |

A borderless shape keeps the footprint of the base shape it names, so dropping the border never
reflows the rail around the item — a `NONE_CIRCLE` item still lines up with the `CIRCLE` items
beside it. `AzButtonShape.isBorderless` / `.baseShape` expose the mapping (Android/CMP);
`isBorderlessShape()` / `baseShapeOf()` do the same on React.

#### One palette for every AzNavRail surface

`activeColor` is not just the rail's own accent — it is what **every other AzNavRail surface** draws
itself in: a second unattached or floating rail, a drop-down menu, the About reader, the
"More from Az" carousel, the Help overlay, nested-rail popups, popups, and the expanded drawer.
Chrome that belongs to the same navigation system has to look like it; falling back to the app's
theme is how a rail's own drop-down ends up a different colour from the rail.

When `activeColor` is left unset, the accent is **derived from the rail's own items** — the colour
most of them are drawn in — before falling back to the app theme. A rail whose every button is white
is a white rail, whatever `MaterialTheme.colorScheme.primary` says:

```kotlin
// No azTheme(activeColor = …) anywhere, yet the drop-down, the About reader and the
// unattached rail all come out white, because the rail reads as white.
azRailItem(id = "home", text = "Home", color = Color.White) { }
azRailItem(id = "docs", text = "Docs", color = Color.White) { }
```

The mechanism is a CompositionLocal on Android/CMP (`LocalAzRailPalette`, read via the internal
`azAccent()`), and a React context plus a published fallback for siblings
(`AzRailPaletteContext` / `useAzAccent()` / `resolveRailAccent()`). Consumers can read it too:

```kotlin
val accent = LocalAzRailPalette.current.accent   // Color.Unspecified when no rail is present
```
```tsx
const accent = useAzAccent()                     // falls back to the library default
```

The About reader and the drop-down's panel additionally force their ground **opaque**:
`translucentBackground` supplies the hue, never the alpha, because a see-through full-screen reader
is an unreadable one — and a menu you can read the app's artwork through is not a menu.

Those panels also run a **legibility guard** over the accent they land on. A colour the library
*chose* (the rail's accent, or the theme fallback behind it) is checked against the panel and swapped
for plain ink when it fails WCAG's 3:1 large-text ratio. A colour **you** named on an item is never
touched: if you pick the colour you get the colour; if you leave it to the library, the library owes
you a legible one.

### B2. Kinetic Typography (`azKinetics`)

Windows-Phone-7-style motion for the menu words: a staggered **turnstile** entrance/exit on the
expanded menu items, a 3D **tilt-on-press**, and the big **screen title's** sweep on navigation. It is
config-driven (preset enums, no free-composable escape hatch). Defaults animate; pass `AzEntrance.None`
/ `AzExit.None` to opt a surface out. In **FAB / floating** mode the cascade becomes a vertical up/down
slide (no docked edge to hinge on).

```kotlin
azKinetics(
    itemEntrance = AzEntrance.Turnstile,   // None | Fade | SlideUp | Turnstile  (default Turnstile)
    itemExit = AzExit.Turnstile,           // None | Fade | Turnstile            (default Turnstile)
    tiltOnPress = true,                    // off by default on the rail (drag-safe)
    itemTextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Light),
    titleEntrance = AzEntrance.Turnstile,  // the big AzNavHost screen title
)
```

The standalone `AzDropdownMenu` exposes the same item knobs on its own `azConfig` (also on by
default), and its app-icon trigger carries an automatic margin.

**React Implementation:** the rail reads kinetics from `settings`
(`itemEntrance`, `itemExit`, `titleEntrance`, `tiltOnPress`, `itemTextStyle`, …); `AzDropdownMenu`
takes matching props. `AzEasing.Wp7Decelerate` is the signature easing.

### C. Advanced Features (`azAdvanced`)
Enables complex behaviors like drag-and-drop and help overlays.

```kotlin
azAdvanced(
    isLoading = isLoading,               // Boolean: Show global loading overlay
    enableRailDragging = true,           // Boolean: Enable FAB Mode (detach rail)
    helpEnabled = showHelp,              // Boolean: Show Help Overlay
    helpList = mapOf("home" to "Home screen"), // Map<String, Any>: Extra help texts
    onDismissHelp = { showHelp = false },
    onInteraction = { itemId, item ->    // Called on every item interaction
        Log.d("Rail", "Interacted: $itemId (${item.text})")
    }
)
```

`onInteraction` fires whenever any rail item is interacted with — click, toggle, cycler advance, nested rail open, or reloc drag. It receives the item's `id` and the full `AzNavItem`, enabling analytics integration without per-item callbacks.

**React Implementation:**
```tsx
const settings: AzNavRailSettings = {
    isLoading: isLoading,
    enableRailDragging: true,
    infoScreen: showHelp,
    helpList: { "home": "Home screen" },
    onDismissInfoScreen: () => setShowHelp(false),
};
// Pass this object to the settings prop on AzNavRail
// onInteraction is passed as a prop on AzNavRail:
// <AzNavRail onInteraction={(action, details, item) => console.log(action, item)} ...>
```


> **Note on Help Overlay:**
> The `HelpOverlay` displays a short, truncated entry for each item to conserve space. Tapping a help card expands it to reveal the full description and any extra text provided in `helpList`. Furthermore, `helpList` can be supplied dynamically to `AzNestedRail` components for distinct, localized help data.

### C2. The About reader (`azAbout`)

```kotlin
azAbout(
    inAppAbout = true,          // in-app markdown reader (default) vs opening the repo in a browser
    moreFromAzEnabled = true,   // offer the "More from Az" carousel inside the reader
    moreRailItem = false,       // also pin a "More" item at the bottom of the rail
    aboutRailItem = true,       // end the rail with the built-in About ("?") item
    dedupeAbout = true,         // …and draw About in exactly ONE surface, never two
)
```

**About appears exactly once.** An app can offer About from several places at the same time — the
`?` rail item, the expanded menu's footer, a drop-down menu's footer — and left alone it will offer
it from all of them, which is how "About" ends up on screen three times over. The library keeps a
registry of every surface that *could* draw it and lets only the highest-ranked one actually do so:

1. a `azAboutRailItem` you declared yourself — nothing outranks a decision made on purpose;
2. the rail's expanded-menu footer;
3. a drop-down menu's footer;
4. the automatic `?` rail item, which the library added on its own and is first to give way.

Registration is by *availability*, not visibility — a drop-down claims its footer as soon as the
drop-down exists, not when its panel opens — so the answer is stable and nothing blinks in and out as
panels come and go. `dedupeAbout = false` (on the rail's `azAbout`, or a drop-down's `azConfig`) opts
out: that surface always draws its own About and stops suppressing anyone else's.

**The reader is loaded before it is opened.** As the rail composes it warms the docs listing, the
first document's markdown, and the More-from-Az manifest in the background, so About opens populated
instead of showing a spinner for work that could have been done while the user was doing something
else. A reader opened mid-flight (cold start, slow network) fills itself in the moment the fetch
lands. Everything still goes through the same ETag + 6h cache, so a warm start is usually a 304.

**The page ends with the author.** Below the carousel sit the same **Feedback** and **@HereLiesAz**
rows as the menu footer. About is where someone goes to find out who made this; making them close it
and hunt through a menu to say something about it would be a joke at their expense.

**The carousel snaps.** A flick hands focus to the next app or two and settles onto it rather than
coasting past a dozen; a gesture that ends between two cards is pulled the rest of the way to the
nearer one; and tapping an off-centre card brings it to the centre.

**About is on the RAIL when nothing else has it.** The rail ends with an About (`?`) **rail item**
whenever no higher-ranked surface is offering one — always the case for a `noMenu` rail, which has no
drawer footer to put it in. When the rail does have a footer, that footer carries About and the
automatic `?` stands down (see de-duplication below). It persists, but it is not fixed:

```kotlin
azAboutRailItem(id = "about", text = "?", color = Color.White, info = "What this app is")
azAbout(aboutRailItem = false)   // …or no About item at all
```

Declaring your own suppresses the automatic one, so its position in the item order, its text, colour,
shape and help text are yours. On React it is `<AzAboutRailItem id="about" />` and the
`aboutRailItem` setting.

**Leaving the reader.** It is deliberately easy to get out of:

- tap the About item again;
- tap **any other rail item, any menu item, or the app icon**;
- drag down anywhere on the reader (a grab handle announces it);
- the 48dp close target, or system back.

**The reader never covers the rail's own gutter.** `AzHostActivityLayout` insets the About /
More-from-Az layer by the rail's width whether or not the rail is folded up. Drawn edge-to-edge over
a folded rail it would sit on the very app icon you tap to bring the rail back — an app you cannot
leave the reader from.

### C3. Gestures the rail will and will not claim

The rail is laid out **over the whole window**, so what it listens for is what the app underneath
does not get. It only installs pointer handlers it can answer, and only consumes the events it acts
on:

| Situation | What the rail does |
| --- | --- |
| Menu expanded | A scrim covering everything **except the rail** collapses the menu on an outside tap. It exists whenever the drawer is open; `dimBehindMenu` only decides whether that area is also darkened. |
| Drag anywhere on the rail | The drag detector is installed only when the rail is floating, `enableRailDragging` is on, or the menu is swipe-openable. It consumes the pointer only on the branch that actually undocks the rail or moves the menu, so a scroll that starts under the rail still reaches your content. |
| Tap on the collapsed, docked rail | Only the buttons take taps. The gaps between them — and the empty strip above and below — pass through to whatever the app drew underneath. |
| Tap while expanded or floating | The rail is a panel in its own right and swallows stray taps. |
| Nested rail open | A tap-to-dismiss listener exists over the strip **only while one is open**. |

There is deliberately **no window-wide tap listener**. If you find the rail eating a gesture your app
wanted, that is a bug — file it.

### D. Drop-down menu — `AzDropdownMenu` (standalone)

A hamburger drop-down is **not** a rail mode — it is a standalone widget, `AzDropdownMenu`, declared
with the **same opinionated DSL as the rail**. In AzNavRail tradition it accepts only the
configuration the rest of the library sanctions (no arbitrary panel background, offsets, or free
composable escape hatch).

**The trigger.** `azConfig(trigger = …)` picks what the user taps, from the sanctioned set
`AzDropdownTrigger`:

| Trigger | What it draws |
| --- | --- |
| `AzDropdownTrigger.MoreVert` | Three vertical dots. **The default.** |
| `AzDropdownTrigger.Hamburger` | The three stacked bars. |
| `AzDropdownTrigger.Text("Filter")` | A word, in the rail's accent and menu type. |
| `AzDropdownTrigger.Icon(myVector)` | Your own `ImageVector` / `Painter` / image URL / any Coil model. |
| `AzDropdownTrigger.AppIcon` | The launcher icon, exactly like the rail's header (the pre-trigger default). |

Its size and clip shape come from `azConfig`'s `headerIconSize` / `headerIconShape`, mirroring the
rail's `azTheme`.

**Where the trigger goes.** `azConfig(triggerPlacement = …)` takes an `AzDropdownTriggerPlacement`.
The default, `AUTO`, means: when the drop-down is declared inside an `AzHostActivityLayout` — i.e.
inside an `onscreen { … }` block, which is where drop-downs actually live — the trigger is **lifted
out of the call site and placed next to the big screen title**, above the onscreen content area, on
the side opposite the rail. Declare several drop-downs and their triggers **line up beside each
other** there, in declaration order. The dropped panel still belongs to the call site and still
drops from the real trigger button, because the button reports its window bounds back. A drop-down
used outside a host has no title row, so `AUTO` leaves it inline. Force either with `TITLE` /
`INLINE`.

Tapping it unfolds an **overlay
panel** (a `Popup`) of the items you declare. Configure it through `azConfig`: `design` picks
`AzDropdownDesign.RAIL` (compact rail buttons at the collapsed width ≈100dp) or `AzDropdownDesign.MENU`
(default; full-width labeled rows at the expanded width ≈160dp); `dockingSide` pins the panel to the
`LEFT`/`RIGHT` screen edge; the panel drops from the trigger automatically. The `MENU` design
renders rows at the rail's menu-item text size and, like the rail's expanded menu, carries the
footer (About / Feedback / @HereLiesAz, gated by `showFooter`). Because the dropdown has no
onscreen/host area, tapping **About** opens a **full-screen** in-app reader drawn as its own layer
when `inAppAbout = true` (the default; `inAppAbout = false` opens the repo in a browser). The repo is
auto-derived from the app namespace on Android, with `azConfig`'s `appRepositoryUrl` as an optional
override (never the AzNavRail library repo). Items use `azItem`/`azToggle`/`azCycler`/`azDivider` with
only the rail's sanctioned per-item knobs, plus a `route` that navigates the supplied `NavController`
(so the drop-down can drive an `AzNavHost`).

```kotlin
AzDropdownMenu(navController = navController) {
    // Three dots next to the screen title, because both defaults already say so.
    azConfig(design = AzDropdownDesign.MENU, dockingSide = AzDockingSide.LEFT)
    azItem("Home", route = "home") { }
    azToggle(isChecked = dark, toggleOnText = "Dark", toggleOffText = "Light") { dark = it }
    azDivider()
    azItem("Sign out") { signOut() }
}
```

```tsx
<AzDropdownMenu design={AzDropdownDesign.MENU} dockingSide={AzDockingSide.LEFT} onNavigate={go}>
  <AzDropdownItem text="Home" route="home" onClick={() => {}} />
  <AzDivider />
  <AzDropdownItem text="Sign out" onClick={signOut} />
</AzDropdownMenu>
```

---

## 3. Navigation Items (DSL)

All items inherit base styling from the `azTheme` block (which scopes properties like text colour and shape to `Rail`, `Menu`, or `Both`).

- `badge`: A text value to show as a badge on the item. By default, this badge is transient and will disappear after 1 second of being changed.
- `persistentBadge`: Set to true to make the badge visible permanently.

Items are added sequentially. The order in the DSL determines the order in the rail/menu.

### Standard Items
*   **Menu Item:** Only appears in the expanded drawer.
*   **Help Rail Item:** Dedicated trigger for the Help overlay.
*   **Rail Item:** Appears in the rail (and drawer).
*   **Content Types:** The `content` field accepts Text, a `Color`, a drawable/vector
    resource id (`Int`), a Compose `ImageVector` (e.g. `Icons.Default.Home`) or `Painter`,
    or any image model Coil can load (`Bitmap`, URL, `File`, `Uri`, …). All non-text graphics
    **fill the item's shape** (scaled to cover, clipped to the shape) without changing the
    item's dimensions. `ImageVector` content is tinted with the item's color, so monochrome
    Material icons adopt the rail's color. This applies to both main-rail and nested-rail items
    (the DSL `content` field). The standalone `AzButton`/`AzToggle`/`AzCycler` instead take a
    composable `itemContent` lambda, which is also clipped to the button shape.

```kotlin
// Menu-only item
azMenuItem(
    id = "home",
    text = "Home",
    route = "home",
    info = "Navigate to the Home screen",
    badge = "New",
    persistentBadge = true,
    onClick = { /* log click */ }
)

// Multi-line text support
azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line")

// Help trigger rail item
azHelpRailItem(id = "help-trigger", text = "Get Help")

// Help trigger as a sub-item
azHelpSubItem(id = "help-sub-trigger", hostId = "rail-host", text = "Get Help Here")

// About trigger. One is appended to the end of the rail automatically, in every mode — declaring
// your own replaces it, so its position, text, colour and shape become yours. `azAbout(aboutRailItem
// = false)` removes it entirely. Tapping it opens the About reader; tapping it again closes it, and
// so does tapping any other rail or menu item.
azAboutRailItem(id = "about", text = "?")

// Rail item with Color content
azRailItem(id = "color-item", text = "Color", content = Color.Red)

// Rail item with Icon Resource
azRailItem(id = "icon-item", text = "Icon", content = android.R.drawable.ic_menu_agenda)

// Rail item with a Compose ImageVector (fills + clips to the shape, tinted with the item color)
azRailItem(id = "vector-item", text = "Delete", content = Icons.Default.Delete)

// Rail item with specific shape override. The borderless family keeps the footprint of the base
// shape it names — NONE is a wide rectangle, NONE_SQUARE a square, NONE_CIRCLE a circle — so a
// borderless item still lines up with the bordered ones beside it.
azRailItem(id = "none-shape", text = "No Shape", shape = AzButtonShape.NONE)
azRailItem(id = "none-square", text = "Square", shape = AzButtonShape.NONE_SQUARE)
azRailItem(id = "none-circle", text = "Circle", shape = AzButtonShape.NONE_CIRCLE)

// Rail item with Custom Composable Content Size
azRailItem(id = "wide-composable", text = "Wide", content = AzComposableContent {
    Box(Modifier.width(120.dp).background(Color.Blue))
}) // Will not clip to rail width!

// Disabled item
azRailItem(id = "profile", text = "Profile", disabled = true, route = "profile")

// Rail item with custom @Composable content via AzComposableContent
azRailItem(
    id = "size_item",
    text = "Size",
    content = AzComposableContent { isEnabled ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEnabled) {
                    if (isEnabled) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            // Drag logic
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
)
```

**React Implementation:**
```tsx
// Tutorials are mapped through helpList in React
const settings: AzNavRailSettings = {
    infoScreen: true,
    helpList: {
        "item-1": "Help text for item 1"
    }
};
```

### Toggles
Binary switches for state (e.g., Online/Offline, Dark Mode).

```kotlin
// Rail Toggle
azRailToggle(
    id = "pack-rail",
    isChecked = packRailButtons,
    toggleOnText = "Packed",
    toggleOffText = "Unpacked",
    route = "pack-rail",
    onClick = { packRailButtons = !packRailButtons }
)

// Menu Toggle
azMenuToggle(
    id = "dark-mode",
    isChecked = isDarkMode,
    toggleOnText = "Dark Mode",
    toggleOffText = "Light Mode",
    onClick = { isDarkMode = !isDarkMode }
)
```

### Cyclers
Multi-state buttons that cycle through a list of options.

```kotlin
// Rail Cycler (with disabled specific option)
azRailCycler(
    id = "rail-cycler",
    options = listOf("A", "B", "C", "D"),
    selectedOption = "A",
    disabledOptions = listOf("C"),
    onClick = { /* cycle logic */ }
)

// Menu Cycler
azMenuCycler(
    id = "menu-cycler",
    options = listOf("X", "Y", "Z"),
    selectedOption = "X",
    onClick = { /* cycle logic */ }
)
```

### Dividers
Visual separators.
```kotlin
azDivider()
```

---

## 4. Hierarchical Navigation (Hosts)

Hosts are accordion-style items that expand to reveal sub-items.

```kotlin
// Menu Host
azMenuHostItem(id = "menu-host", text = "Menu Host")
// Sub-items must reference the hostId
azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1")
azMenuSubToggle(id = "sub-toggle", hostId = "menu-host", isChecked = true, toggleOnText = "On", toggleOffText = "Off")

// Rail Host
azRailHostItem(id = "rail-host", text = "Rail Host")
azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1")
azHelpSubItem(id = "help-sub-item", hostId = "rail-host", text = "Help Sub")
azRailSubCycler(id = "sub-cycler", hostId = "rail-host", options = listOf("A", "B"), selectedOption = "A")
```

### Nested hosts (sub-items that are also hosts)

A sub-item can itself be a host with its own sub-items via `azRailSubHostItem` /
`azMenuSubHostItem`. Hosts nest to **any depth**: opening a sub-host reveals its children
inline while its sibling sub-items stay visible (accordion behavior at every level).

Children are matched to their host by `hostId` (a reference, not by position), so a sub-host's
children are unambiguous even when they sit among other sub-items.

```kotlin
azRailHostItem(id = "rail-host", text = "Rail Host")
azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1")

// "rail-subhost" is a child of "rail-host" AND a host for its own children.
azRailSubHostItem(id = "rail-subhost", hostId = "rail-host", text = "Rail Sub Host")
azRailSubItem(id = "nested-a", hostId = "rail-subhost", text = "Nested A")
azRailSubItem(id = "nested-b", hostId = "rail-subhost", text = "Nested B")
```

> The parent host referenced by `hostId` must be declared **before** the sub-host, and a
> sub-host may not reference itself.

### Reactive expansion (`expandWhen`)

All host-item builders accept an optional `expandWhen: (() -> Boolean)?` parameter.
The lambda is a reactive condition: when its return value transitions **false → true** the
host auto-expands; when it transitions **true → false** the host auto-collapses.
The "user wins" rule applies: a manual collapse while the condition is `true` is respected;
the condition fires again only on the next false→true edge.

```kotlin
// Auto-expand the "features" host while a tutorial is active
azRailHostItem(
    id = "features",
    text = "Features",
    expandWhen = { tutorialController.activeTutorialId.value == "onboarding" }
)
azRailSubItem(id = "feature-a", hostId = "features", text = "Feature A")
```

This is particularly useful with the tutorial framework: a tutorial card that uses
`AzHighlight.Item("feature-a")` requires "feature-a" to be laid out (and therefore in
`itemBoundsCache`). Without `expandWhen` a collapsed host hides its children from layout,
causing the punch-out to silently fall back to a full-screen dim.

`expandWhen` and `initiallyExpanded` coexist: `initiallyExpanded` fires once on first
appearance; `expandWhen` fires on every subsequent edge transition.

The React/web equivalent is the `expandWhen` prop on `<AzRailHostItem>`:

```tsx
<AzRailHostItem
  id="features"
  text="Features"
  expandWhen={() => tutorialController.activeTutorialId === 'onboarding'}
/>
```

### Observing expansion state (`onExpandedChange`)

To react to the rail's own expand/collapse transitions from outside the composable — for example to adjust adjacent layout, drive analytics, or synchronise external state — pass `onExpandedChange` to `AzNavRail` (Android) or `AzHostActivityLayout` / `AzNavRail` (React/web).

**Android:**
```kotlin
AzNavRail(
    onExpandedChange = { expanded ->
        // true when the rail opens its menu, false when it collapses
        updateSidebarWidth(expanded)
    }
) { … }
```

Or via `AzHostActivityLayout`:
```kotlin
AzHostActivityLayout(
    onExpandedChange = { expanded -> railIsExpanded = expanded },
    …
) { … }
```

**React/web:**
```tsx
<AzHostActivityLayout
  onExpandedChange={(expanded) => setRailExpanded(expanded)}
  …
>
  …
</AzHostActivityLayout>
```

The callback fires once per state transition (expand or collapse), including on initial composition with the starting value. To also observe host-item sub-menu expansion, use `onInteraction` and filter for `action === 'Host toggled'` (React) or the item's `isHost` flag (Android).

---

### Unattached hosts (`azUnattachedHostItem`)

A host does not have to live *in* the rail. `azUnattachedHostItem` declares a rail host that is
drawn on its own somewhere else on the screen; tapping it unfolds its sub-items inline beneath it,
exactly as they would have unfolded inside the rail. Sub-items attach the usual way — by pointing
their `hostId` at it — so `azRailSubItem` / `azRailSubToggle` / `azRailSubCycler` /
`azRailSubHostItem` all work unchanged, and sub-hosts still nest to any depth.

The host and its whole subtree are removed from the rail strip *and* the drawer menu: an unattached
host exists only at its anchor.

`anchor: AzUnattachedAnchor` says where it parks:

| Anchor | Where it sits |
| --- | --- |
| `OPPOSITE` (default) | The side of the screen opposite the rail, level with where the rail's own items start. |
| `BOTTOM` | The bottom of the screen, on the side opposite the rail. |
| `FLOATING` | Free-floating and **draggable**, with its position **persisted** across launches. |

Declare several unattached hosts sharing an anchor and they **stack into a column**, spaced exactly
as they would have been in the rail (and packed when `packButtons` is on). The `FLOATING` stack
drags as one unit; its position is stored as a fraction of the window, so it survives rotation and
lands sensibly on a different screen size, and it is clamped to the same 10%–90% vertical safe zone
FAB mode uses.

```kotlin
azUnattachedHostItem(id = "tools", text = "Tools", anchor = AzUnattachedAnchor.FLOATING)
azRailSubItem(id = "measure", hostId = "tools", text = "Measure") { measure() }
azRailSubToggle(
    id = "grid", hostId = "tools",
    isChecked = grid, toggleOnText = "Grid On", toggleOffText = "Grid Off",
) { grid = !grid }

azUnattachedHostItem(id = "layers", text = "Layers", anchor = AzUnattachedAnchor.BOTTOM)
azRailSubItem(id = "layer-1", hostId = "layers", text = "Base") { select(0) }
```


### Per-item badges, loading and alerts (`azItemState`)

Every item builder takes `badge` / `persistentBadge` / `isLoading` directly, and **every** item —
rail item, menu item, sub-item, host, unattached host, nested-rail child — can also be decorated
after the fact with `azItemState`, so you never have to thread the same three arguments through a
builder that happens not to be the one you are using:

```kotlin
azRailItem(id = "sync", text = "Sync") { startSync() }
azItemState(
    id = "sync",
    isLoading = syncing,                                   // this item spins its own animation
    badge = pending.takeIf { it > 0 }?.toString(),          // and carries its own badge
    secondary = queued,                                     // …and wears the secondary highlight
)
```

`azItemState` is applied after every item is declared, so declaration order does not matter, and
values left `null` leave whatever the item already had — decorating the badge does not clear the
loading state. Unknown ids are ignored.

**Loading is per item, not per app.** A loading item hides its content and spins an `AzLoad` ring
scaled to its own button and tinted to its own colour; the rest of the rail stays live. In the
drawer, the row keeps its label and spins a small ring beside it.

**Badges render everywhere.** Rail buttons, menu rows and nested-rail children all draw them
(nested-rail children previously dropped a declared badge on the floor).

## 5. Drag & Drop (Relocatable Items)

Items that can be reordered by the user.
**Requirement:** Minimum of 2 items with the same `hostId`.

```kotlin
azRailRelocItem(
    id = "reloc-1",
    hostId = "rail-host", // Cluster ID
    text = "Reloc Item 1",
    forceHiddenMenuOpen = false, // Programmatic control for hidden context menu
    onHiddenMenuDismiss = { /* Menu was closed! */ },
    onRelocate = { from, to, newOrder -> /* handle reorder */ }
) {
    // Hidden Context Menu (Tap to open)
    listItem(text = "Action 1", onClick = { })
}
```

---

## 6. Nested Rails (Popups)

Secondary rails that open in a popup overlay. Do NOT assign a route to the parent item.

**Dynamic Bumping Effect:** When a vertical nested rail is opened, the main navigation rail will dynamically decrease its width (shrinking to the button width) to simulate the nested rail bumping it out of the way. Closing the nested rail restores the main rail to its original width.

```kotlin
// Vertical Nested Rail
azNestedRail(
    id = "nested-rail",
    text = "Vertical Nested",
    alignment = AzNestedRailAlignment.VERTICAL,
    keepNestedRailOpen = true // Remains open until parent is tapped again
) {
    azRailItem(id = "nested-1", text = "Nested Item 1", route = "nested-1")
}

// Horizontal Nested Rail
azNestedRail(
    id = "nested-horizontal",
    text = "Horizontal Nested",
    alignment = AzNestedRailAlignment.HORIZONTAL
) {
    azRailItem(id = "nested-h-1", text = "H-Item 1")
}
```

---

## 7. Layout Layers (Background & Onscreen)

AzNavRail allows defining content layers relative to the rail.

### Background Layers
Content placed *behind* the rail.

```kotlin
background(weight = 0) {
    // Full screen background (e.g. Map)
    Box(Modifier.fillMaxSize().background(Color(0xFFEEEEEE)))
}

background(weight = 10) {
    // Layer with padding
    Box(...)
}
```

### Onscreen Content
The main UI content, automatically padded to respect safe zones and rail width.

**Usage:**
~~~kotlin
// Basic Usage
azRailRelocItem(
    id = "1",
    hostId = "favs",
    text = "Favorite A",
    onRelocate = { from, to, newOrder -> }
) {
    // Define Hidden Context Menu (Fallback)
    listItem("Delete") { }
}

// As a Nested Rail Parent
azRailRelocItem(
    id = "tools_reloc",
    hostId = "toolbar",
    text = "Drag Me",
    nestedRailAlignment = AzNestedRailAlignment.HORIZONTAL, // Customize direction
    keepNestedRailOpen = true, // Remains open until parent is tapped again
    nestedContent = {
        // This content appears in the popup when the item is clicked (not dragged)
        azRailItem("hammer", "Hammer")
        azRailItem("wrench", "Wrench")
    }
) {
    // Hidden Menu (optional if nestedContent is provided)
    listItem("Remove Tool") { }
}
~~~

---

## 8. Standalone Components

These components are used within your screens (e.g., inside `AzNavHost`), not inside the rail configuration.

### AzTextBox
Advanced text input with history support.

*   **Uncontrolled (History):** `historyContext` persists values.
    ```kotlin
    AzTextBox(hint = "Search", historyContext = "search_history", onSubmit = {})
    ```
*   **Controlled:** Manually manage state via `value` and `onValueChange`.
    ```kotlin
    AzTextBox(value = text, onValueChange = { text = it }, hint = "Controlled")
    ```
*   **No Outline:** `outlined = false`
*   **Disabled:** `enabled = false`

### AzForm
Groups AzTextBoxes for validation and traversal.

```kotlin
AzForm(
    formName = "loginForm",
    onSubmit = { formData -> /* Map<String, String> */ }
) {
    entry(entryName = "username", hint = "Username", initialValue = "AzRailFan") // Pre-filled!
    entry(entryName = "password", hint = "Password", secret = true) // Password mask
    entry(entryName = "bio", hint = "Biography", multiline = true)  // Multi-line
}
```

### AzRoller
Slot-machine style selector.

```kotlin
AzRoller(
    options = listOf("Cherry", "Bell", "Bar"),
    selectedOption = "Cherry",
    onOptionSelected = { it -> }
)
```

### AzButton / AzToggle / AzCycler
Standalone versions of rail components for general UI use.

```kotlin
AzButton(text = "Button", onClick = {}, shape = AzButtonShape.SQUARE)
AzToggle(isChecked = true, onToggle = {}, toggleOnText = "On", toggleOffText = "Off")
AzCycler(options = listOf("1", "2"), selectedOption = "1", onCycle = {})
```


## 9. Tutorial Framework

The tutorial framework scripts interactive, multi-scene walkthroughs over a dimmed overlay. Each tutorial has one or more scenes; each scene has one or more cards. Cards can spotlight rail items, require user actions before advancing, show inline media, and present interactive checklists. Scenes can branch based on runtime variables or based on which highlighted item the user taps.

### 9.1 Concepts

**Scene** — a "scripted screen state." You provide a `content` composable/component that renders behind the overlay. The overlay dims everything outside the spotlight and shows the current card.

**Card** — a single instructional step. It has a title, body text, an optional spotlight (`AzHighlight`), and an advance condition (`AzAdvanceCondition`).

**Advance conditions:**
- `Button` (default) — a "Next" button is shown.
- `TapTarget` — user must tap the spotlighted item.
- `TapAnywhere` — user taps anywhere to advance.
- `Event(name)` — advances when the app calls `controller.fireEvent(name)`.

**Highlights:**
- `AzHighlight.None` — no spotlight.
- `AzHighlight.FullScreen` — full-screen highlight.
- `AzHighlight.Item(id)` — spotlights a named rail item (uses measured bounds).
- `AzHighlight.Area(rect)` — spotlights an arbitrary rect.

Card auto-positioning: defaults to bottom. Flips to top when highlight center Y > 60% of screen height. `TapTarget` degrades to `TapAnywhere` if the highlight is not `AzHighlight.Item`.

### 9.2 How advancement works (read this before building your own coach)

This framework is the supported way to walk a user through the rail. **Do not roll your own
overlay that waits for taps on rail items to "fall through" to the canvas — they never will.**
The rail consumes its own pointer events by design (a tap on a nav control must not leak to the
content behind it). `AzTutorialOverlay` does **not** depend on that leak; it advances through its
own hit-testing layer:

- **`TapTarget`** — the overlay draws an invisible, full-screen tap absorber plus a clickable box
  positioned exactly over the spotlight punch-out (computed from `itemBoundsCache`). Tapping inside
  the spotlight advances (or branches via `branches`); tapping outside is swallowed. Advancement is
  driven by the overlay's own box, not by the rail item's `onClick`. This is why `TapTarget`
  **requires** an `AzHighlight.Item(id)` whose bounds are known — without bounds it degrades to
  `TapAnywhere`.
- **`Event(name)`** — you advance imperatively by calling `controller.fireEvent(name)` from your own
  logic. Pair this with `azAdvanced(onInteraction = ...)` (see below) when you want a *real* rail tap
  — including expanding a host — to drive the tutorial.
- **`TapAnywhere` / `Button`** — advance on any screen tap, or on an explicit "Next" button.

**Required wiring for item spotlights.** `AzHighlight.Item(id)` and `TapTarget` both need the live,
measured bounds of the rail item. The rail reports these through
`azAdvanced(onItemGloballyPositioned = { id, rect -> ... })`; you collect them into a map and pass
that same map to the overlay as `itemBoundsCache`. If the cache is missing an id, the spotlight
renders no punch-out and `TapTarget` degrades to `TapAnywhere`. This handshake is shown end-to-end
in §9.4 (steps 2 and 3) and is easy to forget — if your spotlight never appears, this is the first
thing to check.

**Driving a tutorial from real rail taps (`onInteraction`).** When you want the tutorial to advance
because the user *actually used* the rail — tapped a leaf item, flipped a toggle, or expanded a host
menu — wire `azAdvanced(onInteraction = { id, item -> ... })` and call `controller.fireEvent(...)`
from it for the ids you care about. `onInteraction` fires for **every** interactive item in **both**
the compact rail and the expanded menu: leaf items (`azRailItem` / `azRailSubItem`), toggles,
cyclers, nested-rail opens, reloc drags, **and host items** (`azRailHostItem` — the expand/collapse
tap reports the interaction just like a leaf tap). This is the correct, supported alternative to
intercepting taps yourself, and it composes cleanly with `Event` advance conditions.

### 9.3 Help/Info Overlay Integration

- **Collapsed card:** Shows a "Tutorial available" hint when a tutorial exists for that item.
- **Expanded card:** Shows a "Start Tutorial" button. Tapping it calls `tutorialController.startTutorial(id)` and dismisses the help overlay.
- The old behavior (any tap immediately starts the tutorial) is removed.

### 9.4 Android — Full Example

```kotlin
import com.hereliesaz.aznavrail.tutorial.*

// 1. Define the tutorial
val myTutorial = azTutorial {
    onComplete { /* fired when last scene finishes */ }
    onSkip { /* fired when Skip Tutorial tapped */ }

    // Invisible redirect node: routes based on a variable
    scene(id = "gate", content = { /* empty backdrop */ }) {
        branch(varName = "userLevel", mapOf(
            "advanced" to "scene-advanced",
            "basic"    to "scene-basic"
        ))
    }

    scene(id = "scene-advanced", content = { AdvancedScreen() }) {

        // TapTarget with per-item branching
        card(
            title = "Pick a path",
            text = "Tap the item you want to learn about.",
            highlight = AzHighlight.Item("nav-menu"),
            advanceCondition = AzAdvanceCondition.TapTarget,
            branches = mapOf(
                "settings-btn" to "scene-settings",
                "profile-btn"  to "scene-profile"
            )
        )

        // Event-driven advance
        card(
            title = "Open the menu",
            text = "Swipe right or tap the rail header.",
            highlight = AzHighlight.Item("rail-header"),
            advanceCondition = AzAdvanceCondition.Event("menu_opened")
        )

        // Checklist card — Next disabled until all items checked
        card(
            title = "Before you continue",
            text = "Confirm the following:",
            checklistItems = listOf("I read the docs", "I set up my account")
        )

        // Media card — rendered between title and text
        card(
            title = "The Rail",
            text = "Sits on the left or right edge.",
            mediaContent = { Image(painterResource(R.drawable.rail), contentDescription = null) }
        )
    }

    scene(id = "scene-basic", content = { BasicScreen() }) {
        card(
            title = "Basic path",
            text = "Here is the simplified view.",
            highlight = AzHighlight.FullScreen,
            advanceCondition = AzAdvanceCondition.TapAnywhere
        )
    }
}

// 2. Register and wire
azAdvanced(
    helpEnabled = true,
    onItemGloballyPositioned = { id, rect -> boundsMap[id] = rect },
    tutorials = mapOf("tut-1" to myTutorial)
)

// 3. Mount the controller and overlay
val controller = rememberAzTutorialController()
CompositionLocalProvider(LocalAzTutorialController provides controller) {
    // ... your content ...
    if (controller.activeTutorialId.value == "tut-1") {
        AzTutorialOverlay(
            tutorialId = "tut-1",
            tutorial = myTutorial,
            onDismiss = { controller.endTutorial() },
            itemBoundsCache = boundsMap
        )
    }
}

// 4. Start with variables (drives the gate scene branch)
controller.startTutorial("tut-1", variables = mapOf("userLevel" to "advanced"))

// 5. Fire an event from your app logic
controller.fireEvent("menu_opened")

// 6. Check read status
val hasRead = controller.isTutorialRead("tut-1")
```

Persistence: `SharedPreferences` file `az_tutorial_prefs`, key `az_navrail_read_tutorials`. State is read on `rememberAzTutorialController()` and written on each `markTutorialRead()`.

### 9.5 React Native — Full Example

```tsx
import {
    AzTutorialProvider,
    useAzTutorialController,
    AzTutorial,
} from '@HereLiesAz/aznavrail-react';

// 1. Define the tutorial
const myTutorial: AzTutorial = {
    onComplete: () => console.log('done'),
    onSkip: () => console.log('skipped'),
    scenes: [
        {
            id: 'gate',
            content: () => null,
            cards: [],
            branchVar: 'userLevel',
            branches: { advanced: 'scene-advanced', basic: 'scene-basic' },
        },
        {
            id: 'scene-advanced',
            content: () => <AdvancedScreen />,
            cards: [
                {
                    title: 'Pick a path',
                    text: 'Tap the item you want to learn about.',
                    highlight: { type: 'Item', id: 'nav-menu' },
                    advanceCondition: { type: 'TapTarget' },
                    branches: {
                        'settings-btn': 'scene-settings',
                        'profile-btn': 'scene-profile',
                    },
                },
                {
                    title: 'Open the menu',
                    text: 'Swipe right or tap the rail header.',
                    highlight: { type: 'Item', id: 'rail-header' },
                    advanceCondition: { type: 'Event', name: 'menu_opened' },
                },
                {
                    title: 'Before you continue',
                    text: 'Confirm the following:',
                    checklistItems: ['I read the docs', 'I set up my account'],
                },
                {
                    title: 'The Rail',
                    text: 'Sits on the left or right edge.',
                    mediaContent: () => <Image source={require('./rail.png')} style={{ height: 120 }} />,
                },
            ],
        },
    ],
};

// 2. Wrap your app root
function Root() {
    return (
        <AzTutorialProvider tutorials={{ 'tut-1': myTutorial }}>
            <App />
        </AzTutorialProvider>
    );
}

// 3. Start and fire events from anywhere in the tree
function TutorialLauncher() {
    const controller = useAzTutorialController();
    return (
        <Button
            title="Start Tutorial"
            onPress={() => controller.startTutorial('tut-1', { userLevel: 'advanced' })}
        />
    );
}

// Fire an event from app logic
controller.fireEvent('menu_opened');
```

Persistence: `@react-native-async-storage/async-storage` (optional peer dependency). Falls back to in-memory if not installed. Key: `az_navrail_read_tutorials`.

### 9.6 Web — Full Example

The web library is a TypeScript port of Android. New files: `web/AzTutorialController.tsx`, `web/AzTutorialOverlay.tsx`, `web/HelpOverlay.tsx`.

```tsx
import {
    AzWebTutorialProvider,
    useAzWebTutorialController,
    AzTutorial,
} from '@HereLiesAz/aznavrail-web';

// Tutorial definition is identical in shape to the React Native example above.

function Root() {
    return (
        <AzWebTutorialProvider tutorials={{ 'tut-1': myTutorial }}>
            <App />
        </AzWebTutorialProvider>
    );
}

function TutorialLauncher() {
    const ctrl = useAzWebTutorialController();
    return (
        <button onClick={() => ctrl.startTutorial('tut-1', { userLevel: 'advanced' })}>
            Start Tutorial
        </button>
    );
}
```

Spotlight implementation: `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)` applied to the highlighted element — the CSS equivalent of Android's `BlendMode.Clear` punch-out.

Persistence: `localStorage`. Key: `az_navrail_read_tutorials`.

### 9.7 Variable Branching

Pass a `variables` map to `startTutorial`. Scenes with `branchVar` set evaluate their `branches` map on entry and redirect to the matching scene ID. A scene used only for branching can have an empty `cards` list and a transparent `content`.

```kotlin
// Android
controller.startTutorial("tut-1", variables = mapOf("userLevel" to "advanced"))
```

```typescript
// React Native / Web
controller.startTutorial('tut-1', { userLevel: 'advanced' });
```

Circular branch detection: if a branch chain loops back to an already-visited scene, a warning is logged and the tutorial advances to the next scene by index. If no next scene exists, the tutorial ends.

### 9.8 Event-Driven Advance

Use `AzAdvanceCondition.Event("event_name")` (Kotlin) or `{ type: 'Event', name: 'event_name' }` (TS) on a card. When your app logic fires that event, the overlay automatically advances.

```kotlin
// Fire from anywhere — e.g., in a real menu open handler
controller.fireEvent("menu_opened")
```

The overlay observes `pendingEvent` and calls `consumeEvent()` internally on match. You do not need to call `consumeEvent()` yourself.

### 9.9 Checklist Cards

Provide `checklistItems` on a card. The Next button is disabled until every item is checked. Compatible with any advance condition (the checklist gates the advance even for `TapAnywhere`).

### 9.10 Media Cards

Provide `mediaContent` (a composable/component) on a card. It is rendered between the title and the body text, clipped to a max height of 120dp/120px with 8dp/8px corner rounding. Useful for images, animated GIFs, or short video previews.

---

## 10. Bottom Sheets

AzNavRail ships a four-detent bottom-sheet shell ported from [LogKitty](https://github.com/HereLiesAz/LogKitty). It is offered in two flavors that share state, theming, and gesture handling, so consumers get identical visual behavior whether the sheet lives inside a normal Activity or floats over the screen from a foreground Service.

### 10.1 The Detent Model

| Detent | Default height | Purpose |
| :--- | :--- | :--- |
| `HIDDEN` | 14dp swipe strip | Sheet is collapsed; the strip is a touch-target for a drag-up gesture but otherwise lets the underlying UI receive touches. |
| `PEEK` | 56dp ticker | Single-line preview of the sheet content. |
| `HALF` | 50% of parent | Half-screen view with a dim scrim above. |
| `FULL` | 90% of parent | Near-full-screen view with the same scrim. |

The fractions and the absolute heights are tunable via `AzSheetConfig`.

### 10.2 In-tree usage

Inside `AzHostActivityLayout` use the `azBottomSheet` DSL. The sheet draws above the rail, the menu, and the `onscreen` content area with `zIndex(2f)`, spans the full screen width edge-to-edge, and extends all the way to the bottom of the screen (no automatic `windowInsetsPadding`) so the HIDDEN-detent strip — 28dp tall by default, with a dimmed drag-handle — is reachable from the system-navigation-bar area. A tap on the strip steps up to PEEK alongside the swipe-up gesture. It is *not* a background. If your sheet body needs to clear the system nav bar visually, pad inside your `content` lambda or use `AzBottomSheetInsetAware` directly outside the DSL.

```kotlin
val sheetController = rememberAzSheetController(initial = AzSheetDetent.PEEK)

AzHostActivityLayout(navController = nav, currentDestination = currentRoute) {
    azConfig(dockingSide = AzDockingSide.LEFT)
    azMenuItem(id = "home", text = "Home", route = "home", onClick = { /* … */ })
    onscreen { AzNavHost(startDestination = "home") { /* … */ } }

    azBottomSheet(controller = sheetController) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Hello sheet")
            Button(onClick = { sheetController.stepUp() }) { Text("Expand") }
        }
    }
}
```

### 10.3 Controller and state

`AzSheetController` carries two channels: a Compose `mutableStateOf`-backed `var` for in-tree consumers, and a `StateFlow` for the system-overlay flavor's window-resize coroutine. Mutate `detent` and `isEnabled` from the main thread; both channels stay in sync.

```kotlin
sheetController.stepUp()                          // HIDDEN → PEEK → HALF → FULL
sheetController.stepDown()                        // reverse
sheetController.snapTo(AzSheetDetent.FULL)         // direct jump
sheetController.isEnabled = false                  // forces HIDDEN, blocks step calls
```

### 10.4 Gestures

- **Swipe up** on the sheet card or hidden strip accumulates per-frame delta and calls `stepUp()` exactly once when `config.dragThresholdDp` is crossed.
- **Swipe down** calls `stepDown()`, descending one detent at a time (`FULL → HALF → PEEK → HIDDEN`) to mirror the swipe-up's one-step expand.
- **Scrim tap** in `HALF` / `FULL` calls `stepDown()` (dim overlay visible).
- **Transparent tap overlay** at `PEEK` — a non-dimmed, full-screen tap catcher that calls `stepDown()`, transitioning to HIDDEN. Makes the dismiss gesture discoverable for users who tap rather than swipe.
- System **back press** calls `stepDown()` while the sheet is non-HIDDEN when `config.collapseOnBack = true`.
- **Horizontal swipe** is opt-in via `config.horizontalSwipeEnabled` and the `onSwipeLeft` / `onSwipeRight` callbacks — LogKitty uses these for tab navigation.

### 10.5 Theming

`AzSheetConfig.backgroundColor` defaults to `MaterialTheme.colorScheme.surface` blended with `backgroundAlpha`. Override both for custom looks; LogKitty wires its user-configurable color + opacity directly through.

### 10.6 System-overlay flavor

For Services that float a sheet over the active foreground app, use `AzBottomSheetWindowHost`. The library ships no `Service` and no permissions; the consumer's Service supplies the lifecycle/savedState owners and declares `SYSTEM_ALERT_WINDOW` itself.

```kotlin
class MyOverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {
    private lateinit var sheetHost: AzBottomSheetWindowHost
    private val controller = AzSheetController(initial = AzSheetDetent.HIDDEN)

    override fun onCreate() {
        super.onCreate()
        sheetHost = AzBottomSheetWindowHost(
            context = this,
            controller = controller,
            config = AzSheetConfig(
                backgroundColor = userBg,
                backgroundAlpha = userAlpha,
            ),
            lifecycleOwner = this,
            viewModelStoreOwner = this,
            savedStateRegistryOwner = this,
            navBarHeightPx = resources.getDimensionPixelSize(
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            ),
        ) { MyContent(controller) }
        sheetHost.attach()
    }

    override fun onDestroy() {
        sheetHost.detach()
        super.onDestroy()
    }
}
```

Call `sheetHost.attachNavBarDecor()` from an accessibility service's `onServiceConnected` to add the secondary `TYPE_ACCESSIBILITY_OVERLAY` window that tints the system nav bar to match the sheet color.

The in-tree flavor animates between detent heights with `animateDpAsState`; the system-overlay flavor hard-jumps via `WindowManager.updateViewLayout`, matching LogKitty's existing look frame-for-frame.

`updateConfig(newConfig)` mutates the live config and — while the sheet is attached at `HIDDEN` or `PEEK` — **immediately resizes the overlay window** to the new `hiddenStripDp` / `peekDp` (the `HALF` / `FULL` detents stay `MATCH_PARENT`). The collector folds `configState` in via `snapshotFlow`, so a config change re-applies the `WindowManager` layout without waiting for the next detent change. This lets a consumer recompute content-driven detent heights or the nav-bar inset on rotation / split-screen without re-attaching.

The overlay also **delivers real window insets to the content**: an `OnApplyWindowInsetsListener` on the host `ComposeView` lets `WindowInsets.navigationBars` / `Modifier.navigationBarsPadding()` resolve to the actual system navigation-bar inset inside the `content` slot, so consumers no longer have to measure the nav bar themselves. The insets are forwarded un-consumed, so the app below still receives them.

**Navigation-mode awareness.** The library detects the device's navigation mode via the `Settings.Secure` `navigation_mode` key (no permission required). Two behaviors follow:

- `AzSheetConfig.drawBehindNavBar` (default `false`): when `true` **and** the device uses button navigation (3-button / 2-button), the sheet draws *behind* the system navigation bar — the exposed height above the bar is unchanged, but the bar is forced see-through so the sheet content shows through it. In the in-tree flavor this sets the host Activity's `navigationBarColor` transparent (and disables contrast enforcement on API 29+), restoring the previous values when the sheet leaves the composition — **below Android 15 only**, since those properties are deprecated in API 35 and ignored for apps targeting SDK 35, where the bar is already transparent under enforced edge-to-edge. In the system-overlay flavor `AzNavBarDecorWindow` paints at a capped semi-transparent alpha (`minOf(backgroundAlpha, 0.5)`) so the sheet window behind it shows through. It is a no-op in gesture navigation.
- **Automatic, no flag:** in gesture navigation `AzHostActivityLayout` imposes **zero** bottom margin on on-screen content (it runs edge-to-edge — there is no button bar to clear). Button-navigation devices keep the usual `max(10% content safe-zone, nav-bar inset)` bottom margin. The rail's own symmetric safe-zone is unaffected.

**Pages (Z-ordering).** `onscreen(alignment, page = 0f)` and `background(weight, page = 0f)` take a `page: Float`. Items sharing a page render on one co-planar layer (positioned with standard Compose `alignment`, so distinct alignments — or your own `Row`/`Column` inside the content — tile without overlapping). Items on *different* pages are stacked in Z and may overlap: a **higher** page number draws **further back**, the lowest page on top. Decimal pages (`1.5f`) insert a layer between existing ones without renumbering. `background()` items form their own book of pages beneath the entire `onscreen` book (itself beneath the rail and nav bar); `weight` breaks ties within a background page, and onscreen pages still respect the safe zones. The system is gated by `AzHostActivityLayout(pagesEnabled = true)` (the default); when on it is forced — items with no explicit page share page `0f`. Set `pagesEnabled = false` to fall back to plain declaration-order rendering (backgrounds by `weight`) with `page` ignored. The React port mirrors this on `<AzOnscreen page={…}>` / `<AzBackground page={…}>` and `pagesEnabled`.

---

### Global loading (`azAdvanced(isLoading = …)`)

Distinct from per-item loading: this draws a screen-centred `AzLoad` **above everything** — rail,
onscreen content, sheets — and swallows input while it is up, because a screen that is loading is
not a screen you can act on. Use `azItemState(id, isLoading = …)` when only one item is busy.

### System overlay (`azSettings(overlayService = …)`)

Supplying an `overlayService` implies `enableRailDragging = true` and makes **undocking hand off to
that service**: the library requests `SYSTEM_ALERT_WINDOW` if needed, then starts it (as a
foreground service when it extends `AzNavRailOverlayService`). `onOverlayDrag` reports `(dx, dy)`
while the overlay is dragged; `onRailDrag` reports the same for in-app FAB dragging. Android only —
on Desktop, Web and iOS undocking stays an in-app floating rail.

## 11. Popups (`AzPopup`)

An `AzPopup` is a window that is **bound to a rail item**, and the two share state in both
directions. Create a controller with `rememberAzPopupController()`, register it in the host DSL with
`azPopup(controller)`, and raise it from anywhere — an item's `onClick`, a coroutine, a callback.

```kotlin
val alerts = rememberAzPopupController()

AzHostActivityLayout(navController = navController) {
    azRailItem(id = "sync", text = "Sync") {
        alerts.show(itemId = "sync", title = "Syncing", message = "Talking to the server…")
    }

    azPopup(alerts) {
        Text(message ?: "")
        AzButton(
            onClick = { item?.setLoading(false); item?.setBadge(null); dismiss() },
            text = "Stop",
        )
    }
}
```

**The shared handle.** The body runs in an `AzPopupScope`, which exposes the request (`kind`,
`title`, `message`, `payload`), a `dismiss()`, and `item` — an `AzPopupItemHandle` on the rail item
that raised the popup. Through it the popup can read the item as the rail currently has it
(`item.item`) and write back to it:

- `setLoading(true)` — spin that item's own loading animation while the popup's work runs
- `setBadge("3")` — drop a badge on it when the work finishes
- `setAlert(AzItemAlert.WARNING)` — flag it
- `clear()` — drop everything the popup pushed, restoring what the DSL declared

Those writes are held on the rail scope, so they survive the DSL re-running on every recomposition
— they last until the popup clears them.

**Which item.** `show(itemId = "sync")` names one explicitly. `show()` with no id binds to the
**last touched** rail item, which is what makes a warning raised from a background job land on
whatever the user just did.

**Notices and warnings mark their item.** While a `AzPopupKind.NOTICE` or `AzPopupKind.WARNING`
popup is up, its bound item is redrawn as a **yellow, rounded-corner triangle outline** — the
warning glyph — and reverts the instant the popup closes. `NOTICE` uses a softer amber, `WARNING` a
saturated hazard yellow. In the drawer, where a row is type rather than a button, the flagged item
takes the same yellow instead. The triangle is also available as an ordinary item shape,
`AzButtonShape.TRIANGLE`.

```kotlin
// No tap to attribute it to — lands on whatever the user last touched.
alerts.show(kind = AzPopupKind.WARNING, title = "Offline", message = "Changes are queued.")
```

`azPopup(controller)` with no body renders the built-in title/message/OK panel;
`azPopup(controller, dismissOnOutsideTap = false)` makes it modal.

## 12. Windows (`AzWindow`)

Every panel this library floats over an app is a **window**, and they all behave the same way. An
`AzPopup` is drawn in one. So is the **hidden menu** a reloc item raises. And you can raise one
yourself for anything else.

```kotlin
val panel = rememberAzWindowState()

AzWindow(title = "Layers", state = panel, onDismiss = { showPanel = false }) {
    Column(Modifier.padding(16.dp)) { /* anything */ }
}
```

**It moves.** Drag the bar across the top and the window follows your finger, clamped so that a
title-bar's worth always stays on screen — a window you can lose is a window you have to re-open. A
panel that lands on top of the very thing it is asking about is otherwise a dead end, and the user's
only way out of it is to dismiss the panel and lose whatever they had typed into it.

**It folds.** The bar's fold control collapses the window to just that bar: still where the user left
it, still one tap from coming back, with the screen behind it visible again. That is the difference
between getting a panel out of the way and having to throw it away and summon it a second time.

**It closes**, when whoever raised it gave it a way to (`onDismiss`).

| Parameter | Meaning |
| --- | --- |
| `title` | Shown in the bar. Blank draws a bare bar — right when the body already has a heading. |
| `state` | `AzWindowState`: `offsetX`/`offsetY`/`minimized`, plus `resetPosition()`. Hoist it when placement or folded state has to outlive the window. |
| `accent` | Border and chrome colour. Defaults to the rail's accent, so a window matches the rail that raised it. |
| `surfaceColor` | Panel fill. Defaults to the theme surface. |
| `movable` / `minimizable` | Drop either affordance for a window that genuinely shouldn't have it. |
| `onDismiss` | Close handler; null draws no close control. |

**The hidden menu is one of these.** `hiddenMenu { … }` on a reloc item opens in a window rather than
a bare popup, which matters most for the menu that can hold a text field: typing into a box you
cannot move off the thing you are typing about is a poor arrangement. It opens beside the item that
raised it, and from there it is the user's to move, fold, or close. Tapping outside it still
dismisses it.
