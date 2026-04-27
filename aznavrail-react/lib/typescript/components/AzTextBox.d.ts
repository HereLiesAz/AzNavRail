import React from 'react';
import { ViewStyle } from 'react-native';
export declare const AzTextBoxDefaults: {
    setSuggestionLimit: (limit: number) => void;
};
export interface AzTextBoxProps {
    value?: string;
    initialValue?: string;
    onValueChange?: (text: string) => void;
    hint?: string;
    outlined?: boolean;
    multiline?: boolean;
    secret?: boolean;
    outlineColor?: string;
    historyContext?: string;
    submitButtonContent?: React.ReactNode;
    onSubmit?: (text: string) => void;
    showSubmitButton?: boolean;
    style?: ViewStyle;
    containerStyle?: ViewStyle;
    backgroundColor?: string;
    backgroundOpacity?: number;
    enabled?: boolean;
}
export declare const AzTextBox: React.FC<AzTextBoxProps>;
//# sourceMappingURL=AzTextBox.d.ts.map