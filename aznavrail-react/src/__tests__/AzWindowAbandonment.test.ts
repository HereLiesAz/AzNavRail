import { offscreenFraction, AzWindowDefaults } from '../components/AzWindow';

/**
 * `offscreenFraction` is the geometry `AzWindow`'s abandonment timer uses to decide, once a
 * dragged window has sat untouched for `AzWindowDefaults.abandonedGraceMs`, whether to pull it
 * back onscreen or dismiss it outright — mirrors `AzWindowState.offscreenFraction` on the
 * Kotlin/Compose side.
 */
describe('offscreenFraction', () => {
  const screenWidth = 1000;
  const screenHeight = 800;
  const windowWidth = 200;
  const windowHeight = 150;

  it('is zero for a window fully inside the screen', () => {
    expect(
      offscreenFraction(
        400,
        300,
        windowWidth,
        windowHeight,
        screenWidth,
        screenHeight
      )
    ).toBe(0);
  });

  it('reports half offscreen when exactly half hangs off the right edge', () => {
    expect(
      offscreenFraction(
        screenWidth - 100,
        300,
        windowWidth,
        windowHeight,
        screenWidth,
        screenHeight
      )
    ).toBeCloseTo(0.5, 5);
  });

  it('is fully offscreen with no overlap at all', () => {
    expect(
      offscreenFraction(
        screenWidth + 50,
        300,
        windowWidth,
        windowHeight,
        screenWidth,
        screenHeight
      )
    ).toBe(1);
  });

  it('clears the nearly-gone threshold once only a sliver remains onscreen', () => {
    // Only a 10px sliver (5% of the 200px width) remains onscreen.
    const fraction = offscreenFraction(
      screenWidth - 10,
      300,
      windowWidth,
      windowHeight,
      screenWidth,
      screenHeight
    );
    expect(fraction).toBeGreaterThanOrEqual(
      AzWindowDefaults.nearlyGoneFraction
    );
  });

  it('does not clear the nearly-gone threshold while half onscreen', () => {
    const fraction = offscreenFraction(
      screenWidth - 100,
      300,
      windowWidth,
      windowHeight,
      screenWidth,
      screenHeight
    );
    expect(fraction).toBeLessThan(AzWindowDefaults.nearlyGoneFraction);
  });
});
