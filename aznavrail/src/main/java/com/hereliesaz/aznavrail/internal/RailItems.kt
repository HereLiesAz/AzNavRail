// aznavrail/src/main/java/com/hereliesaz/aznavrail/internal/RailItems.kt
package com.hereliesaz.aznavrail.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzOrientation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal fun RailItems(
    items: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    buttonSize: Dp,
    onRailCyclerClick: (AzNavItem) -> Unit,
    onItemSelected: (AzNavItem) -> Unit,
    hostStates: MutableMap<String, Boolean>,
    packRailButtons: Boolean,
    orientation: AzOrientation = AzOrientation.Vertical,
    onClickOverride: ((AzNavItem) -> Unit)? = null,
    onItemGloballyPositioned: ((String, Rect) -> Unit)? = null,
    infoScreen: Boolean = false,
    reverseLayout: Boolean = false,
    activeColor: Color,
    defaultShape: AzButtonShape
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp
    
    val maxRailHeight = screenHeight * 0.8f
    val maxRailWidth = screenWidth * 0.8f

    val topLevelItems = items.filter { !it.isSubItem }
    val baseItems = if (packRailButtons) topLevelItems.filter { it.isRailItem } else topLevelItems
    val itemsToRender = if (reverseLayout) baseItems.reversed() else baseItems

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val itemHeights = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>() }
    val itemWidths = remember { androidx.compose.runtime.mutableStateMapOf<String, Int>() }
    val itemBounds = remember { androidx.compose.runtime.mutableStateMapOf<String, Rect>() }
    var hiddenMenuOpenId by remember { mutableStateOf<String?>(null) }
    var nestedRailOpenId by remember { mutableStateOf<String?>(null) }
    var capturedAnchorBounds by remember { mutableStateOf<Rect?>(null) }
    var currentDropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var rootBounds by remember { mutableStateOf<Rect?>(null) }

    val snappingOffsets = remember { androidx.compose.runtime.mutableStateMapOf<String, Animatable<Float, androidx.compose.animation.core.AnimationVector1D>>() }
    var lastTappedId by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val isVertical = orientation == AzOrientation.Vertical
    val currentItemSizes = if (isVertical) itemHeights else itemWidths

    val renderItem: @Composable (AzNavItem) -> Unit = { item ->
        key(item.id) {
            if (item.isRailItem) {
                DraggableRailItemWrapper(
                    item = item, scope = scope, navController = navController, currentDestination = currentDestination,
                    buttonSize = buttonSize, orientation = orientation, onRailCyclerClick = onRailCyclerClick,
                    onItemSelected = onItemSelected, hostStates = hostStates, onClickOverride = onClickOverride,
                    onItemGloballyPositioned = onItemGloballyPositioned, infoScreen = infoScreen, draggedItemId = draggedItemId,
                    dragOffset = dragOffset, currentDropTargetIndex = currentDropTargetIndex,
                    onDragStart = { id -> draggedItemId = id; currentDropTargetIndex = scope.navItems.indexOfFirst { it.id == id } },
                    onDragEnd = { draggedItemId = null; dragOffset = 0f; currentDropTargetIndex = null },
                    onDragDelta = { delta -> dragOffset += delta }, onDragTargetChange = { index -> currentDropTargetIndex = index },
                    onMenuOpen = { id -> hiddenMenuOpenId = id }, itemSizes = currentItemSizes, itemWidths = itemWidths, itemBounds = itemBounds,
                    onHeightReported = { id, height -> itemHeights[id] = height }, onWidthReported = { id, width -> itemWidths[id] = width },
                    onBoundsReported = { id, bounds -> itemBounds[id] = bounds }, coroutineScope = coroutineScope, hiddenMenuOpenId = hiddenMenuOpenId,
                    nestedRailOpenId = nestedRailOpenId, onNestedRailToggle = { id -> if (nestedRailOpenId == id) { nestedRailOpenId = null; capturedAnchorBounds = null } else { nestedRailOpenId = id; capturedAnchorBounds = itemBounds[id] } },
                    onHiddenMenuDismiss = { hiddenMenuOpenId = null }, lastTappedId = lastTappedId, onUpdateLastTappedId = { id -> lastTappedId = id },
                    snappingOffset = snappingOffsets[item.id]?.value, activeColor = activeColor, defaultShape = defaultShape
                )
            }
        }
    }

    Box(modifier = Modifier.onGloballyPositioned { rootBounds = it.boundsInWindow() }) {
        if (isVertical) {
            Column(modifier = Modifier.heightIn(max = maxRailHeight).verticalScroll(rememberScrollState())) { itemsToRender.forEach { renderItem(it) } }
        } else {
            Row(modifier = Modifier.widthIn(max = maxRailWidth).horizontalScroll(rememberScrollState())) { itemsToRender.forEach { renderItem(it) } }
        }

        if (nestedRailOpenId != null) {
            rootBounds?.let { rb ->
                val item = items.find { it.id == nestedRailOpenId }
                val bounds = if (item?.nestedRailAlignment == com.hereliesaz.aznavrail.model.AzNestedRailAlignment.VERTICAL) capturedAnchorBounds else itemBounds[nestedRailOpenId]
                if (item != null && bounds != null && item.nestedRailItems != null && item.nestedRailAlignment != null) {
                     NestedRail(parentItem = item, items = item.nestedRailItems!!, scope = scope, navController = navController, currentDestination = currentDestination, anchorBounds = bounds, rootBounds = rb, onDismiss = { nestedRailOpenId = null; capturedAnchorBounds = null }, isRightDocked = scope.dockingSide == AzDockingSide.RIGHT, onItemSelected = onItemSelected, activeColor = activeColor, defaultShape = defaultShape)
                }
            }
        }
    }
}

@Composable
private fun DraggableRailItemWrapper(
    item: AzNavItem, scope: AzNavRailScopeImpl, navController: NavController?, currentDestination: String?, buttonSize: Dp,
    orientation: AzOrientation, onRailCyclerClick: (AzNavItem) -> Unit, onItemSelected: (AzNavItem) -> Unit, hostStates: MutableMap<String, Boolean>,
    onClickOverride: ((AzNavItem) -> Unit)?, onItemGloballyPositioned: ((String, Rect) -> Unit)?, infoScreen: Boolean, draggedItemId: String?,
    dragOffset: Float, currentDropTargetIndex: Int?, onDragStart: (String) -> Unit, onDragEnd: () -> Unit, onDragDelta: (Float) -> Unit,
    onDragTargetChange: (Int) -> Unit, onMenuOpen: (String) -> Unit, itemSizes: Map<String, Int>, itemWidths: Map<String, Int>, itemBounds: Map<String, Rect>,
    onHeightReported: (String, Int) -> Unit, onWidthReported: (String, Int) -> Unit, onBoundsReported: (String, Rect) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope, hiddenMenuOpenId: String?, nestedRailOpenId: String?, onNestedRailToggle: (String) -> Unit,
    onHiddenMenuDismiss: () -> Unit, lastTappedId: String?, onUpdateLastTappedId: (String) -> Unit, snappingOffset: Float?, activeColor: Color, defaultShape: AzButtonShape
) {
    val isSelected = item.route != null && item.route == currentDestination
    val isClassifierActive = item.classifiers.any { it in scope.activeClassifiers }
    val isVisuallyActive = isSelected || isClassifierActive || lastTappedId == item.id

    Box {
         RailContent(
             item = item, navController = navController, isSelected = isVisuallyActive, buttonSize = buttonSize,
             onClick = { scope.onFocusMap[item.id]?.invoke(); if (item.isNestedRail) onNestedRailToggle(item.id) else if (onClickOverride != null) onClickOverride(item) else scope.onClickMap[item.id]?.invoke() },
             onRailCyclerClick = onRailCyclerClick, onItemClick = { onItemSelected(item) },
             onHostClick = { hostStates[item.id] = !(hostStates[item.id] ?: false) },
             onItemGloballyPositioned = onItemGloballyPositioned, infoScreen = infoScreen,
             dragModifier = Modifier.onGloballyPositioned { onBoundsReported(item.id, it.boundsInWindow()) },
             activeColor = activeColor, shape = defaultShape
         )
    }
}
