import type { AzShapeBounds } from './AzStatus';

/**
 * Pure geometry for placing a guidance callout near its target but never on top of it, off the known
 * obstacles, and inside the safe area. Window-space px. Mirrors the Kotlin `AzCalloutPlacement`.
 */

/** Area of the intersection of two rects (0 when disjoint). */
export function overlapArea(a: AzShapeBounds, b: AzShapeBounds): number {
  const w = Math.max(0, Math.min(a.left + a.width, b.left + b.width) - Math.max(a.left, b.left));
  const h = Math.max(0, Math.min(a.top + a.height, b.top + b.height) - Math.max(a.top, b.top));
  return w * h;
}

function clampToSafe(x: number, y: number, w: number, h: number, safe: AzShapeBounds): AzShapeBounds {
  const maxLeft = Math.max(safe.left, safe.left + safe.width - w);
  const maxTop = Math.max(safe.top, safe.top + safe.height - h);
  const left = Math.min(Math.max(x, safe.left), maxLeft);
  const top = Math.min(Math.max(y, safe.top), maxTop);
  return { left, top, width: w, height: h };
}

function penalty(rect: AzShapeBounds, obstacles: AzShapeBounds[], target: AzShapeBounds | null): number {
  let p = 0;
  if (target) p += overlapArea(rect, target) * 2;
  for (const o of obstacles) p += overlapArea(rect, o);
  return p;
}

/**
 * Choose the best window-space rect for a `size` callout: the four sides of `target` (with `gap`), or a
 * grid scan of `safe` when there is no target. Lowest obstacle-overlap wins (ties prefer the earlier
 * candidate — below first), then it is clamped fully into `safe`.
 */
export function placeCallout(
  target: AzShapeBounds | null,
  size: { width: number; height: number },
  obstacles: AzShapeBounds[],
  safe: AzShapeBounds,
  gap = 8,
): AzShapeBounds {
  const w = size.width;
  const h = size.height;
  const candidates: AzShapeBounds[] = [];
  if (target) {
    candidates.push(clampToSafe(target.left, target.top + target.height + gap, w, h, safe)); // below
    candidates.push(clampToSafe(target.left, target.top - h - gap, w, h, safe)); // above
    candidates.push(clampToSafe(target.left + target.width + gap, target.top, w, h, safe)); // end
    candidates.push(clampToSafe(target.left - w - gap, target.top, w, h, safe)); // start
  } else {
    const stepX = Math.max(1, w * 0.5);
    const stepY = Math.max(1, h * 0.5);
    const maxY = Math.max(safe.top, safe.top + safe.height - h);
    const maxX = Math.max(safe.left, safe.left + safe.width - w);
    for (let y = safe.top; y <= maxY; y += stepY) {
      for (let x = safe.left; x <= maxX; x += stepX) {
        candidates.push(clampToSafe(x, y, w, h, safe));
      }
    }
    if (candidates.length === 0) candidates.push(clampToSafe(safe.left, safe.top, w, h, safe));
  }
  let best = candidates[0];
  let bestP = penalty(best, obstacles, target);
  for (let i = 1; i < candidates.length; i++) {
    const p = penalty(candidates[i], obstacles, target);
    if (p < bestP) {
      best = candidates[i];
      bestP = p;
    }
  }
  return best;
}
