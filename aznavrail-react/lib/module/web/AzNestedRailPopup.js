import React from 'react';
import './AzNestedRailPopup.css';

/**
 * A popup overlay for nested navigation rails in the web library.
 */
const AzNestedRailPopup = ({
  visible,
  onDismiss,
  items,
  alignment = 'VERTICAL',
  renderItem,
  anchorPosition,
  dockingSide = 'LEFT',
  helpList = {}
}) => {
  if (!visible) return null;
  const isHorizontal = alignment === 'HORIZONTAL';
  const popupStyle = {};
  if (anchorPosition) {
    popupStyle.top = `${anchorPosition.top}px`;
    if (dockingSide === 'LEFT') {
      popupStyle.left = `${anchorPosition.left + anchorPosition.width + 8}px`;
    } else {
      popupStyle.right = `${window.innerWidth - anchorPosition.left + 8}px`;
    }
  }
  return /*#__PURE__*/React.createElement("div", {
    className: "az-nested-rail-overlay",
    onClick: onDismiss
  }, /*#__PURE__*/React.createElement("div", {
    className: `az-nested-rail-popup ${alignment.toLowerCase()}`,
    style: popupStyle,
    onClick: e => e.stopPropagation()
  }, /*#__PURE__*/React.createElement("div", {
    className: `nested-content ${isHorizontal ? 'row' : 'column'}`
  }, items.map((item, index) => renderItem(item, index)))));
};
export default AzNestedRailPopup;
//# sourceMappingURL=AzNestedRailPopup.js.map