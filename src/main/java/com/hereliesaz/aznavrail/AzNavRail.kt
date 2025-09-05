package com.hereliesaz.aznavrail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.hereliesaz.aznavrail.ui.AzNavRailUi

@Composable
fun AzNavRail(
    content: AzNavRailScope.() -> Unit
) {
    val scope = remember(content) { AzNavRailScopeImpl().apply(content) }
    AzNavRailUi(
        navItems = scope.navItems,
        displayAppNameInHeader = scope.displayAppNameInHeader,
        packRailButtons = scope.packRailButtons
    )
}
