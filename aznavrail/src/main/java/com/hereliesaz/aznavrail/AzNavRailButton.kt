package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.util.text.AutoSizeText

@Composable
internal fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    size: Dp = AzNavRailDefaults.ButtonSize,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    itemContent: Any? = null,
    onLongClick: (() -> Unit)? = null,
    onGloballyPositioned: ((Rect) -> Unit)? = null
) {
    val finalColor = if (isSelected) activeColor else color
    val disabledColor = color.copy(alpha = 0.38f)

    val context = LocalContext.current
    val isResource = remember(itemContent) {
        if (itemContent is Int) {
            try {
                context.resources.getResourceName(itemContent) != null
            } catch (e: android.content.res.Resources.NotFoundException) {
                false
            }
        } else false
    }

    val effectiveContentPadding = if (itemContent is Color || isResource || (itemContent != null && itemContent !is Int && itemContent !is Number && itemContent !is Color)) {
        PaddingValues(0.dp)
    } else {
        contentPadding
    }

    val widthModifier = if (shape == AzButtonShape.RECTANGLE || shape == AzButtonShape.NONE) {
        Modifier.padding(horizontal = 8.dp)
    } else {
        Modifier.size(size)
    }

    val heightModifier = if (shape == AzButtonShape.RECTANGLE || shape == AzButtonShape.NONE) {
        Modifier.padding(vertical = 4.dp)
    } else {
        Modifier
    }

    val shapeModifier = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RoundedCornerShape(8.dp)
        AzButtonShape.RECTANGLE -> RoundedCornerShape(8.dp)
        AzButtonShape.NONE -> RectangleShape
    }

    val interactionSource = remember { MutableInteractionSource() }
    val defaultColors = ButtonDefaults.outlinedButtonColors(
        containerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent
    )

    Box(
        modifier = modifier
            .then(widthModifier)
            .then(heightModifier)
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned?.invoke(coordinates.boundsInWindow())
            }
    ) {
        val btnModifier = if (shape == AzButtonShape.RECTANGLE || shape == AzButtonShape.NONE) Modifier else Modifier.fillMaxSize()

        OutlinedButton(
            onClick = { },
            modifier = btnModifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { if (enabled) onClick() },
                    onLongClick = { if (enabled && onLongClick != null) onLongClick() }
                ),
            shape = shapeModifier,
            border = if (shape == AzButtonShape.NONE) BorderStroke(0.dp, Color.Transparent)
            else BorderStroke(3.dp, if (enabled) finalColor else disabledColor),
            colors = colors ?: defaultColors,
            contentPadding = effectiveContentPadding,
            enabled = enabled,
            interactionSource = interactionSource
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (itemContent != null) {
                    Box(
                        modifier = Modifier.fillMaxSize().alpha(if (isLoading) 0f else 1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (itemContent) {
                            is Color -> Box(modifier = Modifier.fillMaxSize().background(itemContent))
                            is Int -> {
                                if (isResource) {
                                    Image(
                                        painter = painterResource(itemContent),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AutoSizeText(
                                        text = itemContent.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = if (enabled) finalColor else disabledColor),
                                        maxLines = 1, softWrap = false, alignment = Alignment.Center, lineSpaceRatio = 0.9f
                                    )
                                }
                            }
                            is Number -> AutoSizeText(
                                text = itemContent.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = if (enabled) finalColor else disabledColor),
                                maxLines = 1, softWrap = false, alignment = Alignment.Center, lineSpaceRatio = 0.9f
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
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (isLoading) 0f else 1f)) {
                        val textModifier = when (shape) {
                            AzButtonShape.RECTANGLE, AzButtonShape.NONE -> Modifier
                            AzButtonShape.CIRCLE, AzButtonShape.SQUARE -> Modifier.weight(1f)
                        }
                        AutoSizeText(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = if (enabled) finalColor else disabledColor),
                            modifier = textModifier,
                            maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
                            softWrap = false, alignment = Alignment.Center, lineSpaceRatio = 0.9f
                        )
                    }
                }
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.matchParentSize().wrapContentSize(align = Alignment.Center, unbounded = true)) {
                AzLoad()
            }
        }
    }
}