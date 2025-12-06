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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults

class AzFormScope {
    internal val entries = mutableListOf<AzFormEntry>()

    fun entry(
        entryName: String,
        hint: String,
        multiline: Boolean = false,
        secret: Boolean = false,
        leadingIcon: @Composable (() -> Unit)? = null,
        isError: Boolean = false,       
        enabled: Boolean = true,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default
    ) {
        entries.add(AzFormEntry(
            entryName = entryName,
            hint = hint,
            multiline = multiline,
            secret = secret,
            leadingIcon = leadingIcon,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            enabled = enabled
        ))
    }
}

internal data class AzFormEntry(
    val entryName: String,
    val hint: String,
    val multiline: Boolean,
    val secret: Boolean,
    val leadingIcon: @Composable (() -> Unit)?,
    val isError: Boolean,
    val keyboardOptions: KeyboardOptions,
    val keyboardActions: KeyboardActions,
    val enabled: Boolean
)

@Composable
fun AzForm(
    formName: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    outlineColor: Color = MaterialTheme.colorScheme.primary,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onSubmit: (Map<String, String>) -> Unit,
    submitButtonContent: @Composable () -> Unit = { Text("Submit") },
    content: AzFormScope.() -> Unit
) {
    val scope = remember(formName, content) { AzFormScope().apply(content) }
    val focusManager = LocalFocusManager.current
    val formData = rememberSaveable(
        saver = listSaver<SnapshotStateMap<String, String>, Pair<String, String>>(
            save = { it.toList() },
            restore = { it.toMutableStateMap() }
        )
    ) {
        mutableStateMapOf<String, String>().apply {
            scope.entries.forEach { entry ->
                this[entry.entryName] = ""
            }
        }
    }

    Column(modifier = modifier) {
        scope.entries.forEachIndexed { index, entry ->
            // Determine ImeAction
            val defaultImeAction = if (index == scope.entries.lastIndex) ImeAction.Send else ImeAction.Next
            val imeAction = if (entry.keyboardOptions.imeAction == ImeAction.Default) {
                if (keyboardOptions.imeAction == ImeAction.Default) defaultImeAction else keyboardOptions.imeAction
            } else {
                entry.keyboardOptions.imeAction
            }

            // Determine KeyboardActions
            val finalKeyboardActions = if (entry.keyboardActions == KeyboardActions.Default && keyboardActions == KeyboardActions.Default) {
                 if (imeAction == ImeAction.Next) {
                     KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                 } else if (imeAction == ImeAction.Send) {
                     KeyboardActions(onSend = { onSubmit(formData.toMap()) })
                 } else {
                     KeyboardActions.Default
                 }
            } else if (entry.keyboardActions != KeyboardActions.Default) {
                entry.keyboardActions
            } else {
                keyboardActions
            }

            val finalKeyboardOptions = entry.keyboardOptions.copy(imeAction = imeAction)

            if (index == scope.entries.lastIndex) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormEntryTextBox(
                            entry = entry,
                            value = formData[entry.entryName] ?: "",
                            onValueChange = { formData[entry.entryName] = it },
                            outlined = outlined,
                            outlineColor = outlineColor,
                            historyContext = formName,
                            trailingIcon = trailingIcon,
                            keyboardOptions = finalKeyboardOptions,
                            keyboardActions = finalKeyboardActions
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
                FormEntryTextBox(
                    entry = entry,
                    value = formData[entry.entryName] ?: "",
                    onValueChange = { formData[entry.entryName] = it },
                    outlined = outlined,
                    outlineColor = outlineColor,
                    historyContext = formName,
                    trailingIcon = trailingIcon,
                    keyboardOptions = finalKeyboardOptions,
                    keyboardActions = finalKeyboardActions
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun FormEntryTextBox(
    entry: AzFormEntry,
    value: String,
    onValueChange: (String) -> Unit,
    outlined: Boolean,
    outlineColor: Color,
    historyContext: String,
    trailingIcon: @Composable (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions
) {
    AzTextBox(
        value = value,
        onValueChange = onValueChange,
        historyContext = historyContext,
        hint = entry.hint,
        outlined = outlined,
        multiline = entry.multiline,
        secret = entry.secret,
        leadingIcon = entry.leadingIcon,
        trailingIcon = trailingIcon,
        isError = entry.isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = entry.enabled,
        outlineColor = outlineColor,
        submitButtonContent = null,
        onSubmit = { /* Individual onSubmit is not used in a form context */ }
    )
}
