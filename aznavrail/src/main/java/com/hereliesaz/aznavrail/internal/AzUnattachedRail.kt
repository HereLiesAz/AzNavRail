package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.AzTextBoxDefaults
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzNestedRailAlignment
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Every id in the unattached hosts' subtrees — the hosts themselves plus their sub-items to any
 * depth. The rail strip and the drawer filter these out, because an unattached host has left the
 * rail: it draws itself (and unfolds its children) wherever its anchor puts it.
 */
internal fun azUnattachedSubtreeIds(items: List<AzNavItem>): Set<String> {
    val roots = items.filter { it.isUnattached }.map { it.id }
    if (roots.isEmpty()) return emptySet()
    val ids = roots.toMutableSet()
    // Sub-items reference their host by id, so one sweep per level resolves the whole tree. Bounded
    // by the item count, which also stops a malformed cycle from spinning forever.
    repeat(items.size) {
        val grew = items.any { it.isSubItem && it.hostId in ids && ids.add(it.id) }
        if (!grew) return ids
    }
    return ids
}

/**
 * Draws the [AzUnattachedAnchor] stacks — the rail host items declared with `azUnattachedHostItem`,
 * which live outside the rail strip.
 *
 * Hosts sharing the [AzUnattachedAnchor.OPPOSITE] or [AzUnattachedAnchor.BOTTOM] anchor stack into a
 * single fixed column in declaration order, spaced exactly like rail items (and packed when the rail
 * is packed), same as always. [AzUnattachedAnchor.FLOATING] hosts are different: each one floats and
 * docks **independently** (see [FloatingDockGroup]) — dragging one near a screen edge pins it there,
 * dragging it flush against another floating rail attaches it to that rail instead, and a rail with
 * at least one other rail attached to it grows a grab bar so the pair can be dragged as a unit.
 *
 * @param scope The rail scope supplying items, callbacks and theme tokens.
 * @param visualDockingSide The side the rail is currently drawn on; the [AzUnattachedAnchor.BOTTOM]
 *   and [AzUnattachedAnchor.OPPOSITE] stacks park on the other one.
 */
@Composable
internal fun AzUnattachedRail(
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    visualDockingSide: AzDockingSide,
    modifier: Modifier = Modifier,
) {
    val unattached = scope.navItems.filter { it.isUnattached }
    if (unattached.isEmpty()) return

    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    // Expansion state is owned here, not by the rail: an unattached host is not in the rail's
    // hostStates map and must keep unfolding even while the rail's own menu is closed.
    val hostStates = remember { mutableStateMapOf<String, Boolean>() }
    val initiallyExpandedSeen = remember { mutableMapOf<String, Boolean>() }
    val expandWhenSeen = remember { mutableMapOf<String, Boolean>() }
    val cyclerJobs = remember { mutableStateMapOf<String, Job>() }

    // A relocatable item's hidden menu (long-press), shared across every anchor stack — only one can
    // be open at a time, same as the rail strip.
    var hiddenMenuOpenId by remember { mutableStateOf<String?>(null) }
    val onMenuOpen: (String) -> Unit = { id -> hiddenMenuOpenId = id }
    val onHiddenMenuDismiss: () -> Unit = { hiddenMenuOpenId = null }

    val subtreeIds = remember(scope.navItems.toList()) { azUnattachedSubtreeIds(scope.navItems) }

    // Rising-edge auto-expand, mirroring the rail: expand when `initiallyExpanded` flips true, then
    // leave the user alone.
    LaunchedEffect(scope.navItems) {
        scope.navItems.forEach { item ->
            if (item.isHost && item.id in subtreeIds) {
                if (item.initiallyExpanded && initiallyExpandedSeen[item.id] != true) {
                    hostStates[item.id] = true
                }
                initiallyExpandedSeen[item.id] = item.initiallyExpanded
            }
        }
    }

    // Reactive expansion for unattached hosts — same edge-triggered contract as the rail's, and the
    // same snapshot-plus-poll observation so plain (non-snapshot) sources still drive it.
    val expandWhenKeys = scope.expandWhenMap.keys.filter { it in subtreeIds }.sorted().joinToString(",")
    LaunchedEffect(expandWhenKeys) {
        scope.expandWhenMap.toMap().forEach { (id, cond) ->
            if (id !in subtreeIds) return@forEach
            launch {
                merge(
                    snapshotFlow { cond() },
                    flow { while (true) { emit(cond()); delay(300) } },
                ).distinctUntilChanged().collect { conditionNow ->
                    val before = expandWhenSeen[id]
                    when {
                        before == null -> if (conditionNow) hostStates[id] = true
                        before != conditionNow -> hostStates[id] = conditionNow
                    }
                    expandWhenSeen[id] = conditionNow
                }
            }
        }
    }

    val buttonSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth
    val spacing = if (scope.packButtons) 0.dp else AzNavRailDefaults.RailContentVerticalArrangement
    val railOnLeft = visualDockingSide == AzDockingSide.LEFT
    val safeInset = with(density) { (screenHeightPx * 0.1f).toDp() }

    val onCyclerClick: (AzNavItem) -> Unit = { item ->
        if (!item.disabled) {
            cyclerJobs[item.id]?.cancel()
            val options = item.options ?: emptyList()
            val enabled = options.filterNot { it in (item.disabledOptions ?: emptyList()) }
            if (enabled.isNotEmpty()) {
                val current = scope.transientCyclerOptions[item.id] ?: item.selectedOption
                val next = enabled[(enabled.indexOf(current) + 1).mod(enabled.size)]
                scope.transientCyclerOptions[item.id] = next
                // Commit after the same 1s "stop" window the rail's cyclers use.
                cyclerJobs[item.id] = coroutineScope.launch {
                    delay(1000L)
                    scope.onClickMap[item.id]?.invoke()
                    cyclerJobs.remove(item.id)
                }
            }
            scope.advancedConfig.onInteraction?.invoke(item.id, item)
        }
    }

    val byAnchor = unattached.groupBy { it.unattachedAnchor ?: AzUnattachedAnchor.OPPOSITE }

    Box(modifier = modifier.fillMaxSize()) {
        byAnchor[AzUnattachedAnchor.OPPOSITE]?.let { hosts ->
            UnattachedStack(
                hosts = hosts,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(if (railOnLeft) Alignment.TopEnd else Alignment.TopStart)
                    .padding(top = safeInset, bottom = safeInset),
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = onMenuOpen,
                onHiddenMenuDismiss = onHiddenMenuDismiss,
                // This stack sits at the corner opposite the main rail, so a nested-rail popup opens
                // back toward the rail (away from the screen edge) exactly like the rail's own items.
                popupOpensLeft = railOnLeft,
            )
        }

        byAnchor[AzUnattachedAnchor.BOTTOM]?.let { hosts ->
            UnattachedStack(
                hosts = hosts,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(if (railOnLeft) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(top = safeInset, bottom = safeInset),
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = onMenuOpen,
                onHiddenMenuDismiss = onHiddenMenuDismiss,
                popupOpensLeft = railOnLeft,
            )
        }

        byAnchor[AzUnattachedAnchor.FLOATING]?.let { hosts ->
            FloatingDockGroup(
                hosts = hosts,
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = onMenuOpen,
                onHiddenMenuDismiss = onHiddenMenuDismiss,
                railOnLeft = railOnLeft,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                context = context,
            )
        }
    }
}

/** One anchor's column of unattached hosts, each unfolding its own sub-items beneath it. */
@Composable
private fun UnattachedStack(
    hosts: List<AzNavItem>,
    modifier: Modifier,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    hostStates: MutableMap<String, Boolean>,
    buttonSize: Dp,
    spacingDp: Dp,
    onCyclerClick: (AzNavItem) -> Unit,
    /** The id of the relocatable item whose hidden menu is currently open, if any. */
    hiddenMenuOpenId: String?,
    /** Invoked with a relocatable item's id to open its hidden menu (long-press). */
    onMenuOpen: (String) -> Unit,
    /** Invoked to close whichever relocatable item's hidden menu is open. */
    onHiddenMenuDismiss: () -> Unit,
    /** Whether a nested-rail popup opened from this stack should open to the left of its parent. */
    popupOpensLeft: Boolean,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacingDp),
    ) {
        hosts.forEach { host ->
            UnattachedNode(
                item = host,
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                onCyclerClick = onCyclerClick,
                hiddenMenuOpenId = hiddenMenuOpenId,
                onMenuOpen = onMenuOpen,
                onHiddenMenuDismiss = onHiddenMenuDismiss,
                popupOpensLeft = popupOpensLeft,
            )
        }
    }
}

/**
 * One unattached item — plus, when it is an expanded host, its rail sub-items beneath it. Emitted
 * flat into the enclosing stack's [Column] so a host and its children share the rail's spacing.
 */
@Composable
private fun UnattachedNode(
    item: AzNavItem,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    hostStates: MutableMap<String, Boolean>,
    buttonSize: Dp,
    onCyclerClick: (AzNavItem) -> Unit,
    hiddenMenuOpenId: String?,
    onMenuOpen: (String) -> Unit,
    onHiddenMenuDismiss: () -> Unit,
    popupOpensLeft: Boolean,
) {
    // A cycler shows its transient option while the commit window is still running, exactly as it
    // does on the rail.
    val displayItem =
        if (item.isCycler) item.copy(selectedOption = scope.transientCyclerOptions[item.id] ?: item.selectedOption)
        else item

    // `RailContent` unconditionally nulls a relocatable item's `onClick`, expecting tap/long-press to
    // be detected by an externally-supplied `dragModifier` instead (see `RailItems.kt`'s own gesture
    // for the rail strip). Without one here, a relocatable item under an unattached host rendered but
    // was 100% inert to touch. Drag-to-reorder is deliberately not replicated — see the KDoc on
    // `azRailRelocItem`'s `onRelocate` parameter — but tap and long-press-to-open-hidden-menu are.
    val dragModifier = if (item.isRelocItem) {
        rememberRelocTapGestureModifier(
            item = item,
            vibrateOnLongPress = scope.vibrate,
            onTap = {
                scope.onFocusMap[item.id]?.invoke()
                scope.lastTouchedItemId = item.id
                scope.onClickMap[item.id]?.invoke()
                scope.advancedConfig.onInteraction?.invoke(item.id, item)
            },
            onLongPress = {
                if (!item.hiddenMenuItems.isNullOrEmpty()) {
                    scope.onFocusMap[item.id]?.invoke()
                    onMenuOpen(item.id)
                }
            },
        )
    } else {
        Modifier
    }

    RailContent(
        defaultShape = scope.defaultShape,
        item = displayItem,
        navController = navController,
        isSelected = (item.route != null && item.route == currentDestination) ||
            item.classifiers.any { scope.activeClassifiers.contains(it) },
        buttonSize = buttonSize,
        onClick = {
            scope.lastTouchedItemId = item.id
            if (item.isNestedRail) {
                // Mirrors the rail strip's own plain-tap toggle (`RailItems.kt`'s
                // `DraggableRailItemWrapper`), minus `reflectSelectionInParent` — an unattached host's
                // nested rail always just opens/closes; it never fires a reflected child's action
                // directly instead.
                scope.nestedRailOpenId = if (scope.nestedRailOpenId == item.id) null else item.id
            }
            scope.onClickMap[item.id]?.invoke()
        },
        onRailCyclerClick = onCyclerClick,
        onItemClick = { scope.advancedConfig.onInteraction?.invoke(item.id, item) },
        onHostClick = {
            val expanded = !(hostStates[item.id] ?: false)
            hostStates[item.id] = expanded
            scope.onExpandedChangeMap[item.id]?.invoke(expanded)
        },
        onItemGloballyPositioned = scope.advancedConfig.onItemGloballyPositioned,
        onBoundsCalculated = { id, bounds -> scope.itemBoundsCache[id] = bounds },
        onBoundsCleared = { id -> scope.itemBoundsCache.remove(id) },
        activeColor = scope.railAccent,
        dragModifier = dragModifier,
        onSliderChange = { id, v -> scope.onSliderChangeMap[id]?.invoke(v) },
        onSliderRangeChange = { id, r -> scope.onSliderRangeChangeMap[id]?.invoke(r) },
    )

    if (item.isRelocItem && hiddenMenuOpenId == item.id && !item.hiddenMenuItems.isNullOrEmpty()) {
        val bounds = scope.itemBoundsCache[item.id] ?: Rect.Zero
        HiddenMenuPopup(
            items = item.hiddenMenuItems,
            onDismiss = {
                item.onHiddenMenuDismiss?.invoke()
                onHiddenMenuDismiss()
            },
            onItemClick = { menuItem ->
                scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                menuItem.route?.let { navController?.navigate(it) }
                item.onHiddenMenuDismiss?.invoke()
                onHiddenMenuDismiss()
            },
            onInputSubmit = { menuItem, value ->
                scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                item.onHiddenMenuDismiss?.invoke()
                onHiddenMenuDismiss()
            },
            backgroundColor = if (scope.translucentBackground != Color.Unspecified) scope.translucentBackground else AzTextBoxDefaults.getBackgroundColor(),
            backgroundOpacity = AzTextBoxDefaults.getBackgroundOpacity(),
            // Window-space right edge, not just the item's own width: unlike the rail strip (docked
            // flush to the screen edge, so its items' width IS their right-edge x), an unattached
            // item can sit anywhere the anchor puts it. Same reasoning as `NestedItemWrapper`.
            anchorWidth = bounds.right.toInt(),
            anchorTop = bounds.top.toInt(),
            accent = scope.railAccent,
        )
    }

    // A nested-rail popup previously had no rendering path at all under an unattached host (only the
    // rail strip's `DraggableRailItemWrapper` drew one) — a nested rail declared under an
    // `azUnattachedHostItem` toggled `scope.nestedRailOpenId` (see `onClick` above) but nothing ever
    // showed for it. [popupOpensLeft] lets each anchor/column pick a sensible side: see the call
    // sites in `AzUnattachedRail` and `FloatingDockGroup`.
    if (item.isNestedRail && scope.nestedRailOpenId == item.id) {
        val bounds = scope.itemBoundsCache[item.id] ?: Rect.Zero
        val anchorWidthPx = bounds.width.toInt()
        val density = LocalDensity.current
        if (item.nestedRailAlignment == AzNestedRailAlignment.VERTICAL) {
            Popup(
                popupPositionProvider = DockedCenteredPopupPositionProvider(popupOpensLeft, anchorWidthPx),
                onDismissRequest = { scope.nestedRailOpenId = null },
                properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false),
            ) {
                NestedRail(
                    parentItem = item,
                    items = item.nestedRailItems ?: emptyList(),
                    currentDestination = currentDestination,
                    activeColor = scope.railAccent,
                    activeClassifiers = scope.activeClassifiers,
                    focusColor = scope.focusColor,
                    secondaryColor = scope.secondaryColor,
                    tertiaryColor = scope.tertiaryColor,
                    onItemSelected = { subItem ->
                        scope.onClickMap[subItem.id]?.invoke()
                        subItem.route?.let { navController?.navigate(it) }
                        scope.advancedConfig.onInteraction?.invoke(subItem.id, subItem)
                        if (!item.keepNestedRailOpen) scope.nestedRailOpenId = null
                    },
                    alignment = item.nestedRailAlignment,
                    isRightDocked = popupOpensLeft,
                    helpList = scope.advancedConfig.helpList,
                    itemSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth,
                    hiddenMenuOpenId = hiddenMenuOpenId,
                    onMenuOpen = onMenuOpen,
                    onHiddenMenuDismiss = onHiddenMenuDismiss,
                    onHiddenMenuItemClick = { menuItem ->
                        scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                        menuItem.route?.let { navController?.navigate(it) }
                    },
                    onHiddenMenuInputSubmit = { menuItem, value ->
                        scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                    },
                    hiddenMenuBackgroundColor = scope.translucentBackground,
                )
            }
        } else {
            val marginPx = with(density) { 8.dp.roundToPx() }
            Popup(
                popupPositionProvider = DockedHorizontalPopupPositionProvider(popupOpensLeft, marginPx),
                onDismissRequest = { scope.nestedRailOpenId = null },
                properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = false),
            ) {
                NestedRail(
                    parentItem = item,
                    items = item.nestedRailItems ?: emptyList(),
                    currentDestination = currentDestination,
                    activeColor = scope.railAccent,
                    activeClassifiers = scope.activeClassifiers,
                    focusColor = scope.focusColor,
                    secondaryColor = scope.secondaryColor,
                    tertiaryColor = scope.tertiaryColor,
                    onItemSelected = { subItem ->
                        scope.onClickMap[subItem.id]?.invoke()
                        subItem.route?.let { navController?.navigate(it) }
                        scope.advancedConfig.onInteraction?.invoke(subItem.id, subItem)
                        if (!item.keepNestedRailOpen) scope.nestedRailOpenId = null
                    },
                    alignment = item.nestedRailAlignment ?: AzNestedRailAlignment.HORIZONTAL,
                    isRightDocked = popupOpensLeft,
                    helpList = scope.advancedConfig.helpList,
                    itemSize = if (scope.railItemWidth.isSpecified) scope.railItemWidth else AzNavRailDefaults.ButtonWidth,
                    hiddenMenuOpenId = hiddenMenuOpenId,
                    onMenuOpen = onMenuOpen,
                    onHiddenMenuDismiss = onHiddenMenuDismiss,
                    onHiddenMenuItemClick = { menuItem ->
                        scope.hiddenMenuOnClickMap[menuItem.id]?.invoke()
                        menuItem.route?.let { navController?.navigate(it) }
                    },
                    onHiddenMenuInputSubmit = { menuItem, value ->
                        scope.hiddenMenuOnValueChangeMap[menuItem.id]?.invoke(value)
                    },
                    hiddenMenuBackgroundColor = scope.translucentBackground,
                )
            }
        }
    }

    if (item.isHost && hostStates[item.id] == true) {
        // Every child, not just the rail-flavoured ones: an unattached host is removed from
        // the drawer along with its whole subtree, so this stack is the only place a
        // `azMenuSubItem` attached to it can be drawn at all.
        val children = scope.navItems.filter { it.isSubItem && it.hostId == item.id }
        children.forEachIndexed { index, child ->
            // The same staggered accordion the rail's own sub-items unfold on. The unfolding is
            // what tells you these belong to the host above them; blinking into place says nothing.
            Box(
                modifier = rememberAzAccordionModifier(
                    index = index,
                    count = children.size,
                    visible = true,
                    isHorizontal = false,
                    staggerMs = AzMotion.ItemStaggerMs,
                    durationMs = AzMotion.ItemDurationMs,
                )
            ) {
                UnattachedNode(
                    item = child,
                    scope = scope,
                    navController = navController,
                    currentDestination = currentDestination,
                    hostStates = hostStates,
                    buttonSize = buttonSize,
                    onCyclerClick = onCyclerClick,
                    hiddenMenuOpenId = hiddenMenuOpenId,
                    onMenuOpen = onMenuOpen,
                    onHiddenMenuDismiss = onHiddenMenuDismiss,
                    popupOpensLeft = popupOpensLeft,
                )
            }
        }
    }
}

/**
 * Detects a plain tap (fires [onTap]) vs. a long-press (fires [onLongPress]) on a relocatable item
 * rendered outside the rail strip. Mirrors the tap/long-press split `RailItems.kt`'s own
 * `dragModifier` performs for reloc items in the rail — minus the drag-to-reorder branch, which does
 * not apply here (see the KDoc on `azRailRelocItem`'s `onRelocate` parameter).
 */
@Composable
private fun rememberRelocTapGestureModifier(
    item: AzNavItem,
    vibrateOnLongPress: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val viewConfiguration = LocalViewConfiguration.current
    return Modifier.pointerInput(item.id) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            var isLongPress = false
            val longPressJob = coroutineScope.launch {
                delay(longPressTimeout)
                isLongPress = true
                if (vibrateOnLongPress) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onLongPress()
            }

            var hasMoved = false
            var gestureCompletedSuccessfully = false

            try {
                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                    val changedToUp = !change.pressed && change.previousPressed
                    if (changedToUp) {
                        change.consume()
                        gestureCompletedSuccessfully = true
                        break
                    }

                    if (!isLongPress) {
                        val positionChange = change.position - change.previousPosition
                        if (positionChange != Offset.Zero &&
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                        ) {
                            hasMoved = true
                            longPressJob.cancel()
                        }
                    }
                }
            } finally {
                longPressJob.cancel()
                // Fires on an ordinary quick tap, and also on a press held past the long-press
                // threshold that had no hidden menu to open for it (`onLongPress` is a no-op in that
                // case — see the call site's own `hiddenMenuItems` guard) — from the user's
                // perspective the latter is still just a slow tap, not a gesture to discard silently.
                // A press that legitimately opened a hidden menu is left alone: that menu is now the
                // interaction the user is looking at.
                if (!hasMoved && gestureCompletedSuccessfully &&
                    (!isLongPress || item.hiddenMenuItems.isNullOrEmpty())
                ) {
                    onTap()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// FLOATING: independent per-rail docking, rail-to-rail attachment, and group dragging.
// ---------------------------------------------------------------------------------------------

/** Which screen edge a top-level `FLOATING` rail is pinned to; [FREE] is unpinned. */
private typealias FloatingDock = AzFloatingDock

/**
 * Runtime state for one top-level `FLOATING` unattached host.
 *
 * Two independent things can position a rail: it can be pinned to a screen edge ([dock] /
 * [priority], used only while it has no rail-to-rail attachment of its own), or it can be attached
 * to another rail ([rightOf] / [belowOf], at most one of which is ever non-null) — in which case its
 * position is always computed relative to that rail (see `resolvedPosition` in [FloatingDockGroup]),
 * and [dock]/[priority]/[freeOffset] are simply ignored until it is detached again.
 *
 * Rail-to-rail attachments are deliberately NOT persisted across process death (only each rail's own
 * [dock]/[freeOffset]/[priority] are, via [AzUnattachedStore]) — safely rebuilding a whole attachment
 * graph on cold start (id renames, stale members, cycles) is a lot of extra edge-case handling for a
 * detail nobody will notice between sessions; every rail still reopens wherever it personally rested.
 */
private class AzFloatingRailState(
    dock: FloatingDock,
    freeOffset: Offset?,
    priority: Float,
) {
    var dock by mutableStateOf(dock)
    var freeOffset by mutableStateOf(freeOffset)
    /** Sort key among peers pinned to the same edge — a fraction of the window along that edge. */
    var priority by mutableStateOf(priority)
    /** This rail is docked to the right of the rail with this id, if any. At most one column over. */
    var rightOf by mutableStateOf<String?>(null)
    /** This rail is docked below the rail with this id, if any. */
    var belowOf by mutableStateOf<String?>(null)
    /** Measured size of this rail's own stack (host + its currently-unfolded children only). */
    var size by mutableStateOf(IntSize.Zero)
    var dragging by mutableStateOf(false)
    var liveDragOffset by mutableStateOf(Offset.Zero)
}

/**
 * Renders every top-level `FLOATING`-anchored unattached host as an independently draggable stack
 * (host + its own unfolded sub-items), layered with three cooperating behaviours:
 *
 * - **Screen-edge docking**: dragging a rail near the top, bottom, or the vertical edge opposite the
 *   main rail snaps it to that edge. Multiple rails pinned to the same edge line up adjacent to one
 *   another — sorted by where each was dropped along the edge — instead of overlapping, and an
 *   earlier rail expanding its sub-items pushes the ones after it along that edge to make room
 *   (edge order is a plain `Column`/row-style running sum over each rail's live measured size, so
 *   this falls out of ordinary recomposition rather than needing special-casing).
 * - **Rail-to-rail docking**: dragging one rail flush against the right or bottom edge of another
 *   attaches it there instead of the screen ([AzFloatingRailState.rightOf] /
 *   [AzFloatingRailState.belowOf]), forming a small grid that moves as a unit. Capped at two columns
 *   (a rail already docked to the right of something never accepts a rail to ITS right) and, per
 *   column, at however many rails can stack with every one of them fully expanded at once without
 *   running off-screen (a drop that would exceed that is refused — the rail stays free-floating at
 *   the drop point instead).
 * - **Group dragging**: a rail with at least one other rail attached to it grows a thin grab bar
 *   above its own content, spanning its column's top row; dragging the bar drags the whole group,
 *   exactly as dragging that rail's own body would — attached rails simply follow, since their
 *   position is always computed relative to whatever they're attached to. Dragging any single rail
 *   directly (bar or no bar) always detaches it from whatever it was attached to first; its own
 *   dependants (if any) are untouched and keep following it to its new spot.
 *
 * A rail belonging to the left-hand column of a two-column group opens its own nested-rail popups
 * further left; the right-hand column opens them further right — so neither ever opens over the
 * other column. A lone (unpaired) rail falls back to the main rail's own left/right convention.
 */
@Composable
private fun FloatingDockGroup(
    hosts: List<AzNavItem>,
    scope: AzNavRailScopeImpl,
    navController: NavController?,
    currentDestination: String?,
    hostStates: MutableMap<String, Boolean>,
    buttonSize: Dp,
    spacingDp: Dp,
    onCyclerClick: (AzNavItem) -> Unit,
    hiddenMenuOpenId: String?,
    onMenuOpen: (String) -> Unit,
    onHiddenMenuDismiss: () -> Unit,
    railOnLeft: Boolean,
    screenWidthPx: Float,
    screenHeightPx: Float,
    context: android.content.Context,
) {
    val density = LocalDensity.current
    val spacingPx = with(density) { spacingDp.roundToPx() }
    val edgeStartPx = with(density) { 8.dp.toPx() }
    val edgeSnapPx = with(density) { 56.dp.toPx() }
    val railSnapPx = with(density) { 24.dp.toPx() }
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val minY = screenHeightPx * 0.1f
    val maxYBase = screenHeightPx * 0.9f
    val verticalCapacityPx = (maxYBase - minY).coerceAtLeast(0f)

    val states = remember { mutableStateMapOf<String, AzFloatingRailState>() }

    // All the small positioning/attachment math below reads and writes `states` directly — it's a
    // `mutableStateMapOf`, so composables that read a given rail's fields still recompose correctly;
    // these are plain (non-@Composable) local functions purely to keep that math out of the render
    // loop below. Declared ahead of the "first time seen" default-placement effect since that effect
    // needs [worstCaseHeightPx] too (see its own comment for why).

    /** How tall [hostId]'s own stack would be with every descendant sub-item unfolded at once. */
    fun worstCaseHeightPx(hostId: String): Float {
        fun rowCount(id: String): Int {
            var count = 1
            scope.navItems.filter { it.isSubItem && it.hostId == id }.forEach { child ->
                count += if (child.isHost) rowCount(child.id) else 1
            }
            return count
        }
        return rowCount(hostId) * (buttonSizePx + spacingPx) - spacingPx
    }

    // Create state for newly-declared hosts (loading any persisted dock/free position), and drop
    // state for hosts no longer declared. Rail-to-rail attachments reset on a structural change too
    // (an id that vanished mid-drag can't stay a valid attachment target).
    LaunchedEffect(hosts.map { it.id }) {
        val ids = hosts.map { it.id }.toSet()
        states.keys.filter { it !in ids }.forEach { states.remove(it) }
        // First-seen hosts are staggered by each PRECEDING first-seen host's own worst-case (fully
        // expanded) height, not a flat single-row step — an `initiallyExpanded` host is taller than
        // one row, and a flat step let a later host's default spot land on top of an earlier host's
        // unfolded children, with the later one (drawn on top) stealing their touches.
        var nextY = minY
        hosts.forEach { host ->
            if (states.containsKey(host.id)) return@forEach
            val saved = AzUnattachedStore.loadFloating(context, host.id)
            states[host.id] = if (saved != null) {
                AzFloatingRailState(
                    dock = saved.dock,
                    freeOffset = if (saved.dock == FloatingDock.FREE) {
                        Offset(saved.a * screenWidthPx, saved.b * screenHeightPx)
                    } else null,
                    priority = saved.a,
                )
            } else {
                // First time seen: park near the top of the side opposite the rail, staggered by
                // declaration order so several never-dragged rails don't land on top of each other.
                AzFloatingRailState(
                    dock = FloatingDock.FREE,
                    freeOffset = Offset(if (railOnLeft) screenWidthPx else 0f, nextY),
                    priority = 0f,
                )
            }
            nextY += worstCaseHeightPx(host.id) + spacingPx
        }
    }

    /** `id` plus everything that (transitively) depends on it — never a valid attachment target. */
    fun subtreeOf(id: String, acc: MutableSet<String> = mutableSetOf()): Set<String> {
        if (!acc.add(id)) return acc
        states.forEach { (otherId, st) -> if (st.rightOf == id || st.belowOf == id) subtreeOf(otherId, acc) }
        return acc
    }

    fun directRightDependent(id: String) = states.entries.firstOrNull { it.value.rightOf == id }?.key
    fun directBelowDependent(id: String) = states.entries.firstOrNull { it.value.belowOf == id }?.key

    /** Bounding size of the little grid rooted at [id] (its own size plus every attached rail's). */
    fun clusterExtent(id: String): IntSize {
        val st = states[id] ?: return IntSize.Zero
        var w = st.size.width
        var h = st.size.height
        directRightDependent(id)?.let { right ->
            val e = clusterExtent(right)
            w += spacingPx + e.width
            h = maxOf(h, e.height)
        }
        directBelowDependent(id)?.let { below ->
            val e = clusterExtent(below)
            h += spacingPx + e.height
            w = maxOf(w, e.width)
        }
        return IntSize(w, h)
    }

    /** The topmost rail of [id]'s column (walking up `belowOf` pointers). */
    fun columnTopOf(id: String): String {
        var cur = id
        while (true) cur = states[cur]?.belowOf ?: return cur
    }

    /** Every rail in [topId]'s column, top to bottom. */
    fun columnMembers(topId: String): List<String> {
        val list = mutableListOf(topId)
        var cur = topId
        while (true) {
            val next = directBelowDependent(cur) ?: break
            list.add(next)
            cur = next
        }
        return list
    }

    fun columnWorstCaseHeightPx(memberIds: List<String>): Float {
        var total = 0f
        memberIds.forEachIndexed { i, id ->
            total += worstCaseHeightPx(id)
            if (i < memberIds.size - 1) total += spacingPx
        }
        return total
    }

    fun rootsOnEdge(edge: FloatingDock): List<String> =
        states.entries
            .filter { (_, st) -> st.dock == edge && st.rightOf == null && st.belowOf == null }
            .sortedBy { it.value.priority }
            .map { it.key }

    fun edgeDockedPosition(id: String, edge: FloatingDock): Offset {
        val siblings = rootsOnEdge(edge)
        val myIndex = siblings.indexOf(id)
        if (myIndex < 0) return Offset.Zero
        val startPx = if (edge == FloatingDock.OPPOSITE) minY else edgeStartPx
        var along = startPx
        for (i in 0 until myIndex) {
            val extent = clusterExtent(siblings[i])
            along += (if (edge == FloatingDock.OPPOSITE) extent.height else extent.width) + spacingPx
        }
        val mySize = states[id]?.size ?: IntSize.Zero
        return when (edge) {
            FloatingDock.TOP -> Offset(along, minY)
            FloatingDock.BOTTOM -> Offset(along, screenHeightPx - minY - mySize.height)
            FloatingDock.OPPOSITE -> {
                val x = if (railOnLeft) screenWidthPx - mySize.width else 0f
                Offset(x, along)
            }
            FloatingDock.FREE -> Offset.Zero
        }
    }

    fun resolvedPosition(id: String, depth: Int = 0): Offset {
        if (depth > 32) return Offset.Zero // Defensive cycle guard; attachment is cycle-free by construction.
        val st = states[id] ?: return Offset.Zero
        if (st.dragging) return st.liveDragOffset
        val rOf = st.rightOf
        val bOf = st.belowOf
        return when {
            rOf != null && states[rOf] != null ->
                resolvedPosition(rOf, depth + 1) + Offset((states[rOf]!!.size.width + spacingPx).toFloat(), 0f)
            bOf != null && states[bOf] != null ->
                resolvedPosition(bOf, depth + 1) + Offset(0f, (states[bOf]!!.size.height + spacingPx).toFloat())
            st.dock == FloatingDock.FREE -> st.freeOffset ?: Offset.Zero
            else -> edgeDockedPosition(id, st.dock)
        }
    }

    /** Total width of [id]'s own row within its group (itself plus its rightward attachment chain). */
    fun topRowWidthPx(id: String): Int {
        val st = states[id] ?: return 0
        val right = directRightDependent(id)
        return st.size.width + if (right != null) spacingPx + topRowWidthPx(right) else 0
    }

    fun beginDrag(id: String) {
        val st = states[id] ?: return
        val current = resolvedPosition(id)
        st.rightOf = null
        st.belowOf = null
        st.liveDragOffset = current
        st.dragging = true
    }

    fun dragBy(id: String, delta: Offset) {
        states[id]?.let { it.liveDragOffset += delta }
    }

    fun persistFree(id: String, offset: Offset) {
        AzUnattachedStore.saveFloating(
            context, id,
            AzFloatingSave(FloatingDock.FREE, offset.x / screenWidthPx, offset.y / screenHeightPx),
        )
    }

    fun persistDock(id: String, dock: FloatingDock, priority: Float) {
        AzUnattachedStore.saveFloating(context, id, AzFloatingSave(dock, priority, 0f))
    }

    fun endDrag(id: String) {
        val st = states[id] ?: return
        st.dragging = false
        val pos = st.liveDragOffset
        val mySize = clusterExtent(id)
        val excluded = subtreeOf(id)

        // 1. Rail-to-rail docking: flush against another rail's right or bottom edge.
        val target = states.entries.firstOrNull { (otherId, other) ->
            if (otherId in excluded) return@firstOrNull false
            val otherPos = resolvedPosition(otherId)
            val nearRight = other.rightOf == null && directRightDependent(otherId) == null &&
                abs(pos.x - (otherPos.x + other.size.width)) < railSnapPx &&
                pos.y < otherPos.y + other.size.height && pos.y + mySize.height > otherPos.y
            val nearBelow = directBelowDependent(otherId) == null &&
                abs(pos.y - (otherPos.y + other.size.height)) < railSnapPx &&
                pos.x < otherPos.x + other.size.width && pos.x + mySize.width > otherPos.x &&
                run {
                    // Refuse if this column, with `id` added, wouldn't fit on screen fully expanded.
                    val members = columnMembers(columnTopOf(otherId)) + id
                    columnWorstCaseHeightPx(members) <= verticalCapacityPx
                }
            nearRight || nearBelow
        }
        if (target != null) {
            val (targetId, targetState) = target
            val targetPos = resolvedPosition(targetId)
            val nearRight = targetState.rightOf == null && directRightDependent(targetId) == null &&
                abs(pos.x - (targetPos.x + targetState.size.width)) < railSnapPx
            if (nearRight) st.rightOf = targetId else st.belowOf = targetId
            return
        }

        // 2. Screen-edge docking.
        val newDock = when {
            pos.y <= minY + edgeSnapPx -> FloatingDock.TOP
            pos.y + mySize.height >= maxYBase - edgeSnapPx -> FloatingDock.BOTTOM
            (railOnLeft && pos.x + mySize.width >= screenWidthPx - edgeSnapPx) ||
                (!railOnLeft && pos.x <= edgeSnapPx) -> FloatingDock.OPPOSITE
            else -> FloatingDock.FREE
        }
        st.dock = newDock
        if (newDock == FloatingDock.FREE) {
            val maxX = maxOf(0f, screenWidthPx - mySize.width)
            val maxY = maxOf(minY, maxYBase - mySize.height)
            val clamped = Offset(pos.x.coerceIn(0f, maxX), pos.y.coerceIn(minY, maxY))
            st.freeOffset = clamped
            persistFree(id, clamped)
        } else {
            st.priority = when (newDock) {
                FloatingDock.OPPOSITE -> (pos.y / screenHeightPx).coerceIn(0f, 1f)
                else -> (pos.x / screenWidthPx).coerceIn(0f, 1f)
            }
            persistDock(id, newDock, st.priority)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        for (host in hosts) key(host.id) {
            val st = states[host.id] ?: continue
            val pos = resolvedPosition(host.id)
            val isColumnRoot = st.rightOf == null && st.belowOf == null
            val hasDependent = directRightDependent(host.id) != null || directBelowDependent(host.id) != null
            val popupOpensLeft = when {
                st.rightOf != null -> false // Right-hand column: open further right.
                directRightDependent(host.id) != null -> true // Left-hand column of a pair: open further left.
                else -> railOnLeft // Unpaired: fall back to the main rail's own convention.
            }

            Box(modifier = Modifier.offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }) {
                Column {
                    if (isColumnRoot && hasDependent) {
                        FloatingGrabBar(
                            widthPx = topRowWidthPx(host.id),
                            accent = scope.railAccent,
                            onDragStart = { beginDrag(host.id) },
                            onDrag = { dragBy(host.id, it) },
                            onDragEnd = { endDrag(host.id) },
                        )
                    }
                    Box(
                        modifier = Modifier
                            // A gutter around the column: without it the stack is wall-to-wall
                            // clickable buttons and the drag gesture has nowhere to begin.
                            .padding(AzNavRailDefaults.RailContentVerticalArrangement)
                            .onGloballyPositioned { st.size = it.size }
                            .pointerInput(host.id) {
                                detectDragGestures(
                                    onDragStart = { beginDrag(host.id) },
                                    onDrag = { change, amount -> change.consume(); dragBy(host.id, amount) },
                                    onDragEnd = { endDrag(host.id) },
                                )
                            },
                    ) {
                        UnattachedStack(
                            hosts = listOf(host),
                            modifier = Modifier,
                            scope = scope,
                            navController = navController,
                            currentDestination = currentDestination,
                            hostStates = hostStates,
                            buttonSize = buttonSize,
                            spacingDp = spacingDp,
                            onCyclerClick = onCyclerClick,
                            hiddenMenuOpenId = hiddenMenuOpenId,
                            onMenuOpen = onMenuOpen,
                            onHiddenMenuDismiss = onHiddenMenuDismiss,
                            popupOpensLeft = popupOpensLeft,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A thin drag handle shown above a [FloatingDockGroup] column-root that has at least one other rail
 * attached to it, spanning that rail's row so the whole group reads as one draggable unit instead of
 * making the user discover the (much smaller) gutter around the rail's own content.
 */
@Composable
private fun FloatingGrabBar(
    widthPx: Int,
    accent: Color,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val widthDp = with(density) { widthPx.toDp() }
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .width(widthDp.coerceAtLeast(24.dp))
            .height(6.dp)
            .background(accent.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount -> change.consume(); onDrag(amount) },
                    onDragEnd = { onDragEnd() },
                )
            },
    )
}
