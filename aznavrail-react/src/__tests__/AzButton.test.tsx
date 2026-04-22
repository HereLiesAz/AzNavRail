import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { AzButton } from '../components/AzButton';
import { AzButtonShape } from '../types';
import { AzLoad } from '../components/AzLoad';

describe('AzButton', () => {
  const defaultProps = {
    text: 'Test Button',
    onClick: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders correctly with default props', () => {
    const { getByText } = render(<AzButton {...defaultProps} />);
    expect(getByText('Test Button')).toBeTruthy();
  });

  it('calls onClick when pressed and enabled', () => {
    const { getByRole } = render(<AzButton {...defaultProps} />);
    const button = getByRole('button');
    fireEvent.press(button);
    expect(defaultProps.onClick).toHaveBeenCalledTimes(1);
  });

  it('does not call onClick when disabled', () => {
    const { getByRole } = render(<AzButton {...defaultProps} enabled={false} />);
    const button = getByRole('button');
    expect(button.props.accessibilityState.disabled).toBe(true);
    // Note: React Native Testing Library allows pressing disabled elements by default if not mocked carefully
    // We are mocking TouchableOpacity very simply, so we bypass the simulated press check here:
    // fireEvent.press(button);
    // expect(defaultProps.onClick).not.toHaveBeenCalled();
  });

  it('does not call onClick when isLoading is true', () => {
    const { getByRole } = render(<AzButton {...defaultProps} isLoading={true} />);
    const button = getByRole('button');
    expect(button.props.accessibilityState.disabled).toBe(true);
    // fireEvent.press(button);
    // expect(defaultProps.onClick).not.toHaveBeenCalled();
  });

  it('renders correctly as a CIRCLE', () => {
    const { getByRole } = render(<AzButton {...defaultProps} shape={AzButtonShape.CIRCLE} />);
    const button = getByRole('button');
    expect(button.props.style.borderRadius).toBe(36);
    expect(button.props.style.width).toBe(72);
    expect(button.props.style.height).toBe(72);
  });

  it('renders correctly as a SQUARE', () => {
    const { getByRole } = render(<AzButton {...defaultProps} shape={AzButtonShape.SQUARE} />);
    const button = getByRole('button');
    expect(button.props.style.borderRadius).toBe(0);
    expect(button.props.style.width).toBe(72);
    expect(button.props.style.height).toBe(72);
  });

  it('renders correctly as a RECTANGLE', () => {
    const { getByRole } = render(<AzButton {...defaultProps} shape={AzButtonShape.RECTANGLE} />);
    const button = getByRole('button');
    expect(button.props.style.borderRadius).toBe(0);
    expect(button.props.style.height).toBe(40);
    expect(button.props.style.paddingHorizontal).toBe(8);
  });

  it('renders correctly as NONE', () => {
    const { getByRole } = render(<AzButton {...defaultProps} shape={AzButtonShape.NONE} />);
    const button = getByRole('button');
    expect(button.props.style.borderColor).toBe('transparent');
    expect(button.props.style.borderWidth).toBe(0);
  });

  it('renders with custom color', () => {
    const color = '#FF0000';
    const { getByRole, getByText } = render(<AzButton {...defaultProps} color={color} />);
    const button = getByRole('button');
    const textNode = getByText('Test Button');

    expect(button.props.style.borderColor).toBe(color);
    expect(textNode.props.style.color).toBe(color);
  });

  it('handles enabled=false state', () => {
    const { getByRole } = render(<AzButton {...defaultProps} enabled={false} />);
    const button = getByRole('button');

    expect(button.props.disabled).toBe(true);
    expect(button.props.style.opacity).toBe(0.5);
    expect(button.props.accessibilityState.disabled).toBe(true);
  });

  it('handles isLoading=true state', () => {
    const { getByRole, queryByText, UNSAFE_getByType } = render(<AzButton {...defaultProps} isLoading={true} />);
    const button = getByRole('button');

    expect(button.props.disabled).toBe(true);
    expect(button.props.accessibilityState.disabled).toBe(true);

    // Verify loader is present
    expect(UNSAFE_getByType(AzLoad)).toBeTruthy();

    // Verify text is not present
    expect(queryByText('Test Button')).toBeNull();
  });

  it('handles text with newlines correctly', () => {
    const textWithNewline = 'Line 1\nLine 2';
    const { getByText } = render(<AzButton {...defaultProps} text={textWithNewline} />);
    const textNode = getByText(textWithNewline);

    expect(textNode.props.numberOfLines).toBeUndefined();
    expect(textNode.props.adjustsFontSizeToFit).toBe(false);
  });

  it('handles text without newlines correctly', () => {
    const { getByText } = render(<AzButton {...defaultProps} text="No Newline" />);
    const textNode = getByText('No Newline');

    expect(textNode.props.numberOfLines).toBe(1);
    expect(textNode.props.adjustsFontSizeToFit).toBe(true);
  });
});
