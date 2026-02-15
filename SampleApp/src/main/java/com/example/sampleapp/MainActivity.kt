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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val primaryColor = MaterialTheme.colorScheme.primary
                
                // THE BUREAUCRACY: Strict Layout Usage
                AzHostActivityLayout(
                    navController = navController,
                    initiallyExpanded = false
                ) {
                    // SECTOR 1: THEME
                    azTheme(
                        activeColor = primaryColor,
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
                    
                    // Dynamic Content Examples
                    azRailItem(id = "color_item", text = "Color", content = Color.Red, onClick = {})
                    azRailItem(id = "number_item", text = "Number", content = 42, onClick = {})

                    // Nested Rail Examples
                    azNestedRail(id = "nested_vert", text = "Vertical", alignment = AzNestedRailAlignment.VERTICAL) {
                        azRailItem(id = "nv1", text = "V1", onClick = {})
                        azRailItem(id = "nv2", text = "V2", onClick = {})
                    }

                    azNestedRail(id = "nested_horz", text = "Horizontal", alignment = AzNestedRailAlignment.HORIZONTAL) {
                        azRailItem(id = "nh1", text = "H1", onClick = {})
                        azRailItem(id = "nh2", text = "H2", onClick = {})
                        // Sub-items in horizontal expand vertically
                        azRailHostItem(id = "nh_host", text = "Host") {
                             // handled by onClick in real usage or state
                        }
                        azRailSubItem(id = "nh_sub1", hostId = "nh_host", text = "Sub1", onClick = {})
                    }

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
