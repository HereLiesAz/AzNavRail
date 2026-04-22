export default AzToggle;
/**
 * A toggle switch component.
 *
 * @param {object} props
 * @param {boolean} props.value - The current state (true/false).
 * @param {function} props.onValueChange - Callback for state change.
 * @param {string} [props.label] - Optional label text.
 * @param {boolean} [props.enabled=true] - Whether the toggle is enabled.
 * @param {string} [props.color='currentColor'] - The color of the toggle.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzToggle({ value, onValueChange, label, enabled, color, className, style }: {
    value: boolean;
    onValueChange: Function;
    label?: string | undefined;
    enabled?: boolean | undefined;
    color?: string | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzToggle.d.ts.map