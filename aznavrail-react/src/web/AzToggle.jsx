import React from 'react';
import './AzToggle.css';

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
const AzToggle = ({
  value,
  onValueChange,
  label,
  enabled = true,
  color = 'currentColor',
  className = '',
  style = {}
}) => {
  const handleChange = (e) => {
    if (enabled) {
      onValueChange(e.target.checked);
    }
  };

  return (
    <label
      className={`az-toggle-container ${!enabled ? 'disabled' : ''} ${className}`}
      style={{ color: color, ...style }}
    >
      <input
        type="checkbox"
        className="az-toggle-input"
        checked={!!value}
        onChange={handleChange}
        disabled={!enabled}
      />
      <div className="az-toggle-track" style={{ borderColor: color }}>
        <div className="az-toggle-thumb" style={{ backgroundColor: color }} />
      </div>
      {label && <span className="az-toggle-label">{label}</span>}
    </label>
  );
};

export default AzToggle;
