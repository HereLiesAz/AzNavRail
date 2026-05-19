package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class HiddenMenuDemoState(
    val relocOrder: List<String>,
    val nicknameValue: String,
    val tagValue: String,
    val lastAction: String,
    val relocateLog: String,
)

@Composable
fun HiddenMenuDemoScreen(state: HiddenMenuDemoState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hidden Menus on Reloc Items", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Tap and hold a reloc item on the rail to reveal its hidden menu. Each menu mixes plain listItem callbacks, route-based listItems, and inputItem fields. State below reflects callbacks fired by the menu DSL.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("Reloc cluster order", fontWeight = FontWeight.SemiBold)
        Text(state.relocOrder.joinToString(", "))
        Text("Last reorder: ${state.relocateLog}", style = MaterialTheme.typography.bodySmall)

        Text("inputItem(\"nickname\")", fontWeight = FontWeight.SemiBold)
        Text(state.nicknameValue.ifEmpty { "(empty)" })

        Text("inputItem(\"tag\", initialValue = \"foo\")", fontWeight = FontWeight.SemiBold)
        Text(state.tagValue.ifEmpty { "(empty)" })

        Text("Last listItem callback", fontWeight = FontWeight.SemiBold)
        Text(state.lastAction)
    }
}
