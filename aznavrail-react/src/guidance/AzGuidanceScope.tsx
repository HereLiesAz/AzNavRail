import React, { useEffect, useRef } from 'react';
import { useAzGuidanceContext } from './AzGuidanceController';
import type {
  AzGuidanceRenderer,
  AzGuideShapeProvider,
  AzInstruction,
  AzInstructionStep,
} from './AzStatus';
import { resolveAzHighlight, stepHighlight } from './AzStatus';

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
 * transitions into your own custom statuses.
 *
 * Choose what the callout sits next to (precedence): `highlightTargetId` (a host-registered arbitrary
 * shape, see `<AzGuidanceTarget>`), then `highlightSelector` (a rail item id resolved each frame), then
 * `highlightItemId` (a static rail item, or the `AZ_ITEM_ACTIVE` token). Provide `steps` to make the
 * edge **paged** — several sub-pointers under one milestone, advanced by tap (or reactively per step's
 * `advanceWhen`).
 *
 * @example
 * ```tsx
 * <AzEdge from="cart_open" to="az.screen.checkout" text="Tap Checkout" highlightItemId="checkout" />
 * ```
 */
export interface AzEdgeProps {
  from: string;
  to?: string | null;
  text?: string;
  title?: string;
  highlightItemId?: string;
  highlightTargetId?: string;
  highlightSelector?: () => string | null;
  steps?: AzInstructionStep[];
}
let edgeCounter = 0;
export const AzEdge: React.FC<AzEdgeProps> = (props) => {
  const { from, to = null } = props;
  const reg = useAzGuidanceContext();
  const keyRef = useRef<string>();
  if (!keyRef.current) keyRef.current = `az_edge_${edgeCounter++}`;
  const propsRef = useRef(props);
  propsRef.current = props;
  // Re-register only when value-content changes — NOT on every render (a fresh `steps`/`selector`
  // identity each render would otherwise loop with the registry's version bump).
  const sig = [
    props.text ?? '',
    props.title ?? '',
    props.highlightItemId ?? '',
    props.highlightTargetId ?? '',
    props.highlightSelector ? 'sel' : '',
    // Serialize steps by value (functions are dropped by JSON.stringify), but encode each step's
    // selector *presence* so toggling a step's highlightSelector on/off re-registers. Identity changes
    // of an existing selector are handled live via propsRef in the effect below — not here.
    JSON.stringify((props.steps ?? []).map((s) => ({
      text: s.text, title: s.title, highlightItemId: s.highlightItemId,
      highlightTargetId: s.highlightTargetId, side: s.side, advanceWhen: s.advanceWhen,
      sel: s.highlightSelector ? 'sel' : '',
    }))),
  ].join('|');
  useEffect(() => {
    const p = propsRef.current;
    // `sig` deliberately ignores `highlightSelector` identity (and JSON.stringify drops the function
    // fields of `steps`) so a fresh selector closure each render can't loop register→bump→render. The
    // tradeoff is that the registered edge must resolve selectors LIVE from `propsRef` — otherwise it
    // would freeze the first render's closure and the spotlight would point at a stale item.
    const liveEdgeSelector = p.highlightSelector ? () => propsRef.current.highlightSelector?.() ?? null : undefined;
    const liveSteps = p.steps?.map((s, idx) =>
      s.highlightSelector ? { ...s, highlightSelector: () => propsRef.current.steps?.[idx]?.highlightSelector?.() ?? null } : s,
    );
    let instruction: AzInstruction;
    if (liveSteps && liveSteps.length > 0) {
      const first = liveSteps[0];
      instruction = { text: first.text, title: first.title ?? p.title, highlight: stepHighlight(first), side: first.side };
    } else {
      instruction = { text: p.text ?? '', title: p.title, highlight: resolveAzHighlight(p.highlightItemId, liveEdgeSelector, p.highlightTargetId) };
    }
    reg.registerEdge(keyRef.current!, { from, to: to ?? null, instruction, steps: liveSteps });
    return () => reg.unregisterEdge(keyRef.current!);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reg, from, to, sig]);
  return null;
};

/**
 * Registers a **moving on-screen highlight target** the guidance overlay can spotlight by id (via
 * `<AzEdge highlightTargetId={id} />`). `shape` returns the current `AzGuideShape` in **window-space px**
 * (circle / rect / path) each frame, so the spotlight can track an object drawn over a canvas. Return
 * `null` while the target is absent (the callout degrades to text-only).
 *
 * @example
 * ```tsx
 * <AzGuidanceTarget id="ball" shape={() => ({ type: 'Circle', cx: ball.x, cy: ball.y, radius: 40 })} />
 * ```
 */
export interface AzGuidanceTargetProps {
  id: string;
  shape: AzGuideShapeProvider;
}
export const AzGuidanceTarget: React.FC<AzGuidanceTargetProps> = ({ id, shape }) => {
  const reg = useAzGuidanceContext();
  const shapeRef = useRef(shape);
  shapeRef.current = shape;
  useEffect(() => {
    reg.registerTarget(id, () => shapeRef.current());
    return () => reg.unregisterTarget(id);
  }, [reg, id]);
  return null;
};

/**
 * Suppresses all guidance while `predicate` is true — drive it from your own gesture state so a callout
 * never pops over an in-progress pinch/drag. When it flips back to false, guidance re-shows after a
 * `settleMs` settle delay. Several suppressors compose (OR); the largest `settleMs` wins.
 *
 * @example
 * ```tsx
 * <AzSuppressGuide predicate={() => gesture.inProgress} settleMs={700} />
 * ```
 */
export interface AzSuppressGuideProps {
  predicate: () => boolean;
  settleMs?: number;
}
let suppressCounter = 0;
export const AzSuppressGuide: React.FC<AzSuppressGuideProps> = ({ predicate, settleMs = 700 }) => {
  const reg = useAzGuidanceContext();
  const keyRef = useRef<string>();
  if (!keyRef.current) keyRef.current = `az_suppress_${suppressCounter++}`;
  const predRef = useRef(predicate);
  predRef.current = predicate;
  useEffect(() => {
    reg.registerSuppressor(keyRef.current!, settleMs, () => predRef.current());
    return () => reg.unregisterSuppressor(keyRef.current!);
  }, [reg, settleMs]);
  return null;
};

/**
 * Replaces the built-in guidance callout body with host-drawn content (the dim + spotlight still draw).
 * `render` receives the current `AzGuidanceSnapshot` and the resolved target bounds (window-space px, or
 * `null` when text-only) — for apps drawing bespoke callouts over a canvas.
 */
export interface AzGuideRendererProps {
  render: AzGuidanceRenderer;
}
export const AzGuideRenderer: React.FC<AzGuideRendererProps> = ({ render }) => {
  const reg = useAzGuidanceContext();
  const ref = useRef(render);
  ref.current = render;
  useEffect(() => {
    reg.setRenderer((snap, bounds) => ref.current(snap, bounds));
    return () => reg.setRenderer(null);
  }, [reg]);
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
