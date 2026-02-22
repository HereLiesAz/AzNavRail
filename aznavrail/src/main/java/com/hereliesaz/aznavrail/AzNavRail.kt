package com.hereliesaz.aznavrail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzLayoutConfig
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.RailContent
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlin.math.roundToInt

@RequiresOptIn(message = "AzNavRail must be used within AzHostActivityLayout to ensure safe zones and proper behavior. Direct usage is discouraged unless building a System Overlay.")
@Retention(AnnotationRetention.BINARY)
annotation class AzStrictLayout

@Composable
fun AzNavRail(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    currentDestination: String? = null,
    isLandscape: Boolean? = null,
    initiallyExpanded: Boolean = false,
    disableSwipeToOpen: Boolean = false,
    providedScope: AzNavRailScopeImpl? = null,
    orientation: AzOrientation = AzOrientation.Vertical,
    visualDockingSide: AzDockingSide = AzDockingSide.LEFT,
    railAlignment: Alignment = Alignment.TopStart,
    reverseLayout: Boolean = false,
    content: AzNavRailScope.() -> Unit
) {
    // STRICT PROTOCOL: Check for Host
    val isHostPresent = LocalAzNavHostPresent.current
    if (!isHostPresent) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Red)) {
            Text(
                "AzNavRail Error: Must be used inside AzHostActivityLayout",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    // 1. Scope Resolution
    val scope = providedScope ?: remember { AzNavRailScopeImpl() }.apply(content)
    
    // 2. State
    var isExpanded by remember { mutableStateOf(initiallyExpanded && !scope.noMenu) }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    var railContentHeight by remember { mutableStateOf(0f) }
    
    // Sync scope config updates
    val railWidth by animateDpAsState(
        targetValue = if (isExpanded) scope.expandedWidth else scope.collapsedWidth,
        animationSpec = tween(300),
        label = "RailWidth"
    )

    // Navigation State
    val effectiveNavController = navController ?: rememberNavController()
    val navBackStackEntry by effectiveNavController.currentBackStackEntryAsState()
    val actualCurrentDestination = currentDestination ?: navBackStackEntry?.destination?.route

    // Host items expansion state
    val hostStates = remember { mutableMapOf<String, Boolean>() }

    // Helpers
    fun toggleExpanded() {
        if (!scope.infoScreen) {
            if (scope.noMenu && !isFloating) {
                if (isFloating) {
                    showFloatingButtons = !showFloatingButtons
                }
            } else if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else {
                isExpanded = !isExpanded
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Gestures
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { },
            onDrag = { change, dragAmount ->
                change.consume()
                if (isFloating) {
                    offsetX += dragAmount.x
                    
                    // Floating Rail Constraint: Keep strictly between 10% and 90% vertical
                    val minY = screenHeightPx * 0.1f
                    // If height is known, clamp bottom edge to 90%, otherwise just clamp top anchor to 90%
                    val maxY = if (railContentHeight > 0) {
                        (screenHeightPx * 0.9f) - railContentHeight
                    } else {
                        screenHeightPx * 0.9f
                    }
                    
                    // We assume initial anchored position is 0 (Top).
                    // We need to track total offset relative to that.
                    val proposedY = offsetY + dragAmount.y
                    
                    // Strict clamping
                    if (proposedY < minY) {
                        offsetY = minY
                    } else if (proposedY > maxY) {
                        offsetY = maxY
                    } else {
                        offsetY = proposedY
                    }
                } else if (!disableSwipeToOpen) {
                    // Standard Drawer Swipe Logic:
                    // Swipe OUT (Away from dock) -> Open Menu
                    // Swipe IN (Towards dock) -> Close Menu
                    
                    val isSwipeOut = if (visualDockingSide == AzDockingSide.LEFT) dragAmount.x > 0 else dragAmount.x < 0
                    val isSwipeIn = if (visualDockingSide == AzDockingSide.LEFT) dragAmount.x < 0 else dragAmount.x > 0
                    
                    if (isSwipeOut && !isExpanded) {
                        // Check threshold
                        if (kotlin.math.abs(dragAmount.x) > 5) { // Sensitivity
                            isExpanded = true
                        }
                    } else if (isSwipeIn && isExpanded) {
                        if (kotlin.math.abs(dragAmount.x) > 5) {
                            isExpanded = false
                        }
                    }
                }
            },
            onDragEnd = {
                if (isFloating && offsetX * offsetX + offsetY * offsetY < AzNavRailDefaults.SNAP_BACK_RADIUS_PX * AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                    isFloating = false
                    offsetX = 0f
                    offsetY = 0f
                    if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    }

    // Header Icon logic: Tap to toggle menu, Long-press to undock
    val headerIconModifier = Modifier
        .size(AzNavRailDefaults.HeaderIconSize)
        .clip(RectangleShape)
        .background(Color.Gray) // Placeholder color
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    if (scope.enableRailDragging) {
                        if (isFloating) {
                            // Dock
                            isFloating = false 
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            // Float
                            isFloating = true 
                            isExpanded = false
                            // Initial Placement: Snap to safe zone if 0 is invalid
                            val safeTop = screenHeightPx * 0.1f
                            offsetY = safeTop
                        }
                        if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.onUndock?.invoke()
                    }
                },
                onTap = { toggleExpanded() }
            )
        }

    // Determine dimensions based on state
    // When floating, we wrap content. When docked, full height (minus safe zones applied via padding).
    val sizeModifier = if (isFloating) {
        if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).wrapContentHeight()
        else Modifier.height(railWidth).wrapContentWidth()
    } else {
        if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).fillMaxHeight()
        else Modifier.height(railWidth).fillMaxWidth()
    }

    // Apply strict vertical padding when DOCKED to ensure rail stays out of 10%/10% zones.
    // Floating rail handles this via drag clamping.
    val verticalPaddingModifier = if (!isFloating && orientation == AzOrientation.Vertical) {
        val safeTopDp = (configuration.screenHeightDp * 0.1f).dp
        val safeBottomDp = (configuration.screenHeightDp * 0.1f).dp
        Modifier.padding(top = safeTopDp, bottom = safeBottomDp)
    } else {
        Modifier
    }

    // Layout
    Box(
        modifier = modifier,
        contentAlignment = if (isFloating) Alignment.TopStart else railAlignment
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(verticalPaddingModifier) // Apply 10% safe zones when docked
                .then(sizeModifier)
                .then(dragModifier)
                .onGloballyPositioned { coordinates ->
                    if (isFloating) {
                        railContentHeight = coordinates.size.height.toFloat()
                    }
                }
                .then(if (isFloating) Modifier.shadow(8.dp, RectangleShape) else Modifier),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RectangleShape
        ) {
            val isVertical = orientation == AzOrientation.Vertical
            
            @Composable
            fun RailContentContainer(modifier: Modifier = Modifier) {
                // CONTENT WRAPPER WITH MODIFIER
                Box(modifier = modifier) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // HEADER
                        Row(
                            modifier = Modifier.padding(AzNavRailDefaults.HeaderPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = headerIconModifier, contentAlignment = Alignment.Center) {
                                Text("App", color = Color.White, fontSize = 10.sp)
                            }
                            
                            if (isExpanded && scope.displayAppName) {
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (context.applicationInfo.labelRes != 0) context.getString(context.applicationInfo.labelRes) else "App",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isFloating && !showFloatingButtons) return@Column

                        // CONTENT
                        Box(modifier = Modifier.weight(1f)) {
                            RailContent(
                                items = scope.navItems,
                                currentDestination = actualCurrentDestination,
                                activeColor = scope.activeColor,
                                shape = scope.defaultShape,
                                activeClassifiers = scope.activeClassifiers,
                                scope = scope,
                                navController = effectiveNavController,
                                onItemSelected = { item ->
                                    scope.onClickMap[item.id]?.invoke()
                                    if (item.route != null) {
                                        effectiveNavController.navigate(item.route) {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    if (item.collapseOnClick && !scope.noMenu) isExpanded = false
                                },
                                hostStates = hostStates,
                                packRailButtons = scope.packButtons,
                                orientation = orientation,
                                onItemGloballyPositioned = scope.onItemGloballyPositioned,
                                infoScreen = scope.infoScreen,
                                reverseLayout = reverseLayout,
                                isRightDocked = visualDockingSide == AzDockingSide.RIGHT
                            )
                        }

                        // FOOTER
                        if (scope.showFooter && isExpanded) {
                            Footer(
                                appName = if (context.applicationInfo.labelRes != 0) context.getString(context.applicationInfo.labelRes) else "App",
                                onToggle = { toggleExpanded() },
                                onUndock = { 
                                    isFloating = true 
                                    isExpanded = false
                                    // Start floating at safe zone
                                    val safeTop = screenHeightPx * 0.1f
                                    offsetY = safeTop
                                    scope.onUndock?.invoke()
                                },
                                scope = scope,
                                footerColor = scope.activeColor
                            )
                        } else if (!isExpanded && !isFloating) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Secret Trigger
                            if (BuildConfig.GENERATED_SEC_LOC_PIN.isNotEmpty()) {
                                val trigger = SecretScreens(BuildConfig.GENERATED_SEC_LOC_PIN)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(onLongPress = { trigger() })
                                        }
                                )
                            }
                        }
                    }
                }
            }

            if (isVertical) {
                // If floating, we don't weight the container so it collapses to content
                if (isFloating) {
                    Column { RailContentContainer() }
                } else {
                    Column { RailContentContainer(Modifier.weight(1f)) }
                }
            } else {
                if (isFloating) {
                    Row { RailContentContainer() }
                } else {
                    Row { RailContentContainer(Modifier.weight(1f)) }
                }
            }
        }
    }

    // Info Screen Overlay
    if (scope.infoScreen) {
        HelpOverlay(
            items = scope.navItems,
            onDismiss = { scope.onDismissInfoScreen?.invoke() }
        )
    }
}
