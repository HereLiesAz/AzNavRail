import React from 'react';
import { ViewStyle } from 'react-native';
import { AzButtonShape } from '../types';
export interface AzButtonProps {
    text: string;
    onClick: () => void;
    color?: string;
    fillColor?: string;
    shape?: AzButtonShape;
    style?: ViewStyle;
    enabled?: boolean;
    isLoading?: boolean;
    testID?: string;
    hasCustomContent?: boolean;
    content?: React.ReactNode;
}
export declare const AzButton: React.FC<AzButtonProps>;
//# sourceMappingURL=AzButton.d.ts.map