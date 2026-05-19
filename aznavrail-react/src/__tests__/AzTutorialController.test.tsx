import React from 'react';
import { render, act } from '@testing-library/react-native';
import { AzTutorialProvider, useAzTutorialController } from '../tutorial/AzTutorialController';

let capturedController: ReturnType<typeof useAzTutorialController> | null = null;

const Capture = () => {
  capturedController = useAzTutorialController();
  return null;
};

const wrap = (initialReadTutorials?: string[]) =>
  render(
    <AzTutorialProvider initialReadTutorials={initialReadTutorials}>
      <Capture />
    </AzTutorialProvider>
  );

beforeEach(() => { capturedController = null; });

describe('AzTutorialController', () => {
  it('startTutorial stores id and variables (id → activeTutorialId, variables → currentVariables)', () => {
    // Failure: If activeTutorialId is null after startTutorial, the call to setActiveTutorialId
    // is missing in startTutorial in AzTutorialController.tsx.
    wrap();
    act(() => { capturedController!.startTutorial('t1', { level: 'advanced' }); });
    expect(capturedController!.activeTutorialId).toBe('t1');
    expect(capturedController!.currentVariables.level).toBe('advanced');
  });

  it('startTutorial without variables defaults to empty object (variables ?? {})', () => {
    // Failure: If currentVariables is undefined, the second-arg default `= {}` is missing.
    wrap();
    act(() => { capturedController!.startTutorial('t1'); });
    expect(capturedController!.currentVariables).toEqual({});
  });

  it('startTutorial(id, variables) overwrites previous variables (does not merge)', () => {
    // Failure: If a merge happens, currentVariables should be a fresh setState call
    // not a spread merge. startTutorial is documented as replacing variables.
    wrap();
    act(() => { capturedController!.startTutorial('t1', { a: 1, b: 2 }); });
    expect(capturedController!.currentVariables).toEqual({ a: 1, b: 2 });
    act(() => { capturedController!.startTutorial('t2', { c: 3 }); });
    expect(capturedController!.currentVariables).toEqual({ c: 3 });
    expect(capturedController!.activeTutorialId).toBe('t2');
  });

  it('startTutorial with nested object variables preserves their shape (no shallow flattening)', () => {
    // Failure: If currentVariables.user is undefined, setCurrentVariables is being
    // called with Object.assign in a wrong order.
    wrap();
    act(() => {
      capturedController!.startTutorial('t', { user: { name: 'Az', role: 'admin' }, count: 3 });
    });
    expect(capturedController!.currentVariables.user).toEqual({ name: 'Az', role: 'admin' });
    expect(capturedController!.currentVariables.count).toBe(3);
  });

  it('fireEvent sets pendingEvent (fireEvent must update state via setPendingEvent)', () => {
    // Failure: If pendingEvent is null after fireEvent, setPendingEvent(name) is missing.
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    expect(capturedController!.pendingEvent).toBe('menu_opened');
  });

  it('consumeEvent clears pendingEvent (sets back to null)', () => {
    // Failure: If pendingEvent still holds the event name, consumeEvent must call
    // setPendingEvent(null) — check that line.
    wrap();
    act(() => { capturedController!.fireEvent('menu_opened'); });
    act(() => { capturedController!.consumeEvent(); });
    expect(capturedController!.pendingEvent).toBeNull();
  });

  it('fireEvent → fireEvent replaces the pending event (latest call wins)', () => {
    // Failure: If pendingEvent stays at "a", fireEvent is queuing events instead of
    // overwriting; spec is setPendingEvent(name) with no queue.
    wrap();
    act(() => { capturedController!.fireEvent('a'); });
    act(() => { capturedController!.fireEvent('b'); });
    expect(capturedController!.pendingEvent).toBe('b');
  });

  it('consumeEvent on an empty pendingEvent is a no-op (no error, state stays null)', () => {
    // Failure: If consumeEvent throws, it is mis-using a callback ref or referencing
    // an undefined value.
    wrap();
    expect(capturedController!.pendingEvent).toBeNull();
    act(() => { capturedController!.consumeEvent(); });
    expect(capturedController!.pendingEvent).toBeNull();
  });

  it('endTutorial clears all transient state (activeTutorialId, currentVariables, pendingEvent)', () => {
    // Failure: If any of these three remain set, endTutorial is missing one of
    // setActiveTutorialId(null), setCurrentVariables({}), setPendingEvent(null).
    wrap();
    act(() => { capturedController!.startTutorial('t1', { x: 1 }); capturedController!.fireEvent('ev'); });
    act(() => { capturedController!.endTutorial(); });
    expect(capturedController!.activeTutorialId).toBeNull();
    expect(capturedController!.currentVariables).toEqual({});
    expect(capturedController!.pendingEvent).toBeNull();
  });

  it('endTutorial does NOT clear readTutorials (read-state is persistent)', () => {
    // Failure: If readTutorials is wiped, endTutorial is mis-clearing the persisted
    // read list; readTutorials must outlive a single tutorial run.
    wrap(['t-already-read']);
    expect(capturedController!.isTutorialRead('t-already-read')).toBe(true);
    act(() => { capturedController!.startTutorial('t1'); });
    act(() => { capturedController!.endTutorial(); });
    expect(capturedController!.isTutorialRead('t-already-read')).toBe(true);
  });

  describe('read-state persistence', () => {
    it('markTutorialRead adds to readTutorials and isTutorialRead reflects it', () => {
      // Failure: If isTutorialRead returns false right after markTutorialRead, the
      // setReadTutorials state update is async-stalled or check uses stale closure.
      wrap();
      expect(capturedController!.isTutorialRead('t1')).toBe(false);
      act(() => { capturedController!.markTutorialRead('t1'); });
      expect(capturedController!.isTutorialRead('t1')).toBe(true);
      expect(capturedController!.readTutorials).toContain('t1');
    });

    it('markTutorialRead is idempotent (calling twice does not duplicate the id)', () => {
      // Failure: If readTutorials contains "t1" twice, the `if (prev.includes(id)) return prev;`
      // guard in markTutorialRead is missing.
      wrap();
      act(() => { capturedController!.markTutorialRead('t1'); });
      act(() => { capturedController!.markTutorialRead('t1'); });
      const occurrences = capturedController!.readTutorials.filter((id) => id === 't1').length;
      expect(occurrences).toBe(1);
    });

    it('isTutorialRead returns false for unknown ids', () => {
      // Failure: A truthy return here means readTutorials.includes is broken or the
      // array contains a stray sentinel value.
      wrap();
      expect(capturedController!.isTutorialRead('never-marked')).toBe(false);
    });

    it('initialReadTutorials seeds readTutorials at mount (constructor input is respected)', () => {
      // Failure: If isTutorialRead returns false, the initialReadTutorials prop is being
      // ignored by useState's initial value.
      wrap(['seeded1', 'seeded2']);
      expect(capturedController!.isTutorialRead('seeded1')).toBe(true);
      expect(capturedController!.isTutorialRead('seeded2')).toBe(true);
      expect(capturedController!.isTutorialRead('not-seeded')).toBe(false);
    });

    it('markTutorialRead preserves previously seeded ids (append, do not replace)', () => {
      // Failure: If "seeded1" is missing after markTutorialRead, the spread `[...prev, id]`
      // is replaced by `[id]`.
      wrap(['seeded1']);
      act(() => { capturedController!.markTutorialRead('new'); });
      expect(capturedController!.readTutorials).toEqual(expect.arrayContaining(['seeded1', 'new']));
    });
  });

  describe('startTutorial + variables for branching', () => {
    it('startTutorial(id, vars) updates both id and vars atomically (single React batch)', () => {
      // Failure: If id and vars are inconsistent (e.g. id set but vars empty), the
      // two state setters are being separated across renders incorrectly.
      wrap();
      act(() => {
        capturedController!.startTutorial('branchy', { tier: 'pro', stage: 2 });
      });
      expect(capturedController!.activeTutorialId).toBe('branchy');
      expect(capturedController!.currentVariables).toEqual({ tier: 'pro', stage: 2 });
    });

    it('startTutorial after endTutorial works (controller is reusable)', () => {
      // Failure: If activeTutorialId stays null after the second startTutorial, the
      // endTutorial implementation is permanently disabling future starts.
      wrap();
      act(() => { capturedController!.startTutorial('a'); });
      act(() => { capturedController!.endTutorial(); });
      act(() => { capturedController!.startTutorial('b', { k: 'v' }); });
      expect(capturedController!.activeTutorialId).toBe('b');
      expect(capturedController!.currentVariables).toEqual({ k: 'v' });
    });
  });
});
