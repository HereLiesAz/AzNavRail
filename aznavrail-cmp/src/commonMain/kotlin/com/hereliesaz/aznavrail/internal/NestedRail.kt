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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
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
 * @param rotationDegrees Rotation to apply to buttons "in place".
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
    onItemGloballyPositioned: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    rotationDegrees: Float = 0f,
    onHostExpandedChange: ((String, Boolean) -> Unit)? = null
) {
    // Window size in px → 80% caps in Dp (LocalConfiguration is Android-only).
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val maxH = with(density) { (containerSize.height * 0.8f).toDp() }
    val maxW = with(density) { (containerSize.width * 0.8f).toDp() }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    val surfaceShape = RoundedCornerShape(16.dp)
    val modifier = Modifier
        .then(if (isRightDocked) Modifier.padding(end = 16.dp) else Modifier.padding(start = 16.dp))
        .clip(surfaceShape)
        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), surfaceShape)
        .padding(8.dp)

    if (alignment == AzNestedRailAlignment.VERTICAL) {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier.heightIn(max = maxH).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.filter { !it.isSubItem }.forEach { item ->
                NestedItemWrapper(item, currentDestination, activeColor, activeClassifiers, onItemSelected, rotationDegrees, onItemGloballyPositioned, hostStates, items, true, onHostExpandedChange)
            }
        }
    } else {
        val scrollState = rememberScrollState()
        Row(
            modifier = modifier.widthIn(max = maxW).horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.filter { !it.isSubItem }.forEach { item ->
                NestedItemWrapper(item, currentDestination, activeColor, activeClassifiers, onItemSelected, rotationDegrees, onItemGloballyPositioned, hostStates, items, false, onHostExpandedChange)
            }
        }
    }
}

@Composable
private fun NestedItemWrapper(
    item: AzNavItem,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    onItemSelected: (AzNavItem) -> Unit,
    rotationDegrees: Float,
    onItemGloballyPositioned: ((String, androidx.compose.ui.geometry.Rect) -> Unit)?,
    hostStates: MutableMap<String, Boolean>,
    allItems: List<AzNavItem>,
    isVerticalRail: Boolean,
    onHostExpandedChange: ((String, Boolean) -> Unit)? = null
) {
    // Evict cached bounds when this nested item leaves composition (popup closes). Without
    // this the help overlay would later draw cards/lines for the now-invisible nested rail.
    DisposableEffect(item.id) {
        onDispose { onItemGloballyPositioned?.invoke(item.id, Rect.Zero) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.onGloballyPositioned { coords ->
            // positionInWindow + size so the bounds stay logical (unclipped) when the popup
            // is scrolled. See the matching comment in RailContent.kt.
            val pos = coords.positionInWindow()
            val size = coords.size
            onItemGloballyPositioned?.invoke(
                item.id,
                Rect(
                    left = pos.x,
                    top = pos.y,
                    right = pos.x + size.width,
                    bottom = pos.y + size.height,
                ),
            )
        }) {
            AzNavRailButton(
                onClick = {
                    if (item.isHost) {
                        val newHostState = !(hostStates[item.id] ?: false)
                        hostStates[item.id] = newHostState
                        onHostExpandedChange?.invoke(item.id, newHostState)
                    } else {
                        onItemSelected(item)
                    }
                },
                text = item.text,
                color = item.color ?: MaterialTheme.colorScheme.onSurface,
                activeColor = activeColor,
                textColor = item.textColor,
                fillColor = item.fillColor,
                shape = item.shape ?: AzButtonShape.CIRCLE,
                size = AzNavRailDefaults.ButtonWidth,
                enabled = !item.disabled,
                isSelected = (item.route != null && currentDestination == item.route) || item.classifiers.any { activeClassifiers.contains(it) },
                itemContent = item.content,
                rotationDegrees = rotationDegrees
            )
        }

        if (item.isHost && hostStates[item.id] == true) {
            val subItems = allItems.filter { it.hostId == item.id && it.isSubItem }
            if (isVerticalRail) {
                // Vertical rail: sub-items continue the column
                subItems.forEach { subItem ->
                    NestedItemWrapper(subItem, currentDestination, activeColor, activeClassifiers, onItemSelected, rotationDegrees, onItemGloballyPositioned, hostStates, allItems, isVerticalRail)
                }
            } else {
                // Horizontal rail: sub-items expand downward vertically
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    subItems.forEach { subItem ->
                        NestedItemWrapper(subItem, currentDestination, activeColor, activeClassifiers, onItemSelected, rotationDegrees, onItemGloballyPositioned, hostStates, allItems, isVerticalRail)
                    }
                }
            }
        }
    }
}
