package com.example.sampleapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    // Logic to request overlay permission if needed when undocking
                    val startOverlay = {
                        if (Settings.canDrawOverlays(context)) {
                            val intent = Intent(context, OverlayService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    SampleScreen(
                        overlayService = SampleOverlayService::class.java
                    )
                }
            }
        }
    }
}

@Composable
fun SampleScreen(
    enableRailDragging: Boolean = true,
    initiallyExpanded: Boolean = false,
    onUndockOverride: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null
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

    // Request Notification Permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Set the global suggestion limit for all AzTextBox instances
    AzTextBoxDefaults.setSuggestionLimit(3)

    Row {
        Box {
            AzNavRail(
                navController = navController,
                currentDestination = currentDestination?.destination?.route,
                isLandscape = isLandscape,
                initiallyExpanded = initiallyExpanded
            ) {
                azSettings(
                    // displayAppNameInHeader = true, // Set to true to display the app name instead of the icon
                    packRailButtons = packRailButtons,
                    isLoading = isLoading,
                    defaultShape = AzButtonShape.RECTANGLE, // Set a default shape for all rail items
                    enableRailDragging = enableRailDragging,
                    onUndock = onUndockOverride,
                    overlayService = overlayService,
                    onOverlayDrag = onOverlayDrag
                )

                // A standard menu item - only appears in the expanded menu
                azMenuItem(id = "home", text = "Home", route = "home", onClick = { Log.d(TAG, "Home menu item clicked") })

                // A menu item with multi-line text
                azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line", onClick = { Log.d(TAG, "Multi-line menu item clicked") })

                // A rail toggle item with the default shape (RECTANGLE)
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
                    onClick = {
                        isOnline = !isOnline
                        Log.d(TAG, "Online toggled to: $isOnline")
                    }
                )

                // A menu toggle item
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
                        Log.d(TAG, "Rail cycler clicked, new option: $railSelectedOption")
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
                        Log.d(TAG, "Menu cycler clicked, new option: $menuSelectedOption")
                    }

                    // Request Notification Permission for Android 13+
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                         Log.d("MainActivity", "Notification permission granted: $isGranted")
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    SampleScreen(
                        onUndockOverride = {
                            startOverlay()
                            // finish() // Optional: close activity when undocked?
                        }
                    )
                }
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
