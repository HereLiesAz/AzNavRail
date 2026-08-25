import React from 'react';
import { render, fireEvent, act } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
import {
  AzRailItem,
  AzRailSubItem,
  AzUnattachedHostItem,
} from '../AzNavRailScope';
import { AzUnattachedAnchor } from '../types';

/**
 * Behavioral coverage for `AzUnattachedRail`, translating the scenarios in Android/CMP's
 * `AzUnattachedRail.kt` (OPPOSITE/BOTTOM stacking, host expand/collapse, and an unattached item
 * staying fully interactive alongside the ordinary rail strip) onto the React port. The
 * `FLOATING` anchor's drag/dock/attach contract is covered separately and directly at the geometry
 * layer in `floatingDockMath.test.ts` — see that file's own doc comment for why.
 */
describe('AzUnattachedRail', () => {
  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  it('renders nothing extra when no unattached host is declared', async () => {
    const { queryByTestId } = await render(
      <AzNavRail>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
      </AzNavRail>
    );
    expect(queryByTestId('tools')).toBeNull();
  });

  it('renders an OPPOSITE-anchored unattached host outside the rail strip, and tapping it expands/collapses', async () => {
    const onExpandedChange = jest.fn();
    const { getByTestId } = await render(
      <AzNavRail>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
        <AzUnattachedHostItem
          id="tools"
          text="Tools"
          anchor={AzUnattachedAnchor.OPPOSITE}
          onExpandedChange={onExpandedChange}
        />
      </AzNavRail>
    );
    const host = getByTestId('tools');
    expect(host).toBeTruthy();
    await act(async () => fireEvent.press(host));
    expect(onExpandedChange).toHaveBeenCalledWith(true);
    await act(async () => fireEvent.press(host));
    expect(onExpandedChange).toHaveBeenCalledWith(false);
  });

  it("an unattached host does not appear in the rail strip's own item list", async () => {
    const { queryAllByTestId } = await render(
      <AzNavRail>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
        <AzUnattachedHostItem id="tools" text="Tools" onClick={() => {}} />
      </AzNavRail>
    );
    // Rendered exactly once (by AzUnattachedRail), not a second time by the rail strip.
    expect(queryAllByTestId('tools').length).toBe(1);
  });

  it("expands an unattached host to reveal its own sub-items, independent of the rail's drawer", async () => {
    const onSubClick = jest.fn();
    const { getByTestId, queryByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem id="tools" text="Tools" />
        <AzRailSubItem
          id="measure"
          hostId="tools"
          text="Measure"
          onClick={onSubClick}
        />
      </AzNavRail>
    );
    expect(queryByTestId('measure')).toBeNull();
    await act(async () => fireEvent.press(getByTestId('tools')));
    const sub = getByTestId('measure');
    expect(sub).toBeTruthy();
    await act(async () => fireEvent.press(sub));
    expect(onSubClick).toHaveBeenCalledTimes(1);
  });

  it("initiallyExpanded unfolds an unattached host's sub-items on first render", async () => {
    const { getByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem id="tools" text="Tools" initiallyExpanded />
        <AzRailSubItem
          id="measure"
          hostId="tools"
          text="Measure"
          onClick={() => {}}
        />
      </AzNavRail>
    );
    expect(getByTestId('measure')).toBeTruthy();
  });

  it('stacks two OPPOSITE hosts and both expand independently', async () => {
    const onA = jest.fn();
    const onB = jest.fn();
    const { getByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem
          id="a"
          text="A"
          anchor={AzUnattachedAnchor.OPPOSITE}
          onExpandedChange={onA}
        />
        <AzUnattachedHostItem
          id="b"
          text="B"
          anchor={AzUnattachedAnchor.OPPOSITE}
          onExpandedChange={onB}
        />
      </AzNavRail>
    );
    await act(async () => fireEvent.press(getByTestId('a')));
    await act(async () => fireEvent.press(getByTestId('b')));
    expect(onA).toHaveBeenCalledWith(true);
    expect(onB).toHaveBeenCalledWith(true);
  });

  it('a BOTTOM-anchored host renders and remains interactive alongside an OPPOSITE one', async () => {
    const onBottomSub = jest.fn();
    const { getByTestId } = await render(
      <AzNavRail>
        <AzUnattachedHostItem
          id="top"
          text="Top"
          anchor={AzUnattachedAnchor.OPPOSITE}
        />
        <AzUnattachedHostItem
          id="bottom"
          text="Bottom"
          anchor={AzUnattachedAnchor.BOTTOM}
          initiallyExpanded
        />
        <AzRailSubItem
          id="bottomSub"
          hostId="bottom"
          text="Sub"
          onClick={onBottomSub}
        />
      </AzNavRail>
    );
    expect(getByTestId('bottom')).toBeTruthy();
    await act(async () => fireEvent.press(getByTestId('bottomSub')));
    expect(onBottomSub).toHaveBeenCalledTimes(1);
  });
});
