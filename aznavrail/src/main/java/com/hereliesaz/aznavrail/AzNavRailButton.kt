package com.hereliesaz.aznavrail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    colors: ButtonColors? = null,
    size: Dp = AzNavRailDefaults.ButtonSize,
    shape: AzButtonShape = AzButtonShape.RECTANGLE,
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

    // STRICT COMPLIANCE: Override all shapes to RectangleShape
    val shapeModifier = RectangleShape

    val interactionSource = remember { MutableInteractionSource() }

    // Logic: If user asked for RECTANGLE or NONE (or anything else since we override shape), use box sizing
    // But we still want to respect the 'intent' of spacing if it was originally Circle/Square vs Rect.
    // However, compliance dictates "NO rounded corners". It doesn't strictly say "everything is 36dp high".
    // But AzNavRail usually has square/circle buttons as 48dp and rect as 36dp.
    // We will keep the sizing logic but force the visual shape to Rectangle.
    
    val btnModifier = if (shape == AzButtonShape.RECTANGLE || shape == AzButtonShape.NONE) {
        Modifier.padding(horizontal = 8.dp, vertical = 4.dp).defaultMinSize(minWidth = 64.dp, minHeight = 36.dp)
    } else {
        // Even if it was CIRCLE, we render it as a Square now (size x size).
        Modifier.requiredSize(size) 
    }

    val borderModifier = if (shape != AzButtonShape.NONE) {
        Modifier.border(3.dp, if (enabled) finalColor else disabledColor, shapeModifier)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(btnModifier)
            .onGloballyPositioned { coordinates ->
                onGloballyPositioned?.invoke(coordinates.boundsInWindow())
            }
            .clip(shapeModifier)
            .then(borderModifier)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { if (enabled) onClick() },
                onLongClick = { if (enabled && onLongClick != null) onLongClick() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(effectiveContentPadding).alpha(if (isLoading) 0f else 1f),
            contentAlignment = Alignment.Center
        ) {
            if (itemContent != null) {
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
            } else {
                AutoSizeText(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = if (enabled) finalColor else disabledColor),
                    maxLines = if (text.contains("\n")) Int.MAX_VALUE else 1,
                    softWrap = false, alignment = Alignment.Center, lineSpaceRatio = 0.9f
                )
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.matchParentSize().wrapContentSize(align = Alignment.Center, unbounded = true)) {
                AzLoad()
            }
        }
    }
}
