import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import { AzTutorialController } from '../types';

// Optional AsyncStorage — used if installed, no-op otherwise
let AsyncStorage: { getItem: (k: string) => Promise<string | null>; setItem: (k: string, v: string) => Promise<void> } | null = null;
try {
  AsyncStorage = require('@react-native-async-storage/async-storage').default;
} catch {}

const STORAGE_KEY = 'az_navrail_read_tutorials';

/** React context that exposes the singleton `AzTutorialController` to descendants of `AzTutorialProvider`. */
export const AzTutorialContext = createContext<AzTutorialController | null>(null);

/** Props for `AzTutorialProvider`. */
interface AzTutorialProviderProps {
  /** Tree that will be able to read the tutorial controller via `useAzTutorialController`. */
  children: React.ReactNode;
  /** Optional tutorial id to be considered active at mount (useful for testing). */
  initialActiveTutorialId?: string | null;
  /** Optional list of tutorial ids to seed as already-read. */
  initialReadTutorials?: string[];
}

/**
 * Wraps the app (or a subtree) in a tutorial controller. The controller persists
 * the read-tutorial list in AsyncStorage when the optional dependency is installed.
 * `AzNavRail` mounts a provider automatically; only wrap manually when you need
 * a tutorial controller outside the rail tree.
 */
export const AzTutorialProvider: React.FC<AzTutorialProviderProps> = ({
  children,
  initialActiveTutorialId = null,
  initialReadTutorials = [],
}) => {
  const [activeTutorialId, setActiveTutorialId] = useState<string | null>(initialActiveTutorialId);
  const [readTutorials, setReadTutorials] = useState<string[]>(initialReadTutorials);
  const [currentVariables, setCurrentVariables] = useState<Record<string, any>>({});
  const [pendingEvent, setPendingEvent] = useState<string | null>(null);

  // Load persisted read tutorials on mount
  useEffect(() => {
    if (!AsyncStorage) return;
    AsyncStorage.getItem(STORAGE_KEY).then((raw) => {
      if (raw) {
        try {
          const ids: string[] = JSON.parse(raw);
          setReadTutorials((prev) => {
            const merged = Array.from(new Set([...prev, ...ids]));
            return merged.length === prev.length ? prev : merged;
          });
        } catch {}
      }
    });
  }, []);

  const startTutorial = useCallback((id: string, variables: Record<string, any> = {}) => {
    setCurrentVariables(variables);
    setActiveTutorialId(id);
  }, []);

  const endTutorial = useCallback(() => {
    setActiveTutorialId(null);
    setCurrentVariables({});
    setPendingEvent(null);
  }, []);

  const markTutorialRead = useCallback((id: string) => {
    setReadTutorials((prev) => {
      if (prev.includes(id)) return prev;
      const next = [...prev, id];
      AsyncStorage?.setItem(STORAGE_KEY, JSON.stringify(next)).catch(() => {});
      return next;
    });
  }, []);

  const isTutorialRead = useCallback(
    (id: string) => readTutorials.includes(id),
    [readTutorials]
  );

  const fireEvent = useCallback((name: string) => { setPendingEvent(name); }, []);
  const consumeEvent = useCallback(() => { setPendingEvent(null); }, []);

  const contextValue = useMemo<AzTutorialController>(
    () => ({
      activeTutorialId,
      readTutorials,
      currentVariables,
      pendingEvent,
      startTutorial,
      endTutorial,
      markTutorialRead,
      isTutorialRead,
      fireEvent,
      consumeEvent,
    }),
    [activeTutorialId, readTutorials, currentVariables, pendingEvent,
     startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent]
  );

  return (
    <AzTutorialContext.Provider value={contextValue}>
      {children}
    </AzTutorialContext.Provider>
  );
};

/**
 * Reads the active `AzTutorialController` from context.
 *
 * @throws When called outside an `AzTutorialProvider` (or outside an `AzNavRail`).
 * @returns The controller exposing `startTutorial`, `endTutorial`, `markTutorialRead`,
 *   `isTutorialRead`, `fireEvent`, `consumeEvent`, and the read-only state fields
 *   `activeTutorialId`, `readTutorials`, `currentVariables`, and `pendingEvent`.
 */
export const useAzTutorialController = (): AzTutorialController => {
  const context = useContext(AzTutorialContext);
  if (!context) {
    throw new Error('useAzTutorialController must be used within an AzTutorialProvider');
  }
  return context;
};
