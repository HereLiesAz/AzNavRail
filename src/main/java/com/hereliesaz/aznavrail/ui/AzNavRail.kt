package com.hereliesaz.aznavrail.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem

/**
 * The core component of the AzNavRail library.
 *
 * This component provides a highly configurable and expressive navigation rail that is compact
 * when collapsed and expands into a full menu drawer. It handles state management for
 * toggleable and cycleable items automatically.
 *
 * It is recommended to use the [AppNavRail] wrapper for a more streamlined and opinionated
 * implementation.
 *
 * @param menuItems The list of [MenuItem]s to display in the expanded menu drawer.
 * @param railItems The list of [RailItem]s to display on the collapsed navigation rail.
 * @param modifier The modifier to be applied to the `NavigationRail` container.
 * @param displayAppNameInHeader If `true`, the header will display the application's name instead of its icon.
 * @param packRailButtons If `true`, the rail buttons will be packed together at the top of the rail.
 * @param buttonContent A composable lambda that allows you to provide a completely custom appearance for the rail buttons.
 * @param initiallyExpanded Whether the rail should be expanded when it first appears.
 * @param allowCyclersOnRail If true, [RailItem.RailCycle] buttons will be displayed on the collapsed rail.
 * @param disableSwipeToOpen If `true`, the swipe-to-open gesture will be disabled.
 */
@Composable
fun AzNavRail(
    menuItems: List<MenuItem>,
    railItems: List<RailItem>,
    modifier: Modifier = Modifier,
    displayAppNameInHeader: Boolean = false,
    packRailButtons: Boolean = false,
    buttonContent: @Composable (item: RailItem, state: MutableState<Any>?) -> Unit = { item, state ->
        DefaultRailButton(item = item, state = state)
    },
    initiallyExpanded: Boolean = false,
    allowCyclersOnRail: Boolean = false,
    disableSwipeToOpen: Boolean = false
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        Log.e("AzNavRail", "Error getting app name", e)
        "App" // Fallback name
    }

    val appIcon = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        Log.e("AzNavRail", "Error getting app icon", e)
        null // Fallback to default icon
    }

    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onToggle: () -> Unit = { isExpanded = !isExpanded }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    val railItemStates = remember(railItems) {
        val states = mutableMapOf<RailItem, MutableState<Any>>()
        railItems.forEach { item ->
            when (item) {
                is RailItem.RailToggle -> states[item] = mutableStateOf(item.isChecked)
                is RailItem.RailCycle -> states[item] = mutableStateOf(item.options.indexOf(item.selectedOption))
                else -> {}
            }
        }
        states
    }

    val menuItemStates = remember(menuItems) {
        val states = mutableMapOf<MenuItem, MutableState<Any>>()
        menuItems.forEach { item ->
            when (item) {
                is MenuItem.MenuToggle -> states[item] = mutableStateOf(item.isChecked)
                is MenuItem.MenuCycle -> states[item] = mutableStateOf(item.options.indexOf(item.selectedOption))
                else -> {}
            }
        }
        states
    }

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .pointerInput(isExpanded, disableSwipeToOpen) {
                if (isExpanded) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x < -20) { onToggle() }
                    }
                } else if (!disableSwipeToOpen) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x > 20) { onToggle() }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(onClick = onToggle, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                if (displayAppNameInHeader) {
                    Text(text = if (isExpanded) appName else appName.first().toString(), style = MaterialTheme.typography.titleMedium)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp)) {
                            if (appIcon != null) {
                                Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "App Icon")
                            } else {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = appName, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    ) {
        if (isExpanded) {
            NavRailMenu(
                appName = appName,
                items = menuItems,
                onCloseDrawer = onToggle,
                itemStates = menuItemStates
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (packRailButtons) Arrangement.Top else Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                if (packRailButtons) {
                    // Compact layout: just render the rail items.
                    railItems.forEach { item ->
                        buttonContent(item, railItemStates[item])
                    }
                } else {
                    // Gapped layout: iterate through menu items to create spacers.
                    val railItemsById = railItems.associateBy { it.id }
                    menuItems.forEach { menuItem ->
                        val correspondingRailItem = railItemsById[menuItem.id]
                        if (correspondingRailItem != null) {
                            buttonContent(correspondingRailItem, railItemStates[correspondingRailItem])
                        } else {
                            // Render a spacer to maintain the gap.
                            Spacer(modifier = Modifier.height(72.dp)) // Height of a NavRailButton
                        }
                    }
                }
            }
        }
    }
}

/**
 * The default implementation for the rail button content.
 * This is a private composable that is used as the default value for the `buttonContent` parameter.
 * It takes a [RailItem] and renders the appropriate [NavRailButton] with the correct text and onClick behavior.
 */
@Composable
private fun DefaultRailButton(
    item: RailItem,
    state: MutableState<Any>?
) {
    val buttonText: String
    val onClick: () -> Unit

    when (item) {
        is RailItem.RailAction -> {
            buttonText = item.text
            onClick = item.onClick
        }
        is RailItem.RailToggle -> {
            val isChecked = state?.value as? Boolean ?: item.isChecked
            buttonText = item.text
            onClick = {
                val newState = !isChecked
                state?.value = newState
                item.onCheckedChange(newState)
            }
        }
        is RailItem.RailCycle -> {
            val currentIndex = state?.value as? Int ?: item.options.indexOf(item.selectedOption)
            buttonText = item.options.getOrNull(currentIndex) ?: ""
            onClick = {
                val nextIndex = (currentIndex + 1) % item.options.size
                state?.value = nextIndex
                item.onOptionSelected(item.options[nextIndex])
            }
        }
    }

    NavRailButton(onClick = onClick, text = buttonText)
}
