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
  /** Current detent. Reading this in a render subscribes the component to detent changes. */
  detent: AzSheetDetent;
  /** When false, `stepUp`/`stepDown`/`snapTo` (except to HIDDEN) are no-ops. */
  isEnabled: boolean;
  /** Imperatively sets the detent. Prefer `snapTo` to honour the `isEnabled` gate. */
  setDetent: (d: AzSheetDetent) => void;
  /** Enables or disables the controller. Disabling collapses the sheet to HIDDEN. */
  setEnabled: (e: boolean) => void;
  /** Moves one detent further up the ladder (HIDDEN → PEEK → HALF → FULL). */
  stepUp: () => void;
  /** Moves one detent further down the ladder (FULL → HALF → PEEK → HIDDEN). */
  stepDown: () => void;
  /** Jumps to the given detent. No-op when `isEnabled` is false unless the target is HIDDEN. */
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
 *
 * @param initial - Detent the sheet starts in. Defaults to `HIDDEN`.
 * @returns A stable controller object suitable for passing to `<AzBottomSheet>`.
 *
 * @example
 * ```tsx
 * function MyScreen() {
 *   const sheet = useAzSheetController(AzSheetDetent.PEEK);
 *   return (
 *     <>
 *       <Button title="Open" onPress={() => sheet.snapTo(AzSheetDetent.HALF)} />
 *       <AzBottomSheet controller={sheet}>...</AzBottomSheet>
 *     </>
 *   );
 * }
 * ```
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
