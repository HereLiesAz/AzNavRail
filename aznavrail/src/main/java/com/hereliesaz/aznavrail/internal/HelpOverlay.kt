package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlin.math.roundToInt

@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    itemPositions: Map<String, Rect>,
    hostStates: Map<String, Boolean>,
    railWidth: Dp,
    onDismiss: () -> Unit
) {
    // We need to layout items manually to avoid overlap and respect top margin.
    // Compose Layout system makes absolute positioning tricky with LazyColumn.
    // Instead, we will use a Box and explicit offsets for each item.

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topMargin = screenHeight * 0.2f
    val spacing = 32.dp
    val density = LocalDensity.current

    val descriptionPositions = remember { mutableStateMapOf<String, Rect>() }
    // Store calculated top offsets for each item
    val itemTopOffsets = remember { mutableStateMapOf<String, Dp>() }

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

    // Sort items by their target Y position (if known) to layout them in order
    val sortedItems = itemsWithInfo.sortedBy { item ->
        itemPositions[item.id]?.center?.y ?: 0f
    }

    // Layout Calculation Effect
    LaunchedEffect(itemsWithInfo, itemPositions, descriptionPositions.toMap()) {
        var currentY = with(density) { topMargin.toPx() }
        val spacingPx = with(density) { spacing.toPx() }

        sortedItems.forEach { item ->
            val targetRect = itemPositions[item.id] ?: return@forEach
            val descRect = descriptionPositions[item.id]
            val descHeight = descRect?.height ?: 0f // Height might be 0 on first frame

            // Ideal Top: Center aligned with button center
            val buttonCenterY = targetRect.center.y
            val idealTop = buttonCenterY - (descHeight / 2)

            // Push down logic
            // Ensure we are at least below currentY
            val finalTop = maxOf(currentY, idealTop)

            itemTopOffsets[item.id] = with(density) { finalTop.toDp() }

            // Update currentY for next item
            currentY = finalTop + descHeight + spacingPx
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Draw Descriptions
        sortedItems.forEach { item ->
            val topOffset = itemTopOffsets[item.id] ?: topMargin // Default to topMargin if not yet calculated

            // We use a Box with offset to position absolutely
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = with(density) { (railWidth + 64.dp).toPx().roundToInt() }, y = with(density) { topOffset.toPx().roundToInt() }) } // Increased left offset for lanes
                    .width(300.dp)
                    .onGloballyPositioned { coordinates ->
                         descriptionPositions[item.id] = coordinates.boundsInWindow()
                    }
            ) {
                DescriptionCard(text = item.info!!)
            }
        }

        // Canvas for arrows
        Canvas(modifier = Modifier.fillMaxSize()) {
            sortedItems.forEachIndexed { index, item ->
                val itemRect = itemPositions[item.id]
                val descRect = descriptionPositions[item.id]
                val isVisible = if (item.isSubItem) hostStates[item.hostId] == true else true

                if (isVisible && itemRect != null && descRect != null) {
                    val arrowColor = Color.Red // Matching web implementation
                    val strokeWidth = 2.dp.toPx()

                    val buttonPoint = Offset(itemRect.right, itemRect.center.y)
                    val descPoint = Offset(descRect.left, descRect.center.y) // Connect to left center

                    val path = Path()
                    path.moveTo(descPoint.x, descPoint.y)

                    // Lane Logic
                    // Base padding from rail right
                    val basePadding = 16.dp.toPx()
                    // Lane step
                    val laneStep = 8.dp.toPx()

                    val elbowX = buttonPoint.x + basePadding + (index * laneStep)

                    path.lineTo(elbowX, descPoint.y)
                    path.lineTo(elbowX, buttonPoint.y)
                    path.lineTo(buttonPoint.x, buttonPoint.y)

                    drawPath(
                        path = path,
                        color = arrowColor,
                        style = Stroke(width = strokeWidth)
                    )

                    // Draw Arrowhead at ButtonPoint
                    val arrowSize = 12.dp.toPx()
                    val arrowPath = Path()
                    arrowPath.moveTo(buttonPoint.x, buttonPoint.y)
                    // Tip at buttonPoint. Base to the right.
                    arrowPath.lineTo(buttonPoint.x + arrowSize, buttonPoint.y - arrowSize/2)
                    arrowPath.lineTo(buttonPoint.x + arrowSize, buttonPoint.y + arrowSize/2)
                    arrowPath.close()

                    drawPath(arrowPath, arrowColor) // Fill
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
            modifier = Modifier.padding(4.dp)
        )
    }
}
