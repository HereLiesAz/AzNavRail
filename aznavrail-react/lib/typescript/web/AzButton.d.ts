export default AzButton;
/**
 * A standalone button component that matches the AzNavRail style.
 *
 * @param {object} props - The component props.
 * @param {string} props.text - The button text.
 * @param {function} props.onClick - The click handler.
 * @param {string} [props.shape='RECTANGLE'] - The shape of the button (CIRCLE, SQUARE, RECTANGLE, ROUNDED, NONE).
 * @param {string} [props.color='currentColor'] - The color of the button (border and text).
 * @param {string} [props.fillColor] - The custom background fill color of the button.
 * @param {boolean} [props.isLoading=false] - Whether the button is in a loading state.
 * @param {boolean} [props.enabled=true] - Whether the button is enabled.
 * @param {object} [props.contentPadding] - Custom padding.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
declare function AzButton({ text, onClick, shape, color, fillColor, isLoading, enabled, contentPadding, className, style }: {
    text: string;
    onClick: Function;
    shape?: string | undefined;
    color?: string | undefined;
    fillColor?: string | undefined;
    isLoading?: boolean | undefined;
    enabled?: boolean | undefined;
    contentPadding?: object | undefined;
    className?: string | undefined;
    style?: object | undefined;
}): React.JSX.Element;
import React from 'react';
//# sourceMappingURL=AzButton.d.ts.map