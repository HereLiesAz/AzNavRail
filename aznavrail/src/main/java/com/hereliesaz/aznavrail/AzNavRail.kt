package com.hereliesaz.aznavrail

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.AzVisualSide
import com.hereliesaz.aznavrail.internal.CenteredPopupPositionProvider
import com.hereliesaz.aznavrail.internal.CyclerTransientState
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.OverlayHelper
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import com.hereliesaz.aznavrail.service.LocalAzNavRailOverlayController
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

import com.hereliesaz.aznavrail.internal.CollapsedRailContent
import com.hereliesaz.aznavrail.internal.ExpandedRailContent

@Target(AnnotationTarget.FUNCTION)
@RequiresOptIn(message = "This API is strictly controlled. Use AzHostActivityLayout.")
annotation class AzStrictLayout

object AzNavRail {
    const val noTitle = "AZNAVRAIL_NO_TITLE"
    const val EXTRA_ROUTE = "com.hereliesaz.aznavrail.extra.ROUTE"
}

@AzStrictLayout
@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean = false,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    providedScope: AzNavRailScopeImpl? = null,
    orientation: AzOrientation = AzOrientation.Vertical,
    visualDockingSide: AzDockingSide? = null,
    railAlignment: Alignment? = null,
    reverseLayout: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    val isHostPresent = LocalAzNavHostPresent.current
    val overlayController = LocalAzNavRailOverlayController.current

    if (!isHostPresent && overlayController == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "FATAL LAYOUT VIOLATION",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AzNavRail MUST be wrapped in AzHostActivityLayout.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        error("FATAL LAYOUT VIOLATION: AzNavRail instantiated without AzHostActivityLayout.")
    }

    val scope = providedScope ?: remember { AzNavRailScopeImpl() }

    if (providedScope == null) {
        scope.reset()
    }
    navController?.let { scope.navController = it }

    scope.apply(content)
    val effectiveNoMenu = scope.noMenu && overlayController == null
    val effectiveDockingSide = visualDockingSide ?: scope.dockingSide
    val isRightDocked = effectiveDockingSide == AzDockingSide.RIGHT

    val displayedNavItems = remember(scope.navItems, effectiveNoMenu) {
        if (effectiveNoMenu) {
            scope.navItems.map { it.copy(isRailItem = true) }
        } else {
            scope.navItems
        }
    }

    displayedNavItems.forEach { item ->
        if (item.isSubItem && item.isRailItem) {
            val host = scope.navItems.find { it.id == item.hostId }
            require(host != null && host.isRailItem) {
                "HIERARCHY ERROR: Rail sub-item '${item.id}' must be hosted by a valid rail host item."
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
            "App"
        }
    }

    val appIcon = remember(packageName) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            AzNavRailLogger.e("AzNavRail", "Error getting app icon", e)
            null
        }
    }

    val footerColor = remember(displayedNavItems) {
        displayedNavItems.firstOrNull()?.color ?: Color.Unspecified
    }

    var isExpandedInternal by rememberSaveable(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    
    val isExpandedState = remember(overlayController, effectiveNoMenu) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = if (overlayController != null || effectiveNoMenu) false else isExpandedInternal
                set(v) {
                     if (overlayController == null && !effectiveNoMenu) isExpandedInternal = v
                }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var isExpanded by isExpandedState

    var showFooterPopup by remember { mutableStateOf(false) }
    var railOffset by remember { mutableStateOf(IntOffset.Zero) }
    var isFloating by remember { mutableStateOf(false) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var wasVisibleOnDragStart by remember { mutableStateOf(false) }
    var isAppIcon by remember { mutableStateOf(!scope.displayAppName) }
    var headerHeight by remember { mutableStateOf(0) }
    var railItemsHeight by remember { mutableStateOf(0) }

    val hapticFeedback = LocalHapticFeedback.current

    val isVertical = orientation == AzOrientation.Vertical

    val railThickness by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedWidth else scope.collapsedWidth,
        label = "railThickness"
    )

    val coroutineScope = rememberCoroutineScope()
    val cyclerStates = remember { mutableStateMapOf<String, CyclerTransientState>() }
    var selectedItem by rememberSaveable { mutableStateOf<AzNavItem?>(null) }
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val itemPositions = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    LaunchedEffect(displayedNavItems) {
        val initialSelectedItem = if (currentDestination != null) {
            displayedNavItems.find { it.route == currentDestination }
        } else {
            displayedNavItems.firstOrNull()
        }
        selectedItem = initialSelectedItem

        displayedNavItems.forEach { item ->
            if (item.isCycler) {
                cyclerStates.putIfAbsent(item.id, CyclerTransientState(item.selectedOption ?: ""))
            }
        }
    }

    // Cycler sync logic
    LaunchedEffect(isExpanded) {
        if (!isExpanded) {
            cyclerStates.forEach { (id, state) ->
                if (state.job != null) {
                    state.job.cancel()
                    val item = displayedNavItems.find { it.id == id }
                    if (item != null) {
                        coroutineScope.launch {
                            val options = requireNotNull(item.options)
                            val currentStateInVm = item.selectedOption
                            val targetState = state.displayedOption
                            val currentIndexInVm = options.indexOf(currentStateInVm)
                            val targetIndex = options.indexOf(targetState)

                            if (currentIndexInVm != -1 && targetIndex != -1) {
                                val clicksToCatchUp = (targetIndex - currentIndexInVm + options.size) % options.size
                                val onClick = scope.onClickMap[item.id]
                                if (onClick != null) {
                                    repeat(clicksToCatchUp) { onClick() }
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
    val screenHeight = configuration.screenHeightDp.dp
    val density = LocalDensity.current

    LaunchedEffect(showFloatingButtons) {
        if (showFloatingButtons) {
            if (scope.onRailDrag == null && overlayController == null) {
                val screenHeightPx = with(density) { screenHeight.toPx() }
                val bottomBound = screenHeightPx * 0.9f
                val railBottom = railOffset.y + headerHeight + railItemsHeight
                if (railBottom > bottomBound) {
                    val newY = bottomBound - headerHeight - railItemsHeight
                    railOffset = IntOffset(railOffset.x, newY.roundToInt())
                }
            }
        }
    }

    val activity = LocalContext.current as? androidx.activity.ComponentActivity
    LaunchedEffect(activity) {
        activity?.intent?.let { intent ->
            if (intent.hasExtra(AzNavRail.EXTRA_ROUTE)) {
                val route = intent.getStringExtra(AzNavRail.EXTRA_ROUTE)
                if (route != null) {
                    navController?.navigate(route)
                    intent.removeExtra(AzNavRail.EXTRA_ROUTE)
                }
            }
        }
    }

    // Determine alignment for the Box based on visual docking side
    // Note: This aligns the inner content (the Surface)
    val alignment = railAlignment ?: (if (isRightDocked) Alignment.TopEnd else Alignment.TopStart)

    Box(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        val buttonSize = AzNavRailDefaults.HeaderIconSize
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

        // Main Rail Content
        Surface(
            modifier = Modifier
                .then(if (isVertical) Modifier.width(railThickness).fillMaxHeight() else Modifier.height(railThickness).fillMaxWidth())
                .offset {
                     if (overlayController != null) {
                         overlayController.contentOffset.value
                     } else {
                         railOffset
                     }
                },
            color = if (isExpanded) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent,
        ) {
            val headerContent = @Composable {
                Box(
                    modifier = Modifier
                        .padding(bottom = if (isVertical) AzNavRailDefaults.HeaderPadding else 0.dp, end = if (!isVertical) AzNavRailDefaults.HeaderPadding else 0.dp)
                        .onSizeChanged { headerHeight = it.height }
                        .pointerInput(isFloating, scope.enableRailDragging, scope.displayAppName, scope.onRailDrag, overlayController) {
                            detectTapGestures(
                                onTap = {
                                    if (effectiveNoMenu) {
                                        showFooterPopup = !showFooterPopup
                                    } else if (isFloating) {
                                        showFloatingButtons = !showFloatingButtons
                                    } else {
                                        if (overlayController == null) isExpanded = !isExpanded
                                    }
                                },
                                onLongPress = {
                                    if (isFloating) {
                                        if (scope.onRailDrag == null && overlayController == null) railOffset = IntOffset.Zero
                                        isFloating = false
                                        if (scope.displayAppName) isAppIcon = false
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } else if (scope.enableRailDragging) {
                                        val overlayService = scope.overlayService
                                        if (overlayService != null) {
                                            OverlayHelper.launch(context, overlayService)
                                        } else {
                                            isFloating = true
                                            isExpanded = false
                                            if (scope.displayAppName) isAppIcon = true
                                        }
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        }
                        .pointerInput(isFloating, scope.enableRailDragging, scope.onRailDrag, overlayController) {
                            detectDragGestures(
                                onDragStart = {
                                    if (overlayController != null) {
                                        overlayController.onDragStart()
                                        if (isFloating) {
                                            wasVisibleOnDragStart = showFloatingButtons
                                            showFloatingButtons = false
                                        }
                                    } else if (isFloating) {
                                        wasVisibleOnDragStart = showFloatingButtons
                                        showFloatingButtons = false
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    if (overlayController != null) {
                                        change.consume()
                                        overlayController.onDrag(dragAmount)
                                    } else if (scope.onOverlayDrag != null) {
                                        change.consume()
                                        scope.onOverlayDrag?.invoke(dragAmount.x, dragAmount.y)
                                    } else if (isFloating) {
                                        change.consume()
                                        val onDrag = scope.onRailDrag
                                        if (onDrag != null) {
                                            onDrag(dragAmount.x, dragAmount.y)
                                        } else {
                                            val newY = railOffset.y + dragAmount.y
                                            val screenHeightPx = with(density) { screenHeight.toPx() }
                                            val bottomBound = screenHeightPx * 0.9f - headerHeight
                                            val clampedY = newY.coerceIn(0f, bottomBound)
                                            railOffset = IntOffset(railOffset.x + dragAmount.x.roundToInt(), clampedY.roundToInt())
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (overlayController != null) {
                                        overlayController.onDragEnd()
                                        if (isFloating) showFloatingButtons = true
                                    } else if (isFloating) {
                                        if (scope.onRailDrag == null) {
                                            val distance = kotlin.math.sqrt(railOffset.x.toFloat().pow(2) + railOffset.y.toFloat().pow(2))
                                            if (distance < AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                                                railOffset = IntOffset.Zero
                                                isFloating = false
                                                if (scope.displayAppName) isAppIcon = false
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else if (wasVisibleOnDragStart) {
                                                showFloatingButtons = true
                                            }
                                        } else {
                                            if (wasVisibleOnDragStart) showFloatingButtons = true
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = if (isAppIcon) Alignment.Center else Alignment.CenterStart
                ) {
                    if (isAppIcon) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (appIcon != null) {
                                val baseModifier = Modifier.size(AzNavRailDefaults.HeaderIconSize)
                                val finalModifier = when (scope.headerIconShape) {
                                    AzHeaderIconShape.CIRCLE -> baseModifier.clip(CircleShape)
                                    AzHeaderIconShape.ROUNDED -> baseModifier.clip(RoundedCornerShape(12.dp))
                                    AzHeaderIconShape.NONE -> baseModifier
                                }
                                Image(painter = rememberAsyncImagePainter(model = appIcon), contentDescription = "Toggle menu, showing $appName icon", modifier = finalModifier)
                            } else {
                                Icon(imageVector = Icons.Default.Menu, contentDescription = "Toggle Menu", modifier = Modifier.size(AzNavRailDefaults.HeaderIconSize))
                            }
                        }
                    } else {
                        Text(text = appName, style = MaterialTheme.typography.titleMedium, softWrap = false, maxLines = 1, textAlign = TextAlign.Start)
                    }
                }
            }

            val contentBlock = @Composable {
                if (isExpanded) {
                    val displayedItems = if (reverseLayout) displayedNavItems.reversed() else displayedNavItems
                    ExpandedRailContent(
                        scope = scope,
                        displayedNavItems = displayedItems,
                        navController = navController,
                        currentDestination = currentDestination,
                        cyclerStates = cyclerStates,
                        hostStates = hostStates,
                        itemPositions = itemPositions,
                        isRightDocked = isRightDocked,
                        appName = appName,
                        footerColor = if (footerColor != Color.Unspecified) footerColor else MaterialTheme.colorScheme.primary,
                        onToggle = { isExpanded = !isExpanded },
                        onUndock = {
                            val overlayService = scope.overlayService
                            if (scope.onUndock != null) {
                                scope.onUndock?.invoke()
                            } else if (overlayService != null) {
                                OverlayHelper.launch(context, overlayService)
                            } else {
                                isFloating = true
                                isExpanded = false
                                if (scope.displayAppName) isAppIcon = true
                                showFooterPopup = false
                            }
                        },
                        coroutineScope = coroutineScope,
                        onItemSelected = { selectedItem = it }
                    )
                } else {
                    val handleOverlayClick: (AzNavItem) -> Unit = { item ->
                        if (overlayController != null && item.route != null) {
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                launchIntent.putExtra(AzNavRail.EXTRA_ROUTE, item.route)
                                context.startActivity(launchIntent)
                            } else {
                                AzNavRailLogger.e("AzNavRail", "Could not find launch intent for package $packageName")
                            }
                        } else {
                            scope.onClickMap[item.id]?.invoke()
                        }
                    }
                    val effectiveNavController = if (overlayController != null) null else navController

                    // Infer Visual Side based on Orientation + Docking
                    // Ideally we should pass this in, but for now we infer:
                    val inferredVisualSide = if (isVertical) {
                        if (isRightDocked) AzVisualSide.RIGHT
                        else AzVisualSide.LEFT
                    } else {
                        // If horizontal, assuming standard top/bottom logic based on docking
                        // However, AzNavHost calculates this precisely.
                        // Since we don't have visualSide param passed fully yet, we estimate.
                        // But wait, RailItems needs visualSide for Popup.
                        // If I don't update AzNavRail signature to accept visualSide, I am guessing.
                        // AzNavHost logic:
                        // LEFT -> 90 -> BOTTOM
                        // LEFT -> 270 -> TOP
                        // RIGHT -> 90 -> TOP
                        // RIGHT -> 270 -> BOTTOM
                        // The `isRightDocked` flag here comes from `visualDockingSide` passed from `AzNavHost`.
                        // In AzNavHost: `visualDockingSideProxy` is RIGHT if (BOTTOM or RIGHT).
                        // So if `isRightDocked` (proxy) is true: it could be Right (Vertical) or Bottom (Horizontal).
                        // Wait, logic in AzNavHost:
                        // val visualDockingSideProxy = if (visualSide == AzVisualSide.BOTTOM || visualSide == AzVisualSide.RIGHT) AzDockingSide.RIGHT else AzDockingSide.LEFT
                        // So inside AzNavRail:
                        // isRightDocked = true => VisualSide is RIGHT or BOTTOM.
                        // isRightDocked = false => VisualSide is LEFT or TOP.
                        // We also have `orientation`.
                        // If Vertical + isRightDocked -> RIGHT.
                        // If Vertical + !isRightDocked -> LEFT.
                        // If Horizontal + isRightDocked -> BOTTOM.
                        // If Horizontal + !isRightDocked -> TOP.
                        if (isRightDocked) AzVisualSide.BOTTOM else AzVisualSide.TOP
                    }

                    CollapsedRailContent(
                        scope = scope,
                        displayedNavItems = displayedNavItems,
                        navController = effectiveNavController,
                        currentDestination = currentDestination,
                        cyclerStates = cyclerStates,
                        hostStates = hostStates,
                        itemPositions = itemPositions,
                        buttonSize = buttonSize,
                        orientation = orientation,
                        visualSide = inferredVisualSide,
                        isRightDocked = isRightDocked,
                        disableSwipeToOpen = disableSwipeToOpen,
                        isFloating = isFloating,
                        showFloatingButtons = showFloatingButtons,
                        onHeightChanged = { railItemsHeight = it },
                        onOpen = { isExpanded = true },
                        onClickOverride = if (overlayController != null) handleOverlayClick else null,
                        onItemSelected = { navItem -> selectedItem = navItem },
                        reverseLayout = reverseLayout
                    )
                }
            }

            if (isVertical) {
                Column(Modifier.fillMaxHeight(), verticalArrangement = if (reverseLayout) androidx.compose.foundation.layout.Arrangement.Bottom else androidx.compose.foundation.layout.Arrangement.Top) {
                    if (reverseLayout) {
                        Box(Modifier.weight(1f)) { contentBlock() }
                        headerContent()
                    } else {
                        headerContent()
                        Box(Modifier.weight(1f)) { contentBlock() }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (reverseLayout) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start) {
                    if (reverseLayout) {
                        Box(Modifier.weight(1f)) { contentBlock() }
                        headerContent()
                    } else {
                        headerContent()
                        Box(Modifier.weight(1f)) { contentBlock() }
                    }
                }
            }
        }

        if (showFooterPopup && effectiveNoMenu) {
            Popup(onDismissRequest = { showFooterPopup = false }, popupPositionProvider = CenteredPopupPositionProvider) {
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Footer(
                        appName = appName,
                        onToggle = { showFooterPopup = false },
                        onUndock = {
                            val overlayService = scope.overlayService
                            if (scope.onUndock != null) {
                                scope.onUndock?.invoke()
                            } else if (overlayService != null) {
                                OverlayHelper.launch(context, overlayService)
                            } else {
                                isFloating = true
                                isExpanded = false
                                if (scope.displayAppName) isAppIcon = true
                                showFooterPopup = false
                            }
                        },
                        scope = scope,
                        footerColor = if (footerColor != Color.Unspecified) footerColor else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (scope.infoScreen) {
            HelpOverlay(
                items = displayedNavItems,
                itemPositions = itemPositions,
                hostStates = hostStates,
                railWidth = railThickness,
                onDismiss = { scope.onDismissInfoScreen?.invoke() },
                isRightDocked = isRightDocked,
                safeZones = LocalAzSafeZones.current
            )
        }
    }
}
