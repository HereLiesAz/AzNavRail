import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import type {
  AzEdge,
  AzGoal,
  AzGuidanceRenderer,
  AzGuidanceSnapshot,
  AzGuideShapeProvider,
  AzStatusPredicate,
} from './AzStatus';

const STORAGE_KEY = 'az_navrail_completed_goals';
const DISMISSED_KEY = 'az_navrail_dismissed_goals';

// Optional AsyncStorage (React Native) — used if installed, no-op otherwise.
let AsyncStorage: { setItem: (k: string, v: string) => Promise<void> } | null = null;
try {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  AsyncStorage = require('@react-native-async-storage/async-storage').default;
} catch {
  /* not installed — fall back to localStorage / no-op */
}

function loadIds(key: string): string[] {
  try {
    if (typeof localStorage !== 'undefined') {
      const raw = localStorage.getItem(key);
      if (raw) return JSON.parse(raw) as string[];
    }
  } catch {
    /* ignore */
  }
  return [];
}

function saveIds(key: string, ids: string[]): void {
  const json = JSON.stringify(ids);
  try {
    if (typeof localStorage !== 'undefined') localStorage.setItem(key, json);
  } catch {
    /* ignore */
  }
  AsyncStorage?.setItem(key, json).catch(() => {});
}

/**
 * The developer-facing guidance controller (the React analog of Android's `AzGuidanceController`,
 * returned from `AzHostActivityLayout`). Obtain it with {@link useAzGuidanceController} anywhere under
 * an `AzNavRail`. Activating a goal enables guidance and starts routing toward its target status.
 */
export interface AzGuidanceController {
  /** Master switch — guidance only renders while enabled. Activating a goal enables it. */
  enabled: boolean;
  /** Goal ids currently guiding (several may guide at once). */
  activeGoals: string[];
  /** Goal ids reached at least once (persisted), so onboarding-style goals don't auto-restart. */
  completedGoals: string[];
  enable: () => void;
  disable: () => void;
  /** Begin guiding toward `goalId`. */
  activate: (goalId: string) => void;
  /** Stop guiding toward `goalId`. */
  deactivate: (goalId: string) => void;
  /** Mark a goal reached: deactivate it and persist completion. */
  markReached: (goalId: string) => void;
  isCompleted: (goalId: string) => boolean;
  /** Goal ids the user skipped (persisted) — a skipped tutorial is never shown again until reset. */
  dismissedGoals: string[];
  isDismissed: (goalId: string) => boolean;
  /** The user skipped a goal (omit `goalId` to cancel tutorial mode: skip all active goals + disable). */
  skip: (goalId?: string) => void;
  /** Clear completion + dismissal (+ de-dup) for `goalId` (or all) so a tutorial can be shown again. */
  resetGuidance: (goalId?: string) => void;
  // --- Paged-edge step cursor (transient; only goal completion persists) ---
  /** Current step index for the paged edge identified by `key` (see `edgeStepKey`). */
  stepIndex: (key: string) => number;
  /** Set the step cursor for `key` (used by the overlay to sync reactive auto-advance). */
  setStep: (key: string, index: number) => void;
  /** Advance the paged edge `key` (or the first active paged instruction when omitted). */
  advance: (key?: string) => void;
  /** Alias for `advance`. */
  next: (key?: string) => void;
  /** Step the paged edge `key` back one (never below 0). */
  back: (key: string) => void;
  // --- Observable current instruction(s) (Gap C) ---
  /** The guidance callouts being shown right now (one per active goal, plus passive tips). */
  currentInstructions: AzGuidanceSnapshot[];
  /** The primary (first) current instruction, a convenience for single-goal flows. */
  current: AzGuidanceSnapshot | null;
}

/** Internal context value: the controller plus the live status/edge/goal/target registry. */
interface AzGuidanceContextValue extends AzGuidanceController {
  statusPredicates: Record<string, AzStatusPredicate>;
  edges: AzEdge[];
  goals: Record<string, AzGoal>;
  targets: Record<string, AzGuideShapeProvider>;
  suppressors: Array<[number, () => boolean]>;
  renderer: AzGuidanceRenderer | null;
  registerStatus: (id: string, predicate: AzStatusPredicate) => void;
  unregisterStatus: (id: string) => void;
  registerEdge: (key: string, edge: AzEdge) => void;
  unregisterEdge: (key: string) => void;
  registerGoal: (goal: AzGoal) => void;
  unregisterGoal: (id: string) => void;
  registerTarget: (id: string, shape: AzGuideShapeProvider) => void;
  unregisterTarget: (id: string) => void;
  registerSuppressor: (key: string, settleMs: number, predicate: () => boolean) => void;
  unregisterSuppressor: (key: string) => void;
  setRenderer: (renderer: AzGuidanceRenderer | null) => void;
  /** Publish the latest routed snapshot list. Called by the rail; not for app use. */
  publishCurrent: (snapshots: AzGuidanceSnapshot[]) => void;
  /** Statuses consumed this session (a shown step whose action was taken). */
  consumedStatuses: Set<string>;
  /** Mark a status consumed (a shown hop's `to` that became true). Called by the rail. */
  consume: (status: string) => void;
}

const AzGuidanceContext = createContext<AzGuidanceContextValue | null>(null);

/** Props for {@link AzGuidanceProvider}. */
interface AzGuidanceProviderProps {
  children: React.ReactNode;
  /** Seed completed goal ids (useful for testing). */
  initialCompleted?: string[];
}

/**
 * Owns guidance state and the status/edge/goal registry. `AzNavRail` mounts one automatically; the DSL
 * components (`<AzStatus>` / `<AzEdge>` / `<AzGoal>`) register into it and the rail's engine reads it.
 */
export const AzGuidanceProvider: React.FC<AzGuidanceProviderProps> = ({ children, initialCompleted }) => {
  const [enabled, setEnabled] = useState(false);
  const [activeGoals, setActiveGoals] = useState<string[]>([]);
  const [completedGoals, setCompletedGoals] = useState<string[]>(() => initialCompleted ?? loadIds(STORAGE_KEY));
  const [dismissedGoals, setDismissedGoals] = useState<string[]>(() => loadIds(DISMISSED_KEY));
  const [consumed, setConsumed] = useState<string[]>([]);

  // Registry kept in refs (predicate identities churn every render); a version bump notifies consumers.
  const statusRef = useRef<Record<string, AzStatusPredicate>>({});
  const edgeRef = useRef<Record<string, AzEdge>>({});
  const goalRef = useRef<Record<string, AzGoal>>({});
  const targetRef = useRef<Record<string, AzGuideShapeProvider>>({});
  const suppressorRef = useRef<Record<string, [number, () => boolean]>>({});
  const rendererRef = useRef<AzGuidanceRenderer | null>(null);
  const [version, setVersion] = useState(0);
  const bump = useCallback(() => setVersion((v) => v + 1), []);

  const registerStatus = useCallback((id: string, predicate: AzStatusPredicate) => { statusRef.current[id] = predicate; bump(); }, [bump]);
  const unregisterStatus = useCallback((id: string) => { delete statusRef.current[id]; bump(); }, [bump]);
  const registerEdge = useCallback((key: string, edge: AzEdge) => { edgeRef.current[key] = edge; bump(); }, [bump]);
  const unregisterEdge = useCallback((key: string) => { delete edgeRef.current[key]; bump(); }, [bump]);
  const registerGoal = useCallback((goal: AzGoal) => { goalRef.current[goal.id] = goal; bump(); }, [bump]);
  const unregisterGoal = useCallback((id: string) => { delete goalRef.current[id]; bump(); }, [bump]);
  const registerTarget = useCallback((id: string, shape: AzGuideShapeProvider) => { targetRef.current[id] = shape; bump(); }, [bump]);
  const unregisterTarget = useCallback((id: string) => { delete targetRef.current[id]; bump(); }, [bump]);
  const registerSuppressor = useCallback((key: string, settleMs: number, predicate: () => boolean) => { suppressorRef.current[key] = [settleMs, predicate]; bump(); }, [bump]);
  const unregisterSuppressor = useCallback((key: string) => { delete suppressorRef.current[key]; bump(); }, [bump]);
  const setRenderer = useCallback((renderer: AzGuidanceRenderer | null) => { rendererRef.current = renderer; bump(); }, [bump]);

  // Transient paged-edge step cursor (only goal completion persists).
  const [stepCursor, setStepCursor] = useState<Record<string, number>>({});
  const stepIndex = useCallback((key: string) => stepCursor[key] ?? 0, [stepCursor]);
  const setStep = useCallback((key: string, index: number) => {
    setStepCursor((prev) => ({ ...prev, [key]: Math.max(0, index) }));
  }, []);
  const back = useCallback((key: string) => {
    setStepCursor((prev) => ({ ...prev, [key]: Math.max(0, (prev[key] ?? 0) - 1) }));
  }, []);

  // Observable current instruction(s).
  const [currentInstructions, setCurrentInstructions] = useState<AzGuidanceSnapshot[]>([]);
  const publishCurrent = useCallback((snaps: AzGuidanceSnapshot[]) => { setCurrentInstructions(snaps); }, []);
  const advance = useCallback((key?: string) => {
    const resolvedKey = key ?? currentInstructions.find((s) => s.stepTotal > 1)?.stepKey;
    if (resolvedKey == null) return;
    setStepCursor((prev) => ({ ...prev, [resolvedKey]: (prev[resolvedKey] ?? 0) + 1 }));
  }, [currentInstructions]);
  const next = advance;

  const enable = useCallback(() => setEnabled(true), []);
  const disable = useCallback(() => { setEnabled(false); setConsumed([]); }, []);
  const activate = useCallback((goalId: string) => {
    // No-op for a completed or skipped goal — it is never re-shown until resetGuidance.
    if (completedGoals.includes(goalId) || dismissedGoals.includes(goalId)) return;
    setEnabled(true);
    setActiveGoals((prev) => (prev.includes(goalId) ? prev : [...prev, goalId]));
  }, [completedGoals, dismissedGoals]);
  const deactivate = useCallback((goalId: string) => {
    setActiveGoals((prev) => prev.filter((g) => g !== goalId));
  }, []);
  const markReached = useCallback((goalId: string) => {
    setActiveGoals((prev) => prev.filter((g) => g !== goalId));
    setCompletedGoals((prev) => {
      if (prev.includes(goalId)) return prev;
      const next = [...prev, goalId];
      saveIds(STORAGE_KEY, next);
      return next;
    });
  }, []);
  const isCompleted = useCallback((goalId: string) => completedGoals.includes(goalId), [completedGoals]);
  const isDismissed = useCallback((goalId: string) => dismissedGoals.includes(goalId), [dismissedGoals]);
  const skip = useCallback((goalId?: string) => {
    if (goalId != null) {
      setActiveGoals((prev) => prev.filter((g) => g !== goalId));
      setDismissedGoals((prev) => {
        if (prev.includes(goalId)) return prev;
        const next = [...prev, goalId];
        saveIds(DISMISSED_KEY, next);
        return next;
      });
    } else {
      // Cancel tutorial mode: skip every active goal (persisted), then turn guidance off.
      setDismissedGoals((prev) => {
        const next = Array.from(new Set([...prev, ...activeGoals]));
        saveIds(DISMISSED_KEY, next);
        return next;
      });
      setActiveGoals([]);
      setEnabled(false);
      setConsumed([]);
    }
  }, [activeGoals]);
  const resetGuidance = useCallback((goalId?: string) => {
    if (goalId != null) {
      setCompletedGoals((prev) => { const next = prev.filter((g) => g !== goalId); saveIds(STORAGE_KEY, next); return next; });
      setDismissedGoals((prev) => { const next = prev.filter((g) => g !== goalId); saveIds(DISMISSED_KEY, next); return next; });
    } else {
      setCompletedGoals(() => { saveIds(STORAGE_KEY, []); return []; });
      setDismissedGoals(() => { saveIds(DISMISSED_KEY, []); return []; });
    }
    setConsumed([]);
  }, []);
  const consume = useCallback((status: string) => {
    setConsumed((prev) => (prev.includes(status) ? prev : [...prev, status]));
  }, []);
  const consumedStatuses = useMemo(() => new Set(consumed), [consumed]);

  // Live registry views, recomputed when the registry version changes.
  const edges = useMemo(() => Object.values(edgeRef.current), [version]);
  const goals = useMemo(() => ({ ...goalRef.current }), [version]);
  const statusPredicates = useMemo(() => ({ ...statusRef.current }), [version]);
  const targets = useMemo(() => ({ ...targetRef.current }), [version]);
  const suppressors = useMemo<Array<[number, () => boolean]>>(() => Object.values(suppressorRef.current), [version]);
  const renderer = useMemo(() => rendererRef.current, [version]);
  const current = currentInstructions.length > 0 ? currentInstructions[0] : null;

  const value = useMemo<AzGuidanceContextValue>(
    () => ({
      enabled, activeGoals, completedGoals, enable, disable, activate, deactivate, markReached, isCompleted,
      dismissedGoals, isDismissed, skip, resetGuidance, consumedStatuses, consume,
      stepIndex, setStep, advance, next, back, currentInstructions, current,
      statusPredicates, edges, goals, targets, suppressors, renderer,
      registerStatus, unregisterStatus, registerEdge, unregisterEdge, registerGoal, unregisterGoal,
      registerTarget, unregisterTarget, registerSuppressor, unregisterSuppressor, setRenderer, publishCurrent,
    }),
    [enabled, activeGoals, completedGoals, enable, disable, activate, deactivate, markReached, isCompleted,
     dismissedGoals, isDismissed, skip, resetGuidance, consumedStatuses, consume,
     stepIndex, setStep, advance, next, back, currentInstructions, current,
     statusPredicates, edges, goals, targets, suppressors, renderer,
     registerStatus, unregisterStatus, registerEdge, unregisterEdge, registerGoal, unregisterGoal,
     registerTarget, unregisterTarget, registerSuppressor, unregisterSuppressor, setRenderer, publishCurrent],
  );

  return <AzGuidanceContext.Provider value={value}>{children}</AzGuidanceContext.Provider>;
};

/** No-op controller returned when used outside a provider (so screens don't have to guard). */
const NOOP: AzGuidanceContextValue = {
  enabled: false, activeGoals: [], completedGoals: [],
  enable: () => {}, disable: () => {}, activate: () => {}, deactivate: () => {}, markReached: () => {}, isCompleted: () => false,
  dismissedGoals: [], isDismissed: () => false, skip: () => {}, resetGuidance: () => {}, consumedStatuses: new Set(), consume: () => {},
  stepIndex: () => 0, setStep: () => {}, advance: () => {}, next: () => {}, back: () => {},
  currentInstructions: [], current: null,
  statusPredicates: {}, edges: [], goals: {}, targets: {}, suppressors: [], renderer: null,
  registerStatus: () => {}, unregisterStatus: () => {}, registerEdge: () => {}, unregisterEdge: () => {}, registerGoal: () => {}, unregisterGoal: () => {},
  registerTarget: () => {}, unregisterTarget: () => {}, registerSuppressor: () => {}, unregisterSuppressor: () => {}, setRenderer: () => {}, publishCurrent: () => {},
};

/** Internal: full context (controller + registry), used by the rail engine and the DSL components. */
export const useAzGuidanceContext = (): AzGuidanceContextValue => useContext(AzGuidanceContext) ?? NOOP;

/**
 * Returns the developer-facing {@link AzGuidanceController} from the nearest `AzNavRail`. Use it to
 * `activate` / `deactivate` goals (several may guide at once) or flip the master switch.
 */
export const useAzGuidanceController = (): AzGuidanceController => {
  const ctx = useAzGuidanceContext();
  return ctx;
};
