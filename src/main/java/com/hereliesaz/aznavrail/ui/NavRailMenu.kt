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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction

/**
 * The content of the expanded menu drawer.
 *
 * @param sections The list of sections to display in the main scrollable area.
 * @param modifier The modifier to be applied to the drawer sheet.
 * @param onCloseDrawer A lambda to be executed when a menu item is clicked to close the drawer.
 * @param onPredefinedAction A lambda to handle clicks on items with a [PredefinedAction].
 * @param itemStates A map holding the mutable state for toggle and cycle items.
 * @param creditText The text for the credit footer item. If null, the item is hidden.
 * @param onCreditClicked A lambda for the credit footer item.
 */
@Composable
internal fun NavRailMenu(
    appName: String,
    sections: List<NavRailMenuSection>,
    modifier: Modifier = Modifier,
    onCloseDrawer: () -> Unit,
    onPredefinedAction: (PredefinedAction) -> Unit,
    itemStates: Map<NavItem, MutableState<Any>>,
    creditText: String? = null,
    onCreditClicked: (() -> Unit)? = null,
) {
    ModalDrawerSheet(
        modifier = modifier.padding(0.dp)
    ) {
        Column {
            // App Name
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            MenuDivider()

            // --- Scrolling Content ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                sections.forEachIndexed { index, section ->
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
                            item = item,
                            state = itemStates[item],
                            onPredefinedAction = onPredefinedAction,
                            onCloseDrawer = onCloseDrawer
                        )
                    }
                    if (index < sections.lastIndex) {
                        MenuDivider()
                    }
                }
            }

            // --- Fixed Footer ---
            Column {
                MenuDivider()
                if (creditText != null) {
                    val creditItem = NavItem(
                        text = creditText,
                        data = NavItemData.Action(onClick = onCreditClicked),
                        enabled = onCreditClicked != null
                    )
                    MenuItem(
                        item = creditItem,
                        state = null,
                        onPredefinedAction = {},
                        onCloseDrawer = onCloseDrawer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MenuItem(
    item: NavItem,
    state: MutableState<Any>?,
    onPredefinedAction: (PredefinedAction) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val clickHandler = {
        when (val data = item.data) {
            is NavItemData.Action -> {
                data.onClick?.invoke()
                data.predefinedAction?.let(onPredefinedAction)
            }
            is NavItemData.Toggle -> {
                val isChecked = state?.value as? Boolean ?: data.initialIsChecked
                val newState = !isChecked
                state?.value = newState
                data.onStateChange(newState)
            }
            is NavItemData.Cycle -> {
                val currentIndex = state?.value as? Int ?: data.options.indexOf(data.initialOption)
                val nextIndex = (currentIndex + 1) % data.options.size
                state?.value = nextIndex
                data.onStateChange(data.options[nextIndex])
            }
        }
        onCloseDrawer()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled, onClick = clickHandler)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
