import React from 'react';
import './MenuItem.css';

/**
 * A single item in the expanded navigation menu.
 *
 * This component handles the rendering and interaction for all types of menu items,
 * including standard, toggle, and cycler items. It supports multi-line text with
 * indentation for all lines after the first.
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
 * @param {function} props.onToggle - The function to collapse the navigation rail.
 * @param {function} props.onCyclerClick - The specialized click handler for cycler items.
 */
const MenuItem = ({ item, onToggle, onCyclerClick }) => {
  const { text, isToggle, isChecked, toggleOnText, toggleOffText, isCycler, selectedOption, onClick } = item;

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
      onToggle(); // Collapse the menu on click for non-cycler items
    }
  };

  const lines = textToShow.split('\n');

  return (
    <div className="menu-item" onClick={handleClick}>
      {lines.map((line, index) => (
        <span key={index} className={index > 0 ? 'indented' : ''}>
          {line}
        </span>
      ))}
    </div>
  );
};

export default MenuItem;