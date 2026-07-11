package com.hereliesaz.aznavrail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AboutOverlay
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.rememberEffectiveAppMeta
import com.hereliesaz.aznavrail.internal.AzSafeZones
import com.hereliesaz.aznavrail.internal.MoreFromAzOverlay
import com.hereliesaz.aznavrail.service.GithubDocsRepository
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzDropdownDesign
import com.hereliesaz.aznavrail.model.AzEasing
import com.hereliesaz.aznavrail.model.AzEntrance
import com.hereliesaz.aznavrail.model.AzExit
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.internal.rememberAzKineticModifier
import com.hereliesaz.aznavrail.internal.rememberAzClosingState
import com.hereliesaz.aznavrail.internal.AzIcons

/**
 * DSL builder scope for an [AzDropdownMenu] — declared the same way as the rail
 * (`AzDropdownMenu { azConfig(...); azItem(...) { } }`).
 *
 * In keeping with AzNavRail's opinionated tradition, the drop-down only accepts configuration the
 * rest of the library sanctions: [azConfig] mirrors the rail's `azConfig`/`azTheme` (design, docking
 * side, vibration, the collapsed/expanded widths, and the app-icon shape/size) and the item builders
 * accept only the per-item knobs the rail's items accept (`color`/`textColor`/`fillColor`/`shape`/
 * `enabled`, plus a navigation `route`). There is no arbitrary panel background, offset, or free
 * composable escape hatch.
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
     * @param headerIconShape Clip shape for the app-icon trigger (mirrors the rail's
     *   `azTheme(headerIconShape = …)`).
     * @param headerIconSize Diameter of the app-icon trigger (mirrors the rail's
     *   `azTheme(headerIconSize = …)`).
     * @param showFooter Whether the [AzDropdownDesign.MENU] panel shows the rail's footer
     *   (About / Feedback / @HereLiesAz), like the rail's expanded menu.
     * @param inAppAbout When true (the default), the footer's "About" opens a full-screen, in-app
     *   markdown reader auto-generated from the host app's repo (the dropdown has no onscreen area,
     *   so it draws its own full-screen layer). When false, "About" opens the repo in a browser.
     * @param appRepositoryUrl Optional explicit override for the host app's GitHub repository used by
     *   the "About" screen. Blank (the default) auto-derives it from the app namespace
     *   (`com.<owner>.<repo>` → `github.com/<owner>/<repo>`); never the AzNavRail library repo.
     * @param itemTextStyle Optional override merged over each item's label style (lets the menu words
     *   be big/light/wide Metro type). Applied to the [AzDropdownDesign.MENU] rows; [AzDropdownDesign.RAIL]
     *   items keep their auto-sized text.
     * @param itemEntrance Windows-Phone-7-style entrance played as the panel opens (see [AzEntrance]);
     *   defaults to [AzEntrance.Turnstile]. Pass [AzEntrance.None] for a static panel.
     * @param entranceStaggerMs Per-item cascade delay, multiplied by the item's position.
     * @param entranceDurationMs Duration of each item's entrance/exit animation.
     * @param entranceEasing Easing for the entrance/exit (defaults to [AzEasing.Wp7Decelerate]).
     * @param entranceStartAngle Starting `rotationY` for the [AzEntrance.Turnstile] sweep, in degrees.
     * @param tiltOnPress When true, items tilt in 3D toward the press point and spring back on release
     *   (the WP7 "tilt effect"); the item's `onClick` still fires.
     * @param maxTiltDegrees Maximum tilt angle for [tiltOnPress].
     * @param itemExit Exit played as the panel dismisses (see [AzExit]); defaults to [AzExit.Turnstile].
     *   Items are held mounted through the close so they can animate out.
     */
    fun azConfig(
        design: AzDropdownDesign = AzDropdownDesign.MENU,
        dockingSide: AzDockingSide = AzDockingSide.LEFT,
        vibrate: Boolean = false,
        expandedWidth: Dp = 160.dp,
        collapsedWidth: Dp = 100.dp,
        headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
        headerIconSize: Dp = 48.dp,
        showFooter: Boolean = true,
        inAppAbout: Boolean = true,
        appRepositoryUrl: String = "",
        itemTextStyle: TextStyle? = null,
        itemEntrance: AzEntrance = AzEntrance.Turnstile,
        entranceStaggerMs: Int = 60,
        entranceDurationMs: Int = 720,
        entranceEasing: Easing = AzEasing.Wp7Decelerate,
        entranceStartAngle: Float = 90f,
        tiltOnPress: Boolean = false,
        maxTiltDegrees: Float = 10f,
        itemExit: AzExit = AzExit.Turnstile,
        dimBehindMenu: Boolean = false,
        dimBehindMenuAlpha: Float = 0.4f,
        menuItemAlignment: com.hereliesaz.aznavrail.model.AzMenuItemAlignment =
            com.hereliesaz.aznavrail.model.AzMenuItemAlignment.SIDE,
        justifyMenuItems: Boolean = true,
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
    val collapsedWidth: Dp = 100.dp,
    val headerIconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
    val headerIconSize: Dp = 48.dp,
    val showFooter: Boolean = true,
    val inAppAbout: Boolean = true,
    val appRepositoryUrl: String = "",
    val itemTextStyle: TextStyle? = null,
    val itemEntrance: AzEntrance = AzEntrance.Turnstile,
    val entranceStaggerMs: Int = 60,
    val entranceDurationMs: Int = 720,
    val entranceEasing: Easing = AzEasing.Wp7Decelerate,
    val entranceStartAngle: Float = 90f,
    val tiltOnPress: Boolean = false,
    val maxTiltDegrees: Float = 10f,
    val itemExit: AzExit = AzExit.Turnstile,
    val dimBehindMenu: Boolean = false,
    val dimBehindMenuAlpha: Float = 0.4f,
    val menuItemAlignment: com.hereliesaz.aznavrail.model.AzMenuItemAlignment =
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.SIDE,
    val justifyMenuItems: Boolean = true,
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
        collapsedWidth: Dp,
        headerIconShape: AzHeaderIconShape,
        headerIconSize: Dp,
        showFooter: Boolean,
        inAppAbout: Boolean,
        appRepositoryUrl: String,
        itemTextStyle: TextStyle?,
        itemEntrance: AzEntrance,
        entranceStaggerMs: Int,
        entranceDurationMs: Int,
        entranceEasing: Easing,
        entranceStartAngle: Float,
        tiltOnPress: Boolean,
        maxTiltDegrees: Float,
        itemExit: AzExit,
        dimBehindMenu: Boolean,
        dimBehindMenuAlpha: Float,
        menuItemAlignment: com.hereliesaz.aznavrail.model.AzMenuItemAlignment,
        justifyMenuItems: Boolean,
    ) {
        config = AzDropdownConfig(
            design, dockingSide, vibrate, expandedWidth, collapsedWidth, headerIconShape, headerIconSize,
            showFooter, inAppAbout, appRepositoryUrl, itemTextStyle, itemEntrance, entranceStaggerMs,
            entranceDurationMs, entranceEasing, entranceStartAngle, tiltOnPress, maxTiltDegrees, itemExit,
            dimBehindMenu, dimBehindMenuAlpha, menuItemAlignment, justifyMenuItems,
        )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle? = null,
    dockingSide: AzDockingSide = AzDockingSide.LEFT,
    menuItemAlignment: com.hereliesaz.aznavrail.model.AzMenuItemAlignment =
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.SIDE,
    justifyMenuItems: Boolean = true,
) {
    val textAlign = when (menuItemAlignment) {
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.CENTER -> TextAlign.Center
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.SIDE ->
            if (dockingSide == AzDockingSide.RIGHT) TextAlign.End else TextAlign.Start
    }
    val columnAlignment = when (menuItemAlignment) {
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.CENTER -> Alignment.CenterHorizontally
        com.hereliesaz.aznavrail.model.AzMenuItemAlignment.SIDE ->
            if (dockingSide == AzDockingSide.RIGHT) Alignment.End else Alignment.Start
    }
    val mergedStyle = MaterialTheme.typography.titleLarge.merge(textStyle)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current

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
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.weight(1f)) {
            val availableWidthPx = with(density) { maxWidth.toPx() }
            val baseFontSizePx = with(density) {
                val fs = mergedStyle.fontSize
                if (fs.isSpecified) fs.toPx()
                else MaterialTheme.typography.titleLarge.fontSize.toPx()
            }
            Column(horizontalAlignment = columnAlignment) {
                text.split('\n').forEach { line ->
                    val (scale, kerningPx) = if (!justifyMenuItems || line.length < 2) 1f to 0f
                    else {
                        val naturalWidthPx = try {
                            textMeasurer.measure(text = line, style = mergedStyle).size.width.toFloat()
                        } catch (_: Throwable) { availableWidthPx }
                        com.hereliesaz.aznavrail.internal.solveHybridJustify(
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
                        color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                        textAlign = textAlign,
                        letterSpacing = with(density) { kerningPx.toSp() },
                        modifier = Modifier.fillMaxWidth(),
                        // Explicit-only line breaks; the solver's shrink branch handles oversized
                        // labels by scaling the font down instead of wrapping mid-word.
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

/**
 * The drop-down's footer for the [AzDropdownDesign.MENU] design — mirrors the rail's footer
 * ([com.hereliesaz.aznavrail.internal.Footer]): About, Feedback (email), and the @HereLiesAz
 * attribution. Centred [titleLarge] text in the theme primary, like the rail.
 *
 * @param repoUrl The effective host-app repository (explicit override or namespace-derived) opened
 *   by "About" when the in-app reader is disabled.
 * @param onAboutClick When non-null, "About" opens the in-app reader via this callback instead of a
 *   browser.
 */
@Composable
private fun AzDropdownFooter(
    repoUrl: String,
    onAboutClick: (() -> Unit)?,
    visible: Boolean = true,
    menuItemCount: Int = 0,
    staggerMs: Int = 60,
    durationMs: Int = 720,
    easing: Easing = AzEasing.Wp7Decelerate,
) {
    val uriHandler = LocalUriHandler.current
    val appMeta = rememberEffectiveAppMeta()
    val footerColor = MaterialTheme.colorScheme.primary
    val appName = appMeta.name

    // Always start collapsed so the first composition plays the fold-in animation. Previous
    // `if (visible) 1f else 0f` meant the footer was already visible on first mount and no
    // animation ever played.
    val scaleY = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        val spec = tween<Float>(durationMillis = durationMs, easing = easing)
        if (visible) {
            // One stagger tick beyond the last item's start — the footer is the next beat.
            delay(menuItemCount.coerceAtLeast(0).toLong() * staggerMs)
            launch { scaleY.animateTo(1f, spec) }
            launch { fade.animateTo(1f, spec) }
        } else {
            // On close the footer is the FIRST to go — fold up immediately so the items can begin
            // their bottom-up exit cascade in its wake.
            launch { scaleY.animateTo(0f, spec) }
            launch { fade.animateTo(0f, spec) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.scaleY = scaleY.value
                this.alpha = fade.value
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    if (onAboutClick != null) {
                        onAboutClick()
                    } else {
                        // Only follow plain web URLs, never an injected scheme (e.g. javascript:/content:).
                        val isHttp = repoUrl.startsWith("http://", ignoreCase = true) ||
                            repoUrl.startsWith("https://", ignoreCase = true)
                        if (isHttp) {
                            runCatching { uriHandler.openUri(repoUrl) }
                        }
                    }
                }
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Feedback",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    runCatching {
                        // Minimal URL-encoding of the subject so app names with spaces / query
                        // separators don't produce a malformed mailto: URI on strict handlers.
                        val subject = appName
                            .replace("%", "%25").replace(" ", "%20")
                            .replace("&", "%26").replace("#", "%23").replace("?", "%3F")
                        uriHandler.openUri("mailto:hereliesaz@gmail.com?subject=$subject")
                    }
                }
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "@HereLiesAz",
            // Same accent as the other footer rows.
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    runCatching {
                        uriHandler.openUri("https://instagram.com/HereLiesAz")
                    }
                }
                .padding(vertical = 4.dp)
        )
    }
}

/** Renders one collected [AzDropdownEntry] in the panel, wiring navigation + dismissal. */
@Composable
private fun AzDropdownEntryItem(
    index: Int,
    count: Int,
    visible: Boolean,
    entry: AzDropdownEntry,
    config: AzDropdownConfig,
    navController: NavController?,
    dismiss: () -> Unit,
    // Captures each entry's true window-space bounds so the dissolve overlay can render at the
    // right screen position after the panel Popup tears down. Called from `.onGloballyPositioned`.
    onBoundsCapture: (Int, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
    // Fires just before `dismiss()` when the tapped entry closes the panel — the outer composable
    // uses it to spawn a `DissolveOverlay` with the entry's captured bounds.
    onDissolveTap: (Int, String) -> Unit = { _, _ -> },
) {
    val design = config.design
    fun navigate(route: String?) {
        route?.let { navController?.navigate(it) }
    }

    // Dividers carry no kinetics; everything else gets the staggered entrance/exit + optional tilt.
    if (entry is AzDropdownEntry.Divider) {
        AzDivider()
        return
    }

    val kinetic = rememberAzKineticModifier(
        index = index,
        count = count,
        visible = visible,
        entrance = config.itemEntrance,
        exit = config.itemExit,
        staggerMs = config.entranceStaggerMs,
        durationMs = config.entranceDurationMs,
        easing = config.entranceEasing,
        startAngle = config.entranceStartAngle,
        tiltOnPress = config.tiltOnPress,
        maxTiltDegrees = config.maxTiltDegrees,
        dockingSide = config.dockingSide
    )

    // Capture the entry's true window-space bounds so the dissolve overlay can render at the
    // right screen position after the panel Popup tears down.
    val boundsModifier = Modifier.onGloballyPositioned { coordinates ->
        val pos = coordinates.positionInWindow()
        val size = coordinates.size
        onBoundsCapture(
            index,
            androidx.compose.ui.geometry.Rect(
                left = pos.x,
                top = pos.y,
                right = pos.x + size.width,
                bottom = pos.y + size.height,
            ),
        )
    }

    Box(modifier = boundsModifier) {
    when (entry) {
        is AzDropdownEntry.Item -> {
            val action = {
                navigate(entry.route)
                entry.onClick()
                if (entry.closeOnClick) {
                    onDissolveTap(index, entry.text)
                    dismiss()
                }
            }
            if (design == AzDropdownDesign.MENU) {
                AzDropdownMenuRow(
                    text = entry.text,
                    enabled = entry.enabled,
                    textColor = effectiveTextColor(entry.textColor, entry.color),
                    onClick = action,
                    modifier = kinetic,
                    textStyle = config.itemTextStyle,
                    dockingSide = config.dockingSide,
                    menuItemAlignment = config.menuItemAlignment,
                    justifyMenuItems = config.justifyMenuItems,
                )
            } else {
                Box(modifier = kinetic) {
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
                        if (entry.closeOnClick) {
                            onDissolveTap(index, entryLabel(entry))
                            dismiss()
                        }
                    },
                    modifier = kinetic,
                    textStyle = config.itemTextStyle,
                    dockingSide = config.dockingSide,
                    menuItemAlignment = config.menuItemAlignment,
                    justifyMenuItems = config.justifyMenuItems,
                )
            } else {
                Box(modifier = kinetic) {
                    AzToggle(
                        isChecked = entry.isChecked,
                        onToggle = {
                            navigate(entry.route)
                            entry.onToggle(it)
                            if (entry.closeOnClick) {
                                onDissolveTap(index, entryLabel(entry))
                                dismiss()
                            }
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
                        if (entry.closeOnClick) {
                            onDissolveTap(index, entryLabel(entry))
                            dismiss()
                        }
                    },
                    modifier = kinetic,
                    textStyle = config.itemTextStyle,
                    dockingSide = config.dockingSide,
                    menuItemAlignment = config.menuItemAlignment,
                    justifyMenuItems = config.justifyMenuItems,
                )
            } else {
                Box(modifier = kinetic) {
                    AzCycler(
                        options = entry.options,
                        selectedOption = entry.selectedOption,
                        onCycle = {
                            navigate(entry.route)
                            entry.onCycle(it)
                            if (entry.closeOnClick) {
                                onDissolveTap(index, entryLabel(entry))
                                dismiss()
                            }
                        },
                        color = entry.color.takeOrElse { MaterialTheme.colorScheme.primary },
                        textColor = entry.textColor,
                        fillColor = entry.fillColor,
                        shape = entry.shape,
                        enabled = entry.enabled
                    )
                }
            }
        }

        AzDropdownEntry.Divider -> Unit // handled above
    }
    } // close the bounds-capture Box
}

/** Best-effort label for the tapped entry — same text the row currently renders. */
private fun entryLabel(entry: AzDropdownEntry): String = when (entry) {
    is AzDropdownEntry.Item -> entry.text
    is AzDropdownEntry.Toggle -> if (entry.isChecked) entry.toggleOnText else entry.toggleOffText
    is AzDropdownEntry.Cycler -> entry.selectedOption
    AzDropdownEntry.Divider -> ""
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
 * The trigger is the **app icon** (auto-drawn exactly like the rail's header; its shape/size are set
 * via [AzDropdownMenuScope.azConfig], mirroring the rail's `azTheme`), dropped inline like any
 * widget. Tapping it unfolds a [Popup] panel of the items you declare in
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

    // Full-screen About / More-from-Az reader state. The dropdown has no host/onscreen area, so its
    // About page is drawn as its own full-screen layer (above everything) rather than inline.
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showMoreFromAz by rememberSaveable { mutableStateOf(false) }
    // Per-entry window-space bounds, captured on each entry's globallyPositioned so the dissolve
    // overlay can render the label at its true screen position after the panel Popup tears down.
    val entryBounds = remember { androidx.compose.runtime.mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    // Dissolve overlay for the tapped entry that closes the panel (see AzNavRail's counterpart).
    var dissolving: com.hereliesaz.aznavrail.internal.DissolveState? by remember { mutableStateOf(null) }
    // A throwaway rail scope only supplies default theme tokens (accent/surface) to the reused
    // overlays; the dropdown declares no rail theme of its own.
    val overlayScope = remember { AzNavRailScopeImpl() }

    val haptic = LocalHapticFeedback.current
    // Effective app metadata (consumer LocalAzAppMeta merged over platform-resolved values) so the
    // trigger shows the real launcher icon automatically, matching the Android library.
    val appMeta = rememberEffectiveAppMeta()
    // The repo backing the About reader: explicit override if set, else derived from the app
    // namespace. Never the AzNavRail library repo.
    val effectiveRepoUrl = remember(config.appRepositoryUrl, appMeta.packageId) {
        config.appRepositoryUrl.ifBlank {
            appMeta.packageId?.let { GithubDocsRepository.repoUrlFromPackage(it) } ?: config.appRepositoryUrl
        }
    }
    // Window height in px (LocalConfiguration is Android-only; use the multiplatform WindowInfo).
    val density = LocalDensity.current
    val maxPanelHeight = with(density) { (LocalWindowInfo.current.containerSize.height * 0.8f).toDp() }
    val panelWidth = if (config.design == AzDropdownDesign.RAIL) config.collapsedWidth else config.expandedWidth

    val positionProvider = remember(config.dockingSide) {
        AzDropdownEdgePositionProvider(config.dockingSide)
    }

    // The trigger is the app's launcher icon, provided via LocalAzAppMeta. Android's
    // AzHostActivityLayout populates this from `packageManager.getApplicationIcon`; other targets
    // pass a caller-supplied Coil3 model (URL string, ByteArray, or Painter).
    val appIcon = appMeta.icon

    Box(modifier = modifier) {
        // The inline app-icon trigger — the app icon, clipped to the configured shape/size. The icon
        // itself is decorative; the box carries the "Menu" accessibility label.
        val iconClipShape: Shape? = when (config.headerIconShape) {
            AzHeaderIconShape.CIRCLE -> CircleShape
            AzHeaderIconShape.ROUNDED -> RoundedCornerShape(12.dp)
            else -> null
        }
        val clipModifier = if (iconClipShape != null) Modifier.clip(iconClipShape) else Modifier
        Box(
            modifier = Modifier
                // Automatic breathing room around the app-icon trigger so it never sits flush against
                // neighbouring widgets (mirrors the rail header's spacing).
                .padding(AzNavRailDefaults.HeaderPadding)
                .size(config.headerIconSize)
                .then(clipModifier)
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
                    modifier = Modifier.fillMaxSize().then(clipModifier)
                )
            } else {
                Icon(imageVector = AzIcons.Menu, contentDescription = null)
            }
        }

        // Keep the panel composed through the staggered exit so items can animate out before teardown.
        val rendered = rememberAzClosingState(
            open = isOpen,
            exit = config.itemExit,
            count = entries.size,
            staggerMs = config.entranceStaggerMs,
            durationMs = config.entranceDurationMs
        )
        if (rendered) {
            // Full-screen dim scrim behind the popup, only when the developer opted in. Rendered as its
            // own Popup so it covers everything the menu might sit above and dismisses the menu on tap.
            if (config.dimBehindMenu) {
                Popup(
                    onDismissRequest = { setOpen(false) },
                    properties = PopupProperties(focusable = false, dismissOnClickOutside = true),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Color.Black.copy(
                                    alpha = config.dimBehindMenuAlpha.coerceIn(0f, 1f),
                                )
                            )
                            .clickable { setOpen(false) }
                    )
                }
            }
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
                        val entryDensity = androidx.compose.ui.platform.LocalDensity.current
                        entries.forEachIndexed { index, entry ->
                            // If this entry was the one tapped-to-close, render an invisible
                            // placeholder the same height as the captured entry so the panel
                            // stays occupied while the dissolve overlay animates its label. A bare
                            // `Spacer` doesn't draw or hit-test — it just holds the layout so the
                            // rows below don't snap upward during the dissolve.
                            val dissolvingHere = dissolving?.takeIf { it.itemId == "azdd:$index" }
                            if (dissolvingHere != null) {
                                Spacer(
                                    Modifier.height(
                                        with(entryDensity) { dissolvingHere.bounds.height.toDp() }
                                    )
                                )
                                return@forEachIndexed
                            }
                            AzDropdownEntryItem(
                                index = index,
                                count = entries.size,
                                visible = isOpen,
                                entry = entry,
                                config = config,
                                navController = navController,
                                dismiss = dismiss,
                                onBoundsCapture = { idx, rect -> entryBounds[idx] = rect },
                                onDissolveTap = { idx, text ->
                                    val bounds = entryBounds[idx]
                                    if (bounds != null) {
                                        dissolving = com.hereliesaz.aznavrail.internal.DissolveState(
                                            itemId = "azdd:$idx",
                                            text = text,
                                            bounds = bounds,
                                        )
                                    }
                                },
                            )
                        }
                        // The expanded-menu design carries the rail's footer (About / Feedback / @HereLiesAz).
                        if (config.design == AzDropdownDesign.MENU && config.showFooter) {
                            AzDivider(color = MaterialTheme.colorScheme.primary)
                            AzDropdownFooter(
                                repoUrl = effectiveRepoUrl,
                                onAboutClick = if (config.inAppAbout && effectiveRepoUrl.isNotBlank()) {
                                    { dismiss(); showAbout = true }
                                } else null,
                                visible = isOpen,
                                menuItemCount = entries.size,
                                staggerMs = config.entranceStaggerMs,
                                durationMs = config.entranceDurationMs,
                                easing = config.entranceEasing,
                            )
                        }
                    }
                }
            }
        }

        // Full-screen About reader / More-from-Az carousel for the dropdown. Drawn in its own Popup
        // so it escapes the inline trigger box and covers the whole window (the dropdown has no host
        // onscreen area). Safe-zone insets come from the system bars since there is no host to
        // compute the rail's 10%/20% zones.
        if (showAbout || showMoreFromAz) {
            val systemBars = WindowInsets.systemBars.asPaddingValues()
            Popup(
                popupPositionProvider = AzFullScreenPopupPositionProvider,
                onDismissRequest = { showAbout = false; showMoreFromAz = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = false)
            ) {
                CompositionLocalProvider(
                    LocalAzSafeZones provides AzSafeZones(
                        systemBars.calculateTopPadding(),
                        systemBars.calculateBottomPadding()
                    )
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (showAbout) {
                            AboutOverlay(
                                repoUrl = effectiveRepoUrl,
                                scope = overlayScope,
                                onOpenMoreFromAz = { showMoreFromAz = true },
                                onDismiss = { showAbout = false },
                            )
                        }
                        // More-from-Az draws above About; dismissing it returns to About underneath.
                        if (showMoreFromAz) {
                            MoreFromAzOverlay(
                                jsonUrl = overlayScope.advancedConfig.moreFromAzJsonUrl,
                                scope = overlayScope,
                                onDismiss = { showMoreFromAz = false },
                            )
                        }
                    }
                }
            }
        }

        // Dissolve overlay for a tapped dropdown entry — see AzNavRail's counterpart.
        dissolving?.let { snapshot ->
            val accent = MaterialTheme.colorScheme.primary
            val style = MaterialTheme.typography.titleLarge.let { base ->
                config.itemTextStyle?.let { base.merge(it) } ?: base
            }
            com.hereliesaz.aznavrail.internal.DissolveOverlay(
                state = snapshot,
                textStyle = style,
                color = accent,
                durationMs = config.entranceDurationMs,
                easing = config.entranceEasing,
                onFinished = { dissolving = null },
            )
        }
    }
}

/** Positions a [Popup] at the window origin so its `fillMaxSize` content covers the whole screen. */
private val AzFullScreenPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(0, 0)
}
