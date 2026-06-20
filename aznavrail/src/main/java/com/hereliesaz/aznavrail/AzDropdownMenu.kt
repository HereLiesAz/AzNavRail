package com.hereliesaz.aznavrail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzDropdownDesign

/** Fixed diameter of the drop-down's app-icon trigger. Not customizable, in the AzNavRail tradition. */
private val AzDropdownTriggerSize = 48.dp

/**
 * DSL builder scope for an [AzDropdownMenu] — declared the same way as the rail
 * (`AzDropdownMenu { azConfig(...); azItem(...) { } }`).
 *
 * In keeping with AzNavRail's opinionated tradition, the drop-down only accepts configuration the
 * rest of the library sanctions: [azConfig] mirrors the rail's `azConfig` (design, docking side,
 * vibration, the collapsed/expanded widths) and the item builders accept only the per-item knobs the
 * rail's items accept (`color`/`textColor`/`fillColor`/`shape`/`enabled`, plus a navigation `route`).
 * There is no arbitrary panel background, offset, icon styling, or free composable escape hatch.
 */
interface AzDropdownMenuScope {
    /**
     * Configures the panel, mirroring the rail's `azConfig`/`azTheme`. Call at most once.
     *
     * @param design Whether the panel imitates the collapsed [AzDropdownDesign.RAIL] (compact rail
     *   buttons at [collapsedWidth]) or the expanded [AzDropdownDesign.MENU] (labeled rows at
     *   [expandedWidth]).
     * @param dockingSide Which screen edge the panel pins to ([AzDockingSide.LEFT]/[AzDockingSide.RIGHT]).
     * @param vibrate Haptic feedback when the trigger is tapped.
     * @param expandedWidth Panel width in the [AzDropdownDesign.MENU] design.
     * @param collapsedWidth Panel width in the [AzDropdownDesign.RAIL] design.
     */
    fun azConfig(
        design: AzDropdownDesign = AzDropdownDesign.MENU,
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        vibrate: Boolean = false,
        expandedWidth: Dp = 160.dp,
        collapsedWidth: Dp = 100.dp
    )

    /**
     * A tappable entry. Navigates to [route] (if set, via the menu's `navController`), runs [onClick],
     * then folds the menu up when [closeOnClick] is true (the default).
     */
    fun azItem(
        text: String,
        route: String? = null,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        closeOnClick: Boolean = true,
        onClick: () -> Unit = {}
    )

    /** A two-state entry. Stays open by default. */
    fun azToggle(
        isChecked: Boolean,
        toggleOnText: String,
        toggleOffText: String,
        route: String? = null,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        closeOnClick: Boolean = false,
        onToggle: (Boolean) -> Unit
    )

    /** A rotating entry. Stays open by default. */
    fun azCycler(
        options: List<String>,
        selectedOption: String,
        route: String? = null,
        color: Color = Color.Unspecified,
        textColor: Color? = null,
        fillColor: Color? = null,
        shape: AzButtonShape = AzButtonShape.RECTANGLE,
        enabled: Boolean = true,
        closeOnClick: Boolean = false,
        onCycle: (String) -> Unit
    )

    /** A separator rendered as an [AzDivider]. */
    fun azDivider()
}

/** Resolved drop-down configuration collected from [AzDropdownMenuScope.azConfig]. */
internal data class AzDropdownConfig(
    val design: AzDropdownDesign = AzDropdownDesign.MENU,
    val dockingSide: AzDockingSide = AzDockingSide.LEFT,
    val vibrate: Boolean = false,
    val expandedWidth: Dp = 160.dp,
    val collapsedWidth: Dp = 100.dp
)

/** One declared entry, collected by the builder and rendered by the composable. */
internal sealed interface AzDropdownEntry {
    val route: String?

    data class Item(
        val text: String,
        override val route: String?,
        val color: Color,
        val textColor: Color?,
        val fillColor: Color?,
        val shape: AzButtonShape,
        val enabled: Boolean,
        val closeOnClick: Boolean,
        val onClick: () -> Unit
    ) : AzDropdownEntry

    data class Toggle(
        val isChecked: Boolean,
        val toggleOnText: String,
        val toggleOffText: String,
        override val route: String?,
        val color: Color,
        val textColor: Color?,
        val fillColor: Color?,
        val shape: AzButtonShape,
        val enabled: Boolean,
        val closeOnClick: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : AzDropdownEntry

    data class Cycler(
        val options: List<String>,
        val selectedOption: String,
        override val route: String?,
        val color: Color,
        val textColor: Color?,
        val fillColor: Color?,
        val shape: AzButtonShape,
        val enabled: Boolean,
        val closeOnClick: Boolean,
        val onCycle: (String) -> Unit
    ) : AzDropdownEntry

    object Divider : AzDropdownEntry {
        override val route: String? = null
    }
}

/**
 * Collects the [azConfig] result and the declared entries. Like the rail's scope, it is a plain
 * (non-`@Composable`) builder; the composable creates a fresh one each recomposition, runs the DSL
 * over it, then renders from [config] and [entries].
 */
private class AzDropdownMenuScopeImpl : AzDropdownMenuScope {
    var config = AzDropdownConfig()
        private set
    val entries = mutableListOf<AzDropdownEntry>()

    override fun azConfig(
        design: AzDropdownDesign,
        dockingSide: AzDockingSide,
        vibrate: Boolean,
        expandedWidth: Dp,
        collapsedWidth: Dp
    ) {
        config = AzDropdownConfig(design, dockingSide, vibrate, expandedWidth, collapsedWidth)
    }

    override fun azItem(
        text: String,
        route: String?,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        shape: AzButtonShape,
        enabled: Boolean,
        closeOnClick: Boolean,
        onClick: () -> Unit
    ) {
        entries += AzDropdownEntry.Item(text, route, color, textColor, fillColor, shape, enabled, closeOnClick, onClick)
    }

    override fun azToggle(
        isChecked: Boolean,
        toggleOnText: String,
        toggleOffText: String,
        route: String?,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        shape: AzButtonShape,
        enabled: Boolean,
        closeOnClick: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        entries += AzDropdownEntry.Toggle(
            isChecked, toggleOnText, toggleOffText, route, color, textColor, fillColor, shape, enabled, closeOnClick, onToggle
        )
    }

    override fun azCycler(
        options: List<String>,
        selectedOption: String,
        route: String?,
        color: Color,
        textColor: Color?,
        fillColor: Color?,
        shape: AzButtonShape,
        enabled: Boolean,
        closeOnClick: Boolean,
        onCycle: (String) -> Unit
    ) {
        entries += AzDropdownEntry.Cycler(
            options, selectedOption, route, color, textColor, fillColor, shape, enabled, closeOnClick, onCycle
        )
    }

    override fun azDivider() {
        entries += AzDropdownEntry.Divider
    }
}

/** Resolves the row text colour: explicit [textColor], else [color], else the theme primary. */
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
    enabled: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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

/** Renders one collected [AzDropdownEntry] in the panel, wiring navigation + dismissal. */
@Composable
private fun AzDropdownEntryItem(
    entry: AzDropdownEntry,
    design: AzDropdownDesign,
    navController: NavController?,
    dismiss: () -> Unit
) {
    fun navigate(route: String?) {
        route?.let { navController?.navigate(it) }
    }
    when (entry) {
        is AzDropdownEntry.Item -> {
            val action = {
                navigate(entry.route)
                entry.onClick()
                if (entry.closeOnClick) dismiss()
            }
            if (design == AzDropdownDesign.MENU) {
                AzDropdownMenuRow(entry.text, entry.enabled, effectiveTextColor(entry.textColor, entry.color), action)
            } else {
                AzButton(
                    onClick = action,
                    text = entry.text,
                    color = entry.color.takeOrElse { MaterialTheme.colorScheme.primary },
                    textColor = entry.textColor,
                    fillColor = entry.fillColor,
                    shape = entry.shape,
                    enabled = entry.enabled
                )
            }
        }

        is AzDropdownEntry.Toggle -> {
            if (design == AzDropdownDesign.MENU) {
                AzDropdownMenuRow(
                    text = if (entry.isChecked) entry.toggleOnText else entry.toggleOffText,
                    enabled = entry.enabled,
                    textColor = effectiveTextColor(entry.textColor, entry.color),
                    onClick = {
                        navigate(entry.route)
                        entry.onToggle(!entry.isChecked)
                        if (entry.closeOnClick) dismiss()
                    }
                )
            } else {
                AzToggle(
                    isChecked = entry.isChecked,
                    onToggle = {
                        navigate(entry.route)
                        entry.onToggle(it)
                        if (entry.closeOnClick) dismiss()
                    },
                    toggleOnText = entry.toggleOnText,
                    toggleOffText = entry.toggleOffText,
                    color = entry.color.takeOrElse { MaterialTheme.colorScheme.primary },
                    textColor = entry.textColor,
                    fillColor = entry.fillColor,
                    shape = entry.shape,
                    enabled = entry.enabled
                )
            }
        }

        is AzDropdownEntry.Cycler -> {
            if (design == AzDropdownDesign.MENU) {
                AzDropdownMenuRow(
                    text = entry.selectedOption,
                    enabled = entry.enabled,
                    textColor = effectiveTextColor(entry.textColor, entry.color),
                    onClick = {
                        if (entry.options.isNotEmpty()) {
                            val next = entry.options[(entry.options.indexOf(entry.selectedOption) + 1).mod(entry.options.size)]
                            navigate(entry.route)
                            entry.onCycle(next)
                        }
                        if (entry.closeOnClick) dismiss()
                    }
                )
            } else {
                AzCycler(
                    options = entry.options,
                    selectedOption = entry.selectedOption,
                    onCycle = {
                        navigate(entry.route)
                        entry.onCycle(it)
                        if (entry.closeOnClick) dismiss()
                    },
                    color = entry.color.takeOrElse { MaterialTheme.colorScheme.primary },
                    textColor = entry.textColor,
                    fillColor = entry.fillColor,
                    shape = entry.shape,
                    enabled = entry.enabled
                )
            }
        }

        AzDropdownEntry.Divider -> AzDivider()
    }
}

/**
 * Pins the dropped panel to the [dockingSide] **screen edge** and **drops it from the trigger**: it
 * opens downward from the trigger's bottom when there is room, otherwise upward from its top. Like
 * the rail, the side is physical ([AzDockingSide.LEFT] → left edge, [AzDockingSide.RIGHT] → right).
 */
private class AzDropdownEdgePositionProvider(
    private val dockingSide: AzDockingSide
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = if (dockingSide == AzDockingSide.LEFT) 0 else windowSize.width - popupContentSize.width
        val fitsBelow = anchorBounds.bottom + popupContentSize.height <= windowSize.height
        val y = if (fitsBelow) {
            anchorBounds.bottom
        } else {
            (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/**
 * A standalone, hamburger-style drop-down menu, declared with the same opinionated DSL as the rail.
 *
 * The trigger is the **app icon** (auto-drawn, exactly like the rail's header — not customizable),
 * dropped inline like any widget. Tapping it unfolds a [Popup] panel of the items you declare in
 * [content]; the panel is presented as a slice of the rail ([AzDropdownDesign.RAIL]) or menu
 * ([AzDropdownDesign.MENU]), width-constrained to match, pinned to the [AzDockingSide] screen edge,
 * and dropping from the trigger. Tapping outside or pressing back folds it up.
 *
 * Configure and populate it through [AzDropdownMenuScope], like the rail:
 * ```
 * AzDropdownMenu(navController = navController) {
 *     azConfig(design = AzDropdownDesign.MENU, dockingSide = AzDockingSide.LEFT)
 *     azItem("Home", route = "home") { }
 *     azToggle(isChecked = dark, toggleOnText = "Dark", toggleOffText = "Light") { dark = it }
 *     azDivider()
 *     azItem("Sign out") { signOut() }
 * }
 * ```
 *
 * Items may declare a `route`; when set, tapping navigates the [navController] (so the drop-down can
 * drive an `AzNavHost`, just like the rail), then runs the item's callback.
 *
 * @param modifier Modifier applied to the trigger's box — place/position it like any widget.
 * @param navController Controller used to navigate item `route`s. Defaults to the enclosing
 *   `AzNavHost`'s controller when present.
 * @param expanded Optional controlled open-state. When null the menu manages its own state.
 * @param onExpandedChange Called whenever the open-state changes.
 * @param content The menu's configuration and items, declared via [AzDropdownMenuScope].
 */
@Composable
fun AzDropdownMenu(
    modifier: Modifier = Modifier,
    navController: NavController? = LocalAzNavHostScope.current?.navController,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    content: AzDropdownMenuScope.() -> Unit
) {
    // Collect the DSL fresh each recomposition (no remembered mutable scope to reset).
    val scope = AzDropdownMenuScopeImpl().apply(content)
    val config = scope.config
    val entries = scope.entries

    var internalOpen by rememberSaveable { mutableStateOf(false) }
    val isOpen = expanded ?: internalOpen
    val setOpen: (Boolean) -> Unit = { value ->
        if (expanded == null) internalOpen = value
        onExpandedChange?.invoke(value)
    }

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    val panelWidth = if (config.design == AzDropdownDesign.RAIL) config.collapsedWidth else config.expandedWidth

    val positionProvider = remember(config.dockingSide) {
        AzDropdownEdgePositionProvider(config.dockingSide)
    }

    // The trigger is the app's launcher icon, loaded exactly like the rail's header icon.
    val appIcon = remember(context.packageName) {
        try { context.packageManager.getApplicationIcon(context.packageName) } catch (e: Exception) { null }
    }

    Box(modifier = modifier) {
        // The inline app-icon trigger — fixed shape/size, no styling knobs. The icon itself is
        // decorative; the box carries the "Menu" accessibility label.
        Box(
            modifier = Modifier
                .size(AzDropdownTriggerSize)
                .clip(CircleShape)
                .clickable {
                    setOpen(!isOpen)
                    if (config.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .semantics(mergeDescendants = true) { contentDescription = "Menu" },
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                Image(
                    painter = rememberAsyncImagePainter(appIcon),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(imageVector = Icons.Default.Menu, contentDescription = null)
            }
        }

        if (isOpen) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { setOpen(false) },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp
                ) {
                    val scrollState = rememberScrollState()
                    val dismiss = { setOpen(false) }
                    Column(
                        modifier = Modifier
                            .width(panelWidth)
                            .heightIn(max = maxPanelHeight)
                            .verticalScroll(scrollState)
                            .then(if (config.design == AzDropdownDesign.RAIL) Modifier.padding(8.dp) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        entries.forEach { entry ->
                            AzDropdownEntryItem(entry, config.design, navController, dismiss)
                        }
                    }
                }
            }
        }
    }
}
