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
import androidx.compose.ui.Modifier
import com.hereliesaz.aznavrail.AzNavRail

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
    val cycleOptions = remember { listOf("A", "B", "C") }
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }

    Row {
        AzNavRail {
            settings(
                displayAppNameInHeader = false,
                packRailButtons = false
            )

            MenuItem(id = "home", text = "Home", onClick = { /* ... */ })
            RailItem(id = "favorites", text = "Favs", onClick = { /* ... */ })

            RailToggle(
                id = "online",
                text = "Online",
                isChecked = isOnline,
                onClick = { isOnline = !isOnline }
            )

            MenuCycler(
                id = "cycler",
                text = "Cycle",
                options = cycleOptions,
                selectedOption = selectedOption,
                onClick = {
                    val currentIndex = cycleOptions.indexOf(selectedOption)
                    selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
                }
            )
        }
        Text("Main content for the app.")
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
