import React, { useState } from 'react';
import { Text, TouchableOpacity, View } from 'react-native';
import { AzNavItem, AzSliderOrientation, AzSliderVariant } from '../types';
import { AzSlider } from './AzSlider';
import { AzButton } from './AzButton';

/** How much taller than a rail button an unfolded slider stands. */
const UNFOLDED_LENGTH_MULTIPLIER = 3;

export interface AzRailSliderItemProps {
  item: AzNavItem;
  buttonSize: number;
  enabled?: boolean;
  color: string;
}

/**
 * A rail item that unfolds into a slider **in its own slot**.
 *
 * Collapsed it is an ordinary rail button carrying the item's label. Tapped, the slot grows along
 * the rail and the button becomes the track, with the value underneath; tapping the value folds it
 * back. Nothing opens over the rail, so the control arrives where the user was already looking —
 * which is the whole point of putting it in the rail rather than in a dialog.
 */
export const AzRailSliderItem: React.FC<AzRailSliderItemProps> = ({
  item,
  buttonSize,
  enabled = true,
  color,
}) => {
  const [unfolded, setUnfolded] = useState(false);

  // The rail is a vertical strip, so a rail slider runs vertically no matter how the item was
  // configured. Everything else the host asked for is honoured.
  const config = {
    ...(item.sliderConfig ?? {}),
    orientation: AzSliderOrientation.VERTICAL,
  };
  const value = item.sliderValue ?? 0;
  const range = item.sliderRange ?? [0, 1];
  const label = item.sliderValueFormatter
    ? item.sliderValueFormatter(value)
    : defaultFormat(item, config, value, range);

  if (!unfolded) {
    return (
      <AzButton
        text={item.text}
        onClick={() => enabled && setUnfolded(true)}
        color={color}
        textColor={item.textColor}
        fillColor={item.fillColor}
        translucentBackgroundColor={item.translucentBackgroundColor}
        shape={item.shape}
        size={buttonSize}
        enabled={enabled}
        testID={item.id}
      />
    );
  }

  return (
    <View style={{ alignItems: 'center' }}>
      <AzSlider
        value={value}
        onValueChange={item.onSliderChange}
        rangeValue={range}
        onRangeChange={item.onSliderRangeChange}
        config={config}
        color={color}
        enabled={enabled}
        length={buttonSize * UNFOLDED_LENGTH_MULTIPLIER}
        label={item.text}
        testID={item.id}
      />
      {/* The value doubles as the way back. A separate close affordance would be a second control
          for a job this one is already doing. */}
      <TouchableOpacity
        onPress={() => setUnfolded(false)}
        accessibilityRole="button"
        accessibilityLabel={`${item.text} ${label}`}
        style={{ width: buttonSize, paddingVertical: 4 }}
      >
        <Text
          style={{
            color,
            fontSize: 12,
            fontWeight: 'bold',
            textAlign: 'center',
          }}
        >
          {label}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

/**
 * The label under an unfolded track when the host supplied no formatter.
 *
 * A stepped slider prints its step index, because on a quantised track "3/5" is what the user is
 * actually choosing; everything else prints two decimals, and a range prints both ends.
 */
function defaultFormat(
  _item: AzNavItem,
  config: {
    variant?: AzSliderVariant;
    steps?: number;
    valueFrom?: number;
    valueTo?: number;
  },
  value: number,
  range: [number, number]
): string {
  const from = config.valueFrom ?? 0;
  const to = config.valueTo ?? 1;
  if (config.variant === AzSliderVariant.RANGE) {
    return `${twoDp(range[0])}–${twoDp(range[1])}`;
  }
  if (config.variant === AzSliderVariant.STEPPED && config.steps) {
    const slots = config.steps + 1;
    const span = to - from || 1;
    return `${Math.round(((value - from) / span) * slots)}/${slots}`;
  }
  return twoDp(value);
}

function twoDp(v: number): string {
  return String(Math.round(v * 100) / 100);
}
