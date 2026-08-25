import type { AzNavItem } from './types';

/** True when `classifiers` overlaps `set` (which may be a Set or a plain array). */
export function classifierHit(
  classifiers: Set<string> | string[] | undefined,
  set: Set<string> | string[] | undefined
): boolean {
  if (!classifiers || !set) return false;
  const haystack = Array.isArray(set) ? new Set(set) : set;
  const list = Array.isArray(classifiers)
    ? classifiers
    : Array.from(classifiers);
  return list.some((c) => haystack.has(c));
}

/**
 * The colour a rail item is drawn in, resolving the four highlights.
 *
 * They answer four different questions — **active** ("where am I?"), **focus** ("what am I
 * touching?"), **secondary** ("whatever the app decides") and **tertiary** (a second app-driven
 * channel) — and they outrank each other in that order reversed: a press is the most immediate
 * thing happening, so focus wins for as long as it lasts, then active, then secondary, then
 * tertiary. An item with none of them lit wears its own `color`.
 *
 * Shared by `AzNavRail.tsx` (the rail strip and menu) and `AzUnattachedRail.tsx` (unattached
 * hosts), so an unattached item's highlight resolves exactly the same way a rail item's does.
 */
export function resolveHighlight(
  item: AzNavItem,
  cfg: any,
  currentDestination?: string,
  lastTappedId?: string | null
): string | undefined {
  const activeColor = (item as any).activeColor ?? cfg.activeColor;
  const secondaryColor =
    (item as any).secondaryColor ?? cfg.secondaryColor ?? activeColor;
  const tertiaryColor =
    (item as any).tertiaryColor ?? cfg.tertiaryColor ?? activeColor;

  const isActive =
    !!item.isChecked ||
    item.id === currentDestination ||
    (!!item.route && item.route === currentDestination) ||
    classifierHit(item.classifiers, cfg.activeClassifiers) ||
    (!item.route && !!lastTappedId && lastTappedId === item.id);
  const isSecondary =
    !!(item as any).secondary ||
    classifierHit(item.classifiers, cfg.secondaryClassifiers);
  const isTertiary =
    !!(item as any).tertiary ||
    classifierHit(item.classifiers, cfg.tertiaryClassifiers);

  if (isActive && activeColor) return activeColor;
  if (isSecondary && secondaryColor) return secondaryColor;
  if (isTertiary && tertiaryColor) return tertiaryColor;
  return item.color;
}
