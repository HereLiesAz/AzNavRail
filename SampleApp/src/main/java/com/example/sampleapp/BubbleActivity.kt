package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

class BubbleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Reuse the SampleScreen, but configure it for Bubble mode
                    // We assume SampleScreen is refactored to accept these parameters.
                    // If not yet, this will fail compilation until we refactor MainActivity.kt
                    SampleScreen(
                        enableRailDragging = false, // Bubble window handles dragging
                        initiallyExpanded = true,   // Show expanded menu by default in bubble
                        onUndockOverride = { finish() } // Undock in bubble closes it? Or maybe re-docks?
                    )
                }
            }
        }
    }
}
