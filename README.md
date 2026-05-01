# AzNavRail

[![](https://jitpack.io/v/HereLiesAz/AzNavRail.svg)](https://jitpack.io/#HereLiesAz/AzNavRail)

A contemptably stubborn if not dictatorially restrictive navigation rail/menu--I call it a renu. Or maybe a mail. No, a navigrenuail--for Jetpack Compose with a streamlined, DSL-style API.

This "navigrenuail" provides a vertical navigation rail that expands to a full menu drawer. It is designed to be "batteries-included," providing common behaviors and features out-of-the-box to ensure a consistent look and feel across applications.

---

## 📚 Documentation

-   **[Complete Guide (The Bible)](/docs/AZNAVRAIL_COMPLETE_GUIDE.md)**: Comprehensive, encyclopedic reference for every feature.
-   **[API Reference](/docs/API.md)**: Javadoc-style listing.
-   **[DSL Reference](/docs/DSL.md)**: Quick look at DSL functions.

---

## 🚀 Setup

Add JitPack to your `settings.gradle.kts`:
## Features

- **Responsive Layout**: Automatically adjusts to orientation changes.
- **Scrollable**: Both rail and menu are scrollable.
- **DSL API**: Simple, declarative API.
- **Multi-line Items**: Supports multi-line text.
- **Stateless**: Hoist and manage state yourself.
- **Shapes**: `CIRCLE`, `SQUARE`, `RECTANGLE`, or `NONE`. `RECTANGLE`/`NONE` auto-size width (fixed 36dp height).
- **Smart Collapse**: Items collapse the rail after interaction.
- **Delayed Cycler**: Built-in delay prevents accidental triggers.
- **Custom Colors**: Apply custom colors to buttons.
- **Dividers**: Add menu dividers.
- **Automatic Header**: Displays app icon or name.
- **Layout**: Pack buttons or preserve spacing.
- **Disabled State**: Disable items or options.
- **Loading State**: Built-in loading animation.
- **Standalone Components**: `AzButton`, `AzToggle`, `AzCycler`, `AzDivider`, `AzRoller`.
- **Navigation**: seamless Jetpack Navigation integration.
- **Hierarchy**: Nested menus with host and sub-items.
- **Draggable (FAB Mode)**: Detach and move the rail.
- **Reorderable Items**: `AzRailRelocItem` allows user drag-and-drop reordering within clusters.
    -   **Drag**: Long press (with vibration feedback) to start dragging.
    -   **Hidden Menu**: Tap to focus/select. If already focused, tap again to open the hidden menu.
- **System Overlay**: System-wide overlay support with automatic resizing and activity launching.
- **Auto-sizing Text**: Text fits without wrapping (unless explicit newline).
- **Toggles/Cyclers**: Simple state management.
- **Gestures**: Swipe/tap to expand, collapse, or undock.
- **`AzTextBox`**: Modern text box with autocomplete and submit button.
- **`AzForm`**: Group multiple text boxes into a single form with a shared submit button.
- **`AzRoller`**: A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
- **Info Screen**: Interactive help mode for onboarding with visual guides and coordinate display.
- **Tutorial Framework**: Scripted multi-scene tutorials with spotlights, 4 advance conditions (Button, TapTarget, TapAnywhere, Event), variable-driven branching, TapTarget branching, checklist cards, media cards, and cross-platform read-state persistence.
- **Left/Right Docking**: Position the rail on the left or right side of the screen.
- **No Menu Mode**: Treat all items as rail items, removing the side drawer.
- **AzHostActivityLayout**: A layout container that enforces strict safe zones and automatic alignment rules.
- **AzNavHost**: A wrapper around `androidx.navigation.compose.NavHost` for seamless integration.
- **Smart Transitions**: `AzNavHost` automatically configures directional transitions (slide in/out) based on the docking side (e.g., standard LTR or mirrored for Right dock).
- **Nested Rails**: `azNestedRail` allows for secondary popup rails (Vertical or Horizontal) triggered from a rail item.

## AzNavRail for Android (Jetpack Compose)

### Setup

To use this library, add JitPack to your `settings.gradle.kts`:

~~~kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
~~~

Add the dependency to your app's `build.gradle.kts`:

~~~kotlin
dependencies {
    implementation("com.github.HereLiesAz:AzNavRail:VERSION") // Replace VERSION with the latest release
}
~~~

---

## 🛠️ The Golden Sample

This is the standard, validated way to initialize `AzNavRail`. It **must** be wrapped in `AzHostActivityLayout`.

~~~kotlin
import com.hereliesaz.aznavrail.*

@Composable
fun SampleScreen() {
    val navController = rememberNavController()

    AzHostActivityLayout(
        navController = navController,
        initiallyExpanded = false
    ) {
        // 1. CONFIGURATION
        azConfig(
            dockingSide = AzDockingSide.LEFT,
            packButtons = true, // Tightly pack rail items
            displayAppName = true
        )

        azTheme(activeColor = Color.Cyan)

        // 2. NAVIGATION ITEMS
        azRailItem(id = "home", text = "Home", route = "home", content = Icons.Default.Home)
        azRailItem(id = "profile", text = "Profile", route = "profile")

        // 3. MENU ONLY ITEMS
        azMenuItem(id = "settings", text = "Settings", route = "settings")

        // 4. ONSCREEN CONTENT
        // Use 'onscreen' to define your UI. 
        // Layout rules (safe zones, padding) are enforced automatically.
        onscreen(alignment = Alignment.Center) {
            AzNavHost(startDestination = "home") {
                composable("home") { Text("Home Screen") }
                composable("profile") { Text("Profile Screen") }
                composable("settings") { Text("Settings Screen") }
            }
        }
    }
}
~~~

---

## ✨ Features & Basics

### AzHostActivityLayout Configuration

`AzHostActivityLayout` accepts several parameters to customize its behavior:

* **`navController`**: The `NavHostController` to use. **Required.**
* **`currentDestination`**: Explicitly set the current route. If null, it is automatically derived from the `navController`.
* **`isLandscape`**: Explicitly set the orientation. If null, it is automatically derived from the screen configuration.
* **`initiallyExpanded`**: Set to `true` to have the rail expanded by default (e.g., for bubble activities).
* **`disableSwipeToOpen`**: Set to `true` to disable the swipe gesture that opens the menu.

### AzHostActivityLayout Layout Rules

`AzHostActivityLayout` enforces a "Strict Mode" layout system:

1.  **Rail Avoidance**: No content in the `onscreen` block will overlap the rail. Padding is automatically applied based on the docking side.
2.  **Vertical Safe Zones**: Content is restricted from the top 10% and bottom 10% of the screen.
3.  **Automatic Flipping**: Alignments passed to `onscreen` (e.g., `TopStart`) are automatically mirrored if the rail is docked to the right.
4.  **Backgrounds**: Use the `background(weight)` DSL to place full-screen content behind the UI (e.g., maps, camera feeds). Backgrounds ignore safe zones.

### Help Overlay (Help Mode)

`AzNavRail` includes an interactive "Help Mode" (formerly Info Screen), ideal for onboarding or help sections.

- **Activation**: Explicitly enable via `helpEnabled = true` in `azAdvanced`.
- **Triggering**:
    - **Auto Menu Item**: A "Help" item is automatically added to the bottom of the drawer menu.
    - **Explicit Trigger**: Use `azHelpRailItem(id, text)` to place a dedicated help button in the rail.
    - **Help List**: An optional mapping of `RailItem` IDs to help texts to display in the help cards along with the item's `info` property. The text from `helpList` is displayed second.
- **Behavior**:
    - **Visual Guides**: Dynamic lines connect interactive info cards to their corresponding rail items (even if they are scrolled off-screen).
    - **Interactive Info Cards**: Cards are displayed in a scrollable list showing short, truncated text by default. Tap any card to expand it for the full description.
    - **Line Persistence**: Connection lines update in real-time as you scroll through the help cards or the rail.
    - **Nested Rail Support**: Help cards automatically resize and pad themselves to prevent overlapping when a nested rail is open, and lines will draw correctly to nested items when their specific `helpList` or `info` is provided.
    - **Contextual Interactivity**: Main navigation items are disabled while the overlay is active. However, **Host Items** remain interactive, allowing users to navigate hierarchical structures within the help context.
- **Exit**: Tap anywhere on the dark background overlay to dismiss the mode.

### `AzTextBox` and `AzForm`

`AzTextBox` is a text input field. `AzForm` is a container that groups multiple `AzTextBox` fields, managing them as a single entity with one submit button.

#### Features

-   **Multiline Support**: `AzTextBox` can be configured as a multiline input, which will automatically expand vertically as the user types. The clear and submit buttons remain anchored to the bottom right.
-   **Secret / Password Fields**: Text boxes can be set to `secret` mode, which masks the input. In this mode, the clear button is replaced by a reveal icon to temporarily show the password.
-   **Mutual Exclusivity**: A field cannot be `multiline` and `secret` at the same time.
-   **Unified Styling**:
    -   The input text, outline, and all icons (clear, reveal, submit) share the same color, which can be customized.
    -   The background color and opacity for all text boxes and forms can be set globally.
    -   The submit button's background always matches the text box's background.
-   **Intelligent Autocomplete**:
    -   Suggestions appear in a dropdown as the user types.
    -   The dropdown's style is clean: no outlines or separators, with a background that alternates between 90% and 80% opacity for each suggestion.
    -   Suggestions are sorted by recency, showing the most recently used matching entries first.
-   **`AzForm` Component**:
    -   Group multiple text fields into a single form with a shared submit button.
    -   Each field within the form has its own clear or reveal button.
    -   Styling (outline, background) is applied consistently to all fields within the form.
-   **Disabled State**: Both `AzTextBox` and `AzForm` entries support an `enabled` parameter. When disabled, the input is non-interactive and visual elements are dimmed.

### AzLoad Animation


#### React Quick Start

```tsx
import { AzNavRail, AzNavItem, AzButtonShape, AzDockingSide } from '@HereLiesAz/aznavrail-react';

export default function App() {
  const [expanded, setExpanded] = useState(false);

  const items: AzNavItem[] = [
    {
      id: "home",
      text: "Home",
      isRailItem: true,
      onClick: () => console.log("Home clicked"),
      shape: AzButtonShape.CIRCLE,
    },
    {
      id: "settings",
      text: "Settings",
      isRailItem: true,
      onClick: () => console.log("Settings clicked"),
      shape: AzButtonShape.RECTANGLE,
    }
  ];

  return (
    <View style={{ flex: 1, flexDirection: 'row' }}>
      <AzNavRail
        appName="My App"
        appIcon={require('./assets/icon.png')}
        items={items}
        expanded={expanded}
        onToggleExpand={() => setExpanded(!expanded)}
        settings={{
            dockingSide: AzDockingSide.LEFT,
            activeColor: '#6200EE'
        }}
      />
      <View style={{ flex: 1 }}>
        {/* Main Content */}
      </View>
    </View>
  );
}
```


### 1. Strict Layout System
`AzHostActivityLayout` enforces a "Constitution" for your UI to ensure consistency and usability:
*   **Safe Zones:** Top 10% and Bottom 10% are reserved. Interactive content is pushed to the center 80%.
*   **Automatic Padding:** Content in `onscreen` is automatically padded to avoid the rail, regardless of docking side or rotation.
*   **Backgrounds:** Use `background(weight)` to place content *behind* the rail (e.g., maps).

### 2. Navigation Items
*   **`azRailItem`**: Always visible.
*   **`azMenuItem`**: Visible only in the drawer.
*   **`azNestedRail`**: Opens a secondary popup rail (Vertical/Horizontal).
*   **`azRailRelocItem`**: Draggable items for user reordering.

### 3. Interactive Components
Manage state directly in the rail without leaving the context.
*   **Toggles:** `azRailToggle` / `azMenuToggle`.
*   **Cyclers:** `azRailCycler` (multi-state buttons).

~~~kotlin
AzNavRail(...) {
    azAdvanced(
        isLoading = true // Shows the AzLoad animation in the center of the screen
        // ...
    )
}
~~~


**React Implementation:**
```tsx
import { AzNavItem } from '@HereLiesAz/aznavrail-react';

const items: AzNavItem[] = [
    {
        id: "power",
        isRailItem: true,
        isToggle: true,
        isChecked: isPowerOn,
        toggleOnText: "Power On",
        toggleOffText: "Power Off",
        onClick: () => setPowerOn(!isPowerOn),
        // ...
    },
    {
        id: "mode",
        isRailItem: true,
        isCycler: true,
        options: ["Auto", "Cool", "Heat"],
        selectedOption: currentMode,
        // ...
    }
];
```


#### Standalone Usage

You can also use `AzLoad` directly in your composables.


**React Implementation:**
```tsx
import { AzLoad } from '@HereLiesAz/aznavrail-react';

<AzLoad size={48} color="#6200EE" />
```


### Standalone Buttons

The `AzButton` component (and `AzToggle`, `AzCycler`) can be used independently of the rail.

~~~kotlin
AzButton(
    onClick = { /* ... */ },
    text = "Save",
    modifier = Modifier.fillMaxWidth(), // Now supports modifiers
    shape = AzButtonShape.RECTANGLE,
    enabled = true, // Can be disabled
    isLoading = false, // Shows loading spinner without resizing button
    contentPadding = PaddingValues(16.dp) // Custom padding
)
~~~

### AzRoller

The `AzRoller` component is a versatile dropdown that behaves like a slot machine but also supports typing and filtering. It extends the functionality of `AzTextBox` with a unique split-click interaction model.

~~~kotlin
AzRoller(
    options = listOf("Cherry", "Bell", "Bar"),
    selectedOption = "Cherry",
    onOptionSelected = { /* handle selection (String) */ },
    hint = "Select Item",
    enabled = true,
    isError = false
)
~~~

- **Split Interaction**:
    - **Left Click**: Activates text edit mode for typing and filtering.
    - **Right Click**: Opens the dropdown in "Slot Machine" mode for browsing.
- **Slot Machine Experience**: The dropdown list visually overlaps the input field, allowing users to "scroll" items into the selection slot. Items snap into place.
- **Typing Support**: Users can type to filter or find options, or enter a value not present in the list. As you type, the dropdown automatically filters to show only matching options. The list automatically manages transparency to ensure the input is visible while typing.
- **Dropdown Reset**: Clicking the dropdown arrow while typing exits "Text Mode" and re-opens the full list in "Slot Machine" mode.


**React Implementation:**
```tsx
import { AzRoller } from '@HereLiesAz/aznavrail-react';

<AzRoller
    options={["Cherry", "Bell", "Bar"]}
    selectedOption="Cherry"
    onOptionSelected={(option) => { /* handle selection */ }}
    hint="Select Item"
    enabled={true}
/>
```


### Hierarchical Navigation

`AzNavRail` supports hierarchical navigation with host and sub-items. This allows you to create nested menus that are easy to navigate.

-   **Host Items**: These are top-level items that can contain sub-items. They can be placed in the rail or the menu.
-   **Sub-Items**: These are nested items that are only visible when their host item is expanded. They can also be placed in the rail or the menu.
-   **Exclusive Expansion**: Only one host item can be expanded at a time. Expanding a host item automatically collapses any other open host items.


**React Implementation:**
```tsx
const items: AzNavItem[] = [
    {
        id: "host-1",
        text: "Host Item",
        isRailItem: true,
        isHost: true,
        isExpanded: isHost1Expanded,
        onClick: () => setHost1Expanded(!isHost1Expanded),
        // ...
    },
    {
        id: "sub-1",
        text: "Sub Item",
        isRailItem: true,
        isSubItem: true,
        hostId: "host-1",
        // ...
    }
];
```


### Draggable Rail (FAB Mode)

The rail can be detached and moved around the screen by long-pressing the header icon, which activates "FAB Mode". To enable this feature, set `enableRailDragging = true` in the `azAdvanced` block.

- **Activation**: Long-press the header (app icon or name) to undock the rail and enter FAB mode. A vertical swipe on the rail will also activate it. Haptic feedback confirms activation/deactivation.
- **Appearance**: In FAB mode, the rail collapses into a floating action button (FAB) displaying the app icon. If the app name was displayed, it transforms into the icon.
- **Interaction**:
    - **Tap**: Tapping the FAB unfolds the rail items downwards. Tapping it again folds them back up. The menu is not available in FAB mode.
    - **Drag**: The FAB can be dragged anywhere on the screen, but is constrained to stay within the top and bottom 10% of the screen. If the rail items are unfolded, they will automatically fold up when a drag begins and unfold when it ends.
- **Deactivation**:
    - **Snapping**: Drag the FAB close to its original docked position to snap it back into place, exiting FAB mode.
    - **Long Press**: Long-pressing the FAB will also immediately re-dock the rail.


**React Implementation:**
```tsx
import { AzNavRailSettings } from '@HereLiesAz/aznavrail-react';

const settings: AzNavRailSettings = {
    enableRailDragging: true
};
// Pass settings to AzNavRail
```


### Reorderable Items (AzRailRelocItem)

`AzRailRelocItem` is a specialized sub-item that users can reorder via drag-and-drop. This feature is supported on Android, Web, and React Native.

~~~kotlin
azRailRelocItem(
    id = "reloc-1",
    hostId = "host-1",
    text = "Item 1",
    forceHiddenMenuOpen = false, // Programmatically open the hidden menu!
    onHiddenMenuDismiss = { /* Menu dismissed */ },
    onRelocate = { from, to, newOrder ->
        // Handle new order (List<String>)
    }
) {
    // Hidden Menu (Tap to select -> Long Press to open)
    listItem("Action 1") { /* ... */ }
    inputItem("Rename", initialValue = "Item 1") { newName -> /* ... */ }
}
~~~


**React Implementation:**
```tsx
import { AzRailRelocItemProps, HiddenMenuScope } from '@HereLiesAz/aznavrail-react';

const relocItem: AzRailRelocItemProps = {
    id: "reloc-1",
    hostId: "host-1",
    text: "Item 1",
    isRailItem: false,
    isSubItem: true,
    forceHiddenMenuOpen: false,
    onHiddenMenuDismiss: () => { /* Menu dismissed */ },
    onRelocate: (fromIndex, toIndex, newOrder) => {
        // Handle new order
    },
    hiddenMenu: (scope: HiddenMenuScope) => {
        scope.listItem("Action 1", () => { /* ... */ });
        scope.inputItem("Rename", "Item 1", (newName) => { /* ... */ });
    }
};
// Pass this object within the items array to AzNavRail
```


- **Drag-and-Drop**: Long-press (triggers a vibration) and drag an item to move it. Other items will animate to create an empty slot at the potential drop target.
- **Cluster Constraints**: Items can only be moved within their "cluster" — a contiguous group of relocation items under the same host. They cannot jump over standard items or move to a different host.
- **Hidden Menu**: Tapping the item brings it into focus (selects it). Long-pressing the item *without dragging* opens the contextual menu. This menu supports list items and input fields.

### System Overlay

AzNavRail can function as a system-wide overlay (using `SYSTEM_ALERT_WINDOW`). This allows users to access the navigation menu from anywhere on their device.

#### Features

---
### Theming and Customization

#### Menu Font Size
The expanded menu text font size (and the footer items text size) is strictly controlled by your app's `MaterialTheme.typography.titleLarge`. To adjust the text size inside the side menu drawer, simply customize the `titleLarge` attribute in your app's typography theme!

#### Customizing Item Text and Colors
Navigation items support overriding their display text and colors when shown in the menu versus the rail using `menuText`, `menuToggleOnText`, `menuToggleOffText`, `menuOptions`, `textColor`, and `fillColor` properties! By default, the `fillColor` (translucent background) is automatically computed to be Black (with 25% opacity), unless the item's main color is Black, in which case it is set to White (with 25% opacity) to ensure proper contrast.


**React Implementation:**
```tsx
// Fonts and colors can be passed as props directly.
// The expanded menu text size can be handled via CSS or React Native styles
// depending on your implementation environment.
const settings: AzNavRailSettings = {
    activeColor: '#6200EE'
};

const items: AzNavItem[] = [
    {
        id: "custom",
        text: "Custom Color",
        color: '#FF0000',
        textColor: '#FFFFFF',
        fillColor: 'rgba(255,0,0,0.25)',
        // ...
    }
];
```


### Documentation

The library includes a comprehensive **Complete Guide** (`docs/AZNAVRAIL_COMPLETE_GUIDE.md`) containing:
* Full Getting Started instructions.
* Complete API and DSL references.
* Layout rules and best practices.
* Complete Sample App source code.

## License

Copyright 2024 The AzNavRail Authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


## Tutorial Framework

AzNavRail ships a full interactive tutorial framework. Tutorials are scripted as sequences of scenes and cards, rendered over a dimmed overlay with optional item spotlights. The framework supports four advance conditions, variable-driven branching, event-driven advances, checklist cards, media cards, and cross-platform persistence of read state.

### Features

- **4 advance conditions:** Next button (default), TapTarget (tap the highlighted item), TapAnywhere, Event (app calls `fireEvent(name)`).
- **Variable branching:** Pass a variables map to `startTutorial`; scenes can act as invisible redirect nodes that route based on variable values.
- **TapTarget branching:** A single card can route to different scenes depending on which highlighted item the user taps.
- **Checklist cards:** Next is disabled until every item is checked.
- **Media cards:** Inline media rendered between title and text (max height 120dp/120px).
- **Persistence:** Read tutorials are stored per-platform (`SharedPreferences` on Android, `AsyncStorage` on React Native, `localStorage` on Web). Key: `az_navrail_read_tutorials`.
- **Help/info overlay integration:** Collapsed cards show a "Tutorial available" hint. Expanding a card shows a "Start Tutorial" button — tapping it calls `startTutorial` and dismisses the overlay.

### Android (Kotlin DSL)

```kotlin
import com.hereliesaz.aznavrail.tutorial.AzHighlight
import com.hereliesaz.aznavrail.tutorial.AzAdvanceCondition
import com.hereliesaz.aznavrail.tutorial.azTutorial

val tutorial = azTutorial {
    onComplete { /* fired when last scene finishes */ }
    onSkip { /* fired when Skip Tutorial tapped */ }

    // Variable branch gate (invisible redirect node)
    scene(id = "gate", content = { /* empty backdrop */ }) {
        branch(varName = "userLevel", mapOf(
            "advanced" to "scene-advanced",
            "basic"    to "scene-basic"
        ))
    }

    scene(id = "scene-advanced", content = { ScreenA() }) {
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

        // Checklist card
        card(
            title = "Before you continue",
            text = "Confirm the following:",
            checklistItems = listOf("I read the docs", "I set up my account")
        )

        // Media card
        card(
            title = "The Rail",
            text = "Sits on the left or right edge.",
            mediaContent = { Image(painterResource(R.drawable.rail), null) }
        )
    }
}

// Wire up the controller
val controller = rememberAzTutorialController()
CompositionLocalProvider(LocalAzTutorialController provides controller) {
    // ...
    if (controller.activeTutorialId.value == "tut-1") {
        AzTutorialOverlay(
            tutorialId = "tut-1",
            tutorial = tutorial,
            onDismiss = { controller.endTutorial() },
            itemBoundsCache = boundsMap, // collected via onItemGloballyPositioned
        )
    }
}

// Start with variables
controller.startTutorial("tut-1", variables = mapOf("userLevel" to "advanced"))

// Fire an event from app code
controller.fireEvent("menu_opened")
```

### React Native / Web (TypeScript)

```typescript
import { AzTutorial } from '@HereLiesAz/aznavrail-react'; // or aznavrail-web

const tutorial: AzTutorial = {
    onComplete: () => {},
    onSkip: () => {},
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
            content: () => <ScreenA />,
            cards: [
                {
                    title: 'Pick a path',
                    text: 'Tap the item you want to learn about.',
                    highlight: { type: 'Item', id: 'nav-menu' },
                    advanceCondition: { type: 'TapTarget' },
                    branches: { 'settings-btn': 'scene-settings', 'profile-btn': 'scene-profile' },
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
            ],
        },
    ],
};

// React Native
import { AzTutorialProvider, useAzTutorialController } from '@HereLiesAz/aznavrail-react';

<AzTutorialProvider>
    <App />
</AzTutorialProvider>

const controller = useAzTutorialController();
controller.startTutorial('tut-1', { userLevel: 'advanced' });
controller.fireEvent('menu_opened');

// Web
import { AzWebTutorialProvider, useAzWebTutorialController } from '@HereLiesAz/aznavrail-web';

<AzWebTutorialProvider>
    <App />
</AzWebTutorialProvider>

const ctrl = useAzWebTutorialController();
ctrl.startTutorial('tut-1', { userLevel: 'advanced' });
```

See [`docs/TUTORIAL_FRAMEWORK_REFERENCE.md`](docs/TUTORIAL_FRAMEWORK_PROPOSAL.md) for the complete API reference and [`docs/AZNAVRAIL_COMPLETE_GUIDE.md`](docs/AZNAVRAIL_COMPLETE_GUIDE.md) for end-to-end usage examples.
