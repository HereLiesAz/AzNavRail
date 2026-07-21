package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzMenuItemAlignment
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Composable for displaying a single item in the expanded menu.
 *
 * This composable handles the display and interaction for all types of
 * menu items, including standard, toggle, and cycler items. It supports
 * multi-line text with indentation for all lines after the first.
 *
 * @param item The navigation item to display.
 * @param onCyclerClick The click handler for cycler items, which includes
 *    the delay logic.
 * @param onToggle The click handler for toggling the rail's expanded
 *    state. This is called immediately for standard and toggle items, and
 *    with a delay for cycler items.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MenuItem(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onCyclerClick: (() -> Unit)? = null,
    onToggle: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onHostClick: () -> Unit = {},
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    onBoundsCleared: ((String) -> Unit)? = null,
    helpEnabled: Boolean = false,
    activeColor: androidx.compose.ui.graphics.Color? = null,
    kineticModifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    dockingSide: AzDockingSide = AzDockingSide.LEFT,
    menuItemAlignment: AzMenuItemAlignment = AzMenuItemAlignment.SIDE,
    justifyMenuItems: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Evict cached bounds when this menu entry leaves composition. The menu re-mounts every time
    // the rail expands, so without this, items keep their last position from the previous open
    // and the help overlay draws cards/lines to those phantom locations when invoked from the
    // collapsed rail.
    DisposableEffect(item.id) {
        onDispose { onBoundsCleared?.invoke(item.id) }
    }

    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.menuToggleOnText ?: item.toggleOnText else item.menuToggleOffText ?: item.toggleOffText
        item.isCycler -> {
            // Find the index of the selected option, and use the menu option at that index if available.
            // If configuration is inconsistent, log a warning so it is visible during development.
            val index = item.options?.indexOf(item.selectedOption) ?: -1
            val menuOptions = item.menuOptions

            if (index != -1 && menuOptions != null && index in menuOptions.indices) {
                menuOptions[index]
            } else {
                if (menuOptions != null) {
                    Log.w(
                        "MenuItem",
                        "Cycler configuration mismatch for item='${item.text}': " +
                            "selectedOption='${item.selectedOption}', " +
                            "index=$index, " +
                            "optionsSize=${item.options?.size}, " +
                            "menuOptionsSize=${menuOptions.size}"
                    )
                }
                item.selectedOption ?: ""
            }
        }
        else -> item.menuText ?: item.text
    }

    // Interaction Logic:
    // If helpEnabled: only Host and Help items are interactive.
    // If normal: depends on item.disabled.

    // Visual Logic:
    // If helpEnabled: non-Host/Help items look disabled (grey).
    // If normal: disabled items look disabled.

    val isDisabled = if (helpEnabled) !(item.isHost || item.isHelpItem) else item.disabled

    val modifier = if (isDisabled) Modifier else {
        if (helpEnabled) {
             // Host or Help item in helpEnabled mode is interactive
             Modifier.clickable(interactionSource = interactionSource, indication = null) {
                 if (item.isHelpItem) onClick?.invoke() else onHostClick()
             }
        } else {
            // Normal mode
            if (item.isToggle) {
                Modifier.toggleable(
                    value = item.isChecked ?: false,
                    interactionSource = interactionSource,
                    indication = null,
                    onValueChange = {
                        item.route?.let { navController?.navigate(it) }
                        onClick?.invoke()
                        onToggle()
                        onItemClick()
                    }
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onLongClick = onLongClick,
                    onClick = {
                        if (item.isHost) {
                            handleHostItemClick(item, navController, onClick, onItemClick, onHostClick)
                        } else if (item.isCycler) {
                            onCyclerClick?.invoke()
                        } else {
                            item.route?.let { navController?.navigate(it) }
                            onClick?.invoke()
                            onToggle()
                            onItemClick()
                        }
                    }
                )
            }
        }
    }

    val effectiveActiveColor = activeColor ?: MaterialTheme.colorScheme.primary
    val effectiveDefaultColor = item.textColor ?: item.color ?: MaterialTheme.colorScheme.primary

    val textColor = if (isDisabled) {
        effectiveDefaultColor.copy(alpha = 0.5f)
    } else {
        effectiveDefaultColor
    }

    val backgroundColor = if (isSelected && !isPressed) {
        (item.fillColor ?: effectiveActiveColor).copy(alpha = 0.12f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Box(
        modifier = kineticModifier
            .onGloballyPositioned { coordinates ->
                // positionInWindow + size so the bounds stay logical (unclipped) when the menu
                // is scrolled or partly off-screen. See the matching comment in RailContent.kt.
                val pos = coordinates.positionInWindow()
                val size = coordinates.size
                onItemGloballyPositioned?.invoke(
                    item.id,
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + size.width,
                        bottom = pos.y + size.height,
                    ),
                )
            }
            .background(backgroundColor)
    ) {
        val textAlign = when (menuItemAlignment) {
            AzMenuItemAlignment.CENTER -> TextAlign.Center
            AzMenuItemAlignment.SIDE ->
                if (dockingSide == AzDockingSide.RIGHT) TextAlign.End else TextAlign.Start
        }
        val columnAlignment = when (menuItemAlignment) {
            AzMenuItemAlignment.CENTER -> Alignment.CenterHorizontally
            AzMenuItemAlignment.SIDE ->
                if (dockingSide == AzDockingSide.RIGHT) Alignment.End else Alignment.Start
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AzNavRailDefaults.MenuItemHorizontalPadding,
                    vertical = AzNavRailDefaults.MenuItemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val lines = textToShow.split('\n')
            val mergedStyle = MaterialTheme.typography.titleLarge.merge(textStyle)
            // Hybrid justify: kerning up to `α·fontSize`, then grow the font past that limit so both
            // letter-spacing and font-scale reach a stable mix that fills the row. See [solveHybridJustify].
            val textMeasurer = rememberTextMeasurer()
            val density = LocalDensity.current

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = columnAlignment
            ) {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val availableWidthPx = with(density) { maxWidth.toPx() }
                    val baseFontSizePx = with(density) {
                        mergedStyle.fontSize.takeIf { it.isSpecified }?.toPx()
                            ?: MaterialTheme.typography.titleLarge.fontSize.toPx()
                    }
                    Column(horizontalAlignment = columnAlignment) {
                        lines.forEach { line ->
                            val (scale, kerningPx) = if (!justifyMenuItems || line.length < 2) 1f to 0f
                            else {
                                val naturalWidthPx = try {
                                    textMeasurer.measure(text = line, style = mergedStyle).size.width.toFloat()
                                } catch (_: Throwable) { availableWidthPx }
                                solveHybridJustify(
                                    naturalWidthPx = naturalWidthPx,
                                    rowWidthPx = availableWidthPx,
                                    charCount = line.length,
                                    baseFontSizePx = baseFontSizePx,
                                )
                            }
                            val scaledFontSize = if (scale == 1f) mergedStyle.fontSize
                                else with(density) { (baseFontSizePx * scale).toSp() }
                            Text(
                                text = line,
                                style = mergedStyle.copy(fontSize = scaledFontSize),
                                color = textColor,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = textAlign,
                                letterSpacing = with(density) { kerningPx.toSp() },
                                // Line breaks in menu labels are explicit-only. Auto-wrap here
                                // used to push a single character onto a new line ("Generat\ne"
                                // for "Generate") when the natural width overflowed the row —
                                // the solver's shrink branch now handles overflow by scaling the
                                // font down, so we can safely lock the render to one line.
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }

            var showBadge by remember { mutableStateOf(false) }
            LaunchedEffect(item.badge) {
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
                    modifier = Modifier.padding(start = 8.dp),
                    containerColor = item.color ?: effectiveActiveColor,
                )
            }
        }
    }
}
