package com.hereliesaz.SampleApp.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.SampleApp.SampleOverlayService
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape

/**
 * Snapshot of the rail-drag callbacks so the screen can render a debug panel showing what the rail
 * is reporting. State lives in MainApp so the rail's azAdvanced(...) configuration can read it; this
 * screen merely renders the snapshot and exposes overlay-service controls.
 */
data class FabOverlayState(
    val railDragEnabled: Boolean,
    val railLog: String,
    val overlayDragLog: String,
    val undockedCount: Int,
)

@Composable
fun FabOverlayDemoScreen(
    state: FabOverlayState,
    onToggleRailDrag: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var lastAction by remember { mutableStateOf("(none)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("FAB Mode + Overlay Service", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Enabling rail dragging lets the user detach the rail and float it inside the app. Starting the overlay service promotes the rail to a real system overlay window via SYSTEM_ALERT_WINDOW.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text("enableRailDragging: ${state.railDragEnabled}")
        AzToggle(
            isChecked = state.railDragEnabled,
            onToggle = { onToggleRailDrag(!state.railDragEnabled) },
            toggleOnText = "Drag: On",
            toggleOffText = "Drag: Off",
            shape = AzButtonShape.RECTANGLE,
        )

        Text("Rail drag log", fontWeight = FontWeight.SemiBold)
        Text(state.railLog, style = MaterialTheme.typography.bodySmall)
        Text("Overlay drag log", fontWeight = FontWeight.SemiBold)
        Text(state.overlayDragLog, style = MaterialTheme.typography.bodySmall)
        Text("Undock callbacks: ${state.undockedCount}")

        Text("System overlay", fontWeight = FontWeight.SemiBold)
        Text(
            "Tapping 'Start overlay' requests SYSTEM_ALERT_WINDOW if needed, then starts SampleOverlayService.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("Last action: $lastAction", style = MaterialTheme.typography.bodySmall)

        AzButton(
            onClick = {
                lastAction = if (canDrawOverlays(context)) {
                    context.startService(Intent(context, SampleOverlayService::class.java))
                    "Started SampleOverlayService"
                } else {
                    requestOverlayPermission(context)
                    "Requested SYSTEM_ALERT_WINDOW permission"
                }
            },
            text = "Start overlay",
            shape = AzButtonShape.RECTANGLE,
        )
        AzButton(
            onClick = {
                context.stopService(Intent(context, SampleOverlayService::class.java))
                lastAction = "Stopped SampleOverlayService"
            },
            text = "Stop overlay",
            shape = AzButtonShape.RECTANGLE,
        )
    }
}

private fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
