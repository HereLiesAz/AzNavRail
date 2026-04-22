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
  initialValue = '',
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
  const [internalValue, setInternalValue] = useState(initialValue);
  const [isRevealed, setIsRevealed] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const isControlled = value !== undefined;
  useEffect(() => {
    if (!isControlled) {
      setInternalValue(initialValue);
    }
  }, [initialValue, isControlled]);
  const currentValue = isControlled ? value : internalValue;

  // Handle outside click to close suggestions
  const containerRef = useRef(null);
  useEffect(() => {
    const handleClickOutside = event => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
  const handleValueChange = newVal => {
    if (!isControlled) {
      setInternalValue(newVal);
    }
    if (onValueChange) {
      onValueChange(newVal);
    }
  };
  const handleClear = () => {
    var _containerRef$current;
    handleValueChange('');
    if ((_containerRef$current = containerRef.current) !== null && _containerRef$current !== void 0 && _containerRef$current.querySelector('input, textarea')) {
      containerRef.current.querySelector('input, textarea').focus();
    }
  };
  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit(currentValue);
      setShowSuggestions(false);
    }
  };
  const handleKeyDown = e => {
    if (e.key === 'Enter' && !multiline) {
      handleSubmit();
    }
  };
  const handleSuggestionClick = suggestion => {
    handleValueChange(suggestion);
    setShowSuggestions(false);
  };
  const inputType = secret && !isRevealed ? 'password' : 'text';
  const InputComponent = multiline ? 'textarea' : 'input';
  const filteredSuggestions = suggestions.filter(s => s.toLowerCase().includes(currentValue.toLowerCase()) && s !== currentValue);
  return /*#__PURE__*/React.createElement("div", {
    className: `az-textbox-container ${className}`,
    ref: containerRef,
    style: {
      color: color,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: `az-textbox-wrapper ${multiline ? 'multiline' : ''} ${isError ? 'error' : ''} ${!enabled ? 'disabled' : ''}`,
    style: {
      borderColor: color
    }
  }, leadingIcon && /*#__PURE__*/React.createElement("div", {
    className: "az-textbox-icon leading"
  }, leadingIcon), /*#__PURE__*/React.createElement(InputComponent, {
    className: "az-textbox-input",
    value: currentValue,
    onChange: e => {
      handleValueChange(e.target.value);
      setShowSuggestions(true);
    },
    onFocus: () => {
      setShowSuggestions(true);
    },
    onKeyDown: handleKeyDown,
    placeholder: hint,
    disabled: !enabled,
    readOnly: readOnly,
    type: multiline ? undefined : inputType,
    rows: multiline ? 3 : undefined,
    style: {
      color: 'inherit'
    }
  }), currentValue && enabled && !readOnly && /*#__PURE__*/React.createElement("div", {
    className: "az-textbox-icon action",
    onClick: secret ? () => setIsRevealed(!isRevealed) : handleClear
  }, secret ? isRevealed ? '👁️' : '👁️‍🗨️' : '✕'), trailingIcon && /*#__PURE__*/React.createElement("div", {
    className: "az-textbox-icon trailing"
  }, trailingIcon), onSubmit && /*#__PURE__*/React.createElement("button", {
    className: "az-textbox-submit",
    onClick: handleSubmit,
    style: {
      borderColor: color,
      color: color
    },
    disabled: !enabled
  }, "Submit")), showSuggestions && filteredSuggestions.length > 0 && /*#__PURE__*/React.createElement("div", {
    className: "az-textbox-suggestions",
    style: {
      borderColor: color
    }
  }, filteredSuggestions.map((suggestion, index) => /*#__PURE__*/React.createElement("div", {
    key: index,
    className: "az-textbox-suggestion-item",
    onClick: () => handleSuggestionClick(suggestion)
  }, suggestion))));
};
export default AzTextBox;
//# sourceMappingURL=AzTextBox.js.map