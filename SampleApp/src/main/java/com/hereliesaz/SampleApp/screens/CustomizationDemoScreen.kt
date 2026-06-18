package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzDropdownMenu
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDropdownAlignment
import com.hereliesaz.aznavrail.model.AzDropdownDesign
import com.hereliesaz.aznavrail.model.AzHeaderIconShape

/** Live theme/config state surfaced from MainApp so the rail can re-key on each change. */
data class CustomizationState(
    val headerIconShape: AzHeaderIconShape,
    val defaultShape: AzButtonShape,
    val translucentBackground: Color,
    val expandedWidth: Dp,
    val collapsedWidth: Dp,
    val displayAppName: Boolean,
    val showFooter: Boolean,
    val appRepositoryUrl: String,
    val helpLineColors: List<Color>,
    val vibrate: Boolean,
    val headerIconSize: Dp = Dp.Unspecified,
)

private val headerIconShapes = AzHeaderIconShape.values().toList()
private val defaultShapes = AzButtonShape.values().toList()
private val dropdownAlignments = AzDropdownAlignment.values().toList()
private val translucentChoices = listOf(
    "Unspecified" to Color.Unspecified,
    "Black 40%" to Color.Black.copy(alpha = 0.4f),
    "White 60%" to Color.White.copy(alpha = 0.6f),
    "Indigo 50%" to Color(0xFF3F51B5).copy(alpha = 0.5f),
)
private val helpLinePalettes = listOf(
    "Default rainbow" to emptyList<Color>(),
    "Mono" to listOf(Color(0xFF00ACC1)),
    "Warm" to listOf(Color(0xFFEF6C00), Color(0xFFD32F2F), Color(0xFFC2185B)),
)
private val repoChoices = listOf(
    "AzNavRail" to "https://github.com/HereLiesAz/AzNavRail",
    "Anthropic" to "https://github.com/anthropics",
    "JetBrains Compose" to "https://github.com/JetBrains/compose-multiplatform",
)

@Composable
fun CustomizationDemoScreen(
    state: CustomizationState,
    onChange: (CustomizationState) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Theming Customization", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Every control below feeds back into the rail's azConfig + azTheme blocks. Watch the rail recompose as you change values.",
            style = MaterialTheme.typography.bodyMedium,
        )

        SectionLabel("headerIconShape")
        AzCycler(
            options = headerIconShapes.map { it.name },
            selectedOption = state.headerIconShape.name,
            onCycle = {
                val next = headerIconShapes[(headerIconShapes.indexOf(state.headerIconShape) + 1) % headerIconShapes.size]
                onChange(state.copy(headerIconShape = next))
            },
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("defaultShape")
        AzCycler(
            options = defaultShapes.map { it.name },
            selectedOption = state.defaultShape.name,
            onCycle = {
                val next = defaultShapes[(defaultShapes.indexOf(state.defaultShape) + 1) % defaultShapes.size]
                onChange(state.copy(defaultShape = next))
            },
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("translucentBackground")
        val translucentLabel = translucentChoices.firstOrNull { it.second == state.translucentBackground }?.first ?: "Custom"
        AzCycler(
            options = translucentChoices.map { it.first },
            selectedOption = translucentLabel,
            onCycle = {
                val idx = translucentChoices.indexOfFirst { it.first == translucentLabel }
                val next = translucentChoices[(idx + 1).coerceAtLeast(0) % translucentChoices.size]
                onChange(state.copy(translucentBackground = next.second))
            },
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("expandedWidth: ${state.expandedWidth.value.toInt()} dp")
        Slider(
            value = state.expandedWidth.value,
            onValueChange = { onChange(state.copy(expandedWidth = it.dp)) },
            valueRange = 120f..320f,
        )

        SectionLabel("collapsedWidth: ${state.collapsedWidth.value.toInt()} dp")
        Slider(
            value = state.collapsedWidth.value,
            onValueChange = { onChange(state.copy(collapsedWidth = it.dp)) },
            valueRange = 60f..160f,
        )

        SectionLabel("headerIconSize: ${if (state.headerIconSize == Dp.Unspecified) "auto (rail width)" else "${state.headerIconSize.value.toInt()} dp"}")
        Slider(
            value = if (state.headerIconSize == Dp.Unspecified) 0f else state.headerIconSize.value,
            onValueChange = { onChange(state.copy(headerIconSize = if (it < 1f) Dp.Unspecified else it.dp)) },
            valueRange = 0f..120f,
        )

        SectionLabel("AzDropdownMenu — standalone hamburger; panel pins to the screen edge as rail/menu")
        AzDropdownMenuDemo()

        SectionLabel("displayAppName")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AzToggle(
                isChecked = state.displayAppName,
                onToggle = { onChange(state.copy(displayAppName = !state.displayAppName)) },
                toggleOnText = "Name",
                toggleOffText = "Icon",
                shape = AzButtonShape.RECTANGLE,
            )
        }

        SectionLabel("showFooter")
        AzToggle(
            isChecked = state.showFooter,
            onToggle = { onChange(state.copy(showFooter = !state.showFooter)) },
            toggleOnText = "Footer On",
            toggleOffText = "Footer Off",
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("appRepositoryUrl")
        val repoLabel = repoChoices.firstOrNull { it.second == state.appRepositoryUrl }?.first ?: "Custom"
        AzCycler(
            options = repoChoices.map { it.first },
            selectedOption = repoLabel,
            onCycle = {
                val idx = repoChoices.indexOfFirst { it.first == repoLabel }
                val next = repoChoices[(idx + 1).coerceAtLeast(0) % repoChoices.size]
                onChange(state.copy(appRepositoryUrl = next.second))
            },
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("helpLineColors")
        val paletteLabel = helpLinePalettes.firstOrNull { it.second == state.helpLineColors }?.first ?: "Default rainbow"
        AzCycler(
            options = helpLinePalettes.map { it.first },
            selectedOption = paletteLabel,
            onCycle = {
                val idx = helpLinePalettes.indexOfFirst { it.first == paletteLabel }
                val next = helpLinePalettes[(idx + 1).coerceAtLeast(0) % helpLinePalettes.size]
                onChange(state.copy(helpLineColors = next.second))
            },
            shape = AzButtonShape.RECTANGLE,
        )

        SectionLabel("vibrate")
        AzToggle(
            isChecked = state.vibrate,
            onToggle = { onChange(state.copy(vibrate = !state.vibrate)) },
            toggleOnText = "Haptics On",
            toggleOffText = "Haptics Off",
            shape = AzButtonShape.RECTANGLE,
        )

        AzButton(
            onClick = {
                onChange(
                    CustomizationState(
                        headerIconShape = AzHeaderIconShape.CIRCLE,
                        defaultShape = AzButtonShape.RECTANGLE,
                        translucentBackground = Color.Unspecified,
                        expandedWidth = 160.dp,
                        collapsedWidth = 100.dp,
                        displayAppName = false,
                        showFooter = true,
                        appRepositoryUrl = "https://github.com/HereLiesAz/AzNavRail",
                        helpLineColors = emptyList(),
                        vibrate = false,
                    )
                )
            },
            text = "Reset to defaults",
            shape = AzButtonShape.RECTANGLE,
        )
    }
}

/**
 * Demonstrates the standalone [AzDropdownMenu] — no rail, no host, no `azConfig`. The hamburger icon
 * is dropped inline here like an [AzButton]; its panel is an overlay pinned to the left/right screen
 * edge (per the alignment cycler) and styled as the rail or the menu (per the design cycler).
 */
@Composable
private fun AzDropdownMenuDemo() {
    var alignment by remember { mutableStateOf(AzDropdownAlignment.TOP_START) }
    var design by remember { mutableStateOf(AzDropdownDesign.MENU) }
    var dark by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("none") }

    AzCycler(
        options = dropdownAlignments.map { it.name },
        selectedOption = alignment.name,
        onCycle = { alignment = dropdownAlignments[(alignment.ordinal + 1) % dropdownAlignments.size] },
        shape = AzButtonShape.RECTANGLE,
    )

    val designs = AzDropdownDesign.values()
    AzCycler(
        options = designs.map { it.name },
        selectedOption = design.name,
        onCycle = { design = designs[(design.ordinal + 1) % designs.size] },
        shape = AzButtonShape.RECTANGLE,
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AzDropdownMenu(alignment = alignment, design = design) {
            azItem("Profile") { lastAction = "Profile" }
            azItem("Settings") { lastAction = "Settings" }
            azToggle(
                isChecked = dark,
                toggleOnText = "Dark",
                toggleOffText = "Light",
            ) { dark = it }
            azDivider()
            azItem("Sign out") { lastAction = "Sign out" }
        }
        Text("last: $lastAction", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
}
