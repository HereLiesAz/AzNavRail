import React from 'react';
import { AzButton } from './AzButton';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';

/** Props for the native `AzToggle` two-state button primitive. */
interface AzToggleProps {
  /** Current checked/on state. */
  isChecked: boolean;
  /** Called when the button is tapped to flip the state. */
  onToggle: () => void;
  /** Label shown when `isChecked` is true. */
  toggleOnText: string;
  /** Label shown when `isChecked` is false. */
  toggleOffText: string;
  /** Border and text color. */
  color?: string;
  /** Background fill color. */
  fillColor?: string;
  /** Button shape. */
  shape?: AzButtonShape;
  /** Additional container style. */
  style?: ViewStyle;
  /** When true, all taps are ignored. */
  disabled?: boolean;
  /** Test identifier. */
  testID?: string;
}

/** Native implementation: Two-state toggle button that swaps its label based on `isChecked`. */
export const AzToggle: React.FC<AzToggleProps> = ({
  isChecked,
  onToggle,
  toggleOnText,
  toggleOffText,
  color,
  fillColor,
  shape,
  style,
  disabled,
  testID,
}) => {
  return (
    <AzButton
      text={isChecked ? toggleOnText : toggleOffText}
      onClick={onToggle}
      color={color}
      fillColor={fillColor}
      shape={shape}
      style={style}
      enabled={!disabled}
      testID={testID}
    />
  );
};
