export default AzTextBox;
/**
 * A text input component with support for icons, clearing, submitting, and suggestions.
 *
 * @param {object} props
 * @param {string} props.value - The current value.
 * @param {function} props.onValueChange - Callback for value change.
 * @param {string} [props.hint] - Placeholder text.
 * @param {boolean} [props.isError=false] - Whether to show error state.
 * @param {boolean} [props.enabled=true] - Whether the input is enabled.
 * @param {boolean} [props.readOnly=false] - Whether the input is read-only.
 * @param {boolean} [props.secret=false] - Whether to hide text (password).
 * @param {boolean} [props.multiline=false] - Whether to allow multiple lines.
 * @param {React.ReactNode} [props.leadingIcon] - Icon to display at the start.
 * @param {React.ReactNode} [props.trailingIcon] - Icon to display at the end.
 * @param {function} [props.onSubmit] - Callback when submit is triggered.
 * @param {string} [props.color='currentColor'] - The color of the border and text.
 * @param {Array<string>} [props.suggestions=[]] - Autocomplete suggestions.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzTextBox({ value, initialValue, onValueChange, hint, isError, enabled, readOnly, secret, multiline, leadingIcon, trailingIcon, onSubmit, color, suggestions, className, style }: {
    value: string;
    onValueChange: Function;
    hint?: string | undefined;
    isError?: boolean | undefined;
    enabled?: boolean | undefined;
    readOnly?: boolean | undefined;
    secret?: boolean | undefined;
    multiline?: boolean | undefined;
    leadingIcon?: React.ReactNode;
    trailingIcon?: React.ReactNode;
    onSubmit?: Function | undefined;
    color?: string | undefined;
    suggestions?: string[] | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzTextBox.d.ts.map