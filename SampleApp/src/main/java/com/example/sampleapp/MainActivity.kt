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
import android.widget.Toast
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.AzDivider
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
    var isLoading by remember { mutableStateOf(false) }
    val cycleOptions = remember { listOf("A", "B", "C") }
    var selectedOption by remember { mutableStateOf(cycleOptions.first()) }
    val context = LocalContext.current

    Row {
        AzNavRail {
            azSettings(
                displayAppNameInHeader = false,
                packRailButtons = false,
                isLoading = isLoading
            )

            azMenuItem(id = "home", text = "Home", onClick = { /* ... */ })
            azRailItem(id = "favorites", text = "Favs", onClick = { /* ... */ })
            azRailItem(id = "long_text", text = "This is a very long text", onClick = { /* ... */ })
            azRailItem(id = "multi_line", text = "Multi\nLine", onClick = { /* ... */ })

            azRailItem(id = "loading", text = "Load", onClick = { isLoading = !isLoading })

            azRailToggle(
                id = "online",
                isChecked = isOnline,
                toggleOnText = "Online",
                toggleOffText = "Offline",
                onClick = {
                    isOnline = !isOnline
                    Toast.makeText(context, "Toggle clicked! isOnline: $isOnline", Toast.LENGTH_SHORT).show()
                }
            )

            azMenuCycler(
                id = "cycler",
                options = cycleOptions,
                selectedOption = selectedOption,
                onClick = {
                    val currentIndex = cycleOptions.indexOf(selectedOption)
                    selectedOption = cycleOptions[(currentIndex + 1) % cycleOptions.size]
                    Toast.makeText(context, "Cycler clicked! selectedOption: $selectedOption", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Column {
            Text("Horizontal Divider:")
            AzDivider()
            Text("Some content below the divider.")
            Row {
                Text("Vertical Divider:")
                AzDivider()
                Text("Some content to the right of the divider.")
            }
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
