import React from 'react';
import { AzNavItem, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps } from './types';
/** Internal React context that connects DSL item declarations to the parent `AzNavRail` registry. */
export declare const AzNavRailContext: React.Context<{
    register: (item: AzNavItem) => void;
    unregister: (id: string) => void;
    updateSettings: (settings: any) => void;
    getDividerId: () => string;
    hasItem: (id: string) => boolean;
} | null>;
/**
 * Declares a standard button item that appears in the collapsed rail (icon) view.
 *
 * @example
 * ```tsx
 * <AzNavRail>
 *   <AzRailItem id="home" text="Home" onClick={() => nav.push('/home')} />
 * </AzNavRail>
 * ```
 */
export declare const AzRailItem: React.FC<AzNavItemProps>;
/**
 * Declares a standard button item that appears only in the expanded menu, not the collapsed rail.
 *
 * @example
 * ```tsx
 * <AzNavRail>
 *   <AzMenuItem id="settings" text="Settings" onClick={openSettings} />
 * </AzNavRail>
 * ```
 */
export declare const AzMenuItem: React.FC<AzNavItemProps>;
/**
 * Declares a two-state toggle button visible in the collapsed rail.
 *
 * @example
 * ```tsx
 * <AzRailToggle
 *   id="dark"
 *   text="Theme"
 *   isChecked={isDark}
 *   toggleOnText="Dark"
 *   toggleOffText="Light"
 *   onClick={() => setIsDark(v => !v)}
 * />
 * ```
 */
export declare const AzRailToggle: React.FC<AzToggleProps>;
/**
 * Declares a two-state toggle button visible only in the expanded menu.
 *
 * @example
 * ```tsx
 * <AzMenuToggle
 *   id="notif"
 *   text="Notifications"
 *   isChecked={notifOn}
 *   toggleOnText="On"
 *   toggleOffText="Off"
 *   onClick={toggleNotif}
 * />
 * ```
 */
export declare const AzMenuToggle: React.FC<AzToggleProps>;
/**
 * Declares a cycler button in the collapsed rail that advances through a list of options on each tap.
 *
 * @example
 * ```tsx
 * <AzRailCycler
 *   id="qty"
 *   text="Quantity"
 *   options={['1', '2', '5', '10']}
 *   selectedOption={qty}
 *   onClick={() => setQty(nextOption(qty))}
 * />
 * ```
 */
export declare const AzRailCycler: React.FC<AzCyclerProps>;
/**
 * Declares a cycler button visible only in the expanded menu.
 *
 * @example
 * ```tsx
 * <AzMenuCycler
 *   id="speed"
 *   text="Speed"
 *   options={['Slow', 'Normal', 'Fast']}
 *   selectedOption={speed}
 *   onClick={cycleSpeed}
 * />
 * ```
 */
export declare const AzMenuCycler: React.FC<AzCyclerProps>;
/**
 * Inserts a horizontal divider line between items in the rail and menu.
 *
 * @example
 * ```tsx
 * <AzNavRail>
 *   <AzRailItem id="a" text="A" />
 *   <AzRailDivider />
 *   <AzRailItem id="b" text="B" />
 * </AzNavRail>
 * ```
 */
export declare const AzRailDivider: React.FC;
/**
 * Declares a collapsible group-header item in the collapsed rail; tapping it expands or collapses its sub-items.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="tools" text="Tools" />
 * <AzRailSubItem id="tool-a" text="Tool A" hostId="tools" onClick={openA} />
 * <AzRailSubItem id="tool-b" text="Tool B" hostId="tools" onClick={openB} />
 * ```
 */
export declare const AzRailHostItem: React.FC<AzHostItemProps>;
/**
 * Declares a collapsible group-header item visible only in the expanded menu.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="more" text="More options" />
 * <AzMenuSubItem id="opt1" text="Option 1" hostId="more" />
 * ```
 */
export declare const AzMenuHostItem: React.FC<AzHostItemProps>;
/**
 * Declares a standard button sub-item nested under a rail host item.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="tools" text="Tools" />
 * <AzRailSubItem id="hammer" text="Hammer" hostId="tools" onClick={useHammer} />
 * ```
 */
export declare const AzRailSubItem: React.FC<AzSubItemProps>;
/**
 * Declares a standard button sub-item visible only in the expanded menu under a host.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="more" text="More" />
 * <AzMenuSubItem id="about" text="About" hostId="more" onClick={openAbout} />
 * ```
 */
export declare const AzMenuSubItem: React.FC<AzSubItemProps>;
/**
 * Declares a sub-item that is itself a host (in the rail). It is a child of `hostId` — so it only
 * appears while that parent host is expanded — and, like any host, expands to reveal its own
 * sub-items (which target this item's `id` via their `hostId`). Hosts can nest to any depth.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="tools" text="Tools" />
 * <AzRailSubHostItem id="brushes" text="Brushes" hostId="tools" />
 * <AzRailSubItem id="pencil" text="Pencil" hostId="brushes" onClick={usePencil} />
 * ```
 */
export declare const AzRailSubHostItem: React.FC<AzSubItemProps>;
/**
 * Declares a sub-item that is itself a host (in the expanded menu). Behaves like `AzMenuSubItem` but
 * also expands to reveal its own sub-items. Hosts can nest to any depth.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="more" text="More" />
 * <AzMenuSubHostItem id="advanced" text="Advanced" hostId="more" />
 * <AzMenuSubItem id="flags" text="Feature flags" hostId="advanced" />
 * ```
 */
export declare const AzMenuSubHostItem: React.FC<AzSubItemProps>;
/**
 * Declares a toggle sub-item nested under a rail host item.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="adv" text="Advanced" />
 * <AzRailSubToggle
 *   id="debug"
 *   text="Debug"
 *   hostId="adv"
 *   isChecked={debug}
 *   toggleOnText="On"
 *   toggleOffText="Off"
 *   onClick={() => setDebug(v => !v)}
 * />
 * ```
 */
export declare const AzRailSubToggle: React.FC<AzSubToggleProps>;
/**
 * Declares a toggle sub-item visible only in the expanded menu under a host.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="prefs" text="Preferences" />
 * <AzMenuSubToggle
 *   id="sync"
 *   text="Sync"
 *   hostId="prefs"
 *   isChecked={sync}
 *   toggleOnText="Enabled"
 *   toggleOffText="Disabled"
 *   onClick={toggleSync}
 * />
 * ```
 */
export declare const AzMenuSubToggle: React.FC<AzSubToggleProps>;
/**
 * Declares a cycler sub-item nested under a rail host item.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="grp" text="Group" />
 * <AzRailSubCycler
 *   id="zoom"
 *   text="Zoom"
 *   hostId="grp"
 *   options={['50%', '100%', '200%']}
 *   selectedOption={zoom}
 *   onClick={cycleZoom}
 * />
 * ```
 */
export declare const AzRailSubCycler: React.FC<AzSubCyclerProps>;
/**
 * Declares a cycler sub-item visible only in the expanded menu under a host.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="display" text="Display" />
 * <AzMenuSubCycler
 *   id="theme"
 *   text="Theme"
 *   hostId="display"
 *   options={['Light', 'Dark', 'Auto']}
 *   selectedOption={theme}
 *   onClick={cycleTheme}
 * />
 * ```
 */
export declare const AzMenuSubCycler: React.FC<AzSubCyclerProps>;
/**
 * Declares a drag-reorderable sub-item in the rail; supports a long-press hidden menu and nested content.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="tabs" text="Tabs" />
 * <AzRailRelocItem
 *   id="tab-1"
 *   text="Tab 1"
 *   hostId="tabs"
 *   onRelocate={(from, to, order) => persistOrder(order)}
 *   hiddenMenu={[
 *     { text: 'Rename', onClick: rename },
 *     { text: 'Delete', onClick: del },
 *   ]}
 * />
 * ```
 */
export declare const AzRailRelocItem: React.FC<AzRailRelocItemProps>;
/** DSL component for overriding `AzNavRailSettings` values from within the child tree. */
export declare const AzSettings: React.FC<any>;
/** Alias for `AzSettings` — overrides theme-related rail settings from within the child tree. */
export declare const AzTheme: React.FC<any>;
/** Alias for `AzSettings` — overrides arbitrary rail config values from within the child tree. */
export declare const AzConfig: React.FC<any>;
/** Alias for `AzSettings` — overrides advanced rail settings from within the child tree. */
export declare const AzAdvanced: React.FC<any>;
/**
 * Declares a rail item that opens a secondary popup rail when tapped; child DSL items populate the popup.
 *
 * @example
 * ```tsx
 * <AzNestedRail id="filters" text="Filters" alignment={AzNestedRailAlignment.HORIZONTAL}>
 *   <AzRailItem id="filter-a" text="A" onClick={() => apply('A')} />
 *   <AzRailItem id="filter-b" text="B" onClick={() => apply('B')} />
 * </AzNestedRail>
 * ```
 */
export declare const AzNestedRail: React.FC<AzNestedRailProps>;
/**
 * Declares a help-only rail item — appears in the info overlay but not in the interactive rail.
 *
 * @example
 * ```tsx
 * <AzHelpRailItem
 *   id="swipe-hint"
 *   text="Swipe to undock"
 *   info="Swipe the rail right to convert it into a floating widget."
 * />
 * ```
 */
export declare const AzHelpRailItem: React.FC<AzNavItemProps>;
/**
 * Declares a help-only sub-item nested under a host; appears in the info overlay only and requires a valid `hostId`.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="settings" text="Settings" />
 * <AzHelpSubItem
 *   id="theme-help"
 *   text="Theme picker"
 *   hostId="settings"
 *   info="Choose between Light, Dark, and Auto modes."
 * />
 * ```
 */
export declare const AzHelpSubItem: React.FC<AzSubItemProps>;
//# sourceMappingURL=AzNavRailScope.d.ts.map