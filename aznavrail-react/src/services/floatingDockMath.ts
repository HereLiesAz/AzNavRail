/**
 * Pure geometry/attachment math for `FLOATING`-anchored unattached hosts (see
 * `components/AzUnattachedRail.tsx`). Deliberately free of React and React Native so it can be
 * unit-tested the same way Android/CMP's `AzFloatingDockGroupTest` exercises the Kotlin
 * equivalents in `AzUnattachedRail.kt` — this module is a line-for-line port of that file's
 * private functions (`resolvedPosition`, `edgeDockedPosition`, `clusterExtent`, `endDrag`, …),
 * adapted from Compose's `Offset`/`IntSize`/`dp` to plain pixel numbers.
 *
 * A `FloatingRailState` here is the mutable-per-rail record Kotlin keeps in `AzFloatingRailState`,
 * just as a plain, serializable-shaped object instead of a Compose `mutableStateOf` bag — the
 * owning React component (`AzUnattachedRail.tsx`) keeps a `Record<string, FloatingRailState>` in
 * `useState`/`useRef` and calls these functions to read/derive from it.
 */

/** Which screen edge a top-level `FLOATING` rail is pinned to; `FREE` is unpinned. */
export enum AzFloatingDock {
  FREE = 'FREE',
  TOP = 'TOP',
  BOTTOM = 'BOTTOM',
  OPPOSITE = 'OPPOSITE',
}

export interface Point {
  x: number;
  y: number;
}

export interface Size {
  width: number;
  height: number;
}

/** Runtime state for one top-level `FLOATING` unattached host — see the file doc above. */
export interface FloatingRailState {
  dock: AzFloatingDock;
  freeOffset: Point | null;
  /** Sort key among peers pinned to the same edge — a fraction of the window along that edge. */
  priority: number;
  /** This rail is docked to the right of the rail with this id, if any. At most one column over. */
  rightOf: string | null;
  /** This rail is docked below the rail with this id, if any. */
  belowOf: string | null;
  /** Measured size of this rail's own stack (host + its currently-unfolded children only). */
  size: Size;
  dragging: boolean;
  liveDragOffset: Point;
}

export function createFloatingRailState(
  dock: AzFloatingDock,
  freeOffset: Point | null,
  priority: number
): FloatingRailState {
  return {
    dock,
    freeOffset,
    priority,
    rightOf: null,
    belowOf: null,
    size: { width: 0, height: 0 },
    dragging: false,
    liveDragOffset: { x: 0, y: 0 },
  };
}

export type FloatingStates = Record<string, FloatingRailState>;

/** Geometry constants shared by every function below — one per `FloatingDockGroup` instance. */
export interface FloatingGeomConfig {
  spacingPx: number;
  edgeStartPx: number;
  edgeSnapPx: number;
  railSnapPx: number;
  grabBarHeightPx: number;
  minY: number;
  maxYBase: number;
  screenWidthPx: number;
  screenHeightPx: number;
  railOnLeft: boolean;
}

export function verticalCapacityPx(cfg: FloatingGeomConfig): number {
  return Math.max(0, cfg.maxYBase - cfg.minY);
}

/** `id` plus everything that (transitively) depends on it — never a valid attachment target. */
export function subtreeOf(
  states: FloatingStates,
  id: string,
  acc: Set<string> = new Set()
): Set<string> {
  if (acc.has(id)) return acc;
  acc.add(id);
  Object.entries(states).forEach(([otherId, st]) => {
    if (st.rightOf === id || st.belowOf === id) subtreeOf(states, otherId, acc);
  });
  return acc;
}

export function directRightDependent(
  states: FloatingStates,
  id: string
): string | undefined {
  return Object.keys(states).find((k) => states[k]!.rightOf === id);
}

export function directBelowDependent(
  states: FloatingStates,
  id: string
): string | undefined {
  return Object.keys(states).find((k) => states[k]!.belowOf === id);
}

/** `grabBarHeightPx` if `id` currently renders a grab bar above it, 0 otherwise. */
export function barHeightPx(
  states: FloatingStates,
  id: string,
  cfg: FloatingGeomConfig
): number {
  const st = states[id];
  if (!st) return 0;
  const isColumnRoot = st.rightOf === null && st.belowOf === null;
  const hasDependent =
    directRightDependent(states, id) !== undefined ||
    directBelowDependent(states, id) !== undefined;
  return isColumnRoot && hasDependent ? cfg.grabBarHeightPx : 0;
}

/** Bounding size of the little grid rooted at `id` (its own size plus every attached rail's). */
export function clusterExtent(
  states: FloatingStates,
  id: string,
  cfg: FloatingGeomConfig
): Size {
  const st = states[id];
  if (!st) return { width: 0, height: 0 };
  let w = st.size.width;
  let h = st.size.height + barHeightPx(states, id, cfg);
  const right = directRightDependent(states, id);
  if (right) {
    const e = clusterExtent(states, right, cfg);
    w += cfg.spacingPx + e.width;
    h = Math.max(h, e.height);
  }
  const below = directBelowDependent(states, id);
  if (below) {
    const e = clusterExtent(states, below, cfg);
    h += cfg.spacingPx + e.height;
    w = Math.max(w, e.width);
  }
  return { width: w, height: h };
}

/** The topmost rail of `id`'s column (walking up `belowOf` pointers). */
export function columnTopOf(states: FloatingStates, id: string): string {
  let cur = id;
  for (;;) {
    const st = states[cur];
    if (!st || st.belowOf === null) return cur;
    cur = st.belowOf;
  }
}

/** Every rail in `topId`'s column, top to bottom. */
export function columnMembers(states: FloatingStates, topId: string): string[] {
  const list = [topId];
  let cur = topId;
  for (;;) {
    const next = directBelowDependent(states, cur);
    if (!next) break;
    list.push(next);
    cur = next;
  }
  return list;
}

export function columnWorstCaseHeightPx(
  memberIds: string[],
  worstCaseHeightPx: (id: string) => number,
  spacingPx: number
): number {
  let total = 0;
  memberIds.forEach((id, i) => {
    total += worstCaseHeightPx(id);
    if (i < memberIds.length - 1) total += spacingPx;
  });
  return total;
}

export function rootsOnEdge(
  states: FloatingStates,
  edge: AzFloatingDock
): string[] {
  return Object.entries(states)
    .filter(
      ([, st]) => st.dock === edge && st.rightOf === null && st.belowOf === null
    )
    .sort((a, b) => a[1].priority - b[1].priority)
    .map(([id]) => id);
}

export function edgeDockedPosition(
  states: FloatingStates,
  id: string,
  edge: AzFloatingDock,
  cfg: FloatingGeomConfig
): Point {
  const siblings = rootsOnEdge(states, edge);
  const myIndex = siblings.indexOf(id);
  if (myIndex < 0) return { x: 0, y: 0 };
  const startPx = edge === AzFloatingDock.OPPOSITE ? cfg.minY : cfg.edgeStartPx;
  let along = startPx;
  for (let i = 0; i < myIndex; i++) {
    const extent = clusterExtent(states, siblings[i]!, cfg);
    along +=
      (edge === AzFloatingDock.OPPOSITE ? extent.height : extent.width) +
      cfg.spacingPx;
  }
  const mySize = states[id]?.size ?? { width: 0, height: 0 };
  switch (edge) {
    case AzFloatingDock.TOP:
      return { x: along, y: cfg.minY };
    case AzFloatingDock.BOTTOM:
      return { x: along, y: cfg.screenHeightPx - cfg.minY - mySize.height };
    case AzFloatingDock.OPPOSITE: {
      const x = cfg.railOnLeft ? cfg.screenWidthPx - mySize.width : 0;
      return { x, y: along };
    }
    case AzFloatingDock.FREE:
    default:
      return { x: 0, y: 0 };
  }
}

export function resolvedPosition(
  states: FloatingStates,
  id: string,
  cfg: FloatingGeomConfig,
  depth = 0
): Point {
  if (depth > 32) return { x: 0, y: 0 }; // Defensive cycle guard; attachment is cycle-free by construction.
  const st = states[id];
  if (!st) return { x: 0, y: 0 };
  if (st.dragging) return st.liveDragOffset;
  const rOf = st.rightOf;
  const bOf = st.belowOf;
  if (rOf !== null && states[rOf]) {
    const base = resolvedPosition(states, rOf, cfg, depth + 1);
    return { x: base.x + states[rOf]!.size.width + cfg.spacingPx, y: base.y };
  }
  if (bOf !== null && states[bOf]) {
    const base = resolvedPosition(states, bOf, cfg, depth + 1);
    return {
      x: base.x,
      y:
        base.y +
        states[bOf]!.size.height +
        barHeightPx(states, bOf, cfg) +
        cfg.spacingPx,
    };
  }
  if (st.dock === AzFloatingDock.FREE) return st.freeOffset ?? { x: 0, y: 0 };
  return edgeDockedPosition(states, id, st.dock, cfg);
}

/** Total width of `id`'s own row within its group (itself plus its rightward attachment chain). */
export function topRowWidthPx(
  states: FloatingStates,
  id: string,
  spacingPx: number
): number {
  const st = states[id];
  if (!st) return 0;
  const right = directRightDependent(states, id);
  return (
    st.size.width +
    (right ? spacingPx + topRowWidthPx(states, right, spacingPx) : 0)
  );
}

export type DropOutcome =
  | { kind: 'attach'; targetId: string; attachRight: boolean }
  | {
      kind: 'dock';
      dock: AzFloatingDock;
      freeOffset?: Point;
      priority?: number;
    };

/**
 * Resolves what should happen when `id` is dropped at its current `liveDragOffset` — port of
 * Kotlin's `endDrag`, split into "decide" (this function, pure) and "apply" (the caller mutates
 * `states` with the result). Mirrors both phases: rail-to-rail docking first, screen-edge docking
 * as the fallback.
 */
export function resolveDrop(
  states: FloatingStates,
  id: string,
  cfg: FloatingGeomConfig,
  worstCaseHeightPx: (hostId: string) => number
): DropOutcome {
  const st = states[id]!;
  const pos = st.liveDragOffset;
  const mySize = clusterExtent(states, id, cfg);
  const excluded = subtreeOf(states, id);

  // 1. Rail-to-rail docking: flush against another rail's right or bottom edge. The branch that
  // actually matches is captured directly, not re-derived after the fact — see the Kotlin
  // `endDrag`'s own comment on why re-deriving "which edge matched" from scratch is unsafe.
  for (const [otherId, other] of Object.entries(states)) {
    if (excluded.has(otherId)) continue;
    const otherPos = resolvedPosition(states, otherId, cfg);
    // The two-column cap applies to the whole COLUMN otherId belongs to, not just otherId itself:
    // otherId can be a rail attached BELOW the column-1 root (its own `rightOf` is null), which
    // would otherwise let a third column grow off of it.
    const otherColumnIsSecond =
      states[columnTopOf(states, otherId)]?.rightOf !== null;
    const nearRight =
      !otherColumnIsSecond &&
      other.rightOf === null &&
      directRightDependent(states, otherId) === undefined &&
      Math.abs(pos.x - (otherPos.x + other.size.width)) < cfg.railSnapPx &&
      pos.y < otherPos.y + other.size.height &&
      pos.y + mySize.height > otherPos.y;
    if (nearRight)
      return { kind: 'attach', targetId: otherId, attachRight: true };

    const nearBelowGeometry =
      directBelowDependent(states, otherId) === undefined &&
      Math.abs(pos.y - (otherPos.y + other.size.height)) < cfg.railSnapPx &&
      pos.x < otherPos.x + other.size.width &&
      pos.x + mySize.width > otherPos.x;
    if (nearBelowGeometry) {
      // Refuse if this column, with `id` AND everything already attached below `id` itself,
      // wouldn't fit on screen fully expanded — those dependants move with `id` (see
      // `resolvedPosition`'s `belowOf` branch), so they occupy space in the destination column
      // too, not just `id` alone.
      const members = [
        ...columnMembers(states, columnTopOf(states, otherId)),
        ...columnMembers(states, id),
      ];
      const fits =
        columnWorstCaseHeightPx(members, worstCaseHeightPx, cfg.spacingPx) <=
        verticalCapacityPx(cfg);
      if (fits)
        return { kind: 'attach', targetId: otherId, attachRight: false };
    }
  }

  // 2. Screen-edge docking.
  let newDock: AzFloatingDock;
  if (pos.y <= cfg.minY + cfg.edgeSnapPx) {
    newDock = AzFloatingDock.TOP;
  } else if (pos.y + mySize.height >= cfg.maxYBase - cfg.edgeSnapPx) {
    newDock = AzFloatingDock.BOTTOM;
  } else if (
    (cfg.railOnLeft &&
      pos.x + mySize.width >= cfg.screenWidthPx - cfg.edgeSnapPx) ||
    (!cfg.railOnLeft && pos.x <= cfg.edgeSnapPx)
  ) {
    newDock = AzFloatingDock.OPPOSITE;
  } else {
    newDock = AzFloatingDock.FREE;
  }

  if (newDock === AzFloatingDock.FREE) {
    const maxX = Math.max(0, cfg.screenWidthPx - mySize.width);
    const maxY = Math.max(cfg.minY, cfg.maxYBase - mySize.height);
    const clamped = {
      x: clamp(pos.x, 0, maxX),
      y: clamp(pos.y, cfg.minY, maxY),
    };
    return { kind: 'dock', dock: newDock, freeOffset: clamped };
  }
  const priority =
    newDock === AzFloatingDock.OPPOSITE
      ? clamp(pos.y / cfg.screenHeightPx, 0, 1)
      : clamp(pos.x / cfg.screenWidthPx, 0, 1);
  return { kind: 'dock', dock: newDock, priority };
}

function clamp(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v));
}
