package com.hereliesaz.SampleApp.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape

private const val TAG = "FormShowcaseScreen"

@Composable
fun FormShowcaseScreen() {
    var suggestionLimit by remember { mutableFloatStateOf(3f) }
    var backgroundOpacity by remember { mutableFloatStateOf(0.0f) }
    var lastSubmission by remember { mutableStateOf("(no submissions yet)") }

    LaunchedEffect(suggestionLimit, backgroundOpacity) {
        AzTextBoxDefaults.setSuggestionLimit(suggestionLimit.toInt())
        AzTextBoxDefaults.setBackgroundColor(Color(0xFF1A237E))
        AzTextBoxDefaults.setBackgroundOpacity(backgroundOpacity)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Forms & Text Inputs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Text("AzTextBoxDefaults", fontWeight = FontWeight.SemiBold)
        Text("setSuggestionLimit: ${suggestionLimit.toInt()}", style = MaterialTheme.typography.bodySmall)
        Slider(value = suggestionLimit, onValueChange = { suggestionLimit = it }, valueRange = 0f..5f, steps = 4)
        Text("setBackgroundOpacity: ${"%.2f".format(backgroundOpacity)} (color = Indigo 900)", style = MaterialTheme.typography.bodySmall)
        Slider(value = backgroundOpacity, onValueChange = { backgroundOpacity = it }, valueRange = 0f..1f)

        Text("AzForm — every entry parameter", fontWeight = FontWeight.SemiBold)
        AzForm(
            formName = "showcaseForm",
            outlined = true,
            outlineColor = Color(0xFF00ACC1),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { Log.d(TAG, "form ime done") }),
            trailingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            onSubmit = { lastSubmission = it.toString() },
            submitButtonContent = { Text("Submit") },
        ) {
            entry(entryName = "username", hint = "Username", leadingIcon = { Icon(Icons.Default.AccountCircle, null) })
            entry(entryName = "email", hint = "Email", initialValue = "you@example.com", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            entry(entryName = "password", hint = "Password", secret = true, leadingIcon = { Icon(Icons.Default.Lock, null) })
            entry(entryName = "bio", hint = "Biography (multiline)", multiline = true, leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null) })
            entry(entryName = "broken", hint = "Always in error state", isError = true)
            entry(entryName = "frozen", hint = "Disabled entry", enabled = false, initialValue = "read only")
        }
        Text("Last submission: $lastSubmission", style = MaterialTheme.typography.bodySmall)

        Text("Standalone AzTextBox variants", fontWeight = FontWeight.SemiBold)

        AzTextBox(
            hint = "Autocomplete (history: showcase)",
            historyContext = "showcase",
            onSubmit = { Log.d(TAG, "Autocomplete submitted: $it") },
            submitButtonContent = { Text("Go") },
        )

        AzTextBox(
            hint = "Password with toggle",
            secret = true,
            onSubmit = { Log.d(TAG, "Password submitted") },
            submitButtonContent = { Text("Set") },
        )

        AzTextBox(
            hint = "No clear button",
            showClearButton = false,
            onSubmit = { Log.d(TAG, "No-clear submitted: $it") },
        )

        AzTextBox(
            hint = "Custom fill color",
            fillColor = Color(0x331565C0),
            outlined = false,
            onSubmit = { Log.d(TAG, "Custom fill submitted: $it") },
            submitButtonContent = { Text("Send") },
        )

        AzTextBox(
            hint = "Half width",
            modifier = Modifier.padding(end = 8.dp),
            onSubmit = { Log.d(TAG, "Half-width submitted: $it") },
        )
    }
}
