import React from 'react';
import renderer from 'react-test-renderer';
import { AzRoller } from '../src/components/AzRoller';

describe('AzRoller', () => {
  it('renders correctly', () => {
    const tree = renderer
      .create(
        <AzRoller
          options={['Option 1', 'Option 2']}
          selectedOption="Option 1"
          onOptionSelected={() => {}}
        />
      )
      .toJSON();
    expect(tree).toMatchSnapshot();
  });
});
