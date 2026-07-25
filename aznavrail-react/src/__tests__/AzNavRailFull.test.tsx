import React from 'react';
import { StyleSheet, Text } from 'react-native';
import { render, fireEvent } from '@testing-library/react-native';
import { AzNavRail } from '../AzNavRail';
// `AzNavHostContext` was removed in the React-port refactor; tests now mount AzNavRail directly.
import { AzRailHostItem, AzRailSubItem } from '../AzNavRailScope';
import { AzButtonShape, AzNavItem } from '../types';
import { RelocItemHandler } from '../util/RelocItemHandler';

describe('AzNavRail Full Suite', () => {
  beforeEach(async () => {
    jest.useFakeTimers();
  });

  afterEach(async () => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  it('renders loading overlay at root level when isLoading is true', async () => {
    const { root, unmount } = await render(
      <AzNavRail isLoading={true}>
        <Text>Content</Text>
      </AzNavRail>
    );

    // Find view with zIndex 10000
    const loader = root!.queryAll(
      (node) =>
        node.type === 'View' &&
        !!node.props.style &&
        node.props.style.zIndex === 10000
    );
    expect(loader.length).toBe(1);

    await unmount();
  });

  // This used to reach into the composite `DraggableRailItemWrapper` and invoke its `onDragMove` /
  // `onDragEnd` props by hand — a capability the test renderer no longer offers, and one that never
  // exercised a real gesture anyway. The computation it was actually asserting lives in
  // `RelocItemHandler`, which is pure and can simply be called.
  it('handles RelocItemHandler drag and drop reordering', async () => {
    const items = [
      { id: 'host', text: 'Host', isHost: true },
      { id: 'reloc1', text: 'Reloc1', hostId: 'host', isRelocItem: true },
      { id: 'reloc2', text: 'Reloc2', hostId: 'host', isRelocItem: true },
    ] as AzNavItem[];

    const cluster = RelocItemHandler.getCluster(items, 'host');
    expect(cluster.map((i) => i.id)).toEqual(['reloc1', 'reloc2']);

    // Dragging the first item down by one slot height (48 + 8) targets cluster index 1.
    const draggedClusterIndex = 0;
    const targetClusterIndex = RelocItemHandler.calculateTargetIndex(
      60,
      draggedClusterIndex,
      cluster.length,
      56
    );
    expect(targetClusterIndex).toBe(1);

    const reordered = RelocItemHandler.reorderItems(
      items,
      'reloc1',
      'host',
      targetClusterIndex
    );
    const newOrder = RelocItemHandler.getCluster(reordered, 'host').map(
      (i) => i.id
    );
    expect(newOrder).toEqual(['reloc2', 'reloc1']);

    // ...which is exactly what the rail reports to a reloc item's `onRelocate`.
    expect([draggedClusterIndex, targetClusterIndex, newOrder]).toEqual([
      0,
      1,
      ['reloc2', 'reloc1'],
    ]);
  });

  it('enforces NONE shape for SubItems regardless of props', async () => {
    const { getByTestId, unmount } = await render(
      <AzNavRail initiallyExpanded={false}>
        <AzRailHostItem id="host" text="Host" />
        <AzRailSubItem
          id="sub"
          hostId="host"
          text="Sub"
          shape={AzButtonShape.SQUARE}
        />
      </AzNavRail>
    );

    // Expand host
    await fireEvent.press(getByTestId('host'));

    // A SQUARE was asked for; NONE is what a sub-item gets — no border at all. The shape lives on
    // the container View around the pressable, not on the pressable itself.
    const shape = StyleSheet.flatten(
      getByTestId('sub').parent!.props.style
    ) as any;
    expect(shape.borderWidth).toBe(0);
    expect(shape.borderColor).toBe('transparent');

    await unmount();
  });
});
