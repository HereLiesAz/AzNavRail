package com.hereliesaz.aznavrail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.NavRailActionButton
import com.hereliesaz.aznavrail.model.NavRailCycleButton
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailItem
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import kotlinx.coroutines.delay

/**
 * An expressive and highly configurable navigation rail component for Jetpack Compose.
 *
 * This component provides a vertical navigation rail that can be expanded to a full menu drawer.
 * It includes built-in support for simple action buttons, stateful cycle buttons with cooldown
 * logic, and a configurable footer. It uses [ModalNavigationDrawer] to provide a material-compliant
 * drawer experience.
 *
 * @param header The configuration for the header of the navigation rail. See [NavRailHeader].
 * If `useAppIconAsHeader` is true, this is ignored.
 * @param buttons The list of items to display in the collapsed state of the rail. This can be a
 * mix of [NavRailActionButton] and [NavRailCycleButton] items. See [NavRailItem].
 * @param menuSections The list of sections and their items to display in the expanded menu view.
 * See [NavRailMenuSection].
 * @param isExpanded Whether the navigation rail is currently in its expanded (menu) state.
 * @param onExpandedChange A callback invoked when the rail's expansion state should change.
 * @param modifier The modifier to be applied to the navigation rail container.
 * @param useAppIconAsHeader If true, the rail will attempt to display the app's launcher icon
 * in the header. If it fails, it will display a fallback menu icon.
 * @param headerIconSize The size of the header icon. Defaults to 80.dp.
 * @param onAboutClicked A lambda to be executed when the 'About' button in the footer is clicked.
 * If null, the button is not shown.
 * @param onFeedbackClicked A lambda to be executed when the 'Feedback' button in the footer is clicked.
 * If null, the button is not shown.
 * @param creditText The text for the credit/signature line in the footer. Defaults to "@HereLiesAz".
 * If null, the item is not shown.
 * @param onCreditClicked A lambda to be executed when the credit line is clicked.
 * @param content The main screen content to be displayed next to the navigation rail.
 */
@Composable
fun AzNavRail(
    header: NavRailHeader,
    buttons: List<NavRailItem>,
    menuSections: List<NavRailMenuSection>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    useAppIconAsHeader: Boolean = false,
    headerIconSize: Dp = 80.dp,
    onAboutClicked: (() -> Unit)? = null,
    onFeedbackClicked: (() -> Unit)? = null,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(if (isExpanded) DrawerValue.Open else DrawerValue.Closed)

    LaunchedEffect(isExpanded) {
        if (isExpanded) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != isExpanded) {
            onExpandedChange(drawerState.isOpen)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavRailMenu(
                sections = menuSections,
                onCloseDrawer = { onExpandedChange(false) },
                onAboutClicked = onAboutClicked,
                onFeedbackClicked = onFeedbackClicked,
                creditText = creditText,
                onCreditClicked = onCreditClicked
            )
        }
    ) {
        Row {
            NavigationRail(
                modifier = modifier.width(80.dp),
                containerColor = Color.Transparent,
                header = {
                    IconButton(
                        onClick = { onExpandedChange(!isExpanded) },
                        modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                    ) {
                        Box(modifier = Modifier.size(headerIconSize)) {
                            if (useAppIconAsHeader) {
                                val context = LocalContext.current
                                val iconDrawable = try {
                                    context.packageManager.getApplicationIcon(context.packageName)
                                } catch (e: Exception) {
                                    null
                                }

                                if (iconDrawable != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = iconDrawable),
                                        contentDescription = "App Icon"
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Menu"
                                    )
                                }
                            } else {
                                header.content()
                            }
                        }
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    buttons.forEach { item ->
                        when (item) {
                            is NavRailActionButton -> {
                                NavRailButton(
                                    onClick = item.onClick,
                                    text = item.text
                                )
                            }
                            is NavRailCycleButton -> {
                                NavRailCycleButtonInternal(item = item)
                            }
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun NavRailCycleButtonInternal(item: NavRailCycleButton) {
    var currentIndex by remember { mutableStateOf(item.options.indexOf(item.initialOption)) }
    var isEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(currentIndex) {
        if (!isEnabled) {
            delay(1000)
            isEnabled = true
        }
    }

    val currentText = item.options.getOrNull(currentIndex) ?: ""

    NavRailButton(
        text = currentText,
        onClick = {
            isEnabled = false

            val nextIndex = (currentIndex + 1) % item.options.size
            currentIndex = nextIndex
            item.onStateChange(item.options[nextIndex])
        },
        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
}

