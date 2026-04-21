import React from 'react';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';
interface AzToggleProps {
    isChecked: boolean;
    onToggle: () => void;
    toggleOnText: string;
    toggleOffText: string;
    color?: string;
    shape?: AzButtonShape;
    style?: ViewStyle;
    disabled?: boolean;
    testID?: string;
}
export declare const AzToggle: React.FC<AzToggleProps>;
export {};
//# sourceMappingURL=AzToggle.d.ts.map