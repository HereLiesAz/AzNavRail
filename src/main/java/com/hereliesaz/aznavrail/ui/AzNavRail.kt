package com.hereliesaz.aznavrail.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.model.NavRailActionButton
import com.hereliesaz.aznavrail.model.NavRailCycleButton
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailItem
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AzNavRail(
    header: NavRailHeader,
    buttons: List<NavRailItem>,
    menuSections: List<NavRailMenuSection>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    headerIconSize: Dp = 80.dp,
    onAboutClicked: (() -> Unit)? = null,
    onFeedbackClicked: (() -> Unit)? = null,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null
) {
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
                            header.onClick()
                        }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(
                onClick = header.onClick,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            ) {
                Box(modifier = Modifier.size(headerIconSize)) {
                    header.content()
                }
            }
        }
    ) {
        if (isExpanded) {
            NavRailMenu(
                sections = menuSections,
                onCloseDrawer = header.onClick,
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
