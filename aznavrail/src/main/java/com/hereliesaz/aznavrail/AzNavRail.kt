package com.hereliesaz.aznavrail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.util.EqualWidthLayout
import com.hereliesaz.aznavrail.util.text.AutoSizeText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

object AzNavRail {
    const val noTitle = "AZNAVRAIL_NO_TITLE"
}

private object AzNavRailLogger {
    var enabled = true
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            android.util.Log.e(tag, message, throwable)
        }
    }
}

@Composable
private fun RailItems(
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: MutableMap<String, Boolean>,
    packRailButtons: Boolean
) {
    val topLevelItems = items.filter { !it.isSubItem }
    val itemsToRender = if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems

    itemsToRender.forEach { item ->
        if (item.isRailItem) {
            RailContent(
                item = item,
                navController = navController,
                isSelected = item.route == currentDestination,
                buttonSize = buttonSize,
                onClick = scope.onClickMap[item.id],
                onRailCyclerClick = onRailCyclerClick,
                onItemClick = { onItemSelected(item) },
                onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) }
            )
            AnimatedVisibility(visible = item.isHost && (hostStates[item.id] ?: false)) {
                Column {
                    val subItems = scope.navItems.filter { it.hostId == item.id && it.isRailItem }
                    subItems.forEach { subItem ->
                        RailContent(
                            item = subItem,
                            navController = navController,
                            isSelected = subItem.route == currentDestination,
                            buttonSize = buttonSize,
                            onClick = scope.onClickMap[subItem.id],
                            onRailCyclerClick = onRailCyclerClick,
                            onItemClick = { onItemSelected(subItem) }
                        )
                    }
                }
            }
        } else { // This branch is only taken when packRailButtons is false for non-rail items
            Spacer(modifier = Modifier.height(AzNavRailDefaults.RailContentSpacerHeight))
        }
    }
}

/**
 * Default values used by the [AzNavRail] composable.
 */
private object AzNavRailDefaults {
    const val SWIPE_THRESHOLD_PX = 20f
    const val SNAP_BACK_RADIUS_PX = 50f
    val HeaderPadding = 8.dp
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

private object CenteredPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = (windowSize.width - popupContentSize.width) / 2
        val y = (windowSize.height - popupContentSize.height) / 2
        return IntOffset(x, y)
    }
}

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
 * @param navController An optional [NavController] to enable integration with Jetpack Navigation.
 * @param currentDestination The route of the current destination, used to highlight the active item.
 * @param isLandscape A boolean to indicate if the device is in landscape mode.
 * @param initiallyExpanded Whether the navigation rail is expanded by default.
 * @param disableSwipeToOpen Whether to disable the swipe-to-open gesture.
 * @param content The DSL content for the navigation rail.
 */
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val scope = remember { AzNavRailScopeImpl() }
    scope.navItems.clear()
    navController?.let { scope.navController = it }

    scope.apply(content)

    scope.navItems.forEach { item ->
        if (item.isSubItem && item.isRailItem) {
            val host = scope.navItems.find { it.id == item.hostId }
            require(host != null && host.isRailItem) {
                "A `azRailSubItem` can only be hosted by a `azRailHostItem`."
            }
        }
    }

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = remember(packageName) {
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
                .toString()
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
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isFloating by remember { mutableStateOf(false) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var isAppIcon by remember { mutableStateOf(!scope.displayAppNameInHeader) }

    val hapticFeedback = LocalHapticFeedback.current

    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedRailWidth else scope.collapsedRailWidth,
        label = "railWidth"
    )

    val coroutineScope = rememberCoroutineScope()
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    var selectedItem by rememberSaveable { mutableStateOf<AzNavItem?>(null) }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(scope.navItems) {
        val initialSelectedItem = if (currentDestination != null) {
            scope.navItems.find { it.route == currentDestination }
        } else {
            scope.navItems.firstOrNull()
        }
        selectedItem = initialSelectedItem

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
                                val clicksToCatchUp =
                                    (targetIndex - currentIndexInVm + options.size) % options.size
                                val onClick = scope.onClickMap[item.id]
                                if (onClick != null) {
                                    repeat(clicksToCatchUp) {
                                        onClick()
                                    }
                                }
                            }
                            cyclerStates[id] = state.copy(job = null)
                        }
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val viewConfiguration = LocalViewConfiguration.current

    Box(modifier = modifier) {
        val buttonSize = if (isLandscape) screenWidth / 18 else screenHeight / 18
        selectedItem?.screenTitle?.let { screenTitle ->
            if (screenTitle.isNotEmpty()) {
                Popup(alignment = Alignment.TopEnd) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, top = 16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
        if (scope.isLoading) {
            Popup(
                popupPositionProvider = CenteredPopupPositionProvider,
                properties = PopupProperties(focusable = false)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AzLoad()
                }
            }
        }
        Box {
            NavigationRail(
                modifier = Modifier
                    .width(railWidth)
                    .offset { railOffset },
                containerColor = Color.Transparent,
                header = {
                    Box(
                        modifier = Modifier
                            .padding(bottom = AzNavRailDefaults.HeaderPadding)
                            .pointerInput(isFloating, scope.enableRailDragging) {
                                awaitEachGesture {
                                    if (isFloating) {
                                        val down = awaitFirstDown()
                                        var change = down
                                        val dragStart = coroutineScope.launch {
                                                delay(viewConfiguration.longPressTimeoutMillis)
                                            }

                                            var dragConsumed = false
                                            while (dragStart.isActive && change.pressed) {
                                                val event = awaitPointerEvent()
                                                if (event.changes.any { it.pressed }) {
                                                    val dragChange = event.changes.first()
                                                    if (dragChange.positionChange() != Offset.Zero) {
                                                        dragConsumed = true
                                                        railOffset += IntOffset(
                                                            dragChange.positionChange().x.toInt(),
                                                            dragChange.positionChange().y.toInt()
                                                        )
                                                        dragChange.consume()
                                                    }
                                                    change = dragChange
                                                } else {
                                                    break
                                                }
                                            }
                                            dragStart.cancel()

                                            if (change.pressed) { // Long press
                                                railOffset = IntOffset.Zero
                                                isFloating = false
                                                if (scope.displayAppNameInHeader) isAppIcon = false
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else { // Drag or tap
                                                if (dragConsumed) { // Drag ended
                                                    val distance = kotlin.math.sqrt(
                                                        railOffset.x.toFloat().pow(2) + railOffset.y.toFloat().pow(2)
                                                    )
                                                    if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                        railOffset = IntOffset.Zero
                                                        isFloating = false
                                                        if (scope.displayAppNameInHeader) isAppIcon = false
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                } else { // Tap
                                                    showFloatingButtons = !showFloatingButtons
                                                }
                                            }
                                    } else { // Docked state
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var dragStarted = false
                                        var totalDrag = Offset.Zero
                                        var isVerticalDrag = false

                                        var finished = false
                                        while (!finished) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.first()

                                            if (change.pressed) {
                                                totalDrag += change.positionChange()
                                                val absX = kotlin.math.abs(totalDrag.x)
                                                val absY = kotlin.math.abs(totalDrag.y)
                                                val slop = viewConfiguration.touchSlop

                                                if (!dragStarted && (absX > slop || absY > slop)) {
                                                    dragStarted = true
                                                    isVerticalDrag = absY > absX
                                                }

                                                if (dragStarted) {
                                                    if (isVerticalDrag) {
                                                        if (scope.enableRailDragging) {
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            isFloating = true
                                                            isExpanded = false
                                                            if (scope.displayAppNameInHeader) isAppIcon = true
                                                            railOffset = IntOffset(totalDrag.x.toInt(), totalDrag.y.toInt())
                                                            event.changes.forEach { it.consume() }
                                                            finished = true // Exit loop to let the next gesture cycle handle floating drag
                                                        }
                                                    } else { // Horizontal drag
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            } else { // Finger lifted
                                                finished = true
                                                if (dragStarted && !isVerticalDrag) { // Horizontal drag finished
                                                    if (isExpanded) {
                                                        if (totalDrag.x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                            isExpanded = false
                                                        }
                                                    } else if (!disableSwipeToOpen) {
                                                        if (totalDrag.x > AzNavRailDefaults.SWIPE_THRESHOLD_PX) {
                                                            isExpanded = true
                                                        }
                                                    }
                                                } else if (!dragStarted) { // Tap
                                                    isExpanded = !isExpanded
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = if (isAppIcon) Alignment.Center else Alignment.CenterStart
                    ) {
                        if (isAppIcon) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = appIcon),
                                        contentDescription = "Toggle menu, showing $appName icon",
                                        modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle Menu",
                                        modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleMedium,
                                softWrap = false,
                                maxLines = 1,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            ) {
                if (isExpanded) {
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val itemsToShow = scope.navItems.filter { !it.isSubItem }
                            itemsToShow.forEach { item ->
                                if (item.isDivider) {
                                    AzDivider()
                                } else {
                                    val onClick = scope.onClickMap[item.id]
                                    val onCyclerClick = if (item.isCycler) {
                                        {
                                            val state = cyclerStates[item.id]
                                            if (state != null && !item.disabled) {
                                                state.job?.cancel()

                                                val options =
                                                    requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                                                val disabledOptions =
                                                    item.disabledOptions ?: emptyList()
                                                val enabledOptions =
                                                    options.filterNot { it in disabledOptions }

                                                if (enabledOptions.isNotEmpty()) {
                                                    val currentDisplayed = state.displayedOption
                                                    val currentIndexInEnabled =
                                                        enabledOptions.indexOf(currentDisplayed)

                                                    val nextIndex =
                                                        if (currentIndexInEnabled != -1) {
                                                            (currentIndexInEnabled + 1) % enabledOptions.size
                                                        } else {
                                                            0
                                                        }
                                                    val nextOption = enabledOptions[nextIndex]

                                                    cyclerStates[item.id] = state.copy(
                                                        displayedOption = nextOption,
                                                        job = coroutineScope.launch {
                                                            delay(1000L)

                                                            val finalItemState =
                                                                scope.navItems.find { it.id == item.id }
                                                                    ?: item
                                                            val currentStateInVm =
                                                                finalItemState.selectedOption
                                                            val targetState = nextOption

                                                            val currentIndexInVm =
                                                                options.indexOf(currentStateInVm)
                                                            val targetIndex =
                                                                options.indexOf(targetState)

                                                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                                                val clicksToCatchUp =
                                                                    (targetIndex - currentIndexInVm + options.size) % options.size
                                                                if (onClick != null) {
                                                                    repeat(clicksToCatchUp) {
                                                                        onClick()
                                                                    }
                                                                }
                                                            }
                                                            isExpanded = false
                                                            cyclerStates[item.id] =
                                                                cyclerStates[item.id]!!.copy(job = null)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        onClick
                                    }
                                    val finalItem = if (item.isCycler) {
                                        item.copy(
                                            selectedOption = cyclerStates[item.id]?.displayedOption
                                                ?: item.selectedOption
                                        )
                                    } else if (item.isHost) {
                                        item.copy(isExpanded = hostStates[item.id] ?: false)
                                    } else {
                                        item
                                    }
                                    MenuItem(
                                        item = finalItem,
                                        navController = navController,
                                        isSelected = finalItem.route == currentDestination,
                                        onClick = onClick,
                                        onCyclerClick = onCyclerClick,
                                        onToggle = { isExpanded = !isExpanded },
                                        onItemClick = { selectedItem = finalItem },
                                        onHostClick = {
                                            hostStates[item.id] = !(hostStates[item.id] ?: false)
                                        }
                                    )

                                    AnimatedVisibility(
                                        visible = item.isHost && (hostStates[item.id] ?: false)
                                    ) {
                                        Column {
                                            val subItems =
                                                scope.navItems.filter { it.hostId == item.id }
                                            subItems.forEach { subItem ->
                                                MenuItem(
                                                    item = subItem,
                                                    navController = navController,
                                                    isSelected = subItem.route == currentDestination,
                                                    onClick = scope.onClickMap[subItem.id],
                                                    onCyclerClick = null,
                                                    onToggle = { isExpanded = !isExpanded },
                                                    onItemClick = { selectedItem = subItem }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (scope.showFooter) {
                            Footer(appName = appName, onToggle = { isExpanded = !isExpanded })
                        }
                    }
                } else {
                    AnimatedVisibility(visible = !isFloating || showFloatingButtons) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = AzNavRailDefaults.RailContentHorizontalPadding)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val onRailCyclerClick: (AzNavItem) -> Unit = { item ->
                                val state = cyclerStates[item.id]
                                if (state != null) {
                                    val options =
                                        requireNotNull(item.options) { "Cycler item '${item.id}' must have options" }
                                    val disabledOptions = item.disabledOptions ?: emptyList()
                                    val enabledOptions = options.filterNot { it in disabledOptions }

                                    if (enabledOptions.isNotEmpty()) {
                                        val currentDisplayed = item.selectedOption
                                        val currentIndexInEnabled =
                                            enabledOptions.indexOf(currentDisplayed)

                                        val nextIndex = if (currentIndexInEnabled != -1) {
                                            (currentIndexInEnabled + 1) % enabledOptions.size
                                        } else {
                                            0
                                        }
                                        val nextOption = enabledOptions[nextIndex]

                                        val finalItemState =
                                            scope.navItems.find { it.id == item.id } ?: item
                                        val currentStateInVm = finalItemState.selectedOption
                                        val targetState = nextOption

                                        val currentIndexInVm = options.indexOf(currentStateInVm)
                                        val targetIndex = options.indexOf(targetState)

                                        if (currentIndexInVm != -1 && targetIndex != -1) {
                                            val clicksToCatchUp =
                                                (targetIndex - currentIndexInVm + options.size) % options.size
                                            val onClick = scope.onClickMap[item.id]
                                            if (onClick != null) {
                                                repeat(clicksToCatchUp) {
                                                    onClick()
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            EqualWidthLayout(
                                verticalSpacing = if (scope.packRailButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
                            ) {
                                RailItems(
                                    items = scope.navItems,
                                    scope = scope,
                                    navController = navController,
                                    currentDestination = currentDestination,
                                    buttonSize = buttonSize,
                                    onRailCyclerClick = onRailCyclerClick,
                                    onItemSelected = { selectedItem = it },
                                    hostStates = hostStates,
                                    packRailButtons = scope.packRailButtons
                                )
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
                                onClick = { isExpanded = false },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                    )
                }
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
private fun RailContent(
    item: AzNavItem,
    navController: NavController?,
    isSelected: Boolean,
    buttonSize: Dp,
    onClick: (() -> Unit)?,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit = {}
) {
    val textToShow = when {
        item.isToggle -> if (item.isChecked == true) item.toggleOnText else item.toggleOffText
        item.isCycler -> item.selectedOption ?: ""
        else -> item.text
    }

    val finalOnClick = if (item.isHost) {
        {
            handleHostItemClick(item, navController, onClick, onItemClick, onHostClick)
        }
    } else if (item.isCycler) {
        {
            onRailCyclerClick(item)
            onItemClick()
        }
    } else {
        {
            item.route?.let { navController?.navigate(it) }
            onClick?.invoke()
            onItemClick()
        }
    }

    Box(
        modifier = if (item.shape == AzButtonShape.RECTANGLE) Modifier.padding(vertical = 2.dp) else Modifier
    ) {
        AzNavRailButton(
            onClick = finalOnClick,
            text = textToShow,
            color = item.color ?: MaterialTheme.colorScheme.primary,
            size = buttonSize,
            shape = item.shape,
            disabled = item.disabled,
            isSelected = isSelected
        )
    }
}

private fun handleHostItemClick(
    item: AzNavItem,
    navController: NavController?,
    onClick: (() -> Unit)?,
    onItemClick: () -> Unit,
    onHostClick: () -> Unit
) {
    onHostClick()
    item.route?.let { navController?.navigate(it) }
    onClick?.invoke()
    onItemClick()
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
    navController: NavController?,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    onCyclerClick: (() -> Unit)? = null,
    onToggle: () -> Unit = {},
    onItemClick: () -> Unit = {},
    onHostClick: () -> Unit = {}
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
                    item.route?.let { navController?.navigate(it) }
                    onClick?.invoke()
                    onToggle()
                    onItemClick()
                }
            )
        } else {
            Modifier.clickable {
                if (item.isHost) {
                    handleHostItemClick(item, navController, onClick, onItemClick, onHostClick)
                } else if (item.isCycler) {
                    onCyclerClick?.invoke()
                } else {
                    item.route?.let { navController?.navigate(it) }
                    onClick?.invoke()
                    onToggle()
                    onItemClick()
                }
            }
        }
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        item.disabled -> MaterialTheme.typography.bodyMedium.color.copy(alpha = 0.5f)
        else -> MaterialTheme.typography.bodyMedium.color
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AzNavRailDefaults.MenuItemHorizontalPadding,
                vertical = AzNavRailDefaults.MenuItemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val lines = textToShow.split('\n')
        Column(modifier = Modifier.weight(1f)) {
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
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/HereLiesAz/$appName".toUri()
                )
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
                    data = "mailto:".toUri()
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
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.instagram.com/hereliesaz")
                )
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                AzNavRailLogger.e("AzNavRail.Footer", "Could not open 'Credit' link.", e)
            }
        }
    }

    Column {
        AzDivider()
        MenuItem(
            item = AzNavItem(id = "about", text = "About", isRailItem = false),
            navController = null,
            isSelected = false,
            onClick = onAboutClick,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {})
        MenuItem(
            item = AzNavItem(id = "feedback", text = "Feedback", isRailItem = false),
            navController = null,
            isSelected = false,
            onClick = onFeedbackClick,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {})
        MenuItem(
            item = AzNavItem(
                id = "credit",
                text = "@HereLiesAz",
                isRailItem = false
            ),
            navController = null,
            isSelected = false,
            onClick = onCreditClick,
            onCyclerClick = null,
            onToggle = onToggle,
            onItemClick = {}
        )
        Spacer(modifier = Modifier.height(AzNavRailDefaults.FooterSpacerHeight))
    }
}

@Composable
fun AzDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    }
}

@Composable
fun AzNavRailButton(
    onClick: () -> Unit,
    text: String,
    color: Color,
    size: Dp,
    shape: AzButtonShape,
    disabled: Boolean,
    isSelected: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else color
    val buttonShape = when (shape) {
        AzButtonShape.CIRCLE -> CircleShape
        AzButtonShape.SQUARE -> RoundedCornerShape(0.dp)
        AzButtonShape.RECTANGLE -> RoundedCornerShape(0.dp)
        AzButtonShape.NONE -> RoundedCornerShape(0.dp)
    }

    val buttonModifier = when (shape) {
        AzButtonShape.RECTANGLE -> Modifier
            .height(48.dp)
            .fillMaxWidth()

        else -> Modifier.size(size)
    }

    val finalColor = if (disabled) color.copy(alpha = 0.5f) else color

    Surface(
        modifier = buttonModifier
            .clickable(
                onClick = { if (!disabled) onClick() },
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor)
            ),
        shape = buttonShape,
        color = if (isSelected) finalColor else Color.Transparent,
        border = if (shape != AzButtonShape.NONE) BorderStroke(
            2.dp,
            if (isSelected) finalColor else finalColor.copy(alpha = 0.5f)
        ) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (shape != AzButtonShape.NONE) {
                AutoSizeText(
                    text = text,
                    color = if (disabled) contentColor.copy(alpha = 0.5f) else contentColor,
                    modifier = Modifier.padding(8.dp),
                    softWrap = false
                )
            } else {
                Text(
                    text = text,
                    color = if (disabled) finalColor.copy(alpha = 0.5f) else finalColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
