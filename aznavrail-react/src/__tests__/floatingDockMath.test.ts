import {
  AzFloatingDock,
  FloatingGeomConfig,
  FloatingStates,
  createFloatingRailState,
  resolvedPosition,
  resolveDrop,
  topRowWidthPx,
} from '../services/floatingDockMath';

/**
 * Translates the scenarios in Android/CMP's `AzFloatingDockGroupTest.kt` — screen-edge docking,
 * rail-to-rail attach-on-flush-drag, the two-column cap, per-column capacity refusal, and the
 * grab-bar group-drag contract — onto the pure geometry this package ports them to
 * (`services/floatingDockMath.ts`). Exercised directly rather than through `PanResponder` gestures:
 * React Native's gesture responder system has no reliable synthetic-touch injection the way
 * Compose's `performTouchInput` does, so the encoded behavioral contract is tested at the same
 * layer the Kotlin private functions (`resolvedPosition`, `endDrag`, `clusterExtent`, …) live at.
 */

const cfg: FloatingGeomConfig = {
  spacingPx: 8,
  edgeStartPx: 8,
  edgeSnapPx: 56,
  railSnapPx: 24,
  grabBarHeightPx: 10,
  minY: 80, // 10% of an 800px-tall screen
  maxYBase: 720, // 90% of an 800px-tall screen
  screenWidthPx: 400,
  screenHeightPx: 800,
  railOnLeft: true,
};

function withSize(
  states: FloatingStates,
  id: string,
  width: number,
  height: number
): void {
  states[id]!.size = { width, height };
}

describe('floatingDockMath', () => {
  describe('screen-edge docking', () => {
    it('docks to TOP when dropped within the top edge snap threshold', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 100, y: 200 }, 0),
      };
      withSize(states, 'a', 72, 72);
      states.a!.dragging = true;
      states.a!.liveDragOffset = { x: 100, y: cfg.minY + 10 }; // within edgeSnapPx of minY
      const outcome = resolveDrop(states, 'a', cfg, () => 72);
      expect(outcome).toEqual({
        kind: 'dock',
        dock: AzFloatingDock.TOP,
        priority: expect.any(Number),
      });
    });

    it('docks to BOTTOM when dropped within the bottom edge snap threshold', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 100, y: 200 }, 0),
      };
      withSize(states, 'a', 72, 72);
      states.a!.dragging = true;
      states.a!.liveDragOffset = { x: 100, y: cfg.maxYBase - 72 - 10 };
      const outcome = resolveDrop(states, 'a', cfg, () => 72);
      expect(outcome.kind).toBe('dock');
      if (outcome.kind === 'dock')
        expect(outcome.dock).toBe(AzFloatingDock.BOTTOM);
    });

    it('docks to OPPOSITE when dropped against the edge opposite a left-docked rail', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 100, y: 300 }, 0),
      };
      withSize(states, 'a', 72, 72);
      states.a!.dragging = true;
      // railOnLeft: true, so OPPOSITE is the right edge.
      states.a!.liveDragOffset = { x: cfg.screenWidthPx - 72 - 10, y: 300 };
      const outcome = resolveDrop(states, 'a', cfg, () => 72);
      expect(outcome.kind).toBe('dock');
      if (outcome.kind === 'dock')
        expect(outcome.dock).toBe(AzFloatingDock.OPPOSITE);
    });

    it('stays FREE and clamps to the safe zone when dropped away from every edge', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 100, y: 300 }, 0),
      };
      withSize(states, 'a', 72, 72);
      states.a!.dragging = true;
      states.a!.liveDragOffset = { x: 150, y: 400 };
      const outcome = resolveDrop(states, 'a', cfg, () => 72);
      expect(outcome).toEqual({
        kind: 'dock',
        dock: AzFloatingDock.FREE,
        freeOffset: { x: 150, y: 400 },
      });
    });

    it('multiple rails docked to the same edge line up adjacent, not overlapping', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.TOP, null, 0),
        b: createFloatingRailState(AzFloatingDock.TOP, null, 1),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      const posA = resolvedPosition(states, 'a', cfg);
      const posB = resolvedPosition(states, 'b', cfg);
      expect(posA).toEqual({ x: cfg.edgeStartPx, y: cfg.minY });
      expect(posB.x).toBeCloseTo(cfg.edgeStartPx + 72 + cfg.spacingPx);
      expect(posB.y).toBe(cfg.minY);
    });
  });

  describe('rail-to-rail attach-on-flush-drag', () => {
    it("attaches to the right when dropped flush against another rail's right edge", () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 300 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, { x: 400, y: 500 }, 0),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      states.b!.dragging = true;
      // Flush against A's right edge (40 + 72 = 112), vertically overlapping A.
      states.b!.liveDragOffset = { x: 112 + 2, y: 300 };
      const outcome = resolveDrop(states, 'b', cfg, () => 72);
      expect(outcome).toEqual({
        kind: 'attach',
        targetId: 'a',
        attachRight: true,
      });
    });

    it("attaches below when dropped flush against another rail's bottom edge and it fits", () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, { x: 400, y: 500 }, 0),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      states.b!.dragging = true;
      states.b!.liveDragOffset = { x: 40, y: 172 + 2 };
      const outcome = resolveDrop(states, 'b', cfg, () => 72);
      expect(outcome).toEqual({
        kind: 'attach',
        targetId: 'a',
        attachRight: false,
      });
    });

    it("resolves the attached rail's position relative to its target", () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, null, 0),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 60, 60);
      states.b!.rightOf = 'a';
      const posA = resolvedPosition(states, 'a', cfg);
      const posB = resolvedPosition(states, 'b', cfg);
      expect(posB).toEqual({ x: posA.x + 72 + cfg.spacingPx, y: posA.y });
    });
  });

  describe('two-column cap', () => {
    it('never attaches a third column: a rail already in the second column refuses another to its right', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 0, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, null, 0), // already column 2 (rightOf a)
        c: createFloatingRailState(AzFloatingDock.FREE, { x: 300, y: 400 }, 0),
      };
      states.b!.rightOf = 'a';
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      withSize(states, 'c', 72, 72);
      const posB = resolvedPosition(states, 'b', cfg);
      states.c!.dragging = true;
      // Flush against B's right edge — would form a THIRD column, which must be refused.
      states.c!.liveDragOffset = { x: posB.x + 72 + 2, y: posB.y };
      const outcome = resolveDrop(states, 'c', cfg, () => 72);
      // Refused as an attachment; falls through to ordinary FREE/edge docking instead.
      expect(outcome.kind).toBe('dock');
    });

    it('a rail attached BELOW a second-column root also refuses a rail to its right (the cap applies to the whole column)', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 0, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, null, 0), // column 2, attached to `a`
        bBelow: createFloatingRailState(AzFloatingDock.FREE, null, 0), // below `b`, same column 2
        c: createFloatingRailState(AzFloatingDock.FREE, { x: 300, y: 400 }, 0),
      };
      states.b!.rightOf = 'a';
      states.bBelow!.belowOf = 'b';
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      withSize(states, 'bBelow', 72, 72);
      withSize(states, 'c', 72, 72);
      const posBBelow = resolvedPosition(states, 'bBelow', cfg);
      states.c!.dragging = true;
      // Flush against bBelow's right edge — bBelow's column top is `b`, and `b` is already the
      // second column (rightOf `a`), so growing a THIRD column off of it must be refused.
      states.c!.liveDragOffset = { x: posBBelow.x + 72 + 2, y: posBBelow.y };
      const outcome = resolveDrop(states, 'c', cfg, () => 72);
      expect(outcome.kind).toBe('dock');
    });
  });

  describe('per-column capacity refusal', () => {
    it('refuses a below-attach that would make the column exceed the vertical safe zone fully expanded', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, { x: 400, y: 500 }, 0),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      states.b!.dragging = true;
      states.b!.liveDragOffset = { x: 40, y: 172 + 2 };
      // A huge worst-case height (e.g. a fully-expanded host with many sub-items) blows the
      // vertical capacity, so the attach must be refused even though the geometry lines up.
      const huge = () => 10000;
      const outcome = resolveDrop(states, 'b', cfg, huge);
      expect(outcome.kind).toBe('dock');
    });

    it('accepts the same drop once the members fit within the vertical safe zone', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 100 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, { x: 400, y: 500 }, 0),
      };
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 72, 72);
      states.b!.dragging = true;
      states.b!.liveDragOffset = { x: 40, y: 172 + 2 };
      const outcome = resolveDrop(states, 'b', cfg, () => 72);
      expect(outcome).toEqual({
        kind: 'attach',
        targetId: 'a',
        attachRight: false,
      });
    });
  });

  describe('grab-bar group drag', () => {
    it('moving the column root while dragging carries every attached dependant along with it', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 40, y: 100 }, 0),
        right: createFloatingRailState(AzFloatingDock.FREE, null, 0),
        below: createFloatingRailState(AzFloatingDock.FREE, null, 0),
      };
      states.right!.rightOf = 'a';
      states.below!.belowOf = 'a';
      withSize(states, 'a', 72, 72);
      withSize(states, 'right', 60, 60);
      withSize(states, 'below', 60, 60);

      const beforeRight = resolvedPosition(states, 'right', cfg);
      const beforeBelow = resolvedPosition(states, 'below', cfg);

      // Grab-bar drag on the column root: mark it dragging with a new live offset (as
      // `beginDrag`/`dragBy` would), leaving the dependants' own attachment pointers untouched.
      states.a!.dragging = true;
      states.a!.liveDragOffset = { x: 40 + 50, y: 100 + 30 };

      const afterA = resolvedPosition(states, 'a', cfg);
      const afterRight = resolvedPosition(states, 'right', cfg);
      const afterBelow = resolvedPosition(states, 'below', cfg);

      expect(afterA).toEqual({ x: 90, y: 130 });
      // Both dependants moved by exactly the same delta as the root — they followed as a unit.
      expect(afterRight.x - beforeRight.x).toBeCloseTo(50);
      expect(afterRight.y - beforeRight.y).toBeCloseTo(30);
      expect(afterBelow.x - beforeBelow.x).toBeCloseTo(50);
      expect(afterBelow.y - beforeBelow.y).toBeCloseTo(30);
    });

    it('topRowWidthPx spans a column root plus its rightward attachment chain', () => {
      const states: FloatingStates = {
        a: createFloatingRailState(AzFloatingDock.FREE, { x: 0, y: 0 }, 0),
        b: createFloatingRailState(AzFloatingDock.FREE, null, 0),
      };
      states.b!.rightOf = 'a';
      withSize(states, 'a', 72, 72);
      withSize(states, 'b', 60, 60);
      expect(topRowWidthPx(states, 'a', 8)).toBe(72 + 8 + 60);
    });
  });
});
