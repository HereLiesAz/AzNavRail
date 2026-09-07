from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

ANDROID = ROOT / "aznavrail/src/main/java/com/hereliesaz/aznavrail"
CMP = ROOT / "aznavrail-cmp/src/commonMain/kotlin/com/hereliesaz/aznavrail"
MODULES = (ANDROID, CMP)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one occurrence, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


VISIBILITY_ENUM = '''package com.hereliesaz.aznavrail.model

/**
 * Reversible ways AzNavRail chrome can leave and re-enter the screen.
 *
 * The same mode is always used in reverse when visibility returns.
 */
enum class AzVisibilityAnimation {
    /** No travel: visibility changes immediately. */
    NONE,

    /** Fade the surface out/in without moving it. */
    DISSOLVE,

    /** Move each surface toward/from whichever screen edge is nearest to its current bounds. */
    NEAREST_EDGE,

    /** Move every surface leftward offscreen; revealing reverses the same path. */
    SWIPE_LEFT,
}
'''

VISIBILITY_HELPER = '''package com.hereliesaz.aznavrail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalWindowInfo
import com.hereliesaz.aznavrail.model.AzVisibilityAnimation

/** Host-level visibility inherited by AzNavRail chrome without affecting ordinary app content. */
data class AzSurfaceVisibilityState(
    val visible: Boolean = true,
    val animation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    val durationMillis: Int = 250,
    /** When true, this state supplies the animation/duration to every Az surface below it. */
    val overrideChildren: Boolean = false,
)

/**
 * Visibility context used by [AzHostActivityLayout]. Ordinary app composables ignore it; Az chrome
 * opts in through [azSurfaceVisibility].
 */
val LocalAzSurfaceVisibility = staticCompositionLocalOf { AzSurfaceVisibilityState() }

/**
 * Keeps a surface composed while hiding it, preserving its remembered state and position. Setting
 * [visible] false runs [animation]; setting it true runs the exact same transform backwards.
 */
@Composable
fun Modifier.azSurfaceVisibility(
    visible: Boolean = true,
    animation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    durationMillis: Int = 250,
): Modifier {
    require(durationMillis >= 0) { "visibility durationMillis must be >= 0" }

    val inherited = LocalAzSurfaceVisibility.current
    val effectiveVisible = visible && inherited.visible
    val effectiveAnimation = if (inherited.overrideChildren) inherited.animation else animation
    val effectiveDuration = if (inherited.overrideChildren) inherited.durationMillis else durationMillis
    require(effectiveDuration >= 0) { "visibility durationMillis must be >= 0" }

    var bounds by remember { mutableStateOf(Rect.Zero) }
    val container = LocalWindowInfo.current.containerSize
    val progress by animateFloatAsState(
        targetValue = if (effectiveVisible) 1f else 0f,
        animationSpec = tween(durationMillis = effectiveDuration),
    )

    val hidden = 1f - progress
    val (targetX, targetY) = when (effectiveAnimation) {
        AzVisibilityAnimation.NONE,
        AzVisibilityAnimation.DISSOLVE -> 0f to 0f

        AzVisibilityAnimation.SWIPE_LEFT -> -bounds.right to 0f

        AzVisibilityAnimation.NEAREST_EDGE -> {
            if (bounds == Rect.Zero || container.width <= 0 || container.height <= 0) {
                0f to 0f
            } else {
                val left = bounds.right
                val right = container.width.toFloat() - bounds.left
                val top = bounds.bottom
                val bottom = container.height.toFloat() - bounds.top
                when (minOf(left, right, top, bottom)) {
                    left -> -left to 0f
                    right -> right to 0f
                    top -> 0f to -top
                    else -> 0f to bottom
                }
            }
        }
    }

    val hiddenInputBlocker = if (!effectiveVisible) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    } else Modifier

    return this
        .onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            bounds = Rect(
                left = position.x,
                top = position.y,
                right = position.x + coordinates.size.width,
                bottom = position.y + coordinates.size.height,
            )
        }
        .graphicsLayer {
            translationX = targetX * hidden
            translationY = targetY * hidden
            alpha = when (effectiveAnimation) {
                AzVisibilityAnimation.DISSOLVE, AzVisibilityAnimation.NONE -> progress
                else -> 1f
            }
        }
        .then(hiddenInputBlocker)
}
'''

UNATTACHED_EXTENSIONS = '''package com.hereliesaz.aznavrail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import com.hereliesaz.aznavrail.model.AzVisibilityAnimation

/**
 * Declares an unattached host with a developer-defined maximum visible height.
 *
 * The rail grows naturally until either its content is exhausted or [maxHeight] is reached. When
 * its host, sub-items, or nested sub-hosts need more room, the content scrolls vertically. The same
 * cap follows the rail while free-floating, screen-edge docked, or rail-to-rail docked.
 */
fun AzNavRailScope.azUnattachedHostItem(
    id: String,
    text: String,
    maxHeight: Dp,
    anchor: AzUnattachedAnchor = AzUnattachedAnchor.OPPOSITE,
    route: String? = null,
    content: Any? = null,
    color: Color? = null,
    shape: AzButtonShape? = null,
    disabled: Boolean = false,
    screenTitle: String? = null,
    info: String? = null,
    classifiers: Set<String> = emptySet(),
    menuText: String? = null,
    textColor: Color? = null,
    fillColor: Color? = null,
    translucentBackgroundColor: Color? = null,
    badge: String? = null,
    persistentBadge: Boolean = false,
    isLoading: Boolean = false,
    initiallyExpanded: Boolean = false,
    expandWhen: (() -> Boolean)? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    forceHiddenMenuOpen: Boolean = false,
    onHiddenMenuDismiss: (() -> Unit)? = null,
    visible: Boolean = true,
    visibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    visibilityDurationMillis: Int = 250,
    hiddenMenu: HiddenMenuScope.() -> Unit = {},
) {
    require(maxHeight.isSpecified && maxHeight > 0.dp) {
        "maxHeight must be a positive, specified Dp"
    }
    require(visibilityDurationMillis >= 0) { "visibilityDurationMillis must be >= 0" }

    azUnattachedHostItem(
        id = id,
        text = text,
        anchor = anchor,
        route = route,
        content = content,
        color = color,
        shape = shape,
        disabled = disabled,
        screenTitle = screenTitle,
        info = info,
        classifiers = classifiers,
        menuText = menuText,
        textColor = textColor,
        fillColor = fillColor,
        translucentBackgroundColor = translucentBackgroundColor,
        badge = badge,
        persistentBadge = persistentBadge,
        isLoading = isLoading,
        initiallyExpanded = initiallyExpanded,
        expandWhen = expandWhen,
        onExpandedChange = onExpandedChange,
        onClick = onClick,
        forceHiddenMenuOpen = forceHiddenMenuOpen,
        onHiddenMenuDismiss = onHiddenMenuDismiss,
        hiddenMenu = hiddenMenu,
    )

    updateUnattachedHost(id) {
        copy(
            unattachedMaxHeightDp = maxHeight.value,
            unattachedVisible = visible,
            unattachedVisibilityAnimation = visibilityAnimation,
            unattachedVisibilityDurationMillis = visibilityDurationMillis,
        )
    }
}

/**
 * Programmatically hides/shows an already-declared unattached host without destroying its state.
 * Recomposition with [visible] restored reverses the same animation.
 */
fun AzNavRailScope.azUnattachedVisibility(
    id: String,
    visible: Boolean,
    animation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,
    durationMillis: Int = 250,
) {
    require(durationMillis >= 0) { "durationMillis must be >= 0" }
    updateUnattachedHost(id) {
        copy(
            unattachedVisible = visible,
            unattachedVisibilityAnimation = animation,
            unattachedVisibilityDurationMillis = durationMillis,
        )
    }
}

private fun AzNavRailScope.updateUnattachedHost(
    id: String,
    transform: com.hereliesaz.aznavrail.model.AzNavItem.() -> com.hereliesaz.aznavrail.model.AzNavItem,
) {
    val implementation = this as? AzNavRailScopeImpl
        ?: error("AzNavRailScope implementation does not support unattached host state")
    val index = implementation.navItems.indexOfLast { it.id == id }
    require(index >= 0 && implementation.navItems[index].isUnattached) {
        "Unattached host '$id' was not registered before its unattached state was configured"
    }
    implementation.navItems[index] = implementation.navItems[index].transform()
}
'''

DOC = '''# Visibility and unattached-rail overflow

AzNavRail chrome can be hidden without destroying its state. The same animation is automatically
played backwards when the surface becomes visible again.

```kotlin
var showAz by remember { mutableStateOf(true) }

AzHostActivityLayout(
    navController = navController,
    azVisible = showAz,
    azVisibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,
    azVisibilityDurationMillis = 450,
    railVisible = true,
) {
    azUnattachedHostItem(
        id = "tools",
        text = "Tools",
        maxHeight = 320.dp,
        anchor = AzUnattachedAnchor.FLOATING,
    )

    azUnattachedVisibility(
        id = "tools",
        visible = toolsVisible,
        animation = AzVisibilityAnimation.SWIPE_LEFT,
        durationMillis = 300,
    )
}
```

`AzVisibilityAnimation` supports `NONE`, `DISSOLVE`, `NEAREST_EDGE`, and `SWIPE_LEFT`.

`AzDropdownMenu` already supports controlled closing through `expanded` / `onExpandedChange`; it now
also accepts `visible`, `visibilityAnimation`, and `visibilityDurationMillis` so the trigger and an
open panel can be hidden and restored together. `AzWindow` accepts the same visibility trio. The
host's `railVisible` controls the main rail itself, while `azVisible` controls all participating Az
chrome under the host.

Unattached hosts grow naturally until their developer cap or the safe 80% viewport height is
reached. Their complete subtree remains composed inside a vertical scroll container, so sub-items
and nested sub-hosts remain reachable whether the rail is fixed, floating, edge-docked, or attached
to another floating rail.
'''

for base in MODULES:
    write(base / "model/AzVisibilityAnimation.kt", VISIBILITY_ENUM)
    write(base / "AzVisibility.kt", VISIBILITY_HELPER)
    write(base / "AzUnattachedHostHeight.kt", UNATTACHED_EXTENSIONS)

    # Store visibility state on unattached host items.
    nav_item = base / "model/AzNavItem.kt"
    replace_once(
        nav_item,
        "    val unattachedMaxHeightDp: Float? = null,\n",
        "    val unattachedMaxHeightDp: Float? = null,\n"
        "    /** Controlled visibility for this unattached host and its complete subtree. */\n"
        "    val unattachedVisible: Boolean = true,\n"
        "    /** Reversible transition used when [unattachedVisible] changes. */\n"
        "    val unattachedVisibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n"
        "    /** Duration used in both hide and reverse-show directions. */\n"
        "    val unattachedVisibilityDurationMillis: Int = 250,\n",
    )

    # Each unattached host becomes its own capped scroll viewport (including all descendants).
    unattached = base / "internal/AzUnattachedRail.kt"
    text = read(unattached)
    text = text.replace(
        "import androidx.compose.foundation.background\n",
        "import androidx.compose.foundation.background\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n",
        1,
    )
    text = text.replace(
        "import androidx.compose.foundation.layout.height\n",
        "import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n",
        1,
    )
    if "import com.hereliesaz.aznavrail.azSurfaceVisibility" not in text:
        marker = "import com.hereliesaz.aznavrail.AzTextBoxDefaults\n"
        if marker not in text:
            raise RuntimeError(f"{unattached}: import marker missing")
        text = text.replace(marker, marker + "import com.hereliesaz.aznavrail.azSurfaceVisibility\n", 1)

    old_stack = '''    Column(\n        modifier = modifier.onGloballyPositioned { onSizeChanged(it.size) },\n        horizontalAlignment = Alignment.CenterHorizontally,\n        verticalArrangement = Arrangement.spacedBy(spacingDp),\n    ) {\n        hosts.forEach { host ->\n            UnattachedNode(\n                item = host,\n                scope = scope,\n                navController = navController,\n                currentDestination = currentDestination,\n                hostStates = hostStates,\n                buttonSize = buttonSize,\n                onCyclerClick = onCyclerClick,\n                hiddenMenuOpenId = hiddenMenuOpenId,\n                onMenuOpen = onMenuOpen,\n                onHiddenMenuDismiss = onHiddenMenuDismiss,\n                popupOpensLeft = popupOpensLeft,\n            )\n        }\n    }\n'''
    new_stack = '''    val configuration = LocalConfiguration.current\n    val safeViewportHeight = (configuration.screenHeightDp * 0.8f).dp\n\n    Column(\n        modifier = modifier.onGloballyPositioned { onSizeChanged(it.size) },\n        horizontalAlignment = Alignment.CenterHorizontally,\n        verticalArrangement = Arrangement.spacedBy(spacingDp),\n    ) {\n        hosts.forEach { host ->\n            val requested = host.unattachedMaxHeightDp?.dp\n            val maxHeight = if (requested != null) minOf(requested, safeViewportHeight) else safeViewportHeight\n            val scrollState = rememberScrollState()\n            Column(\n                modifier = Modifier\n                    .heightIn(max = maxHeight)\n                    .verticalScroll(scrollState)\n                    .azSurfaceVisibility(\n                        visible = host.unattachedVisible,\n                        animation = host.unattachedVisibilityAnimation,\n                        durationMillis = host.unattachedVisibilityDurationMillis,\n                    ),\n                horizontalAlignment = Alignment.CenterHorizontally,\n                verticalArrangement = Arrangement.spacedBy(spacingDp),\n            ) {\n                UnattachedNode(\n                    item = host,\n                    scope = scope,\n                    navController = navController,\n                    currentDestination = currentDestination,\n                    hostStates = hostStates,\n                    buttonSize = buttonSize,\n                    onCyclerClick = onCyclerClick,\n                    hiddenMenuOpenId = hiddenMenuOpenId,\n                    onMenuOpen = onMenuOpen,\n                    onHiddenMenuDismiss = onHiddenMenuDismiss,\n                    popupOpensLeft = popupOpensLeft,\n                )\n            }\n        }\n    }\n'''
    if old_stack not in text:
        raise RuntimeError(f"{unattached}: UnattachedStack body marker missing")
    write(unattached, text.replace(old_stack, new_stack, 1))

    # AzWindow: explicit programmatic visibility, with state retained while hidden.
    window = base / "AzWindow.kt"
    replace_once(
        window,
        "    onDismiss: (() -> Unit)? = null,\n    obstruction: Rect? = null,\n",
        "    onDismiss: (() -> Unit)? = null,\n"
        "    visible: Boolean = true,\n"
        "    visibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n"
        "    visibilityDurationMillis: Int = 250,\n"
        "    obstruction: Rect? = null,\n",
    )
    text = read(window)
    if "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation" not in text:
        anchor = "import androidx.compose.ui.unit.dp\n"
        if anchor not in text:
            raise RuntimeError(f"{window}: import anchor missing")
        text = text.replace(anchor, anchor + "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n", 1)
    old = "        modifier = modifier\n            .offset { IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()) }\n"
    new = "        modifier = modifier\n            .azSurfaceVisibility(visible, visibilityAnimation, visibilityDurationMillis)\n            .offset { IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()) }\n"
    if old not in text:
        raise RuntimeError(f"{window}: Surface modifier marker missing")
    write(window, text.replace(old, new, 1))

    # Drop-down trigger specs carry their own visibility, including when lifted into the title row.
    triggers = base / "internal/AzTitleTriggers.kt"
    text = read(triggers)
    if "import com.hereliesaz.aznavrail.azSurfaceVisibility" not in text:
        text = text.replace(
            "import com.hereliesaz.aznavrail.azAccent\n",
            "import com.hereliesaz.aznavrail.azAccent\nimport com.hereliesaz.aznavrail.azSurfaceVisibility\n",
            1,
        )
    if "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation" not in text:
        text = text.replace(
            "import com.hereliesaz.aznavrail.model.AzHeaderIconShape\n",
            "import com.hereliesaz.aznavrail.model.AzHeaderIconShape\nimport com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n",
            1,
        )
    text = text.replace(
        "    val contentDescription: String = \"Menu\",\n)",
        "    val contentDescription: String = \"Menu\",\n"
        "    val visible: Boolean = true,\n"
        "    val visibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n"
        "    val visibilityDurationMillis: Int = 250,\n)",
        1,
    )
    text = text.replace(
        "        modifier = modifier\n            // Automatic breathing room",
        "        modifier = modifier\n"
        "            .azSurfaceVisibility(spec.visible, spec.visibilityAnimation, spec.visibilityDurationMillis)\n"
        "            // Automatic breathing room",
        1,
    )
    write(triggers, text)

    dropdown = base / "AzDropdownMenu.kt"
    text = read(dropdown)
    if "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation" not in text:
        anchor = "import com.hereliesaz.aznavrail.model.AzHeaderIconShape\n"
        if anchor not in text:
            raise RuntimeError(f"{dropdown}: import anchor missing")
        text = text.replace(anchor, anchor + "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n", 1)
    text = text.replace(
        "    expanded: Boolean? = null,\n    onExpandedChange: ((Boolean) -> Unit)? = null,\n    content: AzDropdownMenuScope.() -> Unit\n",
        "    expanded: Boolean? = null,\n"
        "    onExpandedChange: ((Boolean) -> Unit)? = null,\n"
        "    visible: Boolean = true,\n"
        "    visibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n"
        "    visibilityDurationMillis: Int = 250,\n"
        "    content: AzDropdownMenuScope.() -> Unit\n",
        1,
    )
    text = text.replace(
        "            appIcon = appIcon,\n            textStyle = config.itemTextStyle,\n",
        "            appIcon = appIcon,\n"
        "            textStyle = config.itemTextStyle,\n"
        "            visible = visible,\n"
        "            visibilityAnimation = visibilityAnimation,\n"
        "            visibilityDurationMillis = visibilityDurationMillis,\n",
        1,
    )
    text = text.replace(
        "    Box(modifier = modifier) {\n",
        "    Box(modifier = modifier.azSurfaceVisibility(visible, visibilityAnimation, visibilityDurationMillis)) {\n",
        1,
    )
    text = text.replace(
        "                Surface(\n                    // The host rail's panel colour",
        "                Surface(\n"
        "                    modifier = Modifier.azSurfaceVisibility(visible, visibilityAnimation, visibilityDurationMillis),\n"
        "                    // The host rail's panel colour",
        1,
    )
    write(dropdown, text)

    # Host-level all-Az visibility + individual main-rail visibility.
    host = base / "AzNavHost.kt"
    text = read(host)
    if "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation" not in text:
        anchor = "import com.hereliesaz.aznavrail.model.AzSheetConfig\n"
        if anchor not in text:
            raise RuntimeError(f"{host}: import anchor missing")
        text = text.replace(anchor, anchor + "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n", 1)
    text = text.replace(
        "    expanded: Boolean? = null,\n    pagesEnabled: Boolean = true,\n",
        "    expanded: Boolean? = null,\n"
        "    azVisible: Boolean = true,\n"
        "    azVisibilityAnimation: AzVisibilityAnimation = AzVisibilityAnimation.DISSOLVE,\n"
        "    azVisibilityDurationMillis: Int = 250,\n"
        "    railVisible: Boolean = true,\n"
        "    pagesEnabled: Boolean = true,\n",
        1,
    )
    old_box = "    BoxWithConstraints(modifier = modifier.fillMaxSize()) {\n"
    new_box = (
        "    require(azVisibilityDurationMillis >= 0) { \"azVisibilityDurationMillis must be >= 0\" }\n"
        "    CompositionLocalProvider(\n"
        "        LocalAzSurfaceVisibility provides AzSurfaceVisibilityState(\n"
        "            visible = azVisible,\n"
        "            animation = azVisibilityAnimation,\n"
        "            durationMillis = azVisibilityDurationMillis,\n"
        "            overrideChildren = true,\n"
        "        )\n"
        "    ) {\n"
        "        BoxWithConstraints(modifier = modifier.fillMaxSize()) {\n"
    )
    if old_box not in text:
        raise RuntimeError(f"{host}: BoxWithConstraints marker missing")
    text = text.replace(old_box, new_box, 1)
    text = text.replace(
        "                    .height(titleHeight)\n                    .then(titlePaddingSide),\n",
        "                    .height(titleHeight)\n                    .azSurfaceVisibility()\n                    .then(titlePaddingSide),\n",
        1,
    )
    text = text.replace(
        "            AzNavRail(\n                modifier = Modifier.fillMaxSize(),\n",
        "            AzNavRail(\n                modifier = Modifier.fillMaxSize().azSurfaceVisibility(visible = railVisible),\n",
        1,
    )
    end_marker = "    return guidanceController\n}"
    if end_marker not in text:
        raise RuntimeError(f"{host}: return marker missing")
    text = text.replace(end_marker, "    }\n\n    return guidanceController\n}", 1)
    write(host, text)

# Tests: data contract for cap + programmatic visibility.
for test_path in (
    ROOT / "aznavrail/src/test/java/com/hereliesaz/aznavrail/AzUnattachedRailTest.kt",
    ROOT / "aznavrail-cmp/src/commonTest/kotlin/com/hereliesaz/aznavrail/AzUnattachedRailTest.kt",
):
    text = read(test_path)
    if "AzVisibilityAnimation" not in text:
        text = text.replace(
            "import com.hereliesaz.aznavrail.model.AzUnattachedAnchor\n",
            "import com.hereliesaz.aznavrail.model.AzUnattachedAnchor\n"
            "import com.hereliesaz.aznavrail.model.AzVisibilityAnimation\n"
            "import androidx.compose.ui.unit.dp\n",
            1,
        )
    addition = '''\n    @Test\n    fun `unattached host stores max height and reversible visibility`() {\n        val scope = AzNavRailScopeImpl()\n        scope.azUnattachedHostItem(\n            id = "tools",\n            text = "Tools",\n            maxHeight = 320.dp,\n            visible = false,\n            visibilityAnimation = AzVisibilityAnimation.NEAREST_EDGE,\n            visibilityDurationMillis = 450,\n        )\n\n        val item = scope.navItems.single { it.id == "tools" }\n        assertEquals(320f, item.unattachedMaxHeightDp)\n        assertEquals(false, item.unattachedVisible)\n        assertEquals(AzVisibilityAnimation.NEAREST_EDGE, item.unattachedVisibilityAnimation)\n        assertEquals(450, item.unattachedVisibilityDurationMillis)\n\n        scope.azUnattachedVisibility(\n            id = "tools",\n            visible = true,\n            animation = AzVisibilityAnimation.SWIPE_LEFT,\n            durationMillis = 125,\n        )\n        val shown = scope.navItems.single { it.id == "tools" }\n        assertEquals(true, shown.unattachedVisible)\n        assertEquals(AzVisibilityAnimation.SWIPE_LEFT, shown.unattachedVisibilityAnimation)\n        assertEquals(125, shown.unattachedVisibilityDurationMillis)\n    }\n'''
    if "unattached host stores max height and reversible visibility" not in text:
        pos = text.rfind("}")
        if pos < 0:
            raise RuntimeError(f"{test_path}: closing brace missing")
        text = text[:pos] + addition + text[pos:]
    write(test_path, text)

write(ROOT / "docs/VISIBILITY_AND_UNATTACHED_OVERFLOW.md", DOC)

print("Applied unattached overflow + reversible surface visibility changes.")
