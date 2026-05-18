package com.hereliesaz.SampleApp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.AzRoller
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.util.EqualWidthLayout
import com.hereliesaz.aznavrail.util.text.AutoSizeText

private const val TAG = "StandaloneWidgetsScreen"

@Composable
fun StandaloneWidgetsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Standalone Widgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Section("AzLoad") {
            Box(modifier = Modifier.size(96.dp)) {
                AzLoad()
            }
        }

        Section("AzDivider (orientation-aware)") {
            AzDivider(modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.height(48.dp)) {
                AzDivider(modifier = Modifier.fillMaxWidth())
            }
            Row(modifier = Modifier.height(48.dp)) {
                Text("Left")
                AzDivider(modifier = Modifier.width(16.dp))
                Text("Right")
            }
        }

        Section("AzButton — every shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzButtonShape.values().forEach { shape ->
                    AzButton(
                        onClick = { Log.d(TAG, "AzButton[$shape] clicked") },
                        text = shape.name,
                        shape = shape,
                    )
                }
            }
        }

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

        Section("AutoSizeText") {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF263238))) {
                AutoSizeText(
                    text = "This text shrinks to fit, no matter how long it gets",
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
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
