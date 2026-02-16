package com.example.sampleapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

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
    var noMenu by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // State for items
    var wifiEnabled by remember { mutableStateOf(true) }
    val modes = listOf("Light", "Dark", "Auto")
    var currentMode by remember { mutableStateOf("Auto") }
    var subToggle by remember { mutableStateOf(false) }
    var menuToggle by remember { mutableStateOf(false) }
    val menuOptions = listOf("Option A", "Option B", "Option C")
    var currentMenuOption by remember { mutableStateOf("Option A") }

    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        initiallyExpanded = initiallyExpanded
    ) {
        // Background Layer Demo
        background(weight = -1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        azConfig(
            dockingSide = dockingSide,
            packButtons = packButtons,
            displayAppName = displayAppName,
            usePhysicalDocking = usePhysicalDocking,
            noMenu = noMenu,
            vibrate = vibrate
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
            onRailDrag = onRailDrag,
            isLoading = isLoading,
            overlayService = SampleOverlayService::class.java
        )

        // --- Standard Items ---
        azRailItem(id = "home", text = "Home", route = "home", info = "Go to home screen", onClick = {})
        azMenuItem(id = "settings", text = "Settings", route = "settings", info = "Configure app settings", onClick = {})

        // --- Dynamic Content Items ---
        // Color Item
        azRailItem(
            id = "color_item",
            text = "Red",
            content = Color.Red,
            info = "Item with Color content",
            onClick = {}
        )
        // Number Item
        azRailItem(
            id = "number_item",
            text = "42",
            content = 42,
            info = "Item with Number content",
            onClick = {}
        )
        // Image Item (using system resource)
        azRailItem(
            id = "image_item",
            text = "Camera",
            content = android.R.drawable.ic_menu_camera,
            info = "Item with Image Resource content",
            onClick = {}
        )

        // --- Toggles ---
        // Rail Toggle (Visible in collapsed rail)
        azRailToggle(
            id = "wifi",
            isChecked = wifiEnabled,
            toggleOnText = "Wi-Fi: On",
            toggleOffText = "Wi-Fi: Off",
            onClick = { wifiEnabled = !wifiEnabled },
            info = "Toggle Wi-Fi connection"
        )
        // Menu Toggle (Visible only in expanded menu)
        azMenuToggle(
            id = "menu_toggle",
            isChecked = menuToggle,
            toggleOnText = "Menu Toggle: On",
            toggleOffText = "Menu Toggle: Off",
            onClick = { menuToggle = !menuToggle },
            info = "A toggle only visible in the menu"
        )

        // --- Cyclers ---
        // Rail Cycler (Visible in collapsed rail)
        azRailCycler(
            id = "mode",
            options = modes,
            selectedOption = currentMode,
            onClick = { /* Handle mode change */ currentMode = modes[(modes.indexOf(currentMode) + 1) % modes.size] },
            info = "Cycle through display modes"
        )
        // Menu Cycler (Visible only in expanded menu)
        azMenuCycler(
            id = "menu_cycler",
            options = menuOptions,
            selectedOption = currentMenuOption,
            onClick = { currentMenuOption = menuOptions[(menuOptions.indexOf(currentMenuOption) + 1) % menuOptions.size] },
            info = "Cycle options in menu"
        )

        // --- Nested Rails ---
        // Vertical Alignment
        azNestedRail(
            id = "nested_vert",
            text = "Vertical Nest",
            alignment = AzNestedRailAlignment.VERTICAL,
            info = "Vertical nested rail"
        ) {
            azRailItem(id = "nv1", text = "V Nested 1", onClick = {})
            azRailItem(id = "nv2", text = "V Nested 2", onClick = {})
        }
        // Horizontal Alignment
        azNestedRail(
            id = "nested_horz",
            text = "Horizontal Nest",
            alignment = AzNestedRailAlignment.HORIZONTAL,
            info = "Horizontal nested rail"
        ) {
            azRailItem(id = "nh1", text = "H Nested 1", onClick = {})
            azRailItem(id = "nh2", text = "H Nested 2", onClick = {})
        }

        // --- Host with Sub-items ---
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

        // --- Relocatable Items ---
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

        // --- Divider ---
        azDivider()

        // --- Menu Host Item ---
        azMenuHostItem(id = "menu_host", text = "Menu Host", info = "Host item in menu only", onClick = {})
        azMenuSubItem(id = "menu_sub1", hostId = "menu_host", text = "Menu Sub 1", onClick = {})


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

                            // Docking & Packing
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

                            // Visuals
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

                            // Footer & Name & NoMenu
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
                                AzToggle(
                                    isChecked = noMenu,
                                    onToggle = { noMenu = !noMenu },
                                    toggleOnText = "No Menu: On",
                                    toggleOffText = "No Menu: Off"
                                )
                            }

                            // Advanced Features
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

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AzToggle(
                                    isChecked = vibrate,
                                    onToggle = { vibrate = !vibrate },
                                    toggleOnText = "Vibrate: On",
                                    toggleOffText = "Vibrate: Off"
                                )
                                AzToggle(
                                    isChecked = isLoading,
                                    onToggle = { isLoading = !isLoading },
                                    toggleOnText = "Loading: On",
                                    toggleOffText = "Loading: Off"
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
