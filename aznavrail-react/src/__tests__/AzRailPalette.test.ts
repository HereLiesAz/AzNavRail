import {
  AzChromeColors,
  azContrastRatio,
  azInkOn,
  azReadableOn,
  resolveRailAccent,
} from '../AzRailPalette';

/**
 * The rail publishes the colour it is actually wearing so every other AzNavRail surface can wear it
 * too, and the panel guard makes sure whatever arrives can be read. Both failures they guard
 * against — a drop-down in the app theme's purple beside a white rail, and near-black words on a
 * near-black panel — are invisible to a type checker and obvious to a user.
 */
describe('resolveRailAccent', () => {
  const item = (color?: string) => ({ color });

  it('lets an explicit activeColor win outright', () => {
    expect(
      resolveRailAccent('#ff0000', [item('#ffffff'), item('#ffffff')])
    ).toBe('#ff0000');
  });

  it('reads the rail as the colour most of its items are', () => {
    expect(
      resolveRailAccent(undefined, [
        item('#ffffff'),
        item('#ffffff'),
        item('#00ff00'),
        item(undefined),
      ])
    ).toBe('#ffffff');
  });

  it('leaves the fallback in charge when the rail states no colour', () => {
    expect(resolveRailAccent(undefined, [item(), item()])).toBeUndefined();
  });
});

describe('panel legibility', () => {
  it('matches the WCAG contrast scale', () => {
    expect(azContrastRatio('#000000', '#ffffff')).toBeCloseTo(21, 2);
    expect(azContrastRatio('#ffffff', '#ffffff')).toBeCloseTo(1, 2);
  });

  it('writes ink that follows the ground', () => {
    expect(azInkOn(AzChromeColors.Ground)).toBe(AzChromeColors.Ink);
    expect(azInkOn('#ffffff')).toBe(AzChromeColors.InkInverse);
  });

  it('keeps an accent that reads on the panel exactly as given', () => {
    expect(azReadableOn(AzChromeColors.Ground, '#ffffff')).toBe('#ffffff');
    expect(azReadableOn('#ffffff', '#000000')).toBe('#000000');
  });

  it('replaces the theme purple that produced the unreadable menu', () => {
    // The default light primary a drop-down landed on when it could not see the rail's palette. On
    // a near-black panel it is dark words on a dark panel — the screenshot that started this.
    expect(azReadableOn(AzChromeColors.Ground, '#6650A4')).toBe(
      AzChromeColors.Ink
    );
    // The same purple on a light panel reads perfectly well and is left alone. The guard replaces
    // colours that fail on the ground they land on, not colours it dislikes.
    expect(azReadableOn('#ffffff', '#6650A4')).toBe('#6650A4');
  });

  it('judges an accent as it will be seen, composited over the panel', () => {
    expect(azReadableOn(AzChromeColors.Ground, 'rgba(255,255,255,0.05)')).toBe(
      AzChromeColors.Ink
    );
    expect(azReadableOn(AzChromeColors.Ground, 'rgba(255,255,255,1)')).toBe(
      'rgba(255,255,255,1)'
    );
  });

  it('still yields ink when the rail states no colour at all', () => {
    expect(azReadableOn(AzChromeColors.Ground, undefined)).toBe(
      AzChromeColors.Ink
    );
  });
});
