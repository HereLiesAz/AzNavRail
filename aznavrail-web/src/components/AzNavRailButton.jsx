import React from 'react';
import useFitText from '../hooks/useFitText';
import './AzNavRailButton.css';

/**
 * A circular button for the collapsed navigation rail.
 *
 * This component displays a single item in the rail, handling standard, toggle, and cycler
 * item types. It uses the `useFitText` hook to dynamically resize the text to fit within
 * the button's circular bounds.
 *
 * @param {object} props - The component props.
 * @param {object} props.item - The navigation item object to be rendered.
 * @param {string} props.item.text - The text for a standard item.
 * @param {boolean} [props.item.isToggle] - True if the item is a toggle.
 * @param {boolean} [props.item.isChecked] - The state of the toggle item.
 * @param {string} [props.item.toggleOnText] - Text for the "on" state of a toggle.
 * @param {string} [props.item.toggleOffText] - Text for the "off" state of a toggle.
 * @param {boolean} [props.item.isCycler] - True if the item is a cycler.
 * @param {string} [props.item.selectedOption] - The currently selected option for a cycler.
 * @param {function} props.item.onClick - The click handler for the item.
 * @param {string} [props.item.color] - The border color of the button.
 * @param {function} props.onCyclerClick - The specialized click handler for cycler items.
 */
const AzNavRailButton = ({ item, onCyclerClick }) => {
  const { text, isToggle, isChecked, toggleOnText, toggleOffText, isCycler, selectedOption, onClick, color } = item;
  const textRef = useFitText();

  const textToShow = (() => {
    if (isToggle) return isChecked ? toggleOnText : toggleOffText;
    if (isCycler) return selectedOption || '';
    return text;
  })();

  const handleClick = () => {
    if (isCycler) {
      onCyclerClick();
    } else {
      onClick();
    }
  };

  return (
    <button className="az-nav-rail-button" onClick={handleClick} style={{ borderColor: color || 'blue' }}>
      <span className="button-text" ref={textRef}>{textToShow}</span>
    </button>
  );
};

export default AzNavRailButton;