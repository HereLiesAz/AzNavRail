package com.hereliesaz.aznavrail.ui

import android.content.pm.PackageManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
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
 * This component provides a self-contained, stateful navigation rail that can be expanded
 * to a menu drawer. It includes built-in support for action buttons, cycle buttons,
 * a configurable footer, and swipe-to-collapse gestures.
 *
 * @param header The configuration for the header of the navigation rail. See [NavRailHeader].
 * @param buttons The list of items to display in the collapsed state of the rail.
 * @param menuSections The list of sections and items to display in the expanded menu view.
 * @param modifier The modifier to be applied to the navigation rail container.
 * @param initiallyExpanded Whether the rail should be expanded when it first appears.
 * @param useAppIconAsHeader If true, the rail will display the app's icon in the header.
 * @param headerIconSize The size of the header icon.
 * @param onAboutClicked A lambda for the 'About' button in the footer.
 * @param onFeedbackClicked A lambda for the 'Feedback' button in the footer.
 * @param creditText The text for the credit line in the footer.
 * @param onCreditClicked A lambda for when the credit line is clicked.
 */
@Composable
fun AzNavRail(
    header: NavRailHeader,
    buttons: List<NavRailItem>,
    menuSections: List<NavRailMenuSection>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    useAppIconAsHeader: Boolean = false,
    headerIconSize: Dp = 80.dp,
    onAboutClicked: (() -> Unit)? = null,
    onFeedbackClicked: (() -> Unit)? = null,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    val onToggle: () -> Unit = { isExpanded = !isExpanded }

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .pointerInput(isExpanded) {
                if (isExpanded) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x < -20) { // Threshold for a left swipe
                            onToggle()
                        }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            ) {
                Box(modifier = Modifier.size(headerIconSize)) {
                    if (useAppIconAsHeader) {
                        val context = LocalContext.current
                        val iconDrawable = try {
                            context.packageManager.getApplicationIcon(context.packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
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
        if (isExpanded) {
            NavRailMenu(
                sections = menuSections,
                onCloseDrawer = onToggle,
                onAboutClicked = onAboutClicked,
                onFeedbackClicked = onFeedbackClicked,
                creditText = creditText,
                onCreditClicked = onCreditClicked
            )
        } else {
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
                            NavRailCycleButtonInternal(item = item, isExpanded = isExpanded)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRailCycleButtonInternal(item: NavRailCycleButton, isExpanded: Boolean) {
    var currentIndex by remember { mutableStateOf(item.options.indexOf(item.initialOption)) }
    var isEnabled by remember { mutableStateOf(true) }

    if (!isEnabled) {
        LaunchedEffect(Unit) {
            delay(1000)
            isEnabled = true
        }
    }

    // When the nav rail is collapsed, the button should be enabled.
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            isEnabled = true
        }
    }

    val currentText = item.options.getOrNull(currentIndex) ?: ""

    NavRailButton(
        text = currentText,
        onClick = {
            if (isEnabled) {
                isEnabled = false
                val nextIndex = (currentIndex + 1) % item.options.size
                currentIndex = nextIndex
                item.onStateChange(item.options[nextIndex])
            }
        },
        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    )
}
