package com.hereliesaz.aznavrail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor

class AzFormScope {
    internal val entries = mutableListOf<AzFormEntry>()

    fun entry(
        entryName: String,
        hint: String,
        multiline: Boolean = false,
        secret: Boolean = false
    ) {
        entries.add(AzFormEntry(entryName, hint, multiline, secret))
    }
}

internal data class AzFormEntry(
    val entryName: String,
    val hint: String,
    val multiline: Boolean,
    val secret: Boolean
)

@Composable
fun AzForm(
    formName: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    onSubmit: (Map<String, String>) -> Unit,
    submitButtonContent: @Composable () -> Unit = { Text("Submit") },
    content: AzFormScope.() -> Unit
) {
    val scope = remember { AzFormScope().apply(content) }
    val formData = rememberSaveable {
        mutableStateMapOf<String, String>().apply {
            scope.entries.forEach { entry ->
                this[entry.entryName] = ""
            }
        }
    }

    Column(modifier = modifier) {
        scope.entries.forEachIndexed { index, entry ->
            if (index == scope.entries.lastIndex) {
                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        AzTextBox(
                            value = formData[entry.entryName] ?: "",
                            onValueChange = { formData[entry.entryName] = it },
                            hint = entry.hint,
                            outlined = outlined,
                            multiline = entry.multiline,
                            secret = entry.secret,
                            outlineColor = outlineColor,
                            submitButtonContent = null,
                            onSubmit = { /* Individual onSubmit is not used in a form context */ }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    CompositionLocalProvider(LocalContentColor provides outlineColor) {
                        Box(
                            modifier = Modifier
                                .clickable { onSubmit(formData.toMap()) }
                                .background(AzTextBoxDefaults.getBackgroundColor().copy(alpha = AzTextBoxDefaults.getBackgroundOpacity()))
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
            } else {
                AzTextBox(
                    value = formData[entry.entryName] ?: "",
                    onValueChange = { formData[entry.entryName] = it },
                    hint = entry.hint,
                    outlined = outlined,
                    multiline = entry.multiline,
                    secret = entry.secret,
                    outlineColor = outlineColor,
                    submitButtonContent = null,
                    onSubmit = { /* Individual onSubmit is not used in a form context */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
