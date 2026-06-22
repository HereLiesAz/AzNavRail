import React from 'react';
import { ViewStyle } from 'react-native';
import { AzTextBoxProps } from './AzTextBox';
/** Declarative description of a single field in an `AzForm` when using the `entries` API. */
export interface AzFormEntryData {
    /** Field name; used as the key into the data object passed to `onSubmit`. */
    name: string;
    /** Placeholder hint shown inside the input. */
    hint: string;
    /** When true, the input expands to multiple lines. */
    multiline?: boolean;
    /** When true, the input masks characters and shows a SHOW/HIDE reveal toggle. */
    secret?: boolean;
    /** React node rendered before the input text. */
    leadingIcon?: React.ReactNode;
    /** When true, the input outline is forced to red. */
    isError?: boolean;
    /** When false, the input renders at 50% opacity and is non-editable. */
    enabled?: boolean;
    /** Pre-filled value. */
    initialValue?: string;
    /** Software-keyboard type passed to the underlying `TextInput`. */
    keyboardType?: any;
    /** Return-key label passed to the underlying `TextInput`. */
    returnKeyType?: any;
}
/** Props for `AzForm` — a container that gathers `AzTextBox` inputs and emits a single submit payload. */
export interface AzFormProps {
    /** Form identifier; used to scope autocomplete history per form. */
    formName: string;
    /** Called with the collected `{ fieldName: value }` map when the user taps submit. */
    onSubmit: (data: Record<string, string>) => void;
    /** Accent color for outlines and the submit button. */
    outlineColor?: string;
    /** When true, fields use the outlined style; otherwise filled. */
    outlined?: boolean;
    /** Custom React content for the submit button. */
    submitButtonContent?: React.ReactNode;
    /** Either `<AzFormEntry>` children (legacy API) or arbitrary content placed above the submit button. */
    children?: React.ReactNode;
    /** Style merged into the outer container. */
    style?: ViewStyle;
    /** Declarative list of fields; when provided, replaces the children API. */
    entries?: AzFormEntryData[];
    /** Trailing icon applied to every field rendered via `entries`. */
    trailingIcon?: React.ReactNode;
}
/**
 * Form container with two equivalent APIs:
 *  - Pass an `entries` array for a declarative spec mirroring the Android `AzForm` builder.
 *  - Or nest `<AzFormEntry>` children for a stack of inputs sharing the form context.
 *
 * Calls `onSubmit` with a `{ fieldName: value }` map when the user taps the inline submit button.
 */
export declare const AzForm: React.FC<AzFormProps>;
/** Props for `AzFormEntry`, an `AzTextBox` registered into the surrounding `AzForm` by `name`. */
export interface AzFormEntryProps extends Omit<AzTextBoxProps, 'onSubmit' | 'submitButtonContent'> {
    /** Field name; used as the key in the data emitted by the parent form. */
    name: string;
    /** Initial value for uncontrolled use. */
    initialValue?: string;
}
/** Single `AzTextBox` row that registers itself into the nearest `AzForm` context. */
export declare const AzFormEntry: React.FC<AzFormEntryProps>;
//# sourceMappingURL=AzForm.d.ts.map