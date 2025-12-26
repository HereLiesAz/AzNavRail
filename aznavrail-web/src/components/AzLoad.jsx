import React from 'react';
import './AzLoad.css';

/**
 * A loading spinner component.
 *
 * @param {object} props - The component props.
 * @param {string} [props.color='currentColor'] - The color of the spinner.
 * @param {number} [props.size=24] - The size of the spinner in pixels.
 * @param {object} [props.style] - Additional styles.
 * @param {string} [props.className] - Additional classes.
 */
const AzLoad = ({ color = 'currentColor', size = 24, style = {}, className = '' }) => {
  return (
    <div
      className={`az-load-container ${className}`}
      style={{ width: size, height: size, ...style }}
    >
      <div
        className="az-load-spinner"
        style={{
          width: size,
          height: size,
          borderColor: 'rgba(0, 0, 0, 0.1)',
          borderLeftColor: color,
        }}
      />
    </div>
  );
};

export default AzLoad;
