import React from 'react';
import { ViewStyle, TextInputProps } from 'react-native';
/** Module-level configuration for `AzTextBox` autocomplete behaviour. */
export declare const AzTextBoxDefaults: {
    /** Sets the maximum number of suggestion entries retained by the shared `historyManager`. */
    setSuggestionLimit: (limit: number) => void;
};
/** Props for the `AzTextBox` text input. Extends `TextInputProps` minus `value`/`onChangeText` which are renamed below. */
export interface AzTextBoxProps extends Omit<TextInputProps, 'value' | 'onChangeText'> {
    /** Controlled text value. When set, the input is fully controlled. */
    value?: string;
    /** Initial text value used when the input is uncontrolled. */
    initialValue?: string;
    /** Called with the new text on every keystroke. */
    onValueChange?: (text: string) => void;
    /** Placeholder text shown when the input is empty. */
    hint?: string;
    /** When true, draws an outline border; when false, uses a background fill. */
    outlined?: boolean;
    /** When true, the input expands to multiple lines. Mutually exclusive with `secret`. */
    multiline?: boolean;
    /** When true, the input masks characters and shows a SHOW/HIDE reveal toggle. */
    secret?: boolean;
    /** Outline color when `outlined` is true; also the placeholder/text color when no override is set. */
    outlineColor?: string;
    /** Independent text color override. */
    textColor?: string;
    /** Background fill color inside the input row. */
    fillColor?: string;
    /** Key used to scope autocomplete history; entries are shared across inputs with the same context. */
    historyContext?: string;
    /** Custom content for the inline submit button. */
    submitButtonContent?: React.ReactNode;
    /** Called with the current value when the user submits (taps the submit button or hits Return). */
    onSubmit?: (text: string) => void;
    /** When true, shows the inline submit button to the right of the input. */
    showSubmitButton?: boolean;
    /** Style merged into the underlying `TextInput`. */
    style?: ViewStyle;
    /** Style merged into the outer container `View`. */
    containerStyle?: ViewStyle;
    /** Background color of the input row. */
    backgroundColor?: string;
    /** Opacity of the input row background, 0–1. */
    backgroundOpacity?: number;
    /** When false, the input is rendered at 50% opacity and is non-editable. */
    enabled?: boolean;
    /** When true, the outline is forced to red and an error indicator is shown. */
    isError?: boolean;
    /** React node rendered before the input text. */
    leadingIcon?: React.ReactNode;
    /** React node rendered after the input text. */
    trailingIcon?: React.ReactNode;
    /** When true, displays an X button to clear the input while it has content. */
    showClearButton?: boolean;
}
/**
 * Text input with optional outline, inline submit button, history-backed autocomplete
 * suggestions (keyed by `historyContext`), secret-mode reveal toggle, and clear button.
 */
export declare const AzTextBox: React.FC<AzTextBoxProps>;
//# sourceMappingURL=AzTextBox.d.ts.map