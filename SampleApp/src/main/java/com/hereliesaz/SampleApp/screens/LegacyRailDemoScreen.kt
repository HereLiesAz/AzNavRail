package com.hereliesaz.SampleApp.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzRoller
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape

private const val TAG = "LegacyRailDemoScreen"

/**
 * The original SampleApp playground — text boxes, forms, button/toggle/cycler/roller variants.
 * Preserved so the existing demos still ship while the new screens cover the remaining API.
 */
@Composable
fun LegacyRailDemoScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        AzTextBox(
            modifier = Modifier.padding(bottom = 16.dp),
            hint = "Uncontrolled (History: Search)",
            historyContext = "search_history",
            onSubmit = { Log.d(TAG, "Uncontrolled submitted: $it") },
            submitButtonContent = { Text("Go") }
        )

        var controlledText by remember { mutableStateOf("") }
        AzTextBox(
            modifier = Modifier.padding(bottom = 16.dp),
            value = controlledText,
            onValueChange = { controlledText = it },
            hint = "Controlled (History: Usernames)",
            historyContext = "username_history",
            onSubmit = { Log.d(TAG, "Controlled submitted: $it") },
            submitButtonContent = { Text("Go") }
        )

        AzTextBox(
            modifier = Modifier.padding(bottom = 16.dp),
            hint = "Uncontrolled (No Outline)",
            outlined = false,
            onSubmit = { Log.d(TAG, "No-outline submitted: $it") },
            submitButtonContent = { Text("Go") }
        )

        AzTextBox(
            modifier = Modifier.padding(bottom = 16.dp),
            hint = "Disabled",
            enabled = false,
            onSubmit = { Log.d(TAG, "Disabled submitted") }
        )

        AzForm(
            formName = "loginForm",
            modifier = Modifier.padding(bottom = 16.dp),
            onSubmit = { Log.d(TAG, "Login form submitted: $it") },
            submitButtonContent = { Text("Login") }
        ) {
            entry(entryName = "username", hint = "Username")
            entry(entryName = "password", hint = "Password", secret = true)
            entry(entryName = "bio", hint = "Biography", multiline = true)
        }

        AzForm(
            formName = "registrationForm",
            outlined = false,
            onSubmit = { Log.d(TAG, "Registration form submitted: $it") },
            submitButtonContent = { Text("Register") }
        ) {
            entry(entryName = "email", hint = "Email", enabled = false)
            entry(entryName = "confirm_password", hint = "Confirm Password", secret = true)
        }

        Row {
            var buttonLoading by remember { mutableStateOf(false) }
            AzButton(
                onClick = {
                    Log.d(TAG, "Standalone AzButton clicked")
                    buttonLoading = !buttonLoading
                },
                text = "Button",
                shape = AzButtonShape.SQUARE,
                isLoading = buttonLoading,
                contentPadding = PaddingValues(16.dp)
            )

            AzButton(
                onClick = { Log.d(TAG, "Disabled clicked") },
                text = "Disabled",
                enabled = false
            )

            var isToggled by remember { mutableStateOf(false) }
            AzToggle(
                isChecked = isToggled,
                onToggle = { isToggled = !isToggled },
                toggleOnText = "On",
                toggleOffText = "Off",
                shape = AzButtonShape.RECTANGLE
            )

            val cyclerOptions = remember { listOf("1", "2", "3") }
            var selectedCyclerOption by remember { mutableStateOf(cyclerOptions.first()) }
            AzCycler(
                options = cyclerOptions,
                selectedOption = selectedCyclerOption,
                onCycle = {
                    val nextIndex = (cyclerOptions.indexOf(selectedCyclerOption) + 1) % cyclerOptions.size
                    selectedCyclerOption = cyclerOptions[nextIndex]
                },
                shape = AzButtonShape.CIRCLE
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AzRoller(
            options = listOf("Cherry", "Bell", "Bar", "Seven", "Diamond"),
            selectedOption = "Cherry",
            onOptionSelected = { Log.d(TAG, "Roller selected: $it") },
            hint = "Roller Select",
            enabled = true
        )
    }
}
