package com.hereliesaz.aznavrail.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.hereliesaz.aznavrail.model.NavRailItem
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * An expressive, stateful, and highly configurable navigation rail component for Jetpack Compose.
 *
 * This component provides a "drop-in" solution for a vertical navigation rail that manages its
 * own state, automatically uses the app's launcher icon, and can be expanded to a full menu drawer.
 * It includes built-in support for simple action buttons, stateful cycle buttons with cooldown
 * logic, a configurable footer, and swipe-to-collapse gestures.
 *
 * @param buttons The list of items to display in the collapsed state of the rail. This can be a
 * mix of [NavRailActionButton] and [NavRailCycleButton] items. See [NavRailItem].
 * @param menuSections The list of sections and their items to display in the expanded menu view.
 * See [NavRailMenuSection].
 * @param modifier The modifier to be applied to the navigation rail container.
 * @param headerIconSize The size of the header icon. Defaults to 80.dp.
 * @param onAboutClicked A lambda to be executed when the 'About' button in the footer is clicked.
 * If null, the button is not shown.
 * @param onFeedbackClicked A lambda to be executed when the 'Feedback' button in the footer is clicked.
 * If null, the button is not shown.
 * @param creditText The text for the credit/signature line in the footer. Defaults to "@HereLiesAz".
 * If null, the item is not shown.
 * @param onCreditClicked A lambda to be executed when the credit line is clicked.
 */
@Composable
fun AzNavRail(
    buttons: List<NavRailItem>,
    menuSections: List<NavRailMenuSection>,
    modifier: Modifier = Modifier,
    headerIconSize: Dp = 80.dp,
    onAboutClicked: (() -> Unit)? = null,
    onFeedbackClicked: (() -> Unit)? = null,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val onToggle: () -> Unit = { isExpanded = !isExpanded }
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

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
                val context = LocalContext.current
                val painter = rememberAsyncImagePainter(
                    model = context.packageManager.getApplicationIcon(context.packageName)
                )
                Box(modifier = Modifier.size(headerIconSize)) {
                    Image(
                        painter = painter,
                        contentDescription = "App Icon"
                    )
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
                            NavRailCycleButtonInternal(item = item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRailCycleButtonInternal(item: NavRailCycleButton) {
    var currentIndex by remember { mutableStateOf(item.options.indexOf(item.initialOption).coerceAtLeast(0)) }
    var isEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    var cooldownJob: Job? by remember { mutableStateOf(null) }

    val currentText = item.options.getOrNull(currentIndex) ?: ""

    NavRailButton(
        text = currentText,
        onClick = {
            cooldownJob?.cancel()

            val nextIndex = (currentIndex + 1) % item.options.size
            currentIndex = nextIndex
            item.onStateChange(item.options[nextIndex])

            isEnabled = false
            cooldownJob = coroutineScope.launch {
                delay(1000)
                isEnabled = true
            }
        },
        color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray
    )
}

