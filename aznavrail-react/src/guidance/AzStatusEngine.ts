import { useEffect, useRef, useState } from 'react';
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

/** True if any registered suppressor predicate is currently true (a throwing predicate counts false). */
export function anySuppressorActive(suppressors: Array<[number, () => boolean]>): boolean {
  return suppressors.some(([, predicate]) => {
    try {
      return predicate();
    } catch {
      return false;
    }
  });
}

/**
 * Returns whether the guidance overlay should currently be hidden, given the host's `suppressors` (each
 * `[settleMs, predicate]`, registered via `<AzSuppressGuide>`). True immediately while any predicate is
 * true; when all go false, a settle delay (the largest registered `settleMs`) elapses before it flips
 * back to false. Predicates are re-evaluated each render and on the same ~300 ms poll as
 * {@link useActiveStatuses}. Mirrors Kotlin `rememberGuidanceSuppressed`.
 */
export function useGuidanceSuppressed(suppressors: Array<[number, () => boolean]>): boolean {
  const rawNow = anySuppressorActive(suppressors);
  const settleMs = suppressors.reduce((m, [s]) => Math.max(m, s), 0);
  // Seed from the live state so the overlay starts hidden when it should (no first-render flash).
  const [suppressed, setSuppressed] = useState(rawNow);
  // Poll so predicates backed by non-React sources resolve within the interval.
  const [, setTick] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setTick((x) => x + 1), STATUS_POLL_MS);
    return () => clearInterval(t);
  }, []);
  useEffect(() => {
    if (rawNow) {
      setSuppressed(true);
      return;
    }
    const id = setTimeout(() => setSuppressed(false), settleMs);
    return () => clearTimeout(id);
  }, [rawNow, settleMs]);
  return suppressed;
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
  // A periodic tick re-evaluates predicates backed by non-React sources (a ref, an external store)
  // within the poll interval. React-state/prop-driven inputs are caught for free because the host
  // re-renders, which re-runs this hook. We compute the set DURING render (cheap, pure reads) and
  // stabilize its identity via a ref — so there is NO render-coupled `setState`, which would otherwise
  // risk a "maximum update depth" loop with the rail's own per-render effects.
  const [, setPollTick] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setPollTick((x) => x + 1), STATUS_POLL_MS);
    return () => clearInterval(t);
  }, []);

  const out = new Set<string>();
  builtins().forEach((s) => out.add(s));
  if (activeClassifiers) {
    (Array.isArray(activeClassifiers) ? activeClassifiers : Array.from(activeClassifiers)).forEach((c) => out.add(c));
  }
  for (const id in predicates) {
    try {
      if (predicates[id]()) out.add(id);
    } catch {
      /* a throwing predicate is treated as false */
    }
  }

  // Return a content-stable reference: identity only changes when the active set actually changes,
  // so downstream memos/effects don't churn on every render.
  const stable = useRef<Set<string>>(out);
  if (!setsEqual(stable.current, out)) stable.current = out;
  return stable.current;
}
