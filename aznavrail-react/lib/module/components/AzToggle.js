import React from 'react';
import { AzButton } from './AzButton';
export const AzToggle = ({
  isChecked,
  onToggle,
  toggleOnText,
  toggleOffText,
  color,
  shape,
  style,
  disabled,
  testID
}) => {
  return /*#__PURE__*/React.createElement(AzButton, {
    text: isChecked ? toggleOnText : toggleOffText,
    onClick: onToggle,
    color: color,
    shape: shape,
    style: style,
    disabled: disabled,
    testID: testID
  });
};
//# sourceMappingURL=AzToggle.js.map