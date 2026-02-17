package com.hereliesaz.aznavrail.internal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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

    val onAboutClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/HereLiesAz/$appName".toUri()
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'About' link.", e)
            }
        }
    }

    val onFeedbackClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("hereliesaz@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for $appName")
                }
                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'Feedback' link.", e)
            }
        }
    }

    val onCreditClick: () -> Unit = remember(context) {
        {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.instagram.com/hereliesaz")
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Log.e("AzNavRail.Footer", "Could not open 'Credit' link.", e)
            }
        }
    }

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

        if (scope.enableRailDragging) {
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                 text = "Undock",
                 style = MaterialTheme.typography.labelSmall,
                 color = footerColor.copy(alpha = 0.7f),
                 modifier = Modifier.clickable { onUndock() }
             )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val linkStyle = MaterialTheme.typography.labelSmall.copy(color = footerColor.copy(alpha = 0.6f))

            Text(
                text = "About",
                style = linkStyle,
                modifier = Modifier.clickable { onAboutClick() }
            )
            Text(
                text = "Feedback",
                style = linkStyle,
                modifier = Modifier.clickable { onFeedbackClick() }
            )
            Text(
                text = "Credit",
                style = linkStyle,
                modifier = Modifier.clickable { onCreditClick() }
            )
        }
    }
}
