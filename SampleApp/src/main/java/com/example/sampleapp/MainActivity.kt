package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzHostActivityLayout
import com.hereliesaz.aznavrail.AzNavHost
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzButtonShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val themeColor = MaterialTheme.colorScheme.primary

            AzHostActivityLayout(navController = navController) {

                azConfig(
                    dockingSide = AzDockingSide.LEFT,
                    packButtons = false,
                    displayAppName = true
                )

                azTheme(
                    activeColor = themeColor,
                    defaultShape = AzButtonShape.CIRCLE
                )

                azRailItem(
                    id = "home",
                    text = "Home",
                    route = "home",
                    content = Icons.Default.Home
                )

                azRailItem(
                    id = "settings",
                    text = "Settings",
                    route = "settings",
                    content = Icons.Default.Settings
                )

                azMenuItem(
                    id = "info",
                    text = "Info",
                    route = "info"
                )

                azMenuToggle(
                    id = "toggle",
                    isChecked = false,
                    toggleOnText = "On",
                    toggleOffText = "Off",
                    onClick = { /* Toggle logic */ }
                )

                azNestedRail(
                    id = "nested",
                    text = "More",
                    content = Icons.Default.Info
                ) {
                    azRailItem(id = "sub1", text = "Sub 1", route = "sub1")
                    azRailItem(id = "sub2", text = "Sub 2", route = "sub2")
                }

                onscreen {
                    AzNavHost(
                        startDestination = "home",
                        navController = navController
                    ) {
                        composable("home") { SampleScreen("Home") }
                        composable("settings") { SampleScreen("Settings") }
                        composable("info") { SampleScreen("Info") }
                        composable("sub1") { SampleScreen("Sub 1") }
                        composable("sub2") { SampleScreen("Sub 2") }
                    }
                }
            }
        }
    }
}
