package com.example.sampleapp

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

@Composable
fun MainApp() {
    val TAG = "SampleApp"
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    var isOnline by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var packRailButtons by remember { mutableStateOf(false) }
    val railCycleOptions = remember { listOf("A", "B", "C", "D") }
    var railSelectedOption by remember { mutableStateOf(railCycleOptions.first()) }
    val menuCycleOptions = remember { listOf("X", "Y", "Z") }
    var menuSelectedOption by remember { mutableStateOf(menuCycleOptions.first()) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Set the global suggestion limit for all AzTextBox instances
    LaunchedEffect(Unit) {
        AzTextBoxDefaults.setSuggestionLimit(3)
    }

    var isDockingRight by remember { mutableStateOf(false) }
    var noMenu by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var usePhysicalDocking by remember { mutableStateOf(false) }

    val themeColor = MaterialTheme.colorScheme.primary

    // Use AzHostActivityLayout as the top-level container
    AzHostActivityLayout(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
        currentDestination = currentDestination?.destination?.route,
        isLandscape = isLandscape,
        initiallyExpanded = false
    ) {
        azConfig(
            packButtons = packRailButtons,
            dockingSide = if (isDockingRight) AzDockingSide.RIGHT else AzDockingSide.LEFT,
            noMenu = noMenu,
            usePhysicalDocking = usePhysicalDocking
        )

        azTheme(
            defaultShape = AzButtonShape.RECTANGLE,
            activeColor = themeColor
        )

        azAdvanced(
            isLoading = isLoading,
            enableRailDragging = true, // Keeps FAB mode enabled (in-app floating)
            infoScreen = showHelp,
            onDismissInfoScreen = { showHelp = false }
        )

        // RAIL ITEMS
        azMenuItem(id = "home", text = "Home", route = "home", info = "Navigate to the Home screen", onClick = { Log.d(TAG, "Home menu item clicked") })
        azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line", info = "Shows how multi-line text is handled", onClick = { Log.d(TAG, "Multi-line menu item clicked") })

        azRailToggle(
            id = "pack-rail",
            isChecked = packRailButtons,
            toggleOnText = "Pack Rail",
            toggleOffText = "Unpack Rail",
            route = "pack-rail",
            info = "Toggle to pack items together or space them out",
            onClick = {
                packRailButtons = !packRailButtons
                Log.d(TAG, "Pack rail toggled to: $packRailButtons")
            }
        )

        // Demonstrating Dynamic Content (Color)
        azRailItem(
            id = "color-item",
            text = "Color",
            content = Color.Red,
            info = "Demonstrates dynamic content with Color",
            onClick = { Log.d(TAG, "Color item clicked") }
        )

        // Demonstrating Dynamic Content (Icon as Resource)
        azRailItem(
            id = "icon-item",
            text = "Icon",
            content = android.R.drawable.ic_menu_agenda,
            info = "Demonstrates dynamic content with Resource ID",
            onClick = { Log.d(TAG, "Icon item clicked") }
        )

        azRailItem(
            id = "profile",
            text = "Profile",
            disabled = true,
            route = "profile",
            info = "User profile settings (Disabled)"
        )

        azDivider()

        azRailToggle(
            id = "online",
            isChecked = isOnline,
            toggleOnText = "Online",
            toggleOffText = "Offline",
            route = "online",
            onClick = {
                isOnline = !isOnline
                Log.d(TAG, "Online toggled to: $isOnline")
            }
        )

        azMenuToggle(
            id = "dark-mode",
            isChecked = isDarkMode,
            toggleOnText = "Dark Mode",
            toggleOffText = "Light Mode",
            route = "dark-mode",
            onClick = {
                isDarkMode = !isDarkMode
                Log.d(TAG, "Dark mode toggled to: $isDarkMode")
            }
        )

        azMenuToggle(
            id = "docking-side",
            isChecked = isDockingRight,
            toggleOnText = "Dock: Right",
            toggleOffText = "Dock: Left",
            route = "docking-side",
            onClick = {
                isDockingRight = !isDockingRight
                Log.d(TAG, "Docking side toggled to: ${if (isDockingRight) "Right" else "Left"}")
            }
        )

        azMenuToggle(
            id = "no-menu",
            isChecked = noMenu,
            toggleOnText = "No Menu: On",
            toggleOffText = "No Menu: Off",
            route = "no-menu",
            onClick = {
                noMenu = !noMenu
                Log.d(TAG, "No Menu toggled to: $noMenu")
            }
        )

        azMenuToggle(
            id = "physical-docking",
            isChecked = usePhysicalDocking,
            toggleOnText = "Physical Dock: On",
            toggleOffText = "Physical Dock: Off",
            route = "physical-docking",
            onClick = {
                usePhysicalDocking = !usePhysicalDocking
                Log.d(TAG, "Physical Docking toggled to: $usePhysicalDocking")
            }
        )

        azRailItem(
            id = "toggle-help",
            text = "Help",
            info = "Toggle help screen mode",
            onClick = { showHelp = !showHelp }
        )

        azDivider()

        azRailCycler(
            id = "rail-cycler",
            options = railCycleOptions,
            selectedOption = railSelectedOption,
            disabledOptions = listOf("C"),
            route = "rail-cycler",
            onClick = {
                val currentIndex = railCycleOptions.indexOf(railSelectedOption)
                val nextIndex = (currentIndex + 1) % railCycleOptions.size
                railSelectedOption = railCycleOptions[nextIndex]
                Log.d(TAG, "Rail cycler clicked, new option: $railSelectedOption")
            }
        )

        azMenuCycler(
            id = "menu-cycler",
            options = menuCycleOptions,
            selectedOption = menuSelectedOption,
            route = "menu-cycler",
            onClick = {
                val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                menuSelectedOption = menuCycleOptions[nextIndex]
                Log.d(TAG, "Menu cycler clicked, new option: $menuSelectedOption")
            }
        )


        azRailItem(id = "loading", text = "Load", route = "loading", onClick = {
            isLoading = !isLoading
            Log.d(TAG, "Loading toggled to: $isLoading")
        })

        azDivider()

        azMenuHostItem(id = "menu-host", text = "Menu Host", route = "menu-host", onClick = { Log.d(TAG, "Menu host item clicked") })
        azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1", route = "menu-sub-1", onClick = { Log.d(TAG, "Menu sub item 1 clicked") })
        azMenuSubItem(id = "menu-sub-2", hostId = "menu-host", text = "Menu Sub 2", route = "menu-sub-2", onClick = { Log.d(TAG, "Menu sub item 2 clicked") })

        azRailHostItem(id = "rail-host", text = "Rail Host", route = "rail-host", onClick = { Log.d(TAG, "Rail host item clicked") })
        azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1", route = "rail-sub-1", onClick = { Log.d(TAG, "Rail sub item 1 clicked") })
        azMenuSubItem(id = "rail-sub-2", hostId = "rail-host", text = "Menu Sub 2", route = "rail-sub-2", onClick = { Log.d(TAG, "Menu sub item 2 (from rail host) clicked") })

        azMenuSubToggle(
            id = "sub-toggle",
            hostId = "menu-host",
            isChecked = isDarkMode,
            toggleOnText = "Sub Toggle On",
            toggleOffText = "Sub Toggle Off",
            route = "sub-toggle",
            onClick = {
                isDarkMode = !isDarkMode
                Log.d(TAG, "Sub toggle clicked, dark mode is now: $isDarkMode")
            }
        )

        azRailSubCycler(
            id = "sub-cycler",
            hostId = "rail-host",
            options = menuCycleOptions,
            selectedOption = menuSelectedOption,
            route = "sub-cycler",
            onClick = {
                val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                menuSelectedOption = menuCycleOptions[nextIndex]
                Log.d(TAG, "Sub cycler clicked, new option: $menuSelectedOption")
            }
        )

        // Reorderable Item Demo
        azRailRelocItem(
            id = "reloc-1",
            hostId = "rail-host",
            text = "Reloc Item 1",
            onRelocate = { from, to, newOrder ->
                Log.d(TAG, "Relocated item from $from to $to. New order: $newOrder")
            }
        ) {
            // Hidden Menu
            listItem(text = "Action", onClick = { Log.d(TAG, "Reloc action clicked") })
        }

        azNestedRail(
            id = "nested-rail",
            text = "Vertical Nested",
            route = "nested-rail",
            alignment = AzNestedRailAlignment.VERTICAL
        ) {
            azRailItem(id = "nested-1", text = "Nested Item 1", route = "nested-1")
            azRailItem(id = "nested-2", text = "Nested Item 2", route = "nested-2")
        }

        azNestedRail(
            id = "nested-horizontal",
            text = "Horizontal Nested",
            route = "nested-horizontal",
            alignment = AzNestedRailAlignment.HORIZONTAL
        ) {
            azRailItem(id = "nested-h-1", text = "H-Item 1", route = "nested-h-1")
            azRailItem(id = "nested-h-2", text = "H-Item 2", route = "nested-h-2")
            azRailItem(id = "nested-h-3", text = "H-Item 3", route = "nested-h-3")
        }


        // BACKGROUNDS
        background(weight = 0) {
            Box(Modifier.fillMaxSize().background(Color(0xFFEEEEEE)))
        }

        background(weight = 10) {
            Box(Modifier.fillMaxSize().padding(50.dp).background(Color.Blue.copy(alpha = 0.1f))) {
                Text("Background Layer (Weight 10)", color = Color.Blue)
            }
        }

        // ONSCREEN COMPONENTS: Properly wrapped inside the onscreen DSL block!
        onscreen(alignment = Alignment.TopStart) {
            Text("Aligned TopStart (Flips)", modifier = Modifier.padding(16.dp))
        }

        onscreen(alignment = Alignment.TopEnd) {
            Text("Aligned TopEnd (Flips)", modifier = Modifier.padding(16.dp))
        }

        onscreen(alignment = Alignment.Center) {
            AzNavHost(startDestination = "home", navController = navController) {
                composable("home") {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        // Uncontrolled AzTextBox with history context
                        AzTextBox(
                            modifier = Modifier.padding(bottom = 16.dp),
                            hint = "Uncontrolled (History: Search)",
                            historyContext = "search_history",
                            onSubmit = { text ->
                                Log.d(TAG, "Submitted text from uncontrolled AzTextBox: $text")
                            },
                            submitButtonContent = {
                                Text("Go")
                            }
                        )

                        // Controlled AzTextBox with a different history context
                        var controlledText by remember { mutableStateOf("") }
                        AzTextBox(
                            modifier = Modifier.padding(bottom = 16.dp),
                            value = controlledText,
                            onValueChange = { controlledText = it },
                            hint = "Controlled (History: Usernames)",
                            historyContext = "username_history",
                            onSubmit = { text ->
                                Log.d(TAG, "Submitted text from controlled AzTextBox: $text")
                            },
                            submitButtonContent = {
                                Text("Go")
                            }
                        )

                        // AzTextBox with inverted outline
                        AzTextBox(
                            modifier = Modifier.padding(bottom = 16.dp),
                            hint = "Uncontrolled (No Outline)",
                            outlined = false,
                            onSubmit = { text ->
                                Log.d(TAG, "Submitted text from no-outline AzTextBox: $text")
                            },
                            submitButtonContent = {
                                Text("Go")
                            }
                        )

                        // Disabled AzTextBox
                        AzTextBox(
                            modifier = Modifier.padding(bottom = 16.dp),
                            hint = "Disabled",
                            enabled = false,
                            onSubmit = { Log.d(TAG, "Submitted disabled") }
                        )

                        AzForm(
                            formName = "loginForm",
                            modifier = Modifier.padding(bottom = 16.dp),
                            onSubmit = { formData ->
                                Log.d(TAG, "Form submitted: $formData")
                            },
                            submitButtonContent = {
                                Text("Login")
                            }
                        ) {
                            entry(entryName = "username", hint = "Username")
                            entry(entryName = "password", hint = "Password", secret = true)
                            entry(entryName = "bio", hint = "Biography", multiline = true)
                        }

                        AzForm(
                            formName = "registrationForm",
                            outlined = false,
                            onSubmit = { formData ->
                                Log.d(TAG, "Registration Form submitted: $formData")
                            },
                            submitButtonContent = {
                                Text("Register")
                            }
                        ) {
                            entry(entryName = "email", hint = "Email", enabled = false)
                            entry(entryName = "confirm_password", hint = "Confirm Password", secret = true)
                        }

                        Row {
                            var buttonLoading by remember { mutableStateOf(false) }
                            AzButton(
                                onClick = {
                                    Log.d(TAG, "Standalone AzButton clicked")
                                    buttonLoading = !buttonLoading
                                },
                                text = "Button",
                                shape = AzButtonShape.SQUARE,
                                isLoading = buttonLoading,
                                contentPadding = PaddingValues(16.dp)
                            )

                            AzButton(
                                onClick = { Log.d(TAG, "Disabled clicked") },
                                text = "Disabled",
                                enabled = false
                            )

                            var isToggled by remember { mutableStateOf(false) }
                            AzToggle(
                                isChecked = isToggled,
                                onToggle = { isToggled = !isToggled },
                                toggleOnText = "On",
                                toggleOffText = "Off",
                                shape = AzButtonShape.RECTANGLE
                            )
                            val cyclerOptions = remember { listOf("1", "2", "3") }
                            var selectedCyclerOption by remember { mutableStateOf(cyclerOptions.first()) }
                            AzCycler(
                                options = cyclerOptions,
                                selectedOption = selectedCyclerOption,
                                onCycle = {
                                    val currentIndex = cyclerOptions.indexOf(selectedCyclerOption)
                                    val nextIndex = (currentIndex + 1) % cyclerOptions.size
                                    selectedCyclerOption = cyclerOptions[nextIndex]
                                },
                                shape = AzButtonShape.CIRCLE
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AzRoller(
                            options = listOf("Cherry", "Bell", "Bar", "Seven", "Diamond"),
                            selectedOption = "Cherry",
                            onOptionSelected = { Log.d(TAG, "Roller selected: $it") },
                            hint = "Roller Select",
                            enabled = true
                        )
                    }
                }
                composable("multi-line") { ScreenContent("Multi-line Screen") }
                composable("menu-host") { ScreenContent("Menu Host Screen") }
                composable("menu-sub-1") { ScreenContent("Menu Sub 1 Screen") }
                composable("menu-sub-2") { ScreenContent("Menu Sub 2 Screen") }
                composable("rail-host") { ScreenContent("Rail Host Screen") }
                composable("rail-sub-1") { ScreenContent("Rail Sub 1 Screen") }
                composable("rail-sub-2") { ScreenContent("Rail Sub 2 Screen") }
                composable("sub-toggle") { ScreenContent("Sub Toggle Screen") }
                composable("sub-cycler") { ScreenContent("Sub Cycler Screen") }
                composable("pack-rail") { ScreenContent("Pack Rail Screen") }
                composable("profile") { ScreenContent("Profile Screen") }
                composable("online") { ScreenContent("Online Screen") }
                composable("dark-mode") { ScreenContent("Dark Mode Screen") }
                composable("rail-cycler") { ScreenContent("Rail Cycler Screen") }
                composable("menu-cycler") { ScreenContent("Menu Cycler Screen") }
                composable("loading") { ScreenContent("Loading Screen") }
                composable("physical-docking") { ScreenContent("Physical Docking Screen") }
                composable("docking-side") { ScreenContent("Docking Side Screen") }
                composable("no-menu") { ScreenContent("No Menu Screen") }
                composable("nested-rail") { ScreenContent("Nested Rail Screen") }
                composable("nested-1") { ScreenContent("Nested Item 1 Screen") }
                composable("nested-2") { ScreenContent("Nested Item 2 Screen") }
            }
        }
    }
}

@Composable
fun ScreenContent(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
