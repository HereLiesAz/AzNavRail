import React, { useState, useEffect, useRef } from 'react';
import { AzButton } from './AzButton';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';

/** Props for the native `AzCycler` button that steps through a list of options. */
interface AzCyclerProps {
  /** Ordered list of option labels to cycle through. */
  options: string[];
  /** The currently selected option label (controlled). */
  selectedOption: string;
  /** Called with the newly committed option after a debounce delay following the last tap. */
  onCycle: (option: string) => void;
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
  /** Options that are skipped when cycling forward. */
  disabledOptions?: string[];
  /** Test identifier. */
  testID?: string;
}

/** Native implementation: Button that cycles through options on each tap and commits after a 1 s debounce. */
export const AzCycler: React.FC<AzCyclerProps> = ({
  options,
  selectedOption,
  onCycle,
  color,
  fillColor,
  shape,
  style,
  disabled,
  disabledOptions = [],
  testID,
}) => {
  // Local state to show the currently "previewed" option
  const [displayOption, setDisplayOption] = useState(selectedOption);
  const timerRef = useRef<NodeJS.Timeout | null>(null);

  // Sync local state if external selection changes (e.g. initial load or external update)
  useEffect(() => {
    setDisplayOption(selectedOption);
  }, [selectedOption]);

  const handlePress = () => {
    if (disabled) return;

    const currentIndex = options.indexOf(displayOption);
    let nextIndex = (currentIndex + 1) % options.length;

    let nextOption = options[nextIndex];

    // Skip disabled options if any
    // Guard against infinite loop if all are disabled
    let loopCount = 0;
    while (disabledOptions.includes(nextOption) && loopCount < options.length) {
       nextIndex = (nextIndex + 1) % options.length;
       nextOption = options[nextIndex];
       loopCount++;
    }

    setDisplayOption(nextOption);

    // Reset timer
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }

    timerRef.current = setTimeout(() => {
      onCycle(nextOption);
    }, 1000);
  };

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
      }
    };
  }, []);

  return (
    <AzButton
      text={displayOption}
      onClick={handlePress}
      color={color}
      fillColor={fillColor}
      shape={shape}
      style={style}
      enabled={!disabled}
      testID={testID}
    />
  );
};
