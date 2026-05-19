import React from 'react';
import renderer from 'react-test-renderer';
import { AzNavRail } from '../AzNavRail';
import { AzRailItem, AzMenuItem, AzHelpRailItem } from '../AzNavRailScope';
import { AzButtonShape, AzDockingSide, AzHeaderIconShape } from '../types';

/**
 * Smoke-level mount tests. Each test exercises one AzNavRailSettings flag (or combination)
 * and asserts the rail mounts and produces a non-null JSON tree.
 *
 * If any of these throws, the corresponding setting is mis-typed in AzNavRailInner's
 * destructuring/config block in AzNavRail.tsx — names must match the AzNavRailSettings
 * interface exactly.
 */
// Note: AzNavRailScope.tsx's unregister cleanup is now deferred via `queueMicrotask` so the
// test-renderer commit doesn't loop. If you see "Maximum update depth exceeded" here again,
// check that the deferral in `useAzItem`'s second `useEffect` is still in place.
describe('AzNavRail rendering with AzNavRailSettings flags', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });
  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  const mount = (jsx: React.ReactElement) => {
    let tree: renderer.ReactTestRenderer = null as any;
    renderer.act(() => {
      tree = renderer.create(jsx);
    });
    return tree;
  };

  it('mounts a bare AzNavRail with a single AzRailItem (smoke: AzRailItem registers without error)', () => {
    // Failure: If this throws, AzRailItem.useAzItem is not handling a minimal item;
    // check the required-prop fill-in in AzNavRailScope.tsx.
    const tree = mount(
      <AzNavRail>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('mounts with AzRailItem + AzMenuItem + AzHelpRailItem children (smoke: mixed DSL items coexist)', () => {
    // Failure: If this throws, one of the three scope components is broken or its
    // useAzItem call is missing required props.
    const tree = mount(
      <AzNavRail initiallyExpanded={true}>
        <AzRailItem id="home" text="Home" onClick={() => {}} />
        <AzMenuItem id="settings" text="Settings" onClick={() => {}} />
        <AzHelpRailItem id="help" text="Help" info="Help text" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects noMenu=true (smoke: rail still mounts when menu expansion is permanently disabled)', () => {
    // Failure: If this throws, the noMenu branch in AzNavRailInner unmounts content
    // instead of just hiding the menu.
    const tree = mount(
      <AzNavRail noMenu={true}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects isLoading=true (renders the loading overlay at root level)', () => {
    // Failure: If no zIndex:10000 view appears, the loading overlay branch in
    // AzNavRail.tsx is gated on a wrong flag.
    const tree = mount(
      <AzNavRail isLoading={true}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    const loader = tree.root.findAll(
      (n: any) => n.type === 'View' && n.props.style && n.props.style.zIndex === 10000
    );
    expect(loader.length).toBe(1);
    renderer.act(() => tree.unmount());
  });

  it('respects defaultShape=SQUARE (smoke: alt button shape mounts without error)', () => {
    // Failure: If this throws, the defaultShape value is being passed through unchecked
    // to a switch that lacks the SQUARE case.
    const tree = mount(
      <AzNavRail defaultShape={AzButtonShape.SQUARE}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects dockingSide=RIGHT (rail mounts on right edge)', () => {
    // Failure: If this throws, AzDockingSide.RIGHT is not handled in the layout helper.
    const tree = mount(
      <AzNavRail dockingSide={AzDockingSide.RIGHT}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects headerIconShape=SQUARE (smoke: header icon shape variant mounts)', () => {
    // Failure: If this throws, the header icon shape switch is missing a case for SQUARE.
    const tree = mount(
      <AzNavRail headerIconShape={AzHeaderIconShape.SQUARE}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects showFooter=false (rail mounts without the footer block)', () => {
    // Failure: If this throws, showFooter=false is breaking the conditional render
    // in AzNavRail.tsx footer section.
    const tree = mount(
      <AzNavRail initiallyExpanded={true} showFooter={false}>
        <AzMenuItem id="settings" text="Settings" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects displayAppNameInHeader=false (smoke: app-name omitted from header)', () => {
    // Failure: If this throws, the displayAppNameInHeader conditional in the header is
    // rendering Text with null/undefined children.
    const tree = mount(
      <AzNavRail initiallyExpanded={true} displayAppNameInHeader={false}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects expandedRailWidth and collapsedRailWidth overrides (smoke: numeric width props pass through)', () => {
    // Failure: If this throws, the width props are being passed to a style as a string
    // rather than a number, or are typed wrong on AzNavRailSettings.
    const tree = mount(
      <AzNavRail expandedRailWidth={300} collapsedRailWidth={72}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects activeColor (smoke: custom accent colour string mounts)', () => {
    // Failure: If this throws, activeColor is being used as a number rather than a CSS string.
    const tree = mount(
      <AzNavRail activeColor="#ff00ff">
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects enableRailDragging=true (smoke: drag-to-float flag mounts)', () => {
    // Failure: If this throws, enableRailDragging is being read as required rather than optional.
    const tree = mount(
      <AzNavRail enableRailDragging={true}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects helpEnabled=true with helpList map (smoke: help-overlay registry mounts)', () => {
    // Failure: If this throws, helpList is being indexed with a non-string key.
    const tree = mount(
      <AzNavRail helpEnabled={true} helpList={{ home: 'The home button' }}>
        <AzRailItem id="home" text="Home" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });

  it('respects packRailButtons=true (smoke: tight-pack layout mounts)', () => {
    // Failure: If this throws, packRailButtons is being applied as a numeric style.
    const tree = mount(
      <AzNavRail packRailButtons={true}>
        <AzRailItem id="home" text="Home" />
        <AzRailItem id="search" text="Search" />
      </AzNavRail>
    );
    expect(tree.toJSON()).toBeTruthy();
    renderer.act(() => tree.unmount());
  });
});
