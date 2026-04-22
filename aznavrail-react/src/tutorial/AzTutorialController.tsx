import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { AzTutorialController } from '../types';

export const AzTutorialContext = createContext<AzTutorialController | null>(null);

interface AzTutorialProviderProps {
  children: React.ReactNode;
  initialActiveTutorialId?: string | null;
  initialReadTutorials?: string[];
}

export const AzTutorialProvider: React.FC<AzTutorialProviderProps> = ({
  children,
  initialActiveTutorialId = null,
  initialReadTutorials = [],
}) => {
  const [activeTutorialId, setActiveTutorialId] = useState<string | null>(initialActiveTutorialId);
  const [readTutorials, setReadTutorials] = useState<string[]>(initialReadTutorials);

  const startTutorial = useCallback((id: string) => {
    setActiveTutorialId(id);
  }, []);

  const endTutorial = useCallback(() => {
    setActiveTutorialId(null);
  }, []);

  const markTutorialRead = useCallback((id: string) => {
    setReadTutorials((prev) => {
      if (!prev.includes(id)) {
        return [...prev, id];
      }
      return prev;
    });
  }, []);

  const isTutorialRead = useCallback(
    (id: string) => {
      return readTutorials.includes(id);
    },
    [readTutorials]
  );

  const contextValue = useMemo<AzTutorialController>(
    () => ({
      activeTutorialId,
      readTutorials,
      startTutorial,
      endTutorial,
      markTutorialRead,
      isTutorialRead,
    }),
    [activeTutorialId, readTutorials, startTutorial, endTutorial, markTutorialRead, isTutorialRead]
  );

  return (
    <AzTutorialContext.Provider value={contextValue}>
      {children}
    </AzTutorialContext.Provider>
  );
};

export const useAzTutorialController = (): AzTutorialController => {
  const context = useContext(AzTutorialContext);
  if (!context) {
    throw new Error('useAzTutorialController must be used within an AzTutorialProvider');
  }
  return context;
};
