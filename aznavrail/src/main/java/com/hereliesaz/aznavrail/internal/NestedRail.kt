// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/NestedRail.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.hereliesaz.aznavrail.model.AzButtonShape
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
    val configuration = LocalConfiguration.current
    val maxH = (configuration.screenHeightDp * 0.8f).dp
    val maxW = (configuration.screenWidthDp * 0.8f).dp

    val surfaceShape = RoundedCornerShape(16.dp)
    val modifier = Modifier
        .then(if (isRightDocked) Modifier.padding(end = 16.dp) else Modifier.padding(start = 16.dp))
        .clip(surfaceShape)
        // .background(MaterialTheme.colorScheme.surfaceVariant) // Removed background per request
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), surfaceShape)
        .padding(8.dp)

    // Restricted to 80% screen to enforce scrolling constraints
    if (alignment == AzNestedRailAlignment.VERTICAL) {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier.heightIn(max = maxH).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                AzNavRailButton(
                    onClick = { onItemSelected(item) },
                    text = item.text,
                    color = item.color ?: MaterialTheme.colorScheme.onSurface,
                    activeColor = activeColor,
                    textColor = item.textColor,
                    fillColor = item.fillColor,
                    shape = AzButtonShape.CIRCLE,
                    size = AzNavRailDefaults.ButtonWidth,
                    enabled = !item.disabled,
                    isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                    itemContent = item.content
                )
            }
        }
    } else {
        val scrollState = rememberScrollState()
        Row(
            modifier = modifier.widthIn(max = maxW).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                AzNavRailButton(
                    onClick = { onItemSelected(item) },
                    text = item.text,
                    color = item.color ?: MaterialTheme.colorScheme.onSurface,
                    activeColor = activeColor,
                    textColor = item.textColor,
                    fillColor = item.fillColor,
                    shape = AzButtonShape.CIRCLE,
                    size = AzNavRailDefaults.ButtonWidth,
                    enabled = !item.disabled,
                    isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                    itemContent = item.content
                )
            }
        }
    }
}