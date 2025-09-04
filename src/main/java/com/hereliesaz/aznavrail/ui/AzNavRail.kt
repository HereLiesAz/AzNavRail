package com.hereliesaz.aznavrail.ui

import android.content.pm.PackageManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
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
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.MenuItem
import com.hereliesaz.aznavrail.model.RailItem
import com.hereliesaz.aznavrail.model.PredefinedAction

/**
 * An expressive and highly configurable navigation rail component for Jetpack Compose.
 *
 * @param headerText The text to display in the header, typically the app name.
 * @param headerIcon The icon to display in the header.
 * @param menuItems The list of items to display in the expanded menu.
 * @param railItems The list of items to display on the collapsed rail.
 * @param modifier The modifier to be applied to the navigation rail container.
 * @param onPredefinedAction A lambda to handle clicks on items with a [PredefinedAction].
 * @param buttonContent A composable lambda to customize the appearance of the rail buttons.
 * @param initiallyExpanded Whether the rail should be expanded when it first appears.
 * @param useAppIconAsHeader If true, the rail will display the app's icon in the header.
 * @param headerIconSize The size of the header icon.
 * @param allowCyclersOnRail If true, cycle buttons will be displayed on the collapsed rail.
 * @param creditText The text for the credit line in the footer.
 * @param onCreditClicked A lambda for when the credit line is clicked.
 */
@Composable
fun AzNavRail(
    headerText: String,
    headerIcon: ImageVector?,
    menuItems: List<MenuItem>,
    railItems: List<RailItem>,
    modifier: Modifier = Modifier,
    buttonContent: @Composable (item: RailItem, state: MutableState<Any>?) -> Unit = { item, state ->
        DefaultRailButton(item = item, state = state)
    },
    initiallyExpanded: Boolean = false,
    allowCyclersOnRail: Boolean = false,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null,
    disableSwipeToOpen: Boolean = false,
    footerItems: List<MenuItem> = emptyList()
) {
    val context = LocalContext.current
    val onCreditClickedLambda = onCreditClicked ?: {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hereliesaz"))
        context.startActivity(intent)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp)) { // Use a fixed size for the icon box
                    if (headerIcon != null) {
                        Icon(imageVector = headerIcon, contentDescription = "Header Icon")
                        } else {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(8.dp))
                    Text(text = headerText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) {
        if (isExpanded) {
            NavRailMenu(
                appName = headerText,
                items = menuItems,
                onCloseDrawer = onToggle,
                itemStates = menuItemStates,
                creditText = creditText,
                onCreditClicked = onCreditClickedLambda,
                footerItems = footerItems
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                railItems.forEach { item ->
                    buttonContent(item, railItemStates[item])
                }
            }
        }
    }
}

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
