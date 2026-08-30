import {
  narrowForObstructions,
  overlapsAnyObstruction,
  type AzWindowObstruction,
} from '../components/AzWindow';

/**
 * Ports `AzWindowStateObstructionTest.kt`'s scenarios onto the React Native geometry
 * `AzWindow.tsx` mirrors it with: a full-height obstruction (a left/right-docked rail) must only
 * ever narrow the window's X range, and a full-width obstruction (a top/bottom bar) must only ever
 * narrow its Y range — even though a full-height rail touches both `top=0` and `bottom=screenHeight`
 * by construction, same as a full-width bar touches both `left=0` and `right=screenWidth`.
 */
describe('narrowForObstructions', () => {
  const screenWidth = 1000;
  const screenHeight = 800;
  const windowWidth = 200;
  const windowHeight = 150;

  it('a left-docked rail only narrows minX, leaving Y untouched', () => {
    const leftRail: AzWindowObstruction = {
      left: 0,
      top: 0,
      right: 80,
      bottom: screenHeight,
    };
    const bounds = narrowForObstructions(
      [leftRail],
      screenWidth,
      screenHeight,
      windowWidth,
      windowHeight,
      0,
      screenWidth - windowWidth,
      0,
      screenHeight - windowHeight
    );
    expect(bounds.minX).toBe(80);
    expect(bounds.maxX).toBe(screenWidth - windowWidth);
    // Bug regression: Y bounds must remain exactly what they were — never pinned by a rect that
    // merely happens to span the full container height.
    expect(bounds.minY).toBe(0);
    expect(bounds.maxY).toBe(screenHeight - windowHeight);
  });

  it('a right-docked rail only narrows maxX, leaving Y untouched', () => {
    const rightRail: AzWindowObstruction = {
      left: screenWidth - 80,
      top: 0,
      right: screenWidth,
      bottom: screenHeight,
    };
    const bounds = narrowForObstructions(
      [rightRail],
      screenWidth,
      screenHeight,
      windowWidth,
      windowHeight,
      0,
      screenWidth - windowWidth,
      0,
      screenHeight - windowHeight
    );
    expect(bounds.minX).toBe(0);
    expect(bounds.maxX).toBe(screenWidth - 80 - windowWidth);
    expect(bounds.minY).toBe(0);
    expect(bounds.maxY).toBe(screenHeight - windowHeight);
  });

  it('a top-docked bar only narrows minY, leaving X untouched', () => {
    const topBar: AzWindowObstruction = {
      left: 0,
      top: 0,
      right: screenWidth,
      bottom: 60,
    };
    const bounds = narrowForObstructions(
      [topBar],
      screenWidth,
      screenHeight,
      windowWidth,
      windowHeight,
      0,
      screenWidth - windowWidth,
      0,
      screenHeight - windowHeight
    );
    expect(bounds.minX).toBe(0);
    expect(bounds.maxX).toBe(screenWidth - windowWidth);
    expect(bounds.minY).toBe(60);
    expect(bounds.maxY).toBe(screenHeight - windowHeight);
  });

  it('a degenerate full-screen obstruction narrows both axes', () => {
    const fullScreen: AzWindowObstruction = {
      left: 0,
      top: 0,
      right: screenWidth,
      bottom: screenHeight,
    };
    const bounds = narrowForObstructions(
      [fullScreen],
      screenWidth,
      screenHeight,
      windowWidth,
      windowHeight,
      0,
      screenWidth - windowWidth,
      0,
      screenHeight - windowHeight
    );
    // Nowhere valid on either axis; both collapse to the single point the min/max coercion
    // converges on, rather than one axis silently staying wide open.
    expect(bounds.minX).toBe(bounds.maxX);
    expect(bounds.minY).toBe(bounds.maxY);
  });

  it('returns the input bounds untouched when there are no obstructions', () => {
    const bounds = narrowForObstructions(
      [],
      screenWidth,
      screenHeight,
      windowWidth,
      windowHeight,
      10,
      500,
      20,
      400
    );
    expect(bounds).toEqual({ minX: 10, maxX: 500, minY: 20, maxY: 400 });
  });
});

describe('overlapsAnyObstruction', () => {
  const leftRail: AzWindowObstruction = {
    left: 0,
    top: 0,
    right: 80,
    bottom: 800,
  };

  it('is true when the window sits on the rail', () => {
    expect(overlapsAnyObstruction(20, 300, 200, 150, [leftRail])).toBe(true);
  });

  it('is false when the window is clear of the rail', () => {
    expect(overlapsAnyObstruction(500, 300, 200, 150, [leftRail])).toBe(false);
  });

  it('is false with no obstructions at all', () => {
    expect(overlapsAnyObstruction(20, 300, 200, 150, [])).toBe(false);
  });
});
