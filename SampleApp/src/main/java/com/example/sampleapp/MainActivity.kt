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
import com.hereliesaz.aznavrail.AzNavRailScope
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.annotation.*
import com.hereliesaz.aznavrail.model.AzDockingSide

@Az(
    app = App(dock = "LEFT"),
    advanced = Advanced(isValid = true, isLoadingProperty = "isLoading", infoScreen = true)
)
class MainActivity : AzActivity() {
    override val graph = AzGraph

    // --- Configuration State (for runtime changes from Home screen) ---
    var dockingSide by mutableStateOf(AzDockingSide.LEFT)
    var packButtons by mutableStateOf(false)
    var showFooter by mutableStateOf(true)
    var displayAppName by mutableStateOf(true)
    var usePhysicalDocking by mutableStateOf(false)
    var noMenu by mutableStateOf(false)
    var vibrate by mutableStateOf(true)
    var isLoading by mutableStateOf(false)
    var infoScreen by mutableStateOf(false)


    // --- Reactive State for Live Dictatorship (v7.25) ---
    var wifiEnabled by mutableStateOf(true)
    var currentMode by mutableStateOf("Auto")
    val modes = listOf("Light", "Dark", "Auto")

    var dynamicTitle by mutableStateOf("Dynamic Item")
    var dynamicBadge by mutableStateOf("1")
    var isItemVisible by mutableStateOf(true)
    var isItemDisabled by mutableStateOf(false)

    override fun AzNavRailScope.configureRail() {
        azConfig(
            dockingSide = dockingSide,
            packButtons = packButtons,
            displayAppName = displayAppName,
            usePhysicalDocking = usePhysicalDocking,
            noMenu = noMenu,
            vibrate = vibrate,
            showFooter = showFooter
        )
        azAdvanced(
            infoScreen = infoScreen,
            onDismissInfoScreen = { infoScreen = false },
            isLoading = isLoading
        )
    }

    // --- Rail Items ---

    @Az(rail = RailItem(
        isValid = true,
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

    @Az(toggle = Toggle(isValid = true, isCheckedProperty = "wifiEnabled", toggleOnText = "Wi-Fi: On", toggleOffText = "Wi-Fi: Off"))
    fun WifiToggle() {
        // This function is empty because the KSP processor handles the state change.
        // It's required to give the annotation a target.
    }

    @Az(cycler = Cycler(isValid = true, optionsProperty = "modes", selectedOptionProperty = "currentMode"))
    fun ModeCycler() {
        // This function is empty for the same reason as WifiToggle.
    }

    @Az(rail = RailItem(isValid = true, icon = android.R.drawable.ic_menu_edit))
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