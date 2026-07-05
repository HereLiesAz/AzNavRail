// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/Footer.kt
package com.hereliesaz.aznavrail.internal

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // Accordion-unfold controls. `visible` drives the anim; the delay is `(menuItemCount-1)*staggerMs`
    // so the footer begins the moment the LAST menu item begins its own kinetic entrance.
    visible: Boolean = true,
    menuItemCount: Int = 0,
    staggerMs: Int = 60,
    durationMs: Int = 720,
    easing: Easing = AzEasing.Wp7Decelerate,
) {
    val context = LocalContext.current

    // Accordion-unfold: scaleY 0→1 from the top edge + alpha 0→1, keyed off `visible`.
    val scaleY = remember { Animatable(if (visible) 1f else 0f) }
    val fade = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) {
        val spec = tween<Float>(durationMillis = durationMs, easing = easing)
        if (visible) {
            // Footer starts AFTER the last menu item begins — one extra stagger tick so the footer
            // is the natural next beat in the cascade rhythm (as if it were the (count+1)th item).
            delay(menuItemCount.coerceAtLeast(0).toLong() * staggerMs)
            launch { scaleY.animateTo(1f, spec) }
            launch { fade.animateTo(1f, spec) }
        } else {
            launch { scaleY.snapTo(0f) }
            launch { fade.snapTo(0f) }
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
            Text(
                text = "Undock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = footerColor),
                modifier = Modifier
                    .clickable { onUndock() }
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    if (onAboutClick != null) {
                        onAboutClick()
                    } else {
                        try { if (repoUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))) } catch (e: Exception) {}
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
                    try {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hereliesaz@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, appName)
                        }
                        context.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
                    } catch (e: Exception) {}
                }
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "@HereLiesAz",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/HereLiesAz"))) } catch (e: Exception) {}
                        },
                        onLongPress = {
                            onSecretClick?.invoke()
                        }
                    )
                }
                .padding(vertical = 4.dp)
        )
    }
}