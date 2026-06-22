package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ShowcaseEntry(val route: String, val title: String, val blurb: String)

private val entries = listOf(
    ShowcaseEntry("legacy", "Rail Configuration Demo", "Toggles, cyclers, nested rails, reloc items, backgrounds, onscreen layers — the existing playground."),
    ShowcaseEntry("bottom-sheet", "Bottom Sheets", "AzBottomSheet + AzBottomSheetInsetAware. All four detents, drag handle, scrim, horizontal swipe, live AzSheetConfig."),
    ShowcaseEntry("tutorial", "Interactive Tutorials", "AzTutorial DSL exercising every AzAdvanceCondition and AzHighlight variant, branching, checklist, media."),
    ShowcaseEntry("fab-overlay", "FAB + Overlay Service", "Drag-to-detach rail, drag callbacks, and a real AzNavRailSimpleOverlayService launched via SYSTEM_ALERT_WINDOW."),
    ShowcaseEntry("customization", "Theming Customization", "Header icon shape, default shape, translucent background, expanded/collapsed width, app name, footer, repo URL, helpLineColors, haptics."),
    ShowcaseEntry("help-system", "Help System", "screenTitle, info, classifiers, helpList, azHelpRailItem, azHelpSubItem, activeClassifiers programmatic highlighting."),
    ShowcaseEntry("forms", "Forms & Text Inputs", "AzForm with every entry parameter, AzTextBoxDefaults live controls, AzTextBox variants (autocomplete, password, clear, submit)."),
    ShowcaseEntry("hidden-menus", "Hidden Menus on Reloc Items", "azRailRelocItem with HiddenMenuScope: listItem(onClick), listItem(route), inputItem(hint, initialValue)."),
    ShowcaseEntry("standalone-widgets", "Standalone Widgets", "AzLoad, AzDivider, AzButton/Toggle/Cycler with every shape, AzRoller, EqualWidthLayout, AutoSizeText."),
)

@Composable
fun ShowcaseHomeScreen(
    onNavigate: (String) -> Unit,
    railIsExpanded: Boolean = false,
    hostExpandedStates: Map<String, Boolean> = emptyMap(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "AzNavRail Showcase",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Every screen below exercises a different slice of the library's public API. Open them in order or jump to whatever catches your eye.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Rail state (onExpandedChange)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = if (railIsExpanded) "Expanded" else "Collapsed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        listOf("menu-host" to "Menu Host", "rail-host" to "Rail Host").forEach { (id, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$label (onExpandedChange)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = if (hostExpandedStates[id] == true) "Expanded" else "Collapsed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        entries.forEach { entry ->
            ShowcaseCard(entry, onClick = { onNavigate(entry.route) })
        }
    }
}

@Composable
private fun ShowcaseCard(entry: ShowcaseEntry, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
