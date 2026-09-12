package com.hereliesaz.aznavrail.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.AzRoller
import com.hereliesaz.aznavrail.AzSlider
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzSliderConfig
import com.hereliesaz.aznavrail.model.AzSliderVariant
import com.hereliesaz.aznavrail.util.EqualWidthLayout
import com.hereliesaz.aznavrail.util.text.AutoSizeText
import kotlinx.coroutines.launch

/**
 * Standalone widget showcase for the CMP demo — mirrors [SampleApp's StandaloneWidgetsScreen]
 * and [LegacyRailDemoScreen] so every public composable in aznavrail-cmp gets an exercise.
 */
@Composable
fun WidgetShowcase() {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Widget Showcase",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        // ── AzTextBox ──────────────────────────────────────────────────────────
        Section("AzTextBox") {
            AzTextBox(
                hint = "Uncontrolled (History: Search)",
                historyContext = "cmp_search",
                onSubmit = {},
                submitButtonContent = { Text("Go") },
            )

            var controlled by remember { mutableStateOf("") }
            AzTextBox(
                value = controlled,
                onValueChange = { controlled = it },
                hint = "Controlled",
                historyContext = "cmp_controlled",
                onSubmit = {},
                submitButtonContent = { Text("Go") },
            )

            AzTextBox(
                hint = "No outline",
                outlined = false,
                onSubmit = {},
                submitButtonContent = { Text("Go") },
            )

            AzTextBox(
                hint = "Disabled",
                enabled = false,
                onSubmit = {},
            )
        }

        // ── AzForm ─────────────────────────────────────────────────────────────
        Section("AzForm") {
            AzForm(
                formName = "cmpLoginForm",
                onSubmit = {},
                submitButtonContent = { Text("Submit") },
            ) {
                entry(entryName = "username", hint = "Username")
                entry(entryName = "password", hint = "Password", secret = true)
                entry(entryName = "bio", hint = "Bio", multiline = true)
            }
        }

        // ── AzLoad ─────────────────────────────────────────────────────────────
        Section("AzLoad") {
            Box(modifier = Modifier.size(96.dp)) {
                AzLoad()
            }
        }

        // ── AzDivider ──────────────────────────────────────────────────────────
        Section("AzDivider (orientation-aware)") {
            AzDivider(modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.height(48.dp)) {
                Text("Left")
                AzDivider(modifier = Modifier.width(16.dp))
                Text("Right")
            }
        }

        // ── AzButton ───────────────────────────────────────────────────────────
        Section("AzButton — every shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzButtonShape.values().forEach { shape ->
                    AzButton(
                        onClick = {},
                        text = shape.name,
                        shape = shape,
                    )
                }
            }
        }

        // ── AzToggle ───────────────────────────────────────────────────────────
        Section("AzToggle — every shape") {
            var checked by remember { mutableStateOf(false) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzButtonShape.values().forEach { shape ->
                    AzToggle(
                        isChecked = checked,
                        onToggle = { checked = !checked },
                        toggleOnText = "${shape.name} On",
                        toggleOffText = "${shape.name} Off",
                        shape = shape,
                    )
                }
            }
        }

        // ── AzCycler ───────────────────────────────────────────────────────────
        Section("AzCycler") {
            val options = remember { listOf("Alpha", "Beta", "Gamma", "Delta") }
            var selected by remember { mutableStateOf(options.first()) }
            AzCycler(
                options = options,
                selectedOption = selected,
                onCycle = {
                    val next = options[(options.indexOf(selected) + 1) % options.size]
                    selected = next
                },
                shape = AzButtonShape.RECTANGLE,
            )
        }

        // ── AzRoller ───────────────────────────────────────────────────────────
        Section("AzRoller (filtering dropdown)") {
            var rollerValue by remember { mutableStateOf("Cherry") }
            AzRoller(
                options = listOf("Cherry", "Bell", "Bar", "Seven", "Diamond", "Lemon", "Plum"),
                selectedOption = rollerValue,
                onOptionSelected = { rollerValue = it },
                hint = "Pick a symbol",
                enabled = true,
            )
            Text("Selected: $rollerValue", style = MaterialTheme.typography.bodySmall)
        }

        // ── AzSlider ───────────────────────────────────────────────────────────
        Section("AzSlider — variants") {
            var continuous by remember { mutableStateOf(0.5f) }
            AzSlider(
                value = continuous,
                onValueChange = { continuous = it },
                label = "Continuous",
            )

            var stepped by remember { mutableStateOf(0f) }
            AzSlider(
                value = stepped,
                onValueChange = { stepped = it },
                config = AzSliderConfig(variant = AzSliderVariant.STEPPED),
                label = "Stepped",
            )

            var centered by remember { mutableStateOf(0f) }
            AzSlider(
                value = centered,
                onValueChange = { centered = it },
                config = AzSliderConfig(variant = AzSliderVariant.CENTERED),
                label = "Centered",
            )
        }

        // ── EqualWidthLayout ───────────────────────────────────────────────────
        Section("EqualWidthLayout") {
            EqualWidthLayout(verticalSpacing = 8.dp) {
                listOf("One", "Two", "Three is much longer", "4").forEach {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE0E0E0))
                            .padding(8.dp),
                    ) {
                        Text(it)
                    }
                }
            }
        }

        // ── AutoSizeText ───────────────────────────────────────────────────────
        Section("AutoSizeText") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF263238)),
            ) {
                AutoSizeText(
                    text = "This text shrinks to fit, no matter how long it gets",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    color = Color.White,
                    maxLines = 1,
                    maxTextSize = 24.sp,
                    minTextSize = 8.sp,
                    alignment = Alignment.Center,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
