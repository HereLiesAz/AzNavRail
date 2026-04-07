import React from 'react';
import { render, screen, act } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import useFitText from './useFitText';

describe('useFitText', () => {
  let observeMock;
  let disconnectMock;
  let resizeCallbacks = [];

  beforeEach(() => {
    observeMock = vi.fn();
    disconnectMock = vi.fn();
    resizeCallbacks = [];
    global.ResizeObserver = class {
      constructor(callback) {
        resizeCallbacks.push(callback);
      }
      observe = observeMock;
      disconnect = disconnectMock;
    };
  });

  afterEach(() => {
    vi.restoreAllMocks();
    delete global.ResizeObserver;
  });

  const setupMockDimensions = (node, containerWidth, containerHeight, sizeMultiplier = 10) => {
    if (node) {
      Object.defineProperties(node, {
        scrollWidth: {
          get: () => {
            const size = parseInt(node.style.fontSize) || 0;
            return size * sizeMultiplier;
          },
          configurable: true
        },
        scrollHeight: {
          get: () => {
            const size = parseInt(node.style.fontSize) || 0;
            return size * (sizeMultiplier / 2); // So height is not usually bounding if height is big enough
          },
          configurable: true
        }
      });
      if (node.parentElement) {
        Object.defineProperties(node.parentElement, {
          clientWidth: { get: () => containerWidth.current, configurable: true },
          clientHeight: { get: () => containerHeight.current, configurable: true },
        });
      }
    }
  };

  it('adjusts font size correctly based on container constraints', () => {
    const TestComponent = () => {
      const ref = useFitText();
      const containerWidth = { current: 100 };
      const containerHeight = { current: 100 };
      return (
        <div data-testid="container">
          <div ref={(node) => {
            if (node) {
              ref.current = node;
              setupMockDimensions(node, containerWidth, containerHeight, 10);
            }
          }} data-testid="text">Hello</div>
        </div>
      );
    };

    render(<TestComponent />);
    const textElement = screen.getByTestId('text');
    expect(textElement.style.fontSize).toBe('10px');
  });

  it('caps font size at maximum 20px if container is very large', () => {
    const TestComponent = () => {
      const ref = useFitText();
      const containerWidth = { current: 1000 };
      const containerHeight = { current: 500 };
      return (
        <div data-testid="container">
          <div ref={(node) => {
            if (node) {
              ref.current = node;
              setupMockDimensions(node, containerWidth, containerHeight, 10);
            }
          }} data-testid="text">Hello</div>
        </div>
      );
    };

    render(<TestComponent />);
    const textElement = screen.getByTestId('text');
    expect(textElement.style.fontSize).toBe('20px');
  });

  it('sets font size down to 0px if even 1px overflows extremely small container', () => {
    const TestComponent = () => {
      const ref = useFitText();
      const containerWidth = { current: 5 };
      const containerHeight = { current: 5 };
      return (
        <div data-testid="container">
          <div ref={(node) => {
            if (node) {
              ref.current = node;
              setupMockDimensions(node, containerWidth, containerHeight, 10);
            }
          }} data-testid="text">Hello</div>
        </div>
      );
    };

    render(<TestComponent />);
    const textElement = screen.getByTestId('text');
    expect(textElement.style.fontSize).toBe('0px');
  });

  it('re-calculates font size when ResizeObserver triggers', () => {
    const containerWidth = { current: 100 };
    const containerHeight = { current: 100 };
    const TestComponent = () => {
      const ref = useFitText();
      return (
        <div data-testid="container">
          <div ref={(node) => {
            if (node) {
              ref.current = node;
              setupMockDimensions(node, containerWidth, containerHeight, 10);
            }
          }} data-testid="text">Hello</div>
        </div>
      );
    };

    render(<TestComponent />);
    const textElement = screen.getByTestId('text');
    expect(textElement.style.fontSize).toBe('10px');

    containerWidth.current = 150;

    act(() => {
      resizeCallbacks[0]([]);
    });

    expect(textElement.style.fontSize).toBe('15px');
  });

  it('disconnects ResizeObserver on unmount', () => {
    const TestComponent = () => {
      const ref = useFitText();
      return (
        <div data-testid="container">
          <div ref={ref} data-testid="text">Hello</div>
        </div>
      );
    };

    const { unmount } = render(<TestComponent />);
    expect(disconnectMock).not.toHaveBeenCalled();
    unmount();
    expect(disconnectMock).toHaveBeenCalled();
  });

  it('does not crash if ref.current is null', () => {
    const TestComponent = () => {
      useFitText();
      return <div data-testid="container">Hello</div>;
    };

    expect(() => render(<TestComponent />)).not.toThrow();
  });

  it('does not crash if element has no parent', () => {
    const TestComponent = () => {
      const ref = useFitText();
      React.useLayoutEffect(() => {
        const div = document.createElement('div');
        ref.current = div;
      }, [ref]);
      return null;
    };

    expect(() => render(<TestComponent />)).not.toThrow();
  });
});
