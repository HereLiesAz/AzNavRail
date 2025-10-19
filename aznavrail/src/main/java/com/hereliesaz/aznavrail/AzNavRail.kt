package com.hereliesaz.aznavrail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.AzLoad
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.util.EqualWidthLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object AzNavRailLogger {
    var enabled = true
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

/**
 * Default values used by the [AzNavRail] composable.
 */
private object AzNavRailDefaults {
    const val SWIPE_THRESHOLD_PX = 20f
    val HeaderPadding = 16.dp
    val HeaderIconSize = 72.dp
    val HeaderTextSpacer = 8.dp
    val RailContentHorizontalPadding = 4.dp
    val RailContentVerticalArrangement = 8.dp
    val RailContentSpacerHeight = 72.dp
    val MenuItemHorizontalPadding = 24.dp
    val MenuItemVerticalPadding = 12.dp
    val FooterDividerHorizontalPadding = 16.dp
    val FooterDividerVerticalPadding = 8.dp
    val FooterSpacerHeight = 12.dp
}

/**
 * Represents the transient state of a cycler item, tracking the displayed option and the
 * coroutine job for the delayed action.
 *
 * @param displayedOption The currently displayed option in the UI.
 * @param job The coroutine job for handling the delayed click action. This is cancelled and
 * restarted on each click.
 */
private data class CyclerTransientState(
    val displayedOption: String,
    val job: Job? = null
)

/**
 * An M3-style navigation rail that expands into a menu drawer.
 *
 * This composable provides a vertical navigation rail that can be expanded to a full menu drawer.
 * It is designed to be "batteries-included," providing common behaviors and features out-of-the-box.
 *
 * The rail is responsive and will adjust its layout for landscape and portrait orientations.
 * The content of both the collapsed rail and the expanded menu is scrollable, allowing for a large
 * number of navigation items.
 *
 * The content of the rail and menu is defined using a DSL within the `content` lambda.
 *
 * The header of the rail will automatically display your app's icon by default. This can be changed
 * to display the app's name instead by using the `azSettings` function in the content lambda.
 * Clicking it will expand or collapse the rail.
 *
 * Standard and toggle menu items will collapse the rail upon being tapped. Cycler items will
 * only collapse the rail after the user has settled on a choice for at least one second.
 *
 * @param modifier The modifier to be applied to the navigation rail.
 * @param initiallyExpanded Whether the navigation rail is expanded by default.
 * @param disableSwipeToOpen Whether to disable the swipe-to-open gesture.
 * @param content The DSL content for the navigation rail.
 */
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val scope = remember { AzNavRailScopeImpl() }
    scope.navItems.clear()
    scope.apply(content)

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = remember(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app name", e)
            "App" // Fallback name
        }
    }

    val appIcon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app icon", e)
            null // Fallback to default icon
        }
    }

    var isExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val onToggle: () -> Unit = remember { { isExpanded = !isExpanded } }

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedRailWidth else scope.collapsedRailWidth,
        label = "railWidth"
    )

    val coroutineScope = rememberCoroutineScope()
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }

    LaunchedEffect(scope.navItems) {
        scope.navItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
            }
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            cyclerStates.forEach { (id, state) ->
                if (state.job != null) {
                    state.job.cancel()
                    val item = scope.navItems.find { it.id == id }
                    if (item != null) {
                        coroutineScope.launch {
                            val options = requireNotNull(item.options)
                            val currentStateInVm = item.selectedOption
                            val targetState = state.displayedOption

                            val currentIndexInVm = options.indexOf(currentStateInVm)
                            val targetIndex = options.indexOf(targetState)

                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                repeat(clicksToCatchUp) {
                                    item.onClick()
                                }
                            }
                            cyclerStates[id] = state.copy(job = null)
                        }
                    }
                }
            }
        }
    }

    val buttonSize = scope.collapsedRailWidth - AzNavRailDefaults.RailContentHorizontalPadding * 2
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.pointerInput(isExpanded, disableSwipeToOpen) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, _) = dragAmount
                    if (isExpanded) {
                        if (x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) { onToggle() }
                    } else if (!disableSwipeToOpen) {
                        if (x > AzNavRailDefaults.SWIPE_THRESHOLD_PX) { onToggle() }
                    }
                }
            }
        ) {
            NavigationRail(
                modifier = Modifier.width(railWidth),
                containerColor = Color.Transparent,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(top = AzNavRailDefaults.HeaderPadding, bottom = AzNavRailDefaults.HeaderPadding)
                            .clickable(
                                onClick = onToggle,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (scope.displayAppNameInHeader) {
                            val textModifier = Modifier.width(scope.expandedRailWidth)
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = textModifier,
                                softWrap = false,
                                maxLines = if (isExpanded && appName.contains("\n")) Int.MAX_VALUE else 1,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = appIcon),
                                        contentDescription = "Toggle menu, showing $appName icon",
                                        modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle Menu",
                                        modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                    )
                                }
                            }
                        }
                    }
                }
            ) {
                if (scope.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        AzLoad()
                    }
                }
                else if (isExpanded) {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            scope.navItems.forEach { item ->
                                if (item.isDivider) {
                                    AzDivider()
                                } else {
                                    val onCyclerClick = if (item.isCycler) {
                                        {
                                            val state = cyclerStates[item.id]
                                            if (state != null && !item.disabled) {
                                                state.job?.cancel()

                                                val options = requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                                                val disabledOptions = item.disabledOptions ?: emptyList()
                                                val enabledOptions = options.filterNot { it in disabledOptions }

                                                if (enabledOptions.isNotEmpty()) {
                                                    val currentDisplayed = state.displayedOption
                                                    val currentIndexInEnabled = enabledOptions.indexOf(currentDisplayed)

                                                    val nextIndex = if (currentIndexInEnabled != -1) {
                                                        (currentIndexInEnabled + 1) % enabledOptions.size
                                                    } else {
                                                        0
                                                    }
                                                    val nextOption = enabledOptions[nextIndex]

                                                    cyclerStates[item.id] = state.copy(
                                                        displayedOption = nextOption,
                                                        job = coroutineScope.launch {
                                                            delay(1000L)

                                                            val finalItemState = scope.navItems.find { it.id == item.id } ?: item
                                                            val currentStateInVm = finalItemState.selectedOption
                                                            val targetState = nextOption

                                                            val currentIndexInVm = options.indexOf(currentStateInVm)
                                                            val targetIndex = options.indexOf(targetState)

                                                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                                                repeat(clicksToCatchUp) {
                                                                    item.onClick()
                                                                }
                                                            }

                                                            onToggle()
                                                            cyclerStates[item.id] = cyclerStates[item.id]!!.copy(job = null)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        item.onClick
                                    }
                                    val finalItem = if (item.isCycler) {
                                        item.copy(selectedOption = cyclerStates[item.id]?.displayedOption ?: item.selectedOption)
                                    } else {
                                        item
                                    }
                                    MenuItem(item = finalItem, onCyclerClick = onCyclerClick, onToggle = onToggle)
                                }
                            }
                        }
                        if (scope.showFooter) {
                            Footer(appName = appName, onToggle = onToggle)
                        }
                    }
                } else {
                    val railItems = remember(scope) { scope.navItems.filter { it.isRailItem } }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = if (scope.packRailButtons) Arrangement.Top else Arrangement.spacedBy(
                            AzNavRailDefaults.RailContentVerticalArrangement,
                            Alignment.CenterVertically
                        )
                    ) {
                        EqualWidthLayout {
                            if (scope.packRailButtons) {
                                railItems.forEach { item ->
                                    RailContent(item = item, buttonSize = buttonSize)
                                }
                            } else {
                                scope.navItems.forEach { menuItem ->
                                    if (menuItem.isRailItem) {
                                        RailContent(item = menuItem, buttonSize = buttonSize)
                                    } else {
                                        Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable(
                            onClick = onToggle,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                )
            }
        }
    }
}

/**
 * Composable for displaying a single item in the collapsed rail.
 * @param item The navigation item to display.
 * @param buttonSize The size of the button.
 */
@Composable
private fun RailContent(item: AzNavItem, buttonSize: Dp) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }
    AzNavRailButton(
        onClick = item.onClick,
        text = textToShow,
        color = item.color ?: MaterialTheme.colorScheme.primary,
        size = buttonSize,
        shape = item.shape,
        disabled = item.disabled
    )
}

/**
 * Composable for displaying a single item in the expanded menu.
 *
 * This composable handles the display and interaction for all types of menu items, including
 * standard, toggle, and cycler items. It supports multi-line text with indentation for all
 * lines after the first.
 *
 * @param item The navigation item to display.
 * @param onCyclerClick The click handler for cycler items, which includes the delay logic.
 * @param onToggle The click handler for toggling the rail's expanded state. This is called
 * immediately for standard and toggle items, and with a delay for cycler items.
 */
@Composable
private fun MenuItem(
    item: AzNavItem,
    onCyclerClick: () -> Unit = item.onClick,
    onToggle: () -> Unit = {}
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val modifier = if (item.disabled) Modifier else {
        if (item.isToggle) {
            Modifier.toggleable(
                value = item.isChecked ?: false,
                onValueChange = {
                    item.onClick()
                    onToggle()
                }
            )
        } else {
            Modifier.clickable {
                if (item.isCycler) {
                    onCyclerClick()
                } else {
                    item.onClick()
                    onToggle()
                }
            }
        }
    }

    val textColor = if (item.disabled) {
        MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.5f)
    } else {
        MaterialTheme.typography.bodyMedium.color
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AzNavRailDefaults.MenuItemHorizontalPadding, vertical = AzNavRailDefaults.MenuItemVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val lines = textToShow.split('\n')
        Column {
            lines.forEachIndexed { index, line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = if (index > 0) Modifier.padding(start = 16.dp) else Modifier
                )
            }
        }
    }
}

/**
 * Composable for displaying the footer in the expanded menu.
 * @param appName The name of the app.
 * @param onToggle The click handler for toggling the rail's expanded state.
 */
@Composable
private fun Footer(appName: String, onToggle: () -> Unit) {
    val context = LocalContext.current
    val onAboutClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HereLiesAz/$appName"))
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'About' link.", e)
            }
        }
    }
    val onFeedbackClick: () -> Unit = remember(context, appName) {
        {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("hereliesaz@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Feedback for $appName")
                }
                context.startActivity(Intent.createChooser(intent, "Send Feedback"))
            } catch (e: ActivityNotFoundException) {
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'Feedback' link.", e)
            }
        }
    }
    val onCreditClick: () -> Unit = remember(context) {
        {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/hereliesaz"))
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'Credit' link.", e)
            }
        }
    }

    Column {
        AzDivider()
        MenuItem(item = AzNavItem(id = "about", text = "About", isRailItem = false, onClick = onAboutClick), onToggle = onToggle)
        MenuItem(item = AzNavItem(id = "feedback", text = "Feedback", isRailItem = false, onClick = onFeedbackClick), onToggle = onToggle)
        MenuItem(
            item = AzNavItem(
                id = "credit",
                text = "@HereLiesAz",
                isRailItem = false,
                onClick = onCreditClick
            ),
            onToggle = onToggle
        )
        Spacer(modifier = Modifier.height(AzNavRailDefaults.FooterSpacerHeight))
    }
}