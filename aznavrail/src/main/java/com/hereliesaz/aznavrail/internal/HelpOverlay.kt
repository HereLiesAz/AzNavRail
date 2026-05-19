package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.LocalAzSafeZones
import com.hereliesaz.aznavrail.model.AzNavItem

import androidx.compose.foundation.BorderStroke

/**
 * Full-screen overlay that draws connecting lines from rail items to their help cards.
 *
 * Only items that have non-blank [AzNavItem.info], a matching entry in [helpList], or an
 * associated tutorial are shown. Tapping a card expands it; tapping the background dismisses
 * the overlay. If a nested rail is open, the help list shows only the nested items and the
 * start-padding is widened to avoid covering the popup.
 *
 * @param items All items configured in the current rail scope.
 * @param helpLineColors Custom line colors cycling through per-item; defaults to rainbow palette.
 * @param onDismiss Invoked when the user taps the background or a tutorial card's close.
 * @param itemBoundsCache Window-space bounds of each item, used to draw connecting lines.
 * @param helpList Map of item ID → help text (String or string resource Int).
 * @param nestedRailOpenId Scope filter: when non-null, only cards for that nested rail's child items
 *   are shown (overlay triggered from inside a nested rail). When null, the overlay shows main-rail
 *   items (overlay triggered from the main rail).
 * @param tutorials Map of item ID → tutorial definition; enables "Start Tutorial" links in cards.
 * @param onTutorialLaunch Invoked with the item ID when the user taps "Start Tutorial".
 */
@Composable
internal fun HelpOverlay(
    items: List<AzNavItem>,
    helpLineColors: List<Color> = emptyList(),
    onDismiss: () -> Unit,
    itemBoundsCache: Map<String, Rect> = emptyMap(),
    helpList: Map<String, Any> = emptyMap(),
    nestedRailOpenId: String? = null,
    tutorials: Map<String, com.hereliesaz.aznavrail.tutorial.AzTutorial> = emptyMap(),
    onTutorialLaunch: ((String) -> Unit)? = null
) {
    val itemsWithInfo = remember(items, helpList, nestedRailOpenId, tutorials) {
        val source = if (nestedRailOpenId != null) {
            items.find { it.id == nestedRailOpenId }?.nestedRailItems.orEmpty()
        } else {
            items
        }

        source.asSequence().filter { item ->
            val listTextRaw = helpList[item.id]
            val hasValidListText = when (listTextRaw) {
                is String -> listTextRaw.isNotBlank()
                is Int -> listTextRaw != 0
                else -> false
            }
            !item.info.isNullOrBlank() || hasValidListText || tutorials.containsKey(item.id)
        }.toList()
    }
    val safeZones = LocalAzSafeZones.current
    val density = LocalDensity.current

    val cardBoundsCache = remember { mutableStateMapOf<String, Rect>() }
    var expandedItemId by remember { mutableStateOf<String?>(null) }

    // Overlay viewport in window coordinates, set from onGloballyPositioned on the root Box.
    // Used to filter out rail items that have been scrolled out of the visible area: their help
    // card and connecting line should be suppressed entirely. Help cards that are themselves
    // scrolled offscreen keep updating their bounds via the card's own onGloballyPositioned, so
    // the line endpoint follows the card's logical position even when the card is offscreen.
    var overlayBounds by remember { mutableStateOf(Rect.Zero) }

    fun isRailItemOnscreen(item: AzNavItem): Boolean {
        val rb = itemBoundsCache[item.id] ?: return false
        if (rb.width <= 0f || rb.height <= 0f) return false
        if (overlayBounds == Rect.Zero) return true
        return rb.overlaps(overlayBounds)
    }

    val visibleItemsWithInfo = itemsWithInfo.filter(::isRailItemOnscreen)

    // Calculate dynamic padding to avoid overlapping nested rails
    val isNestedRailOpen = nestedRailOpenId != null
    val dynamicStartPadding = if (isNestedRailOpen) 240.dp else 120.dp
    val defaultColors = listOf(Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow)
    val colorPalette = if (helpLineColors.isNotEmpty()) helpLineColors else defaultColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayBounds = it.boundsInWindow() }
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss) // Background tap to dismiss
            .drawBehind {
                val strokeWidth = 4.dp

                visibleItemsWithInfo.forEachIndexed { index, item ->
                    val drawColor = colorPalette[index % colorPalette.size]
                    val itemBounds = itemBoundsCache[item.id]
                    val cardBounds = cardBoundsCache[item.id]

                    if (itemBounds != null && cardBounds != null) {
                        // Endpoints are in window coordinates. Even when the card is scrolled
                        // outside the viewport its bounds keep updating via onGloballyPositioned,
                        // so the connector line continues to follow the card's logical position;
                        // Compose clips the portion of the line outside the overlay box.
                        val start = Offset(itemBounds.right, itemBounds.center.y)
                        val end = Offset(cardBounds.left, cardBounds.center.y)

                        drawLine(
                            color = drawColor,
                            start = start,
                            end = end,
                            strokeWidth = strokeWidth.toPx()
                        )
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = dynamicStartPadding, end = 16.dp) // Leave space for rail + nested rail
                .padding(top = safeZones.top, bottom = safeZones.bottom)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Equivalent to top contentPadding
            visibleItemsWithInfo.forEachIndexed { index, item ->
                val isExpanded = expandedItemId == item.id
                val hasTutorial = tutorials.containsKey(item.id)
                val cardColor = colorPalette[index % colorPalette.size]

                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RectangleShape,
                    border = BorderStroke(2.dp, cardColor)
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            cardBoundsCache[item.id] = coords.boundsInWindow()
                        }
                        .clickable {
                            expandedItemId = if (isExpanded) null else item.id
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.text.ifBlank { "Item ${item.id}" },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val infoText = item.info
                    val listTextRaw = helpList[item.id]
                    val listText = when (listTextRaw) {
                        is Int -> if (listTextRaw != 0) stringResource(id = listTextRaw) else null
                        is String -> listTextRaw
                        else -> null
                    }

                    if (!infoText.isNullOrBlank()) {
                        Text(
                            text = infoText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!listText.isNullOrBlank()) {
                        if (!infoText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = listText,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isExpanded) {
                        if (hasTutorial) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start Tutorial",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onTutorialLaunch?.invoke(item.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to collapse",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    } else if (hasTutorial) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tutorial available (Tap to expand)",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Equivalent to bottom contentPadding
        }
    }
}
