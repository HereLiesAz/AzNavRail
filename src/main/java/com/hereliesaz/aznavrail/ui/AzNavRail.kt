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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.NavItem
import com.hereliesaz.aznavrail.model.NavItemData
import com.hereliesaz.aznavrail.model.NavRailHeader
import com.hereliesaz.aznavrail.model.NavRailMenuSection
import com.hereliesaz.aznavrail.model.PredefinedAction

/**
 * An expressive and highly configurable navigation rail component for Jetpack Compose.
 *
 * @param header The configuration for the header of the navigation rail.
 * @param menuSections The list of sections and items that define the navigation structure.
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
    header: NavRailHeader,
    menuSections: List<NavRailMenuSection>,
    modifier: Modifier = Modifier,
    onPredefinedAction: (PredefinedAction) -> Unit,
    buttonContent: @Composable (item: NavItem, state: MutableState<Any>?) -> Unit = { item, state ->
        DefaultRailButton(item = item, state = state, onPredefinedAction = onPredefinedAction)
    },
    initiallyExpanded: Boolean = false,
    useAppIconAsHeader: Boolean = false,
    headerIconSize: Dp = 80.dp,
    allowCyclersOnRail: Boolean = false,
    creditText: String? = "@HereLiesAz",
    onCreditClicked: (() -> Unit)? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onToggle: () -> Unit = { isExpanded = !isExpanded }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) 260.dp else 80.dp,
        label = "railWidth"
    )

    val itemStates = remember(menuSections) {
        val states = mutableMapOf<NavItem, MutableState<Any>>()
        menuSections.flatMap { it.items }.forEach { item ->
            when (val data = item.data) {
                is NavItemData.Toggle -> states[item] = mutableStateOf(data.initialIsChecked)
                is NavItemData.Cycle -> states[item] = mutableStateOf(data.options.indexOf(data.initialOption))
                else -> {}
            }
        }
        states
    }

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .pointerInput(isExpanded) {
                if (isExpanded) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, _) = dragAmount
                        if (x < -20) { onToggle() }
                    }
                }
            },
        containerColor = Color.Transparent,
        header = {
            IconButton(onClick = onToggle, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                Box(modifier = Modifier.size(headerIconSize)) {
                    if (useAppIconAsHeader) {
                        val context = LocalContext.current
                        val iconDrawable = try {
                            context.packageManager.getApplicationIcon(context.packageName)
                        } catch (e: PackageManager.NameNotFoundException) { null }
                        if (iconDrawable != null) {
                            Image(painter = rememberAsyncImagePainter(model = iconDrawable), contentDescription = "App Icon")
                        } else {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
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
                onPredefinedAction = onPredefinedAction,
                itemStates = itemStates,
                creditText = creditText,
                onCreditClicked = onCreditClicked
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                menuSections.flatMap { it.items }.forEach { item ->
                    val shouldShowOnRail = item.showOnRail && item.enabled && (allowCyclersOnRail || item.data !is NavItemData.Cycle)
                    if (shouldShowOnRail) {
                        buttonContent(item, itemStates[item])
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultRailButton(
    item: NavItem,
    state: MutableState<Any>?,
    onPredefinedAction: (PredefinedAction) -> Unit
) {
    val buttonText: String
    val onClick: () -> Unit

    when (val data = item.data) {
        is NavItemData.Action -> {
            buttonText = item.railButtonText ?: item.text
            onClick = {
                data.onClick?.invoke()
                data.predefinedAction?.let(onPredefinedAction)
            }
        }
        is NavItemData.Toggle -> {
            val isChecked = state?.value as? Boolean ?: data.initialIsChecked
            buttonText = item.railButtonText ?: item.text
            onClick = {
                val newState = !isChecked
                state?.value = newState
                data.onStateChange(newState)
            }
        }
        is NavItemData.Cycle -> {
            val currentIndex = state?.value as? Int ?: data.options.indexOf(data.initialOption)
            buttonText = data.options.getOrNull(currentIndex) ?: ""
            onClick = {
                val nextIndex = (currentIndex + 1) % data.options.size
                state?.value = nextIndex
                data.onStateChange(data.options[nextIndex])
            }
        }
    }

    NavRailButton(onClick = onClick, text = buttonText)
}
