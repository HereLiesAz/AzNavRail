package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape

data class HelpSystemState(
    /**
     * Drives `azAdvanced(helpEnabled = ...)`. The library uses this flag to **auto-inject** a Help
     * menu item when no explicit `azHelpRailItem` / `azHelpSubItem` exists. The overlay itself is
     * always toggled by tapping that Help item — there is no public API to open it externally.
     */
    val autoInjectHelpEnabled: Boolean,
    /** Drives `azConfig(activeClassifiers = ...)` for programmatic highlighting. */
    val activeClassifiers: Set<String>,
    /** Counts every call to `onDismissHelp` so the user can see it fires. */
    val dismissCount: Int = 0,
)

/** Classifiers that are pre-applied to rail items defined in MainApp. */
val DemoClassifiers = listOf("focus", "advanced", "danger")

@Composable
fun HelpSystemDemoScreen(
    state: HelpSystemState,
    onChange: (HelpSystemState) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Help System", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Callout(
            "How to open the help overlay",
            "Tap the \"Help\" rail item (icon-only when collapsed, labelled \"Help\" when expanded). " +
                "The overlay is internal state of AzNavRail — there is no external API to open it. " +
                "Every item with `info` set will render its help text and a connecting line.",
        )

        Text("onDismissHelp callback fired: ${state.dismissCount} times", style = MaterialTheme.typography.bodyMedium)

        Text("azAdvanced(helpEnabled) — auto-inject behaviour", fontWeight = FontWeight.SemiBold)
        Text(
            "When `helpEnabled` is true AND no explicit Help item is registered, the library auto-injects one in the menu. This sample already registers `azHelpRailItem(\"toggle-help\")`, so the flag below has no effect here — turning it on confirms the library's no-op path.",
            style = MaterialTheme.typography.bodySmall,
        )
        AzToggle(
            isChecked = state.autoInjectHelpEnabled,
            onToggle = { onChange(state.copy(autoInjectHelpEnabled = !state.autoInjectHelpEnabled)) },
            toggleOnText = "auto-inject: On",
            toggleOffText = "auto-inject: Off",
            shape = AzButtonShape.RECTANGLE,
        )

        Text("activeClassifiers", fontWeight = FontWeight.SemiBold)
        Text(
            "Items in MainApp tag themselves with classifiers (\"focus\", \"advanced\", \"danger\"). Toggling a chip puts that string into `azConfig(activeClassifiers)` and the rail highlights every matching item.",
            style = MaterialTheme.typography.bodySmall,
        )
        DemoClassifiers.forEach { classifier ->
            val active = classifier in state.activeClassifiers
            AzToggle(
                isChecked = active,
                onToggle = {
                    val next = if (active) state.activeClassifiers - classifier else state.activeClassifiers + classifier
                    onChange(state.copy(activeClassifiers = next))
                },
                toggleOnText = "[$classifier] On",
                toggleOffText = "[$classifier] Off",
                shape = AzButtonShape.RECTANGLE,
            )
        }
        AzButton(
            onClick = { onChange(state.copy(activeClassifiers = emptySet())) },
            text = "Clear all classifiers",
            shape = AzButtonShape.RECTANGLE,
        )

        Text(
            "Bonus: under \"Menu Host\" in the menu, you'll also find `azHelpSubItem(\"menu-host-help\")` — a help trigger nested under a host. Try it from the expanded menu.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Callout(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
