package com.hereliesaz.aznavrail.tutorial

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.hereliesaz.aznavrail.model.AzNavItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "az_tutorial_prefs"
private const val PREF_KEY = "az_navrail_completed_goals"

/**
 * Runtime for the status-driven guidance framework. The developer activates one or more goals; the
 * engine (which provides the reactive `activeStatuses`) plus [routeInstructions] decide what to show.
 */
class AzGuidanceController(
    initialCompleted: List<String> = emptyList(),
    private val prefs: SharedPreferences? = null,
) {
    /** Master switch — guidance only renders while enabled. */
    var enabled by mutableStateOf(false)
        private set

    private val _activeGoals = mutableStateListOf<String>()
    /** Goal ids currently guiding (several may guide at once). */
    val activeGoals: List<String> get() = _activeGoals

    private val _completed = mutableStateListOf<String>().apply { addAll(initialCompleted) }
    /** Goal ids reached at least once (persisted), so onboarding-style goals don't auto-restart. */
    val completedGoals: List<String> get() = _completed

    // --- Paged-edge step cursor (transient; only goal completion persists) ---
    private val _stepCursor = mutableStateMapOf<String, Int>()
    /** Current step index for the paged edge identified by [key] (see `AzEdge.stepKey()`). */
    fun stepIndex(key: String): Int = _stepCursor[key] ?: 0
    /** Set the step cursor for [key] (used by the overlay to sync reactive auto-advance). */
    fun setStep(key: String, index: Int) { _stepCursor[key] = index.coerceAtLeast(0) }
    /** Advance the paged edge [key] to its next step (clamped at render against the step count). */
    fun advance(key: String) { _stepCursor[key] = stepIndex(key) + 1 }
    /** Alias for [advance]. */
    fun next(key: String) = advance(key)
    /** Step the paged edge [key] back one (never below 0). */
    fun back(key: String) { _stepCursor[key] = (stepIndex(key) - 1).coerceAtLeast(0) }
    /** Reset the cursor for [key] (called when its edge is reached so re-entry starts at step 0). */
    internal fun resetSteps(key: String) { _stepCursor.remove(key) }

    /** Advance the first active paged instruction (host-driven "Next" with no specific edge key). */
    fun advance() { currentInstructions.firstOrNull { it.stepTotal > 1 && it.stepKey != null }?.stepKey?.let { advance(it) } }

    // --- Observable current instruction(s) (Gap C) ---
    /**
     * The guidance callouts being shown right now (one per active goal, plus passive tips), published
     * each routing pass so a host can mirror them with bespoke rendering and analytics. Empty when
     * nothing is showing or guidance is disabled.
     */
    var currentInstructions by mutableStateOf<List<AzGuidanceSnapshot>>(emptyList())
        private set
    /** The primary (first) current instruction, a convenience for single-goal flows. */
    val current: AzGuidanceSnapshot? get() = currentInstructions.firstOrNull()
    private val _currentFlow = MutableStateFlow<List<AzGuidanceSnapshot>>(emptyList())
    /** A [StateFlow] mirror of [currentInstructions] for non-Compose observers. */
    val currentFlow: StateFlow<List<AzGuidanceSnapshot>> = _currentFlow.asStateFlow()
    /** Publish the latest routed snapshot list. Called by the rail; not for app use. */
    internal fun publishCurrent(snapshots: List<AzGuidanceSnapshot>) {
        currentInstructions = snapshots
        _currentFlow.value = snapshots
    }

    fun enable() { enabled = true }
    fun disable() { enabled = false }

    /** Begin guiding toward [goalId]. */
    fun activate(goalId: String) {
        enabled = true
        if (goalId !in _activeGoals) _activeGoals.add(goalId)
    }

    /** Stop guiding toward [goalId]. */
    fun deactivate(goalId: String) { _activeGoals.remove(goalId) }

    /** Mark a goal reached: deactivate it and persist completion. */
    fun markReached(goalId: String) {
        _activeGoals.remove(goalId)
        if (goalId !in _completed) {
            _completed.add(goalId)
            prefs?.edit()?.putStringSet(PREF_KEY, _completed.toSet())?.apply()
        }
    }

    fun isCompleted(goalId: String): Boolean = goalId in _completed

    companion object {
        fun Saver(context: Context): Saver<AzGuidanceController, List<Any?>> = Saver(
            save = { listOf(ArrayList(it._activeGoals), ArrayList(it._completed), it.enabled) },
            restore = { list ->
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                AzGuidanceController(
                    initialCompleted = (list[1] as List<*>).filterIsInstance<String>(),
                    prefs = prefs,
                ).apply {
                    (list[0] as List<*>).filterIsInstance<String>().forEach { _activeGoals.add(it) }
                    if (list[2] as Boolean) enabled = true
                }
            },
        )
    }
}

@Composable
fun rememberAzGuidanceController(): AzGuidanceController {
    val context = LocalContext.current.applicationContext
    return rememberSaveable(saver = AzGuidanceController.Saver(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val done = prefs.getStringSet(PREF_KEY, emptySet())?.toList() ?: emptyList()
        AzGuidanceController(initialCompleted = done, prefs = prefs)
    }
}

/**
 * Provides the host-owned [AzGuidanceController] down the composition so the rail can route/render the
 * same controller the developer receives back from `AzHostActivityLayout`. `null` when the rail is used
 * standalone (it falls back to its own [rememberAzGuidanceController]).
 */
val LocalAzGuidanceController = staticCompositionLocalOf<AzGuidanceController?> { null }

/**
 * Derives the guidance **auto-edges** for the rail's own affordances, so the developer only hand-authors
 * edges whose `to` is a custom status. Generated each render from the live [items]:
 *  - "Open the menu": `az.rail.collapsed → az.rail.expanded`;
 *  - tap a host item → `az.host.<id>.expanded`;
 *  - tap a nested-rail item → `az.nestedRail.<id>.open`;
 *  - tap a routed item → `az.screen.<route>`.
 *
 * Rail items are tappable from anywhere (`az.app.ready`); menu-only items require the menu open
 * (`az.rail.expanded`). Each edge spotlights its own item.
 *
 * The instruction text is supplied by the caller ([openMenuLabel] and the [tapLabel] formatter) so it
 * can be sourced from localized string resources; the English defaults keep this pure function usable
 * from tests. Apps localize by overriding `R.string.az_guide_open_menu` / `R.string.az_guide_tap_item`.
 */
internal fun computeAutoEdges(
    items: List<AzNavItem>,
    openMenuLabel: String = "Open the menu",
    tapLabel: (String) -> String = { "Tap $it" },
): List<AzEdge> {
    val edges = ArrayList<AzEdge>()
    edges += AzEdge(
        from = "az.rail.collapsed",
        to = "az.rail.expanded",
        instruction = AzInstruction(openMenuLabel, highlight = AzGuideHighlight.None),
    )
    items.forEach { item ->
        val visibleFrom = if (item.isRailItem) "az.app.ready" else "az.rail.expanded"
        val highlight = AzGuideHighlight.Item(item.id)
        val tap = tapLabel(item.text.ifBlank { item.id })
        when {
            item.isHost -> edges += AzEdge(visibleFrom, "az.host.${item.id}.expanded", AzInstruction(tap, highlight = highlight))
            item.isNestedRail -> edges += AzEdge(visibleFrom, "az.nestedRail.${item.id}.open", AzInstruction(tap, highlight = highlight))
            item.route != null -> edges += AzEdge(visibleFrom, "az.screen.${item.route}", AzInstruction(tap, highlight = highlight))
        }
    }
    return edges
}

/**
 * BFS the status flowchart from the currently-true [activeStatuses] to [target]; return the first edge
 * on a shortest path (its instruction is the next hop), or null if [target] is already true or
 * unreachable. Only interactive edges (`to != null`) are traversed.
 */
internal fun nextHop(edges: List<AzEdge>, activeStatuses: Set<String>, target: String): AzEdge? {
    if (target in activeStatuses) return null
    val adjacency = edges.filter { it.to != null }.groupBy { it.from }
    val visited = HashSet(activeStatuses)
    val queue = ArrayDeque<Pair<String, AzEdge>>() // (reached status, first edge taken)
    activeStatuses.forEach { start ->
        adjacency[start].orEmpty().forEach { e ->
            val to = e.to!!
            if (visited.add(to)) queue.add(to to e)
        }
    }
    while (queue.isNotEmpty()) {
        val (status, firstEdge) = queue.removeFirst()
        if (status == target) return firstEdge
        adjacency[status].orEmpty().forEach { e ->
            val to = e.to!!
            if (visited.add(to)) queue.add(to to firstEdge)
        }
    }
    return null
}

/**
 * One instruction the engine has chosen to show, resolved to its current paged step. Carries the owning
 * [edge] and [goalId] plus the active step's [stepIndex]/[stepTotal] so the overlay can page it and the
 * controller can publish an [AzGuidanceSnapshot].
 */
internal data class ResolvedInstruction(
    val instruction: AzInstruction,
    val edge: AzEdge,
    val goalId: String?,
    val stepIndex: Int,
    val stepTotal: Int,
)

/** Builds the public [AzGuidanceSnapshot] for this resolved instruction (the [shape] resolved by caller). */
internal fun ResolvedInstruction.toSnapshot(shape: AzGuideShape?, activeItemId: String?): AzGuidanceSnapshot =
    AzGuidanceSnapshot(
        text = instruction.text,
        title = instruction.title,
        goalId = goalId,
        highlight = instruction.highlight,
        targetId = instruction.highlight.resolveTargetId(activeItemId),
        resolvedShape = shape,
        resolvedBounds = shape?.bounds(),
        stepIndex = stepIndex,
        stepTotal = stepTotal,
        stepKey = edge.stepKey(),
    )

/**
 * The set of instructions to show right now: the next-hop instruction for each active goal not yet
 * reached, plus any passive edge whose `from` is currently true. Deduped by (text + highlight).
 * Returns the goal ids that are already at their target (the caller should `markReached` them).
 */
internal data class GuidanceFrame(
    val resolved: List<ResolvedInstruction>,
    val reachedGoals: Set<String>,
) {
    /** Back-compat view: just the per-step instructions to render. */
    val instructions: List<AzInstruction> get() = resolved.map { it.instruction }
}

/**
 * Resolves an edge to the instruction for its **current step**. A non-paged edge resolves to its single
 * instruction. A paged edge consults [stepIndexOf] for the manual cursor, then advances past any leading
 * steps whose `advanceWhen` is already satisfied (reactive advance always wins over the tap cursor).
 */
internal fun resolveEdge(
    edge: AzEdge,
    goalId: String?,
    stepIndexOf: (String) -> Int,
    activeStatuses: Set<String>,
): ResolvedInstruction {
    val steps = edge.steps
    if (steps.isEmpty()) return ResolvedInstruction(edge.instruction, edge, goalId, 0, 1)
    var idx = stepIndexOf(edge.stepKey()).coerceIn(0, steps.lastIndex)
    while (idx < steps.lastIndex && steps[idx].advanceWhen?.let { it in activeStatuses } == true) idx++
    return ResolvedInstruction(steps[idx].toInstruction(edge.instruction.title), edge, goalId, idx, steps.size)
}

internal fun routeInstructions(
    edges: List<AzEdge>,
    goals: Map<String, AzGoal>,
    activeGoalIds: Collection<String>,
    activeStatuses: Set<String>,
    stepIndexOf: (String) -> Int = { 0 },
): GuidanceFrame {
    val out = LinkedHashMap<Pair<String, AzGuideHighlight>, ResolvedInstruction>()
    val reached = HashSet<String>()
    activeGoalIds.forEach { gid ->
        val goal = goals[gid] ?: return@forEach
        if (goal.target in activeStatuses) { reached.add(gid); return@forEach }
        nextHop(edges, activeStatuses, goal.target)?.let { e ->
            val r = resolveEdge(e, gid, stepIndexOf, activeStatuses)
            out[r.instruction.text to r.instruction.highlight] = r
        }
    }
    // Passive tips: shown while their `from` status holds.
    edges.filter { it.to == null && it.from in activeStatuses }.forEach { e ->
        val r = resolveEdge(e, null, stepIndexOf, activeStatuses)
        out[r.instruction.text to r.instruction.highlight] = r
    }
    return GuidanceFrame(out.values.toList(), reached)
}
