import { orderBackgrounds, orderOnscreen } from '../AzNavHost';

/** Parity tests for the pages Z-ordering helpers (mirrors the Android AzPagesOrderingTest). */
describe('pages ordering', () => {
  const bg = (id: string, weight: number, page: number) => ({ id, weight, page, content: null });
  const on = (id: string, page: number) => ({ id, page, alignment: 'TopStart' as any, content: null });

  it('orders backgrounds higher-page-furthest-back when pages enabled', () => {
    const items = [bg('a', 0, 0), bg('b', 0, 2), bg('c', 0, 1)];
    expect(orderBackgrounds(items, true).map(i => i.page)).toEqual([2, 1, 0]);
  });

  it('breaks background ties within a page by weight (lower further back)', () => {
    const items = [bg('heavy', 5, 1), bg('light', 0, 1)];
    expect(orderBackgrounds(items, true).map(i => i.weight)).toEqual([0, 5]);
  });

  it('falls back to legacy weight sort when pages disabled', () => {
    const items = [bg('a', 10, 0), bg('b', 1, 5)];
    expect(orderBackgrounds(items, false).map(i => i.weight)).toEqual([1, 10]);
  });

  it('orders onscreen higher-page-furthest-back when pages enabled', () => {
    const items = [on('front', 0), on('back', 3), on('mid', 1)];
    expect(orderOnscreen(items, true).map(i => i.page)).toEqual([3, 1, 0]);
  });

  it('keeps declaration order within a page (stable)', () => {
    const items = [on('first', 1), on('second', 1)];
    expect(orderOnscreen(items, true).map(i => i.id)).toEqual(['first', 'second']);
  });

  it('preserves declaration order when pages disabled', () => {
    const items = [on('a', 9), on('b', 0)];
    expect(orderOnscreen(items, false).map(i => i.id)).toEqual(['a', 'b']);
  });
});
