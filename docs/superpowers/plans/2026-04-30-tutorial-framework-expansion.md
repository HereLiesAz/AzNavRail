# AzNavRail Tutorial Framework Expansion & Help/Info Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the tutorial framework with advance conditions, branching, new card types, and persistence; fix the help/info overlay across all platforms; bring the React Web library to an exact TypeScript port of the Android library.

**Architecture:** Android is the reference implementation. React Native and Web mirror it exactly. All platforms share the same behavioral spec: four advance conditions (`Button`, `TapTarget`, `TapAnywhere`, `Event`), two branching mechanisms (tap-target and variable), two new card types (media, checklist), and platform-native persistence. The web overlay uses CSS `box-shadow` spread as a visual equivalent of Android's `BlendMode.Clear` punch-out.

**Tech Stack:** Kotlin/Jetpack Compose (Android), TypeScript/React Native, TypeScript/React DOM (Web), JUnit+Robolectric (Android tests), Jest+@testing-library/react-native (RN tests), Jest+jsdom (Web tests)

---

## File Map

| File | Action |
|---|---|
| `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorial.kt` | Modify: add `AzAdvanceCondition`, update `AzCard`/`AzScene`/`AzTutorial`, update DSL builders |
| `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialDataModelTest.kt` | Create: unit tests for DSL and data model |
| `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialController.kt` | Modify: add `startTutorial(variables)`, `fireEvent`, `consumeEvent`, SharedPreferences persistence |
| `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialControllerTest.kt` | Create: unit tests for controller |
| `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialOverlay.kt` | Rewrite: all four advance conditions, branching, checklist, media, auto-position |
| `aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/HelpOverlay.kt.orig` | Delete: stale backup |
| `aznavrail-react/src/types.ts` | Modify: add `AzAdvanceCondition`, update `AzCard`/`AzScene`/`AzTutorial`/`AzTutorialController` |
| `aznavrail-react/src/tutorial/AzTutorialController.tsx` | Modify: add variables, pendingEvent, fireEvent, consumeEvent, AsyncStorage persistence |
| `aznavrail-react/src/__tests__/AzTutorialController.test.tsx` | Create: RN controller tests |
| `aznavrail-react/src/components/AzTutorialOverlay.tsx` | Modify: all four advance conditions, branching, checklist, media, auto-position |
| `aznavrail-react/src/components/HelpOverlay.tsx` | Modify: fix tutorial launch flow to match Android |
| `aznavrail-react/src/__tests__/HelpOverlay.test.tsx` | Create: RN HelpOverlay tutorial launch tests |
| `aznavrail-react/src/web/AzTutorialController.tsx` | Create: React Context + Provider + localStorage |
| `aznavrail-react/src/web/AzTutorialController.test.tsx` | Create: web controller tests |
| `aznavrail-react/src/web/AzTutorialOverlay.tsx` | Create: CSS box-shadow cutout, all four advance conditions |
| `aznavrail-react/src/web/HelpOverlay.tsx` | Create: TypeScript rewrite of HelpOverlay.jsx with tutorial integration |
| `aznavrail-react/src/web/HelpOverlay.jsx` | Delete: replaced by HelpOverlay.tsx |

---

## Task 1: Android — Data Model

**Files:**
- Modify: `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorial.kt`
- Create: `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialDataModelTest.kt`

- [ ] **Step 1: Write failing tests**

Create `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialDataModelTest.kt`:

```kotlin
package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.tutorial.*
import org.junit.Assert.*
import org.junit.Test

class AzTutorialDataModelTest {

    @Test
    fun `azTutorial DSL builds scenes and cards with defaults`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "T1", text = "Body1")
            }
        }
        assertEquals(1, tutorial.scenes.size)
        assertEquals(1, tutorial.scenes[0].cards.size)
        assertEquals("T1", tutorial.scenes[0].cards[0].title)
        assertEquals(AzAdvanceCondition.Button, tutorial.scenes[0].cards[0].advanceCondition)
        assertEquals(emptyMap<String, String>(), tutorial.scenes[0].cards[0].branches)
        assertNull(tutorial.scenes[0].cards[0].mediaContent)
        assertNull(tutorial.scenes[0].cards[0].checklistItems)
    }

    @Test
    fun `card stores advanceCondition and branches`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(
                    title = "Choose",
                    text = "Pick one",
                    advanceCondition = AzAdvanceCondition.TapTarget,
                    branches = mapOf("btn-a" to "scene-a", "btn-b" to "scene-b")
                )
            }
        }
        val card = tutorial.scenes[0].cards[0]
        assertEquals(AzAdvanceCondition.TapTarget, card.advanceCondition)
        assertEquals(mapOf("btn-a" to "scene-a", "btn-b" to "scene-b"), card.branches)
    }

    @Test
    fun `card stores Event advanceCondition`() {
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "T", text = "X", advanceCondition = AzAdvanceCondition.Event("menu_opened"))
            }
        }
        val cond = tutorial.scenes[0].cards[0].advanceCondition
        assertTrue(cond is AzAdvanceCondition.Event)
        assertEquals("menu_opened", (cond as AzAdvanceCondition.Event).name)
    }

    @Test
    fun `card stores checklistItems`() {
        val items = listOf("Step 1", "Step 2")
        val tutorial = azTutorial {
            scene(id = "s1", content = {}) {
                card(title = "Check", text = "Do these:", checklistItems = items)
            }
        }
        assertEquals(items, tutorial.scenes[0].cards[0].checklistItems)
    }

    @Test
    fun `branch DSL sets branchVar and branches on scene`() {
        val tutorial = azTutorial {
            scene(id = "gate", content = {}) {
                branch(varName = "level", mapOf("advanced" to "scene-a", "basic" to "scene-b"))
            }
        }
        val scene = tutorial.scenes[0]
        assertEquals("level", scene.branchVar)
        assertEquals(mapOf("advanced" to "scene-a", "basic" to "scene-b"), scene.branches)
        assertTrue(scene.cards.isEmpty())
    }

    @Test
    fun `AzTutorial onComplete and onSkip DSL functions work`() {
        var completed = false
        var skipped = false
        val tutorial = azTutorial {
            onComplete { completed = true }
            onSkip { skipped = true }
            scene(id = "s1", content = {}) { card(title = "T", text = "X") }
        }
        tutorial.onComplete?.invoke()
        tutorial.onSkip?.invoke()
        assertTrue(completed)
        assertTrue(skipped)
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test --tests "com.hereliesaz.aznavrail.AzTutorialDataModelTest" 2>&1 | tail -20
```

Expected: FAIL — `AzAdvanceCondition`, `branch()`, `onComplete {}`, `onSkip {}` not found.

- [ ] **Step 3: Rewrite `AzTutorial.kt`**

Replace the full contents of `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorial.kt`:

```kotlin
package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

sealed class AzAdvanceCondition {
    object Button : AzAdvanceCondition()
    object TapTarget : AzAdvanceCondition()
    object TapAnywhere : AzAdvanceCondition()
    data class Event(val name: String) : AzAdvanceCondition()
}

sealed class AzHighlight {
    data class Area(val bounds: Rect) : AzHighlight()
    data class Item(val id: String) : AzHighlight()
    object FullScreen : AzHighlight()
    object None : AzHighlight()
}

data class AzCard(
    val title: String,
    val text: String,
    val highlight: AzHighlight = AzHighlight.None,
    val advanceCondition: AzAdvanceCondition = AzAdvanceCondition.Button,
    val actionText: String = "Next",
    val onAction: (() -> Unit)? = null,
    val branches: Map<String, String> = emptyMap(),
    val mediaContent: (@Composable () -> Unit)? = null,
    val checklistItems: List<String>? = null,
)

data class AzScene(
    val id: String,
    val content: @Composable () -> Unit,
    val cards: List<AzCard>,
    val onComplete: (() -> Unit)? = null,
    val branchVar: String? = null,
    val branches: Map<String, String> = emptyMap(),
)

data class AzTutorial(
    val scenes: List<AzScene>,
    val onComplete: (() -> Unit)? = null,
    val onSkip: (() -> Unit)? = null,
)

class AzTutorialBuilder {
    private val scenes = mutableListOf<AzScene>()
    private var onComplete: (() -> Unit)? = null
    private var onSkip: (() -> Unit)? = null

    fun onComplete(action: () -> Unit) { onComplete = action }
    fun onSkip(action: () -> Unit) { onSkip = action }

    fun scene(
        id: String,
        onComplete: (() -> Unit)? = null,
        content: @Composable () -> Unit,
        block: AzSceneBuilder.() -> Unit,
    ) {
        val builder = AzSceneBuilder()
        builder.block()
        scenes.add(AzScene(
            id = id,
            content = content,
            cards = builder.buildCards(),
            onComplete = onComplete,
            branchVar = builder.branchVar,
            branches = builder.branches,
        ))
    }

    fun build(): AzTutorial = AzTutorial(scenes, onComplete, onSkip)
}

class AzSceneBuilder {
    private val cards = mutableListOf<AzCard>()
    internal var branchVar: String? = null
    internal var branches: Map<String, String> = emptyMap()

    fun branch(varName: String, branches: Map<String, String>) {
        this.branchVar = varName
        this.branches = branches
    }

    fun card(
        title: String,
        text: String,
        highlight: AzHighlight = AzHighlight.None,
        advanceCondition: AzAdvanceCondition = AzAdvanceCondition.Button,
        actionText: String = "Next",
        onAction: (() -> Unit)? = null,
        branches: Map<String, String> = emptyMap(),
        mediaContent: (@Composable () -> Unit)? = null,
        checklistItems: List<String>? = null,
    ) {
        cards.add(AzCard(title, text, highlight, advanceCondition, actionText, onAction, branches, mediaContent, checklistItems))
    }

    fun buildCards(): List<AzCard> = cards
}

fun azTutorial(block: AzTutorialBuilder.() -> Unit): AzTutorial {
    val builder = AzTutorialBuilder()
    builder.block()
    return builder.build()
}
```

- [ ] **Step 4: Run tests to confirm pass**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test --tests "com.hereliesaz.aznavrail.AzTutorialDataModelTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 5: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorial.kt aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialDataModelTest.kt && git commit -m "feat(android): expand tutorial data model with advance conditions, branching, card types"
```

---

## Task 2: Android — Controller

**Files:**
- Modify: `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialController.kt`
- Create: `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialControllerTest.kt`

- [ ] **Step 1: Write failing tests**

Create `aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialControllerTest.kt`:

```kotlin
package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.tutorial.AzTutorialController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import android.content.Context

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzTutorialControllerTest {

    @Test
    fun `startTutorial with variables stores id and variables`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1", mapOf("level" to "advanced", "count" to 3))
        assertEquals("t1", controller.activeTutorialId.value)
        assertEquals("advanced", controller.currentVariables["level"])
        assertEquals(3, controller.currentVariables["count"])
    }

    @Test
    fun `startTutorial without variables defaults to empty map`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1")
        assertEquals("t1", controller.activeTutorialId.value)
        assertTrue(controller.currentVariables.isEmpty())
    }

    @Test
    fun `fireEvent sets pendingEvent`() {
        val controller = AzTutorialController()
        controller.fireEvent("menu_opened")
        assertEquals("menu_opened", controller.pendingEvent.value)
    }

    @Test
    fun `consumeEvent clears pendingEvent`() {
        val controller = AzTutorialController()
        controller.fireEvent("menu_opened")
        controller.consumeEvent()
        assertNull(controller.pendingEvent.value)
    }

    @Test
    fun `endTutorial clears activeTutorialId, variables, and pendingEvent`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1", mapOf("x" to 1))
        controller.fireEvent("ev")
        controller.endTutorial()
        assertNull(controller.activeTutorialId.value)
        assertTrue(controller.currentVariables.isEmpty())
        assertNull(controller.pendingEvent.value)
    }

    @Test
    fun `markTutorialRead writes to SharedPreferences`() {
        val prefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("az_tutorial_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val controller = AzTutorialController(prefs = prefs)
        controller.markTutorialRead("tutorial-1")
        val saved = prefs.getStringSet("az_navrail_read_tutorials", emptySet())
        assertTrue(saved!!.contains("tutorial-1"))
    }

    @Test
    fun `AzTutorialController loads read tutorials from SharedPreferences`() {
        val prefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("az_tutorial_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("az_navrail_read_tutorials", setOf("t-a", "t-b")).apply()
        val controller = AzTutorialController(
            initialReadTutorials = prefs.getStringSet("az_navrail_read_tutorials", emptySet())!!.toList(),
            prefs = prefs
        )
        assertTrue(controller.isTutorialRead("t-a"))
        assertTrue(controller.isTutorialRead("t-b"))
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test --tests "com.hereliesaz.aznavrail.AzTutorialControllerTest" 2>&1 | tail -20
```

Expected: FAIL — `currentVariables`, `pendingEvent`, `fireEvent`, `consumeEvent` not found; `startTutorial` missing `variables` parameter; `prefs` constructor parameter missing.

- [ ] **Step 3: Rewrite `AzTutorialController.kt`**

Replace the full contents of `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialController.kt`:

```kotlin
package com.hereliesaz.aznavrail.tutorial

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

private const val PREFS_NAME = "az_tutorial_prefs"
private const val PREF_KEY = "az_navrail_read_tutorials"

class AzTutorialController(
    initialActiveTutorialId: String? = null,
    initialReadTutorials: List<String> = emptyList(),
    private val prefs: SharedPreferences? = null,
) {
    private val _activeTutorialId = mutableStateOf<String?>(initialActiveTutorialId)
    val activeTutorialId: State<String?> get() = _activeTutorialId

    private val _readTutorials = mutableStateListOf<String>().apply { addAll(initialReadTutorials) }
    val readTutorials: List<String> get() = _readTutorials

    private val _currentVariables = mutableStateOf<Map<String, Any>>(emptyMap())
    val currentVariables: Map<String, Any> get() = _currentVariables.value

    private val _pendingEvent = mutableStateOf<String?>(null)
    val pendingEvent: State<String?> get() = _pendingEvent

    fun startTutorial(id: String, variables: Map<String, Any> = emptyMap()) {
        _currentVariables.value = variables
        _activeTutorialId.value = id
    }

    fun endTutorial() {
        _activeTutorialId.value = null
        _currentVariables.value = emptyMap()
        _pendingEvent.value = null
    }

    fun fireEvent(name: String) {
        _pendingEvent.value = name
    }

    fun consumeEvent() {
        _pendingEvent.value = null
    }

    fun markTutorialRead(id: String) {
        if (!_readTutorials.contains(id)) {
            _readTutorials.add(id)
            prefs?.edit()?.putStringSet(PREF_KEY, _readTutorials.toSet())?.apply()
        }
    }

    fun isTutorialRead(id: String): Boolean = _readTutorials.contains(id)

    companion object {
        fun Saver(context: Context): Saver<AzTutorialController, List<Any?>> = Saver(
            save = { listOf(it.activeTutorialId.value, ArrayList(it._readTutorials)) },
            restore = { list ->
                @Suppress("UNCHECKED_CAST")
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                AzTutorialController(
                    initialActiveTutorialId = list[0] as String?,
                    initialReadTutorials = list[1] as ArrayList<String>,
                    prefs = prefs,
                )
            }
        )
    }
}

val LocalAzTutorialController = compositionLocalOf<AzTutorialController> {
    error("AzTutorialController not provided")
}

@Composable
fun rememberAzTutorialController(): AzTutorialController {
    val context = LocalContext.current
    return rememberSaveable(saver = AzTutorialController.Saver(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIds = prefs.getStringSet(PREF_KEY, emptySet())?.toList() ?: emptyList()
        AzTutorialController(initialReadTutorials = savedIds, prefs = prefs)
    }
}
```

- [ ] **Step 4: Run tests to confirm pass**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test --tests "com.hereliesaz.aznavrail.AzTutorialControllerTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 6 tests passed.

- [ ] **Step 5: Run full Android test suite to check for regressions**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test 2>&1 | tail -15
```

Expected: BUILD SUCCESSFUL, all tests pass. Fix any failures before continuing.

- [ ] **Step 6: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialController.kt aznavrail/src/test/java/com/hereliesaz/aznavrail/AzTutorialControllerTest.kt && git commit -m "feat(android): add fireEvent, variables, SharedPreferences persistence to tutorial controller"
```

---

## Task 3: Android — Overlay

**Files:**
- Modify: `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialOverlay.kt`

The overlay is pure UI — no unit-testable logic to extract. The TDD loop here is: compile-check first, then manual verification in SampleApp.

- [ ] **Step 1: Rewrite `AzTutorialOverlay.kt`**

Replace the full contents of `aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialOverlay.kt`:

```kotlin
package com.hereliesaz.aznavrail.tutorial

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val TAG = "AzTutorialOverlay"

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AzTutorialOverlay(
    tutorialId: String,
    tutorial: AzTutorial,
    onDismiss: () -> Unit,
    itemBoundsCache: Map<String, Rect> = emptyMap(),
) {
    val tutorialController = LocalAzTutorialController.current
    val variables = tutorialController.currentVariables
    val pendingEvent by tutorialController.pendingEvent
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    var currentSceneIndex by remember { mutableStateOf(0) }
    var currentCardIndex by remember { mutableStateOf(0) }
    var checkedIndices by remember { mutableStateOf(setOf<Int>()) }
    val visitedSceneIds = remember { mutableSetOf<String>() }

    fun indexOfScene(id: String) = tutorial.scenes.indexOfFirst { it.id == id }

    fun advanceCard() {
        checkedIndices = emptySet()
        val scene = tutorial.scenes[currentSceneIndex]
        if (currentCardIndex + 1 >= scene.cards.size) {
            scene.onComplete?.invoke()
            currentSceneIndex++
            currentCardIndex = 0
        } else {
            currentCardIndex++
        }
    }

    fun navigateToScene(id: String) {
        val idx = indexOfScene(id)
        if (idx != -1) {
            currentSceneIndex = idx
            currentCardIndex = 0
            checkedIndices = emptySet()
        } else {
            Log.w(TAG, "Scene '$id' not found in tutorial '$tutorialId'")
        }
    }

    // Variable branching: evaluate on every scene change
    LaunchedEffect(currentSceneIndex) {
        if (currentSceneIndex >= tutorial.scenes.size) return@LaunchedEffect
        val scene = tutorial.scenes[currentSceneIndex]
        val bv = scene.branchVar
        if (bv != null && scene.branches.isNotEmpty()) {
            val varValue = variables[bv]?.toString()
            val targetId = varValue?.let { scene.branches[it] }
            if (targetId != null) {
                if (visitedSceneIds.contains(targetId)) {
                    Log.w(TAG, "Circular branch detected at scene '${scene.id}' → '$targetId', advancing linearly")
                    val next = currentSceneIndex + 1
                    if (next >= tutorial.scenes.size) {
                        tutorialController.markTutorialRead(tutorialId)
                        tutorial.onComplete?.invoke()
                        onDismiss()
                    } else {
                        currentSceneIndex = next
                        currentCardIndex = 0
                        checkedIndices = emptySet()
                    }
                } else {
                    visitedSceneIds.add(scene.id)
                    navigateToScene(targetId)
                }
                return@LaunchedEffect
            }
        }
        visitedSceneIds.add(scene.id)
    }

    // Tutorial completion
    if (currentSceneIndex >= tutorial.scenes.size) {
        LaunchedEffect(Unit) {
            tutorialController.markTutorialRead(tutorialId)
            tutorial.onComplete?.invoke()
            onDismiss()
        }
        return
    }

    val currentScene = tutorial.scenes[currentSceneIndex]
    val currentCard = currentScene.cards.getOrNull(currentCardIndex) ?: return

    // Event-driven advance
    LaunchedEffect(pendingEvent) {
        val cond = currentCard.advanceCondition
        if (pendingEvent != null && cond is AzAdvanceCondition.Event && cond.name == pendingEvent) {
            tutorialController.consumeEvent()
            advanceCard()
        }
    }

    val highlightBounds: Rect? = remember(currentCard.highlight, itemBoundsCache) {
        when (val h = currentCard.highlight) {
            is AzHighlight.Area -> h.bounds
            is AzHighlight.Item -> itemBoundsCache[h.id]
            else -> null
        }
    }
    val isFullScreen = currentCard.highlight is AzHighlight.FullScreen
    val isTapAnywhere = currentCard.advanceCondition is AzAdvanceCondition.TapAnywhere
    val isTapTarget = currentCard.advanceCondition is AzAdvanceCondition.TapTarget
    val highlightItemId = (currentCard.highlight as? AzHighlight.Item)?.id

    // Card auto-position: float to top if highlight is in the lower 60% of the screen
    val cardAlignment = remember(highlightBounds, density, screenHeightDp) {
        val highlightCenterYDp = highlightBounds?.let { with(density) { it.center.y.toDp().value } } ?: 0f
        if (highlightCenterYDp > screenHeightDp * 0.6f) Alignment.TopCenter else Alignment.BottomCenter
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Scene content
        currentScene.content()

        // 2. Dimmed overlay (with punch-out for non-fullscreen highlights)
        if (!isFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawBehind {
                        drawRect(color = Color.Black.copy(alpha = 0.7f))
                        if (highlightBounds != null) {
                            drawRoundRect(
                                color = Color.Transparent,
                                topLeft = Offset(highlightBounds.left, highlightBounds.top),
                                size = Size(highlightBounds.width, highlightBounds.height),
                                cornerRadius = CornerRadius(16.dp.toPx()),
                                blendMode = BlendMode.Clear,
                            )
                        }
                    }
                    .then(if (isTapAnywhere) Modifier.clickable { advanceCard() } else Modifier)
            )
        }

        // 3. TapTarget clickable box over the punch-out
        // If highlight is not AzHighlight.Item, TapTarget degrades to TapAnywhere (handled above by full overlay click)
        if (isTapTarget && highlightBounds != null && highlightItemId != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = with(density) { highlightBounds.left.toDp() },
                        y = with(density) { highlightBounds.top.toDp() },
                    )
                    .size(
                        width = with(density) { highlightBounds.width.toDp() },
                        height = with(density) { highlightBounds.height.toDp() },
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val targetSceneId = currentCard.branches[highlightItemId]
                        if (targetSceneId != null) navigateToScene(targetSceneId)
                        else advanceCard()
                    }
            )
        } else if (isTapTarget && highlightItemId == null) {
            // TapTarget without an Item highlight → degrade to TapAnywhere
            Box(modifier = Modifier.fillMaxSize().clickable { advanceCard() })
        }

        // 4. Card UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = cardAlignment,
        ) {
            AnimatedContent(
                targetState = currentCard,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(300)))
                },
                label = "TutorialCardTransition",
            ) { card ->
                val showButton = card.advanceCondition is AzAdvanceCondition.Button || card.checklistItems != null
                val allChecked = card.checklistItems == null || checkedIndices.size == card.checklistItems.size

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Media content
                    card.mediaContent?.let { media ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        ) { media() }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = card.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Checklist
                    card.checklistItems?.let { items ->
                        Spacer(modifier = Modifier.height(16.dp))
                        items.forEachIndexed { idx, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        checkedIndices = if (checkedIndices.contains(idx))
                                            checkedIndices - idx else checkedIndices + idx
                                    }
                                    .padding(vertical = 4.dp),
                            ) {
                                Checkbox(
                                    checked = checkedIndices.contains(idx),
                                    onCheckedChange = { checked ->
                                        checkedIndices = if (checked) checkedIndices + idx else checkedIndices - idx
                                    },
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (showButton) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(onClick = {
                                tutorialController.markTutorialRead(tutorialId)
                                tutorial.onSkip?.invoke()
                                onDismiss()
                            }) { Text("Skip Tutorial") }

                            Button(
                                onClick = { card.onAction?.invoke(); advanceCard() },
                                enabled = allChecked,
                            ) { Text(card.actionText) }
                        }
                    } else {
                        // For TapTarget / TapAnywhere / Event conditions, show only Skip
                        TextButton(
                            onClick = {
                                tutorialController.markTutorialRead(tutorialId)
                                tutorial.onSkip?.invoke()
                                onDismiss()
                            },
                            modifier = Modifier.align(Alignment.Start),
                        ) { Text("Skip Tutorial") }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify Android compiles**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL. Fix any compile errors before continuing.

- [ ] **Step 3: Run full Android test suite**

```bash
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test 2>&1 | tail -15
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail/src/main/java/com/hereliesaz/aznavrail/tutorial/AzTutorialOverlay.kt && git commit -m "feat(android): rewrite overlay with advance conditions, branching, checklist, media, auto-position"
```

---

## Task 4: Android — Cleanup

**Files:**
- Delete: `aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/HelpOverlay.kt.orig`

- [ ] **Step 1: Delete the stale backup**

```bash
rm /run/media/az/0722-7C65/AzNavRail/aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/HelpOverlay.kt.orig
```

- [ ] **Step 2: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git rm aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/HelpOverlay.kt.orig && git commit -m "chore(android): delete stale HelpOverlay.kt.orig backup"
```

---

## Task 5: React/TypeScript — Shared Types

**Files:**
- Modify: `aznavrail-react/src/types.ts`

- [ ] **Step 1: Update `types.ts`**

Find the existing `AzCard`, `AzScene`, `AzTutorial`, and `AzTutorialController` interfaces and replace them. Also add `AzAdvanceCondition`. The surrounding file content is unchanged.

Replace the block from `export type AzHighlight` through `export interface AzTutorialController {` with:

```typescript
export type AzHighlight =
  | { type: 'Area'; bounds: { x: number; y: number; width: number; height: number } }
  | { type: 'Item'; id: string }
  | { type: 'FullScreen' }
  | { type: 'None' };

export type AzAdvanceCondition =
  | { type: 'Button' }
  | { type: 'TapTarget' }
  | { type: 'TapAnywhere' }
  | { type: 'Event'; name: string };

export interface AzCard {
  title: string;
  text: string;
  highlight?: AzHighlight;
  advanceCondition?: AzAdvanceCondition;
  actionText?: string;
  onAction?: () => void;
  branches?: Record<string, string>;
  mediaContent?: () => React.ReactNode;
  checklistItems?: string[];
}

export interface AzScene {
  id: string;
  content: () => React.ReactNode;
  cards: AzCard[];
  onComplete?: () => void;
  branchVar?: string;
  branches?: Record<string, string>;
}

export interface AzTutorial {
  scenes: AzScene[];
  onComplete?: () => void;
  onSkip?: () => void;
}

export interface AzTutorialController {
  activeTutorialId: string | null;
  readTutorials: string[];
  currentVariables: Record<string, any>;
  pendingEvent: string | null;
  startTutorial: (id: string, variables?: Record<string, any>) => void;
  endTutorial: () => void;
  markTutorialRead: (id: string) => void;
  isTutorialRead: (id: string) => boolean;
  fireEvent: (name: string) => void;
  consumeEvent: () => void;
}
```

- [ ] **Step 2: Typecheck**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck 2>&1 | tail -20
```

Expected: No errors. Fix any type errors surfaced by the new interface before continuing — they indicate places where existing code needs updating.

- [ ] **Step 3: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/types.ts && git commit -m "feat(react): add AzAdvanceCondition, expand AzCard/AzScene/AzTutorial/AzTutorialController types"
```

---

## Task 6: React Native — Controller

**Files:**
- Modify: `aznavrail-react/src/tutorial/AzTutorialController.tsx`
- Create: `aznavrail-react/src/__tests__/AzTutorialController.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `aznavrail-react/src/__tests__/AzTutorialController.test.tsx`:

```typescript
import React from 'react';
import { render, act } from '@testing-library/react-native';
import { AzTutorialProvider, useAzTutorialController } from '../tutorial/AzTutorialController';

let capturedController: ReturnType<typeof useAzTutorialController> | null = null;

const Capture = () => {
  capturedController = useAzTutorialController();
  return null;
};

const wrap = () =>
  render(
    <AzTutorialProvider>
      <Capture />
    </AzTutorialProvider>
  );

beforeEach(() => { capturedController = null; });

describe('AzTutorialController', () => {
  it('startTutorial stores id and variables', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1', { level: 'advanced' }); });
    expect(capturedController!.activeTutorialId).toBe('t1');
    expect(capturedController!.currentVariables.level).toBe('advanced');
  });

  it('startTutorial without variables defaults to empty', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1'); });
    expect(capturedController!.currentVariables).toEqual({});
  });

  it('fireEvent sets pendingEvent', () => {
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    expect(capturedController!.pendingEvent).toBe('menu_opened');
  });

  it('consumeEvent clears pendingEvent', () => {
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    act(() => { capturedController!.consumeEvent(); });
    expect(capturedController!.pendingEvent).toBeNull();
  });

  it('endTutorial clears all transient state', () => {
    wrap();
    act(() => { capturedController!.startTutorial('t1', { x: 1 }); capturedController!.fireEvent('ev'); });
    act(() => { capturedController!.endTutorial(); });
    expect(capturedController!.activeTutorialId).toBeNull();
    expect(capturedController!.currentVariables).toEqual({});
    expect(capturedController!.pendingEvent).toBeNull();
  });
});
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="AzTutorialController" 2>&1 | tail -20
```

Expected: FAIL — `currentVariables`, `pendingEvent`, `fireEvent`, `consumeEvent` not found on controller.

- [ ] **Step 3: Rewrite `AzTutorialController.tsx`**

Replace the full contents of `aznavrail-react/src/tutorial/AzTutorialController.tsx`:

```typescript
import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import { AzTutorialController } from '../types';

// Optional AsyncStorage — used if installed, no-op otherwise
let AsyncStorage: { getItem: (k: string) => Promise<string | null>; setItem: (k: string, v: string) => Promise<void> } | null = null;
try {
  AsyncStorage = require('@react-native-async-storage/async-storage').default;
} catch {}

const STORAGE_KEY = 'az_navrail_read_tutorials';

export const AzTutorialContext = createContext<AzTutorialController | null>(null);

interface AzTutorialProviderProps {
  children: React.ReactNode;
  initialActiveTutorialId?: string | null;
  initialReadTutorials?: string[];
}

export const AzTutorialProvider: React.FC<AzTutorialProviderProps> = ({
  children,
  initialActiveTutorialId = null,
  initialReadTutorials = [],
}) => {
  const [activeTutorialId, setActiveTutorialId] = useState<string | null>(initialActiveTutorialId);
  const [readTutorials, setReadTutorials] = useState<string[]>(initialReadTutorials);
  const [currentVariables, setCurrentVariables] = useState<Record<string, any>>({});
  const [pendingEvent, setPendingEvent] = useState<string | null>(null);

  // Load persisted read tutorials on mount
  useEffect(() => {
    if (!AsyncStorage) return;
    AsyncStorage.getItem(STORAGE_KEY).then((raw) => {
      if (raw) {
        try {
          const ids: string[] = JSON.parse(raw);
          setReadTutorials((prev) => {
            const merged = Array.from(new Set([...prev, ...ids]));
            return merged.length === prev.length ? prev : merged;
          });
        } catch {}
      }
    });
  }, []);

  const startTutorial = useCallback((id: string, variables: Record<string, any> = {}) => {
    setCurrentVariables(variables);
    setActiveTutorialId(id);
  }, []);

  const endTutorial = useCallback(() => {
    setActiveTutorialId(null);
    setCurrentVariables({});
    setPendingEvent(null);
  }, []);

  const markTutorialRead = useCallback((id: string) => {
    setReadTutorials((prev) => {
      if (prev.includes(id)) return prev;
      const next = [...prev, id];
      AsyncStorage?.setItem(STORAGE_KEY, JSON.stringify(next)).catch(() => {});
      return next;
    });
  }, []);

  const isTutorialRead = useCallback(
    (id: string) => readTutorials.includes(id),
    [readTutorials]
  );

  const fireEvent = useCallback((name: string) => { setPendingEvent(name); }, []);
  const consumeEvent = useCallback(() => { setPendingEvent(null); }, []);

  const contextValue = useMemo<AzTutorialController>(
    () => ({
      activeTutorialId,
      readTutorials,
      currentVariables,
      pendingEvent,
      startTutorial,
      endTutorial,
      markTutorialRead,
      isTutorialRead,
      fireEvent,
      consumeEvent,
    }),
    [activeTutorialId, readTutorials, currentVariables, pendingEvent,
     startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent]
  );

  return (
    <AzTutorialContext.Provider value={contextValue}>
      {children}
    </AzTutorialContext.Provider>
  );
};

export const useAzTutorialController = (): AzTutorialController => {
  const context = useContext(AzTutorialContext);
  if (!context) {
    throw new Error('useAzTutorialController must be used within an AzTutorialProvider');
  }
  return context;
};
```

- [ ] **Step 4: Run tests to confirm pass**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="AzTutorialController" 2>&1 | tail -10
```

Expected: All 5 tests pass.

- [ ] **Step 5: Typecheck**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck 2>&1 | tail -10
```

Expected: No errors.

- [ ] **Step 6: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/tutorial/AzTutorialController.tsx aznavrail-react/src/__tests__/AzTutorialController.test.tsx && git commit -m "feat(react-native): add variables, pendingEvent, fireEvent, AsyncStorage persistence to controller"
```

---

## Task 7: React Native — Overlay

**Files:**
- Modify: `aznavrail-react/src/components/AzTutorialOverlay.tsx`

- [ ] **Step 1: Rewrite `AzTutorialOverlay.tsx`**

Replace the full contents of `aznavrail-react/src/components/AzTutorialOverlay.tsx`:

```typescript
import React, { useState, useMemo, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity, ScrollView, Dimensions,
} from 'react-native';
import { AzTutorial } from '../types';
import { useAzTutorialController } from '../tutorial/AzTutorialController';

interface AzTutorialOverlayProps {
  tutorialId: string;
  tutorial: AzTutorial;
  onDismiss: () => void;
  itemBoundsCache: Record<string, { x: number; y: number; width: number; height: number }>;
}

const TAG = 'AzTutorialOverlay';

export const AzTutorialOverlay: React.FC<AzTutorialOverlayProps> = ({
  tutorialId, tutorial, onDismiss, itemBoundsCache,
}) => {
  const [currentSceneIndex, setCurrentSceneIndex] = useState(0);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [checkedIndices, setCheckedIndices] = useState<Set<number>>(new Set());
  const visitedSceneIds = React.useRef<Set<string>>(new Set());
  const tutorialController = useAzTutorialController();
  const { currentVariables, pendingEvent } = tutorialController;
  const screenHeight = Dimensions.get('window').height;

  const indexOfScene = useCallback(
    (id: string) => tutorial.scenes.findIndex((s) => s.id === id),
    [tutorial.scenes]
  );

  const navigateToScene = useCallback((id: string) => {
    const idx = indexOfScene(id);
    if (idx !== -1) {
      setCurrentSceneIndex(idx);
      setCurrentCardIndex(0);
      setCheckedIndices(new Set());
    } else {
      console.warn(`[${TAG}] Scene '${id}' not found`);
    }
  }, [indexOfScene]);

  const advanceCard = useCallback(() => {
    setCheckedIndices(new Set());
    const scene = tutorial.scenes[currentSceneIndex];
    if (!scene) return;
    if (currentCardIndex + 1 >= scene.cards.length) {
      scene.onComplete?.();
      setCurrentSceneIndex((prev) => prev + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex((prev) => prev + 1);
    }
  }, [tutorial.scenes, currentSceneIndex, currentCardIndex]);

  // Variable branching on scene change
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) return;
    const scene = tutorial.scenes[currentSceneIndex];
    const bv = scene.branchVar;
    if (bv && scene.branches && Object.keys(scene.branches).length > 0) {
      const varValue = String(currentVariables[bv] ?? '');
      const targetId = scene.branches[varValue];
      if (targetId) {
        if (visitedSceneIds.current.has(targetId)) {
          console.warn(`[${TAG}] Circular branch at '${scene.id}' → '${targetId}', advancing linearly`);
          const next = currentSceneIndex + 1;
          if (next >= tutorial.scenes.length) {
            tutorialController.markTutorialRead(tutorialId);
            tutorial.onComplete?.();
            onDismiss();
          } else {
            setCurrentSceneIndex(next);
            setCurrentCardIndex(0);
            setCheckedIndices(new Set());
          }
        } else {
          visitedSceneIds.current.add(scene.id);
          navigateToScene(targetId);
        }
        return;
      }
    }
    visitedSceneIds.current.add(scene.id);
  }, [currentSceneIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  // Tutorial completion
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) {
      tutorialController.markTutorialRead(tutorialId);
      tutorial.onComplete?.();
      onDismiss();
    }
  }, [currentSceneIndex, tutorial.scenes.length]); // eslint-disable-line react-hooks/exhaustive-deps

  // Event-driven advance
  useEffect(() => {
    if (!pendingEvent || currentSceneIndex >= tutorial.scenes.length) return;
    const scene = tutorial.scenes[currentSceneIndex];
    const card = scene?.cards[currentCardIndex];
    if (!card) return;
    const cond = card.advanceCondition;
    if (cond?.type === 'Event' && cond.name === pendingEvent) {
      tutorialController.consumeEvent();
      advanceCard();
    }
  }, [pendingEvent]); // eslint-disable-line react-hooks/exhaustive-deps

  if (currentSceneIndex >= tutorial.scenes.length) return null;

  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene?.cards[currentCardIndex];
  if (!currentCard) return null;

  const highlightBounds = (() => {
    const h = currentCard.highlight;
    if (!h) return null;
    if (h.type === 'Area') return h.bounds;
    if (h.type === 'Item') return itemBoundsCache[h.id] ?? null;
    return null;
  })();

  const isFullScreen = currentCard.highlight?.type === 'FullScreen';
  const isTapAnywhere = currentCard.advanceCondition?.type === 'TapAnywhere';
  const isTapTarget = currentCard.advanceCondition?.type === 'TapTarget';
  const highlightItemId = currentCard.highlight?.type === 'Item' ? currentCard.highlight.id : null;

  const highlightCenterY = highlightBounds
    ? highlightBounds.y + highlightBounds.height / 2
    : 0;
  const cardAtTop = highlightCenterY > screenHeight * 0.6;

  const showButton =
    !currentCard.advanceCondition ||
    currentCard.advanceCondition.type === 'Button' ||
    !!currentCard.checklistItems;
  const allChecked =
    !currentCard.checklistItems ||
    checkedIndices.size === currentCard.checklistItems.length;

  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    tutorial.onSkip?.();
    onDismiss();
  };

  const handleTapTarget = () => {
    if (highlightItemId) {
      const targetSceneId = currentCard.branches?.[highlightItemId];
      if (targetSceneId) { navigateToScene(targetSceneId); return; }
    }
    advanceCard();
  };

  const toggleCheck = (idx: number) => {
    setCheckedIndices((prev) => {
      const next = new Set(prev);
      if (next.has(idx)) next.delete(idx); else next.add(idx);
      return next;
    });
  };

  return (
    <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
      {/* 1. Scene content */}
      <View style={StyleSheet.absoluteFill} pointerEvents="box-none">
        {currentScene.content()}
      </View>

      {/* 2. Dimmed overlay + cutout */}
      {!isFullScreen && (
        <TouchableOpacity
          style={styles.overlayMask}
          activeOpacity={1}
          onPress={isTapAnywhere ? advanceCard : undefined}
          pointerEvents={isTapAnywhere ? 'auto' : 'box-none'}
        >
          {highlightBounds && (
            <View
              style={[styles.cutoutOuter, {
                left: highlightBounds.x, top: highlightBounds.y,
                width: highlightBounds.width, height: highlightBounds.height,
              }]}
              pointerEvents="none"
            >
              <View style={styles.cutoutInner} />
            </View>
          )}
        </TouchableOpacity>
      )}

      {/* 3. TapTarget clickable area */}
      {isTapTarget && highlightBounds && highlightItemId && (
        <TouchableOpacity
          style={[styles.tapTargetArea, {
            left: highlightBounds.x, top: highlightBounds.y,
            width: highlightBounds.width, height: highlightBounds.height,
          }]}
          onPress={handleTapTarget}
          activeOpacity={0.8}
        />
      )}
      {isTapTarget && !highlightItemId && (
        <TouchableOpacity style={StyleSheet.absoluteFill} onPress={advanceCard} activeOpacity={1} />
      )}

      {/* 4. Card UI */}
      <View
        style={[styles.cardContainer, cardAtTop ? styles.cardTop : styles.cardBottom]}
        pointerEvents="box-none"
      >
        <View style={styles.card}>
          <Text style={styles.cardTitle}>{currentCard.title}</Text>

          {currentCard.mediaContent && (
            <View style={styles.mediaContainer}>
              {currentCard.mediaContent()}
            </View>
          )}

          <Text style={styles.cardText}>{currentCard.text}</Text>

          {currentCard.checklistItems && (
            <View style={styles.checklist}>
              {currentCard.checklistItems.map((item, idx) => (
                <TouchableOpacity
                  key={idx}
                  style={styles.checklistRow}
                  onPress={() => toggleCheck(idx)}
                  activeOpacity={0.7}
                >
                  <View style={[styles.checkbox, checkedIndices.has(idx) && styles.checkboxChecked]}>
                    {checkedIndices.has(idx) && <Text style={styles.checkmark}>✓</Text>}
                  </View>
                  <Text style={styles.checklistText}>{item}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          <View style={styles.buttonRow}>
            <TouchableOpacity onPress={handleSkip} style={styles.skipButton}>
              <Text style={styles.skipButtonText}>Skip Tutorial</Text>
            </TouchableOpacity>
            {showButton && (
              <TouchableOpacity
                onPress={() => { currentCard.onAction?.(); advanceCard(); }}
                style={[styles.actionButton, !allChecked && styles.actionButtonDisabled]}
                disabled={!allChecked}
              >
                <Text style={styles.actionButtonText}>{currentCard.actionText || 'Next'}</Text>
              </TouchableOpacity>
            )}
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  overlayMask: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9998,
    overflow: 'hidden',
  },
  cutoutOuter: {
    position: 'absolute',
    borderRadius: 16,
    overflow: 'hidden',
  },
  cutoutInner: {
    ...StyleSheet.absoluteFillObject,
    borderRadius: 16,
    borderColor: 'rgba(0,0,0,0.7)',
    borderWidth: 9999,
    margin: -9999,
  },
  tapTargetArea: {
    position: 'absolute',
    borderRadius: 16,
    zIndex: 9999,
  },
  cardContainer: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    padding: 32,
    zIndex: 10000,
  },
  cardTop: { justifyContent: 'flex-start' },
  cardBottom: { justifyContent: 'flex-end' },
  card: {
    width: '85%',
    maxWidth: 420,
    backgroundColor: '#fff',
    borderRadius: 16,
    padding: 24,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  cardTitle: { fontSize: 20, fontWeight: 'bold', color: '#000', marginBottom: 12 },
  mediaContainer: {
    width: '100%',
    height: 120,
    borderRadius: 8,
    overflow: 'hidden',
    marginBottom: 12,
  },
  cardText: { fontSize: 16, color: '#333' },
  checklist: { marginTop: 16 },
  checklistRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  checkbox: {
    width: 22, height: 22, borderRadius: 4,
    borderWidth: 2, borderColor: '#6200EE',
    justifyContent: 'center', alignItems: 'center', marginRight: 10,
  },
  checkboxChecked: { backgroundColor: '#6200EE' },
  checkmark: { color: '#fff', fontSize: 14, fontWeight: 'bold' },
  checklistText: { fontSize: 14, color: '#333', flex: 1 },
  buttonRow: {
    flexDirection: 'row', justifyContent: 'space-between',
    alignItems: 'center', marginTop: 24,
  },
  skipButton: { padding: 8 },
  skipButtonText: { color: '#6200EE', fontSize: 14, fontWeight: '600' },
  actionButton: {
    backgroundColor: '#6200EE',
    paddingHorizontal: 24, paddingVertical: 10, borderRadius: 20,
  },
  actionButtonDisabled: { backgroundColor: '#ccc' },
  actionButtonText: { color: '#fff', fontSize: 14, fontWeight: '600' },
});
```

- [ ] **Step 2: Typecheck**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck 2>&1 | tail -10
```

Expected: No errors.

- [ ] **Step 3: Run full RN test suite**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test 2>&1 | tail -15
```

Expected: All tests pass.

- [ ] **Step 4: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/components/AzTutorialOverlay.tsx && git commit -m "feat(react-native): rewrite overlay with advance conditions, branching, checklist, media, auto-position"
```

---

## Task 8: React Native — HelpOverlay Fix

**Files:**
- Modify: `aznavrail-react/src/components/HelpOverlay.tsx`
- Create: `aznavrail-react/src/__tests__/HelpOverlay.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `aznavrail-react/src/__tests__/HelpOverlay.test.tsx`:

```typescript
import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { HelpOverlay } from '../components/HelpOverlay';
import { AzTutorialProvider } from '../tutorial/AzTutorialController';
import { AzNavItem } from '../types';

const mockItem: AzNavItem = {
  id: 'item-1', text: 'Home', isRailItem: true,
  isToggle: false, toggleOnText: '', toggleOffText: '',
  isCycler: false, isDivider: false, collapseOnClick: false,
  shape: 'CIRCLE' as any, disabled: false,
  isHost: false, isSubItem: false, isExpanded: false,
  info: 'This is the home button.',
};

const renderWithProvider = (ui: React.ReactElement) =>
  render(<AzTutorialProvider>{ui}</AzTutorialProvider>);

describe('HelpOverlay tutorial launch flow', () => {
  it('shows Tutorial available hint on collapsed card when tutorial exists', () => {
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    expect(getByText('Tutorial available')).toBeTruthy();
  });

  it('does NOT show Start Tutorial button on collapsed card', () => {
    const { queryByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    expect(queryByText('Start Tutorial')).toBeNull();
  });

  it('shows Start Tutorial button after expanding card', () => {
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    fireEvent.press(getByText('Home'));
    expect(getByText('Start Tutorial')).toBeTruthy();
  });

  it('Start Tutorial button calls onDismiss', () => {
    const onDismiss = jest.fn();
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={onDismiss}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    fireEvent.press(getByText('Home'));
    fireEvent.press(getByText('Start Tutorial'));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });
});
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="__tests__/HelpOverlay" 2>&1 | tail -20
```

Expected: FAIL — "Tutorial available" hint not shown, "Start Tutorial" not found.

- [ ] **Step 3: Fix `HelpOverlay.tsx`**

In `aznavrail-react/src/components/HelpOverlay.tsx`, replace the card's `onPress` handler and inner content. Find the `TouchableOpacity` that wraps each card (lines ~113–165 of the current file) and replace it with:

```typescript
<TouchableOpacity
    key={i.id}
    style={styles.card}
    activeOpacity={0.8}
    onPress={() => setExpandedItemId(isExpanded ? null : i.id)}
    onLayout={(e) => {
        const layout = e.nativeEvent.layout;
        setCardBounds(prev => ({ ...prev, [i.id]: layout }));
    }}
>
    <Text style={styles.cardTitle}>{titleText}</Text>
    {infoText && (
        <Text style={styles.cardText} numberOfLines={isExpanded ? undefined : 1}>
            {infoText}
        </Text>
    )}
    {listText && (
        <Text
            style={[styles.cardText, infoText ? { marginTop: 8 } : {}]}
            numberOfLines={isExpanded ? undefined : 1}
        >
            {listText}
        </Text>
    )}
    {hasTutorial && !isExpanded && (
        <Text style={styles.tutorialHint}>Tutorial available</Text>
    )}
    {hasTutorial && isExpanded && (
        <TouchableOpacity
            style={styles.startTutorialButton}
            onPress={() => {
                tutorialController.startTutorial(i.id);
                onDismiss();
            }}
        >
            <Text style={styles.startTutorialText}>Start Tutorial</Text>
        </TouchableOpacity>
    )}
    {isExpanded && !hasTutorial && (
        <Text style={styles.tapToCollapse}>Tap to collapse</Text>
    )}
</TouchableOpacity>
```

Also add these entries to the `StyleSheet.create({})` at the bottom of the file:

```typescript
tutorialHint: {
    color: '#aaa',
    fontSize: 11,
    marginTop: 6,
    fontStyle: 'italic',
},
startTutorialButton: {
    marginTop: 12,
    backgroundColor: '#6200EE',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 16,
    alignSelf: 'flex-start',
},
startTutorialText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: '600',
},
```

- [ ] **Step 4: Run tests to confirm pass**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="__tests__/HelpOverlay" 2>&1 | tail -10
```

Expected: All 4 tests pass.

- [ ] **Step 5: Run full test suite**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test 2>&1 | tail -15
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/components/HelpOverlay.tsx aznavrail-react/src/__tests__/HelpOverlay.test.tsx && git commit -m "fix(react-native): align help overlay tutorial launch flow with Android reference behavior"
```

---

## Task 9: Web — Tutorial Controller

**Files:**
- Create: `aznavrail-react/src/web/AzTutorialController.tsx`
- Create: `aznavrail-react/src/web/AzTutorialController.test.tsx`

- [ ] **Step 1: Write failing tests**

Create `aznavrail-react/src/web/AzTutorialController.test.tsx`:

```typescript
/**
 * @jest-environment jsdom
 */
import React from 'react';
import { render, act } from '@testing-library/react';
import { AzWebTutorialProvider, useAzWebTutorialController } from './AzTutorialController';

let ctrl: ReturnType<typeof useAzWebTutorialController> | null = null;

const Capture = () => {
  ctrl = useAzWebTutorialController();
  return null;
};

const wrap = () =>
  render(<AzWebTutorialProvider><Capture /></AzWebTutorialProvider>);

beforeEach(() => {
  ctrl = null;
  localStorage.clear();
});

describe('AzWebTutorialController', () => {
  it('startTutorial stores id and variables', () => {
    wrap();
    act(() => { ctrl!.startTutorial('t1', { level: 'advanced' }); });
    expect(ctrl!.activeTutorialId).toBe('t1');
    expect(ctrl!.currentVariables.level).toBe('advanced');
  });

  it('fireEvent sets pendingEvent', () => {
    wrap();
    act(() => { ctrl!.fireEvent('menu_opened'); });
    expect(ctrl!.pendingEvent).toBe('menu_opened');
  });

  it('consumeEvent clears pendingEvent', () => {
    wrap();
    act(() => { ctrl!.fireEvent('ev'); });
    act(() => { ctrl!.consumeEvent(); });
    expect(ctrl!.pendingEvent).toBeNull();
  });

  it('markTutorialRead persists to localStorage', () => {
    wrap();
    act(() => { ctrl!.markTutorialRead('t1'); });
    const stored = JSON.parse(localStorage.getItem('az_navrail_read_tutorials') ?? '[]');
    expect(stored).toContain('t1');
  });

  it('loads persisted read tutorials from localStorage on init', () => {
    localStorage.setItem('az_navrail_read_tutorials', JSON.stringify(['t-prev']));
    wrap();
    expect(ctrl!.isTutorialRead('t-prev')).toBe(true);
  });
});
```

- [ ] **Step 2: Run to confirm failure**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="web/AzTutorialController" 2>&1 | tail -20
```

Expected: FAIL — `AzWebTutorialProvider` not found.

- [ ] **Step 3: Create `AzTutorialController.tsx`**

Create `aznavrail-react/src/web/AzTutorialController.tsx`:

```typescript
import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { AzTutorialController } from '../types';

const STORAGE_KEY = 'az_navrail_read_tutorials';

function loadFromStorage(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as string[];
  } catch {}
  return [];
}

function saveToStorage(ids: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  } catch {}
}

const AzWebTutorialContext = createContext<AzTutorialController | null>(null);

interface AzWebTutorialProviderProps {
  children: React.ReactNode;
}

export const AzWebTutorialProvider: React.FC<AzWebTutorialProviderProps> = ({ children }) => {
  const [activeTutorialId, setActiveTutorialId] = useState<string | null>(null);
  const [readTutorials, setReadTutorials] = useState<string[]>(() => loadFromStorage());
  const [currentVariables, setCurrentVariables] = useState<Record<string, any>>({});
  const [pendingEvent, setPendingEvent] = useState<string | null>(null);

  const startTutorial = useCallback((id: string, variables: Record<string, any> = {}) => {
    setCurrentVariables(variables);
    setActiveTutorialId(id);
  }, []);

  const endTutorial = useCallback(() => {
    setActiveTutorialId(null);
    setCurrentVariables({});
    setPendingEvent(null);
  }, []);

  const markTutorialRead = useCallback((id: string) => {
    setReadTutorials((prev) => {
      if (prev.includes(id)) return prev;
      const next = [...prev, id];
      saveToStorage(next);
      return next;
    });
  }, []);

  const isTutorialRead = useCallback(
    (id: string) => readTutorials.includes(id),
    [readTutorials]
  );

  const fireEvent = useCallback((name: string) => { setPendingEvent(name); }, []);
  const consumeEvent = useCallback(() => { setPendingEvent(null); }, []);

  const value = useMemo<AzTutorialController>(
    () => ({
      activeTutorialId, readTutorials, currentVariables, pendingEvent,
      startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent,
    }),
    [activeTutorialId, readTutorials, currentVariables, pendingEvent,
     startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent]
  );

  return (
    <AzWebTutorialContext.Provider value={value}>
      {children}
    </AzWebTutorialContext.Provider>
  );
};

export const useAzWebTutorialController = (): AzTutorialController => {
  const ctx = useContext(AzWebTutorialContext);
  if (!ctx) throw new Error('useAzWebTutorialController must be used within AzWebTutorialProvider');
  return ctx;
};
```

- [ ] **Step 4: Run tests to confirm pass**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test --testPathPattern="web/AzTutorialController" 2>&1 | tail -10
```

Expected: All 5 tests pass.

- [ ] **Step 5: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/web/AzTutorialController.tsx aznavrail-react/src/web/AzTutorialController.test.tsx && git commit -m "feat(web): add AzWebTutorialController with localStorage persistence"
```

---

## Task 10: Web — Tutorial Overlay

**Files:**
- Create: `aznavrail-react/src/web/AzTutorialOverlay.tsx`

- [ ] **Step 1: Create `AzTutorialOverlay.tsx`**

Create `aznavrail-react/src/web/AzTutorialOverlay.tsx`:

```typescript
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { AzTutorial } from '../types';
import { useAzWebTutorialController } from './AzTutorialController';

interface AzWebTutorialOverlayProps {
  tutorialId: string;
  tutorial: AzTutorial;
  onDismiss: () => void;
  itemBoundsCache?: Record<string, { x: number; y: number; width: number; height: number }>;
}

export const AzWebTutorialOverlay: React.FC<AzWebTutorialOverlayProps> = ({
  tutorialId, tutorial, onDismiss, itemBoundsCache = {},
}) => {
  const [currentSceneIndex, setCurrentSceneIndex] = useState(0);
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [checkedIndices, setCheckedIndices] = useState<Set<number>>(new Set());
  const visitedSceneIds = useRef<Set<string>>(new Set());
  const tutorialController = useAzWebTutorialController();
  const { currentVariables, pendingEvent } = tutorialController;

  const indexOfScene = useCallback(
    (id: string) => tutorial.scenes.findIndex((s) => s.id === id),
    [tutorial.scenes]
  );

  const navigateToScene = useCallback((id: string) => {
    const idx = indexOfScene(id);
    if (idx !== -1) {
      setCurrentSceneIndex(idx);
      setCurrentCardIndex(0);
      setCheckedIndices(new Set());
    }
  }, [indexOfScene]);

  const advanceCard = useCallback(() => {
    setCheckedIndices(new Set());
    const scene = tutorial.scenes[currentSceneIndex];
    if (!scene) return;
    if (currentCardIndex + 1 >= scene.cards.length) {
      scene.onComplete?.();
      setCurrentSceneIndex((p) => p + 1);
      setCurrentCardIndex(0);
    } else {
      setCurrentCardIndex((p) => p + 1);
    }
  }, [tutorial.scenes, currentSceneIndex, currentCardIndex]);

  // Variable branching
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) return;
    const scene = tutorial.scenes[currentSceneIndex];
    if (scene.branchVar && scene.branches && Object.keys(scene.branches).length > 0) {
      const varValue = String(currentVariables[scene.branchVar] ?? '');
      const targetId = scene.branches[varValue];
      if (targetId) {
        if (visitedSceneIds.current.has(targetId)) {
          const next = currentSceneIndex + 1;
          if (next >= tutorial.scenes.length) {
            tutorialController.markTutorialRead(tutorialId);
            tutorial.onComplete?.();
            onDismiss();
          } else {
            setCurrentSceneIndex(next);
            setCurrentCardIndex(0);
            setCheckedIndices(new Set());
          }
        } else {
          visitedSceneIds.current.add(scene.id);
          navigateToScene(targetId);
        }
        return;
      }
    }
    visitedSceneIds.current.add(scene.id);
  }, [currentSceneIndex]); // eslint-disable-line react-hooks/exhaustive-deps

  // Completion
  useEffect(() => {
    if (currentSceneIndex >= tutorial.scenes.length) {
      tutorialController.markTutorialRead(tutorialId);
      tutorial.onComplete?.();
      onDismiss();
    }
  }, [currentSceneIndex, tutorial.scenes.length]); // eslint-disable-line react-hooks/exhaustive-deps

  // Event-driven advance
  useEffect(() => {
    if (!pendingEvent || currentSceneIndex >= tutorial.scenes.length) return;
    const card = tutorial.scenes[currentSceneIndex]?.cards[currentCardIndex];
    if (card?.advanceCondition?.type === 'Event' && card.advanceCondition.name === pendingEvent) {
      tutorialController.consumeEvent();
      advanceCard();
    }
  }, [pendingEvent]); // eslint-disable-line react-hooks/exhaustive-deps

  if (currentSceneIndex >= tutorial.scenes.length) return null;

  const currentScene = tutorial.scenes[currentSceneIndex];
  const currentCard = currentScene?.cards[currentCardIndex];
  if (!currentCard) return null;

  const getBounds = () => {
    const h = currentCard.highlight;
    if (!h) return null;
    if (h.type === 'Area') return h.bounds;
    if (h.type === 'Item') {
      const cached = itemBoundsCache[h.id];
      if (cached) return cached;
      const el = document.querySelector(`[data-az-nav-id="${h.id}"]`);
      if (el) {
        const r = el.getBoundingClientRect();
        return { x: r.left, y: r.top, width: r.width, height: r.height };
      }
    }
    return null;
  };

  const bounds = getBounds();
  const isFullScreen = currentCard.highlight?.type === 'FullScreen';
  const isTapAnywhere = currentCard.advanceCondition?.type === 'TapAnywhere';
  const isTapTarget = currentCard.advanceCondition?.type === 'TapTarget';
  const highlightItemId = currentCard.highlight?.type === 'Item' ? currentCard.highlight.id : null;
  const showButton = !currentCard.advanceCondition || currentCard.advanceCondition.type === 'Button' || !!currentCard.checklistItems;
  const allChecked = !currentCard.checklistItems || checkedIndices.size === currentCard.checklistItems.length;

  const highlightCenterY = bounds ? bounds.y + bounds.height / 2 : 0;
  const cardAtTop = highlightCenterY > window.innerHeight * 0.6;

  const handleSkip = () => {
    tutorialController.markTutorialRead(tutorialId);
    tutorial.onSkip?.();
    onDismiss();
  };

  const handleTargetTap = () => {
    if (highlightItemId) {
      const targetId = currentCard.branches?.[highlightItemId];
      if (targetId) { navigateToScene(targetId); return; }
    }
    advanceCard();
  };

  const toggleCheck = (idx: number) => {
    setCheckedIndices((prev) => {
      const next = new Set(prev);
      if (next.has(idx)) next.delete(idx); else next.add(idx);
      return next;
    });
  };

  return (
    <div style={styles.root}>
      {/* 1. Scene content */}
      <div style={styles.sceneContent}>{currentScene.content()}</div>

      {/* 2. Dim overlay */}
      {!isFullScreen && !bounds && (
        <div
          style={{ ...styles.fullDim, cursor: isTapAnywhere ? 'pointer' : 'default' }}
          onClick={isTapAnywhere ? advanceCard : undefined}
        />
      )}

      {/* 3. Cutout highlight via box-shadow */}
      {!isFullScreen && bounds && (
        <>
          {/* Full-screen dim that doesn't cover the highlight area */}
          <div
            style={{
              position: 'fixed', inset: 0, zIndex: 9997,
              cursor: isTapAnywhere ? 'pointer' : 'default',
              pointerEvents: isTapAnywhere ? 'auto' : 'none',
            }}
            onClick={isTapAnywhere ? advanceCard : undefined}
          />
          {/* The highlight element — its box-shadow creates the dim effect */}
          <div
            style={{
              position: 'fixed',
              left: bounds.x, top: bounds.y,
              width: bounds.width, height: bounds.height,
              borderRadius: 16,
              boxShadow: '0 0 0 9999px rgba(0,0,0,0.7)',
              zIndex: 9998,
              cursor: isTapTarget ? 'pointer' : 'default',
              pointerEvents: isTapTarget ? 'auto' : 'none',
            }}
            onClick={isTapTarget ? handleTargetTap : undefined}
          />
        </>
      )}

      {/* Full-screen dim for FullScreen highlight */}
      {isFullScreen && (
        <div
          style={{ ...styles.fullDim, cursor: isTapAnywhere ? 'pointer' : 'default' }}
          onClick={isTapAnywhere ? advanceCard : undefined}
        />
      )}

      {/* 4. Card UI */}
      <div style={{ ...styles.cardContainer, ...(cardAtTop ? styles.cardTop : styles.cardBottom) }}>
        <div style={styles.card}>
          <h3 style={styles.cardTitle}>{currentCard.title}</h3>

          {currentCard.mediaContent && (
            <div style={styles.mediaContainer}>{currentCard.mediaContent()}</div>
          )}

          <p style={styles.cardText}>{currentCard.text}</p>

          {currentCard.checklistItems && (
            <div style={styles.checklist}>
              {currentCard.checklistItems.map((item, idx) => (
                <label key={idx} style={styles.checklistRow}>
                  <input
                    type="checkbox"
                    checked={checkedIndices.has(idx)}
                    onChange={(e) => toggleCheck(idx)}
                    style={{ marginRight: 8, accentColor: '#6200EE' }}
                  />
                  <span style={styles.checklistText}>{item}</span>
                </label>
              ))}
            </div>
          )}

          <div style={styles.buttonRow}>
            <button onClick={handleSkip} style={styles.skipButton}>Skip Tutorial</button>
            {showButton && (
              <button
                onClick={() => { currentCard.onAction?.(); advanceCard(); }}
                style={{ ...styles.actionButton, ...(allChecked ? {} : styles.actionButtonDisabled) }}
                disabled={!allChecked}
              >
                {currentCard.actionText || 'Next'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  root: { position: 'fixed', inset: 0, zIndex: 9996, pointerEvents: 'none' },
  sceneContent: { position: 'absolute', inset: 0 },
  fullDim: {
    position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.7)',
    zIndex: 9997, pointerEvents: 'auto',
  },
  cardContainer: {
    position: 'fixed', left: 0, right: 0, zIndex: 10000,
    display: 'flex', justifyContent: 'center', padding: 32, pointerEvents: 'auto',
  },
  cardTop: { top: 0 },
  cardBottom: { bottom: 0 },
  card: {
    background: '#fff', borderRadius: 16, padding: 24,
    width: '85%', maxWidth: 420,
    boxShadow: '0 4px 24px rgba(0,0,0,0.3)',
  },
  cardTitle: { margin: '0 0 12px', fontSize: 20, fontWeight: 'bold', color: '#000' },
  mediaContainer: {
    width: '100%', height: 120, borderRadius: 8,
    overflow: 'hidden', marginBottom: 12,
  },
  cardText: { margin: '0 0 16px', fontSize: 16, color: '#333' },
  checklist: { marginBottom: 16 },
  checklistRow: { display: 'flex', alignItems: 'center', marginBottom: 8, cursor: 'pointer' },
  checklistText: { fontSize: 14, color: '#333' },
  buttonRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 },
  skipButton: {
    background: 'none', border: 'none', color: '#6200EE',
    fontSize: 14, fontWeight: 600, cursor: 'pointer', padding: '8px 0',
  },
  actionButton: {
    background: '#6200EE', color: '#fff', border: 'none',
    borderRadius: 20, padding: '10px 24px', fontSize: 14,
    fontWeight: 600, cursor: 'pointer',
  },
  actionButtonDisabled: { background: '#ccc', cursor: 'default' },
};
```

- [ ] **Step 2: Typecheck**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck 2>&1 | tail -10
```

Expected: No errors.

- [ ] **Step 3: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/web/AzTutorialOverlay.tsx && git commit -m "feat(web): add AzWebTutorialOverlay with CSS box-shadow cutout, all four advance conditions"
```

---

## Task 11: Web — HelpOverlay Rewrite

**Files:**
- Create: `aznavrail-react/src/web/HelpOverlay.tsx`
- Delete: `aznavrail-react/src/web/HelpOverlay.jsx`

- [ ] **Step 1: Create `HelpOverlay.tsx`**

Create `aznavrail-react/src/web/HelpOverlay.tsx`:

```typescript
import React, { useRef, useEffect, useState, useCallback } from 'react';
import { AzNavItem, AzTutorial } from '../types';
import { useAzWebTutorialController } from './AzTutorialController';

interface HelpOverlayProps {
  items: AzNavItem[];
  railWidth: string | number;
  onDismiss: () => void;
  itemBounds?: Record<string, { x: number; y: number; width: number; height: number }>;
  helpList?: Record<string, string>;
  nestedRailVisibleId?: string | null;
  tutorials?: Record<string, AzTutorial>;
}

const HelpOverlay: React.FC<HelpOverlayProps> = ({
  items,
  railWidth,
  onDismiss,
  itemBounds = {},
  helpList = {},
  nestedRailVisibleId = null,
  tutorials = {},
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const descriptionsRef = useRef<HTMLDivElement>(null);
  const [expandedItemId, setExpandedItemId] = useState<string | null>(null);
  const tutorialController = useAzWebTutorialController();

  const allItems = React.useMemo(() => {
    const list = [...items];
    if (nestedRailVisibleId) {
      const host = items.find((i) => i.id === nestedRailVisibleId);
      if (host?.nestedRailItems) list.push(...host.nestedRailItems);
    }
    return list;
  }, [items, nestedRailVisibleId]);

  const drawArrows = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.strokeStyle = 'gray';
    ctx.lineWidth = 2;

    allItems.forEach((item) => {
      const infoText = item.info?.trim();
      const listText = helpList[item.id]?.trim();
      if (!infoText && !listText) return;

      const itemRect =
        itemBounds[item.id] ??
        document.querySelector<HTMLElement>(`[data-az-nav-id="${item.id}"]`)?.getBoundingClientRect();
      const descEl = document.querySelector(`[data-az-desc-id="${item.id}"]`);

      if (itemRect && descEl) {
        const descRect = descEl.getBoundingClientRect();
        const buttonX = itemRect.x + itemRect.width;
        const buttonY = itemRect.y + itemRect.height / 2;
        const descX = descRect.left;
        const descY = descRect.top + descRect.height / 2;
        const elbowX = (buttonX + descX) / 2;

        ctx.beginPath();
        ctx.moveTo(descX, descY);
        ctx.lineTo(elbowX, descY);
        ctx.lineTo(elbowX, buttonY);
        ctx.lineTo(buttonX, buttonY);
        ctx.stroke();

        const arrowSize = 8;
        ctx.beginPath();
        ctx.moveTo(buttonX, buttonY);
        ctx.lineTo(buttonX + arrowSize, buttonY - arrowSize / 2);
        ctx.lineTo(buttonX + arrowSize, buttonY + arrowSize / 2);
        ctx.closePath();
        ctx.fillStyle = 'gray';
        ctx.fill();
      }
    });
  }, [allItems, itemBounds, helpList]);

  useEffect(() => {
    drawArrows();
    const descContainer = descriptionsRef.current;
    const railContainer = document.querySelector('.rail');
    const handleScroll = () => requestAnimationFrame(drawArrows);
    window.addEventListener('resize', handleScroll);
    descContainer?.addEventListener('scroll', handleScroll);
    railContainer?.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('resize', handleScroll);
      descContainer?.removeEventListener('scroll', handleScroll);
      railContainer?.removeEventListener('scroll', handleScroll);
    };
  }, [drawArrows]);

  const isNestedRailOpen = nestedRailVisibleId !== null;
  const effectiveMarginLeft = isNestedRailOpen
    ? `calc(${typeof railWidth === 'number' ? `${railWidth}px` : railWidth} + 120px)`
    : typeof railWidth === 'number' ? `${railWidth}px` : railWidth;

  return (
    <div className="az-help-overlay">
      <div
        className="az-help-descriptions"
        style={{ marginLeft: effectiveMarginLeft }}
        ref={descriptionsRef}
      >
        {allItems.map((item) => {
          const infoText = item.info?.trim();
          const listText = helpList[item.id]?.trim();
          if (!infoText && !listText) return null;

          const titleText = (item.text ?? '').trim() || `Item ${item.id}`;
          const isExpanded = expandedItemId === item.id;
          const hasTutorial = !!tutorials[item.id];

          return (
            <div
              key={item.id}
              className="az-help-card"
              data-az-desc-id={item.id}
              onClick={() => setExpandedItemId(isExpanded ? null : item.id)}
              style={{ cursor: 'pointer' }}
            >
              <div style={cardTitleStyle}>{titleText}</div>

              {infoText && (
                <div style={isExpanded ? {} : clampStyle}>{infoText}</div>
              )}
              {listText && (
                <div style={{ ...(isExpanded ? {} : clampStyle), marginTop: infoText ? 8 : 0 }}>
                  {listText}
                </div>
              )}

              {hasTutorial && !isExpanded && (
                <div style={tutorialHintStyle}>Tutorial available</div>
              )}

              {hasTutorial && isExpanded && (
                <button
                  style={startTutorialButtonStyle}
                  onClick={(e) => {
                    e.stopPropagation();
                    tutorialController.startTutorial(item.id);
                    onDismiss();
                  }}
                >
                  Start Tutorial
                </button>
              )}

              {isExpanded && !hasTutorial && (
                <div style={{ marginTop: 8, fontSize: '0.8em', color: 'gray' }}>
                  Tap to collapse
                </div>
              )}
            </div>
          );
        })}
      </div>
      <canvas ref={canvasRef} className="az-help-canvas" />
      <button className="az-fab-exit" onClick={onDismiss}>✕</button>
    </div>
  );
};

const cardTitleStyle: React.CSSProperties = {
  fontWeight: 'bold',
  color: 'var(--md-sys-color-primary, #6200ee)',
  marginBottom: 8,
};

const clampStyle: React.CSSProperties = {
  display: '-webkit-box',
  WebkitLineClamp: 1,
  WebkitBoxOrient: 'vertical',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

const tutorialHintStyle: React.CSSProperties = {
  marginTop: 6,
  fontSize: '0.75em',
  color: '#aaa',
  fontStyle: 'italic',
};

const startTutorialButtonStyle: React.CSSProperties = {
  marginTop: 12,
  background: '#6200EE',
  color: '#fff',
  border: 'none',
  borderRadius: 16,
  padding: '8px 16px',
  fontSize: 13,
  fontWeight: 600,
  cursor: 'pointer',
};

export default HelpOverlay;
```

- [ ] **Step 2: Delete the old JSX file**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git rm aznavrail-react/src/web/HelpOverlay.jsx
```

- [ ] **Step 3: Typecheck**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck 2>&1 | tail -10
```

Expected: No errors. If any existing web files import `HelpOverlay` from `./HelpOverlay` (without extension), they'll still resolve to the new `.tsx` — no import changes needed.

- [ ] **Step 4: Run full test suite**

```bash
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && yarn test 2>&1 | tail -15
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
cd /run/media/az/0722-7C65/AzNavRail && git add aznavrail-react/src/web/HelpOverlay.tsx && git commit -m "feat(web): rewrite HelpOverlay as TypeScript with tutorial integration"
```

---

## Self-Review Checklist

After all tasks are complete, run:

```bash
# Android
cd /run/media/az/0722-7C65/AzNavRail && ./gradlew :aznavrail:test 2>&1 | tail -5

# React typecheck + tests
cd /run/media/az/0722-7C65/AzNavRail/aznavrail-react && npm run typecheck && yarn test 2>&1 | tail -10
```

Then verify spec coverage:
- [ ] `AzAdvanceCondition` (Button/TapTarget/TapAnywhere/Event) — Tasks 1, 5, 7, 10
- [ ] Tap-target branching (`branches` on `AzCard`) — Tasks 1, 7, 10
- [ ] Variable branching (`branchVar`/`branches` on `AzScene`, `startTutorial(variables)`) — Tasks 1, 2, 6, 7, 10
- [ ] Media cards (`mediaContent`) — Tasks 1, 7, 10
- [ ] Checklist cards (`checklistItems`) — Tasks 1, 7, 10
- [ ] `fireEvent` / `consumeEvent` — Tasks 2, 6, 9
- [ ] SharedPreferences persistence (Android) — Task 2
- [ ] AsyncStorage persistence (React Native) — Task 6
- [ ] localStorage persistence (Web) — Task 9
- [ ] Card auto-position (avoid highlight) — Tasks 3, 7, 10
- [ ] `TapTarget` degrades to `TapAnywhere` for non-Item highlights — Tasks 3, 7, 10
- [ ] Circular branch detection — Tasks 3, 7, 10
- [ ] Tutorial `onComplete`/`onSkip` callbacks — Task 1
- [ ] React Native help overlay tutorial launch flow fix — Task 8
- [ ] Web HelpOverlay TypeScript rewrite with tutorial integration — Task 11
- [ ] `HelpOverlay.kt.orig` deleted — Task 4
- [ ] No `.jsx` files remain in web source — Task 11
