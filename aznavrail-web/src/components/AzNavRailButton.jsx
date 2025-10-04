import React from 'react';
import useFitText from '../hooks/useFitText';
import './AzNavRailButton.css';

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