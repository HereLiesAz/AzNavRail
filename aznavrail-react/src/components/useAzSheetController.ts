import { useCallback, useMemo, useState } from 'react';
import { AzSheetDetent } from '../types';

/**
 * Imperative controller surface for an `<AzBottomSheet>`.
 *
 * Mirrors the public API of the Android `AzSheetController`:
 *   - `detent` / `isEnabled` reflect current state and drive recomposition.
 *   - `stepUp` / `stepDown` move one detent in the LogKitty-style ladder.
 *   - `snapTo` jumps directly to a target detent.
 *
 * When `isEnabled` flips to `false`, `stepUp` / `stepDown` become no-ops and the
 * sheet collapses to `HIDDEN`, mirroring LogKitty's launcher-pass-through behaviour.
 */
export interface AzSheetController {
  detent: AzSheetDetent;
  isEnabled: boolean;
  setDetent: (d: AzSheetDetent) => void;
  setEnabled: (e: boolean) => void;
  stepUp: () => void;
  stepDown: () => void;
  snapTo: (target: AzSheetDetent) => void;
}

const order: AzSheetDetent[] = [
  AzSheetDetent.HIDDEN,
  AzSheetDetent.PEEK,
  AzSheetDetent.HALF,
  AzSheetDetent.FULL,
];

/**
 * Remembers an `AzSheetController` across renders. The detent state is regular React
 * state, so reading `controller.detent` in a render is enough to drive re-renders.
 */
export function useAzSheetController(
  initial: AzSheetDetent = AzSheetDetent.HIDDEN,
): AzSheetController {
  const [detent, setDetent] = useState<AzSheetDetent>(initial);
  const [isEnabled, setEnabledState] = useState<boolean>(true);

  const setEnabled = useCallback((enabled: boolean) => {
    setEnabledState(enabled);
    if (!enabled) setDetent(AzSheetDetent.HIDDEN);
  }, []);

  const stepUp = useCallback(() => {
    setDetent((d) => {
      if (!isEnabled) return d;
      const idx = order.indexOf(d);
      return order[Math.min(idx + 1, order.length - 1)];
    });
  }, [isEnabled]);

  const stepDown = useCallback(() => {
    setDetent((d) => {
      if (!isEnabled && d !== AzSheetDetent.HIDDEN) return AzSheetDetent.HIDDEN;
      const idx = order.indexOf(d);
      return order[Math.max(idx - 1, 0)];
    });
  }, [isEnabled]);

  const snapTo = useCallback(
    (target: AzSheetDetent) => {
      if (target !== AzSheetDetent.HIDDEN && !isEnabled) return;
      setDetent(target);
    },
    [isEnabled],
  );

  return useMemo<AzSheetController>(
    () => ({ detent, isEnabled, setDetent, setEnabled, stepUp, stepDown, snapTo }),
    [detent, isEnabled, setEnabled, stepUp, stepDown, snapTo],
  );
}
