package com.hereliesaz.aznavrail.internal

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.navigation.NavController
import com.hereliesaz.aznavrail.AzNavRailScopeImpl
import com.hereliesaz.aznavrail.model.AzMotion
import com.hereliesaz.aznavrail.model.AzDockingSide
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.model.AzUnattachedAnchor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
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
 * Hosts sharing an anchor stack into a column in declaration order, spaced exactly like rail items
 * (and packed when the rail is packed). Tapping one unfolds its sub-items inline beneath it, and
 * sub-hosts nest to any depth, matching the rail's accordion behaviour.
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
                    .align(if (railOnLeft) Alignment.TopEnd else Alignment.TopStart)
                    .padding(top = safeInset, bottom = safeInset),
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
            )
        }

        byAnchor[AzUnattachedAnchor.BOTTOM]?.let { hosts ->
            UnattachedStack(
                hosts = hosts,
                modifier = Modifier
                    .align(if (railOnLeft) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(top = safeInset, bottom = safeInset),
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
            )
        }

        byAnchor[AzUnattachedAnchor.FLOATING]?.let { hosts ->
            // The live position is kept in pixels for the drag and persisted as a window fraction,
            // so it survives rotation and lands sensibly on a different screen size.
            var stackSize by remember { mutableStateOf(IntSize.Zero) }
            var position by remember { mutableStateOf<Offset?>(null) }

            val minY = screenHeightPx * 0.1f
            val maxY = maxOf(minY, screenHeightPx * 0.9f - stackSize.height)
            val maxX = maxOf(0f, screenWidthPx - stackSize.width)

            // Resolve the position once the stack has been measured: the persisted spot when there
            // is one, otherwise parked against the edge opposite the rail. Re-clamped afterwards so
            // a rotation, a resize, or an unfolding host can't strand it outside the safe zone.
            LaunchedEffect(stackSize, screenWidthPx, screenHeightPx) {
                if (stackSize == IntSize.Zero) return@LaunchedEffect
                val settled = position
                position = if (settled == null) {
                    val saved = AzUnattachedStore.load(context)
                    if (saved != null) {
                        Offset(
                            (saved.first * screenWidthPx).coerceIn(0f, maxX),
                            (saved.second * screenHeightPx).coerceIn(minY, maxY),
                        )
                    } else {
                        Offset(if (railOnLeft) maxX else 0f, minY)
                    }
                } else {
                    Offset(settled.x.coerceIn(0f, maxX), settled.y.coerceIn(minY, maxY))
                }
            }

            UnattachedStack(
                hosts = hosts,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    // A gutter around the column: without it the stack is wall-to-wall clickable
                    // buttons and the drag gesture has nowhere to begin.
                    .padding(AzNavRailDefaults.RailContentVerticalArrangement)
                    .offset {
                        val p = position
                        IntOffset(p?.x?.roundToInt() ?: 0, p?.y?.roundToInt() ?: 0)
                    }
                    .onGloballyPositioned { stackSize = it.size }
                    .pointerInput(minY, maxY, maxX) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val base = position ?: Offset(0f, minY)
                                position = Offset(
                                    (base.x + dragAmount.x).coerceIn(0f, maxX),
                                    (base.y + dragAmount.y).coerceIn(minY, maxY),
                                )
                            },
                            onDragEnd = {
                                val settled = position
                                if (settled != null && screenWidthPx > 0f && screenHeightPx > 0f) {
                                    AzUnattachedStore.save(
                                        context,
                                        settled.x / screenWidthPx,
                                        settled.y / screenHeightPx,
                                    )
                                }
                            },
                        )
                    },
                scope = scope,
                navController = navController,
                currentDestination = currentDestination,
                hostStates = hostStates,
                buttonSize = buttonSize,
                spacingDp = spacing,
                onCyclerClick = onCyclerClick,
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
) {
    // A cycler shows its transient option while the commit window is still running, exactly as it
    // does on the rail.
    val displayItem =
        if (item.isCycler) item.copy(selectedOption = scope.transientCyclerOptions[item.id] ?: item.selectedOption)
        else item

    RailContent(
        defaultShape = scope.defaultShape,
        item = displayItem,
        navController = navController,
        isSelected = (item.route != null && item.route == currentDestination) ||
            item.classifiers.any { scope.activeClassifiers.contains(it) },
        buttonSize = buttonSize,
        onClick = {
            scope.lastTouchedItemId = item.id
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
        activeColor = scope.activeColor,
        onSliderChange = { id, v -> scope.onSliderChangeMap[id]?.invoke(v) },
        onSliderRangeChange = { id, r -> scope.onSliderRangeChangeMap[id]?.invoke(r) },
    )

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
                )
            }
        }
    }
}
