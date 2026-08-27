import React from 'react';
import { render, fireEvent, act } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
import { AzUnattachedHostItem, AzRailSubItem } from '../AzNavRailScope';
import { AzUnattachedAnchor } from '../types';

/**
 * Smoke coverage that a `FLOATING` unattached host mounts, positions itself without crashing, and
 * stays fully interactive — including its own sub-items — alongside a second independent `FLOATING`
 * host. This is the React analog of Android/CMP's `AzFloatingDockGroupTest`'s first two cases
 * ("item A/B stays clickable while a second independent FLOATING host coexists"); the drag/dock/
 * attach geometry those tests also cover is exercised directly in `floatingDockMath.test.ts`.
 */
describe('AzUnattachedRail — FLOATING', () => {
  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  it('renders two independent FLOATING hosts and both expand independently', async () => {
    const onA = jest.fn();
    const onB = jest.fn();
    const { getByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem
          id="hostA"
          text="A"
          anchor={AzUnattachedAnchor.FLOATING}
          onExpandedChange={onA}
        />
        <AzUnattachedHostItem
          id="hostB"
          text="B"
          anchor={AzUnattachedAnchor.FLOATING}
          onExpandedChange={onB}
        />
      </AzNavRail>
    );
    await act(async () => fireEvent.press(getByTestId('hostA')));
    await act(async () => fireEvent.press(getByTestId('hostB')));
    expect(onA).toHaveBeenCalledWith(true);
    expect(onB).toHaveBeenCalledWith(true);
  });

  it("a FLOATING host's sub-item stays clickable while a second FLOATING host coexists", async () => {
    const onSub = jest.fn();
    const { getByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem
          id="hostA"
          text="A"
          anchor={AzUnattachedAnchor.FLOATING}
          initiallyExpanded
        />
        <AzRailSubItem
          id="itemA"
          hostId="hostA"
          text="Item A"
          onClick={onSub}
        />
        <AzUnattachedHostItem
          id="hostB"
          text="B"
          anchor={AzUnattachedAnchor.FLOATING}
        />
      </AzNavRail>
    );
    await act(async () => fireEvent.press(getByTestId('itemA')));
    expect(onSub).toHaveBeenCalledTimes(1);
  });
});
