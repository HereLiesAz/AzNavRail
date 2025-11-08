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

object AzTextBoxDefaults {
    private var suggestionLimit: Int = 5

    fun setSuggestionLimit(limit: Int) {
        suggestionLimit = limit.coerceIn(0, 5)
    }

    internal fun getSuggestionLimit(): Int = suggestionLimit
}

@Composable
fun AzTextBox(
    modifier: Modifier = Modifier,
    hint: String = "",
    outlined: Boolean = true,
    submitButtonContent: @Composable () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val suggestionLimit = AzTextBoxDefaults.getSuggestionLimit()

    LaunchedEffect(suggestionLimit) {
        HistoryManager.init(context, suggestionLimit)
    }

    LaunchedEffect(text) {
        suggestions = if (text.isNotBlank()) {
            HistoryManager.getSuggestions(text)
        } else {
            emptyList()
        }
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (outlined) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    textStyle = TextStyle(fontSize = 10.sp, color = LocalContentColor.current),
                    singleLine = true,
                    cursorBrush = SolidColor(LocalContentColor.current)
                ) { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (text.isEmpty()) {
                                Text(text = hint, fontSize = 10.sp, color = Color.Gray)
                            }
                            innerTextField()
                        }
                        if (text.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear text",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { text = "" },
                                tint = LocalContentColor.current
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clickable {
                        onSubmit(text)
                        HistoryManager.addEntry(text)
                        text = ""
                    }
                    .then(
                        if (!outlined) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                submitButtonContent()
            }
        }

        if (suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                items(suggestions) { suggestion ->
                    Text(
                        text = suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                text = suggestion
                                suggestions = emptyList()
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .fadeRight(),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        fontSize = 10.sp
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
