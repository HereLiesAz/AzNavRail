import React from 'react';
import useFitText from '../hooks/useFitText';
import './AzNavRailButton.css';

/**
 * A circular button for the collapsed navigation rail.
 */
const AzNavRailButton = ({ item, onCyclerClick, onClickOverride, infoScreen, style }) => {
  const {
      text, isToggle, isChecked, toggleOnText, toggleOffText,
      isCycler, selectedOption, onClick, onFocus,
      color, id, disabled, content
  } = item;

  const fitTextRef = useFitText();
  const textRef = fitTextRef; // Always fit text in rail button

  const textToShow = (() => {
    if (isToggle) return isChecked ? toggleOnText : toggleOffText;
    if (isCycler) return selectedOption || '';
    return text;
  })();

  const isInteractive = infoScreen ? !!onClickOverride : !disabled;

  const handleClick = (e) => {
    if (!isInteractive) return;

    if (infoScreen) {
        if (onClickOverride) {
            onClickOverride(e); // Allow host expansion
        }
        return;
    }

    // Always trigger onFocus if present (parity with Android)
    if (onFocus) {
        onFocus();
    }

    if (onClickOverride) {
        onClickOverride(e);
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
                cursor: isInteractive ? 'pointer' : 'default',
                ...style
            }}
            disabled={!isInteractive}
        >
          {content ? (
              <div className="button-content-wrapper" style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {typeof content === 'string' && (content.startsWith('http') || content.startsWith('/') || content.startsWith('data:')) ? (
                      <img src={content} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: 'inherit' }} />
                  ) : (
                      <div style={{ backgroundColor: content, width: '100%', height: '100%', borderRadius: 'inherit' }} />
                  )}
              </div>
          ) : (
              <span className="button-text" ref={textRef}>{textToShow}</span>
          )}
        </button>
    </div>
  );
};

export default AzNavRailButton;
