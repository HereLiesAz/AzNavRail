# Tutorial Framework Reference

This document is the complete reference for the AzNavRail Tutorial Framework as shipped. It covers the full feature set, API surface across all three platforms (Android/Kotlin, React Native/TypeScript, Web/TypeScript), DSL examples, persistence behavior, and the help/info overlay launch flow.

---

## Feature Summary

- **4 advance conditions:** Button (Next button, default), TapTarget (tap the highlighted item), TapAnywhere, Event (app calls `fireEvent(name)`).
- **Variable-driven scene branching:** Pass a `variables` map to `startTutorial`; scenes configured with `branchVar`/`branches` act as invisible redirect nodes, routing to different scenes based on variable values.
- **TapTarget item branching:** A single card can route to different scenes depending on which highlighted item the user taps (`card.branches: Map<itemId, sceneId>`).
- **Checklist cards:** Next button disabled until every checklist item is checked.
- **Media cards:** Inline media rendered between title and body text (max 120dp/120px height, 8dp/8px corner clip).
- **Cross-platform persistence:** Read-tutorial state is persisted per platform. Same key everywhere: `az_navrail_read_tutorials`.
- **Card auto-positioning:** Defaults to bottom; flips to top when spotlight center Y > 60% of screen height.
- **TapTarget degradation:** Falls back to TapAnywhere when highlight is not `AzHighlight.Item`.
- **Circular branch detection:** Logs a warning and advances to the next scene by index (or ends the tutorial if out of bounds).
- **Help/Info overlay integration:** Collapsed cards show "Tutorial available" hint; expanded cards show a "Start Tutorial" button that calls `startTutorial` and dismisses the overlay.
- **Web port parity:** The Web library is a TypeScript port of Android. Spotlight implemented via `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)` — the CSS equivalent of Android's `BlendMode.Clear` punch-out.

---

## Data Model

The same logical shape is used across all platforms. Kotlin uses a sealed class and a DSL builder; TypeScript uses plain objects with discriminated unions.

### `AzAdvanceCondition`

**Kotlin (sealed class):**

| Variant | Description |
| :--- | :--- |
| `AzAdvanceCondition.Button` | Next button shown. User taps to advance. |
| `AzAdvanceCondition.TapTarget` | User taps the spotlighted item. Degrades to TapAnywhere if highlight is not `AzHighlight.Item`. |
| `AzAdvanceCondition.TapAnywhere` | User taps anywhere on screen. |
| `AzAdvanceCondition.Event(name: String)` | Advances when `controller.fireEvent(name)` is called. |

**TypeScript (discriminated union):**

| Literal | Description |
| :--- | :--- |
| `{ type: 'Button' }` | Next button. |
| `{ type: 'TapTarget' }` | Tap the spotlighted item. |
| `{ type: 'TapAnywhere' }` | Tap anywhere. |
| `{ type: 'Event', name: string }` | App fires the named event. |

### `AzHighlight`

**Kotlin (sealed class):**

| Variant | Description |
| :--- | :--- |
| `AzHighlight.None` | No spotlight. |
| `AzHighlight.FullScreen` | Full-screen highlight (no punch-out). |
| `AzHighlight.Item(id: String)` | Spotlights the rail item with the given ID using its measured bounds. |
| `AzHighlight.Area(rect: Rect)` | Spotlights an arbitrary dp rect. |

**TypeScript:**

| Literal | Description |
| :--- | :--- |
| `{ type: 'None' }` | No spotlight. |
| `{ type: 'FullScreen' }` | Full-screen highlight. |
| `{ type: 'Item', id: string }` | Spotlights a named item. |
| `{ type: 'Area', rect: DpRect }` | Spotlights an arbitrary rect. |

### `AzCard`

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `title` | `String` | required | Card heading. |
| `text` | `String` | required | Body text. |
| `highlight` | `AzHighlight` | `None` | What to spotlight. |
| `actionText` | `String?` | `null` | Custom action button label. |
| `onAction` | `(() -> Unit)?` | `null` | Additional callback on action tap. |
| `advanceCondition` | `AzAdvanceCondition` | `Button` | How the card advances. |
| `branches` | `Map<String, String>` | `emptyMap()` | TapTarget only: itemId → sceneId. |
| `mediaContent` | composable / component | `null` | Media rendered between title and text. Max 120dp/120px, 8dp/8px clip. |
| `checklistItems` | `List<String>` | `emptyList()` | Checklist items. Next disabled until all checked. |

### `AzScene`

| Field | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `id` | `String` | required | Unique scene identifier. |
| `content` | composable / component | required | UI rendered behind the overlay. |
| `cards` | `List<AzCard>` | required | Ordered cards for this scene. May be empty for redirect nodes. |
| `branchVar` | `String?` | `null` | Variable name evaluated on scene entry. |
| `branches` | `Map<String, String>` | `emptyMap()` | variableValue → sceneId. Evaluated when `branchVar` is set. |

### `AzTutorial`

| Field | Type | Description |
| :--- | :--- | :--- |
| `scenes` | `List<AzScene>` | Ordered scene list. |
| `onComplete` | `(() -> Unit)?` | Fired when last card of last scene completes. |
| `onSkip` | `(() -> Unit)?` | Fired when Skip Tutorial is tapped. |

---

## Controller API

### Android — `AzTutorialController`

**Creation:** `rememberAzTutorialController()` at the composable root. Provide via `LocalAzTutorialController`.

~~~kotlin
val controller = rememberAzTutorialController()
CompositionLocalProvider(LocalAzTutorialController provides controller) {
    // ...
}
~~~

**`Saver`:** `AzTutorialController.Saver(context: Context)` — a function call, not a plain `val`. `rememberAzTutorialController()` handles this internally.

| Member | Description |
| :--- | :--- |
| `activeTutorialId: State<String?>` | Currently running tutorial ID, or `null`. |
| `readTutorials: List<String>` | Tutorial IDs persisted as read. |
| `startTutorial(id, variables)` | Starts tutorial. `variables: Map<String,String>` drives branching. |
| `endTutorial()` | Ends the active tutorial without triggering `onComplete`. |
| `markTutorialRead(id)` | Persists tutorial as read. |
| `isTutorialRead(id)` | Returns `true` if the tutorial has been read. |
| `fireEvent(name)` | Fires a named event consumed by the active `Event` advance condition. |
| `consumeEvent(): String?` | Returns and clears the pending event (called internally by the overlay). |

### React Native — `AzTutorialProvider` / `useAzTutorialController`

~~~tsx
import { AzTutorialProvider, useAzTutorialController } from '@HereLiesAz/aznavrail-react';

<AzTutorialProvider tutorials={{ 'tut-1': tutorial }}>
    <App />
</AzTutorialProvider>

const controller = useAzTutorialController();
~~~

Methods and properties are identical to the Android controller above (TypeScript types).

`pendingEvent: string | null` — observable pending event for the overlay.

### Web — `AzWebTutorialProvider` / `useAzWebTutorialController`

Distinct from the React Native provider. Do not mix imports.

~~~tsx
import { AzWebTutorialProvider, useAzWebTutorialController } from '@HereLiesAz/aznavrail-web';

<AzWebTutorialProvider tutorials={{ 'tut-1': tutorial }}>
    <App />
</AzWebTutorialProvider>

const ctrl = useAzWebTutorialController();
~~~

Methods and properties are identical to the React Native controller.

---

## Overlay Components

### Android — `AzTutorialOverlay`

Mount conditionally based on `activeTutorialId`:

~~~kotlin
if (controller.activeTutorialId.value == "tut-1") {
    AzTutorialOverlay(
        tutorialId = "tut-1",
        tutorial = myTutorial,
        onDismiss = { controller.endTutorial() },
        itemBoundsCache = boundsMap // Map<String, Rect> from onItemGloballyPositioned
    )
}
~~~

### Web — `AzWebTutorialOverlay`

Equivalent component from the web library. New source file: `web/AzTutorialOverlay.tsx`.

---

## Persistence

| Platform | Storage | File / store | Key |
| :--- | :--- | :--- | :--- |
| Android | `SharedPreferences` | `az_tutorial_prefs` | `az_navrail_read_tutorials` |
| React Native | `AsyncStorage` (or in-memory fallback if package absent) | N/A | `az_navrail_read_tutorials` |
| Web | `localStorage` | N/A | `az_navrail_read_tutorials` |

State is read on controller creation/provider mount and written on each `markTutorialRead()` call.

---

## Help/Info Overlay Launch Flow

1. User opens the help overlay.
2. **Collapsed card:** If a tutorial is registered for that item, the card shows a "Tutorial available" hint. No action on tap beyond expanding.
3. **Expanded card:** A "Start Tutorial" button is shown. Tapping it:
   - Calls `tutorialController.startTutorial(id)`.
   - Dismisses the help overlay.
4. The tutorial overlay mounts and begins at the first scene.

The old behavior — any tap on a collapsed card immediately starting the tutorial — is removed.

---

## DSL Examples

### Android (Kotlin)

~~~kotlin
import com.hereliesaz.aznavrail.tutorial.*

val tutorial = azTutorial {
    onComplete { }
    onSkip     { }

    // Invisible redirect node
    scene(id = "gate", content = { }) {
        branch(varName = "userLevel", mapOf(
            "advanced" to "scene-advanced",
            "basic"    to "scene-basic"
        ))
    }

    scene(id = "scene-advanced", content = { AdvancedScreen() }) {
        // 1. Button (default)
        card(title = "Welcome", text = "Pro experience.", highlight = AzHighlight.FullScreen)

        // 2. TapAnywhere
        card(
            title = "Tap anywhere",
            text  = "To continue.",
            advanceCondition = AzAdvanceCondition.TapAnywhere
        )

        // 3. TapTarget with branching
        card(
            title  = "Pick a path",
            text   = "Tap the item.",
            highlight = AzHighlight.Item("nav-menu"),
            advanceCondition = AzAdvanceCondition.TapTarget,
            branches = mapOf("settings-btn" to "scene-settings", "profile-btn" to "scene-profile")
        )

        // 4. Event-driven
        card(
            title  = "Open the menu",
            text   = "Swipe right.",
            highlight = AzHighlight.Item("rail-header"),
            advanceCondition = AzAdvanceCondition.Event("menu_opened")
        )

        // 5. Checklist
        card(
            title = "Before you continue",
            text  = "Confirm:",
            checklistItems = listOf("I read the docs", "I set up my account")
        )

        // 6. Media
        card(
            title  = "The Rail",
            text   = "Sits on the left or right edge.",
            mediaContent = { Image(painterResource(R.drawable.rail), null) }
        )
    }
}

// Register
azAdvanced(
    helpEnabled = true,
    onItemGloballyPositioned = { id, rect -> boundsMap[id] = rect },
    tutorials = mapOf("tut-1" to tutorial)
)

// Start
controller.startTutorial("tut-1", variables = mapOf("userLevel" to "advanced"))

// Fire event
controller.fireEvent("menu_opened")
~~~

### TypeScript (React Native / Web)

~~~typescript
const tutorial: AzTutorial = {
    onComplete: () => {},
    onSkip:     () => {},
    scenes: [
        {
            id: 'gate', content: () => null, cards: [],
            branchVar: 'userLevel',
            branches: { advanced: 'scene-advanced', basic: 'scene-basic' },
        },
        {
            id: 'scene-advanced',
            content: () => <AdvancedScreen />,
            cards: [
                // 1. Button (default — omit advanceCondition)
                { title: 'Welcome', text: 'Pro experience.', highlight: { type: 'FullScreen' } },

                // 2. TapAnywhere
                {
                    title: 'Tap anywhere', text: 'To continue.',
                    highlight: { type: 'None' },
                    advanceCondition: { type: 'TapAnywhere' },
                },

                // 3. TapTarget with branching
                {
                    title: 'Pick a path', text: 'Tap the item.',
                    highlight: { type: 'Item', id: 'nav-menu' },
                    advanceCondition: { type: 'TapTarget' },
                    branches: { 'settings-btn': 'scene-settings', 'profile-btn': 'scene-profile' },
                },

                // 4. Event-driven
                {
                    title: 'Open the menu', text: 'Swipe right.',
                    highlight: { type: 'Item', id: 'rail-header' },
                    advanceCondition: { type: 'Event', name: 'menu_opened' },
                },

                // 5. Checklist
                {
                    title: 'Before you continue', text: 'Confirm:',
                    checklistItems: ['I read the docs', 'I set up my account'],
                },

                // 6. Media
                {
                    title: 'The Rail', text: 'Sits on the left or right edge.',
                    mediaContent: () => <img src={railImg} style={{ maxHeight: 120 }} alt="" />,
                },
            ],
        },
    ],
};

// React Native
// <AzTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzTutorialProvider>
// const controller = useAzTutorialController();

// Web
// <AzWebTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzWebTutorialProvider>
// const ctrl = useAzWebTutorialController();

controller.startTutorial('tut-1', { userLevel: 'advanced' });
controller.fireEvent('menu_opened');
~~~

---

## Web Port Details

New source files added to the web library:

| File | Description |
| :--- | :--- |
| `web/AzTutorialController.tsx` | Controller context, provider, and hook (`AzWebTutorialProvider`, `useAzWebTutorialController`). |
| `web/AzTutorialOverlay.tsx` | Overlay component. Spotlight via `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)`. |
| `web/HelpOverlay.tsx` | Replaces `HelpOverlay.jsx`. Implements the "Tutorial available" hint and "Start Tutorial" button. |
