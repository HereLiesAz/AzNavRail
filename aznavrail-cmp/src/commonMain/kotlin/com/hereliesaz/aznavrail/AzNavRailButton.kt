// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRailButton.kt
package com.hereliesaz.aznavrail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.toComposeShape
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzComposableContent
import com.hereliesaz.aznavrail.util.text.AutoSizeText

/**
 * The internal button primitive for every item rendered in the navigation rail or nested rail.
 *
 * Handles shape enforcement, color transitions on press/selection, custom content rendering
 * (Color, Image, resource ID, or [AzComposableContent]), and the loading state. All rail buttons
 * share the strict [AzNavRailDefaults.ButtonWidth] — only height varies by shape.
 *
 * @param onClick Click handler, or null to make the button non-interactive.
 * @param text Text displayed when [itemContent] is null.
 * @param modifier Applied to the button container.
 * @param size Button size in dp; typically [AzNavRailDefaults.ButtonWidth].
 * @param color Base border/icon color (unselected state).
 * @param activeColor Color used when the button is pressed or selected.
 * @param textColor Overrides computed text/icon color.
 * @param fillColor Overrides the *hue* of the translucent fill color for the button background; its
 *   alpha is still overridden by the rail's own computed 0.12f/0.25f selected/base split.
 * @param backgroundColor Verbatim (alpha-included) fill color override — when non-null, used AS-IS
 *   for the button's fill, bypassing the [fillColor]-hue-plus-computed-alpha logic entirely
 *   regardless of selected/focused/pressed state. Backs [com.hereliesaz.aznavrail.model.AzNavItem.translucentBackgroundColor].
 * @param colors Unused Material [ButtonColors] slot (reserved for future compatibility).
 * @param shape Determines the geometric shape of the button.
 * @param enabled Whether the button can be interacted with.
 * @param isSelected Whether the button is in the selected/active state.
 * @param isLoading When true, the button content is hidden and [AzLoad] is shown in its place.
 * @param contentPadding Padding applied around text content (ignored when [itemContent] is set).
 * @param itemContent Arbitrary content to render inside the button instead of [text]. Accepts
 *   [Color], [AzComposableContent], [Int] (resource ID), [String], an
 *   [androidx.compose.ui.graphics.vector.ImageVector] or
 *   [androidx.compose.ui.graphics.painter.Painter] (vector graphics), or any image model
 *   loadable by Coil (Bitmap, URL, File, Uri, …). All non-text graphics fill the button
 *   shape (Crop) and are clipped to it.
 * @param onLongClick Long-press handler.
 * @param onGloballyPositioned Reports the window-space bounds of the button after layout.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AzNavRailButton(
    onClick: (() -> Unit)?,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = AzNavRailDefaults.ButtonWidth,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color? = null,
    fillColor: Color? = null,
    backgroundColor: Color? = null,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    /**
     * Whether this button wears the **focus** highlight: the last-tapped item that carries no route
     * of its own. A press always counts as focus too, whatever this says.
     */
    isFocused: Boolean = false,
    /** Whether this button wears the **secondary** highlight — see [com.hereliesaz.aznavrail.model.AzHighlight]. */
    isSecondaryActive: Boolean = false,
    /** Colour of the focus highlight. Null reuses [activeColor]. */
    focusColor: Color? = null,
    /** Colour of the secondary highlight. Null reuses [activeColor]. */
    secondaryColor: Color? = null,
    /** Whether this button wears the **tertiary** highlight — see [com.hereliesaz.aznavrail.model.AzHighlight]. */
    isTertiaryActive: Boolean = false,
    /** Colour of the tertiary highlight. Null reuses [activeColor]. */
    tertiaryColor: Color? = null,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    itemContent: Any? = null,
    onLongClick: (() -> Unit)? = null,
    onGloballyPositioned: ((Rect) -> Unit)? = null,
    rotationDegrees: Float = 0f,
    /**
     * Overrides the Compose [Shape] derived from [shape]. Used to hand the button a shape that is
     * mid-morph (see [com.hereliesaz.aznavrail.internal.AzAlertMorphShape]); [shape] still decides
     * the footprint and the border.
     */
    shapeOverride: Shape? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonShape: Shape = shapeOverride ?: shape.toComposeShape()

    val isRotated = rotationDegrees != 0f && (rotationDegrees % 180 != 0f)

    // STRICT WIDTH COMPLIANCE for all shapes. Swapped in landscape to maintain "physical" size.
    // A borderless shape keeps the footprint of the base shape it was built on, so dropping the
    // border never reflows the rail around the item.
    val buttonModifier = when (shape.baseShape) {
        // The triangle occupies the same square footprint as a circle, so an item that flips to the
        // warning glyph doesn't reflow the rail around it.
        AzButtonShape.CIRCLE, AzButtonShape.SQUARE, AzButtonShape.TRIANGLE -> modifier
            .size(size)
            .aspectRatio(1f)
        else -> {
            if (isRotated) {
                modifier.width(40.dp).height(size)
            } else {
                modifier.width(size).height(40.dp)
            }
        }
    }

    // Apply rotation back to keep text upright "in place"
    val finalModifier = buttonModifier.graphicsLayer {
        rotationZ = -rotationDegrees
    }

    val disabledColor = color.copy(alpha = 0.38f)
    // The three highlights, in the order they outrank each other. Focus wins over active because it
    // is about the gesture happening right now; active wins over secondary because where you are
    // beats what the app happens to be doing; and the unhighlighted colour is the item's own.
    val targetColor = when {
        isPressed || isFocused -> focusColor ?: activeColor
        isSelected -> activeColor
        isSecondaryActive -> secondaryColor ?: activeColor
        isTertiaryActive -> tertiaryColor ?: activeColor
        else -> color
    }
    val finalColor = if (enabled) targetColor else disabledColor

    val computedFillColor = if (finalColor == Color.Black) Color.White else Color.Black
    val computedActiveFillColor = if (targetColor == Color.Black) Color.White else Color.Black

    val baseFillColor = fillColor ?: computedFillColor
    val activeFillColor = fillColor ?: computedActiveFillColor

    // `backgroundColor` (verbatim, alpha included) bypasses the computed 0.12f/0.25f split
    // entirely, regardless of state — see `AzNavItem.translucentBackgroundColor`.
    val containerColor = if (backgroundColor != null) {
        backgroundColor
    } else if ((isSelected || isFocused || isSecondaryActive || isTertiaryActive) && !isPressed) {
        activeFillColor.copy(alpha = 0.12f)
    } else {
        baseFillColor.copy(alpha = 0.25f)
    }

    val finalTextColor = textColor ?: finalColor

    val clickableModifier = if (enabled && (onClick != null || onLongClick != null)) {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        Modifier
    }

    // The fill (Surface, clipped to buttonShape) and the outline (a separate, unclipped layer drawn
    // on top) live in an outer Box that carries the button's real, unclipped layout bounds — click
    // handling, semantics, and position reporting all attach here, exactly as they did on the old
    // single clipped Surface, so hit-testing and reported bounds are unchanged.
    //
    // The outline is drawn OUTSIDE the fill (CSS `outline` semantics, not `border`): Compose's
    // border()/BorderStroke always centers a stroke ON the shape's path, and clipping the same
    // element to that path (as the old code did) threw away the outward half, so the stroke used to
    // read as entirely inside the shape. Instead we compute the shape's outline for a size inflated
    // by half the stroke width on every side, then stroke THAT path (centered, so it spans from the
    // true edge out to strokeWidth beyond it) — the ring lands entirely outside the fill, touching it
    // but never overlapping it. Compose does not clip a child's drawing to its own layout bounds
    // unless something upstream clips it, and nothing here does, so the ring can bleed into the
    // rail's inter-item spacing without reserving extra layout space — the button's measured
    // footprint (and therefore rail spacing) is unchanged.
    Box(
        modifier = finalModifier
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned?.invoke(coordinates.boundsInWindow())
            }
            .semantics { contentDescription = text; this.selected = isSelected }
            .then(clickableModifier)
    ) {
        Surface(
            shape = buttonShape,
            color = containerColor,
            contentColor = finalColor,
            border = null,
            modifier = Modifier.matchParentSize().clip(buttonShape)
        ) {
            Box(
                // If itemContent is present (Color/Img), we force 0 padding to Fill/Crop. Otherwise, apply text padding.
                // A triangle's usable area is its lower half, so its content is nudged down off the apex.
                modifier = when {
                    itemContent != null -> Modifier
                    shape.baseShape == AzButtonShape.TRIANGLE -> Modifier.padding(contentPadding).padding(top = size * 0.25f)
                    else -> Modifier.padding(contentPadding)
                },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (itemContent != null) {
                        ItemContentRenderer(itemContent, finalTextColor, enabled)
                    } else {
                        AutoSizeText(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textAlign = TextAlign.Center,
                                color = finalTextColor
                            ),
                            modifier = Modifier.fillMaxSize(),
                            maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
                            softWrap = false,
                            alignment = Alignment.Center,
                            lineSpaceRatio = 0.9f
                        )
                    }
                }
                // Each button spins its own spinner, scaled to the button rather than the full-screen
                // default, and tinted to the item's own colour so a loading item still reads as itself.
                if (isLoading) {
                    val spinnerSize = when (shape.baseShape) {
                        AzButtonShape.RECTANGLE -> 28.dp
                        else -> size * 0.6f
                    }
                    AzLoad(size = spinnerSize, color = finalColor)
                }
            }
        }
        if (!shape.isBorderless) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // Qualified `this.size` (the DrawScope's own draw-area size, in px) because
                        // the enclosing `AzNavRailButton`'s `size: Dp` parameter would otherwise
                        // shadow it — an unqualified `size` here resolves to that outer Dp, not this
                        // draw scope's Size.
                        val strokeWidthPx = 3.dp.toPx()
                        val drawSize = this.size
                        val inflatedSize = Size(drawSize.width + strokeWidthPx, drawSize.height + strokeWidthPx)
                        val outline = buttonShape.createOutline(inflatedSize, layoutDirection, this)
                        translate(left = -strokeWidthPx / 2f, top = -strokeWidthPx / 2f) {
                            drawOutline(outline = outline, color = finalColor, style = Stroke(width = strokeWidthPx))
                        }
                    }
            )
        }
    }
}

/**
 * A small circular badge drawn on the top-end corner of a rail/menu button. Renders [text] (a few
 * characters — e.g. an unread count or short status) centered in a filled circle. Blank text renders
 * nothing. [containerColor] fills the circle; [contentColor] is the text.
 */
@Composable
internal fun AzBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    if (text.isBlank()) return
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ItemContentRenderer(itemContent: Any, color: Color, enabled: Boolean) {
    when (itemContent) {
        // Zero padding, completely fills shape
        is Color -> Box(modifier = Modifier.fillMaxSize().alpha(if (enabled) 1f else 0.5f).background(itemContent))
        is AzComposableContent -> itemContent.content(enabled)
        // Note: the Android sibling special-cases `Int` as a drawable resource id (via
        // `context.resources.getResourceName`) and renders it with the Android-only
        // `painterResource(Int)`. CMP has no equivalent Int-as-resource concept — callers who want
        // to render a drawable should pass a Painter directly (see the `is Painter` branch below).
        is Int -> TextContent(itemContent.toString(), color)
        is String -> TextContent(itemContent, color)
        // Vector graphics. Coil cannot render an ImageVector/Painter (it throws
        // "Unsupported type: ImageVector"), so these must be drawn with foundation Image
        // before the Coil fallback. Tinted with the button color so monochrome icons
        // (e.g. Icons.Default.*) adopt the rail's color, and cropped to fill the shape.
        is ImageVector -> Image(
            imageVector = itemContent,
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (enabled) 1f else 0.5f)
        )
        is Painter -> Image(
            painter = itemContent,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (enabled) 1f else 0.5f)
        )
        else -> {
            Image(
                painter = rememberAsyncImagePainter(model = itemContent),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TextContent(text: String, color: Color) {
    AutoSizeText(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center, color = color),
        maxLines = 1,
        softWrap = false,
        alignment = Alignment.Center,
        lineSpaceRatio = 0.9f
    )
}
