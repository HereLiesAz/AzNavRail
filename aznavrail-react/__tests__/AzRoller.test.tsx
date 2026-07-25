import React from 'react';
import { render } from '@testing-library/react-native';
import { AzRoller } from '../src/components/AzRoller';

describe('AzRoller', () => {
  it('renders correctly', async () => {
    const { toJSON } = await render(
      <AzRoller
        options={['Option 1', 'Option 2']}
        selectedOption="Option 1"
        onOptionSelected={() => {}}
      />
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
