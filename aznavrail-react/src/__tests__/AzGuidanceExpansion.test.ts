import { routeInstructions } from '../guidance/AzGuidance';
import {
  AZ_ITEM_ACTIVE,
  resolveAzHighlight,
  resolveItemId,
  resolveShape,
  shapeBounds,
} from '../guidance/AzStatus';
import { anySuppressorActive } from '../guidance/AzStatusEngine';
import type { AzEdge, AzGoal, AzGuideShape, AzItemBounds } from '../guidance/AzStatus';

/** Parity with the Kotlin AzGuidanceExpansionTest: paged steps, highlight resolution, suppression. */

const stepped: AzEdge = {
  from: 'a',
  to: 'done',
  instruction: { text: 'step0' },
  steps: [
    { text: 'step0' },
    { text: 'step1', highlightTargetId: 'ball', advanceWhen: 'x' },
    { text: 'step2' },
  ],
};
const goals: Record<string, AzGoal> = { g: { id: 'g', target: 'done' } };

describe('paged routing', () => {
  it('shows the step at the cursor', () => {
    const f0 = routeInstructions([stepped], goals, ['g'], new Set(['a']), () => 0);
    expect(f0.instructions[0].text).toBe('step0');
    expect(f0.resolved[0].stepIndex).toBe(0);
    expect(f0.resolved[0].stepTotal).toBe(3);

    const f1 = routeInstructions([stepped], goals, ['g'], new Set(['a']), () => 1);
    expect(f1.instructions[0].text).toBe('step1');
    expect(f1.resolved[0].instruction.highlight).toEqual({ type: 'Target', id: 'ball' });
  });

  it('reactive advanceWhen wins over the tap cursor', () => {
    const f = routeInstructions([stepped], goals, ['g'], new Set(['a', 'x']), () => 1);
    expect(f.instructions[0].text).toBe('step2');
    expect(f.resolved[0].stepIndex).toBe(2);
  });

  it('does not skip an informational step when a later step status is already true', () => {
    const f = routeInstructions([stepped], goals, ['g'], new Set(['a', 'x']), () => 0);
    expect(f.instructions[0].text).toBe('step0');
  });

  it('reaches a paged goal when its target becomes true', () => {
    const f = routeInstructions([stepped], goals, ['g'], new Set(['a', 'done']), () => 1);
    expect(f.reachedGoals.has('g')).toBe(true);
    expect(f.instructions).toHaveLength(0);
  });

  it('keeps single-callout edges unchanged (stepTotal 1)', () => {
    const edge: AzEdge = { from: 'a', to: 'b', instruction: { text: 'Do A', highlight: { type: 'Item', id: 'ia' } } };
    const f = routeInstructions([edge], { g: { id: 'g', target: 'b' } }, ['g'], new Set(['a']));
    expect(f.instructions[0].text).toBe('Do A');
    expect(f.resolved[0].stepTotal).toBe(1);
  });
});

describe('highlight + shape resolution', () => {
  it('resolveAzHighlight precedence is target, selector, active token, item, none', () => {
    expect(resolveAzHighlight('item', () => 'sel', 't')).toEqual({ type: 'Target', id: 't' });
    expect(resolveAzHighlight('item', () => 'sel', undefined).type).toBe('Dynamic');
    expect(resolveAzHighlight(AZ_ITEM_ACTIVE, undefined, undefined)).toEqual({ type: 'ActiveItem' });
    expect(resolveAzHighlight('item', undefined, undefined)).toEqual({ type: 'Item', id: 'item' });
    expect(resolveAzHighlight(undefined, undefined, undefined)).toEqual({ type: 'None' });
  });

  it('resolveItemId folds ActiveItem and Dynamic against the live active id', () => {
    expect(resolveItemId({ type: 'ActiveItem' }, 'home')).toBe('home');
    expect(resolveItemId({ type: 'Dynamic', selector: () => 'layer.1' }, null)).toBe('layer.1');
    expect(resolveItemId({ type: 'Dynamic', selector: () => AZ_ITEM_ACTIVE }, 'home')).toBe('home');
    expect(resolveItemId({ type: 'Target', id: 't' }, 'home')).toBeNull();
  });

  it('resolveShape resolves targets, areas and items, degrading to null', () => {
    const circle: AzGuideShape = { type: 'Circle', cx: 1, cy: 2, radius: 3 };
    const targets = { t: () => circle, gone: () => null };
    expect(resolveShape({ type: 'Target', id: 't' }, {}, null, targets)).toEqual(circle);
    expect(resolveShape({ type: 'Target', id: 'gone' }, {}, null, targets)).toBeNull();
    expect(resolveShape({ type: 'Target', id: 'missing' }, {}, null, targets)).toBeNull();
    expect(resolveShape({ type: 'Area', left: 5, top: 6, width: 7, height: 8 }, {}, null, {})?.type).toBe('Rect');

    const cache: Record<string, AzItemBounds> = { home: { x: 0, y: 0, width: 10, height: 10 } };
    expect(resolveShape({ type: 'Item', id: 'home' }, cache, null, {})?.type).toBe('Rect');
    expect(resolveShape({ type: 'Item', id: 'nope' }, cache, null, {})).toBeNull();
    expect(resolveShape({ type: 'ActiveItem' }, cache, 'home', {})?.type).toBe('Rect');
    expect(resolveShape({ type: 'ActiveItem' }, cache, null, {})).toBeNull();
  });

  it('shapeBounds wraps each geometry with padding', () => {
    expect(shapeBounds({ type: 'Circle', cx: 100, cy: 50, radius: 10, padding: 5 })).toEqual({ left: 85, top: 35, width: 30, height: 30 });
    expect(shapeBounds({ type: 'Rect', left: 10, top: 20, width: 30, height: 40 })).toEqual({ left: 10, top: 20, width: 30, height: 40 });
    expect(
      shapeBounds({ type: 'Path', commands: [{ type: 'M', x: 0, y: 0 }, { type: 'Q', x1: 5, y1: 30, x: 10, y: 20 }, { type: 'Z' }] }),
    ).toEqual({ left: 0, top: 0, width: 10, height: 30 });
  });
});

describe('suppression', () => {
  it('anySuppressorActive ORs predicates and treats a throw as false', () => {
    expect(anySuppressorActive([])).toBe(false);
    expect(anySuppressorActive([[700, () => true]])).toBe(true);
    expect(anySuppressorActive([[700, () => false]])).toBe(false);
    expect(anySuppressorActive([[0, () => false], [0, () => true]])).toBe(true);
    expect(anySuppressorActive([[0, () => { throw new Error('boom'); }]])).toBe(false);
  });
});
