import React from 'react';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';
interface AzCyclerProps {
    options: string[];
    selectedOption: string;
    onCycle: (option: string) => void;
    color?: string;
    shape?: AzButtonShape;
    style?: ViewStyle;
    disabled?: boolean;
    disabledOptions?: string[];
    testID?: string;
}
export declare const AzCycler: React.FC<AzCyclerProps>;
export {};
//# sourceMappingURL=AzCycler.d.ts.map