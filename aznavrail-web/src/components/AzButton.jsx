import React from 'react';
import useFitText from '../hooks/useFitText';
import AzLoad from './AzLoad';
import './AzButton.css';

/**
 * A standalone button component that matches the AzNavRail style.
 *
 * @param {object} props - The component props.
 * @param {string} props.text - The button text.
 * @param {function} props.onClick - The click handler.
 * @param {string} [props.shape='RECTANGLE'] - The shape of the button (CIRCLE, SQUARE, RECTANGLE, ROUNDED, NONE).
 * @param {string} [props.color='currentColor'] - The color of the button (border and text).
 * @param {boolean} [props.isLoading=false] - Whether the button is in a loading state.
 * @param {boolean} [props.enabled=true] - Whether the button is enabled.
 * @param {object} [props.contentPadding] - Custom padding.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzButton = ({
  text,
  onClick,
  shape = 'RECTANGLE',
  color = 'currentColor',
  isLoading = false,
  enabled = true,
  contentPadding,
  className = '',
  style = {}
}) => {
  const isFixedSize = shape === 'CIRCLE' || shape === 'SQUARE';
  const fitTextRef = useFitText();
  const textRef = isFixedSize ? fitTextRef : null;

  const shapeClass = `az-button-shape-${shape.toLowerCase()}`;

  const customStyle = {
    borderColor: color,
    color: color,
    ...style,
    ...(contentPadding ? { padding: contentPadding } : {})
  };

  return (
    <button
      className={`az-button ${shapeClass} ${className}`}
      onClick={enabled && !isLoading ? onClick : undefined}
      disabled={!enabled}
      style={customStyle}
    >
      <div className="az-button-content" style={{ opacity: isLoading ? 0 : 1 }}>
        <span
          ref={textRef}
          className="az-button-text"
          style={!isFixedSize ? { fontSize: '14px' } : {}}
        >
          {text}
        </span>
      </div>
      {isLoading && (
        <AzLoad color={color} size={20} />
      )}
    </button>
  );
};

export default AzButton;
