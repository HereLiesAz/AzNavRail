import { nextHop, routeInstructions } from '../guidance/AzGuidance';
import type { AzEdge, AzGoal } from '../guidance/AzStatus';

/** Verifies BFS next-hop routing and the multi-goal instruction frame (parity with the Kotlin tests). */

const edge = (from: string, to: string | null, text: string, item?: string): AzEdge => ({
  from,
  to,
  instruction: { text, highlight: item ? { type: 'Item', id: item } : { type: 'None' } },
});

// a --Do A--> b --Do B--> c
const chain: AzEdge[] = [edge('a', 'b', 'Do A', 'ia'), edge('b', 'c', 'Do B', 'ib')];

describe('nextHop', () => {
  it('returns the first edge on the shortest path', () => {
    expect(nextHop(chain, new Set(['a']), 'c')?.instruction.text).toBe('Do A');
  });

  it('re-routes as the user advances', () => {
    expect(nextHop(chain, new Set(['b']), 'c')?.instruction.text).toBe('Do B');
  });

  it('is null when the target is already true', () => {
    expect(nextHop(chain, new Set(['c']), 'c')).toBeNull();
  });

  it('is null when the target is unreachable', () => {
    expect(nextHop(chain, new Set(['x']), 'c')).toBeNull();
  });

  it('prefers the shorter of two paths', () => {
    const edges = [...chain, edge('a', 'c', 'Shortcut', 'sc')];
    expect(nextHop(edges, new Set(['a']), 'c')?.instruction.text).toBe('Shortcut');
  });
});

describe('routeInstructions', () => {
  it('reports a goal already at its target as reached', () => {
    const goals: Record<string, AzGoal> = { g: { id: 'g', target: 'c' } };
    const frame = routeInstructions(chain, goals, ['g'], new Set(['c']));
    expect(frame.reachedGoals.has('g')).toBe(true);
    expect(frame.instructions).toHaveLength(0);
  });

  it('surfaces a next hop per active goal simultaneously', () => {
    const edges = [edge('root', 'p', 'Tap P', 'ip'), edge('root', 'q', 'Tap Q', 'iq')];
    const goals: Record<string, AzGoal> = {
      gp: { id: 'gp', target: 'p' },
      gq: { id: 'gq', target: 'q' },
    };
    const frame = routeInstructions(edges, goals, ['gp', 'gq'], new Set(['root']));
    expect(new Set(frame.instructions.map((i) => i.text))).toEqual(new Set(['Tap P', 'Tap Q']));
    expect(frame.reachedGoals.size).toBe(0);
  });

  it('dedupes a hop shared by two goals', () => {
    const goals: Record<string, AzGoal> = {
      g1: { id: 'g1', target: 'b' },
      g2: { id: 'g2', target: 'c' },
    };
    const frame = routeInstructions(chain, goals, ['g1', 'g2'], new Set(['a']));
    expect(frame.instructions.map((i) => i.text)).toEqual(['Do A']);
  });

  it('includes passive tips while their from holds', () => {
    const edges = [...chain, edge('a', null, 'Heads up', 'ip')];
    const goals: Record<string, AzGoal> = { g: { id: 'g', target: 'c' } };
    const frame = routeInstructions(edges, goals, ['g'], new Set(['a']));
    const texts = new Set(frame.instructions.map((i) => i.text));
    expect(texts.has('Do A')).toBe(true);
    expect(texts.has('Heads up')).toBe(true);
  });
});
