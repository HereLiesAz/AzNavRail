package com.hereliesaz.aznavrail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDropdownAlignment
import com.hereliesaz.aznavrail.model.AzDropdownDesign
import com.hereliesaz.aznavrail.model.AzHeaderIconShape

/**
 * Builder scope for the items inside an [AzDropdownMenu].
 *
 * The builders reuse the library's own standalone widgets ([AzButton], [AzToggle], [AzCycler],
 * [AzDivider]) so a drop-down's entries carry the exact AzNavRail aesthetic — this is **not** the
 * rail/menu `AzNavItem` machinery. Tapping an [azItem] runs its callback and (by default) folds the
 * menu back up; call [dismiss] to close it from anywhere.
 */
interface AzDropdownMenuScope {
    /** Folds the menu back up. */
    fun dismiss()

    /**
     * A tappable entry rendered as an [AzButton].
     *
     * @param closeOnClick When true (default) the menu folds up after [onClick] runs.
     */
    @Composable
    fun azItem(
        text: String,
        modifier: Modifier = Modifier,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        closeOnClick: Boolean = true,
        onClick: () -> Unit
    )

    /** A two-state entry rendered as an [AzToggle]. Stays open by default. */
    @Composable
    fun azToggle(
        isChecked: Boolean,
        toggleOnText: String,
        toggleOffText: String,
        modifier: Modifier = Modifier,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        closeOnClick: Boolean = false,
        onToggle: (Boolean) -> Unit
    )

    /** A rotating entry rendered as an [AzCycler]. Stays open by default. */
    @Composable
    fun azCycler(
        options: List<String>,
        selectedOption: String,
        modifier: Modifier = Modifier,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        closeOnClick: Boolean = false,
        onCycle: (String) -> Unit
    )

    /** A separator rendered as an [AzDivider]. */
    @Composable
    fun azDivider()

    /** Escape hatch for arbitrary composable content inside the panel. */
    @Composable
    fun azCustom(content: @Composable () -> Unit)
}

private class AzDropdownMenuScopeImpl(
    private val design: AzDropdownDesign,
    private val onDismiss: () -> Unit
) : AzDropdownMenuScope {

    override fun dismiss() = onDismiss()

    @Composable
    override fun azItem(
        text: String,
        modifier: Modifier,
        shape: AzButtonShape,
        enabled: Boolean,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        closeOnClick: Boolean,
        onClick: () -> Unit
    ) {
        val action = {
            onClick()
            if (closeOnClick) onDismiss()
        }
        if (design == AzDropdownDesign.MENU) {
            AzDropdownMenuRow(
                text = text,
                modifier = modifier,
                enabled = enabled,
                textColor = effectiveTextColor(textColor, color),
                onClick = action
            )
        } else {
            AzButton(
                onClick = action,
                text = text,
                modifier = modifier,
                color = color.takeOrElse { MaterialTheme.colorScheme.primary },
                textColor = textColor,
                fillColor = fillColor,
                shape = shape,
                enabled = enabled
            )
        }
    }

    @Composable
    override fun azToggle(
        isChecked: Boolean,
        toggleOnText: String,
        toggleOffText: String,
        modifier: Modifier,
        shape: AzButtonShape,
        enabled: Boolean,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        closeOnClick: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        if (design == AzDropdownDesign.MENU) {
            AzDropdownMenuRow(
                text = if (isChecked) toggleOnText else toggleOffText,
                modifier = modifier,
                enabled = enabled,
                textColor = effectiveTextColor(textColor, color),
                onClick = {
                    onToggle(!isChecked)
                    if (closeOnClick) onDismiss()
                }
            )
        } else {
            AzToggle(
                isChecked = isChecked,
                onToggle = {
                    onToggle(it)
                    if (closeOnClick) onDismiss()
                },
                toggleOnText = toggleOnText,
                toggleOffText = toggleOffText,
                modifier = modifier,
                color = color.takeOrElse { MaterialTheme.colorScheme.primary },
                textColor = textColor,
                fillColor = fillColor,
                shape = shape,
                enabled = enabled
            )
        }
    }

    @Composable
    override fun azCycler(
        options: List<String>,
        selectedOption: String,
        modifier: Modifier,
        shape: AzButtonShape,
        enabled: Boolean,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        closeOnClick: Boolean,
        onCycle: (String) -> Unit
    ) {
        if (design == AzDropdownDesign.MENU) {
            AzDropdownMenuRow(
                text = selectedOption,
                modifier = modifier,
                enabled = enabled,
                textColor = effectiveTextColor(textColor, color),
                onClick = {
                    if (options.isNotEmpty()) {
                        val next = options[(options.indexOf(selectedOption) + 1).mod(options.size)]
                        onCycle(next)
                    }
                    if (closeOnClick) onDismiss()
                }
            )
        } else {
            AzCycler(
                options = options,
                selectedOption = selectedOption,
                onCycle = {
                    onCycle(it)
                    if (closeOnClick) onDismiss()
                },
                modifier = modifier,
                color = color.takeOrElse { MaterialTheme.colorScheme.primary },
                textColor = textColor,
                fillColor = fillColor,
                shape = shape,
                enabled = enabled
            )
        }
    }

    @Composable
    override fun azDivider() {
        AzDivider()
    }

    @Composable
    override fun azCustom(content: @Composable () -> Unit) {
        content()
    }
}

/**
 * Resolves the row text colour: explicit [textColor], else [color], else the theme primary. Always
 * returns a *specified* colour so callers can safely `copy(alpha = …)` it.
 */
@Composable
private fun effectiveTextColor(textColor: Color?, color: Color): Color =
    (textColor ?: color).takeOrElse { MaterialTheme.colorScheme.primary }

/**
 * A full-width, labeled menu row mirroring the expanded drawer's look (centred [titleLarge] text,
 * the menu's horizontal/vertical padding). Used for [AzDropdownDesign.MENU] items.
 */
@Composable
private fun AzDropdownMenuRow(
    text: String,
    modifier: Modifier,
    enabled: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = AzNavRailDefaults.MenuItemHorizontalPadding,
                vertical = AzNavRailDefaults.MenuItemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            text.split('\n').forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Pins the dropped panel to a **screen edge** horizontally and **drops it from the trigger**
 * vertically. The anchor's start/centre/end picks the horizontal side — start hugs the left window
 * edge, end hugs the right, centre is window-centred — ignoring the trigger's x. Top/centre anchors
 * open downward from the trigger's bottom, [AzDropdownAlignment.BOTTOM_START]/`BOTTOM_CENTER`/
 * `BOTTOM_END` open upward from its top. A fine [offsetXPx]/[offsetYPx] nudges the result.
 */
private class AzDropdownEdgePositionProvider(
    private val alignment: AzDropdownAlignment,
    private val offsetXPx: Int,
    private val offsetYPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // `start`/`end` are layout-direction-relative: start hugs the leading edge (left in LTR,
        // right in RTL), end the trailing edge.
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val x = when (alignment) {
            AzDropdownAlignment.TOP_START, AzDropdownAlignment.CENTER_START, AzDropdownAlignment.BOTTOM_START ->
                if (isRtl) windowSize.width - popupContentSize.width else 0
            AzDropdownAlignment.TOP_CENTER, AzDropdownAlignment.CENTER, AzDropdownAlignment.BOTTOM_CENTER ->
                (windowSize.width - popupContentSize.width) / 2
            AzDropdownAlignment.TOP_END, AzDropdownAlignment.CENTER_END, AzDropdownAlignment.BOTTOM_END ->
                if (isRtl) 0 else windowSize.width - popupContentSize.width
        }
        val y = if (alignment.isBottom) {
            anchorBounds.top - popupContentSize.height
        } else {
            anchorBounds.bottom
        }
        return IntOffset(x + offsetXPx, y + offsetYPx)
    }
}

/**
 * A standalone, hamburger-style drop-down menu — used the usual, expected way.
 *
 * Drop the **icon** inline anywhere in your own UI, exactly like [AzButton] or [AzTextBox] — it
 * takes a normal layout slot, like a hamburger button. There is no `AzNavHost`, no DSL host scope,
 * no `background()`/`onscreen()`, and no reserved safe zones. Tapping the icon unfolds a [Popup]
 * panel of the items you declare in [content]; the panel is presented as a slice of the rail
 * ([AzDropdownDesign.RAIL]) or menu ([AzDropdownDesign.MENU]) — width-constrained to match — and is
 * **pinned to the left or right screen edge** ([alignment] start/end) while dropping from the
 * trigger. Tapping outside or pressing back folds it up.
 *
 * Items reuse the library's own widgets through [AzDropdownMenuScope]:
 * ```
 * AzDropdownMenu(icon = painterResource(R.drawable.menu)) {
 *     azItem("Settings") { openSettings() }
 *     azToggle(isChecked = dark, toggleOnText = "Dark", toggleOffText = "Light") { dark = it }
 *     azDivider()
 *     azItem("Sign out") { signOut() }
 * }
 * ```
 *
 * @param modifier Modifier applied to the trigger icon's box — place/position it like any widget.
 * @param icon The hamburger icon. When null, a default [Icons.Default.Menu] is shown.
 * @param contentDescription Accessibility label for the trigger.
 * @param iconSize Diameter of the trigger icon.
 * @param iconShape Clip shape for the trigger icon.
 * @param iconTint Tint for the default [Icons.Default.Menu] icon (ignored for image [icon]s).
 * @param design Whether the dropped panel imitates the collapsed [AzDropdownDesign.RAIL] (compact
 *   buttons, rail width) or the expanded [AzDropdownDesign.MENU] (labeled rows, menu width).
 * @param menuWidth Optional fixed width for the panel; when [Dp.Unspecified] the width follows
 *   [design] (≈100dp for RAIL, ≈160dp for MENU).
 * @param backgroundColor Panel background; defaults to [MaterialTheme]'s surface.
 * @param alignment Which screen edge the panel pins to (start = left, end = right, centre =
 *   centred) and which way it drops from the trigger.
 * @param offset A fine nudge applied to the panel from its anchor.
 * @param vibrate Haptic feedback when the trigger is tapped.
 * @param expanded Optional controlled open-state. When null the menu manages its own state.
 * @param onExpandedChange Called whenever the open-state changes.
 * @param content The menu's items, declared via [AzDropdownMenuScope].
 */
@Composable
fun AzDropdownMenu(
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    contentDescription: String = "Menu",
    iconSize: Dp = 48.dp,
    iconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
    iconTint: Color = Color.Unspecified,
    design: AzDropdownDesign = AzDropdownDesign.MENU,
    menuWidth: Dp = Dp.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    alignment: AzDropdownAlignment = AzDropdownAlignment.TOP_START,
    offset: DpOffset = DpOffset.Zero,
    vibrate: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: @Composable AzDropdownMenuScope.() -> Unit
) {
    var internalOpen by rememberSaveable { mutableStateOf(false) }
    val isOpen = expanded ?: internalOpen
    val setOpen: (Boolean) -> Unit = { value ->
        if (expanded == null) internalOpen = value
        onExpandedChange?.invoke(value)
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    // The panel matches the rail/menu it imitates: rail width for RAIL, menu width for MENU,
    // unless the developer pins an explicit menuWidth.
    val panelWidth = if (menuWidth != Dp.Unspecified) {
        menuWidth
    } else if (design == AzDropdownDesign.RAIL) {
        100.dp
    } else {
        160.dp
    }

    val positionProvider = remember(alignment, offset, density) {
        with(density) {
            AzDropdownEdgePositionProvider(
                alignment = alignment,
                offsetXPx = offset.x.roundToPx(),
                offsetYPx = offset.y.roundToPx()
            )
        }
    }

    val clipShape = when (iconShape) {
        AzHeaderIconShape.CIRCLE -> CircleShape
        AzHeaderIconShape.ROUNDED -> RoundedCornerShape(12.dp)
        else -> RoundedCornerShape(0.dp)
    }

    Box(modifier = modifier) {
        // The inline hamburger trigger.
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(clipShape)
                .clickable {
                    setOpen(!isOpen)
                    if (vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(iconSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = contentDescription,
                    tint = iconTint.takeOrElse { androidx.compose.material3.LocalContentColor.current }
                )
            }
        }

        if (isOpen) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { setOpen(false) },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    color = backgroundColor.takeOrElse { MaterialTheme.colorScheme.surface },
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    val scrollState = rememberScrollState()
                    val scope = AzDropdownMenuScopeImpl(design = design, onDismiss = { setOpen(false) })
                    Column(
                        modifier = Modifier
                            .width(panelWidth)
                            .heightIn(max = maxPanelHeight)
                            .verticalScroll(scrollState)
                            .then(if (design == AzDropdownDesign.RAIL) Modifier.padding(8.dp) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        scope.content()
                    }
                }
            }
        }
    }
}
