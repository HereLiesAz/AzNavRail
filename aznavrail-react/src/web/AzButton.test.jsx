import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import AzButton from './AzButton';

// Mock the useFitText hook
vi.mock('../hooks/useFitText', () => ({
  default: () => ({ current: null })
}));

describe('AzButton', () => {
  it('renders text correctly', () => {
    render(<AzButton text="Click Me" onClick={() => {}} />);
    expect(screen.getByText('Click Me')).toBeInTheDocument();
  });

  it('calls onClick when clicked and enabled', () => {
    const handleClick = vi.fn();
    render(<AzButton text="Click Me" onClick={handleClick} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('does not call onClick when enabled is false', () => {
    const handleClick = vi.fn();
    render(<AzButton text="Click Me" onClick={handleClick} enabled={false} />);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('does not call onClick when isLoading is true', () => {
    const handleClick = vi.fn();
    render(<AzButton text="Click Me" onClick={handleClick} isLoading={true} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('applies the correct shape class', () => {
    const { rerender } = render(<AzButton text="Shape" shape="CIRCLE" onClick={() => {}} />);
    let button = screen.getByRole('button');
    expect(button).toHaveClass('az-button-shape-circle');

    rerender(<AzButton text="Shape" shape="ROUNDED" onClick={() => {}} />);
    button = screen.getByRole('button');
    expect(button).toHaveClass('az-button-shape-rounded');
  });

  it('applies custom padding, color and class', () => {
    render(
      <AzButton
        text="Custom"
        onClick={() => {}}
        contentPadding="20px"
        color="#ff0000"
        className="my-custom-class"
      />
    );
    const button = screen.getByRole('button');
    expect(button).toHaveClass('my-custom-class');
    // Using rgb for the color since it might be converted
    expect(button).toHaveStyle({ padding: '20px', color: 'rgb(255, 0, 0)' });
  });

  it('renders loading state correctly', () => {
    const { container } = render(<AzButton text="Loading" onClick={() => {}} isLoading={true} />);

    // The text content wrapper should have opacity 0 when loading
    const contentWrapper = container.querySelector('.az-button-content');
    expect(contentWrapper).toHaveStyle({ opacity: 0 });

    // The AzLoad component should be rendered
    const loader = container.querySelector('.az-load-container');
    expect(loader).toBeInTheDocument();
  });
});
