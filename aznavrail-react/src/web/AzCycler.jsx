import React, { useState, useEffect, useRef } from 'react';
import './AzCycler.css';

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
const AzCycler = ({
  options = [],
  value,
  onValueChange,
  label,
  enabled = true,
  color = 'currentColor',
  className = '',
  style = {}
}) => {
  const [displayedValue, setDisplayedValue] = useState(value);
  const timerRef = useRef(null);

  useEffect(() => {
    setDisplayedValue(value);
  }, [value]);

  useEffect(() => {
    return () => {
        if (timerRef.current) clearTimeout(timerRef.current);
    }
  }, []);

  const handleClick = () => {
    if (!enabled || options.length === 0) return;

    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    const currentIndex = options.indexOf(displayedValue);
    const nextIndex = (currentIndex + 1) % options.length;
    const nextValue = options[nextIndex];

    setDisplayedValue(nextValue);

    timerRef.current = setTimeout(() => {
      onValueChange(nextValue);
      timerRef.current = null;
    }, 1000);
  };

  return (
    <div
      className={`az-cycler-container ${!enabled ? 'disabled' : ''} ${className}`}
      onClick={handleClick}
      style={{ borderColor: color, color: color, ...style }}
    >
      {label && <span className="az-cycler-label">{label}</span>}
      <span className="az-cycler-value">{displayedValue}</span>
    </div>
  );
};

export default AzCycler;
