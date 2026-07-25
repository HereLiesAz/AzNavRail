import React from 'react';
import { Dimensions } from 'react-native';
import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { AzDropdownMenu, AzDropdownItem } from '../components/AzDropdownMenu';
import { AzDockingSide, AzDropdownDesign, AzHeaderIconShape } from '../types';

describe('AzDropdownMenu', () => {
  it('is closed initially and opens when the app-icon trigger is pressed', async () => {
    const { queryByText, getByLabelText } = await render(
      <AzDropdownMenu>
        <AzDropdownItem text="Settings" onClick={() => {}} />
      </AzDropdownMenu>
    );

    expect(queryByText('Settings')).toBeNull();
    await fireEvent.press(getByLabelText('Menu'));
    expect(queryByText('Settings')).not.toBeNull();
  });

  it('runs the item callback and folds the menu by default', async () => {
    const onClick = jest.fn();
    const { queryByText, getByText, getByLabelText } = await render(
      <AzDropdownMenu>
        <AzDropdownItem text="Sign out" onClick={onClick} />
      </AzDropdownMenu>
    );

    await fireEvent.press(getByLabelText('Menu'));
    await fireEvent.press(getByText('Sign out'));

    expect(onClick).toHaveBeenCalledTimes(1);
    // Folded — but the panel plays an exit animation on the way out, so wait for it to leave
    // rather than asserting on the frame immediately after the press.
    await waitFor(() => expect(queryByText('Sign out')).toBeNull());
  });

  it('keeps the menu open when closeOnClick is false', async () => {
    const onClick = jest.fn();
    const { queryByText, getByText, getByLabelText } = await render(
      <AzDropdownMenu>
        <AzDropdownItem text="Toggle" closeOnClick={false} onClick={onClick} />
      </AzDropdownMenu>
    );

    await fireEvent.press(getByLabelText('Menu'));
    await fireEvent.press(getByText('Toggle'));

    expect(onClick).toHaveBeenCalledTimes(1);
    expect(queryByText('Toggle')).not.toBeNull(); // still open
  });

  it('respects a controlled expanded prop', async () => {
    const { queryByText } = await render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(queryByText('Profile')).not.toBeNull();
  });

  it('constrains the panel to the menu width by default (160)', async () => {
    const { getByTestId } = await render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-panel').props.style.flat();
    expect(style).toEqual(
      expect.arrayContaining([expect.objectContaining({ width: 160 })])
    );
  });

  it('constrains the panel to the rail width for the rail design (100)', async () => {
    const { getByTestId } = await render(
      <AzDropdownMenu expanded design={AzDropdownDesign.RAIL}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-panel').props.style.flat();
    expect(style).toEqual(
      expect.arrayContaining([expect.objectContaining({ width: 100 })])
    );
  });

  it('pins the panel to the left edge by default and the right edge when docked right', async () => {
    const left = await render(
      <AzDropdownMenu expanded dockingSide={AzDockingSide.LEFT}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const leftStyle = left.getByTestId('az-dropdown-panel').props.style.flat();
    expect(leftStyle).toEqual(
      expect.arrayContaining([expect.objectContaining({ left: 0 })])
    );

    const right = await render(
      <AzDropdownMenu expanded dockingSide={AzDockingSide.RIGHT}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    // Right edge = window width minus the panel width. Read the width rather than hard-coding it:
    // this used to assume the 375 that a hand-rolled `react-native` mock reported.
    const windowWidth = Dimensions.get('window').width;
    const rightStyle = right
      .getByTestId('az-dropdown-panel')
      .props.style.flat();
    expect(rightStyle).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ left: windowWidth - 160 }),
      ])
    );
  });

  it('applies a configurable app-icon size and shape to the trigger', async () => {
    const { getByTestId } = await render(
      <AzDropdownMenu
        headerIconSize={72}
        headerIconShape={AzHeaderIconShape.ROUNDED}
      >
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByTestId('az-dropdown-trigger').props.style;
    expect(style.width).toBe(72);
    expect(style.height).toBe(72);
    expect(style.borderRadius).toBe(8); // ROUNDED → 8
  });

  it('shows the rail footer in the MENU design by default', async () => {
    const { queryByText } = await render(
      <AzDropdownMenu expanded appRepositoryUrl="https://github.com/acme/app">
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(queryByText('About')).not.toBeNull();
    expect(queryByText('Feedback')).not.toBeNull();
    expect(queryByText('@HereLiesAz')).not.toBeNull();
  });

  it('hides the footer About when no appRepositoryUrl is set', async () => {
    const { queryByText } = await render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    // About is gated on appRepositoryUrl; the rest of the footer still renders.
    expect(queryByText('About')).toBeNull();
    expect(queryByText('Feedback')).not.toBeNull();
    expect(queryByText('@HereLiesAz')).not.toBeNull();
  });

  it('omits the footer when showFooter is false and in the RAIL design', async () => {
    const noFooter = await render(
      <AzDropdownMenu expanded showFooter={false}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(noFooter.queryByText('Feedback')).toBeNull();

    const rail = await render(
      <AzDropdownMenu expanded design={AzDropdownDesign.RAIL}>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    expect(rail.queryByText('Feedback')).toBeNull();
  });

  it('renders menu rows at the rail menu-item text size (16)', async () => {
    const { getByText } = await render(
      <AzDropdownMenu expanded>
        <AzDropdownItem text="Profile" onClick={() => {}} />
      </AzDropdownMenu>
    );
    const style = getByText('Profile').props.style.flat();
    expect(style).toEqual(
      expect.arrayContaining([expect.objectContaining({ fontSize: 16 })])
    );
  });

  it('dispatches an item route through onNavigate before the callback', async () => {
    const onNavigate = jest.fn();
    const onClick = jest.fn();
    const { getByText, getByLabelText } = await render(
      <AzDropdownMenu onNavigate={onNavigate}>
        <AzDropdownItem text="Home" route="home" onClick={onClick} />
      </AzDropdownMenu>
    );

    await fireEvent.press(getByLabelText('Menu'));
    await fireEvent.press(getByText('Home'));

    expect(onNavigate).toHaveBeenCalledWith('home');
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
