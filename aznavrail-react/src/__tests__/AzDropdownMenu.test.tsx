import React from 'react';
import { act, fireEvent, render } from '@testing-library/react-native';
import { AzDropdownMenu, AzDropdownItem } from '../components/AzDropdownMenu';
import { AzDockingSide, AzDropdownDesign, AzHeaderIconShape } from '../types';

describe('AzDropdownMenu', () => {
  it('is closed initially and opens when the app-icon trigger is pressed', () => {
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

  it('pins the panel to the left edge by default and the right edge when docked right', () => {
    const left = render(
      <AzDropdownMenu expanded dockingSide={AzDockingSide.LEFT}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const leftStyle = left.getByTestId('az-dropdown-panel').props.style.flat();
    expect(leftStyle).toEqual(expect.arrayContaining([expect.objectContaining({ left: 0 })]));

    const right = render(
      <AzDropdownMenu expanded dockingSide={AzDockingSide.RIGHT}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    // Default mocked window width is 375; right edge = 375 - 160 = 215.
    const rightStyle = right.getByTestId('az-dropdown-panel').props.style.flat();
    expect(rightStyle).toEqual(expect.arrayContaining([expect.objectContaining({ left: 375 - 160 })]));
  });

  it('applies a configurable app-icon size and shape to the trigger', () => {
    const { getByTestId } = render(
      <AzDropdownMenu headerIconSize={72} headerIconShape={AzHeaderIconShape.ROUNDED}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-trigger').props.style;
    expect(style.width).toBe(72);
    expect(style.height).toBe(72);
    expect(style.borderRadius).toBe(8); // ROUNDED → 8
  });

  it('shows the rail footer in the MENU design by default', () => {
    const { queryByText } = render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(queryByText('About')).not.toBeNull();
    expect(queryByText('Feedback')).not.toBeNull();
    expect(queryByText('@HereLiesAz')).not.toBeNull();
  });

  it('omits the footer when showFooter is false and in the RAIL design', () => {
    const noFooter = render(
      <AzDropdownMenu expanded showFooter={false}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(noFooter.queryByText('Feedback')).toBeNull();

    const rail = render(
      <AzDropdownMenu expanded design={AzDropdownDesign.RAIL}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(rail.queryByText('Feedback')).toBeNull();
  });

  it('renders menu rows at the rail menu-item text size (16)', () => {
    const { getByText } = render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByText('Profile').props.style.flat();
    expect(style).toEqual(expect.arrayContaining([expect.objectContaining({ fontSize: 16 })]));
  });

  it('dispatches an item route through onNavigate before the callback', () => {
    const onNavigate = jest.fn();
    const onClick = jest.fn();
    const { getByText, getByLabelText } = render(
      <AzDropdownMenu onNavigate={onNavigate}>
        <AzDropdownItem text="Home" route="home" onClick={onClick} />
      </AzDropdownMenu>
    );

    act(() => {
      fireEvent.press(getByLabelText('Menu'));
    });
    act(() => {
      fireEvent.press(getByText('Home'));
    });

    expect(onNavigate).toHaveBeenCalledWith('home');
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
