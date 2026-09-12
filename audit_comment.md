[UNSOUND] aznavrail/src/test/java/com/hereliesaz/aznavrail/AzNavigationReadinessTest.kt:290 — The test `cancelPendingNavigation removes the destination-changed listener` passes even if the listener is never removed.
  Failure: If the listener removal call in `cancelPendingNavigation()` is omitted, the test still passes because clearing the routes queue alone is enough to prevent navigation when the graph later initializes.
  Evidence: I commented out `listeners.remove(controller)?.let { controller.removeOnDestinationChangedListener(it) }` in `NavigationReadiness.kt` and ran the test; it happily passed, because an orphaned listener iterating over an empty route queue does nothing and allows the weak assertion (`assertEquals("home", ...)`) to succeed.
  Confidence: CONFIRMED (I traced it and deliberately broke the code to verify the test's failure mode)

- `DisposableEffect` placement and controller cleanup in `AzHostActivityLayout`: Genuinely functional.
- Pre-readiness queueing and coalescing logic in `NavigationReadiness.kt`: Genuinely functional.
- Recomposition logic and duplicate suppression testing: Genuinely functional.

Verdict: One test is a placebo masking its own success condition, but the underlying implementation code is sound.
