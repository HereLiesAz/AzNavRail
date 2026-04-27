import React from 'react';
import renderer from 'react-test-renderer';
import { Text } from 'react-native';
import { AzNavRail, AzNavHostContext } from '../AzNavRail';
import { AzRailHostItem, AzRailSubItem, AzRailRelocItem } from '../AzNavRailScope';
import { AzButtonShape } from '../types';
import { AzButton } from '../components/AzButton';
import { DraggableRailItemWrapper } from '../components/DraggableRailItemWrapper';

describe('AzNavRail Full Suite', () => {
  beforeEach(() => {
      jest.useFakeTimers();
  });

  afterEach(() => {
      jest.useRealTimers();
      jest.clearAllMocks();
  });

  it('renders loading overlay at root level when isLoading is true', () => {
    let component: renderer.ReactTestRenderer = {} as any;
    renderer.act(() => {
component = renderer.create(
        <AzNavHostContext.Provider value={true}>
<AzNavRail isLoading={true}>
          <Text>Content</Text>
        </AzNavRail>
</AzNavHostContext.Provider>
      );
});
    const root = component.root;

    // Find view with zIndex 10000
    const loader = root.findAll((node: any) =>
        node.type === 'View' &&
        node.props.style &&
        node.props.style.zIndex === 10000
    );
    expect(loader.length).toBe(1);

    // Explicitly unmount to avoid warnings
    renderer.act(() => {
      component.unmount();
    });
  });

  it('handles RelocItemHandler drag and drop reordering', () => {
    const mockOnRelocate = jest.fn();

    let component: renderer.ReactTestRenderer = {} as any;
    renderer.act(() => {
component = renderer.create(
          <AzNavHostContext.Provider value={true}>
<AzNavRail initiallyExpanded={false}>
            <AzRailHostItem id="host" text="Host" />
            <AzRailRelocItem id="reloc1" hostId="host" text="Reloc1" onRelocate={mockOnRelocate} />
            <AzRailRelocItem id="reloc2" hostId="host" text="Reloc2" />
          </AzNavRail>
</AzNavHostContext.Provider>
        );
});

    const root = component.root;
    const buttons = root.findAllByType(AzButton);
    const hostBtn = buttons.find((b) => b.props.text === 'Host');

    // Expand host
    renderer.act(() => {
        hostBtn?.props.onClick();
    });

    const wrappers = root.findAllByType(DraggableRailItemWrapper);
    expect(wrappers.length).toBe(2);

    // Get the first item's wrapper (reloc1)
    const firstWrapper = wrappers.find((w) => w.props.item.id === 'reloc1');

    // Find its index in the items array. The wrapper gets index from mapping over `effectiveRailItems`
    const index = firstWrapper?.props.index;

    renderer.act(() => {
        // Simulate dragging down past the second item (dy = 60)
        firstWrapper?.props.onDragMove(60, index);

        // Simulate drop
        firstWrapper?.props.onDragEnd(index);
    });

    // Verify mockOnRelocate was called with:
    // draggedClusterIndex (0), targetClusterIndex (1), newOrder (['reloc2', 'reloc1'])
    expect(mockOnRelocate).toHaveBeenCalledWith(0, 1, ['reloc2', 'reloc1']);

    // Explicitly unmount to avoid warnings
    renderer.act(() => {
      component.unmount();
    });
  });

  it('enforces NONE shape for SubItems regardless of props', () => {
    let component: renderer.ReactTestRenderer = {} as any;
    renderer.act(() => {
component = renderer.create(
          <AzNavHostContext.Provider value={true}>
<AzNavRail initiallyExpanded={false}>
            <AzRailHostItem id="host" text="Host" />
            <AzRailSubItem id="sub" hostId="host" text="Sub" shape={AzButtonShape.SQUARE} />
          </AzNavRail>
</AzNavHostContext.Provider>
        );
});

    const root = component.root;
    const buttons = root.findAllByType(AzButton);
    const hostBtn = buttons.find((b: any) => b.props.text === 'Host');

    // Expand host
    renderer.act(() => {
        hostBtn?.props.onClick();
    });

    const updatedButtons = root.findAllByType(AzButton);
    const subBtn = updatedButtons.find((b: any) => b.props.text === 'Sub');

    expect(subBtn).toBeDefined();
    expect(subBtn?.props.shape).toBe(AzButtonShape.NONE);

    // Explicitly unmount to avoid warnings
    renderer.act(() => {
      component.unmount();
    });
  });
});
