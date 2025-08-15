package com.hereliesaz.aznavrail.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.NavRailButton
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection

@Composable
fun AzNavRail(
    header: NavRailHeader,
    buttons: List<NavRailButton>,
    menuSections: List<NavRailMenuSection>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    NavigationRail(
        modifier = modifier.width(railWidth),
        containerColor = if (isExpanded) MaterialTheme.colorScheme.surface else Color.Transparent,
        header = {
            IconButton(
                onClick = header.onClick,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            ) {
                header.content()
            }
        }
    ) {
        if (isExpanded) {
            NavRailMenu(
                sections = menuSections,
                onCloseDrawer = header.onClick
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                buttons.forEach { button ->
                    NavRailButton(
                        onClick = button.onClick,
                        text = button.text
                    )
                }
            }
        }
    }
}
