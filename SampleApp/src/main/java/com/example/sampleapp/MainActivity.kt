package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.model.AzButtonShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
fun SampleScreen() {
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

    Row {
        AzNavRail(
            navController = navController,
            currentDestination = currentDestination?.destination?.route,
            isLandscape = isLandscape
        ) {
            azSettings(
                // displayAppNameInHeader = true, // Set to true to display the app name instead of the icon
                packRailButtons = packRailButtons,
                isLoading = isLoading,
                defaultShape = AzButtonShape.RECTANGLE, // Set a default shape for all rail items
                enableRailDragging = true
            )

            // A standard menu item - only appears in the expanded menu
            azMenuItem(id = "home", text = "Home", route = "home")

            // A menu item with multi-line text
            azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line")

            // A rail toggle item with the default shape (RECTANGLE)
            azRailToggle(
                id = "pack-rail",
                isChecked = packRailButtons,
                toggleOnText = "Pack Rail",
                toggleOffText = "Unpack Rail",
                route = "pack-rail",
                onClick = { packRailButtons = !packRailButtons }
            )

            // A disabled rail item that overrides the default shape
            azRailItem(
                id = "profile",
                text = "Profile",
                shape = AzButtonShape.CIRCLE,
                disabled = true,
                route = "profile"
            )

            azDivider()

            // A rail toggle item with the SQUARE shape
            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                shape = AzButtonShape.SQUARE,
                route = "online",
                onClick = { isOnline = !isOnline }
            )

            // A menu toggle item
            azMenuToggle(
                id = "dark-mode",
                isChecked = isDarkMode,
                toggleOnText = "Dark Mode",
                toggleOffText = "Light Mode",
                route = "dark-mode",
                onClick = { isDarkMode = !isDarkMode }
            )

            azDivider()

            // A rail cycler with a disabled option
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
                }
            )

            // A menu cycler
            azMenuCycler(
                id = "menu-cycler",
                options = menuCycleOptions,
                selectedOption = menuSelectedOption,
                route = "menu-cycler",
                onClick = {
                    val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                    val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                    menuSelectedOption = menuCycleOptions[nextIndex]
                }
            )


            // A button to demonstrate the loading state
            azRailItem(id = "loading", text = "Load", route = "loading", onClick = { isLoading = !isLoading })

            azDivider()

            azMenuHostItem(id = "menu-host", text = "Menu Host", route = "menu-host")
            azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1", route = "menu-sub-1")
            azMenuSubItem(id = "menu-sub-2", hostId = "menu-host", text = "Menu Sub 2", route = "menu-sub-2")

            azRailHostItem(id = "rail-host", text = "Rail Host", route = "rail-host")
            azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1", route = "rail-sub-1")
            azMenuSubItem(id = "rail-sub-2", hostId = "rail-host", text = "Menu Sub 2", route = "rail-sub-2")

            azMenuSubToggle(
                id = "sub-toggle",
                hostId = "menu-host",
                isChecked = isDarkMode,
                toggleOnText = "Sub Toggle On",
                toggleOffText = "Sub Toggle Off",
                route = "sub-toggle",
                onClick = { isDarkMode = !isDarkMode }
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
                }
            )
        }

        // Your app's main content goes here
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

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
