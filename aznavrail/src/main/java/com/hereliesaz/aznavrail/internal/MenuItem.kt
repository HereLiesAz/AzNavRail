package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlinx.coroutines.delay

@Composable
internal fun MenuItem(
    item: AzNavItem,
    subItems: List<AzNavItem>,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    hostStates: Map<String, Boolean>,
    onToggleHost: (String) -> Unit,
    onItemClick: (AzNavItem) -> Unit,
    onMenuCyclerClick: (AzNavItem) -> Unit,
    infoScreen: Boolean
) {
    val isHostExpanded = hostStates[item.id] == true
    var showScreenTitle by remember { mutableStateOf(false) }

    Column {
        MenuRow(
            item = item,
            currentDestination = currentDestination,
            activeColor = activeColor,
            activeClassifiers = activeClassifiers,
            onClick = {
                if (infoScreen) return@MenuRow
                showScreenTitle = true
                if (item.isHost) {
                    onToggleHost(item.id)
                } else if (item.isCycler) {
                    onMenuCyclerClick(item)
                } else {
                    onItemClick(item)
                }
            },
            isSubItem = false,
            infoScreen = infoScreen
        )

        // STRICT RULE: No Rounded Corners. Changed shape to RectangleShape.
        if (showScreenTitle && item.screenTitle != AzNavRailDefaults.NO_TITLE) {
            val titleText = item.screenTitle ?: item.text
            if (titleText.isNotEmpty()) {
                Popup(
                    alignment = Alignment.TopEnd,
                    properties = PopupProperties(focusable = false)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, end = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = titleText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                LaunchedEffect(Unit) { delay(1500); showScreenTitle = false }
            }
        }

        if (isHostExpanded) {
            subItems.forEach { subItem ->
                MenuRow(
                    item = subItem,
                    currentDestination = currentDestination,
                    activeColor = activeColor,
                    activeClassifiers = activeClassifiers,
                    onClick = {
                        if (infoScreen) return@MenuRow
                        if (subItem.isCycler) {
                            onMenuCyclerClick(subItem)
                        } else {
                            onItemClick(subItem)
                        }
                    },
                    isSubItem = true,
                    infoScreen = infoScreen
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    item: AzNavItem,
    currentDestination: String?,
    activeColor: Color,
    activeClassifiers: Set<String>,
    onClick: () -> Unit,
    isSubItem: Boolean,
    infoScreen: Boolean
) {
    val isActiveRoute = item.route != null && currentDestination == item.route
    val isActiveClassifier = item.classifiers.any { activeClassifiers.contains(it) }
    val isActive = isActiveRoute || isActiveClassifier

    val color = item.color ?: MaterialTheme.colorScheme.onSurface
    val finalColor = if (isActive) activeColor else color
    val disabledColor = color.copy(alpha = 0.38f)

    val displayText = if (item.isToggle) {
        if (item.isChecked == true) item.toggleOnText else item.toggleOffText
    } else if (item.isCycler) {
        item.selectedOption ?: ""
    } else {
        item.text
    }

    val basePadding = if (isSubItem) 32.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.disabled && (!infoScreen || item.isHost)) { onClick() }
            .padding(horizontal = basePadding, vertical = 16.dp)
            .alpha(if (infoScreen && !item.isHost) 0.3f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (!item.disabled) finalColor else disabledColor
        )
    }
}
