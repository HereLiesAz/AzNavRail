import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { render, fireEvent } from '@testing-library/react-native';
import type { TestInstance } from 'test-renderer';
import { AzButton } from '../components/AzButton';
import { AzButtonShape } from '../types';
import { AzLoadTestID } from '../components/AzLoad';
import { AzFillContentGraphicTestID } from '../components/fillContent';

// The pressable carries the role and the accessibility state; the container View around it carries
// the shape (border, radius, size). Reading the shape off the pressable finds nothing — these tests
// used to do exactly that, against a hand-rolled `react-native` mock that flattened the two into
// one node. They now run against the real RN component tree.
const shapeOf = (button: TestInstance) => StyleSheet.flatten(button.parent!.props.style) as any;

describe('AzButton', () => {
  const defaultProps = {
    text: 'Test Button',
    onClick: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders correctly with default props', async () => {
    const { getByText } = await render(<AzButton {...defaultProps} />);
    expect(getByText('Test Button')).toBeTruthy();
  });

  it('calls onClick when pressed and enabled', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} />);
    await fireEvent.press(getByRole('button'));
    expect(defaultProps.onClick).toHaveBeenCalledTimes(1);
  });

  it('does not call onClick when disabled', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} enabled={false} />);
    const button = getByRole('button');
    expect(button.props.accessibilityState.disabled).toBe(true);
    await fireEvent.press(button);
    expect(defaultProps.onClick).not.toHaveBeenCalled();
  });

  it('does not call onClick when isLoading is true', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} isLoading={true} />);
    const button = getByRole('button');
    expect(button.props.accessibilityState.disabled).toBe(true);
    await fireEvent.press(button);
    expect(defaultProps.onClick).not.toHaveBeenCalled();
  });

  it('renders correctly as a CIRCLE', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} shape={AzButtonShape.CIRCLE} />);
    const shape = shapeOf(getByRole('button'));
    expect(shape.borderRadius).toBe(36);
    expect(shape.width).toBe(72);
    expect(shape.height).toBe(72);
  });

  it('renders correctly as a SQUARE', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} shape={AzButtonShape.SQUARE} />);
    const shape = shapeOf(getByRole('button'));
    expect(shape.borderRadius).toBe(0);
    expect(shape.width).toBe(72);
    expect(shape.height).toBe(72);
  });

  it('renders correctly as a RECTANGLE', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} shape={AzButtonShape.RECTANGLE} />);
    const shape = shapeOf(getByRole('button'));
    expect(shape.borderRadius).toBe(0);
    expect(shape.height).toBe(40);
  });

  it('renders correctly as NONE', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} shape={AzButtonShape.NONE} />);
    const shape = shapeOf(getByRole('button'));
    expect(shape.borderColor).toBe('transparent');
    expect(shape.borderWidth).toBe(0);
    expect(shape.height).toBe(40);
  });

  it('renders with custom color', async () => {
    const color = '#FF0000';
    const { getByRole, getByText } = await render(<AzButton {...defaultProps} color={color} />);

    expect(shapeOf(getByRole('button')).borderColor).toBe(color);
    expect(StyleSheet.flatten(getByText('Test Button').props.style).color).toBe(color);
  });

  it('handles enabled=false state', async () => {
    const { getByRole } = await render(<AzButton {...defaultProps} enabled={false} />);
    const button = getByRole('button');

    expect(button.props.accessibilityState.disabled).toBe(true);
    expect(shapeOf(button).opacity).toBe(0.5);
  });

  it('handles isLoading=true state', async () => {
    const { getByRole, queryByText, getByTestId } = await render(<AzButton {...defaultProps} isLoading={true} />);

    expect(getByRole('button').props.accessibilityState.disabled).toBe(true);

    // Verify loader is present
    expect(getByTestId(AzLoadTestID)).toBeTruthy();

    // Verify text is not present
    expect(queryByText('Test Button')).toBeNull();
  });

  it('handles text with newlines correctly', async () => {
    const textWithNewline = 'Line 1\nLine 2';
    const { getByText } = await render(<AzButton {...defaultProps} text={textWithNewline} />);
    const textNode = getByText(textWithNewline);

    expect(textNode.props.numberOfLines).toBeUndefined();
    expect(textNode.props.adjustsFontSizeToFit).toBe(false);
  });

  it('handles text without newlines correctly', async () => {
    const { getByText } = await render(<AzButton {...defaultProps} text="No Newline" />);
    const textNode = getByText('No Newline');

    expect(textNode.props.numberOfLines).toBe(1);
    expect(textNode.props.adjustsFontSizeToFit).toBe(true);
  });

  describe('custom content fills the shape', () => {
    it('renders an image source as a filling cover Image', async () => {
      const { getByTestId, queryByText } = await render(
        <AzButton {...defaultProps} hasCustomContent content={{ uri: 'https://example.com/x.png' }} />
      );
      const image = getByTestId(AzFillContentGraphicTestID);
      expect(image.props.resizeMode).toBe('cover');
      const style = StyleSheet.flatten(image.props.style) as any;
      expect(style.width).toBe('100%');
      expect(style.height).toBe('100%');
      // Text label is replaced by the graphic.
      expect(queryByText('Test Button')).toBeNull();
    });

    it('clones an <Image> element to fill and cover', async () => {
      const { getByTestId } = await render(
        <AzButton {...defaultProps} hasCustomContent content={<Image source={{ uri: 'https://example.com/y.png' }} />} />
      );
      const image = getByTestId(AzFillContentGraphicTestID);
      expect(image.props.resizeMode).toBe('cover');
      const style = StyleSheet.flatten(image.props.style) as any;
      expect(style.width).toBe('100%');
      expect(style.height).toBe('100%');
    });

    it('stretches a generic vector element (e.g. an <Svg>) to fill', async () => {
      // A stand-in for a react-native-svg <Svg> element: any element gets fill sizing merged.
      const { getByTestId } = await render(
        <AzButton {...defaultProps} hasCustomContent content={<View testID="svg" />} />
      );
      const style = StyleSheet.flatten(getByTestId('svg').props.style) as any;
      expect(style.width).toBe('100%');
      expect(style.height).toBe('100%');
    });
  });

  // Folded in from a second, near-identical AzButton suite that lived at the repo root and
  // duplicated every case above. These two were the only ones it covered on its own.
  it('renders correctly with custom style', async () => {
    const { getByRole } = await render(
      <AzButton {...defaultProps} style={{ margin: 10 }} shape={AzButtonShape.NONE} />
    );
    expect(shapeOf(getByRole('button')).margin).toBe(10);
  });

  it('renders gracefully when an unknown shape is provided', async () => {
    // @ts-expect-error: intentionally passing an invalid shape to exercise the fallback branch
    const { getByRole } = await render(<AzButton {...defaultProps} shape={'UNKNOWN_SHAPE'} />);
    // No shape branch matched, so the shape logic contributes no height.
    expect(shapeOf(getByRole('button')).height).toBeUndefined();
  });
});
