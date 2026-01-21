package com.hereliesaz.aznavrail.internal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.hereliesaz.aznavrail.AzDivider
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzNavItem

/**
 * Composable for displaying the footer in the expanded menu.
 *
 * @param appName The name of the app.
 * @param onToggle The click handler for toggling the rail's expanded
 *    state.
 */
import androidx.compose.ui.graphics.Color

@Composable
internal fun Footer(
    appName: String,
    onToggle: () -> Unit,
    onUndock: () -> Unit,
    scope: AzNavRailScopeImpl,
    footerColor: Color
) {
    // Initialize secret screens functionality
    val onSecretTrigger = SecretScreens(secLoc = scope.secLoc)

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
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'About' link.", e)
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
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'Feedback' link.", e)
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
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'Credit' link.", e)
            }
        }
    }

    Column {
        AzDivider()
        if (scope.enableRailDragging) {
            MenuItem(
                item = AzNavItem(id = "undock", text = "Undock", isRailItem = false, color = footerColor),
                navController = null,
                isSelected = false,
                onClick = onUndock,
                onCyclerClick = null,
                onToggle = {}, // This is the fix: prevent onToggle from being called
                onItemClick = {}
            )
        }
        MenuItem(
            item = AzNavItem(id = "about", text = "About", isRailItem = false, color = footerColor),
            navController = null,
            isSelected = false,
            onClick = onAboutClick,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {})
        MenuItem(
            item = AzNavItem(id = "feedback", text = "Feedback", isRailItem = false, color = footerColor),
            navController = null,
            isSelected = false,
            onClick = onFeedbackClick,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {})
        MenuItem(
            item = AzNavItem(
                id = "credit",
                text = "@HereLiesAz",
                isRailItem = false,
                color = footerColor
            ),
            navController = null,
            isSelected = false,
            onClick = onCreditClick,
            onLongClick = if (scope.secLoc.isNullOrEmpty()) null else onSecretTrigger,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {}
        )
        Spacer(modifier = Modifier.height(AzNavRailDefaults.FooterSpacerHeight))
    }
}
