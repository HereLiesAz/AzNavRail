# AzNavRail Tutorial Framework Proposal

This document outlines the proposed architecture for the upcoming Interactive Tutorial Framework feature in AzNavRail.

## Concept
The tutorial framework transforms the existing `HelpOverlay`'s simple informational text into an interactive, multi-step learning experience. Instead of just "short" and "full" textual descriptions, expanding a help card will launch an `AzTutorial`.

An `AzTutorial` consists of a sequence of `AzScene`s. A scene represents a defined "scripted screen" taken from the application, where:
- The screen's initial conditions and data can be mocked or forcefully overridden (e.g., showing a dummy profile instead of the real user's profile).
- The completion conditions for a scene can differ from the real app (e.g., instead of submitting a real form, the user only needs to type into a specific field to advance the tutorial).
- Standard highlighting methods (e.g., a dimmed overlay with spotlights) direct the user's attention.
- Tooltips, animated pointers, or floating instructional cards replace standard help text.

## Proposed Data Models (Kotlin Example)

```kotlin
/**
 * A full interactive tutorial consisting of multiple scenes.
 */
data class AzTutorial(
    val id: String,
    val title: String,
    val scenes: List<AzScene>,
    val onComplete: () -> Unit,
    val onSkip: () -> Unit
)

/**
 * A single step/screen in the tutorial.
 */
data class AzScene(
    val id: String,
    val targetElementId: String?, // ID of the UI element to spotlight
    val instructionText: String,
    val highlightMethod: AzHighlightMethod = AzHighlightMethod.SPOTLIGHT,
    val mockData: Map<String, Any> = emptyMap(), // Overrides for the screen's initial state
    val advanceCondition: AzAdvanceCondition = AzAdvanceCondition.TAP_TARGET
)

/**
 * Defines how to highlight the target element.
 */
enum class AzHighlightMethod {
    SPOTLIGHT, // Dim screen, cut out a circle/rect around target
    PULSE,     // Pulsing border around target
    POINTER    // Floating arrow pointing to target
}

/**
 * Defines how the tutorial advances to the next scene.
 */
enum class AzAdvanceCondition {
    TAP_TARGET,   // User taps the highlighted element
    TAP_ANYWHERE, // User taps anywhere to continue
    CUSTOM_EVENT  // The app fires a specific programmatic event
}
```

## Cross-Platform Implementation Strategy

1. **Android (Jetpack Compose):**
   - Integrate with `Modifier.onGloballyPositioned` to dynamically calculate spotlight masks using `Canvas` and `BlendMode.Clear`.
   - The developer provides the `ScreenContent` lambda, which reads `LocalTutorialMockData` via `CompositionLocalProvider`.
2. **React Native:**
   - Use `measureInWindow` to find absolute coordinates for the spotlight.
   - Requires an overlay with `pointerEvents="box-none"` and potentially an SVG or clever border-radius styling to create a transparent cutout in a dark overlay.
3. **Web (React):**
   - Use `getBoundingClientRect()`.
   - Implement spotlights using CSS `box-shadow` with massive spread radii (e.g., `box-shadow: 0 0 0 9999px rgba(0,0,0,0.7)`) on an absolutely positioned element, or a `clip-path`.

## Scripting Interface
The scripting interface will allow developers to define tutorials inline within the `AzNavRailScope`:

```kotlin
azTutorial(id = "first_run_tutorial", title = "Welcome to the App") {
    scene("welcome") {
        instructionText = "Welcome! Let's get started."
        advanceCondition = AzAdvanceCondition.TAP_ANYWHERE
    }
    scene("profile") {
        targetElementId = "profile_button"
        instructionText = "Tap here to view your profile."
        highlightMethod = AzHighlightMethod.SPOTLIGHT
        advanceCondition = AzAdvanceCondition.TAP_TARGET
    }
}
```

*This framework will be developed in an upcoming ticket as an extension to the completed tap-to-expand HelpOverlay feature.*