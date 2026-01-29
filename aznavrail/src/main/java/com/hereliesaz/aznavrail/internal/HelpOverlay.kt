package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
    safeZones: AzSafeZones = AzSafeZones(),
    onDescriptionGloballyPositioned: (String, Rect) -> Unit
) {
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
                    )
                    .animateContentSize(),
                horizontalAlignment = Alignment.Start
            ) {
                items(itemsWithInfo, key = { it.id }) { item ->
                    // Calculate visual offset to align with button
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
                                onDescriptionGloballyPositioned(item.id, coordinates.boundsInWindow())
                            }
                    )
                }
            }

            if (isRightDocked) {
                Spacer(modifier = Modifier.width(railWidth))
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
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
