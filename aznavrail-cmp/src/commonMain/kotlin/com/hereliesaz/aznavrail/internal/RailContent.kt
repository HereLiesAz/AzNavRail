// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailContent.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.lerp
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Renders a single button in the collapsed rail, wiring click behaviour based on the item's type.
 *
 * Handles standard items, toggles, cyclers, host items, nested-rail parents, and relocatable items.
 * When [helpEnabled] is true, only host and help items are interactive.
 *
 * @param defaultShape Fallback shape from the scope when [item.shape] is null.
 * @param item The nav item to render.
 * @param navController Used to trigger route navigation on click.
 * @param isSelected Whether the button is visually highlighted as the active item.
 * @param buttonSize The uniform button size in dp (shrunk when a vertical nested rail is open).
 * @param onClick Primary click callback (null renders the button as inert for relocatable items).
 * @param onRailCyclerClick Invoked when a cycler item is tapped (includes delay-commit logic).
 * @param onItemClick Invoked after click for side-effects like collapsing the menu.
 * @param onHostClick Invoked when a host item is tapped to expand/collapse its sub-items.
 * @param onItemGloballyPositioned Reports window-space bounds to the scope for help/tutorial overlays.
 * @param onBoundsCalculated Secondary bounds callback written directly to the scope's bounds cache.
 * @param helpEnabled When true, disables non-host/non-help interactions and dims other items.
 * @param dragModifier Additional pointer-input modifier attached for relocatable drag handling.
 * @param activeColor Override for the active-state color; falls back to [MaterialTheme.colorScheme.primary].
 */
@Composable
internal fun RailContent(
    defaultShape: com.hereliesaz.aznavrail.model.AzButtonShape,
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    buttonSize: Dp,
    onClick: (() -> Unit)?,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit = {},
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    onBoundsCalculated: ((String, Rect) -> Unit)? = null,
    onBoundsCleared: ((String) -> Unit)? = null,
    helpEnabled: Boolean = false,
    dragModifier: Modifier = Modifier,
    activeColor: androidx.compose.ui.graphics.Color? = null,
    rotationDegrees: Float = 0f,
    /** Reports a slider item's new value. Null for every rail that declares no slider. */
    onSliderChange: ((String, Float) -> Unit)? = null,
    /** Reports a `RANGE` slider item's new span. */
    onSliderRangeChange: ((String, ClosedFloatingPointRange<Float>) -> Unit)? = null,
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val isEnabled = if (helpEnabled) (item.isHost || item.isHelpItem) else !item.disabled

    val finalOnClick: (() -> Unit)? = if (helpEnabled) {
        if (item.isHelpItem) { { onClick?.invoke() } } else if (item.isHost) { { onHostClick() } } else { null }
    } else if (item.isRelocItem) {
        null
    } else {
        if (item.isHost) {
            { handleHostItemClick(item, navController, onClick, onItemClick, onHostClick) }
        } else if (item.isCycler) {
            {
                onRailCyclerClick(item)
                onItemClick()
            }
        } else if (item.isNestedRail) {
            {
                onClick?.invoke()
                // Explicitly do not trigger navigate or collapse for NestedRail parent.
            }
        } else {
            {
                item.route?.let { navController?.navigate(it) }
                onClick?.invoke()
                onItemClick()
            }
        }
    }

    // Evict cached bounds when this item leaves composition. Without this, items that the user
    // can no longer see (menu collapsed, nested-rail popup closed, scrolled off) keep their last
    // reported window-space bounds, and the help overlay happily draws cards and connector lines
    // pointing at those phantom positions.
    DisposableEffect(item.id) {
        onDispose { onBoundsCleared?.invoke(item.id) }
    }

    // Small margin applied uniformly on all sides to all items.
    Box(
        modifier = Modifier
            .padding(4.dp)
            .onGloballyPositioned { coordinates ->
                // Use positionInWindow() + size so the reported bounds reflect the item's
                // logical position — even when the rail's verticalScroll clips it offscreen.
                // boundsInWindow() would collapse to Rect.Zero outside the viewport, which would
                // (a) yank every off-screen line's endpoint to the top-left, and (b) defeat the
                // help-overlay viewport filter's ability to distinguish 'never measured' from
                // 'measured then scrolled off'. The filter's `Rect.overlaps(overlayBounds)` still
                // correctly rejects items whose unclipped bounds sit entirely above/below the
                // overlay viewport.
                val pos = coordinates.positionInWindow()
                val size = coordinates.size
                val bounds = Rect(
                    left = pos.x,
                    top = pos.y,
                    right = pos.x + size.width,
                    bottom = pos.y + size.height,
                )
                onBoundsCalculated?.invoke(item.id, bounds)
                onItemGloballyPositioned?.invoke(item.id, bounds)
            }
            .then(dragModifier)
    ) {
        // A slider item unfolds where it stands. Nothing opens over the rail and nothing moves the
        // user elsewhere: the slot the item occupies grows into the track, and folds back when the
        // value is set. The control ends up exactly where the user's attention already was.
        if (item.isSlider) {
            AzRailSliderItem(
                item = item,
                buttonSize = buttonSize,
                enabled = isEnabled,
                color = item.color ?: activeColor ?: MaterialTheme.colorScheme.primary,
                onValueChange = { onSliderChange?.invoke(item.id, it) },
                onRangeChange = { onSliderRangeChange?.invoke(item.id, it) },
            )
            return@Box
        }

        // While an item owns a notice/warning popup it stops being itself and becomes the alert
        // glyph: a yellow, rounded-corner triangle outline. Everything else about it is untouched,
        // and it reverts the instant the popup is dismissed.
        val alert = item.alert
        val baseShape = item.shape ?: defaultShape
        // The item does not *cut* to a warning triangle — it becomes one, and comes back the same
        // way. The morph is the conveyance; a hard swap is just a different picture.
        val alertProgress by animateFloatAsState(
            targetValue = if (alert != null) 1f else 0f,
            animationSpec = tween(durationMillis = AzMotion.SettleDurationMs, easing = AzEasing.Wp7Decelerate),
            label = "az-alert-morph",
        )
        val baseColor = item.color ?: MaterialTheme.colorScheme.primary
        // Remember the colour the alert was, so the morph *out* has something to fade back from —
        // otherwise the colour snaps to normal on the frame the alert clears, while the shape is
        // still a triangle.
        val lastAlertColor = remember(item.id) { mutableStateOf<androidx.compose.ui.graphics.Color?>(null) }
        val liveAlertColor = alert?.color()
        if (liveAlertColor != null && lastAlertColor.value != liveAlertColor) {
            lastAlertColor.value = liveAlertColor
        }
        val alertColor = liveAlertColor ?: lastAlertColor.value
        val morphedColor = if (alertProgress <= 0f || alertColor == null) baseColor
            else lerp(baseColor, alertColor, alertProgress)
        AzNavRailButton(
            onClick = finalOnClick,
            text = textToShow,
            modifier = Modifier,
            color = morphedColor,
            activeColor = if (alertProgress > 0f && alertColor != null) morphedColor
                else (activeColor ?: MaterialTheme.colorScheme.primary),
            textColor = if (alertProgress > 0f && alertColor != null) morphedColor else item.textColor,
            fillColor = item.fillColor,
            size = buttonSize,
            shape = baseShape,
            shapeOverride = if (alertProgress > 0f) AzAlertMorphShape(baseShape, alertProgress) else null,
            enabled = isEnabled,
            isSelected = isSelected,
            isLoading = item.isLoading,
            // The item's own content fades out as the triangle takes over, rather than vanishing
            // the instant the alert lands.
            itemContent = if (alertProgress >= 0.5f) null else item.content,
            rotationDegrees = rotationDegrees
        )

        var showBadge by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(item.badge) {
            if (!item.badge.isNullOrBlank()) {
                showBadge = true
                if (!item.persistentBadge) {
                    kotlinx.coroutines.delay(1000)
                    showBadge = false
                }
            } else {
                showBadge = false
            }
        }

        if (showBadge && !item.badge.isNullOrBlank()) {
            com.hereliesaz.aznavrail.AzBadge(
                text = item.badge,
                modifier = Modifier.align(Alignment.TopEnd),
                containerColor = item.color ?: activeColor ?: MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Executes the standard click sequence for a host item: toggles expansion, navigates if a route
 * is set, fires the item's own callback, and notifies the general item-click handler.
 */
internal fun handleHostItemClick(
    item: AzNavItem,
    navController: NavController?,
    onClick: (() -> Unit)?,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit
) {
    onHostClick()
    item.route?.let { navController?.navigate(it) }
    onClick?.invoke()
    onItemClick()
}
