package com.example.sampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem
import com.hereliesaz.aznavrail.ui.AppNavRail

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
    val menuItems = listOf(
        MenuItem.MenuAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = {}),
        MenuItem.MenuAction(id = "favorites", text = "Favorites", icon = Icons.Default.Favorite, onClick = {}),
        MenuItem.MenuAction(id = "settings", text = "Settings", icon = Icons.Default.Settings, onClick = {})
    )

    val railItems = listOf(
        RailItem.RailAction(id = "home", text = "Home", icon = Icons.Default.Home, onClick = {}),
        RailItem.RailAction(id = "favorites", text = "Favs", icon = Icons.Default.Favorite, onClick = {})
    )

    val footerItems = listOf(
        MenuItem.MenuAction(id = "about", text = "About", icon = Icons.Default.Info, onClick = {})
    )

    AppNavRail(
        headerText = "Sample App",
        headerIcon = null,
        menuItems = menuItems,
        railItems = railItems,
        footerItems = footerItems
    )
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
