package com.hereliesaz.aznavrail.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
 * implementation, but this component can be used directly for more advanced customization.
 *
 * @param headerText The text to display in the header when the rail is expanded. Typically the app name.
 * @param headerIcon The icon to display in the header. A default menu icon is used if null.
 * @param menuItems The list of [MenuItem]s to display in the expanded menu drawer. The order of this list is preserved.
 * @param railItems The list of [RailItem]s to display on the collapsed navigation rail. The order of this list is preserved.
 * @param modifier The modifier to be applied to the `NavigationRail` container. Use this for sizing, padding, etc.
 * @param buttonContent A composable lambda that allows you to provide a completely custom appearance for the rail buttons.
 *        It provides the `RailItem` and its mutable state (`null` for actions) for you to draw. The default implementation uses the [NavRailButton].
 * @param initiallyExpanded Whether the rail should be expanded when it first appears. Defaults to `false`.
 * @param allowCyclersOnRail If true, [RailItem.RailCycle] buttons will be displayed on the collapsed rail. By default, they are hidden from the rail to avoid taking up too much space. Defaults to `false`.
 * @param creditText The text for the credit line in the footer of the expanded menu. If null, no credit is shown.
 * @param onCreditClicked A lambda to be executed when the credit line is clicked. Defaults to opening the author's social media page.
 * @param disableSwipeToOpen If `true`, the swipe-to-open gesture on the collapsed rail will be disabled. Defaults to `false`. Swipe-to-close is always enabled.
 * @param footerItems A list of [MenuItem]s to be displayed in the footer of the expanded menu, below the main items and any credit line.
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
