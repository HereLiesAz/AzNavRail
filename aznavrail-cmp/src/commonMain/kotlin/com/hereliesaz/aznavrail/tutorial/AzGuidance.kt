package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.hereliesaz.aznavrail.model.AzNavItem
import com.hereliesaz.aznavrail.service.azCacheSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREF_KEY = "az_navrail_completed_goals"
private const val PREF_KEY_DISMISSED = "az_navrail_dismissed_goals"

/**
 * Runtime for the status-driven guidance framework. The developer activates one or more goals; the
 * engine (which provides the reactive `activeStatuses`) plus [routeInstructions] decide what to show.
 *
 * Port note vs the Android sibling: The CMP variant uses `azCacheSettings` (multiplatform-settings)
 * for persistence instead of SharedPreferences, maintaining the same behavior across all platforms.
 */
class AzGuidanceController(
    initialCompleted: List<String> = emptyList(),
    initialDismissed: List<String> = emptyList(),
) {
    /** Master switch — guidance only renders while enabled. */
    var enabled by mutableStateOf(false)
        private set

    private val _activeGoals = mutableStateListOf<String>()
    /** Goal ids currently guiding (several may guide at once). */
    val activeGoals: List<String> get() = _activeGoals

    private val _completed = mutableStateListOf<String>().apply { addAll(initialCompleted) }
    /** Goal ids reached at least once, so onboarding-style goals don't auto-restart within this session. */
    val completedGoals: List<String> get() = _completed

    private val _dismissed = mutableStateListOf<String>().apply { addAll(initialDismissed) }
    /** Goal ids the user skipped, so a skipped tutorial is never shown again this session. */
    val dismissedGoals: List<String> get() = _dismissed
    fun isDismissed(goalId: String): Boolean = goalId in _dismissed

    // Session-scoped de-dup: statuses that were a shown next-hop's target and have since been reached.
    // Routing treats them as permanently traversed, so a step shown + acted on never re-appears.
    private val _consumed = mutableStateListOf<String>()
    /** Statuses consumed this session (a shown step whose action was taken). Reactive. */
    val consumedStatuses: Set<String> get() = _consumed.toHashSet()
    /** Mark [status] consumed (idempotent). Called by the rail when a shown hop's `to` becomes true. */
    internal fun consume(status: String) { if (status !in _consumed) _consumed.add(status) }

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
    fun disable() { enabled = false; _consumed.clear() }

    /**
     * Begin guiding toward [goalId]. **No-op if the goal was already completed or skipped** — a finished
     * or dismissed tutorial is never re-shown until the developer calls [resetGuidance].
     */
    fun activate(goalId: String) {
        if (goalId in _completed || goalId in _dismissed) return
        enabled = true
        if (goalId !in _activeGoals) _activeGoals.add(goalId)
    }

    /** Stop guiding toward [goalId] (without recording it as skipped). */
    fun deactivate(goalId: String) { _activeGoals.remove(goalId) }

    /**
     * The user **skipped** [goalId]: deactivate and record the dismissal for this session.
     */
    fun skip(goalId: String) {
        _activeGoals.remove(goalId)
        if (goalId !in _dismissed) {
            _dismissed.add(goalId)
            azCacheSettings.putString(PREF_KEY_DISMISSED, Json.encodeToString(_dismissed.toList()))
        }
    }

    /** Cancel tutorial mode entirely: skip every active goal and turn guidance off. */
    fun skip() {
        _activeGoals.toList().forEach { skip(it) }
        enabled = false
        _consumed.clear()
    }

    /** Mark a goal reached: deactivate it and record completion for this session. */
    fun markReached(goalId: String) {
        _activeGoals.remove(goalId)
        if (goalId !in _completed) {
            _completed.add(goalId)
            azCacheSettings.putString(PREF_KEY, Json.encodeToString(_completed.toList()))
        }
    }

    fun isCompleted(goalId: String): Boolean = goalId in _completed

    /** Clear [goalId]'s completion + dismissal (+ session de-dup) so it can be guided again. */
    fun resetGuidance(goalId: String) {
        val wasCompleted = _completed.remove(goalId)
        val wasDismissed = _dismissed.remove(goalId)
        if (wasCompleted) azCacheSettings.putString(PREF_KEY, Json.encodeToString(_completed.toList()))
        if (wasDismissed) azCacheSettings.putString(PREF_KEY_DISMISSED, Json.encodeToString(_dismissed.toList()))
        _consumed.clear()
    }

    /** Clear all completion + dismissal (+ session de-dup). */
    fun resetGuidance() {
        _completed.clear()
        _dismissed.clear()
        _consumed.clear()
        azCacheSettings.remove(PREF_KEY)
        azCacheSettings.remove(PREF_KEY_DISMISSED)
    }

    companion object {
        val Saver: Saver<AzGuidanceController, List<Any?>> = Saver(
            save = { listOf(ArrayList(it._activeGoals), ArrayList(it._completed), it.enabled, ArrayList(it._dismissed)) },
            restore = { list ->
                AzGuidanceController(
                    initialCompleted = (list[1] as List<*>).filterIsInstance<String>(),
                    initialDismissed = (list.getOrNull(3) as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
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
    return rememberSaveable(saver = AzGuidanceController.Saver) {
        val doneStr = azCacheSettings.getString(PREF_KEY, "")
        val dismissedStr = azCacheSettings.getString(PREF_KEY_DISMISSED, "")
        val done = if (doneStr.isNotBlank()) Json.decodeFromString<List<String>>(doneStr) else emptyList()
        val dismissed = if (dismissedStr.isNotBlank()) Json.decodeFromString<List<String>>(dismissedStr) else emptyList()
        AzGuidanceController(initialCompleted = done, initialDismissed = dismissed)
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
    consumedStatuses: Set<String> = emptySet(),
): GuidanceFrame {
    // De-dup: a status that was a shown hop's target and has since been reached is treated as
    // permanently true, so its hop is never routed to (or re-shown) again.
    val effective = if (consumedStatuses.isEmpty()) activeStatuses else activeStatuses + consumedStatuses
    val out = LinkedHashMap<Pair<String, AzGuideHighlight>, ResolvedInstruction>()
    val reached = HashSet<String>()
    activeGoalIds.forEach { gid ->
        val goal = goals[gid] ?: return@forEach
        if (goal.target in effective) { reached.add(gid); return@forEach }
        nextHop(edges, effective, goal.target)?.let { e ->
            val r = resolveEdge(e, gid, stepIndexOf, effective)
            out[r.instruction.text to r.instruction.highlight] = r
        }
    }
    // Passive tips: shown while their `from` status holds.
    edges.filter { it.to == null && it.from in effective }.forEach { e ->
        val r = resolveEdge(e, null, stepIndexOf, effective)
        out[r.instruction.text to r.instruction.highlight] = r
    }
    return GuidanceFrame(out.values.toList(), reached)
}
