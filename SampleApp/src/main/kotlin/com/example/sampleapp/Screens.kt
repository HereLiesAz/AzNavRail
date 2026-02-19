// SampleApp/src/main/kotlin/com/example/sampleapp/Screens.kt
package com.example.sampleapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.annotation.Az
import com.hereliesaz.aznavrail.annotation.RailItem
import com.hereliesaz.aznavrail.model.AzDockingSide

@Az(rail = RailItem(home = true))
@Composable
fun Home() {
    val context = LocalContext.current
    val activity = context as? MainActivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Configuration", style = MaterialTheme.typography.headlineSmall)

        if (activity != null) {
            // Docking & Packing
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzToggle(
                    isChecked = activity.dockingSide == AzDockingSide.LEFT,
                    onToggle = { activity.dockingSide = if (activity.dockingSide == AzDockingSide.LEFT) AzDockingSide.RIGHT else AzDockingSide.LEFT },
                    toggleOnText = "Dock: Left",
                    toggleOffText = "Dock: Right"
                )
                AzToggle(
                    isChecked = activity.packButtons,
                    onToggle = { activity.packButtons = !activity.packButtons },
                    toggleOnText = "Packed: On",
                    toggleOffText = "Packed: Off"
                )
            }

            // Footer & Name & NoMenu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzToggle(
                    isChecked = activity.showFooter,
                    onToggle = { activity.showFooter = !activity.showFooter },
                    toggleOnText = "Footer: Visible",
                    toggleOffText = "Footer: Hidden"
                )
                AzToggle(
                    isChecked = activity.displayAppName,
                    onToggle = { activity.displayAppName = !activity.displayAppName },
                    toggleOnText = "Name: Visible",
                    toggleOffText = "Name: Hidden"
                )
                AzToggle(
                    isChecked = activity.noMenu,
                    onToggle = { activity.noMenu = !activity.noMenu },
                    toggleOnText = "No Menu: On",
                    toggleOffText = "No Menu: Off"
                )
            }

            // Advanced Features
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzToggle(
                    isChecked = activity.usePhysicalDocking,
                    onToggle = { activity.usePhysicalDocking = !activity.usePhysicalDocking },
                    toggleOnText = "Physical: On",
                    toggleOffText = "Physical: Off"
                )
                AzToggle(
                    isChecked = activity.infoScreen,
                    onToggle = { activity.infoScreen = !activity.infoScreen },
                    toggleOnText = "Info: On",
                    toggleOffText = "Info: Off"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AzToggle(
                    isChecked = activity.vibrate,
                    onToggle = { activity.vibrate = !activity.vibrate },
                    toggleOnText = "Vibrate: On",
                    toggleOffText = "Vibrate: Off"
                )
                AzToggle(
                    isChecked = activity.isLoading,
                    onToggle = { activity.isLoading = !activity.isLoading },
                    toggleOnText = "Loading: On",
                    toggleOffText = "Loading: Off"
                )
            }
        } else {
            Text("Activity context not found!", color = androidx.compose.ui.graphics.Color.Red)
        }

        Text("Components Showcase", style = MaterialTheme.typography.headlineSmall)

        AzButton(onClick = {}, text = "Normal Button")
        AzButton(onClick = {}, text = "Loading", isLoading = true)
        AzButton(onClick = {}, text = "Disabled", enabled = false)

        Text("Text Boxes", style = MaterialTheme.typography.titleMedium)
        AzTextBox(hint = "Normal Input", onSubmit = {})
        AzTextBox(hint = "Secret Input", secret = true, onSubmit = {})
        AzTextBox(hint = "Multiline Input", multiline = true, onSubmit = {})
        AzTextBox(hint = "Error State", isError = true, onSubmit = {})
    }
}

@Az(rail = RailItem(icon = android.R.drawable.ic_menu_preferences))
@Composable
fun Settings() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.headlineMedium)
    }
}
