package com.hereliesaz.aznavrail.tutorial

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Pure geometry for placing a guidance callout so it sits **near its target but never on top of it**,
 * avoids the known [obstacles] (other rail items / targets / already-placed callouts), and stays fully
 * inside the [safe] area (so text is never clipped and nothing lands in the system-bar zone). All values
 * are window-space px. Kept pure so it is unit-testable without a renderer; mirrored in the React port.
 */

/** Area of the intersection of two rects (0 when disjoint). */
internal fun overlapArea(a: Rect, b: Rect): Float {
    val w = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0f)
    val h = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0f)
    return w * h
}

/** Shift a [width]×[height] rect anchored at ([x],[y]) so it lies fully within [safe] (best effort). */
private fun clampToSafe(x: Float, y: Float, width: Float, height: Float, safe: Rect): Rect {
    val left = x.coerceIn(safe.left, (safe.right - width).coerceAtLeast(safe.left))
    val top = y.coerceIn(safe.top, (safe.bottom - height).coerceAtLeast(safe.top))
    return Rect(left, top, left + width, top + height)
}

/** Total overlap of [rect] against every obstacle plus the [target] it must not cover. */
private fun penalty(rect: Rect, obstacles: List<Rect>, target: Rect?): Float {
    var p = 0f
    if (target != null) p += overlapArea(rect, target) * 2f // weigh covering the target itself heaviest
    obstacles.forEach { p += overlapArea(rect, it) }
    return p
}

/**
 * Choose the best window-space rect for a [size] callout. When [target] is non-null the four sides
 * (below, above, end, start, with [gap]) are tried; otherwise a coarse grid of [safe] is scanned so
 * untargeted callouts spread out instead of stacking. The lowest-overlap candidate wins (ties resolve in
 * the listed order — below is preferred), then it is clamped into [safe].
 */
internal fun placeCallout(
    target: Rect?,
    size: Size,
    obstacles: List<Rect>,
    safe: Rect,
    gap: Float = 8f,
): Rect {
    val w = size.width
    val h = size.height
    val candidates = ArrayList<Rect>()
    if (target != null) {
        // Below, above, end, start — anchored to the target, then clamped into the safe area.
        candidates += clampToSafe(target.left, target.bottom + gap, w, h, safe)
        candidates += clampToSafe(target.left, target.top - h - gap, w, h, safe)
        candidates += clampToSafe(target.right + gap, target.top, w, h, safe)
        candidates += clampToSafe(target.left - w - gap, target.top, w, h, safe)
    } else {
        // No anchor: scan a grid of the safe area (so multiple untargeted callouts don't collide).
        val stepX = (w * 0.5f).coerceAtLeast(1f)
        val stepY = (h * 0.5f).coerceAtLeast(1f)
        var y = safe.top
        while (y <= (safe.bottom - h).coerceAtLeast(safe.top)) {
            var x = safe.left
            while (x <= (safe.right - w).coerceAtLeast(safe.left)) {
                candidates += clampToSafe(x, y, w, h, safe)
                x += stepX
            }
            y += stepY
        }
        if (candidates.isEmpty()) candidates += clampToSafe(safe.left, safe.top, w, h, safe)
    }
    // Pick the minimum-penalty candidate; the first listed wins ties (stable preference order).
    var best = candidates.first()
    var bestPenalty = penalty(best, obstacles, target)
    for (i in 1 until candidates.size) {
        val p = penalty(candidates[i], obstacles, target)
        if (p < bestPenalty) {
            best = candidates[i]
            bestPenalty = p
        }
    }
    return best
}
