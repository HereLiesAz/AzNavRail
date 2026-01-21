package com.example.sampleapp

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.AzToggle
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide

@Composable
fun SampleScreen(
    enableRailDragging: Boolean = true,
    initiallyExpanded: Boolean = false,
    onUndockOverride: (() -> Unit)? = null,
    onRailDrag: ((Float, Float) -> Unit)? = null,
    showContent: Boolean = true
) {
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
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Set the global suggestion limit for all AzTextBox instances
    AzTextBoxDefaults.setSuggestionLimit(3)

    var useBasicOverlay by remember { mutableStateOf(false) }
    var isDockingRight by remember { mutableStateOf(false) }
    var noMenu by remember { mutableStateOf(false) }

    AzNavHost(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    ) {
        azSettings(
            // displayAppNameInHeader = true, // Set to true to display the app name instead of the icon
            packRailButtons = packRailButtons,
            isLoading = isLoading,
            defaultShape = AzButtonShape.RECTANGLE, // Set a default shape for all rail items
            enableRailDragging = enableRailDragging,
            onUndock = onUndockOverride,
            onRailDrag = onRailDrag,
            overlayService = if (useBasicOverlay) SampleBasicOverlayService::class.java else SampleOverlayService::class.java,
            dockingSide = if (isDockingRight) AzDockingSide.RIGHT else AzDockingSide.LEFT,
            noMenu = noMenu
        )

        // RAIL ITEMS
        azMenuItem(id = "home", text = "Home", route = "home", onClick = { Log.d(TAG, "Home menu item clicked") })
        azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line", onClick = { Log.d(TAG, "Multi-line menu item clicked") })

        azRailToggle(
            id = "pack-rail",
            isChecked = packRailButtons,
            toggleOnText = "Pack Rail",
            toggleOffText = "Unpack Rail",
            route = "pack-rail",
            onClick = {
                packRailButtons = !packRailButtons
                Log.d(TAG, "Pack rail toggled to: $packRailButtons")
            }
        )

        azRailItem(
            id = "profile",
            text = "Profile",
            shape = AzButtonShape.CIRCLE,
            disabled = true,
            route = "profile"
        )

        azDivider()

        azRailToggle(
            id = "online",
            isChecked = isOnline,
            toggleOnText = "Online",
            toggleOffText = "Offline",
            shape = AzButtonShape.SQUARE,
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
            id = "overlay-mode",
            isChecked = useBasicOverlay,
            toggleOnText = "Using Basic Overlay",
            toggleOffText = "Using Foreground Overlay",
            route = "overlay-mode",
            onClick = {
                useBasicOverlay = !useBasicOverlay
                Log.d(TAG, "Overlay mode toggled to: $useBasicOverlay")
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
            shape = null,
            onClick = {
                val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                menuSelectedOption = menuCycleOptions[nextIndex]
                Log.d(TAG, "Sub cycler clicked, new option: $menuSelectedOption")
            }
        )

        // BACKGROUNDS
        background(weight = 0) {
            Box(Modifier.fillMaxSize().background(Color(0xFFEEEEEE)))
        }

        background(weight = 10) {
             // Example overlay background
             Box(Modifier.fillMaxSize().padding(50.dp).background(Color.Blue.copy(alpha = 0.1f))) {
                 Text("Background Layer (Weight 10)", color = Color.Blue)
             }
        }

        // ONSCREEN COMPONENTS
        if (showContent) {
            onscreen(alignment = Alignment.TopStart) {
                Text("Aligned TopStart (Flips)", modifier = Modifier.padding(16.dp))
            }

            onscreen(alignment = Alignment.TopEnd) {
                Text("Aligned TopEnd (Flips)", modifier = Modifier.padding(16.dp))
            }

            onscreen(alignment = Alignment.Center) {
                Column(modifier = Modifier.padding(16.dp)) {
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

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { Text("Home Screen") }
                        composable("multi-line") { Text("Multi-line Screen") }
                        composable("menu-host") { Text("Menu Host Screen") }
                        composable("menu-sub-1") { Text("Menu Sub 1 Screen") }
                        composable("menu-sub-2") { Text("Menu Sub 2 Screen") }
                        composable("rail-host") { Text("Rail Host Screen") }
                        composable("rail-sub-1") { Text("Rail Sub 1 Screen") }
                        composable("rail-sub-2") { Text("Rail Sub 2 Screen") }
                        composable("sub-toggle") { Text("Sub Toggle Screen") }
                        composable("sub-cycler") { Text("Sub Cycler Screen") }
                        composable("pack-rail") { Text("Pack Rail Screen") }
                        composable("profile") { Text("Profile Screen") }
                        composable("online") { Text("Online Screen") }
                        composable("dark-mode") { Text("Dark Mode Screen") }
                        composable("rail-cycler") { Text("Rail Cycler Screen") }
                        composable("menu-cycler") { Text("Menu Cycler Screen") }
                        composable("loading") { Text("Loading Screen") }
                    }
                }
            }
        }
    }
}
