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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.AzRoller
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape

@Composable
fun SampleScreen(
    enableRailDragging: Boolean = true,
    initiallyExpanded: Boolean = false,
    onUndockOverride: (() -> Unit)? = null,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    showContent: Boolean = true
) {
    val navController = rememberNavController()

    // Configuration State
    var dockingSide by remember { mutableStateOf(AzDockingSide.LEFT) }
    var packButtons by remember { mutableStateOf(false) }
    var buttonShape by remember { mutableStateOf(AzButtonShape.CIRCLE) }
    var headerShape by remember { mutableStateOf(AzHeaderIconShape.CIRCLE) }
    var activeColor by remember { mutableStateOf(Color.Cyan) }
    var showFooter by remember { mutableStateOf(true) }
    var displayAppName by remember { mutableStateOf(true) }
    var usePhysicalDocking by remember { mutableStateOf(false) }
    var infoScreen by remember { mutableStateOf(false) }

    // State for items
    var wifiEnabled by remember { mutableStateOf(true) }
    val modes = listOf("Light", "Dark", "Auto")
    var currentMode by remember { mutableStateOf("Auto") }
    var subToggle by remember { mutableStateOf(false) }

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        initiallyExpanded = initiallyExpanded
    ) {
        azConfig(
            dockingSide = dockingSide,
            packButtons = packButtons,
            displayAppName = displayAppName,
            usePhysicalDocking = usePhysicalDocking
        )

        azTheme(
            defaultShape = buttonShape,
            headerIconShape = headerShape,
            activeColor = activeColor,
            showFooter = showFooter
        )

        azAdvanced(
            infoScreen = infoScreen,
            onDismissInfoScreen = { infoScreen = false },
            enableRailDragging = enableRailDragging,
            onUndock = onUndockOverride,
            onRailDrag = onRailDrag
        )

        // Standard Items
        azRailItem(id = "home", text = "Home", route = "home", info = "Go to home screen", onClick = {})
        azMenuItem(id = "settings", text = "Settings", route = "settings", info = "Configure app settings", onClick = {})

        // Toggles
        azRailToggle(
            id = "wifi",
            isChecked = wifiEnabled,
            toggleOnText = "Wi-Fi: On",
            toggleOffText = "Wi-Fi: Off",
            onClick = { wifiEnabled = !wifiEnabled },
            info = "Toggle Wi-Fi connection"
        )

        // Cyclers
        azRailCycler(
            id = "mode",
            options = modes,
            selectedOption = currentMode,
            onClick = { /* Handle mode change */ currentMode = modes[(modes.indexOf(currentMode) + 1) % modes.size] },
            info = "Cycle through display modes"
        )

        // Nested Rail
        azNestedRail(id = "nested", text = "More", info = "Access nested items") {
            azRailItem(id = "n1", text = "Nested 1", onClick = {})
            azRailItem(id = "n2", text = "Nested 2", onClick = {})
        }

        // Host with Sub-items
        azRailHostItem(id = "adv", text = "Advanced", info = "Advanced settings group", onClick = {})
        azRailSubItem(id = "sub1", hostId = "adv", text = "Sub Item 1", onClick = {}, info = "A standard sub-item")

        azRailSubToggle(
            id = "subT",
            hostId = "adv",
            isChecked = subToggle,
            toggleOnText = "Sub On",
            toggleOffText = "Sub Off",
            onClick = { subToggle = !subToggle },
            info = "A toggle inside a host"
        )

        // Relocatable Items
        azRailRelocItem(
            id = "reloc1",
            hostId = "adv",
            text = "Drag Me 1",
            info = "Long press to drag",
            onRelocate = { _, _, _ -> }
        ) {
            listItem("Action 1") {}
        }
        azRailRelocItem(
            id = "reloc2",
            hostId = "adv",
            text = "Drag Me 2",
            info = "Long press to drag",
            onRelocate = { _, _, _ -> }
        ) {
            listItem("Action 2") {}
        }

        if (showContent) {
            onscreen(Alignment.TopStart) {
                AzNavHost(startDestination = "home") {
                    composable("home") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Configuration", style = MaterialTheme.typography.headlineSmall)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AzToggle(
                                    isChecked = dockingSide == AzDockingSide.LEFT,
                                    onToggle = { dockingSide = if (dockingSide == AzDockingSide.LEFT) AzDockingSide.RIGHT else AzDockingSide.LEFT },
                                    toggleOnText = "Dock: Left",
                                    toggleOffText = "Dock: Right"
                                )
                                AzToggle(
                                    isChecked = packButtons,
                                    onToggle = { packButtons = !packButtons },
                                    toggleOnText = "Packed: On",
                                    toggleOffText = "Packed: Off"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AzRoller(
                                    options = AzButtonShape.values().map { it.name },
                                    selectedOption = buttonShape.name,
                                    onOptionSelected = { buttonShape = AzButtonShape.valueOf(it) },
                                    hint = "Button Shape"
                                )
                                AzRoller(
                                    options = AzHeaderIconShape.values().map { it.name },
                                    selectedOption = headerShape.name,
                                    onOptionSelected = { headerShape = AzHeaderIconShape.valueOf(it) },
                                    hint = "Header Shape"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AzToggle(
                                    isChecked = showFooter,
                                    onToggle = { showFooter = !showFooter },
                                    toggleOnText = "Footer: Visible",
                                    toggleOffText = "Footer: Hidden"
                                )
                                AzToggle(
                                    isChecked = displayAppName,
                                    onToggle = { displayAppName = !displayAppName },
                                    toggleOnText = "Name: Visible",
                                    toggleOffText = "Name: Hidden"
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AzToggle(
                                    isChecked = usePhysicalDocking,
                                    onToggle = { usePhysicalDocking = !usePhysicalDocking },
                                    toggleOnText = "Physical: On",
                                    toggleOffText = "Physical: Off"
                                )
                                AzToggle(
                                    isChecked = infoScreen,
                                    onToggle = { infoScreen = !infoScreen },
                                    toggleOnText = "Info: On",
                                    toggleOffText = "Info: Off"
                                )
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

                            Text("Forms", style = MaterialTheme.typography.titleMedium)
                            AzForm(
                                formName = "sampleForm",
                                onSubmit = { /* Handle submit */ }
                            ) {
                                entry(entryName = "name", hint = "Name")
                                entry(entryName = "email", hint = "Email")
                                entry(entryName = "bio", hint = "Bio", multiline = true)
                            }

                            Text("Standalone Roller", style = MaterialTheme.typography.titleMedium)
                            AzRoller(
                                options = listOf("Apple", "Banana", "Cherry"),
                                selectedOption = "Apple",
                                onOptionSelected = {},
                                hint = "Pick a fruit"
                            )
                        }
                    }

                    composable("settings") {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Settings Screen", style = MaterialTheme.typography.headlineMedium)
                            AzButton(
                                text = "Back to Home",
                                onClick = { navController.navigate("home") }
                            )
                        }
                    }
                }
            }
        }
    }
}
