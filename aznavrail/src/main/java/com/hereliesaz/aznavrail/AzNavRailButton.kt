package com.hereliesaz.aznavrail

import android.annotation.SuppressLint
import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.util.text.AutoSizeText

/**
 * A versatile button component used within the AzNavRail.
 *
 * It supports various shapes, dynamic content (text, image, color), and states (loading, selected).
 * It automatically sizes text to fit within its bounds.
 *
 * @param onClick A lambda to be executed when the button is clicked.
 * @param text The text to display on the button.
 * @param modifier The modifier to be applied to the button.
 * @param size The size of the button (width/height for square/circle shapes).
 * @param color The base color of the button's border and text.
 * @param activeColor The color used when the button is in an active/selected state.
 * @param colors Custom [ButtonColors] to override the default color logic.
 * @param shape The shape of the button ([AzButtonShape.CIRCLE], [AzButtonShape.RECTANGLE], etc.).
 * @param enabled Whether the button is enabled and interactive.
 * @param isSelected Whether the button is currently selected.
 * @param isLoading Whether the button is in a loading state.
 * @param contentPadding The padding to be applied to the button's content.
 * @param itemContent Optional dynamic content to display instead of text. Can be a [Color], resource ID (Int), Number, or image URL.
 * @param content Optional composable content to append to the text layout.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    shape: AzButtonShape = AzButtonShape.CIRCLE,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    itemContent: Any? = null,
    content: @Composable () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val buttonShape = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RoundedCornerShape(0.dp)
        AzButtonShape.RECTANGLE -> RoundedCornerShape(0.dp)
        AzButtonShape.NONE -> RoundedCornerShape(0.dp)
    }

    val buttonModifier = when (shape) {
        AzButtonShape.CIRCLE -> modifier
            .requiredSize(size)
            .aspectRatio(1f)
        AzButtonShape.SQUARE -> modifier
            .requiredSize(size)
            .aspectRatio(1f)
        AzButtonShape.RECTANGLE, AzButtonShape.NONE -> modifier.requiredHeight(36.dp)
    }

    val disabledColor = color.copy(alpha = 0.5f)
    val targetColor = if (isPressed) {
        if (isSelected) color else activeColor
    } else {
        if (isSelected) activeColor else color
    }

    val finalColor = targetColor
    val defaultColors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (isSelected && !isPressed) activeColor.copy(alpha = 0.1f) else Color.Transparent,
        contentColor = if (!enabled) disabledColor else finalColor,
        disabledContainerColor = Color.Transparent,
        disabledContentColor = disabledColor
    )

    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier,
        shape = buttonShape,
        border = if (shape == AzButtonShape.NONE) BorderStroke(0.dp, Color.Transparent) else BorderStroke(3.dp, if (!enabled) disabledColor else finalColor),
        colors = colors ?: defaultColors,
        contentPadding = contentPadding,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (itemContent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isLoading) 0f else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (itemContent) {
                        is Color -> Box(modifier = Modifier.fillMaxSize().background(itemContent))
                        is Int -> {
                            val context = LocalContext.current
                            val isResource = remember(itemContent) {
                                try {
                                    context.resources.getResourceName(itemContent)
                                    true
                                } catch (e: Resources.NotFoundException) {
                                    false
                                }
                            }

                            if (isResource) {
                                Image(
                                    painter = painterResource(itemContent),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AutoSizeText(
                                    text = itemContent.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textAlign = TextAlign.Center,
                                        color = if (!enabled) disabledColor else finalColor
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    alignment = Alignment.Center,
                                    lineSpaceRatio = 0.9f
                                )
                            }
                        }
                        is Number -> AutoSizeText(
                            text = itemContent.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = if (!enabled) disabledColor else finalColor
                            ),
                            maxLines = 1,
                            softWrap = false,
                            alignment = Alignment.Center,
                            lineSpaceRatio = 0.9f
                        )
                        else -> {
                            Image(
                                painter = rememberAsyncImagePainter(model = itemContent),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(if (isLoading) 0f else 1f)
                ) {
                    val textModifier = when (shape) {
                        AzButtonShape.RECTANGLE, AzButtonShape.NONE -> Modifier
                        AzButtonShape.CIRCLE, AzButtonShape.SQUARE -> Modifier.weight(1f)
                    }

                    AutoSizeText(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textAlign = TextAlign.Center,
                            color = if (!enabled) disabledColor else finalColor
                        ),
                        modifier = textModifier,
                        maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
                        softWrap = false,
                        alignment = Alignment.Center,
                        lineSpaceRatio = 0.9f
                    )
                    content()
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .wrapContentSize(align = Alignment.Center, unbounded = true)
                ) {
                    AzLoad()
                }
            }
        }
    }
}
