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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.internal.AzNavRailDefaults
import com.hereliesaz.aznavrail.internal.AzNavRailLogger
import com.hereliesaz.aznavrail.internal.Footer
import com.hereliesaz.aznavrail.internal.HelpOverlay
import com.hereliesaz.aznavrail.internal.RailContent
import com.hereliesaz.aznavrail.internal.SecretScreens
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzHeaderIconShape
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlin.math.roundToInt

@RequiresOptIn(message = "AzNavRail must be used within AzHostActivityLayout to ensure safe zones and proper behavior. Direct usage is discouraged unless building a System Overlay.")
@Retention(AnnotationRetention.BINARY)
annotation class AzStrictLayout

internal const val noTitle = "NO_TITLE_AZ_NAV_RAIL"

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
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // 1. Scope Resolution
    val scope = providedScope ?: remember { AzNavRailScopeImpl() }.apply(content)
    
    // 2. State
    var isExpanded by remember { mutableStateOf(initiallyExpanded && !scope.noMenu) }
    var isFloating by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showFloatingButtons by remember { mutableStateOf(false) }
    
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
                // If menu is disabled, maybe toggle floating buttons or just do nothing?
                // For now, no-op or custom logic if needed.
            } else if (isFloating) {
                showFloatingButtons = !showFloatingButtons
            } else {
                isExpanded = !isExpanded
            }
            if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Gestures
    val dragModifier = if (scope.enableRailDragging) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    if (!isFloating) {
                        isFloating = true
                        isExpanded = false
                        if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.onUndock?.invoke() // Notify external listeners
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (isFloating) {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    } else if (!disableSwipeToOpen) {
                        // Simple swipe to open/close logic
                        if (dragAmount.x > AzNavRailDefaults.SWIPE_THRESHOLD_PX && !isExpanded && visualDockingSide == AzDockingSide.LEFT) {
                            isExpanded = true
                        } else if (dragAmount.x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX && isExpanded && visualDockingSide == AzDockingSide.LEFT) {
                            isExpanded = false
                        }
                        // Mirror for right docking
                        if (dragAmount.x < -AzNavRailDefaults.SWIPE_THRESHOLD_PX && !isExpanded && visualDockingSide == AzDockingSide.RIGHT) {
                            isExpanded = true
                        } else if (dragAmount.x > AzNavRailDefaults.SWIPE_THRESHOLD_PX && isExpanded && visualDockingSide == AzDockingSide.RIGHT) {
                            isExpanded = false
                        }
                    }
                },
                onDragEnd = {
                    // Snap back logic if close to origin
                    if (isFloating && offsetX * offsetX + offsetY * offsetY < AzNavRailDefaults.SNAP_BACK_RADIUS_PX * AzNavRailDefaults.SNAP_BACK_RADIUS_PX) {
                        isFloating = false
                        offsetX = 0f
                        offsetY = 0f
                        if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )
        }
    } else Modifier

    val headerIconModifier = Modifier
        .size(AzNavRailDefaults.HeaderIconSize)
        .clip(
            when (scope.headerIconShape) {
                AzHeaderIconShape.CIRCLE -> CircleShape
                AzHeaderIconShape.ROUNDED -> RoundedCornerShape(12.dp)
                AzHeaderIconShape.NONE -> RectangleShape
                else -> CircleShape
            }
        )
        .background(Color.Gray) // Placeholder color
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    if (scope.enableRailDragging) {
                        if (isFloating) {
                            isFloating = false // Dock
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            isFloating = true // Float
                            isExpanded = false
                        }
                        if (scope.vibrate) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.onUndock?.invoke()
                    }
                },
                onTap = { toggleExpanded() }
            )
        }

    // Layout
    Box(
        modifier = modifier,
        contentAlignment = if (isFloating) Alignment.TopStart else railAlignment
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(if (orientation == AzOrientation.Vertical) Modifier.width(railWidth).fillMaxHeight() else Modifier.fillMaxWidth().height(railWidth))
                .then(dragModifier)
                .then(if (isFloating) Modifier.shadow(8.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)) else Modifier),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            val isVertical = orientation == AzOrientation.Vertical
            
            @Composable
            fun RailContentContainer() {
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

                if (isFloating && !showFloatingButtons) return

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
                            if (item.onClick != null) item.onClick.invoke()
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
                            scope.onUndock?.invoke()
                        },
                        scope = scope,
                        footerColor = scope.activeColor
                    )
                } else if (!isExpanded && !isFloating) {
                    // Small footer or spacer? Usually just spacer in rail mode unless custom logic
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

            if (isVertical) {
                Column { RailContentContainer() }
            } else {
                Row { RailContentContainer() }
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
