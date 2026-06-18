import React from 'react';
import { Text, Dimensions, I18nManager } from 'react-native';
import { act, fireEvent, render } from '@testing-library/react-native';
import { AzDropdownMenu, AzDropdownItem } from '../components/AzDropdownMenu';
import { AzDropdownAlignment, AzDropdownDesign } from '../types';

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

  it('constrains the panel to the menu width by default (160)', () => {
    const { getByTestId } = render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-panel').props.style.flat();
    expect(style).toEqual(expect.arrayContaining([expect.objectContaining({ width: 160 })]));
  });

  it('constrains the panel to the rail width for the rail design (100)', () => {
    const { getByTestId } = render(
      <AzDropdownMenu expanded design={AzDropdownDesign.RAIL}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-panel').props.style.flat();
    expect(style).toEqual(expect.arrayContaining([expect.objectContaining({ width: 100 })]));
  });

  it('pins the panel to the right screen edge for end alignments', () => {
    const { getByTestId } = render(
      <AzDropdownMenu expanded alignment={AzDropdownAlignment.TOP_END}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const screenWidth = Dimensions.get('window').width;
    const style = getByTestId('az-dropdown-panel').props.style.flat();
    expect(style).toEqual(expect.arrayContaining([expect.objectContaining({ left: screenWidth - 160 })]));
  });

  it('flips the pinned edge under RTL (start → right)', () => {
    const original = I18nManager.isRTL;
    (I18nManager as { isRTL: boolean }).isRTL = true;
    try {
      const { getByTestId } = render(
        <AzDropdownMenu expanded alignment={AzDropdownAlignment.TOP_START}>
          <AzDropdownItem text="Profile" onClick={() => {}} />
        </AzDropdownMenu>
      );
      const screenWidth = Dimensions.get('window').width;
      const style = getByTestId('az-dropdown-panel').props.style.flat();
      expect(style).toEqual(expect.arrayContaining([expect.objectContaining({ left: screenWidth - 160 })]));
    } finally {
      (I18nManager as { isRTL: boolean }).isRTL = original;
    }
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
