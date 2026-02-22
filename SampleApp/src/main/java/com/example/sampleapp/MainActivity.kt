package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide

class MainActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                // THE BUREAUCRACY: Explicit Layout Usage (6.99 Style)
                AzHostActivityLayout(
                    navController = navController,
                    initiallyExpanded = false
                ) {
                    azSettings(
                        dockingSide = dockingSide,
                        packRailButtons = packButtons,
                        displayAppNameInHeader = displayAppName,
                        usePhysicalDocking = usePhysicalDocking,
                        noMenu = noMenu,
                        vibrate = vibrate,
                        showFooter = showFooter,
                        isLoading = isLoading,
                        infoScreen = infoScreen,
                        onDismissInfoScreen = { infoScreen = false },
                        defaultShape = AzButtonShape.CIRCLE
                    )

                    // RAIL ITEMS: Explicitly defined and bound to state
                    if (isItemVisible) {
                        azRailItem(
                            id = "dynamic_item",
                            text = dynamicTitle,
                            content = dynamicBadge,
                            disabled = isItemDisabled,
                            route = "dynamic_item"
                        )
                    }

                    azRailToggle(
                        id = "wifi_toggle",
                        isChecked = wifiEnabled,
                        toggleOnText = "Wi-Fi: On",
                        toggleOffText = "Wi-Fi: Off",
                        onClick = { wifiEnabled = !wifiEnabled }
                    )

                    azRailCycler(
                        id = "mode_cycler",
                        options = modes,
                        selectedOption = currentMode,
                        onClick = {
                            val currentIndex = modes.indexOf(currentMode)
                            currentMode = modes[(currentIndex + 1) % modes.size]
                        }
                    )

                    azRailItem(
                        id = "control_panel",
                        content = android.R.drawable.ic_menu_edit,
                        text = "Control Panel",
                        route = "control_panel"
                    )

                    // ONSCREEN CONTENT: Explicitly managed layering
                    onscreen {
                        AzNavHost(
                            startDestination = "dynamic_item",
                            navController = navController
                        ) {
                            composable("dynamic_item") {
                                DynamicItem()
                            }
                            composable("control_panel") {
                                ControlPanel()
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Content Screens ---

    @Composable
    fun DynamicItem() {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Dynamic Content Area", style = MaterialTheme.typography.headlineMedium)
            Text("Title: $dynamicTitle")
            Text("Badge: $dynamicBadge")
        }
    }

    @Composable
    fun ControlPanel() {
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
