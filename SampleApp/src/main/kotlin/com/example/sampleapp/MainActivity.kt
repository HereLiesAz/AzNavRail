// SampleApp/src/main/kotlin/com/example/sampleapp/MainActivity.kt
package com.example.sampleapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzActivity
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.annotation.*

@Az(
    app = App(dock = "LEFT"),
    advanced = Advanced(isLoadingProperty = "isLoading", infoScreen = true)
)
class MainActivity : AzActivity() {
    override val graph = AzGraph

    // Reactive state properties bound via *Property strings in annotations
    var isLoading by mutableStateOf(false)
    var wifiEnabled by mutableStateOf(true)
    var currentMode by mutableStateOf("Auto")
    val modes = listOf("Light", "Dark", "Auto")
    
    var dynamicTitle by mutableStateOf("Dynamic Item")
    var dynamicBadge by mutableStateOf("1")
    var isItemVisible by mutableStateOf(true)
    var isItemDisabled by mutableStateOf(false)

    @Az(rail = RailItem(
        textProperty = "dynamicTitle",
        iconTextProperty = "dynamicBadge",
        visibleProperty = "isItemVisible",
        disabledProperty = "isItemDisabled"
    ))
    @Composable
    fun DynamicItem() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Dynamic Content Area", style = MaterialTheme.typography.headlineMedium)
            Text("Title: $dynamicTitle")
            Text("Badge: $dynamicBadge")
        }
    }

    @Az(toggle = Toggle(isCheckedProperty = "wifiEnabled", toggleOnText = "Wi-Fi: On", toggleOffText = "Wi-Fi: Off"))
    fun WifiToggle() {}

    @Az(cycler = Cycler(optionsProperty = "modes", selectedOptionProperty = "currentMode"))
    fun ModeCycler() {}

    @Az(rail = RailItem(icon = android.R.drawable.ic_menu_edit))
    @Composable
    fun ControlPanel() {
        // This screen allows testing the reactive bindings
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Live Dictatorship Controller", style = MaterialTheme.typography.headlineSmall)
            
            AzTextBox(hint = "Change Item Title", onSubmit = { dynamicTitle = it })
            AzTextBox(hint = "Change Badge", onSubmit = { dynamicBadge = it })
            
            AzToggle(isChecked = isItemVisible, onToggle = { isItemVisible = it }, toggleOnText = "Item Visible", toggleOffText = "Item Hidden")
            AzToggle(isChecked = isItemDisabled, onToggle = { isItemDisabled = it }, toggleOnText = "Item Disabled", toggleOffText = "Item Enabled")
            AzToggle(isChecked = isLoading, onToggle = { isLoading = it }, toggleOnText = "Loading: On", toggleOffText = "Loading: Off")
        }
    }
}
