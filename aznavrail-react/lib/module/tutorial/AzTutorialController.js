import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
export const AzTutorialContext = /*#__PURE__*/createContext(null);
export const AzTutorialProvider = ({
  children,
  initialActiveTutorialId = null,
  initialReadTutorials = []
}) => {
  const [activeTutorialId, setActiveTutorialId] = useState(initialActiveTutorialId);
  const [readTutorials, setReadTutorials] = useState(initialReadTutorials);
  const startTutorial = useCallback(id => {
    setActiveTutorialId(id);
  }, []);
  const endTutorial = useCallback(() => {
    setActiveTutorialId(null);
  }, []);
  const markTutorialRead = useCallback(id => {
    setReadTutorials(prev => {
      if (!prev.includes(id)) {
        return [...prev, id];
      }
      return prev;
    });
  }, []);
  const isTutorialRead = useCallback(id => {
    return readTutorials.includes(id);
  }, [readTutorials]);
  const contextValue = useMemo(() => ({
    activeTutorialId,
    readTutorials,
    startTutorial,
    endTutorial,
    markTutorialRead,
    isTutorialRead
  }), [activeTutorialId, readTutorials, startTutorial, endTutorial, markTutorialRead, isTutorialRead]);
  return /*#__PURE__*/React.createElement(AzTutorialContext.Provider, {
    value: contextValue
  }, children);
};
export const useAzTutorialController = () => {
  const context = useContext(AzTutorialContext);
  if (!context) {
    throw new Error('useAzTutorialController must be used within an AzTutorialProvider');
  }
  return context;
};
//# sourceMappingURL=AzTutorialController.js.map