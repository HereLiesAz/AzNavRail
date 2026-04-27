import React from 'react';
import { AzButton } from './AzButton';
export const AzToggle = ({
  isChecked,
  onToggle,
  toggleOnText,
  toggleOffText,
  color,
  fillColor,
  shape,
  style,
  disabled,
  testID
}) => {
  return /*#__PURE__*/React.createElement(AzButton, {
    text: isChecked ? toggleOnText : toggleOffText,
    onClick: onToggle,
    color: color,
    fillColor: fillColor,
    shape: shape,
    style: style,
    enabled: !disabled,
    testID: testID
  });
};
//# sourceMappingURL=AzToggle.js.map