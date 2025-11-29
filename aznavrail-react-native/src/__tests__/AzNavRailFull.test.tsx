import React from 'react';
import renderer from 'react-test-renderer';
import { Text } from 'react-native';
import { AzNavRail } from '../AzNavRail';
import { AzRailHostItem, AzRailSubItem } from '../AzNavRailScope';
import { AzButtonShape } from '../types';
import { AzButton } from '../components/AzButton';



describe('AzNavRail Full Suite', () => {
  beforeEach(() => {
      jest.useFakeTimers();
  });

  it('renders loading overlay at root level when isLoading is true', () => {
    const component = renderer.create(
      <AzNavRail isLoading={true}>
        <Text>Content</Text>
      </AzNavRail>
    );
    const root = component.root;

    // Find view with zIndex 10000
    const loader = root.findAll((node: any) =>
        node.type === 'View' &&
        node.props.style &&
        node.props.style.zIndex === 10000
    );
    expect(loader.length).toBe(1);
  });

  it('enforces NONE shape for SubItems regardless of props', async () => {
    let component: renderer.ReactTestRenderer | undefined;
    await renderer.act(async () => {
        component = renderer.create(
          <AzNavRail initiallyExpanded={false}>
            <AzRailHostItem id="host" text="Host" />
            <AzRailSubItem id="sub" hostId="host" text="Sub" shape={AzButtonShape.SQUARE} />
          </AzNavRail>
        );
    });

    if (!component) throw new Error('Component not created');

    const root = component.root;
    const buttons = root.findAllByType(AzButton);
    const hostBtn = buttons.find((b: any) => b.props.text === 'Host');
    if (!hostBtn) throw new Error('Host button not found');

    // Expand host
    await renderer.act(async () => {
        hostBtn.props.onClick();
    });

    const updatedButtons = root.findAllByType(AzButton);
    const subBtn = updatedButtons.find((b: any) => b.props.text === 'Sub');

    if (!subBtn) throw new Error('Sub button not found');

    expect(subBtn).toBeDefined();
    expect(subBtn.props.shape).toBe(AzButtonShape.NONE);
  });
});
