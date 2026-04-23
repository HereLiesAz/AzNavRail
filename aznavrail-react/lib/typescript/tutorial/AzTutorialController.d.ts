import React from 'react';
import { AzTutorialController } from '../types';
export declare const AzTutorialContext: React.Context<AzTutorialController | null>;
interface AzTutorialProviderProps {
    children: React.ReactNode;
    initialActiveTutorialId?: string | null;
    initialReadTutorials?: string[];
}
export declare const AzTutorialProvider: React.FC<AzTutorialProviderProps>;
export declare const useAzTutorialController: () => AzTutorialController;
export {};
//# sourceMappingURL=AzTutorialController.d.ts.map