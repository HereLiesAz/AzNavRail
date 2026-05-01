# The Synthetic Tongue (AzNavRailScope)

This document outlines the API surface of `AzNavRailScope`. This interface defines the DSL used to configure the rail, whether manually or via the generated code.

## The Live Dictatorship (v7.25)

The **v7.25 KSP Processor** injects reactive logic. When using `@Az` annotations with property bindings (e.g., `textProperty = "myTitle"`), the generated code no longer passes hardcoded literals. Instead, it accesses your `AzActivity` instance directly.

Because these properties are ideally `mutableStateOf`, the entire `AzGraph` will **recompose** automatically when your Activity state changes.

## Configuration & Theme

### `azConfig`
Controls behavioral and geometrical mechanics.

~~~kotlin
fun azConfig(
    dockingSide: AzDockingSide = AzDockingSide.LEFT,
    packButtons: Boolean = false,
    noMenu: Boolean = false,
    vibrate: Boolean = false,
    displayAppName: Boolean = false,
    activeClassifiers: Set<String> = emptySet(),
    usePhysicalDocking: Boolean = false,
    expandedWidth: Dp = 130.dp,
    collapsedWidth: Dp = 80.dp,
    showFooter: Boolean = true
)
~~~

### `azTheme`
Controls the visual style of the rail.

~~~kotlin
fun azTheme(
    activeColor: Color = Color.Unspecified,
    defaultShape: AzButtonShape = AzButtonShape.CIRCLE,
    headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE
)
~~~

### `azAdvanced`
Controls system overrides, loading states, and floating window bindings.

~~~kotlin
fun azAdvanced(
    isLoading: Boolean = false,
    helpEnabled: Boolean = false,
    onDismissHelp: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onUndock: (() -> Unit)? = null,
    enableRailDragging: Boolean = false,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    helpList: Map<String, Any> = emptyMap()
)
~~~

## Hidden Menu Builders (for `azRailRelocItem`)
* `listItem(text, route)`
* `listItem(text, onClick)`
* `inputItem(hint, onValueChange)`
* `inputItem(hint, initialValue, onValueChange)`

## Item Builders

The following functions are used to define the rail structure.

* `azMenuItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azHelpRailItem(id, text, content, color, shape)`
* `azRailItem(id, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `@Composable azNestedRail(id, text, route, content, color, shape, alignment, disabled, screenTitle, info, classifiers, onFocus) { ... }`
* `azMenuToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailToggle(id, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailCycler(id, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azDivider()`
* `azMenuHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailHostItem(id, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick)`
* `azMenuSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azRailSubToggle(id, hostId, isChecked, toggleOnText, toggleOffText, route, color, shape, disabled, screenTitle, info, onClick)`
* `azMenuSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailSubCycler(id, hostId, options, selectedOption, route, color, shape, disabled, disabledOptions, screenTitle, info, onClick)`
* `azRailRelocItem(id, hostId, text, route, content, color, shape, disabled, screenTitle, info, classifiers, onFocus, onClick, onRelocate, forceHiddenMenuOpen, onHiddenMenuDismiss) { ... }`

---

## Tutorial DSL (`azTutorial`)

Tutorials are defined with `azTutorial { ... }` and registered via `azAdvanced(tutorials = mapOf("id" to tutorial))`.

### Kotlin

~~~kotlin
import com.hereliesaz.aznavrail.tutorial.*

val tutorial = azTutorial {
    // Lifecycle callbacks (both optional)
    onComplete { /* fired when last scene completes */ }
    onSkip    { /* fired when Skip Tutorial tapped */ }

    // ── Variable branch gate (invisible redirect node) ──────────────────
    scene(id = "gate", content = { /* empty or backdrop composable */ }) {
        branch(varName = "plan", mapOf(
            "pro"  to "scene-pro",
            "free" to "scene-free"
        ))
    }

    // ── Scene with all card types ────────────────────────────────────────
    scene(id = "scene-pro", content = { ProScreen() }) {

        // 1. Default advance condition: Button (Next button shown)
        card(
            title  = "Welcome",
            text   = "This is the pro experience.",
            highlight = AzHighlight.FullScreen
        )

        // 2. TapAnywhere
        card(
            title  = "Tap anywhere to continue",
            text   = "Really, anywhere.",
            highlight = AzHighlight.None,
            advanceCondition = AzAdvanceCondition.TapAnywhere
        )

        // 3. TapTarget — with per-item branching
        card(
            title  = "Pick a path",
            text   = "Tap the item you want to explore.",
            highlight = AzHighlight.Item("nav-menu"),
            advanceCondition = AzAdvanceCondition.TapTarget,
            branches = mapOf(
                "settings-btn" to "scene-settings",
                "profile-btn"  to "scene-profile"
            )
        )

        // 4. Event-driven advance
        card(
            title  = "Open the menu",
            text   = "Swipe right or tap the rail header.",
            highlight = AzHighlight.Item("rail-header"),
            advanceCondition = AzAdvanceCondition.Event("menu_opened")
        )

        // 5. Checklist card — Next disabled until all items checked
        card(
            title  = "Before you continue",
            text   = "Confirm the following:",
            checklistItems = listOf("I read the docs", "I set up my account")
        )

        // 6. Media card — content shown between title and text
        card(
            title  = "The Rail",
            text   = "Sits on the left or right edge.",
            mediaContent = { Image(painterResource(R.drawable.rail), contentDescription = null) }
        )
    }
}

// Register
azAdvanced(
    helpEnabled = true,
    onItemGloballyPositioned = { id, rect -> boundsMap[id] = rect },
    tutorials = mapOf("tut-1" to tutorial)
)

// Start with variables
controller.startTutorial("tut-1", variables = mapOf("plan" to "pro"))

// Fire an event from app logic
controller.fireEvent("menu_opened")
~~~

### TypeScript (React Native / Web)

The TypeScript shape is a plain discriminated-union object — no builder DSL.

~~~typescript
import { AzTutorial, AzScene, AzCard } from '@HereLiesAz/aznavrail-react'; // or aznavrail-web

const tutorial: AzTutorial = {
    onComplete: () => {},
    onSkip:     () => {},
    scenes: [
        // Variable branch gate
        {
            id: 'gate',
            content: () => null,
            cards: [],
            branchVar: 'plan',
            branches: { pro: 'scene-pro', free: 'scene-free' },
        },
        {
            id: 'scene-pro',
            content: () => <ProScreen />,
            cards: [
                // 1. Button (default)
                {
                    title: 'Welcome',
                    text: 'This is the pro experience.',
                    highlight: { type: 'FullScreen' },
                },
                // 2. TapAnywhere
                {
                    title: 'Tap anywhere to continue',
                    text: 'Really, anywhere.',
                    highlight: { type: 'None' },
                    advanceCondition: { type: 'TapAnywhere' },
                },
                // 3. TapTarget with branching
                {
                    title: 'Pick a path',
                    text: 'Tap the item you want to explore.',
                    highlight: { type: 'Item', id: 'nav-menu' },
                    advanceCondition: { type: 'TapTarget' },
                    branches: {
                        'settings-btn': 'scene-settings',
                        'profile-btn':  'scene-profile',
                    },
                },
                // 4. Event-driven
                {
                    title: 'Open the menu',
                    text: 'Swipe right or tap the rail header.',
                    highlight: { type: 'Item', id: 'rail-header' },
                    advanceCondition: { type: 'Event', name: 'menu_opened' },
                },
                // 5. Checklist
                {
                    title: 'Before you continue',
                    text: 'Confirm the following:',
                    checklistItems: ['I read the docs', 'I set up my account'],
                },
                // 6. Media
                {
                    title: 'The Rail',
                    text: 'Sits on the left or right edge.',
                    mediaContent: () => <img src={railImg} style={{ maxHeight: 120 }} alt="" />,
                },
            ],
        },
    ],
};

// React Native provider + hook
// <AzTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzTutorialProvider>
// const controller = useAzTutorialController();

// Web provider + hook
// <AzWebTutorialProvider tutorials={{ 'tut-1': tutorial }}><App /></AzWebTutorialProvider>
// const ctrl = useAzWebTutorialController();

controller.startTutorial('tut-1', { plan: 'pro' });
controller.fireEvent('menu_opened');
~~~
