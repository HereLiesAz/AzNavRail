import React, { useState } from 'react';
import AzTextBox from './AzTextBox';
import AzButton from './AzButton';
import './AzForm.css';

/**
 * A form component that manages multiple entries.
 *
 * @param {object} props
 * @param {Array<object>} props.entries - The list of form entries.
 *   Each entry object can have:
 *   - name (string): Unique identifier.
 *   - hint (string): Placeholder.
 *   - secret (boolean): Is password?
 *   - multiline (boolean): Is textarea?
 *   - leadingIcon (ReactNode)
 *   - initialValue (string)
 * @param {React.ReactNode} [props.trailingIcon] - Icon applied to all entries.
 * @param {function} props.onSubmit - Callback with map of values { name: value }.
 * @param {string} [props.submitText='Submit'] - Text for the submit button.
 * @param {string} [props.color='currentColor'] - The color.
 * @param {boolean} [props.isLoading=false] - Whether the form is submitting.
 * @param {string} [props.className] - Additional classes.
 * @param {object} [props.style] - Additional styles.
 */
const AzForm = ({
  entries = [],
  trailingIcon,
  onSubmit,
  submitText = 'Submit',
  color = 'currentColor',
  isLoading = false,
  className = '',
  style = {}
}) => {
  const [values, setValues] = useState(() => {
    const initial = {};
    entries.forEach(e => {
      initial[e.name] = e.initialValue || '';
    });
    return initial;
  });

  const handleChange = (name, newValue) => {
    setValues(prev => ({ ...prev, [name]: newValue }));
  };

  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit(values);
    }
  };

  return (
    <div className={`az-form-container ${className}`} style={style}>
      {entries.map(entry => (
        <AzTextBox
          key={entry.name}
          value={values[entry.name]}
          onValueChange={(val) => handleChange(entry.name, val)}
          hint={entry.hint}
          secret={entry.secret}
          multiline={entry.multiline}
          leadingIcon={entry.leadingIcon}
          trailingIcon={trailingIcon} // Applied to all entries
          color={color}
          enabled={!isLoading}
        />
      ))}
      <AzButton
        text={submitText}
        onClick={handleSubmit}
        color={color}
        isLoading={isLoading}
        shape="RECTANGLE"
        style={{ alignSelf: 'flex-end' }}
      />
    </div>
  );
};

export default AzForm;
