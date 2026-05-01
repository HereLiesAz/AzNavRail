import React from 'react';
import { render, fireEvent } from '@testing-library/react-native';
import { HelpOverlay } from '../components/HelpOverlay';
import { AzTutorialProvider } from '../tutorial/AzTutorialController';
import { AzNavItem } from '../types';

const mockItem: AzNavItem = {
  id: 'item-1', text: 'Home', isRailItem: true,
  isToggle: false, toggleOnText: '', toggleOffText: '',
  isCycler: false, isDivider: false, collapseOnClick: false,
  shape: 'CIRCLE' as any, disabled: false,
  isHost: false, isSubItem: false, isExpanded: false,
  info: 'This is the home button.',
};

const renderWithProvider = (ui: React.ReactElement) =>
  render(<AzTutorialProvider>{ui}</AzTutorialProvider>);

describe('HelpOverlay tutorial launch flow', () => {
  it('shows Tutorial available hint on collapsed card when tutorial exists', () => {
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    expect(getByText('Tutorial available')).toBeTruthy();
  });

  it('does NOT show Start Tutorial button on collapsed card', () => {
    const { queryByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    expect(queryByText('Start Tutorial')).toBeNull();
  });

  it('shows Start Tutorial button after expanding card', () => {
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    fireEvent.press(getByText('Home'));
    expect(getByText('Start Tutorial')).toBeTruthy();
  });

  it('Start Tutorial button calls onDismiss', () => {
    const onDismiss = jest.fn();
    const { getByText } = renderWithProvider(
      <HelpOverlay
        items={[mockItem]}
        onDismiss={onDismiss}
        helpList={{}}
        itemBounds={{}}
        tutorials={{ 'item-1': { scenes: [] } }}
      />
    );
    fireEvent.press(getByText('Home'));
    fireEvent.press(getByText('Start Tutorial'));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });
});
