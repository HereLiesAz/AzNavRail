import React from 'react';
import renderer from 'react-test-renderer';
import { AzNavRail, AzNavHostContext } from '../AzNavRail';
import { AzRailItem, AzMenuItem } from '../AzNavRailScope';


describe('AzNavRail', () => {
  it('renders correctly with rail items', () => {
    const tree = renderer.create(
      <AzNavHostContext.Provider value={true}>
<AzNavRail>
        <AzRailItem id="1" text="Home" onClick={() => {}} />
        <AzMenuItem id="2" text="Settings" onClick={() => {}} />
      </AzNavRail>
</AzNavHostContext.Provider>
    ).toJSON();
    expect(tree).toMatchSnapshot();
  });

  it('renders correctly when expanded', () => {
      const tree = renderer.create(
        <AzNavHostContext.Provider value={true}>
<AzNavRail initiallyExpanded={true}>
          <AzRailItem id="1" text="Home" onClick={() => {}} />
          <AzMenuItem id="2" text="Settings" onClick={() => {}} />
        </AzNavRail>
</AzNavHostContext.Provider>
      ).toJSON();
      expect(tree).toMatchSnapshot();
    });
});
