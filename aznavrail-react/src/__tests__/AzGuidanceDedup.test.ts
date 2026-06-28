import { routeInstructions } from '../guidance/AzGuidance';
import type { AzEdge, AzGoal } from '../guidance/AzStatus';

/** The no-repeat rule: a status consumed (a shown hop's target that was reached) is never re-shown. */

const edge = (from: string, to: string | null, text: string): AzEdge => ({ from, to, instruction: { text } });

describe('routeInstructions de-dup', () => {
  it('does not re-show a consumed hop', () => {
    const edges = [edge('a', 'b', 'Open menu'), edge('b', 'c', 'Tap X')];
    const goals: Record<string, AzGoal> = { g: { id: 'g', target: 'c' } };
    // From {a}: the first hop shows.
    expect(routeInstructions(edges, goals, ['g'], new Set(['a'])).instructions[0].text).toBe('Open menu');
    // With "b" consumed, routing skips that hop even though we're back at {a}.
    const f = routeInstructions(edges, goals, ['g'], new Set(['a']), () => 0, new Set(['b']));
    expect(f.instructions[0].text).toBe('Tap X');
  });
});
