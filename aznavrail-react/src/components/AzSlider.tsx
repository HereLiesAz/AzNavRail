import React, { useMemo, useRef, useState } from 'react';
import {
  LayoutChangeEvent,
  PanResponder,
  StyleSheet,
  View,
  ViewStyle,
} from 'react-native';
import {
  AzSliderConfig,
  AzSliderOrientation,
  AzSliderSize,
  AzSliderSizeMetrics,
  AzSliderVariant,
} from '../types';

/** The accessible name an `AzSlider` answers to when the caller supplies none. */
export const AzSliderAccessibilityLabel = 'slider';

/** Test identifier on the slider's track. */
export const AzSliderTestID = 'az-slider';

/** Props for {@link AzSlider}. */
export interface AzSliderProps {
  /** The current value, for every variant except `RANGE`. */
  value?: number;
  /** Called continuously as the thumb moves. */
  onValueChange?: (value: number) => void;
  /** The two ends, for the `RANGE` variant only. */
  rangeValue?: [number, number];
  /** Called continuously as either range thumb moves. */
  onRangeChange?: (range: [number, number]) => void;
  config?: Partial<AzSliderConfig>;
  /** Active-track and thumb colour. */
  color?: string;
  /** Inactive-track colour. Defaults to a faded [color]. */
  trackColor?: string;
  enabled?: boolean;
  /** Length along the slider's own axis. Omit to fill the space it is given. */
  length?: number;
  /** An accessible name. Falls back to {@link AzSliderAccessibilityLabel}. */
  label?: string;
  testID?: string;
  style?: ViewStyle;
}

const DEFAULT_CONFIG: AzSliderConfig = {
  variant: AzSliderVariant.CONTINUOUS,
  size: AzSliderSize.SMALL,
  orientation: AzSliderOrientation.VERTICAL,
  valueFrom: 0,
  valueTo: 1,
  steps: 0,
};

/**
 * The rail's slider, drawn on Material 3 Expressive lines.
 *
 * One instrument in four shapes — continuous, stepped, centred and range (see
 * {@link AzSliderVariant}) — because they differ only in where the active track begins and how many
 * thumbs ride it. Everything else is shared: a thick track with fully-rounded ends, an inset gap
 * where the thumb sits so the thumb reads as *on* the track rather than *in* it, a pill thumb
 * standing across the track, and stop indicators on the inactive track wherever the value is
 * quantised.
 *
 * It is composed from plain views rather than a platform slider so it renders identically on
 * native and web, and so it can stand in a rail slot 100dp wide running vertically.
 */
export const AzSlider: React.FC<AzSliderProps> = ({
  value = 0,
  onValueChange,
  rangeValue = [0, 1],
  onRangeChange,
  config: configProp,
  color = '#6200ee',
  trackColor,
  enabled = true,
  length,
  label,
  testID = AzSliderTestID,
  style,
}) => {
  const config: AzSliderConfig = { ...DEFAULT_CONFIG, ...configProp };
  const metrics = AzSliderSizeMetrics[config.size];
  const isVertical = config.orientation === AzSliderOrientation.VERTICAL;
  const span = config.valueTo - config.valueFrom || 1;

  const [measured, setMeasured] = useState({ width: 0, height: 0 });
  // Which thumb a range drag has hold of. Decided once on touch-down by proximity and kept for the
  // whole gesture — recomputing per move lets the thumbs swap under the finger mid-drag.
  const draggingUpper = useRef(false);
  const latest = useRef({ value, rangeValue, config, measured });
  latest.current = { value, rangeValue, config, measured };

  const fractionOf = (v: number) =>
    Math.min(1, Math.max(0, (v - config.valueFrom) / span));

  const quantise = (fraction: number) => {
    const c = latest.current.config;
    const s = c.valueTo - c.valueFrom || 1;
    const raw = c.valueFrom + Math.min(1, Math.max(0, fraction)) * s;
    if (c.variant !== AzSliderVariant.STEPPED || !c.steps) return raw;
    const slots = c.steps + 1;
    const stepSize = s / slots;
    return c.valueFrom + Math.round((raw - c.valueFrom) / stepSize) * stepSize;
  };

  // A vertical slider runs bottom-up: down is less, the direction every physical fader moves.
  const fractionAt = (x: number, y: number) => {
    const m = latest.current.measured;
    return isVertical
      ? 1 - y / Math.max(1, m.height)
      : x / Math.max(1, m.width);
  };

  const emit = (fraction: number) => {
    const next = quantise(fraction);
    const c = latest.current.config;
    if (c.variant === AzSliderVariant.RANGE) {
      const [lo, hi] = latest.current.rangeValue;
      onRangeChange?.(
        draggingUpper.current
          ? [lo, Math.max(lo, next)]
          : [Math.min(hi, next), hi]
      );
    } else {
      onValueChange?.(Math.min(c.valueTo, Math.max(c.valueFrom, next)));
    }
  };

  const responder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => enabled,
        onMoveShouldSetPanResponder: () => enabled,
        onPanResponderGrant: (e) => {
          const { locationX, locationY } = e.nativeEvent;
          const f = fractionAt(locationX, locationY);
          const c = latest.current.config;
          if (c.variant === AzSliderVariant.RANGE) {
            const [lo, hi] = latest.current.rangeValue;
            draggingUpper.current =
              Math.abs(f - fractionOf(hi)) < Math.abs(f - fractionOf(lo));
          }
          emit(f);
        },
        onPanResponderMove: (e) => {
          const { locationX, locationY } = e.nativeEvent;
          emit(fractionAt(locationX, locationY));
        },
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [enabled, isVertical]
  );

  const onLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    setMeasured({ width, height });
  };

  const across = metrics.trackThickness;
  const alongLength = length ?? (isVertical ? measured.height : measured.width);
  const activeColor = enabled ? color : withAlpha(color, 0.38);
  const inactiveColor = trackColor ?? withAlpha(color, enabled ? 0.24 : 0.12);

  // Along-axis offset in px for a fraction, mapped so a vertical slider's low end is at the bottom.
  const px = (fraction: number) =>
    isVertical ? alongLength * (1 - fraction) : alongLength * fraction;

  const valueFraction = fractionOf(value);
  const originFraction =
    config.variant === AzSliderVariant.CENTERED
      ? config.origin != null
        ? fractionOf(config.origin)
        : 0.5
      : 0;

  const bandFor = (a: number, b: number): ViewStyle => {
    const lo = Math.min(px(a), px(b));
    const size = Math.abs(px(a) - px(b));
    return isVertical
      ? { position: 'absolute', top: lo, height: size, left: 0, right: 0 }
      : { position: 'absolute', left: lo, width: size, top: 0, bottom: 0 };
  };

  const activeBand =
    config.variant === AzSliderVariant.RANGE
      ? bandFor(fractionOf(rangeValue[0]), fractionOf(rangeValue[1]))
      : config.variant === AzSliderVariant.CENTERED
        ? bandFor(originFraction, valueFraction)
        : bandFor(0, valueFraction);

  const thumbAt = (fraction: number) => {
    const at = Math.min(
      Math.max(px(fraction), metrics.thumbThickness),
      Math.max(alongLength - metrics.thumbThickness, metrics.thumbThickness)
    );
    return isVertical
      ? {
          position: 'absolute' as const,
          top: at - metrics.thumbThickness / 2,
          height: metrics.thumbThickness,
          width: metrics.thumbLength,
          left: across / 2 - metrics.thumbLength / 2,
        }
      : {
          position: 'absolute' as const,
          left: at - metrics.thumbThickness / 2,
          width: metrics.thumbThickness,
          height: metrics.thumbLength,
          top: across / 2 - metrics.thumbLength / 2,
        };
  };

  const stops =
    config.variant === AzSliderVariant.STEPPED && config.steps
      ? Array.from(
          { length: config.steps + 2 },
          (_, i) => i / (config.steps + 1)
        )
      : [];

  return (
    <View
      {...responder.panHandlers}
      onLayout={onLayout}
      testID={testID}
      accessible
      accessibilityRole="adjustable"
      accessibilityLabel={label ?? AzSliderAccessibilityLabel}
      accessibilityValue={{
        min: config.valueFrom,
        max: config.valueTo,
        now: value,
      }}
      style={[
        isVertical
          ? { width: across, height: length ?? '100%' }
          : { height: across, width: length ?? '100%' },
        {
          borderRadius: across / 2,
          backgroundColor: inactiveColor,
          overflow: 'hidden',
        },
        style,
      ]}
    >
      <View
        style={[
          activeBand,
          { backgroundColor: activeColor, borderRadius: across / 2 },
        ]}
      />

      {/* Stop indicators. Only a stepped track gets them, and it always does: they are the sole
          visible difference between a track that snaps and one that does not. */}
      {stops.map((f, i) => {
        const at = px(f);
        const onActive = f <= valueFraction;
        const dot = Math.max(2, Math.min(4, across * 0.08));
        return (
          <View
            key={`stop-${i}`}
            pointerEvents="none"
            style={{
              position: 'absolute',
              width: dot,
              height: dot,
              borderRadius: dot / 2,
              opacity: 0.55,
              backgroundColor: onActive ? inactiveColor : activeColor,
              ...(isVertical
                ? { top: at - dot / 2, left: across / 2 - dot / 2 }
                : { left: at - dot / 2, top: across / 2 - dot / 2 }),
            }}
          />
        );
      })}

      {(config.variant === AzSliderVariant.RANGE
        ? [fractionOf(rangeValue[0]), fractionOf(rangeValue[1])]
        : [valueFraction]
      ).map((f, i) => (
        <View
          key={`thumb-${i}`}
          pointerEvents="none"
          style={[
            thumbAt(f),
            {
              backgroundColor: activeColor,
              borderRadius: metrics.thumbThickness / 2,
            },
          ]}
        />
      ))}
    </View>
  );
};

/** Fades a hex or rgb colour. Falls back to the colour untouched when it cannot be parsed. */
function withAlpha(color: string, alpha: number): string {
  const hex = /^#([0-9a-f]{6})$/i.exec(color);
  if (hex) {
    const n = parseInt(hex[1], 16);
    // eslint-disable-next-line no-bitwise
    return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
  }
  return color;
}

export const azSliderStyles = StyleSheet.create({});
