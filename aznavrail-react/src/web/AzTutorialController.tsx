/// <reference lib="dom" />
import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { AzTutorialController } from '../types';

const STORAGE_KEY = 'az_navrail_read_tutorials';

function loadFromStorage(): string[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw) as string[];
  } catch {}
  return [];
}

function saveToStorage(ids: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  } catch {}
}

const AzWebTutorialContext = createContext<AzTutorialController | null>(null);

interface AzWebTutorialProviderProps {
  children: React.ReactNode;
}

export const AzWebTutorialProvider: React.FC<AzWebTutorialProviderProps> = ({ children }) => {
  const [activeTutorialId, setActiveTutorialId] = useState<string | null>(null);
  const [readTutorials, setReadTutorials] = useState<string[]>(() => loadFromStorage());
  const [currentVariables, setCurrentVariables] = useState<Record<string, any>>({});
  const [pendingEvent, setPendingEvent] = useState<string | null>(null);

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
      saveToStorage(next);
      return next;
    });
  }, []);

  const isTutorialRead = useCallback(
    (id: string) => readTutorials.includes(id),
    [readTutorials]
  );

  const fireEvent = useCallback((name: string) => { setPendingEvent(name); }, []);
  const consumeEvent = useCallback(() => { setPendingEvent(null); }, []);

  const value = useMemo<AzTutorialController>(
    () => ({
      activeTutorialId, readTutorials, currentVariables, pendingEvent,
      startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent,
    }),
    [activeTutorialId, readTutorials, currentVariables, pendingEvent,
     startTutorial, endTutorial, markTutorialRead, isTutorialRead, fireEvent, consumeEvent]
  );

  return (
    <AzWebTutorialContext.Provider value={value}>
      {children}
    </AzWebTutorialContext.Provider>
  );
};

export const useAzWebTutorialController = (): AzTutorialController => {
  const ctx = useContext(AzWebTutorialContext);
  if (!ctx) throw new Error('useAzWebTutorialController must be used within AzWebTutorialProvider');
  return ctx;
};
