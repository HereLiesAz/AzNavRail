// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/Footer.kt
package com.hereliesaz.aznavrail.internal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.pointerInput
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
    onSecretClick: (() -> Unit)?,
    scope: AzNavRailScopeImpl,
    footerColor: Color
) {
    val context = LocalContext.current

    // Footer items strictly on their own rows (Column) and center aligned
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (scope.advancedConfig.enableRailDragging || scope.advancedConfig.onUndock != null) {
            Text(
                text = "Undock",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = footerColor),
                modifier = Modifier
                    .clickable { onUndock() }
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scope.appRepositoryUrl))) } catch (e: Exception) {}
                }
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Feedback",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor),
            modifier = Modifier
                .clickable {
                    try {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:hereliesaz@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, appName)
                        }
                        context.startActivity(Intent.createChooser(emailIntent, "Send feedback"))
                    } catch (e: Exception) {}
                }
                .padding(vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "@HereLiesAz",
            style = MaterialTheme.typography.titleLarge.copy(color = footerColor.copy(alpha = 0.5f)),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/HereLiesAz"))) } catch (e: Exception) {}
                        },
                        onLongPress = {
                            onSecretClick?.invoke()
                        }
                    )
                }
                .padding(vertical = 4.dp)
        )
    }
}