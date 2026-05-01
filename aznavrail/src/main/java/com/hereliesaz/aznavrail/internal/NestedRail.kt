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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

/**
 * Renders a secondary popup rail anchored beside a parent [AzNavItem].
 *
 * The layout is determined by [alignment]: VERTICAL stacks items in a scrollable column;
 * HORIZONTAL lays them out in a scrollable row. Both modes cap size at 80 % of the screen
 * to force scrolling on overflow. Position is handled externally by
 * [DockedCenteredPopupPositionProvider] / [DockedHorizontalPopupPositionProvider].
 *
 * @param parentItem The host item that opened this nested rail.
 * @param items The sub-items to render inside the popup.
 * @param currentDestination The active navigation route; used to highlight matching items.
 * @param activeColor The rail-level active color applied to selected buttons.
 * @param activeClassifiers Classifier strings used for programmatic active-state.
 * @param onItemSelected Invoked with the tapped sub-item.
 * @param alignment Whether to render items vertically or horizontally.
 * @param isRightDocked Whether the rail is docked to the right (affects padding side).
 * @param helpList Forwarded from the scope for potential help-overlay integration.
 * @param onItemGloballyPositioned Reports window-space bounds of each sub-item after layout.
 */
@Composable
internal fun NestedRail(
    parentItem: AzNavItem,
    items: List<AzNavItem>,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    onItemSelected: (AzNavItem) -> Unit,
    alignment: AzNestedRailAlignment,
    isRightDocked: Boolean,
    helpList: Map<String, Any> = emptyMap(),
    onItemGloballyPositioned: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null
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
                androidx.compose.foundation.layout.Box(modifier = Modifier.onGloballyPositioned { coords ->
                    onItemGloballyPositioned?.invoke(item.id, coords.boundsInWindow())
                }) {
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
    } else {
        val scrollState = rememberScrollState()
        Row(
            modifier = modifier.widthIn(max = maxW).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                androidx.compose.foundation.layout.Box(modifier = Modifier.onGloballyPositioned { coords ->
                    onItemGloballyPositioned?.invoke(item.id, coords.boundsInWindow())
                }) {
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
}