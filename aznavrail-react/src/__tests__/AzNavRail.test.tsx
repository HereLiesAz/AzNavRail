import React from 'react';
import { render } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
import { AzRailItem, AzMenuItem } from '../AzNavRailScope';

// `AzNavHostContext` was renamed/removed during the React-port refactor (the matching context
// lives on `AzHostContext` in `AzNavHost.tsx`). These snapshot tests don't actually need an
// explicit provider — `AzNavRail` mounts standalone — so the wrappers are dropped.

describe('AzNavRail', () => {
  // The rail runs continuous entrance animations; without fake timers `render` never settles and
  // the snapshot tests time out waiting for act() to drain.
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders correctly with rail items', async () => {
    const { toJSON } = await render(
      <AzNavRail>
        <AzRailItem id="1" text="Home" onClick={() => {}} />
        <AzMenuItem id="2" text="Settings" onClick={() => {}} />
      </AzNavRail>
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('renders correctly when expanded', async () => {
    const { toJSON } = await render(
      <AzNavRail initiallyExpanded={true}>
        <AzRailItem id="1" text="Home" onClick={() => {}} />
        <AzMenuItem id="2" text="Settings" onClick={() => {}} />
      </AzNavRail>
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
