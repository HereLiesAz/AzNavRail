import React, { useEffect, useRef } from 'react';
import { useAzGuidanceContext } from './AzGuidanceController';
import type { AzGuideHighlight } from './AzStatus';

/**
 * Registers a status-driven guidance **status** — a named boolean predicate that becomes a node in the
 * app's flowchart. The predicate may read React state (observed instantly) or a plain source like a
 * mutable ref / external store (observed within the ~300 ms poll). Built-in `az.*` statuses and active
 * classifiers are also statuses — register only the ones your app domain needs.
 *
 * @example
 * ```tsx
 * <AzStatus id="cart_open" predicate={() => cart.isOpen} />
 * ```
 */
export interface AzStatusProps {
  id: string;
  predicate: () => boolean;
}
export const AzStatus: React.FC<AzStatusProps> = ({ id, predicate }) => {
  const reg = useAzGuidanceContext();
  const predRef = useRef(predicate);
  predRef.current = predicate;
  useEffect(() => {
    reg.registerStatus(id, () => predRef.current());
    return () => reg.unregisterStatus(id);
  }, [reg, id]);
  return null;
};

/**
 * Registers a guidance **edge**: while status `from` is true, the shown instruction tells the user how
 * to make status `to` true (an interactive hop). A passive edge (omit `to`) just shows the instruction
 * while `from` holds. The rail auto-generates edges for its own affordances, so author edges here for
 * transitions into your own custom statuses. `highlightItemId` chooses what the callout sits next to.
 *
 * @example
 * ```tsx
 * <AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
 * ```
 */
export interface AzEdgeProps {
  from: string;
  to?: string | null;
  text: string;
  title?: string;
  highlightItemId?: string;
}
let edgeCounter = 0;
export const AzEdge: React.FC<AzEdgeProps> = ({ from, to = null, text, title, highlightItemId }) => {
  const reg = useAzGuidanceContext();
  const keyRef = useRef<string>();
  if (!keyRef.current) keyRef.current = `az_edge_${edgeCounter++}`;
  useEffect(() => {
    const highlight: AzGuideHighlight = highlightItemId ? { type: 'Item', id: highlightItemId } : { type: 'None' };
    reg.registerEdge(keyRef.current!, { from, to: to ?? null, instruction: { text, title, highlight } });
    return () => reg.unregisterEdge(keyRef.current!);
  }, [reg, from, to, text, title, highlightItemId]);
  return null;
};

/**
 * Declares a guidance **goal** — a `target` status the framework routes toward when you `activate` it on
 * the controller. Several goals may guide at once; each active goal's next-hop instruction shows
 * simultaneously, placed next to its own target. `autoStartWhen` self-activates the goal once that
 * status becomes true (onboarding-style).
 *
 * @example
 * ```tsx
 * <AzGoal id="checkout" target="az.screen.confirmation" label="Check out" />
 * ```
 */
export interface AzGoalProps {
  id: string;
  target: string;
  label?: string;
  autoStartWhen?: string | null;
}
export const AzGoal: React.FC<AzGoalProps> = ({ id, target, label, autoStartWhen }) => {
  const reg = useAzGuidanceContext();
  useEffect(() => {
    reg.registerGoal({ id, target, label, autoStartWhen: autoStartWhen ?? null });
    return () => reg.unregisterGoal(id);
  }, [reg, id, target, label, autoStartWhen]);
  return null;
};
