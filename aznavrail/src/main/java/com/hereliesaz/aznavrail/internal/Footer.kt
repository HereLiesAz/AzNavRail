package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzNavRailScopeImpl

@Composable
internal fun Footer(
    appName: String,
    onToggle: () -> Unit,
    onUndock: () -> Unit,
    scope: AzNavRailScopeImpl,
    footerColor: Color
) {
    val context = LocalContext.current
    val overlayService = scope.overlayService

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AzDivider(
            modifier = Modifier.padding(
                horizontal = AzNavRailDefaults.FooterDividerHorizontalPadding,
                vertical = AzNavRailDefaults.FooterDividerVerticalPadding
            )
        )
        Spacer(modifier = Modifier.height(AzNavRailDefaults.FooterSpacerHeight))
        
        Text(
            text = appName,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = footerColor
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onToggle() }
        )

        // Show "Undock" if enabled
        if (scope.enableRailDragging) {
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                 text = "Undock",
                 style = MaterialTheme.typography.labelSmall,
                 color = footerColor.copy(alpha = 0.7f),
                 modifier = Modifier.clickable { onUndock() }
             )
        }
    }
}
