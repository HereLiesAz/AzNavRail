import React from 'react';
import { AzButton } from './AzButton';

/** Props for the standalone `AzToggle` button used inside the rail to render a two-state item. */

/** Two-state toggle button — internally an `AzButton` whose label is driven by `isChecked`. */
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