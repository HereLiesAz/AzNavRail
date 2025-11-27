package com.example.sampleapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.AzCycler
import com.hereliesaz.aznavrail.AzForm
import com.hereliesaz.aznavrail.AzNavRail
import com.hereliesaz.aznavrail.AzTextBox
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.AzToggle
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
                    SampleScreen(
                        onUndockOverride = { createBubble(this) }
                    )
                }
            }
        }
    }
}

fun createBubble(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

    val target = Intent(context, BubbleActivity::class.java)
    val bubbleIntent = PendingIntent.getActivity(context, 0, target, PendingIntent.FLAG_MUTABLE)

    val icon = IconCompat.createWithResource(context, android.R.drawable.sym_def_app_icon)
    val bubbleData = NotificationCompat.BubbleMetadata.Builder(bubbleIntent, icon)
        .setDesiredHeight(600)
        .setAutoExpandBubble(true)
        .setSuppressNotification(true)
        .build()

    val person = Person.Builder()
        .setName("NavRail")
        .setImportant(true)
        .build()

    val channelId = "bubble_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Bubbles", NotificationManager.IMPORTANCE_HIGH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            channel.setAllowBubbles(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val shortcutId = "navrail_bubble"
    val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
        .setShortLabel("NavRail")
        .setLongLabel("NavRail Bubble")
        .setIcon(icon)
        .setIntent(target.setAction(Intent.ACTION_MAIN))
        .setPerson(person)
        .build()
    ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle("NavRail Overlay")
        .setContentText("Tap to open")
        .setSmallIcon(android.R.drawable.sym_def_app_icon)
        .setBubbleMetadata(bubbleData)
        .setShortcutId(shortcutId)
        .addPerson(person)
        .setCategory(Notification.CATEGORY_STATUS)
        .setStyle(NotificationCompat.MessagingStyle(person).setConversationTitle("NavRail"))

    notificationManager.notify(1, builder.build())
}


@Composable
fun SampleScreen(
    enableRailDragging: Boolean = true,
    initiallyExpanded: Boolean = false,
    onUndockOverride: (() -> Unit)? = null
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
                    onUndock = onUndockOverride
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
                )


                // A button to demonstrate the loading state
                azRailItem(id = "loading", text = "Load", route = "loading", onClick = {
                    isLoading = !isLoading
                    Log.d(TAG, "Loading toggled to: $isLoading")
                })

                azDivider()

                azMenuHostItem(id = "menu-host", text = "Menu Host", route = "menu-host", onClick = { Log.d(TAG, "Menu host item clicked") })
                azMenuSubItem(id = "menu-sub-1", hostId = "menu-host", text = "Menu Sub 1", route = "menu-sub-1", onClick = { Log.d(TAG, "Menu sub item 1 clicked") })
                azMenuSubItem(id = "menu-sub-2", hostId = "menu-host", text = "Menu Sub 2", route = "menu-sub-2", onClick = { Log.d(TAG, "Menu sub item 2 clicked") })

                azRailHostItem(id = "rail-host", text = "Rail Host", route = "rail-host", onClick = { Log.d(TAG, "Rail host item clicked") })
                azRailSubItem(id = "rail-sub-1", hostId = "rail-host", text = "Rail Sub 1", route = "rail-sub-1", onClick = { Log.d(TAG, "Rail sub item 1 clicked") })
                azMenuSubItem(id = "rail-sub-2", hostId = "rail-host", text = "Menu Sub 2", route = "rail-sub-2", onClick = { Log.d(TAG, "Menu sub item 2 (from rail host) clicked") })

                azMenuSubToggle(
                    id = "sub-toggle",
                    hostId = "menu-host",
                    isChecked = isDarkMode,
                    toggleOnText = "Sub Toggle On",
                    toggleOffText = "Sub Toggle Off",
                    route = "sub-toggle",
                    onClick = {
                        isDarkMode = !isDarkMode
                        Log.d(TAG, "Sub toggle clicked, dark mode is now: $isDarkMode")
                    }
                )

                azRailSubCycler(
                    id = "sub-cycler",
                    hostId = "rail-host",
                    options = menuCycleOptions,
                    selectedOption = menuSelectedOption,
                    route = "sub-cycler",
                    shape = null,
                    onClick = {
                        val currentIndex = menuCycleOptions.indexOf(menuSelectedOption)
                        val nextIndex = (currentIndex + 1) % menuCycleOptions.size
                        menuSelectedOption = menuCycleOptions[nextIndex]
                        Log.d(TAG, "Sub cycler clicked, new option: $menuSelectedOption")
                    }
                )
            }
        }

        // Your app's main content goes here
        Column(modifier = Modifier.padding(16.dp)) {
            // Uncontrolled AzTextBox with history context
            AzTextBox(
                modifier = Modifier.padding(bottom = 16.dp),
                hint = "Uncontrolled (History: Search)",
                historyContext = "search_history",
                onSubmit = { text ->
                    Log.d(TAG, "Submitted text from uncontrolled AzTextBox: $text")
                },
                submitButtonContent = {
                    Text("Go")
                }
            )

            // Controlled AzTextBox with a different history context
            var controlledText by remember { mutableStateOf("") }
            AzTextBox(
                modifier = Modifier.padding(bottom = 16.dp),
                value = controlledText,
                onValueChange = { controlledText = it },
                hint = "Controlled (History: Usernames)",
                historyContext = "username_history",
                onSubmit = { text ->
                    Log.d(TAG, "Submitted text from controlled AzTextBox: $text")
                },
                submitButtonContent = {
                    Text("Go")
                }
            )

            // AzTextBox with inverted outline
            AzTextBox(
                modifier = Modifier.padding(bottom = 16.dp),
                hint = "Uncontrolled (No Outline)",
                outlined = false,
                onSubmit = { text ->
                    Log.d(TAG, "Submitted text from no-outline AzTextBox: $text")
                },
                submitButtonContent = {
                    Text("Go")
                }
            )

            AzForm(
                formName = "loginForm",
                modifier = Modifier.padding(bottom = 16.dp),
                onSubmit = { formData ->
                    Log.d(TAG, "Form submitted: $formData")
                },
                submitButtonContent = {
                    Text("Login")
                }
            ) {
                entry(entryName = "username", hint = "Username")
                entry(entryName = "password", hint = "Password", secret = true)
                entry(entryName = "bio", hint = "Biography", multiline = true)
            }

            AzForm(
                formName = "registrationForm",
                outlined = false,
                onSubmit = { formData ->
                    Log.d(TAG, "Registration Form submitted: $formData")
                },
                submitButtonContent = {
                    Text("Register")
                }
            ) {
                entry(entryName = "email", hint = "Email")
                entry(entryName = "confirm_password", hint = "Confirm Password", secret = true)
            }

            Row {
                AzButton(onClick = { Log.d(TAG, "Standalone AzButton clicked") }, text = "Button", shape = AzButtonShape.SQUARE)
                var isToggled by remember { mutableStateOf(false) }
                AzToggle(
                    isChecked = isToggled,
                    onToggle = { isToggled = !isToggled },
                    toggleOnText = "On",
                    toggleOffText = "Off",
                    shape = AzButtonShape.RECTANGLE
                )
                val cyclerOptions = remember { listOf("1", "2", "3") }
                var selectedCyclerOption by remember { mutableStateOf(cyclerOptions.first()) }
                AzCycler(
                    options = cyclerOptions,
                    selectedOption = selectedCyclerOption,
                    onCycle = {
                        val currentIndex = cyclerOptions.indexOf(selectedCyclerOption)
                        val nextIndex = (currentIndex + 1) % cyclerOptions.size
                        selectedCyclerOption = cyclerOptions[nextIndex]
                    },
                    shape = AzButtonShape.CIRCLE
                )
            }

            NavHost(navController = navController, startDestination = "home") {
                composable("home") { Text("Home Screen") }
                composable("multi-line") { Text("Multi-line Screen") }
                composable("menu-host") { Text("Menu Host Screen") }
                composable("menu-sub-1") { Text("Menu Sub 1 Screen") }
                composable("menu-sub-2") { Text("Menu Sub 2 Screen") }
                composable("rail-host") { Text("Rail Host Screen") }
                composable("rail-sub-1") { Text("Rail Sub 1 Screen") }
                composable("rail-sub-2") { Text("Rail Sub 2 Screen") }
                composable("sub-toggle") { Text("Sub Toggle Screen") }
                composable("sub-cycler") { Text("Sub Cycler Screen") }
                composable("pack-rail") { Text("Pack Rail Screen") }
                composable("profile") { Text("Profile Screen") }
                composable("online") { Text("Online Screen") }
                composable("dark-mode") { Text("Dark Mode Screen") }
                composable("rail-cycler") { Text("Rail Cycler Screen") }
                composable("menu-cycler") { Text("Menu Cycler Screen") }
                composable("loading") { Text("Loading Screen") }
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
