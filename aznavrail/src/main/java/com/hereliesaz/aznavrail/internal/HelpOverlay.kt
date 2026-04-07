package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.LocalAzSafeZones
import com.hereliesaz.aznavrail.model.AzNavItem

import androidx.compose.foundation.BorderStroke

@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    helpLineColors: List<Color> = emptyList(),
    onDismiss: () -> Unit,
    itemBoundsCache: Map<String, Rect> = emptyMap(),
    helpList: Map<String, String> = emptyMap(),
    nestedRailOpenId: String? = null,
    tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial> = emptyMap(),
    onTutorialLaunch: ((String) -> Unit)? = null
) {
    val itemsWithInfo = remember(items, helpList, nestedRailOpenId, tutorials) {
        val flatItems = items.toMutableList()
        if (nestedRailOpenId != null) {
            val nestedHost = items.find { it.id == nestedRailOpenId }
            if (nestedHost?.nestedRailItems != null) {
                flatItems.addAll(nestedHost.nestedRailItems)
            }
        }
        flatItems.filter { !it.info.isNullOrBlank() || !helpList[it.id].isNullOrBlank() || tutorials.containsKey(it.id) }
    }
    val safeZones = LocalAzSafeZones.current
    val density = LocalDensity.current

    val cardBoundsCache = remember { mutableStateMapOf<String, Rect>() }
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    // Calculate dynamic padding to avoid overlapping nested rails
    val isNestedRailOpen = nestedRailOpenId != null
    val dynamicStartPadding = if (isNestedRailOpen) 240.dp else 120.dp
    val defaultColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow)
    val colorPalette = if (helpLineColors.isNotEmpty()) helpLineColors else defaultColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss) // Background tap to dismiss
            .drawBehind {
                val strokeWidth = 4.dp

                itemsWithInfo.forEachIndexed { index, item ->
                    val drawColor = colorPalette[index % colorPalette.size]
                    val itemBounds = itemBoundsCache[item.id]
                    val cardBounds = cardBoundsCache[item.id]

                    if (itemBounds != null && cardBounds != null) {
                        val start = Offset(itemBounds.right, itemBounds.center.y)
                        val end = Offset(cardBounds.left, cardBounds.center.y)

                        drawLine(
                            color = drawColor,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth.toPx()
                        )
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dynamicStartPadding, end = 16.dp) // Leave space for rail + nested rail
                .padding(top = safeZones.top, bottom = safeZones.bottom)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Equivalent to top contentPadding
            itemsWithInfo.forEachIndexed { index, item ->
                val isExpanded = expandedItemId == item.id
                val hasTutorial = tutorials.containsKey(item.id)
                val cardColor = colorPalette[index % colorPalette.size]

                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, cardColor)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            cardBoundsCache[item.id] = coords.boundsInWindow()
                        }
                        .clickable {
                            expandedItemId = if (isExpanded) null else item.id
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.text.ifBlank { "Item ${item.id}" },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val infoText = item.info
                    val listText = helpList[item.id]

                    if (!infoText.isNullOrBlank()) {
                        Text(
                            text = infoText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!listText.isNullOrBlank()) {
                        if (!infoText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = listText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isExpanded) {
                        if (hasTutorial) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start Tutorial",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onTutorialLaunch?.invoke(item.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to collapse",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else if (hasTutorial) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tutorial available (Tap to expand)",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Equivalent to bottom contentPadding
        }
    }
}
