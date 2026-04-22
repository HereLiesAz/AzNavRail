import React from 'react';
import { AzTutorial } from '../types';
interface AzTutorialOverlayProps {
    tutorialId: string;
    tutorial: AzTutorial;
    onDismiss: () => void;
    itemBoundsCache: Record<string, {
        x: number;
        y: number;
        width: number;
        height: number;
    }>;
}
export declare const AzTutorialOverlay: React.FC<AzTutorialOverlayProps>;
export {};
//# sourceMappingURL=AzTutorialOverlay.d.ts.map