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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var isOnline by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val cycleOptions = remember { listOf("A", "B", "C", "D") }
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }
    val context = LocalContext.current

    Row {
        AzNavRail {
            azSettings(
                displayAppNameInHeader = true,
                packRailButtons = false,
                isLoading = isLoading,
                defaultShape = AzButtonShape.RECTANGLE // Set a default shape for all rail items
            )

            // A standard menu item
            azMenuItem(id = "home", text = "Home", onClick = { /* ... */ })

            // A rail item with the default shape (RECTANGLE)
            azRailItem(id = "favorites", text = "Favorites", onClick = { /* ... */ })

            // A disabled rail item that overrides the default shape
            azRailItem(
                id = "profile",
                text = "Profile",
                shape = AzButtonShape.CIRCLE,
                disabled = true,
                onClick = { /* This will not be triggered */ }
            )

            azDivider()

            // A toggle item with the SQUARE shape
            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                shape = AzButtonShape.SQUARE,
                onClick = { isOnline = !isOnline }
            )

            // A cycler with a disabled option
            azRailCycler(
                id = "cycler",
                options = cycleOptions,
                selectedOption = selectedOption,
                disabledOptions = listOf("C"),
                onClick = {
                    val currentIndex = cycleOptions.indexOf(selectedOption)
                    selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
                }
            )

            // A button to demonstrate the loading state
            azRailItem(id = "loading", text = "Load", onClick = { isLoading = !isLoading })
        }

        // Your app's main content goes here
        Column(modifier = Modifier.padding(16.dp)) {
            Text("isOnline: $isOnline")
            Text("selectedOption: $selectedOption")
            Text("isLoading: $isLoading")
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
