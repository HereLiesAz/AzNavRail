import type {
  AzEdge,
  AzGoal,
  AzGuideHighlight,
  AzGuidanceSnapshot,
  AzGuideShape,
  AzInstruction,
  AzItemBounds,
} from './AzStatus';
import { edgeStepKey, resolveShape, resolveTargetId, shapeBounds, stepHighlight, stepToInstruction } from './AzStatus';
import type { AzNavItem } from '../types';

/**
 * BFS the status flowchart from the currently-true `activeStatuses` to `target`; return the first edge
 * on a shortest path (its instruction is the next hop), or null if `target` is already true or
 * unreachable. Only interactive edges (`to != null`) are traversed.
 */
export function nextHop(edges: AzEdge[], activeStatuses: Set<string>, target: string): AzEdge | null {
  if (activeStatuses.has(target)) return null;
  const adjacency: Record<string, AzEdge[]> = {};
  for (const e of edges) {
    if (e.to == null) continue;
    (adjacency[e.from] ||= []).push(e);
  }
  const visited = new Set<string>(activeStatuses);
  const queue: Array<{ status: string; firstEdge: AzEdge }> = [];
  activeStatuses.forEach((start) => {
    (adjacency[start] || []).forEach((e) => {
      const to = e.to as string;
      if (!visited.has(to)) {
        visited.add(to);
        queue.push({ status: to, firstEdge: e });
      }
    });
  });
  while (queue.length) {
    const { status, firstEdge } = queue.shift()!;
    if (status === target) return firstEdge;
    (adjacency[status] || []).forEach((e) => {
      const to = e.to as string;
      if (!visited.has(to)) {
        visited.add(to);
        queue.push({ status: to, firstEdge });
      }
    });
  }
  return null;
}

/** One instruction the engine has chosen to show, resolved to its current paged step. */
export interface ResolvedInstruction {
  instruction: AzInstruction;
  edge: AzEdge;
  goalId: string | null;
  stepIndex: number;
  stepTotal: number;
}

/** The set of instructions to show right now, plus the goals already at their target. */
export interface GuidanceFrame {
  resolved: ResolvedInstruction[];
  /** Back-compat view: just the per-step instructions to render. */
  instructions: AzInstruction[];
  reachedGoals: Set<string>;
}

function highlightKey(h?: AzGuideHighlight): string {
  if (!h) return 'none';
  switch (h.type) {
    case 'Item':
      return `item:${h.id}`;
    case 'Area':
      return `area:${h.left},${h.top},${h.width},${h.height}`;
    case 'Target':
      return `target:${h.id}`;
    default:
      return h.type;
  }
}

/**
 * Resolves an edge to the instruction for its current step. A non-paged edge resolves to its single
 * instruction; a paged edge consults `stepIndexOf` then advances past leading steps whose `advanceWhen`
 * is already satisfied (reactive advance wins over the tap cursor). Mirrors Kotlin `resolveEdge`.
 */
export function resolveEdge(
  edge: AzEdge,
  goalId: string | null,
  stepIndexOf: (key: string) => number,
  activeStatuses: Set<string>,
): ResolvedInstruction {
  const steps = edge.steps ?? [];
  if (steps.length === 0) return { instruction: edge.instruction, edge, goalId, stepIndex: 0, stepTotal: 1 };
  const last = steps.length - 1;
  let idx = Math.max(0, Math.min(stepIndexOf(edgeStepKey(edge)), last));
  while (idx < last && steps[idx].advanceWhen != null && activeStatuses.has(steps[idx].advanceWhen as string)) idx++;
  return { instruction: stepToInstruction(steps[idx], edge.instruction.title), edge, goalId, stepIndex: idx, stepTotal: steps.length };
}

/**
 * The next-hop instruction for each active goal not yet reached, plus any passive edge whose `from` is
 * currently true. Deduped by (text + highlight). Goals already at their target are reported in
 * `reachedGoals` (the caller should `markReached` them). Mirrors the Kotlin `routeInstructions`.
 */
export function routeInstructions(
  edges: AzEdge[],
  goals: Record<string, AzGoal>,
  activeGoalIds: string[],
  activeStatuses: Set<string>,
  stepIndexOf: (key: string) => number = () => 0,
): GuidanceFrame {
  const out = new Map<string, ResolvedInstruction>();
  const reached = new Set<string>();
  const keyOf = (ins: AzInstruction) => `${ins.text}|${highlightKey(ins.highlight)}`;
  for (const gid of activeGoalIds) {
    const goal = goals[gid];
    if (!goal) continue;
    if (activeStatuses.has(goal.target)) {
      reached.add(gid);
      continue;
    }
    const e = nextHop(edges, activeStatuses, goal.target);
    if (e) {
      const r = resolveEdge(e, gid, stepIndexOf, activeStatuses);
      out.set(keyOf(r.instruction), r);
    }
  }
  for (const e of edges) {
    if (e.to == null && activeStatuses.has(e.from)) {
      const r = resolveEdge(e, null, stepIndexOf, activeStatuses);
      out.set(keyOf(r.instruction), r);
    }
  }
  const resolved = Array.from(out.values());
  return { resolved, instructions: resolved.map((r) => r.instruction), reachedGoals: reached };
}

/** Builds the public `AzGuidanceSnapshot` for a resolved instruction (the `shape` resolved by caller). */
export function toSnapshot(
  r: ResolvedInstruction,
  shape: AzGuideShape | null,
  activeItemId: string | null,
): AzGuidanceSnapshot {
  const h = r.instruction.highlight ?? { type: 'None' };
  return {
    text: r.instruction.text,
    title: r.instruction.title,
    goalId: r.goalId,
    highlight: h,
    targetId: resolveTargetId(h, activeItemId),
    resolvedShape: shape,
    resolvedBounds: shape ? shapeBounds(shape) : null,
    stepIndex: r.stepIndex,
    stepTotal: r.stepTotal,
    stepKey: edgeStepKey(r.edge),
  };
}

/**
 * Resolves each resolved instruction to a snapshot. Moving-target shapes are intentionally NOT invoked
 * here (the host has its own shape and gets the `targetId`); only stable rail-item bounds are resolved.
 */
export function snapshotsOf(
  resolved: ResolvedInstruction[],
  cache: Record<string, AzItemBounds>,
  activeItemId: string | null,
): AzGuidanceSnapshot[] {
  return resolved.map((r) => {
    const h = r.instruction.highlight ?? { type: 'None' };
    // Don't invoke moving target lambdas here (the host has its own shape + the targetId).
    const shape = h.type === 'Target' ? null : resolveShape(h, cache, activeItemId, {});
    return toSnapshot(r, shape, activeItemId);
  });
}

// Re-exported for the overlay/layer and tests.
export { resolveShape, stepHighlight, edgeStepKey };

/**
 * Derives the guidance **auto-edges** for the rail's own affordances, so the developer only hand-authors
 * edges whose `to` is a custom status: "Open the menu", tap a host / nested-rail / routed item. Rail
 * items are tappable from anywhere (`az.app.ready`); menu-only items require the menu open
 * (`az.rail.expanded`).
 */
export function computeAutoEdges(
  items: AzNavItem[],
  openMenuLabel = 'Open the menu',
  tapLabel: (label: string) => string = (s) => `Tap ${s}`,
): AzEdge[] {
  const edges: AzEdge[] = [];
  edges.push({
    from: 'az.rail.collapsed',
    to: 'az.rail.expanded',
    instruction: { text: openMenuLabel, highlight: { type: 'None' } },
  });
  for (const item of items) {
    if (item.isDivider) continue;
    const visibleFrom = item.isRailItem ? 'az.app.ready' : 'az.rail.expanded';
    const highlight: AzGuideHighlight = { type: 'Item', id: item.id };
    const tap = tapLabel(item.text || item.id);
    if (item.isHost) {
      edges.push({ from: visibleFrom, to: `az.host.${item.id}.expanded`, instruction: { text: tap, highlight } });
    } else if (item.isNestedRail) {
      edges.push({ from: visibleFrom, to: `az.nestedRail.${item.id}.open`, instruction: { text: tap, highlight } });
    } else if (item.route) {
      edges.push({ from: visibleFrom, to: `az.screen.${item.route}`, instruction: { text: tap, highlight } });
    }
  }
  return edges;
}
