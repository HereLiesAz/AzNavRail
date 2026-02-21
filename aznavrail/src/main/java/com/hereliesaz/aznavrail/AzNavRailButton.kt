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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.util.text.AutoSizeText

@Composable
internal fun AzNavRailButton(
    item: AzNavItem,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onGloballyPositioned: ((Rect) -> Unit)? = null
) {
    val isActiveRoute = item.route != null && currentDestination == item.route
    val isActiveClassifier = item.classifiers.any { activeClassifiers.contains(it) }
    val isActive = isActiveRoute || isActiveClassifier

    val color = item.color ?: MaterialTheme.colorScheme.onSurface
    val finalColor = if (isActive) activeColor else color
    val disabledColor = color.copy(alpha = 0.38f)

    val displayText = if (item.isToggle) {
        if (item.isChecked == true) item.toggleOnText else item.toggleOffText
    } else if (item.isCycler) {
        item.selectedOption ?: ""
    } else {
        item.text
    }

    val context = LocalContext.current
    val itemContent = item.content
    val isResource = remember(itemContent) {
        if (itemContent is Int) {
            try {
                context.resources.getResourceName(itemContent) != null
            } catch (e: android.content.res.Resources.NotFoundException) {
                false
            }
        } else false
    }

    val basePadding = 8.dp
    val effectiveContentPadding = if (itemContent is Color || isResource || (itemContent != null && itemContent !is Int && itemContent !is Number && itemContent !is Color)) {
        PaddingValues(0.dp)
    } else {
        PaddingValues(basePadding)
    }

    val widthModifier = if (item.shape == AzButtonShape.RECTANGLE || item.shape == AzButtonShape.NONE) {
        Modifier.padding(horizontal = 8.dp)
    } else {
        Modifier.size(size)
    }

    val heightModifier = if (item.shape == AzButtonShape.RECTANGLE || item.shape == AzButtonShape.NONE) {
        Modifier.padding(vertical = 4.dp)
    } else {
        Modifier
    }

    val shapeModifier = when (item.shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RoundedCornerShape(8.dp)
        AzButtonShape.RECTANGLE -> RoundedCornerShape(8.dp)
        AzButtonShape.NONE -> RectangleShape
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(widthModifier)
            .then(heightModifier)
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned?.invoke(coordinates.boundsInWindow())
            }
    ) {
        val btnModifier = if (item.shape == AzButtonShape.RECTANGLE || item.shape == AzButtonShape.NONE) {
            Modifier
        } else {
            Modifier.fillMaxSize()
        }

        OutlinedButton(
            onClick = { },
            modifier = btnModifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { if (!item.disabled) onClick() },
                    onLongClick = { if (!item.disabled && onLongClick != null) onLongClick() }
                ),
            shape = shapeModifier,
            border = if (item.shape == AzButtonShape.NONE) BorderStroke(0.dp, Color.Transparent)
            else BorderStroke(3.dp, if (!item.disabled) finalColor else disabledColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = effectiveContentPadding,
            enabled = !item.disabled,
            interactionSource = interactionSource
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (itemContent != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            textAlign = TextAlign.Center,
                                            color = if (!item.disabled) finalColor else disabledColor
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
                                    color = if (!item.disabled) finalColor else disabledColor
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
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val textModifier = when (item.shape) {
                            AzButtonShape.RECTANGLE, AzButtonShape.NONE -> Modifier
                            AzButtonShape.CIRCLE, AzButtonShape.SQUARE -> Modifier.weight(1f)
                        }
                        AutoSizeText(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = if (!item.disabled) finalColor else disabledColor
                            ),
                            modifier = textModifier,
                            maxLines = if (displayText.contains("\n")) Int.MAX_VALUE else 1,
                            softWrap = false,
                            alignment = Alignment.Center,
                            lineSpaceRatio = 0.9f
                        )
                    }
                }
            }
        }
    }
}
