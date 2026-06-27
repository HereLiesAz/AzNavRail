import type { AzEdge, AzGoal, AzGuideHighlight, AzInstruction } from './AzStatus';
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

/** The set of instructions to show right now, plus the goals already at their target. */
export interface GuidanceFrame {
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
    default:
      return h.type;
  }
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
): GuidanceFrame {
  const out = new Map<string, AzInstruction>();
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
    if (e) out.set(keyOf(e.instruction), e.instruction);
  }
  for (const e of edges) {
    if (e.to == null && activeStatuses.has(e.from)) out.set(keyOf(e.instruction), e.instruction);
  }
  return { instructions: Array.from(out.values()), reachedGoals: reached };
}

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
