// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/Footer.kt
package com.hereliesaz.aznavrail.internal

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzEasing
import com.hereliesaz.aznavrail.util.text.AutoSizeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Sizing bounds for the auto-shrinking footer labels. */
internal object AzFooterDefaults {
    /** Nothing in the footer is ever drawn larger than this, whatever the theme says. */
    val MaxTextSize: TextUnit = 22.sp

    /** The floor. A label that still doesn't fit at this size is clipped, not wrapped. */
    val MinTextSize: TextUnit = 9.sp
}

/**
 * One footer row, drawn with the same auto-sizing the rail's own buttons use.
 *
 * The footer is a fixed-width column at the bottom of a menu, and its longest label — `@HereLiesAz`
 * — is the one that doesn't fit. Wrapping it to a second line makes the footer taller than the strip
 * it is pinned to and reads as a typo. So the label shrinks to fit its row instead, exactly like a
 * rail item's text does: one line, no wrap, binary-searched down from the theme's `titleLarge` until
 * it fits.
 */
@Composable
internal fun AzFooterLabel(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val base: TextStyle = MaterialTheme.typography.titleLarge
    val maxTextSize = base.fontSize.takeIf { it.isSpecified } ?: AzFooterDefaults.MaxTextSize
    // The row is as tall as one line of the largest permitted size; the text shrinks inside it.
    val rowHeight = with(LocalDensity.current) { maxTextSize.toDp() } * 1.4f
    val gestures = if (onLongClick != null) {
        Modifier.pointerInput(onClick, onLongClick) {
            detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
        }
    } else {
        Modifier.clickable { onClick() }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .then(gestures),
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(
            text = text,
            style = base.copy(color = color, fontWeight = fontWeight ?: base.fontWeight),
            modifier = Modifier.fillMaxSize(),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            alignment = Alignment.Center,
            maxTextSize = maxTextSize,
            minTextSize = AzFooterDefaults.MinTextSize,
            lineSpaceRatio = 1f,
        )
    }
}

/** The author's page — the destination behind every `@HereLiesAz` row the library draws. */
internal const val AZ_INSTAGRAM_URL = "https://instagram.com/HereLiesAz"

/** The author's site — where the About page sends anyone who wants the rest of the work. */
internal const val AZ_SITE_URL = "https://hereliesaz.com"

/** Where the About page's tip button sends anyone who wants to leave one. */
internal const val AZ_DONATE_URL = "https://paypal.me/HereLiesAz"

/**
 * Renders the pinned footer strip shown at the bottom of the expanded menu.
 *
 * Contains About, Feedback, and the @HereLiesAz attribution link. The Undock action is shown
 * only when rail-dragging or a custom [com.hereliesaz.aznavrail.model.AzAdvancedConfig.onUndock]
 * is configured. Long-pressing @HereLiesAz invokes [onSecretClick] to open the developer debug menu.
 *
 * @param appName The app name embedded in the feedback email subject.
 * @param onToggle Callback to collapse the rail (bound to the Close action, currently unused in footer).
 * @param onUndock Callback to detach the rail into FAB mode.
 * @param onSecretClick Callback that opens the Secret Screens dialog; null when [com.hereliesaz.aznavrail.model.AzAdvancedConfig.secLoc] is unset.
 * @param scope The active rail scope used to read dragging/undock flags.
 * @param repoUrl The effective host-app repository URL (explicit override or namespace-derived),
 *   opened in a browser by "About" when the in-app reader is disabled.
 * @param footerColor Tint color applied to all footer text items.
 * @param onAboutClick When non-null, the "About" item invokes this (to open the in-app About reader)
 *   instead of opening the repository URL in a browser. Null restores the legacy browser behavior.
 * @param showAbout Whether this footer is the surface that owns the About affordance. False when
 *   another surface (a developer-declared `?` rail item, another menu) already offers it — see
 *   [AzAboutRegistry].
 */
@Composable
internal fun Footer(
    appName: String,
    onToggle: () -> Unit,
    onUndock: () -> Unit,
    onSecretClick: (() -> Unit)?,
    scope: AzNavRailScopeImpl,
    repoUrl: String,
    footerColor: Color,
    onAboutClick: (() -> Unit)? = null,
    showAbout: Boolean = true,
    // Accordion-unfold controls. `visible` drives the anim; the delay is `(menuItemCount-1)*staggerMs`
    // so the footer begins the moment the LAST menu item begins its own kinetic entrance.
    visible: Boolean = true,
    menuItemCount: Int = 0,
    staggerMs: Int = AzMotion.ItemStaggerMs,
    durationMs: Int = AzMotion.ItemDurationMs,
    easing: Easing = AzEasing.Wp7Decelerate,
) {
    val context = LocalContext.current

    // Accordion state — ALWAYS start collapsed (0f) so the very first composition also plays the
    // fold-in animation. Previously this initialized to `visible ? 1f : 0f`, which meant the
    // footer was already fully open on first mount when the drawer opened — no animation played.
    val scaleY = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        val spec = tween<Float>(durationMillis = durationMs, easing = easing)
        if (visible) {
            // Footer unfolds one stagger tick AFTER the last menu item begins its own kinetic
            // entrance — the natural next beat in the cascade rhythm.
            delay(menuItemCount.coerceAtLeast(0).toLong() * staggerMs)
            launch { scaleY.animateTo(1f, spec) }
            launch { fade.animateTo(1f, spec) }
        } else {
            // On close the footer is the FIRST thing to go — folds up immediately, without any
            // delay, while the menu items are still preparing their bottom-up exit cascade.
            launch { scaleY.animateTo(0f, spec) }
            launch { fade.animateTo(0f, spec) }
        }
    }

    // Footer items strictly on their own rows (Column) and center aligned
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
        if (scope.advancedConfig.enableRailDragging || scope.advancedConfig.onUndock != null) {
            AzFooterLabel(
                text = "Undock",
                color = footerColor,
                fontWeight = FontWeight.Bold,
                onClick = onUndock,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showAbout) {
            AzFooterLabel(
                text = "About",
                color = footerColor,
                onClick = {
                    if (onAboutClick != null) {
                        onAboutClick()
                    } else {
                        try {
                            if (repoUrl.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)))
                            }
                        } catch (e: Exception) {
                        }
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        AzFooterLabel(
            text = "Feedback",
            color = footerColor,
            onClick = {
                try {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:hereliesaz@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, appName)
                    }
                    context.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
                } catch (e: Exception) {
                }
            },
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AzFooterLabel(
            text = "@HereLiesAz",
            // Same accent as the rest of the footer — the muted 0.5 alpha used to make this row
            // read as a second-class link visually inconsistent with About / Feedback.
            color = footerColor,
            onClick = {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AZ_INSTAGRAM_URL)))
                } catch (e: Exception) {
                }
            },
            onLongClick = { onSecretClick?.invoke() },
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}
