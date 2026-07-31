package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.azAccent
import com.hereliesaz.aznavrail.model.AzDropdownTrigger
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import kotlin.math.roundToInt

/**
 * The stable, comparable description of a drop-down's trigger.
 *
 * Every field is an immutable value, so a slot's spec can be re-published on each recomposition and
 * compared away when nothing changed — which is what keeps the title-hosted trigger from looping the
 * host's composition. Behaviour (the tap handler, the bounds reporter) deliberately lives on
 * [AzTitleTriggerSlot] as plain fields instead, because those are only read from event callbacks.
 */
internal data class AzDropdownTriggerSpec(
    val trigger: AzDropdownTrigger = AzDropdownTrigger.MoreVert,
    val size: Dp = 40.dp,
    val iconShape: AzHeaderIconShape = AzHeaderIconShape.CIRCLE,
    /** Resolved launcher icon model, used only by [AzDropdownTrigger.AppIcon]. */
    val appIcon: Any? = null,
    /** Accent applied to the glyph / label. [Color.Unspecified] falls back to the theme primary. */
    val color: Color = Color.Unspecified,
    /** Optional style merged over an [AzDropdownTrigger.Text] trigger's default. */
    val textStyle: TextStyle? = null,
    val contentDescription: String = "Menu",
)

/**
 * One drop-down's claim on a slot in the screen-title row.
 *
 * The drop-down composable owns its own open-state and renders its own panel where it is declared;
 * only the trigger is relocated. It publishes [spec] (snapshot state, so the title row redraws when
 * the trigger's look changes) and keeps [onTap] / [onBounds] refreshed as plain fields, so the title
 * row's button always calls back into the drop-down's current composition.
 */
internal class AzTitleTriggerSlot {
    var spec: AzDropdownTriggerSpec by mutableStateOf(AzDropdownTriggerSpec())

    /** Opens/closes the owning drop-down. Read only from the click handler, never during composition. */
    var onTap: () -> Unit = {}

    /** Reports the trigger's window-space bounds so the owning drop-down can anchor its panel there. */
    var onBounds: (IntRect) -> Unit = {}

    /** Publishes [next] only when it actually differs, so equal re-publishes cost no recomposition. */
    fun publish(next: AzDropdownTriggerSpec) {
        if (spec != next) spec = next
    }
}

/**
 * Draws one trigger and wires its tap + bounds reporting.
 *
 * Used for both placements: the screen-title row renders one of these per registered slot, and an
 * `INLINE` drop-down renders one at its own call site. Either way the drop-down learns where its
 * trigger ended up, so the dropped panel is positioned against the real thing.
 */
@Composable
internal fun AzDropdownTriggerButton(
    slot: AzTitleTriggerSlot,
    modifier: Modifier = Modifier,
) {
    val spec = slot.spec
    val accent = spec.color.takeOrElse { azAccent() }
    val clipShape: Shape? = when (spec.iconShape) {
        AzHeaderIconShape.CIRCLE -> CircleShape
        AzHeaderIconShape.ROUNDED -> RoundedCornerShape(12.dp)
        else -> null
    }
    val clipModifier = if (clipShape != null) Modifier.clip(clipShape) else Modifier

    val boundsModifier = Modifier.onGloballyPositioned { coordinates ->
        val pos = coordinates.positionInWindow()
        val size = coordinates.size
        slot.onBounds(
            IntRect(
                left = pos.x.roundToInt(),
                top = pos.y.roundToInt(),
                right = pos.x.roundToInt() + size.width,
                bottom = pos.y.roundToInt() + size.height,
            )
        )
    }

    // A text trigger sizes to its own label; every icon trigger is a square of `spec.size`.
    val sizeModifier =
        if (spec.trigger is AzDropdownTrigger.Text) Modifier else Modifier.size(spec.size)

    Box(
        modifier = modifier
            // Automatic breathing room so neighbouring triggers (and the title) never sit flush.
            .padding(AzNavRailDefaults.HeaderPadding)
            .then(sizeModifier)
            .then(clipModifier)
            .then(boundsModifier)
            .clickable { slot.onTap() }
            .semantics(mergeDescendants = true) { contentDescription = spec.contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        when (val trigger = spec.trigger) {
            is AzDropdownTrigger.Text -> Text(
                text = trigger.text,
                style = MaterialTheme.typography.titleLarge.merge(spec.textStyle),
                color = accent,
                maxLines = 1,
                softWrap = false,
            )

            is AzDropdownTrigger.Icon -> when (val model = trigger.model) {
                is ImageVector -> Icon(
                    imageVector = model,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.fillMaxSize(),
                )

                is Painter -> Image(
                    painter = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().then(clipModifier),
                )

                else -> Image(
                    painter = rememberAsyncImagePainter(model),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().then(clipModifier),
                )
            }

            AzDropdownTrigger.AppIcon -> {
                val appIcon = spec.appIcon
                if (appIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(appIcon),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().then(clipModifier),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            AzDropdownTrigger.Hamburger -> Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.fillMaxSize(),
            )

            AzDropdownTrigger.MoreVert -> Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
