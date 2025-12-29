import React from 'react';
import './MenuItem.css';

/**
 * A menu item component for the expanded navigation rail.
 */
const MenuItem = ({ item, depth = 0, onToggle, onCyclerClick, isHost, isExpanded, onHostClick, infoScreen }) => {
  const {
    text,
    isToggle,
    isChecked,
    toggleOnText,
    toggleOffText,
    isCycler,
    selectedOption,
    onClick,
    isDivider,
    color = 'currentColor'
  } = item;

  if (isDivider) {
    return <div className="az-menu-divider" style={{ backgroundColor: color, opacity: 0.2 }} />;
  }

  const handleClick = () => {
    if (infoScreen) {
        if (isHost) {
            onHostClick();
        }
        return;
    }

    if (isHost) {
        onHostClick();
    } else if (isToggle) {
        onClick && onClick();
    } else if (isCycler) {
        onCyclerClick();
    } else {
        onClick && onClick();
        onToggle(); // Collapse menu
    }
  };

  const paddingLeft = 16 + (depth * 16);

  return (
    <div
        className="az-menu-item"
        style={{ color: color, paddingLeft: `${paddingLeft}px`, position: 'relative' }}
        onClick={handleClick}
        data-az-nav-id={item.id}
    >
        {isToggle ? (
            <div className="az-menu-item-content toggle">
                 <span className="az-menu-item-text">
                     {isChecked ? toggleOnText : toggleOffText}
                 </span>
            </div>
        ) : isCycler ? (
             <div className="az-menu-item-content cycler">
                 <span className="az-menu-item-text">{text}</span>
                 <span className="az-menu-item-value">{selectedOption}</span>
             </div>
        ) : (
            <div className="az-menu-item-content button">
                <span className="az-menu-item-text">{text}</span>
                {isHost && (
                    <span className="az-menu-item-arrow">
                        {isExpanded ? '▼' : '▶'}
                    </span>
                )}
            </div>
        )}
    </div>
  );
};

export default MenuItem;
