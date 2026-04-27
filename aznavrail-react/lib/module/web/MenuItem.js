function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import React from 'react';
import './MenuItem.css';

/**
 * A menu item component for the expanded navigation rail.
 */
const MenuItem = ({
  item,
  depth = 0,
  onToggle,
  onCyclerClick,
  isHost,
  isExpanded,
  onHostClick,
  infoScreen
}) => {
  const {
    text,
    isToggle,
    isChecked,
    toggleOnText,
    toggleOffText,
    isCycler,
    selectedOption,
    menuText,
    menuToggleOnText,
    menuToggleOffText,
    menuOptions,
    textColor,
    fillColor,
    onClick,
    isDivider,
    color = 'currentColor',
    onFocus
  } = item;
  if (isDivider) {
    return /*#__PURE__*/React.createElement("div", {
      className: "az-menu-divider",
      style: {
        backgroundColor: color,
        opacity: 0.2
      }
    });
  }
  const isInteractive = infoScreen ? isHost || item.isHelpItem : !item.disabled;
  const handleClick = () => {
    if (!isInteractive) return;
    if (infoScreen) {
      if (item.isHelpItem && onClick) {
        onClick();
      } else if (isHost) {
        onHostClick();
      }
      return;
    }
    if (onFocus) onFocus();
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
  const paddingLeft = 16 + depth * 16;
  const ariaProps = {};
  if (isToggle) {
    ariaProps['aria-checked'] = isChecked;
    ariaProps.role = 'switch';
  } else if (isHost) {
    ariaProps['aria-expanded'] = isExpanded;
  } else if (isCycler) {
    ariaProps['aria-label'] = `${text} ${selectedOption}`;
  } else if (item.isNestedRail) {
    ariaProps['aria-expanded'] = isExpanded;
    ariaProps['aria-haspopup'] = 'true';
  }
  return /*#__PURE__*/React.createElement("div", _extends({
    className: "az-menu-item",
    style: {
      color: color,
      paddingLeft: `${paddingLeft}px`,
      position: 'relative'
    },
    onClick: handleClick,
    "data-az-nav-id": item.id
  }, ariaProps), isToggle ? /*#__PURE__*/React.createElement("div", {
    className: "az-menu-item-content toggle"
  }, /*#__PURE__*/React.createElement("span", {
    className: "az-menu-item-text"
  }, isChecked ? toggleOnText : toggleOffText)) : isCycler ? /*#__PURE__*/React.createElement("div", {
    className: "az-menu-item-content cycler"
  }, /*#__PURE__*/React.createElement("span", {
    className: "az-menu-item-text"
  }, text), /*#__PURE__*/React.createElement("span", {
    className: "az-menu-item-value"
  }, selectedOption)) : /*#__PURE__*/React.createElement("div", {
    className: "az-menu-item-content button"
  }, /*#__PURE__*/React.createElement("span", {
    className: "az-menu-item-text"
  }, text), isHost && /*#__PURE__*/React.createElement("span", {
    className: "az-menu-item-arrow"
  }, isExpanded ? '▼' : '▶')));
};
export default MenuItem;
//# sourceMappingURL=MenuItem.js.map