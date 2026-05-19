import React from 'react';
import { Text, View } from 'react-native';
import { act, render, renderHook } from '@testing-library/react-native';
import { AzBottomSheet } from '../components/AzBottomSheet';
import { useAzSheetController } from '../components/useAzSheetController';
import { AzSheetDetent } from '../types';

describe('useAzSheetController', () => {
  it('starts at HIDDEN by default and steps up through every detent', () => {
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

  it('stepDown saturates at HIDDEN', () => {
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.PEEK));
    act(() => result.current.stepDown());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.stepDown());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
  });

  it('snapTo jumps directly when enabled and is gated when disabled', () => {
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

  it('disabling collapses to HIDDEN and stops stepUp', () => {
    const { result } = renderHook(() => useAzSheetController(AzSheetDetent.FULL));
    expect(result.current.detent).toBe(AzSheetDetent.FULL);
    act(() => result.current.setEnabled(false));
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
    act(() => result.current.stepUp());
    expect(result.current.detent).toBe(AzSheetDetent.HIDDEN);
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

  it('renders sheet children', () => {
    const { getByTestId } = render(<Harness detent={AzSheetDetent.PEEK} />);
    expect(getByTestId('sheet-body')).toBeTruthy();
  });

  it('renders without crashing at every detent', () => {
    [AzSheetDetent.HIDDEN, AzSheetDetent.PEEK, AzSheetDetent.HALF, AzSheetDetent.FULL].forEach((d) => {
      expect(() => render(<Harness detent={d} />)).not.toThrow();
    });
  });
});
