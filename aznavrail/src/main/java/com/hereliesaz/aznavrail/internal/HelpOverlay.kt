package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Overlay that draws visual guides (lines) connecting rail items to their descriptions.
 * Rewritten to use straight lines as requested.
 */
@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    itemPositions: Map<String, Rect>,
    hostStates: Map<String, Boolean>,
    railWidth: Dp,
    onDismiss: () -> Unit,
    isRightDocked: Boolean = false,
    safeZones: AzSafeZones = AzSafeZones(),
    railBounds: Rect = Rect.Zero
) {
    val descriptionPositions = remember { mutableStateMapOf<String, Rect>() }

    val itemsWithInfo = items.filter { item ->
        val hasInfo = !item.info.isNullOrBlank()
        val isRailItem = item.isRailItem
        val isVisible = if (item.isSubItem) {
            hostStates[item.hostId] == true
        } else {
            true
        }
        hasInfo && isRailItem && isVisible
    }

    val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
    val safeTop = screenHeight * 0.2f
    val safeBottom = screenHeight * 0.1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = safeTop, bottom = safeBottom)
    ) {
        Row(Modifier.fillMaxSize()) {
            if (!isRightDocked) {
                Spacer(modifier = Modifier.width(railWidth))
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(
                        top = safeZones.top,
                        bottom = safeZones.bottom,
                        start = 16.dp,
                        end = 16.dp
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                items(itemsWithInfo, key = { it.id }) { item ->
                    val itemRect = itemPositions[item.id]
                    val locationInfo = if (itemRect != null) {
                        "\nLocation: (${itemRect.left.toInt()}, ${itemRect.top.toInt()})"
                    } else ""

                    TrackedDescriptionCard(
                        id = item.id,
                        text = item.info!! + locationInfo,
                        onPositioned = { rect -> descriptionPositions[item.id] = rect },
                        onDispose = { descriptionPositions.remove(item.id) }
                    )
                }
            }

            if (isRightDocked) {
                Spacer(modifier = Modifier.width(railWidth))
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            itemsWithInfo.forEach { item ->
                val itemRect = itemPositions[item.id]
                val descRect = descriptionPositions[item.id]

                val isVisible = if (item.isSubItem) {
                    hostStates[item.hostId] == true
                } else {
                    true
                }

                if (isVisible && itemRect != null && descRect != null) {
                    // Check if item is actually visible in the rail viewport
                    val isItemVisibleInRail = railBounds == Rect.Zero || railBounds.contains(itemRect.center)

                    if (isItemVisibleInRail) {
                        val lineColor = Color.Black
                        val strokeWidth = 2.dp.toPx()

                        val buttonPoint: Offset
                        val descPoint: Offset

                        if (isRightDocked) {
                            buttonPoint = Offset(itemRect.left, itemRect.center.y)
                            descPoint = Offset(descRect.right, descRect.center.y)
                        } else {
                            buttonPoint = Offset(itemRect.right, itemRect.center.y)
                            descPoint = Offset(descRect.left, descRect.center.y)
                        }

                        drawLine(
                            color = lineColor,
                            start = buttonPoint,
                            end = descPoint,
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onDismiss,
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Close, contentDescription = "Exit Help")
            }
        }
    }
}

@Composable
private fun TrackedDescriptionCard(
    id: String,
    text: String,
    onPositioned: (Rect) -> Unit,
    onDispose: () -> Unit
) {
    DisposableEffect(id) {
        onDispose { onDispose() }
    }
    DescriptionCard(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInWindow())
            }
    )
}

@Composable
fun DescriptionCard(text: String, modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .border(width = 2.dp, color = borderColor)
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
