import React from 'react';
import { render, fireEvent, act } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
import {
  AzRailItem,
  AzRailHostItem,
  AzRailToggle,
  AzUnattachedHostItem,
} from '../AzNavRailScope';

/**
 * Regression coverage for the hidden context menu being reloc-item-only in the React port: the
 * popup was rendered only inside `DraggableRailItemWrapper` (used exclusively for `isRelocItem`),
 * and `AzUnattachedRail`'s default branch and popup render were both gated on `item.isRelocItem`
 * too. Every other item type (plain, host, toggle, cycler, nested-rail) had zero hidden-menu
 * wiring. A hidden menu is not a reloc-only affordance — any item may carry one.
 */
describe('hidden menu on non-reloc items', () => {
  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  it('long-press on a plain AzRailItem with a hiddenMenu opens it', async () => {
    const onAction = jest.fn();
    const { getByTestId, getByText } = await render(
      <AzNavRail>
        <AzRailItem
          id="item1"
          text="Item 1"
          onClick={() => {}}
          hiddenMenu={[{ text: 'Rename', onClick: onAction }]}
        />
      </AzNavRail>
    );

    await act(async () => fireEvent(getByTestId('item1'), 'longPress'));

    const action = getByText('Rename');
    expect(action).toBeTruthy();
    await act(async () => fireEvent.press(action));
    expect(onAction).toHaveBeenCalled();
  });

  it('long-press on an AzRailHostItem with a hiddenMenu opens it', async () => {
    const onAction = jest.fn();
    const { getByTestId, getByText } = await render(
      <AzNavRail>
        <AzRailHostItem
          id="host1"
          text="Host"
          hiddenMenu={[{ text: 'Rename', onClick: onAction }]}
        />
      </AzNavRail>
    );

    await act(async () => fireEvent(getByTestId('host1'), 'longPress'));

    const action = getByText('Rename');
    await act(async () => fireEvent.press(action));
    expect(onAction).toHaveBeenCalled();
  });

  it('a hiddenMenu builder function resolves listItem entries for a plain item', async () => {
    const onAction = jest.fn();
    const { getByTestId, getByText } = await render(
      <AzNavRail>
        <AzRailItem
          id="item1"
          text="Item 1"
          onClick={() => {}}
          hiddenMenu={(scope) => {
            scope.listItem('Delete', onAction);
          }}
        />
      </AzNavRail>
    );

    await act(async () => fireEvent(getByTestId('item1'), 'longPress'));

    const action = getByText('Delete');
    await act(async () => fireEvent.press(action));
    expect(onAction).toHaveBeenCalled();
  });

  it('an item with no hiddenMenu has no long-press affordance and no popup appears', async () => {
    const { getByTestId, queryByTestId } = await render(
      <AzNavRail>
        <AzRailItem id="item1" text="Item 1" onClick={() => {}} />
      </AzNavRail>
    );

    await act(async () => fireEvent(getByTestId('item1'), 'longPress'));

    expect(queryByTestId('item1_hidden_menu')).toBeNull();
  });

  it('long-press on an AzUnattachedHostItem with a hiddenMenu opens it', async () => {
    const onAction = jest.fn();
    const { getByTestId, getByText } = await render(
      <AzNavRail>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
        <AzUnattachedHostItem
          id="tools"
          text="Tools"
          hiddenMenu={[{ text: 'Rename', onClick: onAction }]}
        />
      </AzNavRail>
    );

    await act(async () => fireEvent(getByTestId('tools'), 'longPress'));

    const action = getByText('Rename');
    await act(async () => fireEvent.press(action));
    expect(onAction).toHaveBeenCalled();
  });

  it('AzRailToggle accepts a hiddenMenu prop without throwing (type-level regression)', async () => {
    const { getByTestId } = await render(
      <AzNavRail>
        <AzRailToggle
          id="toggle1"
          text="Toggle"
          isChecked={false}
          toggleOnText="On"
          toggleOffText="Off"
          hiddenMenu={[{ text: 'Reset', onClick: () => {} }]}
        />
      </AzNavRail>
    );
    expect(getByTestId('toggle1')).toBeTruthy();
  });
});
