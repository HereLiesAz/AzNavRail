package com.hereliesaz.aznavrail.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/**
 * Reactive, status-driven guidance model — the replacement for the scripted scene/card tutorial.
 *
 * The app (and AzNavHost automatically) describe the userflow as a flowchart of **statuses** (nodes)
 * and **edges** (transitions, each carrying the instruction to traverse it). The developer declares
 * one or more **goals** (target statuses) and activates them; the engine ([AzStatusEngine]) observes
 * which statuses are currently true and always shows the instruction to reach the next status on the
 * path toward each active goal, auto-advancing the instant a target status becomes true.
 *
 * A "status" is just a `String` id, resolved uniformly by the engine from three sources:
 *  - a developer predicate registered via `azStatus(id) { … }`,
 *  - an active classifier name (true while it is in `activeClassifiers`),
 *  - a built-in `az.*` id published by AzNavHost from the rail/host/route/onscreen/sheet state.
 */

/**
 * The dynamic highlight token: when passed anywhere a `highlightItemId` is accepted, the callout
 * spotlights whatever rail item is currently `*.active` (resolved fresh every frame), rather than a
 * statically-named id. Degrades to text-only when no item is active. See [AzGuideHighlight.ActiveItem].
 */
const val AZ_ITEM_ACTIVE = "az.item.active"

/** What a guidance instruction spotlights. Reused by the highlight renderer. */
sealed interface AzGuideHighlight {
    /** No spotlight; the instruction card floats over a plain dim. */
    data object None : AzGuideHighlight
    /** Dim the whole screen with no punch-out. */
    data object FullScreen : AzGuideHighlight
    /** Spotlight the rail item with this id (looked up in `itemBoundsCache`). */
    data class Item(val id: String) : AzGuideHighlight
    /** Spotlight an explicit window-space rectangle. */
    data class Area(val left: Float, val top: Float, val width: Float, val height: Float) : AzGuideHighlight
    /**
     * Spotlight whatever rail item is currently active, resolved at render time against the live
     * active-item id (the same item that publishes `az.item.<id>.active`). Text-only when none is
     * active. Authored by passing the [AZ_ITEM_ACTIVE] token as a `highlightItemId`.
     */
    data object ActiveItem : AzGuideHighlight
    /**
     * Spotlight the item whose id [selector] returns, resolved at render time against the live
     * item-bounds map every frame — so an edge declared before its target exists (a runtime
     * `layer.<uuid>` rail item, the just-created item) can still point at it. Returning `null` (or an
     * id with no measured bounds) degrades to text-only. The [AZ_ITEM_ACTIVE] token is honored here too.
     */
    data class Dynamic(val selector: () -> String?) : AzGuideHighlight
    /**
     * Spotlight a host-registered, arbitrary on-screen target (not a rail item): a moving circle, rect,
     * or path drawn over a camera/AR canvas. Resolved at render time against the live target registry
     * (see `azGuidanceTarget(id) { … }`); the lambda returns the current [AzGuideShape] in window-space
     * px. Returning `null` (or an unregistered id) degrades to text-only. Authored by passing
     * `highlightTargetId`.
     */
    data class Target(val id: String) : AzGuideHighlight
}

/**
 * A spotlight geometry in **window-space px** (the same space as `itemBoundsCache`, so it aligns with
 * the overlay punch-out at zero offset). Returned by an `azGuidanceTarget` shape lambda and resolved
 * fresh each frame, so a target can track a moving on-screen object. [padding] inflates the spotlight
 * uniformly beyond the geometry.
 */
sealed interface AzGuideShape {
    val padding: Float
    /** A circle centered at ([cx], [cy]) with [radius]. */
    data class Circle(val cx: Float, val cy: Float, val radius: Float, override val padding: Float = 0f) : AzGuideShape
    /** An axis-aligned rounded rectangle. */
    data class Rect(
        val left: Float, val top: Float, val width: Float, val height: Float,
        val cornerRadius: Float = 16f, override val padding: Float = 0f,
    ) : AzGuideShape
    /** An arbitrary outline described by absolute, window-space [commands] (filled even-odd). */
    data class Path(val commands: List<AzPathCmd>, override val padding: Float = 0f) : AzGuideShape
}

/** One command of an [AzGuideShape.Path], in absolute window-space px (SVG-style). */
sealed interface AzPathCmd {
    data class MoveTo(val x: Float, val y: Float) : AzPathCmd
    data class LineTo(val x: Float, val y: Float) : AzPathCmd
    data class QuadTo(val x1: Float, val y1: Float, val x: Float, val y: Float) : AzPathCmd
    data class CubicTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float) : AzPathCmd
    data object Close : AzPathCmd
}

/**
 * The axis-aligned window-space bounding box of any [AzGuideShape] (inflated by [AzGuideShape.padding]).
 * Used for callout placement and for a host's own hit-testing; the true geometry is only used by the
 * punch-out draw. A path with no points yields [Rect.Zero].
 */
fun AzGuideShape.bounds(): Rect = when (this) {
    is AzGuideShape.Circle -> Rect(cx - radius - padding, cy - radius - padding, cx + radius + padding, cy + radius + padding)
    is AzGuideShape.Rect -> Rect(left - padding, top - padding, left + width + padding, top + height + padding)
    is AzGuideShape.Path -> {
        var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
        fun acc(x: Float, y: Float) { minX = minOf(minX, x); minY = minOf(minY, y); maxX = maxOf(maxX, x); maxY = maxOf(maxY, y) }
        commands.forEach { cmd ->
            when (cmd) {
                is AzPathCmd.MoveTo -> acc(cmd.x, cmd.y)
                is AzPathCmd.LineTo -> acc(cmd.x, cmd.y)
                is AzPathCmd.QuadTo -> { acc(cmd.x1, cmd.y1); acc(cmd.x, cmd.y) }
                is AzPathCmd.CubicTo -> { acc(cmd.x1, cmd.y1); acc(cmd.x2, cmd.y2); acc(cmd.x, cmd.y) }
                AzPathCmd.Close -> {}
            }
        }
        if (minX > maxX) Rect.Zero else Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }
}

/**
 * One paged sub-step of a multi-step edge (see [azEdge] with `steps`). A single milestone (one status
 * hop) can carry several of these, revealed one at a time, moving the spotlight as the user reads:
 * "your design is ready — tap a layer (point at the layer), then open Modes (point at the Modes host)".
 *
 * @param text The line shown for this sub-step.
 * @param title Optional per-step title (overrides the edge title while this step shows).
 * @param highlightItemId Item to spotlight; the [AZ_ITEM_ACTIVE] token resolves to the active item.
 * @param highlightTargetId A host-registered [AzGuideShape] target to spotlight (see `azGuidanceTarget`);
 *   takes precedence over [highlightItemId]/[highlightSelector] when non-null.
 * @param side Preferred callout placement relative to the spotlight target.
 * @param highlightSelector Resolved every frame to the id to spotlight; overrides [highlightItemId]
 *   when non-null, so a step can point at a runtime/dynamically-created item. `null` ⇒ text-only.
 * @param advanceWhen Optional status id; when it becomes true the overlay auto-advances past this step
 *   (reactive advance), regardless of the manual tap cursor. `null` ⇒ this is an informational step the
 *   user advances by tapping.
 */
data class AzInstructionStep(
    val text: String,
    val title: String? = null,
    val highlightItemId: String? = null,
    val highlightTargetId: String? = null,
    val side: AzCalloutSide = AzCalloutSide.Auto,
    val highlightSelector: (() -> String?)? = null,
    val advanceWhen: String? = null,
)

/** Resolves a step's highlight directive into the renderer's [AzGuideHighlight]. */
internal fun AzInstructionStep.toHighlight(): AzGuideHighlight =
    resolveAzHighlight(highlightItemId, highlightSelector, highlightTargetId)

/** This step as a standalone [AzInstruction] (used when the overlay pages a stepped edge). */
internal fun AzInstructionStep.toInstruction(edgeTitle: String?): AzInstruction =
    AzInstruction(text = text, title = title ?: edgeTitle, highlight = toHighlight(), side = side)

/**
 * Resolves a highlight directive into an [AzGuideHighlight] by precedence: a registered [highlightTargetId]
 * ([AzGuideHighlight.Target]) wins, then a [highlightSelector] ([AzGuideHighlight.Dynamic]), then the
 * [AZ_ITEM_ACTIVE] token ([AzGuideHighlight.ActiveItem]), then a literal item id ([AzGuideHighlight.Item]),
 * else [AzGuideHighlight.None].
 */
internal fun resolveAzHighlight(
    highlightItemId: String?,
    highlightSelector: (() -> String?)?,
    highlightTargetId: String? = null,
): AzGuideHighlight = when {
    highlightTargetId != null -> AzGuideHighlight.Target(highlightTargetId)
    highlightSelector != null -> AzGuideHighlight.Dynamic(highlightSelector)
    highlightItemId == AZ_ITEM_ACTIVE -> AzGuideHighlight.ActiveItem
    highlightItemId != null -> AzGuideHighlight.Item(highlightItemId)
    else -> AzGuideHighlight.None
}

/**
 * Resolves a highlight to the **item id** it currently points at (null ⇒ no item / not item-based),
 * folding [AzGuideHighlight.ActiveItem] and [AzGuideHighlight.Dynamic] against the live [activeItemId].
 * Pure, so the resolution is unit-testable without a renderer. [AzGuideHighlight.Area] returns null
 * here (it carries an explicit rect, resolved separately by the overlay).
 */
internal fun AzGuideHighlight.resolveItemId(activeItemId: String?): String? = when (this) {
    is AzGuideHighlight.Item -> id
    is AzGuideHighlight.ActiveItem -> activeItemId
    is AzGuideHighlight.Dynamic -> selector()?.let { if (it == AZ_ITEM_ACTIVE) activeItemId else it }
    else -> null
}

/**
 * Resolves a highlight to the concrete [AzGuideShape] to spotlight, in window-space px: a registered
 * [AzGuideHighlight.Target] via [targets] (the moving-target lambda), an explicit [AzGuideHighlight.Area]
 * rect, or a rail item's cached bounds (Item/ActiveItem/Dynamic). `null` ⇒ text-only (no spotlight).
 * Called both at composition (anchor/float decision) and live in the overlay's draw scope (so a moving
 * target tracks every frame).
 */
internal fun AzGuideHighlight.resolveShape(
    cache: Map<String, Rect>,
    activeItemId: String?,
    targets: Map<String, () -> AzGuideShape?>,
): AzGuideShape? = when (this) {
    is AzGuideHighlight.Area -> AzGuideShape.Rect(left, top, width, height)
    is AzGuideHighlight.Target -> targets[id]?.invoke()
    else -> resolveItemId(activeItemId)?.let { itemId ->
        cache[itemId]?.let { AzGuideShape.Rect(it.left, it.top, it.width, it.height) }
    }
}

/** The resolved target/item id this highlight points at (for the snapshot/analytics), if any. */
internal fun AzGuideHighlight.resolveTargetId(activeItemId: String?): String? = when (this) {
    is AzGuideHighlight.Target -> id
    else -> resolveItemId(activeItemId)
}

/**
 * The instruction shown while the user traverses one edge: the prompt text, an optional title, what to
 * spotlight, and optional inline media.
 *
 * The instruction is rendered as a **callout positioned adjacent to its [highlight] target** — next to
 * the control the user would use to accomplish that hop — not as a centered card. When several goals
 * are active, **every** active instruction is shown at once, each placed by its own target, so the
 * user sees, in place, how to accomplish each goal. [side] biases which edge of the target the callout
 * prefers (auto-resolved against screen bounds when [AzCalloutSide.Auto]).
 */
data class AzInstruction(
    val text: String,
    val title: String? = null,
    val highlight: AzGuideHighlight = AzGuideHighlight.None,
    val side: AzCalloutSide = AzCalloutSide.Auto,
    val media: (@Composable () -> Unit)? = null,
)

/** Preferred placement of a callout relative to its highlight target. */
enum class AzCalloutSide { Auto, Above, Below, Start, End }

/**
 * One edge of the flowchart: while status [from] is true, performing the [instruction] is expected to
 * make status [to] true (an interactive hop). A passive edge ([to] == `null`) just shows info while
 * [from] holds. Authored via `azEdge(...)`, or auto-generated by AzNavHost for rail affordances.
 *
 * When [steps] is non-empty the edge is **paged**: the overlay reveals one [AzInstructionStep] at a
 * time (advancing on tap, or reactively when a step's `advanceWhen` becomes true), moving the spotlight
 * per step — several sub-pointers under one milestone. The last step behaves like a single-instruction
 * edge: the edge is reached when [to] becomes true. Empty [steps] ⇒ the classic single-callout edge.
 */
data class AzEdge(
    val from: String,
    val to: String?,
    val instruction: AzInstruction,
    val steps: List<AzInstructionStep> = emptyList(),
)

/**
 * Stable key identifying this edge for the per-edge step cursor (so two goals routing through the same
 * edge share a cursor, consistent with routing's dedup-by-(text,highlight)). Uses string fields only,
 * so it is independent of any lambda identity.
 */
internal fun AzEdge.stepKey(): String = "$from ${to ?: ""} ${instruction.text}"

/**
 * A developer-declared guidance target. The engine routes from the current status toward [target]; the
 * developer activates/deactivates goals on the controller (several may guide at once). When
 * [autoStartWhen] is non-null the goal self-activates once that status becomes true (onboarding-style).
 *
 * When several active goals are guiding at once, **every** goal's next-hop instruction is shown
 * simultaneously, each as a callout placed adjacent to its own target (deduped where two goals share
 * the exact same next hop). No goal's instruction ever suppresses another's.
 */
data class AzGoal(
    val id: String,
    val target: String,
    val label: String? = null,
    val autoStartWhen: String? = null,
)

/**
 * A read-only snapshot of one guidance callout currently being shown, published live on
 * [AzGuidanceController.currentInstructions] so a host can mirror it with bespoke rendering (e.g. a
 * pulsing highlight drawn over its own canvas) and analytics. The framework may show several at once
 * (one per active goal), so the controller exposes a list.
 *
 * @param text The line currently shown (the active step's text for a paged edge).
 * @param title The title currently shown, if any.
 * @param goalId The owning goal, or `null` for a passive ambient tip.
 * @param highlight The spotlight directive.
 * @param targetId The resolved registered-target / rail-item id this points at, if any.
 * @param resolvedShape The target geometry resolved at publish time (best-effort; the overlay
 *   re-resolves live each frame for moving targets), or `null` when text-only.
 * @param resolvedBounds The axis-aligned bounds of [resolvedShape], a convenience for host hit-testing.
 * @param stepIndex Zero-based index of the active step within a paged edge (0 for a single-callout edge).
 * @param stepTotal Total steps in the edge (1 for a single-callout edge).
 * @param stepKey The owning edge's cursor key (pass to `AzGuidanceController.advance`/`back`).
 */
data class AzGuidanceSnapshot(
    val text: String,
    val title: String? = null,
    val goalId: String? = null,
    val highlight: AzGuideHighlight = AzGuideHighlight.None,
    val targetId: String? = null,
    val resolvedShape: AzGuideShape? = null,
    val resolvedBounds: Rect? = null,
    val stepIndex: Int = 0,
    val stepTotal: Int = 1,
    val stepKey: String? = null,
)

