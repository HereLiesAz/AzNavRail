import React, { useState, useRef, useEffect } from 'react';
import './AzRoller.css';

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
const AzRoller = ({
  options = [],
  selectedOption,
  onOptionSelected,
  hint = '',
  enabled = true,
  color = 'currentColor',
  className = '',
  style = {}
}) => {
  const [expanded, setExpanded] = useState(false);
  const containerRef = useRef(null);
  const dropdownRef = useRef(null);

  // Handle outside click to close dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setExpanded(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Simulate infinite scroll by repeating options
  // We repeat the list multiple times.
  const repeatCount = 50;
  const displayOptions = [];
  if (options.length > 0) {
      for (let i = 0; i < repeatCount; i++) {
          displayOptions.push(...options);
      }
  }

  // Auto-scroll to middle on open
  useEffect(() => {
      if (expanded && dropdownRef.current && options.length > 0) {
          const itemHeight = 34; // Approximate height of an option
          const totalItems = displayOptions.length;
          // Scroll to roughly the middle
          dropdownRef.current.scrollTop = (totalItems * itemHeight) / 2;
      }
  }, [expanded, options.length, displayOptions.length]);

  return (
    <div
      className={`az-roller-container ${className}`}
      ref={containerRef}
      style={{ color: color, ...style }}
    >
      <div
        className={`az-roller-wrapper ${!enabled ? 'disabled' : ''}`}
        style={{ borderColor: color }}
        onClick={() => enabled && setExpanded(!expanded)}
      >
        <div className="az-roller-text" style={{ color: selectedOption ? 'inherit' : 'rgba(0,0,0,0.5)' }}>
            {selectedOption || hint}
        </div>
        <div className="az-roller-icon">
            ▼
        </div>
      </div>

      {expanded && enabled && options.length > 0 && (
        <div
            className="az-roller-dropdown"
            style={{ borderColor: color, backgroundColor: 'white' }}
            ref={dropdownRef}
        >
          {displayOptions.map((option, index) => {
             // Determine background color for striping or selection
             // Note: Since this is infinite list, true 'selected' highlighting might appear many times.
             // We'll highlight if it matches selectedOption.
             const isSelected = option === selectedOption;
             const isEven = index % 2 === 0;
             let bg = 'transparent';
             if (isSelected) bg = 'rgba(0,0,0,0.1)';
             else if (isEven) bg = 'rgba(0,0,0,0.05)';
             else bg = 'transparent'; // or slightly different? Android version uses Surface with alpha.

             return (
                <div
                    key={index}
                    className="az-roller-option"
                    style={{ backgroundColor: bg }}
                    onClick={() => {
                        onOptionSelected(option);
                        setExpanded(false);
                    }}
                >
                    {option}
                </div>
             );
          })}
        </div>
      )}
    </div>
  );
};

export default AzRoller;
