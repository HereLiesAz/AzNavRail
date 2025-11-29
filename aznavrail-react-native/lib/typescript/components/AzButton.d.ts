import React from 'react';
import { ViewStyle } from 'react-native';
import { AzButtonShape } from '../types';
interface AzButtonProps {
    text: string;
    onClick: () => void;
    color?: string;
    shape?: AzButtonShape;
    style?: ViewStyle;
    disabled?: boolean;
    testID?: string;
}
export declare const AzButton: React.FC<AzButtonProps>;
export {};
//# sourceMappingURL=AzButton.d.ts.map