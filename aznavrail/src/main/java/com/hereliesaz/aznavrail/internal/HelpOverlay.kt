package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.LocalAzSafeZones
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlin.math.roundToInt

internal data class HelpLayoutItem(val id: String, val top: Float, val bottom: Float)
internal data class HelpTargetItem(val id: String, val info: String, val targetY: Float)

internal fun calculateHelpLayout(
    items: List<HelpTargetItem>,
    topMargin: Float,
    fixedHeight: Float = 100f,
    spacing: Float = 32f
): List<HelpLayoutItem> {
    val result = mutableListOf<HelpLayoutItem>()
    var currentY = topMargin

    items.sortedBy { it.targetY }.forEach { item ->
        val idealTop = item.targetY - (fixedHeight / 2)
        val top = maxOf(currentY, idealTop)
        val bottom = top + fixedHeight
        result.add(HelpLayoutItem(item.id, top, bottom))
        currentY = bottom + spacing
    }
    return result
}

@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    onDismiss: () -> Unit,
    itemBoundsCache: Map<String, androidx.compose.ui.geometry.Rect> = emptyMap()
) {
    val itemsWithInfo = items.filter { !it.info.isNullOrBlank() }
    val safeZones = LocalAzSafeZones.current
    val density = LocalDensity.current

    val testItems = itemsWithInfo.mapNotNull { item ->
        val bounds = itemBoundsCache[item.id]
        if (bounds != null) {
            HelpTargetItem(item.id, item.info!!, bounds.center.y)
        } else null
    }

    val topMarginPx = with(density) { safeZones.top.toPx() }
    val layoutItems = remember(testItems, topMarginPx) {
        calculateHelpLayout(testItems, topMarginPx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .drawBehind {
                val drawColor = Color.Yellow
                val strokeWidth = 2.dp

                layoutItems.forEach { layoutItem ->
                    val bounds = itemBoundsCache[layoutItem.id]
                    if (bounds != null) {
                        val isItemVisibleInRail = bounds.top >= safeZones.top.toPx() && bounds.bottom <= (size.height - safeZones.bottom.toPx())
                        if (isItemVisibleInRail) {
                            val itemCenterY = bounds.center.y
                            val descriptionCenterY = layoutItem.top + 50f // approx center of text

                            val startX = 300f
                            val endX = bounds.left - 20f

                            drawLine(
                                color = drawColor,
                                start = Offset(startX, descriptionCenterY),
                                end = Offset(endX, itemCenterY),
                                strokeWidth = strokeWidth.toPx()
                            )
                        }
                    }
                }
            }
    ) {
        layoutItems.forEach { layoutItem ->
            val item = itemsWithInfo.find { it.id == layoutItem.id }
            if (item != null) {
                // Info Card - STRICT SQUARE
                Text(
                    text = "${item.text}: ${item.info}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .offset { IntOffset(100.dp.roundToPx(), layoutItem.top.roundToInt()) }
                        .background(Color.DarkGray, RectangleShape) // No rounded corners
                        .padding(16.dp)
                )
            }
        }

        // FAB - STRICT SQUARE
        FloatingActionButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            shape = RectangleShape // No rounded corners
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Help")
        }
    }
}
