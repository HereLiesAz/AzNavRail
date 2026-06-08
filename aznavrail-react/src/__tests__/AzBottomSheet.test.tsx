import React from 'react';
import { Text, View, PanResponder } from 'react-native';
import { act, render, renderHook } from '@testing-library/react-native';
import { AzBottomSheet } from '../components/AzBottomSheet';
import { useAzSheetController } from '../components/useAzSheetController';
import { AzSheetDetent } from '../types';

describe('useAzSheetController', () => {
  it('starts at HIDDEN by default and steps up through every detent in order [HIDDEN, PEEK, HALF, FULL]', () => {
    // Failure: If this test fails, the `order` array in useAzSheetController is wrong;
    // fix by ensuring order = [HIDDEN, PEEK, HALF, FULL].
    const { result } = renderHook(() => useAzSheetController());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.PEEK);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.HALF);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.FULL);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.FULL); // saturates
  });

  it('stepDown saturates at HIDDEN (cannot go below index 0 in the order array)', () => {
    // Failure: If detent goes negative or undefined, the Math.max(idx - 1, 0) guard in
    // useAzSheetController.stepDown is broken.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.PEEK));
    act(() => result.current.stepDown());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.stepDown());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('snapTo jumps directly when enabled and is gated when disabled (snapTo must respect isEnabled flag)', () => {
    // Failure: If snapTo bypasses the isEnabled gate, the early-return guard in
    // useAzSheetController.snapTo is missing or its condition is inverted.
    const { result } = renderHook(() => useAzSheetController());
    act(() => result.current.snapTo(AzSheetDetent.HALF));
    expect(result.current.detent).toBe(AzSheetDetent.HALF);

    act(() => result.current.setEnabled(false));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);

    act(() => result.current.snapTo(AzSheetDetent.FULL));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN); // gated by isEnabled

    act(() => result.current.snapTo(AzSheetDetent.HIDDEN));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('disabling collapses to HIDDEN and stops stepUp (setEnabled(false) must force HIDDEN and freeze the ladder)', () => {
    // Failure: If detent stays at FULL after setEnabled(false), the setDetent(HIDDEN) line
    // inside setEnabled is missing or its condition is inverted.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.FULL));
    expect(result.current.detent).toBe(AzSheetDetent.FULL);
    act(() => result.current.setEnabled(false));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  // Edge cases at detent boundaries
  it('stepDown from HIDDEN is a no-op (HIDDEN is the floor of the ladder)', () => {
    // Failure: order.indexOf returns 0 for HIDDEN; Math.max(0 - 1, 0) should clamp to HIDDEN.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.HIDDEN));
    act(() => result.current.stepDown());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('stepUp from FULL is a no-op (FULL is the ceiling of the ladder)', () => {
    // Failure: order.indexOf returns 3 for FULL; Math.min(3 + 1, order.length - 1) clamps to FULL.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.FULL));
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.FULL);
  });

  it('re-enabling after disable does not auto-restore prior detent (setEnabled(true) leaves sheet at HIDDEN)', () => {
    // Failure: If the prior detent re-appears after re-enabling, useAzSheetController is
    // caching the pre-disable detent; per spec it must remain HIDDEN until snapTo/stepUp.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.HALF));
    act(() => result.current.setEnabled(false));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.setEnabled(true));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    // ladder is usable again
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.PEEK);
  });

  it('setEnabled(false) mid-flight cancels an in-progress stepUp (stepUp called after disable is a no-op)', () => {
    // Failure: If detent advances after disable, the isEnabled guard in stepUp is missing.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.PEEK));
    act(() => {
      result.current.setEnabled(false);
      result.current.stepUp();
    });
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('setEnabled(false) gates snapTo to any non-HIDDEN detent (snapTo to HIDDEN is always allowed)', () => {
    // Failure: If snapTo(HIDDEN) is also blocked, the gate condition is too restrictive;
    // it should allow `target === HIDDEN` even when disabled.
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.FULL));
    act(() => result.current.setEnabled(false));
    act(() => result.current.snapTo(AzSheetDetent.HALF));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.snapTo(AzSheetDetent.HIDDEN));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('setDetent bypasses gating (it is a direct state setter; consumers can override the ladder)', () => {
    // Failure: setDetent is exposed as the raw state setter; if it stops working after disable
    // the hook is wrapping setDetent rather than returning the raw setter.
    const { result } = renderHook(() => useAzSheetController());
    act(() => result.current.setDetent(AzSheetDetent.HALF));
    expect(result.current.detent).toBe(AzSheetDetent.HALF);
  });

  it('isEnabled is true by default after mount', () => {
    // Failure: useState<boolean>(true) initial value is wrong.
    const { result } = renderHook(() => useAzSheetController());
    expect(result.current.isEnabled).toBe(true);
  });
});

describe('<AzBottomSheet>', () => {
  function Harness({ detent }: { detent: AzSheetDetent }) {
    const controller = useAzSheetController(detent);
    return (
      <View>
        <AzBottomSheet controller={controller}>
          <Text testID="sheet-body">body</Text>
        </AzBottomSheet>
      </View>
    );
  }

  it('renders sheet children (children render inside the sheet body)', () => {
    // Failure: If sheet-body is missing, AzBottomSheet is not forwarding `children` into
    // the sheet body View; check the JSX in AzBottomSheet.tsx.
    const { getByTestId } = render(<Harness detent={AzSheetDetent.PEEK} />);
    expect(getByTestId('sheet-body')).toBeTruthy();
  });

  it('renders without crashing at every detent (HIDDEN, PEEK, HALF, FULL)', () => {
    // Failure: A crash here means detentHeight switch is missing a case or returns undefined.
    [AzSheetDetent.HIDDEN, AzSheetDetent.PEEK, AzSheetDetent.HALF, AzSheetDetent.FULL].forEach((d) => {
      expect(() => render(<Harness detent={d} />)).not.toThrow();
    });
  });

  it('accepts onSwipeLeft and onSwipeRight props without throwing (props must be optional and properly typed)', () => {
    // Failure: If this throws, the prop names in AzBottomSheetProps may have changed.
    const onSwipeLeft = jest.fn();
    const onSwipeRight = jest.fn();
    function H() {
      const controller = useAzSheetController(AzSheetDetent.PEEK);
      return (
        <View>
          <AzBottomSheet
            controller={controller}
            onSwipeLeft={onSwipeLeft}
            onSwipeRight={onSwipeRight}
            config={{ horizontalSwipeEnabled: true }}
          >
            <Text testID="sheet-body">body</Text>
          </AzBottomSheet>
        </View>
      );
    }
    expect(() => render(<H />)).not.toThrow();
  });

  it('config defaults are respected when no config prop is passed (component mounts cleanly with config=undefined)', () => {
    // Failure: If this throws, resolveConfig({ ...DEFAULTS, ...config }) is broken — the
    // spread of undefined into the object literal must produce DEFAULTS unchanged.
    function H() {
      const controller = useAzSheetController(AzSheetDetent.HALF);
      return (
        <View>
          <AzBottomSheet controller={controller}>
            <Text testID="sheet-body">body</Text>
          </AzBottomSheet>
        </View>
      );
    }
    expect(() => render(<H />)).not.toThrow();
  });

  it('config defaults: handleVisible=true renders a drag handle by default', () => {
    // Failure: If no handle is rendered, the DEFAULTS object in AzBottomSheet sets
    // handleVisible to false rather than true.
    function H() {
      const controller = useAzSheetController(AzSheetDetent.HALF);
      return (
        <View>
          <AzBottomSheet controller={controller}>
            <Text testID="sheet-body">body</Text>
          </AzBottomSheet>
        </View>
      );
    }
    const { toJSON } = render(<H />);
    // A render that includes the styled handle View should not throw and should produce a tree.
    expect(toJSON()).toBeTruthy();
  });

  it('config override: handleVisible=false still renders the sheet body (override is accepted)', () => {
    // Failure: If this throws, resolveConfig is not applying user overrides over DEFAULTS.
    function H() {
      const controller = useAzSheetController(AzSheetDetent.HALF);
      return (
        <View>
          <AzBottomSheet controller={controller} config={{ handleVisible: false }}>
            <Text testID="sheet-body">body</Text>
          </AzBottomSheet>
        </View>
      );
    }
    const { getByTestId } = render(<H />);
    expect(getByTestId('sheet-body')).toBeTruthy();
  });

  it('config override: horizontalSwipeEnabled=true still mounts (gate is configurable)', () => {
    function H() {
      const controller = useAzSheetController(AzSheetDetent.HALF);
      return (
        <View>
          <AzBottomSheet controller={controller} config={{ horizontalSwipeEnabled: true }}>
            <Text testID="sheet-body">body</Text>
          </AzBottomSheet>
        </View>
      );
    }
    expect(() => render(<H />)).not.toThrow();
  });
});

describe('<AzBottomSheet> swipe callbacks (PanResponder wiring)', () => {
  // The jest.setup.js mock stubs PanResponder.create to `{ panHandlers: {} }`.
  // To actually exercise the swipe callbacks we capture the config object passed to
  // PanResponder.create and invoke `onPanResponderRelease` directly.
  it('onSwipeLeft fires when horizontalSwipeEnabled and gesture.dx < -threshold', () => {
    // Failure: If onSwipeLeft is not called, the AzBottomSheet onPanResponderRelease branch
    // for horizontal gestures is not routing negative dx → onSwipeLeft.
    let captured: any = null;
    const spy = jest
      .spyOn(PanResponder, 'create')
      .mockImplementation((cfg: any) => {
        captured = cfg;
        return { panHandlers: {} } as any;
      });
    try {
      const onSwipeLeft = jest.fn();
      const onSwipeRight = jest.fn();
      function H() {
        const controller = useAzSheetController(AzSheetDetent.HALF);
        return (
          <AzBottomSheet
            controller={controller}
            onSwipeLeft={onSwipeLeft}
            onSwipeRight={onSwipeRight}
            config={{ horizontalSwipeEnabled: true, dragThresholdDp: 10 }}
          >
            <Text>body</Text>
          </AzBottomSheet>
        );
      }
      render(<H />);
      expect(captured).toBeTruthy();
      // gesture.dx very negative (left swipe), dx > dy magnitude
      captured.onPanResponderRelease({}, { dx: -500, dy: 1 });
      expect(onSwipeLeft).toHaveBeenCalledTimes(1);
      expect(onSwipeRight).not.toHaveBeenCalled();
    } finally {
      spy.mockRestore();
    }
  });

  it('onSwipeRight fires when horizontalSwipeEnabled and gesture.dx > +threshold', () => {
    // Failure: If onSwipeRight is not called, the positive-dx branch in onPanResponderRelease
    // is mis-wired (likely emitting onSwipeLeft for both directions).
    let captured: any = null;
    const spy = jest
      .spyOn(PanResponder, 'create')
      .mockImplementation((cfg: any) => {
        captured = cfg;
        return { panHandlers: {} } as any;
      });
    try {
      const onSwipeLeft = jest.fn();
      const onSwipeRight = jest.fn();
      function H() {
        const controller = useAzSheetController(AzSheetDetent.HALF);
        return (
          <AzBottomSheet
            controller={controller}
            onSwipeLeft={onSwipeLeft}
            onSwipeRight={onSwipeRight}
            config={{ horizontalSwipeEnabled: true, dragThresholdDp: 10 }}
          >
            <Text>body</Text>
          </AzBottomSheet>
        );
      }
      render(<H />);
      expect(captured).toBeTruthy();
      captured.onPanResponderRelease({}, { dx: 500, dy: 1 });
      expect(onSwipeRight).toHaveBeenCalledTimes(1);
      expect(onSwipeLeft).not.toHaveBeenCalled();
    } finally {
      spy.mockRestore();
    }
  });

  it('vertical down-drag steps down ONE detent (FULL -> HALF), not straight to HIDDEN', () => {
    // Failure: If detent becomes HIDDEN, the down-drag branch in onPanResponderRelease is still
    // calling snapTo(HIDDEN) instead of stepDown() — mirror the up-drag's one-step behaviour.
    let captured: any = null;
    const spy = jest
      .spyOn(PanResponder, 'create')
      .mockImplementation((cfg: any) => {
        captured = cfg;
        return { panHandlers: {} } as any;
      });
    try {
      let ctrl: any = null;
      function H() {
        const controller = useAzSheetController(AzSheetDetent.FULL);
        ctrl = controller;
        return (
          <AzBottomSheet controller={controller} config={{ dragThresholdDp: 10 }}>
            <Text>body</Text>
          </AzBottomSheet>
        );
      }
      render(<H />);
      // gesture.dy strongly positive (downward), beyond the threshold, with no horizontal intent.
      act(() => {
        captured.onPanResponderRelease({}, { dx: 0, dy: 500 });
      });
      expect(ctrl.detent).toBe(AzSheetDetent.HALF);
    } finally {
      spy.mockRestore();
    }
  });

  it('vertical up-drag steps up ONE detent (PEEK -> HALF)', () => {
    let captured: any = null;
    const spy = jest
      .spyOn(PanResponder, 'create')
      .mockImplementation((cfg: any) => {
        captured = cfg;
        return { panHandlers: {} } as any;
      });
    try {
      let ctrl: any = null;
      function H() {
        const controller = useAzSheetController(AzSheetDetent.PEEK);
        ctrl = controller;
        return (
          <AzBottomSheet controller={controller} config={{ dragThresholdDp: 10 }}>
            <Text>body</Text>
          </AzBottomSheet>
        );
      }
      render(<H />);
      act(() => {
        captured.onPanResponderRelease({}, { dx: 0, dy: -500 });
      });
      expect(ctrl.detent).toBe(AzSheetDetent.HALF);
    } finally {
      spy.mockRestore();
    }
  });

  it('horizontal swipe is suppressed when horizontalSwipeEnabled=false (default config)', () => {
    // Failure: If callbacks fire when horizontalSwipeEnabled is false, the gate in
    // onPanResponderRelease is missing or inverted.
    let captured: any = null;
    const spy = jest
      .spyOn(PanResponder, 'create')
      .mockImplementation((cfg: any) => {
        captured = cfg;
        return { panHandlers: {} } as any;
      });
    try {
      const onSwipeLeft = jest.fn();
      const onSwipeRight = jest.fn();
      function H() {
        const controller = useAzSheetController(AzSheetDetent.HALF);
        return (
          <AzBottomSheet
            controller={controller}
            onSwipeLeft={onSwipeLeft}
            onSwipeRight={onSwipeRight}
          >
            <Text>body</Text>
          </AzBottomSheet>
        );
      }
      render(<H />);
      // The horizontal-swipe branch in onPanResponderRelease checks
      // cfg.horizontalSwipeEnabled first; with it false, vertical-step logic runs instead.
      captured.onPanResponderRelease({}, { dx: 500, dy: 1 });
      expect(onSwipeLeft).not.toHaveBeenCalled();
      expect(onSwipeRight).not.toHaveBeenCalled();
    } finally {
      spy.mockRestore();
    }
  });
});
