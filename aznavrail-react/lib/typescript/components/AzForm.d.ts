import React from 'react';
import { ViewStyle } from 'react-native';
import { AzTextBoxProps } from './AzTextBox';
export interface AzFormProps {
    formName: string;
    onSubmit: (data: Record<string, string>) => void;
    outlineColor?: string;
    outlined?: boolean;
    submitButtonContent?: React.ReactNode;
    children: React.ReactNode;
    style?: ViewStyle;
}
export declare const AzForm: React.FC<AzFormProps>;
interface AzFormEntryProps extends Omit<AzTextBoxProps, 'onSubmit' | 'submitButtonContent'> {
    name: string;
}
export declare const AzFormEntry: React.FC<AzFormEntryProps>;
export {};
//# sourceMappingURL=AzForm.d.ts.map