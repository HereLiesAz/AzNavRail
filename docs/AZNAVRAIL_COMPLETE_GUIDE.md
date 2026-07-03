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
    usePhysicalDocking = usePhysicalDocking // Boolean: Anchor to physical hardware edge vs visual left
    // appRepositoryUrl = ""                // Optional override for the About reader's repo (see below)
)
```

`azConfig` also takes `appRepositoryUrl` (default `""`), the repo the in-app **About** reader uses.
On **Android** the repo is auto-derived from the app **namespace** — `com.<owner>.<repo>` →
`https://github.com/<owner>/<repo>` (owner = 2nd segment, repo = last segment; a trailing build
suffix like `.debug` is stripped) — so `appRepositoryUrl` is an **optional** override and it **never**
falls back to the AzNavRail library repo. On **web** there is no package namespace, so
`appRepositoryUrl` is **required** there (no auto-derivation); when unset the About entry is hidden.
While a footer screen (About or More from Az) is open, visible Help cards and any guidance callouts
are hidden and restore exactly where they were on close (all platforms).

**About page layout.** The About screen is split into two vertically-stacked halves:

- **Top half** — auto-generated table of contents of the app's markdown docs (`.md` files in the
  repo root and `docs/`). Selecting a row swaps in an inline reader for that document.
- **Bottom half** — a **focused-hero More-from-Az carousel** with a size pattern
  `small · medium · LARGE · medium · small`. The LARGE (center) item is the currently active app;
  its **banner** (when the repo has `docs/banner.png` / `.webp` / `.jpg` — or `docs/hero.*`),
  name, description, and link buttons (Play / Website / GitHub) render below the carousel.

**Icons & banners** are baked into `more-from-az.json` by CI (Play `og:image` → website `og:image` →
Android launcher icons on `raw.githubusercontent.com` → repo social preview). If the manifest is
stale or `iconUrl` is blank at runtime, the runtime performs the same launcher-icon walk itself so
new apps show a proper icon before the next CI bake.


### B. Theming (`azTheme`)
Controls visual style defaults.

```kotlin
azTheme(
    defaultShape = AzButtonShape.RECTANGLE, // Default shape for all items
    activeColor = MaterialTheme.colorScheme.primary, // Color for active state
    translucentBackground = Color.Black.copy(alpha = 0.5f), // Set the background color for menus/overlays!
    headerIconSize = 48.dp                  // Exact app-icon diameter (Dp.Unspecified = size to rail width)
)
```

**React Implementation:**
```tsx
const settings: AzNavRailSettings = {
    defaultShape: AzButtonShape.RECTANGLE,
    activeColor: '#6200EE',
    translucentBackground: 'rgba(0,0,0,0.5)',
};
// Pass this object to the settings prop on AzNavRail
```

### B2. Kinetic Typography (`azKinetics`)

Windows-Phone-7-style motion for the menu words: a staggered **turnstile** entrance/exit on the
expanded menu items, a 3D **tilt-on-press**, and the big **screen title's** sweep on navigation. It is
config-driven (preset enums, no free-composable escape hatch). Defaults animate; pass `AzEntrance.None`
/ `AzExit.None` to opt a surface out. In **FAB / floating** mode the cascade becomes a vertical up/down
slide (no docked edge to hinge on).

The signature look is a **pure 90° `rotationY` sweep** hinged on the docked edge — no fade, no vertical
slide. Items overlap heavily by default: `entranceDurationMs = 720` and `entranceStaggerMs = 60`
means the next item begins ~60 ms after the previous one *begins* while the previous one is still
animating. The footer (About / Feedback / @HereLiesAz) then **unfolds like an accordion** from the
top edge, starting the moment the last item begins — so the whole rail-open motion completes in one
continuous beat.

```kotlin
azKinetics(
    itemEntrance = AzEntrance.Turnstile,   // None | Fade | SlideUp | Turnstile  (default Turnstile)
    itemExit = AzExit.Turnstile,           // None | Fade | Turnstile            (default Turnstile)
    entranceStartAngle = 90f,              // pure edge-on → flat  (default 90)
    entranceDurationMs = 720,              // per-item duration ms (default 720)
    entranceStaggerMs = 60,                // per-item stagger ms  (default 60 → items overlap)
    tiltOnPress = true,                    // off by default on the rail (drag-safe)
    itemTextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Light),
    titleEntrance = AzEntrance.Turnstile,  // the big AzNavHost screen title
)
```

The standalone `AzDropdownMenu` exposes the same item knobs on its own `azConfig` (also on by
default), and its app-icon trigger carries an automatic margin.

**React Implementation:** the rail reads kinetics from `settings`
(`itemEntrance`, `itemExit`, `titleEntrance`, `tiltOnPress`, `itemTextStyle`, …); `AzDropdownMenu`
takes matching props. `AzEasing.Wp7Decelerate` is the signature easing. On **native React Native**
the hinge is emulated via a `translateX ±(width/2)` pivot correction around the rotation because
`transformOrigin` is silently ignored there; on web the CSS `transform-origin: left/right center`
does it natively.

### B.1 Menu-drawer look-and-feel (dim, side-alignment, kerning-justify)

Three developer knobs on `azConfig` shape the drawer itself. They only affect the **expanded-menu
drawer** (and the standalone `AzDropdownMenu`'s panel) — small rail-button labels are unaffected.

```kotlin
azConfig(
    dimBehindMenu = true,                              // opt-in dim scrim; default false
    dimBehindMenuAlpha = 0.4f,                         // 0..1; default 0.4
    menuItemAlignment = AzMenuItemAlignment.SIDE,      // SIDE (default) | CENTER
    justifyMenuItems = true,                           // full-justify labels via letter-spacing; default true
)
```

`SIDE` alignment: labels use `TextAlign.Start` when docked LEFT and `TextAlign.End` when docked
RIGHT. `justifyMenuItems` measures each label's natural width, computes the extra pixels needed to
fill the row, and applies it as `letterSpacing` — a Word-style full justification. Labels shorter
than 2 characters, or already at/past the row width, are skipped.

**React Implementation:** identical fields on `AzNavRailSettings` and on the `AzDropdownMenu` props:
`dimBehindMenu`, `dimBehindMenuAlpha`, `menuItemAlignment: 'center' | 'side'`, `justifyMenuItems`.
Same defaults (`side`, `true`, opt-in dim at `0.4`).

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


### D. Drop-down menu — `AzDropdownMenu` (standalone)

A hamburger drop-down is **not** a rail mode — it is a standalone widget, `AzDropdownMenu`, declared
with the **same opinionated DSL as the rail**. In AzNavRail tradition it accepts only the
configuration the rest of the library sanctions (no arbitrary panel background, offsets, icon
tint/source, or free composable escape hatch). Its trigger is the **app icon** (auto-drawn like the
rail's header; its shape/size set via `azConfig`'s `headerIconShape`/`headerIconSize`), dropped
inline like any widget. Tapping it unfolds an **overlay
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
    onClick = { /* log click */ }
)

// Multi-line text support
azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line")

// Help trigger rail item
azHelpRailItem(id = "help-trigger", text = "Get Help")

// Help trigger as a sub-item
azHelpSubItem(id = "help-sub-trigger", hostId = "rail-host", text = "Get Help Here")

// Rail item with Color content
azRailItem(id = "color-item", text = "Color", content = Color.Red)

// Rail item with Icon Resource
azRailItem(id = "icon-item", text = "Icon", content = android.R.drawable.ic_menu_agenda)

// Rail item with a Compose ImageVector (fills + clips to the shape, tinted with the item color)
azRailItem(id = "vector-item", text = "Delete", content = Icons.Default.Delete)

// Rail item with specific shape override
azRailItem(id = "none-shape", text = "No Shape", shape = AzButtonShape.NONE)

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
// Per-item help text is mapped through helpList in React
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
// Auto-expand the "features" host while a guidance goal is active
azRailHostItem(
    id = "features",
    text = "Features",
    expandWhen = { guidance.activeGoals.contains("onboarding") }
)
azRailSubItem(id = "feature-a", hostId = "features", text = "Feature A")
```

This is particularly useful with the guidance framework: a guidance edge that highlights
"feature-a" (via `highlightItemId`) requires "feature-a" to be laid out so its bounds are known.
Without `expandWhen` a collapsed host hides its children from layout, so the callout can't anchor
to the item.

`expandWhen` and `initiallyExpanded` coexist: `initiallyExpanded` fires once on first
appearance; `expandWhen` fires on every subsequent edge transition.

The React/web equivalent is the `expandWhen` prop on `<AzRailHostItem>`:

```tsx
<AzRailHostItem
  id="features"
  text="Features"
  expandWhen={() => guidance.activeGoals.includes('onboarding')}
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


## 9. Status-Driven Guidance Framework

The guidance framework **replaces the old scripted scene/card tutorial framework**. Instead of
authoring a linear walkthrough, you describe your app's userflow as a **flowchart of statuses**
(string-id nodes) connected by **edges** (transitions that each carry an instruction). You declare
**goals** (target statuses) and activate them; the engine then shows the instruction for the next
status toward each active goal, **auto-advancing the instant a target status becomes true** (there is
no Next button), re-routing live as the user's state changes, and showing **every active goal's
instruction simultaneously** as a callout next to its control.

### 9.1 Concepts

**Status** — a named node in the flowchart, identified by a string id, defined by a reactive
predicate. When the predicate is true the status is "true." Statuses are the vertices of the graph.

**Edge** — a directed transition from one status to another (or into a status from anywhere),
carrying the **instruction text** shown to the user to make that transition. Edges are the
graph's edges. Developers hand-author edges only into their **custom** statuses; edges for rail
affordances are generated automatically (see Auto-edges).

**Goal** — a target status the developer wants the user to reach, plus an optional label. A goal is
inert until **activated**; while active, the engine routes from the user's current state toward the
goal's target and surfaces the next-hop instruction. The moment the target status becomes true the
goal completes (and is recorded as completed in persistent storage).

**Built-in `az.*` statuses** (auto-published by the rail, always available as graph nodes):

| Status | True when |
| :--- | :--- |
| `az.app.ready` | always true (a convenient source node) |
| `az.rail.expanded` / `az.rail.collapsed` | the rail is expanded / collapsed |
| `az.rail.floating` | the rail is in FAB / floating mode |
| `az.host.<id>.expanded` | the host item `<id>` is expanded |
| `az.screen.<route>` | the current route is `<route>` |
| `az.item.<id>.active` | the item `<id>` is the active/selected item |
| `az.nestedRail.<id>.open` | the nested rail `<id>` is open |
| `az.help.open` | the help overlay is open |
| `az.onscreen.<id>.visible` | the onscreen content `<id>` is visible |

**Auto-edges** (generated for rail affordances — you never hand-author these):

| Auto-edge | Instruction |
| :--- | :--- |
| `az.rail.collapsed → az.rail.expanded` | "Open the menu" |
| tap host → `az.host.<id>.expanded` | (generated) |
| tap nested-rail → `az.nestedRail.<id>.open` | (generated) |
| tap routed item → `az.screen.<route>` | (generated) |

Developers only hand-author `azEdge` entries that lead **into custom statuses** they define.

### 9.2 How advancement works (read this before building your own coach)

This framework is the supported way to walk a user through the rail. **Do not roll your own
overlay that waits for taps on rail items to "fall through" to the canvas — they never will.**
The rail consumes its own pointer events by design (a tap on a nav control must not leak to the
content behind it). The guidance engine does **not** depend on that leak — it never intercepts
taps at all. Instead it observes **status predicates** and reacts:

- **No Next button.** Advancement is driven entirely by status truth. When the user does whatever
  the current instruction asks (opens the menu, expands a host, navigates to a screen, satisfies a
  custom predicate), the next status toward the goal becomes true and the engine advances
  automatically. The instant a goal's **target** status becomes true, the goal completes.
- **Live re-routing.** If the user's state changes in a way that makes a different next-hop optimal
  (or makes the current instruction obsolete), the engine recomputes the route on the fly and shows
  the new instruction.
- **Every active goal at once.** If several goals are active, each shows its own instruction
  simultaneously as a callout next to the control that satisfies its next hop.

**Presentation is non-blocking and never dims.** Guidance draws a thin **outline** around the step's
target and a small **callout placed near (never on) that target**, with a **pointer/arrow** connecting
them. It **never darkens the screen** and **never intercepts input outside a callout** — the app stays
fully usable while guidance is up, so the user can actually perform the action the step teaches.
Callouts are positioned to avoid covering the target, other known UI (rail items / registered targets),
each other, and the screen's safe-area edges; several callouts can share the screen, each by its own
target. A step that has been shown and acted on is **never shown again**, even if the user later undoes
the action and the router would otherwise re-route to it.

**Skipping & replay.** The user can **swipe a callout away to cancel tutorial mode** — that skip is
persisted (`SharedPreferences` / `localStorage`), so a skipped tutorial is **not** shown again. To let
it run again, call `controller.resetGuidance(goalId)` (or `resetGuidance()` for all). Completed and
skipped goals are both ignored by `activate()` until reset.

**Predicate observation timing.** Predicates that read Compose/React state (a `mutableStateOf`,
a React state value, a derived value) are observed **instantly**. Predicates that read
**non-reactive** sources — a `StateFlow.value`, a plain `var`, a `ref`, an external store — are
observed within a **~300 ms poll**, so the callout for such a status may lag up to a poll interval.

**Highlighting the control for a custom edge.** An `azEdge` that should point at a specific rail
control passes `highlightItemId`; the callout anchors next to that item's measured bounds. As with
any item-anchored UI, the item must be laid out — if it lives inside a collapsed host, use the host's
`expandWhen` (see §4) so the item's bounds are known when the callout needs to anchor.

**Goal activation is developer-driven.** There is no built-in end-user picker — **starting is always
your decision**. You activate goals imperatively through the `AzGuidanceController` (`activate(id)`).
`autoStartWhen` (a status id that self-activates a goal) still exists but is **discouraged**; prefer an
explicit `activate(...)`. Either way, a goal that the user has **completed or skipped never (re)starts**
until you call `resetGuidance(...)`.

### 9.3 Help/Info Overlay Integration

Guidance is **developer-activated**, not launched from the help overlay. The old help-overlay "Start
Tutorial" launch affordance has been **removed**. Help cards and guidance callouts coexist: while a
footer screen (About or More from Az) is open, both are hidden and restore exactly where they were on
close (all platforms).

### 9.4 Android — Full Example

The DSL functions live alongside the rest of the rail DSL inside `AzHostActivityLayout { ... }`:

- `azStatus(id) { predicate }` — declare a custom status node.
- `azEdge(from, to = null, text, title = null, highlightItemId = null)` — declare a transition
  carrying instruction `text`; `to = null` means "into this status from anywhere", `highlightItemId`
  anchors the callout to a rail item.
- `azGoal(id, target, label = null, autoStartWhen = null)` — declare a goal targeting status
  `target`; `autoStartWhen` self-activates the goal when its condition becomes true.

```kotlin
import com.hereliesaz.aznavrail.tutorial.*

val controller = AzHostActivityLayout(
    navController = navController,
    currentDestination = currentRoute,
) {
    // … your rail items, onscreen, etc. …

    azRailHostItem(id = "features", text = "Features")
    azRailSubItem(id = "feature-a", hostId = "features", text = "Feature A", route = "feature-a")

    // 1. Custom status node: the predicate defines when it is "true".
    azStatus("profileComplete") { viewModel.profile.isComplete }

    // 2. Hand-author edges INTO custom statuses (rail affordances are auto-edged).
    azEdge(
        from = "az.screen.feature-a",
        to = "profileComplete",
        text = "Fill in your name and email to finish setup.",
        title = "Complete your profile",
        highlightItemId = "feature-a"
    )

    // 3. Declare goals. autoStartWhen is a STATUS ID (not a lambda): the goal self-activates once that
    //    status becomes true, and only if it hasn't already been completed (completion is tracked for you).
    azGoal(
        id = "onboarding",
        target = "profileComplete",
        label = "Finish onboarding",
        autoStartWhen = "az.app.ready"
    )
    azGoal(id = "find-features", target = "az.host.features.expanded", label = "Discover Features")
}

// AzHostActivityLayout RETURNS the controller; it is also available as
// LocalAzGuidanceController.current anywhere inside the layout.

// Activate / deactivate goals imperatively from your app logic:
controller.activate("find-features")
controller.deactivate("find-features")

// Inspect state:
val active: List<String> = controller.activeGoals
val done: List<String> = controller.completedGoals
val finished = controller.isCompleted("onboarding")

// Mark a status reached manually (e.g. from an external success callback):
controller.markReached("profileComplete")

// Skip (usually driven by the swipe-to-cancel gesture) and replay:
controller.skip("onboarding")      // dismiss one goal (persisted)
controller.skip()                  // cancel tutorial mode: skip all active goals + disable
controller.resetGuidance("onboarding") // clear completion + dismissal so it can run again
controller.resetGuidance()         // clear all

// Turn the whole framework on/off:
controller.disable()
controller.enable()
```

You can also obtain a controller via `rememberAzGuidanceController()` and read it through
`LocalAzGuidanceController.current`. `AzGuidanceController` lives in package
`com.hereliesaz.aznavrail.tutorial` and exposes: `enabled`, `activeGoals`, `completedGoals`,
`dismissedGoals`, `enable()`, `disable()`, `activate(id)`, `deactivate(id)`, `markReached(id)`,
`isCompleted(id)`, `isDismissed(id)`, `skip(id?)`, `resetGuidance(id?)`, and (for paged edges)
`advance`/`next`/`back` plus the observable `currentInstructions`/`current`/`currentFlow`.

Persistence: completed goals persist in `SharedPreferences` under key
`az_navrail_completed_goals`.

### 9.5 React Native / Web — Full Example

The DSL is expressed as JSX children of the rail, and the controller is read with a hook. Method and
property names match Android (`boolean` / `string[]` in place of Kotlin types):

```tsx
import {
    AzGuidanceProvider,
    useAzGuidanceController,
    AzStatus,
    AzEdge,
    AzGoal,
} from '@HereLiesAz/aznavrail-react';

function AppRail() {
    return (
        <AzNavRail /* … */>
            <AzRailHostItem id="features" text="Features" />
            <AzRailSubItem id="feature-a" hostId="features" text="Feature A" route="feature-a" />

            {/* 1. Custom status */}
            <AzStatus id="profileComplete" predicate={() => viewModel.profile.isComplete} />

            {/* 2. Edge into the custom status (rail affordances are auto-edged) */}
            <AzEdge
                from="az.screen.feature-a"
                to="profileComplete"
                text="Fill in your name and email to finish setup."
                title="Complete your profile"
                highlightItemId="feature-a"
            />

            {/* 3. Goals; autoStartWhen is a STATUS ID (not a function) that self-activates onboarding once */}
            <AzGoal
                id="onboarding"
                target="profileComplete"
                label="Finish onboarding"
                autoStartWhen="az.app.ready"
            />
            <AzGoal id="find-features" target="az.host.features.expanded" label="Discover Features" />
        </AzNavRail>
    );
}

function GuidanceButtons() {
    const ctrl = useAzGuidanceController();
    const active: string[] = ctrl.activeGoals;
    const done: string[] = ctrl.completedGoals;
    return (
        <>
            <Button title="Show features tour" onPress={() => ctrl.activate('find-features')} />
            <Button title="Stop" onPress={() => ctrl.deactivate('find-features')} />
        </>
    );
}
```

The controller exposes the same surface as Android: `enabled`, `activeGoals`, `completedGoals`,
`enable()`, `disable()`, `activate(id)`, `deactivate(id)`, `markReached(id)`, `isCompleted(id)`.

Also exported for advanced use: `AzGuidanceProvider`, `AzInstructionOverlay`, `useActiveStatuses`,
`computeBuiltinStatuses`, `nextHop`, `routeInstructions`, `computeAutoEdges`.

Persistence: completed goals persist to `localStorage` (and `AsyncStorage` on React Native) under key
`az_navrail_completed_goals`.

**Platform parity note:** neither platform dims the screen. Android strokes the target's true geometry
(circle/rect/path) and draws an arrowhead to the callout; React rings the target's bounding box and
draws a plain connector line (per-shape masking / arrowheads aren't portable on React Native). Routing,
placement, and advancement behave identically.

### 9.6 Highlighting arbitrary on-screen targets (`azGuidanceTarget`)

`highlightItemId` points at a **rail item**. To spotlight **arbitrary on-screen content that is not a
rail item** — a ball drawn over a camera/AR canvas, an aiming line, an on-screen slider — register a
**guidance target**: an id mapped to a function returning the current shape in **window-space px**
(circle, rounded-rect, or path), recomputed every frame so the spotlight tracks a moving object. Then
reference it from an edge (or a step) with `highlightTargetId`. If the function returns `null` (target
absent), the callout gracefully degrades to text-only.

The library both **draws a non-blocking outline** around the shape (circle/rect/path) **and** publishes
the resolved shape on the controller (see §9.8) so a host can draw its own highlight. It never dims or
covers the target.

```kotlin
// Window-space shape, recomputed each frame (read Compose state inside for smooth tracking).
azGuidanceTarget("target.ball") {
    val b = ballBounds.value ?: return@azGuidanceTarget null   // null ⇒ text-only
    AzGuideShape.Circle(b.center.x, b.center.y, b.minDimension / 2f, padding = 8f)
}
azEdge(from = "az.app.ready", to = "ball.aimed", text = "Aim at the ball", highlightTargetId = "target.ball")
```

`AzGuideShape` is `Circle(cx, cy, radius, padding)`, `Rect(left, top, width, height, cornerRadius,
padding)`, or `Path(commands, padding)` where `commands` are absolute `AzPathCmd` (`MoveTo`/`LineTo`/
`QuadTo`/`CubicTo`/`Close`). React mirrors this as `{ type: 'Circle' | 'Rect' | 'Path', … }`:

```tsx
<AzGuidanceTarget id="target.ball" shape={() => ball ? { type: 'Circle', cx: ball.x, cy: ball.y, radius: ball.r, padding: 8 } : null} />
<AzEdge from="az.app.ready" to="ball.aimed" text="Aim at the ball" highlightTargetId="target.ball" />
```

You can also point at the **currently-active rail item** without naming a static id by passing the
`az.item.active` token (`AZ_ITEM_ACTIVE`) as `highlightItemId`, or resolve a runtime item id every frame
with `highlightSelector = { viewModel.activeLayerId }` (React: `highlightSelector={() => activeLayerId}`)
— useful for dynamically-created rail items (e.g. `layer.<uuid>`).

### 9.7 Paged steps & manual advance

A single edge can carry an ordered list of **steps** — several sub-pointers under one milestone,
revealed one at a time, moving the spotlight as the user reads. A step with no `advanceWhen` is
**informational**: its callout shows a *“Tap to continue”* affordance and advances on tap. A step with
`advanceWhen = "<statusId>"` is **actionable**: it auto-advances the instant that status becomes true
(reactive advance always wins over the tap cursor). The whole edge still completes when its `to`
status becomes true — so a single goal can mix “read this” steps with “now do this” steps.

```kotlin
azGuidanceTarget("target.ball") { ballShape() }
azStatus("ball.dragged") { gesture.value.dragged }
azEdge(
    from = "az.app.ready",
    to = "ball.dragged",
    title = "Meet the coach",
    steps = listOf(
        AzInstructionStep("This is a protractor."),                                  // info: tap to advance
        AzInstructionStep("This handle sets the angle.", highlightTargetId = "target.ball"), // info on a moving target
        AzInstructionStep("Drag the handle to rotate.", highlightTargetId = "target.ball",
                          advanceWhen = "ball.dragged"),                              // actionable: reactive
    ),
)
azGoal(id = "learnProtractor", target = "ball.dragged", autoStartWhen = "az.app.ready")
```

```tsx
<AzGuidanceTarget id="target.ball" shape={ballShape} />
<AzStatus id="ball.dragged" predicate={() => gesture.dragged} />
<AzEdge from="az.app.ready" to="ball.dragged" title="Meet the coach" steps={[
    { text: 'This is a protractor.' },
    { text: 'This handle sets the angle.', highlightTargetId: 'target.ball' },
    { text: 'Drag the handle to rotate.', highlightTargetId: 'target.ball', advanceWhen: 'ball.dragged' },
]} />
<AzGoal id="learnProtractor" target="ball.dragged" autoStartWhen="az.app.ready" />
```

`AzInstructionStep(text, title?, highlightItemId?, highlightTargetId?, side?, highlightSelector?,
advanceWhen?)` — each step can spotlight its own target. To drive paging yourself (e.g. a host “Next”
button), the controller exposes `advance(stepKey)` / `next(stepKey)` / `back(stepKey)` (and a no-arg
`advance()` that advances the first active paged instruction); read the `stepKey` from the snapshot
(§9.8). Step cursors are transient — only **goal completion** persists.

### 9.8 Observing the current instruction

The controller publishes the callouts it is currently showing, so a host can mirror them with bespoke
rendering (e.g. a pulsing highlight drawn over its own canvas) and analytics. The framework can show
several at once (one per active goal), so it is a list with a singular convenience.

```kotlin
val snap = controller.current                  // AzGuidanceSnapshot? (the primary one)
val all = controller.currentInstructions       // List<AzGuidanceSnapshot>
// Or observe reactively (non-Compose): controller.currentFlow: StateFlow<List<AzGuidanceSnapshot>>
// Inside composition: LocalAzGuidanceController.current?.currentInstructions
```

`AzGuidanceSnapshot` carries `text`, `title`, `goalId`, `highlight`, `targetId`, `resolvedShape` /
`resolvedBounds` (window-space, when available), `stepIndex` / `stepTotal`, and `stepKey`. React mirrors
this on the controller from `useAzGuidanceController()` (`current`, `currentInstructions`).

### 9.9 Suppressing guidance during gestures

Drive suppression from your own gesture state so a callout never pops over an in-progress pinch/drag.
While the predicate is true the overlay hides; when it flips back to false, guidance re-shows after a
configurable **settle delay** (default ~700 ms). Several suppressors compose (OR); the largest settle
wins.

```kotlin
azSuppressGuide(settleMs = 700) { gesture.inProgress }
```

```tsx
<AzSuppressGuide predicate={() => gesture.inProgress} settleMs={700} />
```

### 9.10 Custom callout rendering (optional)

To draw the callout body yourself (over a camera/canvas), register a renderer; the outline + connector
still draw, but the callout content is yours. It receives the current snapshot and the resolved target
bounds.

```kotlin
azGuideRenderer { snapshot, bounds -> MyCallout(snapshot, bounds) }
```

```tsx
<AzGuideRenderer render={(snapshot, bounds) => <MyCallout snapshot={snapshot} bounds={bounds} />} />
```

### 9.11 Migration from the old scripted tutorial framework

The scripted scene/card framework was removed. Map old constructs to the new ones:

| Old (scripted tutorial) | New (status-driven guidance) |
| :--- | :--- |
| `AzTutorial` / `scene(...)` / `card(...)` | `azStatus` / `azEdge` / `azGoal` + the guidance engine |
| `AzTutorialController.startTutorial(id)` | `AzGuidanceController.activate(id)` |
| `AzTutorialController.markTutorialRead(id)` | `AzGuidanceController.markReached(id)` |
| `azAdvanced(tutorials = ...)` config | **removed** — declare `azStatus`/`azEdge`/`azGoal` instead |
| Help-overlay "Start Tutorial" launch | **removed** — guidance is developer-activated, not launched from help |
| Persistence key `az_navrail_read_tutorials` | `az_navrail_completed_goals` |

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

- `AzSheetConfig.drawBehindNavBar` (default `false`): when `true` **and** the device uses button navigation (3-button / 2-button), the sheet draws *behind* the system navigation bar — the exposed height above the bar is unchanged, but the bar is forced see-through so the sheet content shows through it. In the in-tree flavor this sets the host Activity's `navigationBarColor` transparent (and disables contrast enforcement on API 29+), restoring the previous values when the sheet leaves the composition; in the system-overlay flavor `AzNavBarDecorWindow` paints at a capped semi-transparent alpha (`minOf(backgroundAlpha, 0.5)`) so the sheet window behind it shows through. It is a no-op in gesture navigation.
- **Automatic, no flag:** in gesture navigation `AzHostActivityLayout` imposes **zero** bottom margin on on-screen content (it runs edge-to-edge — there is no button bar to clear). Button-navigation devices keep the usual `max(10% content safe-zone, nav-bar inset)` bottom margin. The rail's own symmetric safe-zone is unaffected.

**Pages (Z-ordering).** `onscreen(alignment, page = 0f)` and `background(weight, page = 0f)` take a `page: Float`. Items sharing a page render on one co-planar layer (positioned with standard Compose `alignment`, so distinct alignments — or your own `Row`/`Column` inside the content — tile without overlapping). Items on *different* pages are stacked in Z and may overlap: a **higher** page number draws **further back**, the lowest page on top. Decimal pages (`1.5f`) insert a layer between existing ones without renumbering. `background()` items form their own book of pages beneath the entire `onscreen` book (itself beneath the rail and nav bar); `weight` breaks ties within a background page, and onscreen pages still respect the safe zones. The system is gated by `AzHostActivityLayout(pagesEnabled = true)` (the default); when on it is forced — items with no explicit page share page `0f`. Set `pagesEnabled = false` to fall back to plain declaration-order rendering (backgrounds by `weight`) with `page` ignored. The React port mirrors this on `<AzOnscreen page={…}>` / `<AzBackground page={…}>` and `pagesEnabled`.

### 10.7 LogKitty migration

LogKitty currently maintains its own `SheetController`, `LogBottomSheet`, and nav-bar-decoration code inside `LogKittyOverlayService`. To replace them with AzNavRail's shell:

1. Add the `aznavrail` dependency.
2. Replace `SheetController` and `LogBottomSheet` with `AzSheetController` and `AzBottomSheetWindowHost` (see snippet above).
3. Pass LogKitty's existing tabs / log-list composable as the `content` slot.
4. Delete `LogBottomSheet.kt`, `SheetController.kt`, and the inline nav-bar decoration block.

Visual behavior — detent heights, drag feel, scrim, animation timing, and nav-bar color sync — is preserved frame-for-frame because `AzBottomSheetWindowHost` ports the same `WindowManager` flag set, the same accumulated-delta gesture, and the same nav-bar decoration window verbatim.
