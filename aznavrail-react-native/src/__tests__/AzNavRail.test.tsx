import React from 'react';
import renderer from 'react-test-renderer';
import { AzNavRail } from '../AzNavRail';
import { AzRailItem, AzMenuItem } from '../AzNavRailScope';

// Mock Animated
jest.mock('react-native/Libraries/Animated/Animated', () => {
  const React = require('react');

  class Value {
    val: any;
    constructor(val: any) { this.val = val; }
    setValue = jest.fn();
    interpolate = jest.fn();
    addListener = jest.fn();
    removeListener = jest.fn();
  }
  class ValueXY {
      x: any; y: any;
      constructor(val: any) { this.x = new Value(val?.x || 0); this.y = new Value(val?.y || 0); }
      setValue = jest.fn();
      setOffset = jest.fn();
      flattenOffset = jest.fn();
      addListener = jest.fn();
      removeListener = jest.fn();
  }
  const timing = jest.fn(() => ({ start: jest.fn() }));
  const event = jest.fn();
  const MockView = (props: any) => <>{props.children}</>;

  return {
    Value: Value,
    ValueXY: ValueXY,
    timing: timing,
    event: event,
    View: MockView,
    default: {
        Value: Value,
        ValueXY: ValueXY,
        timing: timing,
        event: event,
        View: MockView,
    }
  };
});

// Mock Dimensions
jest.mock('react-native/Libraries/Utilities/Dimensions', () => ({
  get: jest.fn().mockReturnValue({ width: 375, height: 812 }),
  addEventListener: jest.fn(() => ({ remove: jest.fn() })),
}));

describe('AzNavRail', () => {
  it('renders correctly with rail items', () => {
    const tree = renderer.create(
      <AzNavRail>
        <AzRailItem id="1" text="Home" onClick={() => {}} />
        <AzMenuItem id="2" text="Settings" onClick={() => {}} />
      </AzNavRail>
    ).toJSON();
    expect(tree).toMatchSnapshot();
  });

  it('renders correctly when expanded', () => {
      const tree = renderer.create(
        <AzNavRail initiallyExpanded={true}>
          <AzRailItem id="1" text="Home" onClick={() => {}} />
          <AzMenuItem id="2" text="Settings" onClick={() => {}} />
        </AzNavRail>
      ).toJSON();
      expect(tree).toMatchSnapshot();
    });
});
