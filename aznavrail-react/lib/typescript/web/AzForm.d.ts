export default AzForm;
/**
 * A form component that manages multiple entries.
 *
 * @param {object} props
 * @param {Array<object>} props.entries - The list of form entries.
 *   Each entry object can have:
 *   - name (string): Unique identifier.
 *   - hint (string): Placeholder.
 *   - secret (boolean): Is password?
 *   - multiline (boolean): Is textarea?
 *   - leadingIcon (ReactNode)
 *   - initialValue (string)
 * @param {React.ReactNode} [props.trailingIcon] - Icon applied to all entries.
 * @param {function} props.onSubmit - Callback with map of values { name: value }.
 * @param {string} [props.submitText='Submit'] - Text for the submit button.
 * @param {string} [props.color='currentColor'] - The color.
 * @param {boolean} [props.isLoading=false] - Whether the form is submitting.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzForm({ entries, trailingIcon, onSubmit, submitText, color, isLoading, className, style }: {
    entries: Array<object>;
    trailingIcon?: React.ReactNode;
    onSubmit: Function;
    submitText?: string | undefined;
    color?: string | undefined;
    isLoading?: boolean | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzForm.d.ts.map