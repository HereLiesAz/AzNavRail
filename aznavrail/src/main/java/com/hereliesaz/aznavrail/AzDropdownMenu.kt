package com.hereliesaz.aznavrail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDropdownAlignment
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
        AzButton(
            onClick = {
                onClick()
                if (closeOnClick) onDismiss()
            },
            text = text,
            modifier = modifier,
            color = color.takeOrElse { MaterialTheme.colorScheme.primary },
            textColor = textColor,
            fillColor = fillColor,
            shape = shape,
            enabled = enabled
        )
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
 * Anchors the unfolded panel to the trigger icon: top/centre anchors open downward (panel below the
 * icon), [AzDropdownAlignment.BOTTOM_START]/`BOTTOM_CENTER`/`BOTTOM_END` open upward (panel above
 * it); start/centre/end pick the horizontal edge the panel lines up with. A fine [offsetXPx]/
 * [offsetYPx] nudges the result.
 */
private class AzDropdownPositionProvider(
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
        val x = when (alignment) {
            AzDropdownAlignment.TOP_START, AzDropdownAlignment.CENTER_START, AzDropdownAlignment.BOTTOM_START ->
                anchorBounds.left
            AzDropdownAlignment.TOP_CENTER, AzDropdownAlignment.CENTER, AzDropdownAlignment.BOTTOM_CENTER ->
                anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
            AzDropdownAlignment.TOP_END, AzDropdownAlignment.CENTER_END, AzDropdownAlignment.BOTTOM_END ->
                anchorBounds.right - popupContentSize.width
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
 * Drop it **inline** anywhere in your own UI, exactly like [AzButton] or [AzTextBox]: there is no
 * `AzNavHost`, no DSL host scope, no `background()`/`onscreen()`, and no reserved safe zones. The
 * composable renders a tappable icon (the hamburger); tapping it unfolds a panel — anchored to the
 * icon via a [Popup] — containing the items you declare in [content]. Tapping outside or pressing
 * back folds it up.
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
 * @param menuWidth Optional fixed width for the panel; wraps its content when [Dp.Unspecified].
 * @param backgroundColor Panel background; defaults to [MaterialTheme]'s surface.
 * @param alignment Where the panel anchors to the icon and which way it unfolds.
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

    val positionProvider = remember(alignment, offset, density) {
        with(density) {
            AzDropdownPositionProvider(
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
                    val scope = AzDropdownMenuScopeImpl(onDismiss = { setOpen(false) })
                    Column(
                        modifier = Modifier
                            .then(if (menuWidth != Dp.Unspecified) Modifier.width(menuWidth) else Modifier)
                            .heightIn(max = maxPanelHeight)
                            .verticalScroll(scrollState)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        scope.content()
                    }
                }
            }
        }
    }
}
