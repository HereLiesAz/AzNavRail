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

private const val TAG = "AzTutorialOverlay"

/**
 * A composable that renders a tutorial: scripted scenes, overlay highlights, and informational cards.
 *
 * Supports four advance conditions (Button, TapTarget, TapAnywhere, Event), tap-target branching,
 * variable branching, checklist cards, media content, and auto card positioning.
 */
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
        // If highlight is not AzHighlight.Item, TapTarget degrades to TapAnywhere
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
