import React from 'react';
import useFitText from '../hooks/useFitText';
import './AzNavRailButton.css';

/**
 * A circular button for the collapsed navigation rail.
 */
const AzNavRailButton = ({ item, onCyclerClick, onClickOverride, infoScreen }) => {
  const { text, isToggle, isChecked, toggleOnText, toggleOffText, isCycler, selectedOption, onClick, color, id, disabled } = item;
  const fitTextRef = useFitText();
  const textRef = fitTextRef; // Always fit text in rail button

  const textToShow = (() => {
    if (isToggle) return isChecked ? toggleOnText : toggleOffText;
    if (isCycler) return selectedOption || '';
    return text;
  })();

  const isInteractive = infoScreen ? !!onClickOverride : !disabled;
  // If infoScreen is active, only items with onClickOverride (hosts) are interactive.

  const handleClick = () => {
    if (!isInteractive) return;

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
    <div style={{ position: 'relative' }} data-az-nav-id={id}>
        <button
            className={`az-nav-rail-button ${!isInteractive ? 'disabled' : ''}`}
            onClick={handleClick}
            style={{
                borderColor: color || 'blue',
                opacity: isInteractive ? 1 : 0.5,
                cursor: isInteractive ? 'pointer' : 'default'
            }}
            disabled={!isInteractive}
        >
          <span className="button-text" ref={textRef}>{textToShow}</span>
        </button>
        {/* Removed inline info popup, now handled by HelpOverlay */}
    </div>
  );
};

export default AzNavRailButton;
