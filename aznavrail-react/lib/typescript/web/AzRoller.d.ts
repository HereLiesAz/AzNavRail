export default AzRoller;
/**
 * A dropdown menu that works like a roller or slot machine, cycling through options infinitely.
 *
 * @param {object} props
 * @param {Array<string>} props.options - The list of options to display.
 * @param {string} props.selectedOption - The currently selected option.
 * @param {function} props.onOptionSelected - Callback when an option is selected.
 * @param {string} [props.hint] - Placeholder text.
 * @param {boolean} [props.enabled=true] - Whether the component is enabled.
 * @param {string} [props.color='currentColor'] - The color of the border and text.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzRoller({ options, selectedOption, onOptionSelected, hint, enabled, color, className, style }: {
    options: Array<string>;
    selectedOption: string;
    onOptionSelected: Function;
    hint?: string | undefined;
    enabled?: boolean | undefined;
    color?: string | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzRoller.d.ts.map