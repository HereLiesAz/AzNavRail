import React from 'react';
import './MenuItem.css';

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
      onToggle();
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