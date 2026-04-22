export default AzCycler;
/**
 * A component that cycles through a list of options when clicked.
 * It waits 1 second after the last click before triggering the action.
 *
 * @param {object} props
 * @param {Array<string>} props.options - The list of options.
 * @param {string} props.value - The current selected value.
 * @param {function} props.onValueChange - Callback when the value changes (confirmed after delay).
 * @param {string} [props.label] - Optional label.
 * @param {boolean} [props.enabled=true] - Whether the cycler is enabled.
 * @param {string} [props.color='currentColor'] - The color.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzCycler({ options, value, onValueChange, label, enabled, color, className, style }: {
    options: Array<string>;
    value: string;
    onValueChange: Function;
    label?: string | undefined;
    enabled?: boolean | undefined;
    color?: string | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzCycler.d.ts.map