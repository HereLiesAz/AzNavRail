package com.hereliesaz.aznavrail.tutorial

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A composable that renders a tutorial, including its scripted scenes,
 * overlay highlights, and informational cards.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AzTutorialOverlay(
    tutorial: AzTutorial,
    onDismiss: () -> Unit,
    itemBoundsCache: Map<String, Rect> = emptyMap()
) {
    var currentSceneIndex by remember { mutableStateOf(0) }
    var currentCardIndex by remember { mutableStateOf(0) }

    if (currentSceneIndex >= tutorial.scenes.size) {
        LaunchedEffect(Unit) {
            onDismiss()
        }
        return
    }

    val currentScene = tutorial.scenes[currentSceneIndex]

    // Check if we finished cards for the current scene
    if (currentCardIndex >= currentScene.cards.size) {
        LaunchedEffect(currentSceneIndex, currentCardIndex) {
            currentScene.onComplete?.invoke()
            currentSceneIndex++
            currentCardIndex = 0
        }
        return
    }

    val currentCard = currentScene.cards[currentCardIndex]

    // Determine the highlight bounds based on the current card
    val highlightBounds = remember(currentCard.highlight, itemBoundsCache) {
        when (val highlight = currentCard.highlight) {
            is AzHighlight.Area -> highlight.bounds
            is AzHighlight.Item -> itemBoundsCache[highlight.id]
            else -> null
        }
    }

    // Full screen overlay that draws the scene and the tutorial UI
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Render the Scene Content (the scripted screen)
        // This is underneath the overlay mask
        currentScene.content()

        // 2. The darkened overlay mask that punches a hole for the highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Use compositing strategy to allow clear blend mode to punch holes
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawBehind {
                    // Draw dim background
                    drawRect(color = Color.Black.copy(alpha = 0.7f))

                    // Punch out the highlight area if it exists
                    if (highlightBounds != null) {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(highlightBounds.left, highlightBounds.top),
                            size = Size(highlightBounds.width, highlightBounds.height),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
                // Allow tapping outside to dismiss if desired, or maybe block it
                // .clickable(onClick = onDismiss) // We might not want to dismiss on background tap for a scripted tutorial
        )

        // 3. Render the Card UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomCenter // Default to bottom center
        ) {
            AnimatedContent(
                targetState = currentCard,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TutorialCardTransition"
            ) { card ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // Don't take full width
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = card.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Skip button to dismiss tutorial entirely
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors()
                        ) {
                            Text("Skip Tutorial")
                        }

                        // Next/Action button
                        Button(
                            onClick = {
                                card.onAction?.invoke()
                                currentCardIndex++
                            }
                        ) {
                            Text(card.actionText)
                        }
                    }
                }
            }
        }
    }
}
