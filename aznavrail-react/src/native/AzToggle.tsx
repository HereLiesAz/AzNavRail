import React from 'react';
import { AzButton } from './AzButton';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';

interface AzToggleProps {
  isChecked: boolean;
  onToggle: () => void;
  toggleOnText: string;
  toggleOffText: string;
  color?: string;
  fillColor?: string;
  shape?: AzButtonShape;
  style?: ViewStyle;
  disabled?: boolean;
  testID?: string;
}

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
