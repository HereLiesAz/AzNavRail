package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.LocalAzSafeZones
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    onDismiss: () -> Unit
) {
    val itemsWithInfo = items.filter { !it.info.isNullOrBlank() }
    val safeZones = LocalAzSafeZones.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .drawBehind {
                val drawColor = Color.Yellow
                val strokeWidth = 2.dp

                itemsWithInfo.forEach { item ->
                    val bounds = RelocItemHandler.itemBoundsCache[item.id]
                    if (bounds != null) {
                        val isItemVisibleInRail = bounds.top >= safeZones.top.toPx() && bounds.bottom <= (size.height - safeZones.bottom.toPx())
                        
                        if (isItemVisibleInRail) {
                            val itemCenterY = bounds.center.y
                            val descriptionCenterY = itemCenterY // For simplicity, draw straight across
                            val startX = 200f // Arbitrary description X start
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeZones.top, bottom = safeZones.bottom, start = 32.dp, end = 120.dp)
                .verticalScroll(rememberScrollState())
        ) {
            itemsWithInfo.forEach { item ->
                Text(
                    text = "${item.text}: ${item.info}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close Help")
        }
    }
}
