import React from 'react';
import { render } from '@testing-library/react-native';
import { HelpOverlay } from '../components/HelpOverlay';
import { AzNavItem, AzButtonShape } from '../types';

/**
 * Smoke tests for the shared HelpOverlay. These confirm cards render for items that
 * have either an `info` field or a `helpList` entry, and that the overlay does not
 * throw when given a minimal set of inputs.
 */
const makeItem = (overrides: Partial<AzNavItem> & Pick<AzNavItem, 'id' | 'text'>): AzNavItem => ({
  isRailItem: true,
  isToggle: false,
  toggleOnText: '',
  toggleOffText: '',
  isCycler: false,
  isDivider: false,
  collapseOnClick: false,
  shape: AzButtonShape.CIRCLE,
  disabled: false,
  isHost: false,
  isSubItem: false,
  isExpanded: false,
  ...overrides,
});

const renderWithProvider = (ui: React.ReactElement) => render(ui);

describe('HelpOverlay smoke', () => {
  it('renders a card for an item that has `info` text (info content drives card visibility)', () => {
    // Failure: If "Home" is not in the tree, the itemsWithInfo filter is rejecting
    // items with valid `info`.
    const items = [
      makeItem({ id: 'home', text: 'Home', info: 'Home button info text.' }),
    ];
    const { getByText } = renderWithProvider(
      <HelpOverlay items={items} onDismiss={jest.fn()} helpList={{}} itemBounds={{}} />
    );
    expect(getByText('Home')).toBeTruthy();
    expect(getByText('Home button info text.')).toBeTruthy();
  });

  it('renders a card for an item that has only a helpList entry (helpList drives visibility too)', () => {
    // Failure: If the card is missing, the itemsWithInfo filter is not checking
    // helpList[item.id]?.trim().
    const items = [makeItem({ id: 'search', text: 'Search' })];
    const helpList = { search: 'Search functionality help.' };
    const { getByText } = renderWithProvider(
      <HelpOverlay items={items} onDismiss={jest.fn()} helpList={helpList} itemBounds={{}} />
    );
    expect(getByText('Search')).toBeTruthy();
    expect(getByText('Search functionality help.')).toBeTruthy();
  });

  it('skips items that have neither info nor helpList entry (silent items render nothing)', () => {
    // Failure: If "Empty" appears, the filter is letting through items with no help text.
    const items = [
      makeItem({ id: 'empty', text: 'Empty' }),
      makeItem({ id: 'visible', text: 'Visible', info: 'Has info.' }),
    ];
    const { queryByText, getByText } = renderWithProvider(
      <HelpOverlay items={items} onDismiss={jest.fn()} helpList={{}} itemBounds={{}} />
    );
    expect(queryByText('Empty')).toBeNull();
    expect(getByText('Visible')).toBeTruthy();
  });

  it('renders multiple cards (each item with info produces its own card)', () => {
    // Failure: If only one card renders, the .map(i => ...) is being short-circuited
    // by a stray break or filter.
    const items = [
      makeItem({ id: 'a', text: 'Alpha', info: 'A info' }),
      makeItem({ id: 'b', text: 'Bravo', info: 'B info' }),
      makeItem({ id: 'c', text: 'Charlie', info: 'C info' }),
    ];
    const { getByText } = renderWithProvider(
      <HelpOverlay items={items} onDismiss={jest.fn()} helpList={{}} itemBounds={{}} />
    );
    expect(getByText('Alpha')).toBeTruthy();
    expect(getByText('Bravo')).toBeTruthy();
    expect(getByText('Charlie')).toBeTruthy();
  });

  it('mounts cleanly with sample itemBounds (bounds drive connector-line geometry)', () => {
    // Failure: If this throws, the line-drawing block is dereferencing navBounds
    // without checking it exists.
    const items = [makeItem({ id: 'home', text: 'Home', info: 'Info.' })];
    const itemBounds = { home: { x: 0, y: 50, width: 56, height: 56 } };
    expect(() =>
      renderWithProvider(
        <HelpOverlay items={items} onDismiss={jest.fn()} helpList={{}} itemBounds={itemBounds} />
      )
    ).not.toThrow();
  });

  it('mounts with no items at all (empty list is a valid input)', () => {
    // Failure: If this throws, the empty-array path through itemsWithInfo is hitting
    // a method on undefined.
    expect(() =>
      renderWithProvider(
        <HelpOverlay items={[]} onDismiss={jest.fn()} helpList={{}} itemBounds={{}} />
      )
    ).not.toThrow();
  });

  it('renders only nested sub-items when nestedRailVisibleId is set (overlay scopes to nested host)', () => {
    // Failure: If the top-level item appears while a nested rail is visible, the
    // allItems memo is not switching to nestedHost.nestedRailItems.
    const items = [
      makeItem({
        id: 'host',
        text: 'Host',
        info: 'Host info',
        nestedRailItems: [
          makeItem({ id: 'sub1', text: 'Sub One', info: 'Sub 1 info', isSubItem: true }),
        ],
      } as any),
    ];
    const { getByText, queryByText } = renderWithProvider(
      <HelpOverlay
        items={items}
        onDismiss={jest.fn()}
        helpList={{}}
        itemBounds={{}}
        nestedRailVisibleId="host"
      />
    );
    expect(getByText('Sub One')).toBeTruthy();
    expect(queryByText('Host')).toBeNull();
  });
});
