package com.hereliesaz.aznavrail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.MenuItem as MenuItemModel

@Composable
internal fun NavRailMenu(
    appName: String,
    items: List<MenuItemModel>,
    modifier: Modifier = Modifier,
    onCloseDrawer: () -> Unit,
    itemStates: Map<MenuItemModel, MutableState<Any>>,
    creditText: String? = null,
    onCreditClicked: (() -> Unit)? = null,
    footerItems: List<MenuItemModel> = emptyList()
) {
    ModalDrawerSheet(
        modifier = modifier.padding(0.dp)
    ) {
        Column {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
            MenuDivider()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                items.forEach { item ->
                    MenuItem(
                        item = item,
                        state = itemStates[item],
                        onCloseDrawer = onCloseDrawer
                    )
                }
            }

            Column {
                MenuDivider()
                footerItems.forEach { item ->
                    MenuItem(
                        item = item,
                        state = itemStates[item],
                        onCloseDrawer = onCloseDrawer
                    )
                }
                if (creditText != null) {
                    val creditItem = MenuItemModel.MenuAction(
                        id = "credit",
                        text = creditText,
                        icon = null, // Icon is now nullable
                        onClick = onCreditClicked ?: {}
                    )
                    MenuItem(
                        item = creditItem,
                        state = null,
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
    item: MenuItemModel,
    state: MutableState<Any>?,
    onCloseDrawer: () -> Unit
) {
    val clickHandler = {
        when (item) {
            is MenuItemModel.MenuAction -> {
                item.onClick()
            }
            is MenuItemModel.MenuToggle -> {
                val isChecked = state?.value as? Boolean ?: item.isChecked
                val newState = !isChecked
                state?.value = newState
                item.onCheckedChange(newState)
            }
            is MenuItemModel.MenuCycle -> {
                val currentIndex = state?.value as? Int ?: item.options.indexOf(item.selectedOption)
                val nextIndex = (currentIndex + 1) % item.options.size
                state?.value = nextIndex
                item.onOptionSelected(item.options[nextIndex])
            }
        }
        onCloseDrawer()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = clickHandler)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Icon(imageVector = item.icon, contentDescription = item.text)
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
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
