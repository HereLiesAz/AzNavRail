import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import type { AzEdge, AzGoal, AzStatusPredicate } from './AzStatus';

const STORAGE_KEY = 'az_navrail_completed_goals';

// Optional AsyncStorage (React Native) — used if installed, no-op otherwise.
let AsyncStorage: { setItem: (k: string, v: string) => Promise<void> } | null = null;
try {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  AsyncStorage = require('@react-native-async-storage/async-storage').default;
} catch {
  /* not installed — fall back to localStorage / no-op */
}

function loadCompleted(): string[] {
  try {
    if (typeof localStorage !== 'undefined') {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw) as string[];
    }
  } catch {
    /* ignore */
  }
  return [];
}

function saveCompleted(ids: string[]): void {
  const json = JSON.stringify(ids);
  try {
    if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, json);
  } catch {
    /* ignore */
  }
  AsyncStorage?.setItem(STORAGE_KEY, json).catch(() => {});
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
}

/** Internal context value: the controller plus the live status/edge/goal registry. */
interface AzGuidanceContextValue extends AzGuidanceController {
  statusPredicates: Record<string, AzStatusPredicate>;
  edges: AzEdge[];
  goals: Record<string, AzGoal>;
  registerStatus: (id: string, predicate: AzStatusPredicate) => void;
  unregisterStatus: (id: string) => void;
  registerEdge: (key: string, edge: AzEdge) => void;
  unregisterEdge: (key: string) => void;
  registerGoal: (goal: AzGoal) => void;
  unregisterGoal: (id: string) => void;
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
  const [completedGoals, setCompletedGoals] = useState<string[]>(() => initialCompleted ?? loadCompleted());

  // Registry kept in refs (predicate identities churn every render); a version bump notifies consumers.
  const statusRef = useRef<Record<string, AzStatusPredicate>>({});
  const edgeRef = useRef<Record<string, AzEdge>>({});
  const goalRef = useRef<Record<string, AzGoal>>({});
  const [version, setVersion] = useState(0);
  const bump = useCallback(() => setVersion((v) => v + 1), []);

  const registerStatus = useCallback((id: string, predicate: AzStatusPredicate) => { statusRef.current[id] = predicate; bump(); }, [bump]);
  const unregisterStatus = useCallback((id: string) => { delete statusRef.current[id]; bump(); }, [bump]);
  const registerEdge = useCallback((key: string, edge: AzEdge) => { edgeRef.current[key] = edge; bump(); }, [bump]);
  const unregisterEdge = useCallback((key: string) => { delete edgeRef.current[key]; bump(); }, [bump]);
  const registerGoal = useCallback((goal: AzGoal) => { goalRef.current[goal.id] = goal; bump(); }, [bump]);
  const unregisterGoal = useCallback((id: string) => { delete goalRef.current[id]; bump(); }, [bump]);

  const enable = useCallback(() => setEnabled(true), []);
  const disable = useCallback(() => setEnabled(false), []);
  const activate = useCallback((goalId: string) => {
    setEnabled(true);
    setActiveGoals((prev) => (prev.includes(goalId) ? prev : [...prev, goalId]));
  }, []);
  const deactivate = useCallback((goalId: string) => {
    setActiveGoals((prev) => prev.filter((g) => g !== goalId));
  }, []);
  const markReached = useCallback((goalId: string) => {
    setActiveGoals((prev) => prev.filter((g) => g !== goalId));
    setCompletedGoals((prev) => {
      if (prev.includes(goalId)) return prev;
      const next = [...prev, goalId];
      saveCompleted(next);
      return next;
    });
  }, []);
  const isCompleted = useCallback((goalId: string) => completedGoals.includes(goalId), [completedGoals]);

  // Live registry views, recomputed when the registry version changes.
  const edges = useMemo(() => Object.values(edgeRef.current), [version]);
  const goals = useMemo(() => ({ ...goalRef.current }), [version]);
  const statusPredicates = useMemo(() => ({ ...statusRef.current }), [version]);

  const value = useMemo<AzGuidanceContextValue>(
    () => ({
      enabled, activeGoals, completedGoals, enable, disable, activate, deactivate, markReached, isCompleted,
      statusPredicates, edges, goals,
      registerStatus, unregisterStatus, registerEdge, unregisterEdge, registerGoal, unregisterGoal,
    }),
    [enabled, activeGoals, completedGoals, enable, disable, activate, deactivate, markReached, isCompleted,
     statusPredicates, edges, goals,
     registerStatus, unregisterStatus, registerEdge, unregisterEdge, registerGoal, unregisterGoal],
  );

  return <AzGuidanceContext.Provider value={value}>{children}</AzGuidanceContext.Provider>;
};

/** No-op controller returned when used outside a provider (so screens don't have to guard). */
const NOOP: AzGuidanceContextValue = {
  enabled: false, activeGoals: [], completedGoals: [],
  enable: () => {}, disable: () => {}, activate: () => {}, deactivate: () => {}, markReached: () => {}, isCompleted: () => false,
  statusPredicates: {}, edges: [], goals: {},
  registerStatus: () => {}, unregisterStatus: () => {}, registerEdge: () => {}, unregisterEdge: () => {}, registerGoal: () => {}, unregisterGoal: () => {},
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
