import { computeAutoEdges } from '../guidance/AzGuidance';

/** Minimal fields `computeAutoEdges` actually reads; cast past the rest of `AzNavItem`. */
const item = (overrides: Record<string, unknown>) =>
  ({
    id: 'x',
    text: 'X',
    isRailItem: false,
    isDivider: false,
    isHost: false,
    isNestedRail: false,
    ...overrides,
  }) as any;

describe('computeAutoEdges', () => {
  it('gates a rail item on app-ready', () => {
    const edges = computeAutoEdges([
      item({ id: 'home', text: 'Home', isRailItem: true, route: 'home' }),
    ]);
    const e = edges.find((e) => e.to === 'az.screen.home');
    expect(e?.from).toBe('az.app.ready');
  });

  it('gates a plain (non-host) menu item on the menu being open', () => {
    const edges = computeAutoEdges([
      item({ id: 'settings', text: 'Settings', route: 'settings' }),
    ]);
    const e = edges.find((e) => e.to === 'az.screen.settings');
    expect(e?.from).toBe('az.rail.expanded');
  });

  it('arms a host on the menu being open, same as any other top-level item', () => {
    const edges = computeAutoEdges([
      item({ id: 'group', text: 'Group', isHost: true }),
    ]);
    const e = edges.find((e) => e.to === 'az.host.group.expanded');
    expect(e?.from).toBe('az.rail.expanded');
  });

  it('gates a sub-item on its own host being expanded, not merely the menu being open', () => {
    // Regression: a sub-item used to inherit the same `az.rail.expanded` gate as any other menu
    // item, so the guidance overlay would route a tutorial step (or spotlight) to it as soon as the
    // menu opened — while it was still hidden inside its still-collapsed host and not on screen.
    const edges = computeAutoEdges([
      item({ id: 'group', text: 'Group', isHost: true }),
      item({
        id: 'child',
        text: 'Child',
        hostId: 'group',
        isSubItem: true,
        route: 'child',
      }),
    ]);
    const e = edges.find((e) => e.to === 'az.screen.child');
    expect(e?.from).toBe('az.host.group.expanded');
  });

  it('gates a sub-item on its host even when the sub-item is itself a rail item', () => {
    // A host's children can render inline in the collapsed strip once the host is expanded — but
    // that inline rendering is still gated by the same host-expanded state, not by app-ready alone.
    const edges = computeAutoEdges([
      item({ id: 'group', text: 'Group', isHost: true }),
      item({
        id: 'child',
        text: 'Child',
        hostId: 'group',
        isSubItem: true,
        isRailItem: true,
        route: 'child',
      }),
    ]);
    const e = edges.find((e) => e.to === 'az.screen.child');
    expect(e?.from).toBe('az.host.group.expanded');
  });
});
