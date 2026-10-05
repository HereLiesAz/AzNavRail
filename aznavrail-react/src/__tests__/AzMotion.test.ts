import { AzMotion, azCascadeDelayMs } from '../AzNavRailDefaults';

describe('count-normalized cascade', () => {
  it('spans the same total time at any count (22 × 8 = 176)', () => {
    expect(azCascadeDelayMs(3, 3, 22)).toBe(176);
    expect(azCascadeDelayMs(12, 12, 22)).toBe(176);
  });
  it('widens gaps for fewer items, tightens for more', () => {
    expect(azCascadeDelayMs(1, 3, 22)).toBe(58);
    expect(azCascadeDelayMs(1, 12, 22)).toBe(14);
  });
  it('handles degenerate input', () => {
    expect(azCascadeDelayMs(0, 0, 22)).toBe(0);
    expect(azCascadeDelayMs(-2, 5, 22)).toBe(0);
    expect(azCascadeDelayMs(1, 0, 22)).toBe(22);
  });
  it('settles within 650 ms for 1–40 items', () => {
    for (let n = 1; n <= 40; n++) {
      expect(azCascadeDelayMs(n - 1, n, AzMotion.ItemStaggerMs) + AzMotion.ItemDurationMs).toBeLessThanOrEqual(650);
    }
  });
});
