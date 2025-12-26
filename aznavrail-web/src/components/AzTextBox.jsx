import React, { useState, useRef, useEffect } from 'react';
import './AzTextBox.css';

/**
 * A text input component with support for icons, clearing, submitting, and suggestions.
 *
 * @param {object} props
 * @param {string} props.value - The current value.
 * @param {function} props.onValueChange - Callback for value change.
 * @param {string} [props.hint] - Placeholder text.
 * @param {boolean} [props.isError=false] - Whether to show error state.
 * @param {boolean} [props.enabled=true] - Whether the input is enabled.
 * @param {boolean} [props.readOnly=false] - Whether the input is read-only.
 * @param {boolean} [props.secret=false] - Whether to hide text (password).
 * @param {boolean} [props.multiline=false] - Whether to allow multiple lines.
 * @param {React.ReactNode} [props.leadingIcon] - Icon to display at the start.
 * @param {React.ReactNode} [props.trailingIcon] - Icon to display at the end.
 * @param {function} [props.onSubmit] - Callback when submit is triggered.
 * @param {string} [props.color='currentColor'] - The color of the border and text.
 * @param {Array<string>} [props.suggestions=[]] - Autocomplete suggestions.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzTextBox = ({
  value,
  onValueChange,
  hint,
  isError = false,
  enabled = true,
  readOnly = false,
  secret = false,
  multiline = false,
  leadingIcon,
  trailingIcon,
  onSubmit,
  color = 'currentColor',
  suggestions = [],
  className = '',
  style = {}
}) => {
  const [isRevealed, setIsRevealed] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Handle outside click to close suggestions
  const containerRef = useRef(null);
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleClear = () => {
    onValueChange('');
    if (containerRef.current?.querySelector('input, textarea')) {
      containerRef.current.querySelector('input, textarea').focus();
    }
  };

  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit(value);
      setShowSuggestions(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !multiline) {
      handleSubmit();
    }
  };

  const handleSuggestionClick = (suggestion) => {
    onValueChange(suggestion);
    setShowSuggestions(false);
  };

  const inputType = secret && !isRevealed ? 'password' : 'text';
  const InputComponent = multiline ? 'textarea' : 'input';

  const filteredSuggestions = suggestions.filter(s =>
    s.toLowerCase().includes(value.toLowerCase()) && s !== value
  );

  return (
    <div
      className={`az-textbox-container ${className}`}
      ref={containerRef}
      style={{ color: color, ...style }}
    >
      <div
        className={`az-textbox-wrapper ${multiline ? 'multiline' : ''} ${isError ? 'error' : ''} ${!enabled ? 'disabled' : ''}`}
        style={{ borderColor: color }}
      >
        {leadingIcon && (
          <div className="az-textbox-icon leading">
            {leadingIcon}
          </div>
        )}

        <InputComponent
          className="az-textbox-input"
          value={value}
          onChange={(e) => {
            onValueChange(e.target.value);
            setShowSuggestions(true);
          }}
          onFocus={() => {
            setShowSuggestions(true);
          }}
          onKeyDown={handleKeyDown}
          placeholder={hint}
          disabled={!enabled}
          readOnly={readOnly}
          type={multiline ? undefined : inputType}
          rows={multiline ? 3 : undefined}
          style={{ color: 'inherit' }}
        />

        {value && enabled && !readOnly && (
          <div className="az-textbox-icon action" onClick={secret ? () => setIsRevealed(!isRevealed) : handleClear}>
            {secret ? (isRevealed ? '👁️' : '👁️‍🗨️') : '✕'}
          </div>
        )}

        {trailingIcon && (
          <div className="az-textbox-icon trailing">
            {trailingIcon}
          </div>
        )}

        {onSubmit && (
          <button
            className="az-textbox-submit"
            onClick={handleSubmit}
            style={{ borderColor: color, color: color }}
            disabled={!enabled}
          >
            Submit
          </button>
        )}
      </div>

      {showSuggestions && filteredSuggestions.length > 0 && (
        <div className="az-textbox-suggestions" style={{ borderColor: color }}>
          {filteredSuggestions.map((suggestion, index) => (
            <div
              key={index}
              className="az-textbox-suggestion-item"
              onClick={() => handleSuggestionClick(suggestion)}
            >
              {suggestion}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AzTextBox;
