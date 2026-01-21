package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    itemPositions: Map<String, Rect>,
    hostStates: Map<String, Boolean>,
    railWidth: Dp,
    onDismiss: () -> Unit,
    isRightDocked: Boolean = false,
    safeZones: AzSafeZones = AzSafeZones()
) {
    val descriptionPositions = remember { mutableStateMapOf<String, Rect>() }

    // Filter items to only those that are logically visible.
    val itemsWithInfo = items.filter { item ->
        val hasInfo = !item.info.isNullOrBlank()
        val isRailItem = item.isRailItem
        val isVisible = if (item.isSubItem) {
            // Check if host is expanded
            hostStates[item.hostId] == true
        } else {
            // Top level item
            true
        }
        hasInfo && isRailItem && isVisible
    }

    // Restore safe zone calculations
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
                    // Calculate visual offset to align with button
                    // Note: This is a simple vertical list, so "alignment" is purely visual ordering.
                    // To truly align, we'd need a custom layout.
                    // But we can add the coordinates to the description to "bolster smart location tools" as requested.
                    val itemRect = itemPositions[item.id]
                    val locationInfo = if (itemRect != null) {
                        "\nLocation: (${itemRect.left.toInt()}, ${itemRect.top.toInt()})"
                    } else ""

                    DescriptionCard(
                        text = item.info!! + locationInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .onGloballyPositioned { coordinates ->
                                descriptionPositions[item.id] = coordinates.boundsInWindow()
                            }
                    )
                }
            }

            if (isRightDocked) {
                Spacer(modifier = Modifier.width(railWidth))
            }
        }

        // Canvas for arrows. Needs to be on top of everything to draw over spacers/margins.
        // It must NOT block touches. `Canvas` does not consume touches by default.
        Canvas(modifier = Modifier.fillMaxSize()) {
            itemsWithInfo.forEach { item ->
                // Only draw if we have a valid position for both item and description
                val itemRect = itemPositions[item.id]
                val descRect = descriptionPositions[item.id]

                // Also double check logical visibility to be safe against stale itemPositions
                val isVisible = if (item.isSubItem) {
                    hostStates[item.hostId] == true
                } else {
                    true
                }

                if (isVisible && itemRect != null && descRect != null) {
                    val arrowColor = Color.Gray
                    val strokeWidth = 2.dp.toPx()

                    val buttonPoint: Offset
                    val descPoint: Offset

                    if (isRightDocked) {
                        // Button is on Right. Description is on Left.
                        buttonPoint = Offset(itemRect.left, itemRect.center.y)
                        descPoint = Offset(descRect.right, descRect.center.y)
                    } else {
                        // Button is on Left. Description is on Right.
                        buttonPoint = Offset(itemRect.right, itemRect.center.y)
                        descPoint = Offset(descRect.left, descRect.center.y)
                    }

                    val path = Path()
                    path.moveTo(descPoint.x, descPoint.y)

                    // Path Logic:
                    // 1. Move Horizontally from Description
                    // 2. Move Vertically to Button Y
                    // 3. Move Horizontally to Button X
                    // We use the midpoint between Button Edge and Description Edge for the vertical segment.

                    val elbowX = (buttonPoint.x + descPoint.x) / 2

                    path.lineTo(elbowX, descPoint.y) // Horiz
                    path.lineTo(elbowX, buttonPoint.y) // Vert
                    path.lineTo(buttonPoint.x, buttonPoint.y) // Horiz to Button

                    drawPath(
                        path = path,
                        color = arrowColor,
                        style = Stroke(width = strokeWidth)
                    )

                    // Draw Arrowhead at ButtonPoint
                    val arrowSize = 8.dp.toPx()
                    val arrowPath = Path()
                    arrowPath.moveTo(buttonPoint.x, buttonPoint.y)

                    if (isRightDocked) {
                        // Arrow points Right.
                        // Vertices: Tip (buttonPoint), TopLeft, BottomLeft
                        arrowPath.lineTo(buttonPoint.x - arrowSize, buttonPoint.y - arrowSize / 2)
                        arrowPath.lineTo(buttonPoint.x - arrowSize, buttonPoint.y + arrowSize / 2)
                    } else {
                        // Arrow points Left.
                        // Vertices: Tip (buttonPoint), TopRight, BottomRight
                        arrowPath.lineTo(buttonPoint.x + arrowSize, buttonPoint.y - arrowSize / 2)
                        arrowPath.lineTo(buttonPoint.x + arrowSize, buttonPoint.y + arrowSize / 2)
                    }
                    arrowPath.close()

                    drawPath(arrowPath, arrowColor)
                }
            }
        }

        // FAB to exit
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
fun DescriptionCard(text: String, modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .border(width = 2.dp, color = borderColor)
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
