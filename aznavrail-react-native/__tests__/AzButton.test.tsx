import React from 'react';
import renderer from 'react-test-renderer';
import { AzButton } from '../src/components/AzButton';
import { AzButtonShape } from '../src/types';
import { TouchableOpacity, Text } from 'react-native';
import { AzLoad } from '../src/components/AzLoad';

describe('AzButton', () => {
  const defaultProps = {
    text: 'Test Button',
    onClick: jest.fn(),
  };

  it('renders correctly with default props', () => {
    const component = renderer.create(<AzButton {...defaultProps} />);
    const textNode = component.root.findByType(Text);
    expect(textNode.props.children).toBe('Test Button');
  });

  it('renders correctly as a CIRCLE', () => {
    const component = renderer.create(<AzButton {...defaultProps} shape={AzButtonShape.CIRCLE} />);
    const touchable = component.root.findByType(TouchableOpacity);
    expect(touchable.props.style.borderRadius).toBe(24); // size / 2
    expect(touchable.props.style.width).toBe(48);
    expect(touchable.props.style.height).toBe(48);
  });

  it('renders correctly as a SQUARE', () => {
    const component = renderer.create(<AzButton {...defaultProps} shape={AzButtonShape.SQUARE} />);
    const touchable = component.root.findByType(TouchableOpacity);
    expect(touchable.props.style.borderRadius).toBe(0);
    expect(touchable.props.style.width).toBe(48);
    expect(touchable.props.style.height).toBe(48);
  });

  it('renders correctly as a RECTANGLE', () => {
    const component = renderer.create(<AzButton {...defaultProps} shape={AzButtonShape.RECTANGLE} />);
    const touchable = component.root.findByType(TouchableOpacity);
    expect(touchable.props.style.borderRadius).toBe(0);
    expect(touchable.props.style.height).toBe(48);
    expect(touchable.props.style.paddingHorizontal).toBe(8);
  });

  it('renders correctly as NONE', () => {
    const component = renderer.create(<AzButton {...defaultProps} shape={AzButtonShape.NONE} />);
    const touchable = component.root.findByType(TouchableOpacity);
    expect(touchable.props.style.borderColor).toBe('transparent');
    expect(touchable.props.style.borderWidth).toBe(0);
  });

  it('renders with custom color', () => {
    const color = '#FF0000';
    const component = renderer.create(<AzButton {...defaultProps} color={color} />);
    const touchable = component.root.findByType(TouchableOpacity);
    const textNode = component.root.findByType(Text);

    expect(touchable.props.style.borderColor).toBe(color);
    expect(textNode.props.style.color).toBe(color);
  });

  it('handles enabled=false state', () => {
    const component = renderer.create(<AzButton {...defaultProps} enabled={false} />);
    const touchable = component.root.findByType(TouchableOpacity);

    expect(touchable.props.disabled).toBe(true);
    expect(touchable.props.style.opacity).toBe(0.5);
    expect(touchable.props.accessibilityState.disabled).toBe(true);
  });

  it('handles isLoading=true state', () => {
    const component = renderer.create(<AzButton {...defaultProps} isLoading={true} />);
    const touchable = component.root.findByType(TouchableOpacity);

    expect(touchable.props.disabled).toBe(true);
    expect(touchable.props.accessibilityState.disabled).toBe(true);

    // Verify loader is present
    expect(component.root.findByType(AzLoad)).toBeDefined();

    // Verify text is not present
    expect(component.root.findAllByType(Text).length).toBe(0);
  });

  it('calls onClick when pressed and enabled', () => {
    const onClick = jest.fn();
    const component = renderer.create(<AzButton {...defaultProps} onClick={onClick} enabled={true} isLoading={false} />);
    const touchable = component.root.findByType(TouchableOpacity);

    touchable.props.onPress();
    expect(onClick).toHaveBeenCalled();
  });

  it('handles text with newlines correctly', () => {
    const textWithNewline = 'Line 1\nLine 2';
    const component = renderer.create(<AzButton {...defaultProps} text={textWithNewline} />);
    const textNode = component.root.findByType(Text);

    expect(textNode.props.numberOfLines).toBeUndefined();
    expect(textNode.props.adjustsFontSizeToFit).toBe(false);
  });

  it('handles text without newlines correctly', () => {
    const component = renderer.create(<AzButton {...defaultProps} text="No Newline" />);
    const textNode = component.root.findByType(Text);

    expect(textNode.props.numberOfLines).toBe(1);
    expect(textNode.props.adjustsFontSizeToFit).toBe(true);
  });
});
