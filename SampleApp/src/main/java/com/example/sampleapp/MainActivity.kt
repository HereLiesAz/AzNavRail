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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.*
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
                    val context = LocalContext.current

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

                    val startOverlay = {
                        if (Settings.canDrawOverlays(context)) {
                            val intent = Intent(context, SampleOverlayService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }

                    SampleScreen(
                        onUndockOverride = {
                            startOverlay()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SampleScreen(
    enableRailDragging: Boolean = false,
    initiallyExpanded: Boolean = false,
    onUndockOverride: (() -> Unit)? = null,
    overlayService: Class<out android.app.Service>? = null,
    onOverlayDrag: ((Float, Float) -> Unit)? = null,
    showContent: Boolean = true
) {
    val TAG = "SampleApp"
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    var isOnline by remember { mutableStateOf(true) }
    var isDarkMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var packRailButtons by remember { mutableStateOf(false) }
    var disableSwipeToOpen by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val railCycleOptions = remember { listOf("A", "B", "C", "D") }
    var railSelectedOption by remember { mutableStateOf(railCycleOptions.first()) }
    val menuCycleOptions = remember { listOf("X", "Y", "Z") }
    var menuSelectedOption by remember { mutableStateOf(menuCycleOptions.first()) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // Set the global suggestion limit for all AzTextBox instances
    AzTextBoxDefaults.setSuggestionLimit(3)

    Row(modifier = Modifier.fillMaxSize()) {
        Box {
            AzNavRail(
                navController = navController,
                currentDestination = currentDestination?.destination?.route,
                isLandscape = isLandscape,
                initiallyExpanded = initiallyExpanded,
                disableSwipeToOpen = disableSwipeToOpen
            ) {
                azSettings(
                    packRailButtons = packRailButtons,
                    isLoading = isLoading,
                    defaultShape = AzButtonShape.RECTANGLE,
                    enableRailDragging = enableRailDragging,
                    onUndock = onUndockOverride,
                    overlayService = overlayService,
                    onOverlayDrag = onOverlayDrag,
                    onItemGloballyPositioned = { id, bounds ->
                        Log.d(TAG, "Item '$id' positioned at $bounds")
                    },
                    infoScreen = showHelp,
                    onDismissInfoScreen = { showHelp = false }
                )

                azMenuItem(id = "home", text = "Home", route = "home", info = "Navigates to the home screen.", onClick = { Log.d(TAG, "Home menu item clicked") })
                azMenuItem(id = "multi-line", text = "This is a\nmulti-line item", route = "multi-line", info = "Demonstrates multi-line text support.", onClick = { Log.d(TAG, "Multi-line menu item clicked") })

                azRailToggle(
                    id = "pack-rail",
                    isChecked = packRailButtons,
                    toggleOnText = "Pack Rail",
                    toggleOffText = "Unpack Rail",
                    route = "pack-rail",
                    info = "Toggles between packed and spaced rail items.",
                    onClick = {
                        packRailButtons = !packRailButtons
                        Log.d(TAG, "Pack rail toggled to: $packRailButtons")
                    }
                )

                azRailToggle(
                    id = "swipe-toggle",
                    isChecked = !disableSwipeToOpen,
                    toggleOnText = "Swipe Open: ON",
                    toggleOffText = "Swipe Open: OFF",
                    onClick = { disableSwipeToOpen = !disableSwipeToOpen }
                )

                azRailItem(
                    id = "profile",
                    text = "Profile",
                    shape = AzButtonShape.CIRCLE,
                    disabled = true,
                    route = "profile"
                )

                azDivider()

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
                )

                azRailHostItem(
                     id = "settings",
                     text = "Settings",
                     route = "settings",
                     shape = AzButtonShape.CIRCLE
                )

                azRailSubItem(
                     id = "general",
                     hostId = "settings",
                     text = "General",
                     route = "settings/general",
                     onClick = { Log.d(TAG, "General Settings clicked") }
                )

                 azRailSubItem(
                     id = "security",
                     hostId = "settings",
                     text = "Security",
                     route = "settings/security",
                     onClick = { Log.d(TAG, "Security Settings clicked") }
                )

                azRailSubToggle(
                    id = "sub-toggle",
                    hostId = "settings",
                    isChecked = isDarkMode,
                    toggleOnText = "Sub Toggle On",
                    toggleOffText = "Sub Toggle Off",
                    shape = AzButtonShape.RECTANGLE,
                    onClick = { isDarkMode = !isDarkMode }
                )

                azRailSubCycler(
                    id = "sub-cycler",
                    hostId = "settings",
                    options = menuCycleOptions,
                    selectedOption = menuSelectedOption,
                    shape = AzButtonShape.RECTANGLE,
                    onClick = {
                         val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                        val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                        menuSelectedOption = menuCycleOptions[nextIndex]
                    }
                )

                azMenuHostItem(
                    id = "menu-host",
                    text = "Menu Host",
                    route = "menu-host"
                )

                azMenuSubToggle(
                    id = "menu-sub-toggle",
                    hostId = "menu-host",
                    isChecked = isDarkMode,
                    toggleOnText = "Menu Toggle On",
                    toggleOffText = "Menu Toggle Off",
                    onClick = { isDarkMode = !isDarkMode }
                )

                azMenuSubCycler(
                    id = "menu-sub-cycler",
                    hostId = "menu-host",
                    options = menuCycleOptions,
                    selectedOption = menuSelectedOption,
                    onClick = {
                        val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                        val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                        menuSelectedOption = menuCycleOptions[nextIndex]
                    }
                )
            }
        }

        if (showContent) {
            Surface(
                modifier = Modifier.weight(1f).fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Standalone Components", style = MaterialTheme.typography.headlineMedium)

                    Text("AzTextBox", style = MaterialTheme.typography.titleMedium)
                    AzTextBox(hint = "Standard input", onSubmit = {})
                    AzTextBox(hint = "Secret input", secret = true, onSubmit = {})
                    AzTextBox(hint = "Multiline input", multiline = true, onSubmit = {})

                    AzForm(formName = "demoForm", onSubmit = { Log.d(TAG, "Form submitted: $it") }) {
                        entry("name", "Name")
                        entry("email", "Email")
                    }

                    AzDivider()

                    Text("AzButton", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AzButton(text = "Circle", shape = AzButtonShape.CIRCLE, onClick = {})
                        AzButton(text = "Rect", shape = AzButtonShape.RECTANGLE, onClick = {})
                        AzButton(text = "Loading", isLoading = true, onClick = {})
                    }
                    AzButton(text = "Toggle Help Mode", shape = AzButtonShape.RECTANGLE, onClick = { showHelp = !showHelp })

                    AzDivider()

                    Text("AzToggle & AzCycler", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AzToggle(isChecked = isOnline, onToggle = { isOnline = !isOnline }, toggleOnText = "Online", toggleOffText = "Offline", shape = AzButtonShape.RECTANGLE)
                        AzCycler(options = listOf("1", "2", "3"), selectedOption = "1", onCycle = {}, shape = AzButtonShape.RECTANGLE)
                    }

                    AzDivider()

                    Text("AzLoad (Standalone)", style = MaterialTheme.typography.titleMedium)
                    AzLoad()

                    // Space at bottom
                    Box(Modifier.padding(32.dp))
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
