import React from 'react';
import useFitText from '../hooks/useFitText';
import './AzNavRailButton.css';

/**
 * A circular button for the collapsed navigation rail.
 */
const AzNavRailButton = ({ item, onCyclerClick, onClickOverride, infoScreen }) => {
  const { text, isToggle, isChecked, toggleOnText, toggleOffText, isCycler, selectedOption, onClick, color, info } = item;
  const fitTextRef = useFitText();
  const textRef = fitTextRef; // Always fit text in rail button

  const textToShow = (() => {
    if (isToggle) return isChecked ? toggleOnText : toggleOffText;
    if (isCycler) return selectedOption || '';
    return text;
  })();

  const handleClick = () => {
    if (infoScreen) {
        if (onClickOverride) {
            onClickOverride(); // Allow host expansion
        }
        return;
    }

    if (onClickOverride) {
        onClickOverride();
    } else if (isCycler) {
      onCyclerClick();
    } else {
      onClick && onClick();
    }
  };

  return (
    <div style={{ position: 'relative' }}>
        <button className="az-nav-rail-button" onClick={handleClick} style={{ borderColor: color || 'blue' }}>
          <span className="button-text" ref={textRef}>{textToShow}</span>
        </button>
        {infoScreen && info && (
            <div style={{
                position: 'absolute',
                left: '100%',
                top: '50%',
                transform: 'translateY(-50%)',
                marginLeft: '8px',
                padding: '4px 8px',
                backgroundColor: '#333',
                color: 'white',
                borderRadius: '4px',
                fontSize: '12px',
                whiteSpace: 'nowrap',
                zIndex: 100
            }}>
                {info}
            </div>
        )}
    </div>
  );
};

export default AzNavRailButton;
