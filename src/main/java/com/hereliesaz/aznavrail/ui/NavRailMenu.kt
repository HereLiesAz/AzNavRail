package com.hereliesaz.aznavrail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.NavRailMenuSection

/**
 * The content of the expanded menu drawer.
 *
 * @param sections The list of sections to display in the main scrollable area.
 * @param modifier The modifier to be applied to the drawer sheet.
 * @param onCloseDrawer A lambda to be executed when a menu item is clicked, typically to close the drawer.
 * @param onAboutClicked A lambda for the 'About' footer item. If null, the item is hidden.
 * @param onFeedbackClicked A lambda for the 'Feedback' footer item. If null, the item is hidden.
 * @param creditText The text for the credit footer item. If null, the item is hidden.
 * @param onCreditClicked A lambda for the credit footer item.
 */
@Composable
internal fun NavRailMenu(
    sections: List<NavRailMenuSection>,
    modifier: Modifier = Modifier,
    onCloseDrawer: () -> Unit,
    onAboutClicked: (() -> Unit)? = null,
    onFeedbackClicked: (() -> Unit)? = null,
    creditText: String? = null,
    onCreditClicked: (() -> Unit)? = null,
) {
    ModalDrawerSheet(
        modifier = modifier.padding(0.dp)
    ) {
        // Main container for scroll + fixed footer
        Column {
            // --- Scrolling Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                sections.forEach { section ->
                    if (section.title.isNotEmpty()) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }
                    section.items.forEach { item ->
                        MenuItem(
                            text = item.text,
                            onClick = { item.onClick(); onCloseDrawer() },
                            enabled = item.enabled
                        )
                    }
                    MenuDivider()
                }
            }

            // --- Fixed Footer ---
            Column {
                MenuDivider()
                if (onAboutClicked != null) {
                    MenuItem(text = "About", onClick = { onAboutClicked(); onCloseDrawer() }, enabled = true)
                }
                if (onFeedbackClicked != null) {
                    MenuItem(text = "Feedback", onClick = { onFeedbackClicked(); onCloseDrawer() }, enabled = true)
                }
                if (creditText != null) {
                    MenuItem(text = creditText, onClick = { onCreditClicked?.invoke(); onCloseDrawer() }, enabled = onCreditClicked != null)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        thickness = DividerDefaults.Thickness,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}
