package com.hereliesaz.aznavrail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.aznavrail.util.HistoryManager

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

object AzTextBoxDefaults {
    private var suggestionLimit: Int = 5
    private var backgroundColor: Color = Color.Transparent
    private var backgroundOpacity: Float = 1.0f

    fun setSuggestionLimit(limit: Int) {
        suggestionLimit = limit.coerceIn(0, 5)
    }

    fun setBackgroundColor(color: Color) {
        backgroundColor = color
    }

    fun setBackgroundOpacity(opacity: Float) {
        backgroundOpacity = opacity.coerceIn(0f, 1f)
    }

    internal fun getSuggestionLimit(): Int = suggestionLimit
    internal fun getBackgroundColor(): Color = backgroundColor
    internal fun getBackgroundOpacity(): Float = backgroundOpacity
}

@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "",
    outlined: Boolean = true,
    multiline: Boolean = false,
    secret: Boolean = false,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    submitButtonContent: (@Composable () -> Unit)? = null,
    onSubmit: (String) -> Unit
) {
    require(!multiline || !secret) {
        "AzTextBox cannot be both multiline and secret."
    }

    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val suggestionLimit = AzTextBoxDefaults.getSuggestionLimit()
    val backgroundColor = AzTextBoxDefaults.getBackgroundColor().copy(alpha = AzTextBoxDefaults.getBackgroundOpacity())
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(suggestionLimit) {
        HistoryManager.init(context, suggestionLimit)
    }

    LaunchedEffect(value) {
        suggestions = if (value.isNotBlank()) {
            HistoryManager.getSuggestions(value)
        } else {
            emptyList()
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.background(backgroundColor),
            verticalAlignment = if (multiline) Alignment.Bottom else Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (!multiline) Modifier.height(36.dp) else Modifier)
                    .then(
                        if (outlined) {
                            Modifier.border(1.dp, outlineColor)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textStyle = TextStyle(fontSize = 10.sp, color = outlineColor),
                    singleLine = !multiline,
                    cursorBrush = SolidColor(outlineColor),
                    visualTransformation = if (secret && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None
                ) { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(text = hint, fontSize = 10.sp, color = Color.Gray)
                            }
                            innerTextField()
                        }
                        if (value.isNotEmpty()) {
                            val icon = when {
                                secret && isPasswordVisible -> Icons.Default.VisibilityOff
                                secret && !isPasswordVisible -> Icons.Default.Visibility
                                else -> Icons.Default.Clear
                            }
                            val contentDescription = when {
                                secret && isPasswordVisible -> "Hide password"
                                secret && !isPasswordVisible -> "Show password"
                                else -> "Clear text"
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = contentDescription,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        if (secret) {
                                            isPasswordVisible = !isPasswordVisible
                                        } else {
                                            onValueChange("")
                                        }
                                    },
                                tint = outlineColor
                            )
                        }
                    }
                }
            }
            if (submitButtonContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                CompositionLocalProvider(LocalContentColor provides outlineColor) {
                    Box(
                        modifier = Modifier
                            .clickable {
                                onSubmit(value)
                                HistoryManager.addEntry(value)
                                onValueChange("")
                            }
                            .then(
                                if (!outlined) {
                                    Modifier.border(1.dp, outlineColor)
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        submitButtonContent()
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            val surfaceColor = MaterialTheme.colorScheme.surface
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .border(1.dp, outlineColor)
            ) {
                itemsIndexed(suggestions) { index, suggestion ->
                    val suggestionBgColor = if (index % 2 == 0) {
                        surfaceColor.copy(alpha = 0.9f)
                    } else {
                        surfaceColor.copy(alpha = 0.8f)
                    }
                    Text(
                        text = suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(suggestionBgColor)
                            .clickable {
                                onValueChange(suggestion)
                                suggestions = emptyList()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .fadeRight(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun Modifier.fadeRight(): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = size.width - 50f, // Start fading from 50px from the right
            endX = size.width
        ),
        blendMode = BlendMode.DstIn
    )
}
