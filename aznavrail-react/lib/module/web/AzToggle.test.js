import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import AzToggle from './AzToggle';
describe('AzToggle', () => {
  it('renders correctly with default props', () => {
    render(/*#__PURE__*/React.createElement(AzToggle, {
      value: false,
      onValueChange: () => {}
    }));
    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toBeInTheDocument();
    expect(checkbox).not.toBeChecked();
    expect(checkbox).not.toBeDisabled();
  });
  it('renders correctly when checked', () => {
    render(/*#__PURE__*/React.createElement(AzToggle, {
      value: true,
      onValueChange: () => {}
    }));
    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).toBeChecked();
  });
  it('renders label if provided', () => {
    render(/*#__PURE__*/React.createElement(AzToggle, {
      value: false,
      onValueChange: () => {},
      label: "Test Label"
    }));
    expect(screen.getByText('Test Label')).toBeInTheDocument();
  });
  it('calls onValueChange when clicked', () => {
    const handleValueChange = vi.fn();
    render(/*#__PURE__*/React.createElement(AzToggle, {
      value: false,
      onValueChange: handleValueChange
    }));
    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);
    expect(handleValueChange).toHaveBeenCalledTimes(1);
    expect(handleValueChange).toHaveBeenCalledWith(true);
  });
  it('does not call onValueChange when disabled', () => {
    const handleValueChange = vi.fn();
    render(/*#__PURE__*/React.createElement(AzToggle, {
      value: false,
      onValueChange: handleValueChange,
      enabled: false
    }));
    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);
    expect(handleValueChange).not.toHaveBeenCalled();
    expect(checkbox).toBeDisabled();
  });
  it('applies custom className and style', () => {
    const {
      container
    } = render(/*#__PURE__*/React.createElement(AzToggle, {
      value: false,
      onValueChange: () => {},
      className: "custom-class",
      style: {
        marginTop: '10px'
      }
    }));
    const wrapper = container.firstChild;
    expect(wrapper).toHaveClass('custom-class');
    expect(wrapper).toHaveStyle('margin-top: 10px');
  });
});
//# sourceMappingURL=AzToggle.test.js.map