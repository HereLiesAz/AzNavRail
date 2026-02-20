package com.hereliesaz.aznavrail.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem

@Composable
internal fun MenuItem(
    item: AzNavItem,
    navController: androidx.navigation.NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
    onCyclerClick: (() -> Unit)?,
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
    onHostClick: (() -> Unit)? = null,
    onItemGloballyPositioned: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    infoScreen: Boolean,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    // Menu item utilizes standard RailContent but can expand into a row if needed.
    RailContent(
        item = item,
        navController = navController,
        isSelected = isSelected,
        buttonSize = AzNavRailDefaults.HeaderIconSize,
        onClick = onClick,
        onRailCyclerClick = { onCyclerClick?.invoke() },
        onItemClick = onItemClick,
        onHostClick = onHostClick,
        onItemGloballyPositioned = onItemGloballyPositioned,
        infoScreen = infoScreen,
        activeColor = activeColor,
        shape = defaultShape
    )
}
