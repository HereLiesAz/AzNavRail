import { placeCallout, overlapArea } from '../guidance/AzCalloutPlacement';
import type { AzShapeBounds } from '../guidance/AzStatus';

/** Parity with the Kotlin AzCalloutPlacementTest. */

const safe: AzShapeBounds = { left: 0, top: 0, width: 1000, height: 2000 };
const size = { width: 240, height: 100 };

describe('placeCallout', () => {
  it('overlapArea computes the intersection', () => {
    expect(overlapArea({ left: 0, top: 0, width: 10, height: 10 }, { left: 5, top: 5, width: 10, height: 10 })).toBe(25);
    expect(overlapArea({ left: 0, top: 0, width: 10, height: 10 }, { left: 20, top: 20, width: 10, height: 10 })).toBe(0);
  });

  it('prefers below the target when there is room', () => {
    const r = placeCallout({ left: 400, top: 400, width: 200, height: 100 }, size, [], safe);
    expect(r.left).toBe(400);
    expect(r.top).toBe(508);
  });

  it('never covers the target', () => {
    const t = { left: 400, top: 400, width: 200, height: 100 };
    expect(overlapArea(placeCallout(t, size, [], safe), t)).toBe(0);
  });

  it('flips above when below would leave the safe area', () => {
    const r = placeCallout({ left: 400, top: 1850, width: 200, height: 100 }, size, [], safe);
    expect(r.top).toBe(1742);
    expect(r.top + r.height).toBeLessThanOrEqual(safe.height);
  });

  it('avoids an obstacle blocking the preferred side', () => {
    const t = { left: 400, top: 400, width: 200, height: 100 };
    const ob = { left: 380, top: 500, width: 300, height: 200 };
    const r = placeCallout(t, size, [ob], safe);
    expect(overlapArea(r, ob)).toBe(0);
    expect(overlapArea(r, t)).toBe(0);
  });

  it('stays fully inside the safe area', () => {
    const r = placeCallout({ left: 950, top: 50, width: 49, height: 49 }, size, [], safe);
    expect(r.left >= 0 && r.left + r.width <= 1000).toBe(true);
    expect(r.top >= 0 && r.top + r.height <= 2000).toBe(true);
  });

  it('untargeted callouts spread off each other', () => {
    const placed = { left: 0, top: 0, width: 240, height: 100 };
    const r = placeCallout(null, size, [placed], safe);
    expect(overlapArea(r, placed)).toBe(0);
  });
});
