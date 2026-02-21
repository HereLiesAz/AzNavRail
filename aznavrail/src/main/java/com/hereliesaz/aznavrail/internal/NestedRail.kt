package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

@Composable
internal fun NestedRail(
    parentItem: AzNavItem,
    items: List<AzNavItem>,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    onItemSelected: (AzNavItem) -> Unit,
    alignment: AzNestedRailAlignment,
    isRightDocked: Boolean
) {
    val density = LocalDensity.current
    val windowHeight = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val anchorBounds = RelocItemHandler.itemBoundsCache[parentItem.id]

    val yOffset = if (alignment == AzNestedRailAlignment.VERTICAL && anchorBounds != null) {
        ((windowHeight / 2f) - anchorBounds.top).toInt()
    } else 0

    // FIXED: Corrected the Modifier.then extension usage for dynamic padding
    val modifier = Modifier
        .then(if (isRightDocked) Modifier.padding(end = 16.dp) else Modifier.padding(start = 16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
        .padding(8.dp)

    if (alignment == AzNestedRailAlignment.VERTICAL) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                AzNavRailButton(
                    onClick = { onItemSelected(item) },
                    text = item.text,
                    color = item.color ?: MaterialTheme.colorScheme.onSurface,
                    activeColor = activeColor,
                    shape = item.shape ?: com.hereliesaz.aznavrail.model.AzButtonShape.CIRCLE,
                    enabled = !item.disabled,
                    isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                    itemContent = item.content
                )
            }
        }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                AzNavRailButton(
                    onClick = { onItemSelected(item) },
                    text = item.text,
                    color = item.color ?: MaterialTheme.colorScheme.onSurface,
                    activeColor = activeColor,
                    shape = item.shape ?: com.hereliesaz.aznavrail.model.AzButtonShape.CIRCLE,
                    enabled = !item.disabled,
                    isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                    itemContent = item.content
                )
            }
        }
    }
}