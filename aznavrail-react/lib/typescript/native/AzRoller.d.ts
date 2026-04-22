import React from 'react';
import { ViewStyle } from 'react-native';
export interface AzRollerProps {
    options: string[];
    selectedOption?: string;
    onOptionSelected: (option: string) => void;
    hint?: string;
    enabled?: boolean;
    outlineColor?: string;
    backgroundColor?: string;
    backgroundOpacity?: number;
    style?: ViewStyle;
    isError?: boolean;
}
export declare const AzRoller: React.FC<AzRollerProps>;
//# sourceMappingURL=AzRoller.d.ts.map