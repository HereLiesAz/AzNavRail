package com.hereliesaz.aznavrail.internal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (scope.enableRailDragging || scope.onUndock != null) {
            Text(
                text = "Undock",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = footerColor),
                modifier = Modifier
                    .clickable { onUndock() }
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier.clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HereLiesAz/AzNavRail"))) } catch (e: Exception) {}
                }
            )
            Text(
                text = "Feedback",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier.clickable {
                    try { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hereliesaz@gmail.com"))) } catch (e: Exception) {}
                }
            )
            Text(
                text = "Credit",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier.clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/hereliesaz"))) } catch (e: Exception) {}
                }
            )
        }
    }
}
