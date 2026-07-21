import React, { useState, useEffect, useRef } from 'react';
import { AzButton } from './AzButton';
import { AzButtonShape } from '../types';
import { ViewStyle } from 'react-native';

/** Props for the standalone `AzCycler` button used inside the rail to step through a list of options. */
export interface AzCyclerProps {
  /** Ordered list of option labels. */
  options: string[];
  /** Currently selected option (must be a member of `options`). */
  selectedOption: string;
  /**
   * Called with the next option after a one-second debounce, so the user can rapidly
   * tap-preview values without firing a callback for each tap.
   */
  onCycle: (option: string) => void;
  /** Border and text color. */
  color?: string;
  /** Background fill color inside the button shape. */
  fillColor?: string;
  /** Button shape. */
  shape?: AzButtonShape;
  /** Optional style merged into the container. */
  style?: ViewStyle;
  /** When true, the button is rendered at 50% opacity and is non-interactive. */
  disabled?: boolean;
  /** Subset of `options` that should be skipped while cycling. */
  disabledOptions?: string[];
  /** Test identifier forwarded to the underlying touchable. */
  testID?: string;
  /** Badge text to display. */
  badge?: string;
  /** Whether the badge is persistent. */
  persistentBadge?: boolean;
  /** Size of the button. */
  size?: number;
}

/**
 * Button that cycles forward through `options` on each tap with a 1 s commit debounce
 * before `onCycle` is invoked with the final selected value.
 */
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
  badge,
  persistentBadge,
  size,
}) => {
  // Local state to show the currently "previewed" option
  const [displayOption, setDisplayOption] = useState(selectedOption);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
      badge={badge}
      persistentBadge={persistentBadge}
      size={size}
    />
  );
};
