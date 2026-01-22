package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                
                // THE BUREAUCRACY: Strict Layout Usage
                AzHostActivityLayout(
                    navController = navController,
                    initiallyExpanded = false
                ) {
                    // SECTOR 1: THEME
                    azTheme(
                        activeColor = MaterialTheme.colorScheme.primary,
                        expandedWidth = 300.dp
                    )

                    // SECTOR 2: CONFIG
                    azConfig(
                        dockingSide = AzDockingSide.LEFT,
                        packButtons = false,
                        displayAppName = true
                    )
                    
                    // SECTOR 3: ADVANCED (Overlay)
                    azAdvanced(
                        overlayService = SampleOverlayService::class.java
                    )

                    // NAVIGATION ITEMS
                    azRailItem(id = "home", text = "Home", route = "home")
                    azRailItem(id = "profile", text = "Profile", route = "profile")
                    
                    // ONSCREEN CONTENT
                    onscreen {
                        AzNavHost(
                            startDestination = "home",
                            navController = navController
                        ) {
                            composable("home") {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Home Screen")
                                }
                            }
                            composable("profile") {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Profile Screen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
