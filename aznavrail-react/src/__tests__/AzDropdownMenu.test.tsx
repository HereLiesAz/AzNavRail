import React from 'react';
import { Text } from 'react-native';
import { act, fireEvent, render } from '@testing-library/react-native';
import { AzDropdownMenu, AzDropdownItem } from '../components/AzDropdownMenu';

describe('AzDropdownMenu', () => {
  it('is closed initially and opens when the trigger is pressed', () => {
    const { queryByText, getByLabelText } = render(
      <AzDropdownMenu>
        <AzDropdownItem text="Settings" onClick={() => {}} />
      </AzDropdownMenu>
    );

    expect(queryByText('Settings')).toBeNull();
    act(() => {
      fireEvent.press(getByLabelText('Menu'));
    });
    expect(queryByText('Settings')).not.toBeNull();
  });

  it('runs the item callback and folds the menu by default', () => {
    const onClick = jest.fn();
    const { queryByText, getByText, getByLabelText } = render(
      <AzDropdownMenu>
        <AzDropdownItem text="Sign out" onClick={onClick} />
      </AzDropdownMenu>
    );

    act(() => {
      fireEvent.press(getByLabelText('Menu'));
    });
    act(() => {
      fireEvent.press(getByText('Sign out'));
    });

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(queryByText('Sign out')).toBeNull(); // folded
  });

  it('keeps the menu open when closeOnClick is false', () => {
    const onClick = jest.fn();
    const { queryByText, getByText, getByLabelText } = render(
      <AzDropdownMenu>
        <AzDropdownItem text="Toggle" closeOnClick={false} onClick={onClick} />
      </AzDropdownMenu>
    );

    act(() => {
      fireEvent.press(getByLabelText('Menu'));
    });
    act(() => {
      fireEvent.press(getByText('Toggle'));
    });

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(queryByText('Toggle')).not.toBeNull(); // still open
  });

  it('respects a controlled expanded prop', () => {
    const { queryByText } = render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(queryByText('Profile')).not.toBeNull();
  });

  it('supports arbitrary children via the dismiss context', () => {
    const { getByText, getByLabelText } = render(
      <AzDropdownMenu>
        <Text>Custom row</Text>
      </AzDropdownMenu>
    );
    act(() => {
      fireEvent.press(getByLabelText('Menu'));
    });
    expect(getByText('Custom row')).toBeTruthy();
  });
});
