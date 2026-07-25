import React from 'react';
import { render } from '@testing-library/react-native';
import { AzSlider, AzSliderAccessibilityLabel } from '../components/AzSlider';
import { AzRailSliderItem } from '../components/AzRailSliderItem';
import { AzMotion } from '../AzNavRailDefaults';
import {
  AzNavItem,
  AzSliderSize,
  AzSliderSizeMetrics,
  AzSliderVariant,
} from '../types';

describe('AzSlider', () => {
  it('is findable by its default accessible name', async () => {
    const { getByLabelText } = await render(<AzSlider length={200} />);
    // The track is composed of unlabelled views, so its accessible name is the only handle
    // assistive tech (and this assertion) has on it.
    expect(getByLabelText(AzSliderAccessibilityLabel)).toBeTruthy();
  });

  it('takes the caller’s label when given one', async () => {
    const { getByLabelText } = await render(
      <AzSlider length={200} label="Volume" />
    );
    expect(getByLabelText('Volume')).toBeTruthy();
  });

  it('publishes its value range to assistive tech', async () => {
    const { getByLabelText } = await render(
      <AzSlider
        length={200}
        label="Trim"
        value={0.25}
        config={{ valueFrom: -1, valueTo: 1 }}
      />
    );
    const node = getByLabelText('Trim');
    expect(node.props.accessibilityRole).toBe('adjustable');
    expect(node.props.accessibilityValue).toEqual({
      min: -1,
      max: 1,
      now: 0.25,
    });
  });

  it('renders every variant without throwing', async () => {
    // One instrument in four shapes. A variant that fails to render would otherwise only surface on
    // whichever screen happened to use it.
    for (const variant of Object.values(AzSliderVariant)) {
      const { getByLabelText } = await render(
        <AzSlider
          length={200}
          label={variant}
          value={0.4}
          rangeValue={[0.2, 0.8]}
          config={{ variant, steps: 4 }}
        />
      );
      expect(getByLabelText(variant)).toBeTruthy();
    }
  });
});

describe('AzSliderSizeMetrics', () => {
  it('thickens the track faster than it grows the thumb', () => {
    // M3 Expressive's ladder is not a uniform scale-up: a large slider should read as a substantial
    // control rather than a magnified small one.
    const sizes = Object.values(AzSliderSize);
    const ratio = (s: AzSliderSize) =>
      AzSliderSizeMetrics[s].trackThickness /
      AzSliderSizeMetrics[s].thumbThickness;
    expect(ratio(sizes[0])).toBeLessThan(ratio(sizes[sizes.length - 1]));
    sizes.slice(1).forEach((s, i) => {
      expect(AzSliderSizeMetrics[s].trackThickness).toBeGreaterThan(
        AzSliderSizeMetrics[sizes[i]].trackThickness
      );
    });
  });
});

describe('AzRailSliderItem', () => {
  const item = {
    id: 'vol',
    text: 'Vol',
    isSlider: true,
    sliderValue: 0.5,
  } as AzNavItem;

  it('starts folded as an ordinary rail button', async () => {
    // The point of a rail slider is that it arrives where the user is already looking: folded it is
    // indistinguishable from its neighbours. Both states answer to the item's label, so the role is
    // what separates them — `button` folded, `adjustable` once the track is out.
    const { getByText, getByLabelText } = await render(
      <AzRailSliderItem item={item} buttonSize={72} color="#6200ee" />
    );
    expect(getByText('Vol')).toBeTruthy();
    expect(getByLabelText('Vol').props.accessibilityRole).toBe('button');
  });
});

describe('AzMotion', () => {
  // The TypeScript twin of Kotlin's AzMotionTest. These are not aesthetic assertions: the values
  // these replaced meant an eight-item drawer took 1.2 seconds to finish arriving, and because the
  // turnstile entrance starts each item edge-on, the panel sat there empty for the first stretch.
  it('settles a twelve-item cascade within budget', () => {
    const settleMs = AzMotion.ItemStaggerMs * 12 + AzMotion.ItemDurationMs;
    expect(settleMs).toBeLessThanOrEqual(650);
  });

  it('keeps the stagger much shorter than the item it staggers', () => {
    expect(AzMotion.ItemStaggerMs * 4).toBeLessThanOrEqual(
      AzMotion.ItemDurationMs
    );
  });

  it('never lets a panel outlast its own contents', () => {
    expect(AzMotion.PanelDurationMs).toBeLessThanOrEqual(
      AzMotion.ItemDurationMs
    );
  });
});
