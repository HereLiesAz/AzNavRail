// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzNavRailButton.kt
package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzComposableContent
import com.hereliesaz.aznavrail.util.text.AutoSizeText

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AzNavRailButton(
    onClick: (() -> Unit)?,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = AzNavRailDefaults.ButtonWidth,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    itemContent: Any? = null,
    onLongClick: (() -> Unit)? = null,
    onGloballyPositioned: ((Rect) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonShape: Shape = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RectangleShape
        AzButtonShape.RECTANGLE -> RectangleShape
        AzButtonShape.NONE -> RectangleShape
    }

    // STRICT WIDTH COMPLIANCE for all shapes
    val buttonModifier = when (shape) {
        AzButtonShape.CIRCLE, AzButtonShape.SQUARE -> modifier
            .size(size)
            .aspectRatio(1f)
        AzButtonShape.RECTANGLE, AzButtonShape.NONE -> modifier
            .width(size) // Fixed identical width
            .height(48.dp) // Fixed height variant
    }

    val disabledColor = color.copy(alpha = 0.38f)
    val targetColor = if (isPressed || isSelected) activeColor else color
    val finalColor = if (enabled) targetColor else disabledColor

    val containerColor = if (isSelected && !isPressed) {
        activeColor.copy(alpha = 0.12f)
    } else {
        finalColor.copy(alpha = 0.25f)
    }

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

    Surface(
        shape = buttonShape,
        color = containerColor,
        contentColor = finalColor,
        border = if (shape == AzButtonShape.NONE) null else BorderStroke(3.dp, finalColor),
        modifier = buttonModifier
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned?.invoke(coordinates.boundsInWindow())
            }
            .then(clickableModifier)
    ) {
        Box(
            // If itemContent is present (Color/Img), we force 0 padding to Fill/Crop. Otherwise, apply text padding.
            modifier = if (itemContent != null) Modifier else Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.alpha(if (isLoading) 0f else 1f),
                contentAlignment = Alignment.Center
            ) {
                if (itemContent != null) {
                    ItemContentRenderer(itemContent, finalColor, enabled)
                } else {
                    AutoSizeText(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            color = finalColor
                        ),
                        modifier = Modifier.fillMaxSize(),
                        maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
                        softWrap = false,
                        alignment = Alignment.Center,
                        lineSpaceRatio = 0.9f
                    )
                }
            }
            if (isLoading) AzLoad()
        }
    }
}

@Composable
private fun ItemContentRenderer(itemContent: Any, color: Color, enabled: Boolean) {
    val context = LocalContext.current
    when (itemContent) {
        // Zero padding, completely fills shape
        is Color -> Box(modifier = Modifier.fillMaxSize().alpha(if (enabled) 1f else 0.5f).background(itemContent))
        is AzComposableContent -> itemContent.content(enabled)
        is Int -> {
            val isResource = try { context.resources.getResourceName(itemContent) != null } catch (e: Exception) { false }
            if (isResource) {
                Image(
                    painter = painterResource(itemContent),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                TextContent(itemContent.toString(), color)
            }
        }
        is String -> TextContent(itemContent, color)
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