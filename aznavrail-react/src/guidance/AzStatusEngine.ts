import { useCallback, useEffect, useRef, useState } from 'react';
import type { AzStatusPredicate } from './AzStatus';

/** Poll interval for status predicates backed by non-React sources (a ref, an external store). */
const STATUS_POLL_MS = 300;

/** Live inputs from which the built-in `az.*` status set is derived. */
export interface BuiltinStatusInput {
  railExpanded: boolean;
  railFloating?: boolean;
  hostStates: Record<string, boolean>;
  currentRoute?: string | null;
  activeItemId?: string | null;
  nestedRailOpenId?: string | null;
  helpOpen?: boolean;
  onscreenVisibleIds?: string[];
}

/**
 * Maps the live rail/host/route/help state to the set of true built-in `az.*` statuses — the status
 * vocabulary an app inherits for free. Pure; mirrors the Kotlin `computeBuiltinStatuses`.
 */
export function computeBuiltinStatuses(input: BuiltinStatusInput): Set<string> {
  const out = new Set<string>();
  out.add('az.app.ready'); // always-true root so navigation auto-edges have a reachable `from`.
  out.add(input.railExpanded ? 'az.rail.expanded' : 'az.rail.collapsed');
  if (input.railFloating) out.add('az.rail.floating');
  Object.entries(input.hostStates || {}).forEach(([id, expanded]) => {
    if (expanded) out.add(`az.host.${id}.expanded`);
  });
  if (input.currentRoute) out.add(`az.screen.${input.currentRoute}`);
  if (input.activeItemId) out.add(`az.item.${input.activeItemId}.active`);
  if (input.nestedRailOpenId) out.add(`az.nestedRail.${input.nestedRailOpenId}.open`);
  if (input.helpOpen) out.add('az.help.open');
  (input.onscreenVisibleIds || []).forEach((id) => out.add(`az.onscreen.${id}.visible`));
  return out;
}

function setsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const x of a) if (!b.has(x)) return false;
  return true;
}

/**
 * Observes the app's full status set reactively and returns the ids currently **true**, unioned from
 * developer predicates, active classifiers, and the built-in `az.*` ids.
 *
 * Uses the same render-eval + low-rate poll pattern as the rail's `expandWhen` observer: re-evaluated
 * after every render (so React-state-driven predicates flip at once) AND on a 300 ms interval (so
 * predicates backed by a mutable ref or an external store still resolve, within the poll).
 */
export function useActiveStatuses(
  predicates: Record<string, AzStatusPredicate>,
  activeClassifiers: Set<string> | string[] | undefined,
  builtins: () => Set<string>,
): Set<string> {
  const predicatesRef = useRef(predicates);
  predicatesRef.current = predicates;
  const builtinsRef = useRef(builtins);
  builtinsRef.current = builtins;
  const classifiersRef = useRef(activeClassifiers);
  classifiersRef.current = activeClassifiers;

  const [active, setActive] = useState<Set<string>>(() => new Set());

  const recompute = useCallback(() => {
    const out = new Set<string>();
    builtinsRef.current().forEach((s) => out.add(s));
    const cls = classifiersRef.current;
    if (cls) (Array.isArray(cls) ? cls : Array.from(cls)).forEach((c) => out.add(c));
    const preds = predicatesRef.current;
    for (const id in preds) {
      try {
        if (preds[id]()) out.add(id);
      } catch {
        /* a throwing predicate is treated as false */
      }
    }
    setActive((prev) => (setsEqual(prev, out) ? prev : out));
  }, []);

  // After every render: React-state/prop-driven predicates and builtins are caught immediately.
  useEffect(() => {
    recompute();
  });
  // Low-rate poll: the safety net for predicates that aren't React state.
  useEffect(() => {
    const t = setInterval(recompute, STATUS_POLL_MS);
    return () => clearInterval(t);
  }, [recompute]);

  return active;
}
