package com.hereliesaz.aznavrail.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.MenuItem as MenuItemModel

@Composable
internal fun NavRailMenu(
    appName: String,
    items: List<MenuItemModel>,
    modifier: Modifier = Modifier,
    onCloseDrawer: () -> Unit,
    itemStates: Map<MenuItemModel, MutableState<Any>>
) {
    val context = LocalContext.current

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

            // Non-negotiable footer
            Column {
                MenuDivider()

                val aboutItem = MenuItemModel.MenuAction(id = "about", text = "About", icon = Icons.Default.Info, onClick = {})
                val feedbackItem = MenuItemModel.MenuAction(id = "feedback", text = "Feedback", icon = Icons.Default.Mail, onClick = {})
                val creditItem = MenuItemModel.MenuAction(
                    id = "credit",
                    text = "@HereLiesAz",
                    icon = Icons.Default.Face,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hereliesaz"))
                        context.startActivity(intent)
                    }
                )

                MenuItem(item = aboutItem, state = null, onCloseDrawer = onCloseDrawer)
                MenuItem(item = feedbackItem, state = null, onCloseDrawer = onCloseDrawer)
                MenuItem(item = creditItem, state = null, onCloseDrawer = onCloseDrawer)

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
        val icon = item.icon
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = item.text)
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
