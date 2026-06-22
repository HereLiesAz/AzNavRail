import React, { useEffect, useContext, useRef } from 'react';
import { AzButtonShape, AzNestedRailAlignment } from './types';

/** Internal React context that connects DSL item declarations to the parent `AzNavRail` registry. */
export const AzNavRailContext = /*#__PURE__*/React.createContext(null);
const useAzItem = item => {
  const context = useContext(AzNavRailContext);
  const previousItem = useRef(null);
  useEffect(() => {
    if (!context) return;

    // Simple comparison to avoid spamming updates
    const prev = previousItem.current;
    const isSame = prev && prev.id === item.id && prev.text === item.text && prev.disabled === item.disabled && prev.isChecked === item.isChecked && prev.selectedOption === item.selectedOption && prev.menuText === item.menuText && prev.menuToggleOnText === item.menuToggleOnText && prev.menuToggleOffText === item.menuToggleOffText && prev.textColor === item.textColor && prev.fillColor === item.fillColor &&
    // Compare arrays
    JSON.stringify(prev.options) === JSON.stringify(item.options) && JSON.stringify(prev.menuOptions) === JSON.stringify(item.menuOptions) && prev.shape === item.shape && prev.color === item.color && prev.info === item.info && prev.isRelocItem === item.isRelocItem &&
    // Reloc props
    JSON.stringify(prev.hiddenMenu) === JSON.stringify(item.hiddenMenu) && prev.onRelocate === item.onRelocate;
    if (!isSame) {
      context.register(item);
      previousItem.current = item;
    }

    // Cleanup only on unmount
    return () => {
      // We don't unregister on every update, only on unmount
      // But if ID changes (rare), we should unregister old ID.
    };
  }, [context, item]); // dependencies should capture all props

  useEffect(() => {
    if (!context) return undefined;
    // Defer the unregister to a microtask so it doesn't re-enter React's commit phase
    // while the parent `AzNavRail` is itself unmounting. The synchronous form caused a
    // "Maximum update depth exceeded" inside react-test-renderer because `unregister`'s
    // `setItems(prev => prev.filter(...))` was scheduled while the host fiber was still
    // processing passive-effect cleanups for the same tree.
    //
    // We snapshot `context` and `item.id` so the deferred callback isn't affected by a
    // later effect re-run with a different id.
    const ctx = context;
    const id = item.id;
    return () => {
      if (typeof queueMicrotask === 'function') {
        queueMicrotask(() => ctx.unregister(id));
      } else {
        // queueMicrotask is missing in very old environments; fall back to a 0ms timeout.
        setTimeout(() => ctx.unregister(id), 0);
      }
    };
  }, [context, item.id]);
  return null;
};

// --- Component Wrappers ---

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
export const AzRailItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzMenuItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailToggle = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false
  });
  return null;
};

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
export const AzMenuToggle = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false
  });
  return null;
};

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
export const AzRailCycler = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzMenuCycler = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailDivider = () => {
  const context = useContext(AzNavRailContext);
  const id = useRef(null);
  if (!id.current && context) {
    id.current = context.getDividerId();
  }
  useAzItem({
    id: id.current || '',
    text: '',
    isRailItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: true,
    collapseOnClick: false,
    shape: AzButtonShape.NONE,
    disabled: false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailHostItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: true,
    isSubItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    // Hosts expand/collapse, don't close rail
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    // Initial state, managed by parent usually
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

/**
 * Declares a collapsible group-header item visible only in the expanded menu.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="more" text="More options" />
 * <AzMenuSubItem id="opt1" text="Option 1" hostId="more" />
 * ```
 */
export const AzMenuHostItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: true,
    isSubItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

/**
 * Declares a standard button sub-item nested under a rail host item.
 *
 * @example
 * ```tsx
 * <AzRailHostItem id="tools" text="Tools" />
 * <AzRailSubItem id="hammer" text="Hammer" hostId="tools" onClick={useHammer} />
 * ```
 */
export const AzRailSubItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

/**
 * Declares a standard button sub-item visible only in the expanded menu under a host.
 *
 * @example
 * ```tsx
 * <AzMenuHostItem id="more" text="More" />
 * <AzMenuSubItem id="about" text="About" hostId="more" onClick={openAbout} />
 * ```
 */
export const AzMenuSubItem = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailSubHostItem = props => {
  if (props.hostId === props.id) {
    console.error(`AzRailSubHostItem error: id '${props.id}' references itself as its own hostId.`);
  }
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: true,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    // Hosts expand/collapse, don't close the rail
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzMenuSubHostItem = props => {
  if (props.hostId === props.id) {
    console.error(`AzMenuSubHostItem error: id '${props.id}' references itself as its own hostId.`);
  }
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: true,
    isSubItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailSubToggle = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false
  });
  return null;
};

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
export const AzMenuSubToggle = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false
  });
  return null;
};

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
export const AzRailSubCycler = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzMenuSubCycler = props => {
  useAzItem({
    ...props,
    isRailItem: false,
    isHost: false,
    isSubItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: ''
  });
  return null;
};

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
export const AzRailRelocItem = props => {
  let hiddenMenuItems = [];
  if (props.hiddenMenu) {
    if (typeof props.hiddenMenu === 'function') {
      const scope = {
        listItem: (text, action) => {
          if (typeof action === 'string') {
            hiddenMenuItems.push({
              id: `${props.id}_hidden_item_${hiddenMenuItems.length}`,
              text,
              route: action
            });
          } else {
            hiddenMenuItems.push({
              id: `${props.id}_hidden_item_${hiddenMenuItems.length}`,
              text,
              onClick: action
            });
          }
        },
        inputItem: (hint, arg2, arg3) => {
          let initialValue = '';
          let onValueChange;
          if (typeof arg2 === 'string') {
            initialValue = arg2;
            if (typeof arg3 !== 'function') {
              console.warn("inputItem requires an onValueChange function callback.");
              onValueChange = () => {};
            } else {
              onValueChange = arg3;
            }
          } else if (typeof arg2 === 'function') {
            onValueChange = arg2;
          } else {
            console.warn("inputItem requires an onValueChange function callback.");
            onValueChange = () => {};
          }
          hiddenMenuItems.push({
            id: `${props.id}_hidden_input_${hiddenMenuItems.length}`,
            text: '',
            isInput: true,
            hint,
            initialValue,
            onValueChange
          });
        }
      };
      props.hiddenMenu(scope);
    } else {
      hiddenMenuItems = props.hiddenMenu.map((item, i) => ({
        id: `${props.id}_hidden_item_${i}`,
        text: item.text,
        onClick: item.onClick
      }));
    }
  }
  useAzItem({
    ...props,
    text: props.text || '',
    isRailItem: true,
    isHost: false,
    isSubItem: true,
    isRelocItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    shape: props.shape || AzButtonShape.NONE,
    disabled: props.disabled || false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
    hiddenMenu: hiddenMenuItems,
    forceHiddenMenuOpen: props.forceHiddenMenuOpen,
    onHiddenMenuDismiss: props.onHiddenMenuDismiss,
    nestedRailAlignment: props.nestedRailAlignment || AzNestedRailAlignment.VERTICAL
  });
  return /*#__PURE__*/React.createElement(AzNavRailContext.Consumer, null, ctx => /*#__PURE__*/React.createElement(AzNavRailContext.Provider, {
    value: ctx
  }, props.nestedContent));
};
const useShallowCompareSettings = props => {
  const context = useContext(AzNavRailContext);
  const prevProps = useRef(null);
  useEffect(() => {
    if (!context) return;
    const prev = prevProps.current;
    let isSame = true;
    if (!prev) {
      isSame = false;
    } else {
      const keys1 = Object.keys(props);
      const keys2 = Object.keys(prev);
      if (keys1.length !== keys2.length) {
        isSame = false;
      } else {
        for (const key of keys1) {
          if (props[key] !== prev[key]) {
            isSame = false;
            break;
          }
        }
      }
    }
    if (!isSame) {
      context.updateSettings(props);
      prevProps.current = props;
    }
  }, [context, props]);
};

/** DSL component for overriding `AzNavRailSettings` values from within the child tree. */
export const AzSettings = props => {
  useShallowCompareSettings(props);
  return null;
};

/** Alias for `AzSettings` — overrides theme-related rail settings from within the child tree. */
export const AzTheme = props => {
  useShallowCompareSettings(props);
  return null;
};

/** Alias for `AzSettings` — overrides arbitrary rail config values from within the child tree. */
export const AzConfig = props => {
  useShallowCompareSettings(props);
  return null;
};

/** Alias for `AzSettings` — overrides advanced rail settings from within the child tree. */
export const AzAdvanced = props => {
  useShallowCompareSettings(props);
  return null;
};

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
export const AzNestedRail = props => {
  const [nestedSettings, setNestedSettings] = React.useState({});
  useAzItem({
    ...props,
    text: props.text || '',
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: false,
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
    isNestedRail: true,
    nestedRailAlignment: props.alignment || AzNestedRailAlignment.VERTICAL,
    nestedRailSettings: nestedSettings
  });
  return /*#__PURE__*/React.createElement(AzNavRailContext.Consumer, null, ctx => {
    if (!ctx) return null;
    const isolatedContext = {
      ...ctx,
      updateSettings: newSettings => {
        setNestedSettings(prev => {
          const merged = {
            ...prev,
            ...newSettings
          };
          // Shallow check to avoid infinite loops
          const keys = Object.keys(merged);
          const changed = keys.some(k => prev[k] !== merged[k]);
          return changed ? merged : prev;
        });
      }
    };
    return /*#__PURE__*/React.createElement(AzNavRailContext.Provider, {
      value: isolatedContext
    }, props.children);
  });
};

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
export const AzHelpRailItem = props => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
    isHelpItem: true,
    shape: props.shape || AzButtonShape.NONE,
    disabled: props.disabled || false
  });
  return null;
};

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
export const AzHelpSubItem = props => {
  const context = useContext(AzNavRailContext);
  useEffect(() => {
    if (context && props.hostId && !context.hasItem(props.hostId)) {
      // To mirror Kotlin's IllegalArgumentException and prevent silent dropping
      console.error(`AzHelpSubItem error: Host ID '${props.hostId}' not found in registry.`);
    }
  }, [context, props.hostId]);

  // Validate hostId contextually via useAzItem or parent scope,
  // Note: hostId validation on the web/rn side is handled by AzNavRail directly linking items.
  // For parity we strictly enforce passing hostId.
  if (!props.hostId) {
    console.warn("AzHelpSubItem requires a valid hostId");
  }
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    isHost: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
    isHelpItem: true,
    isSubItem: true,
    shape: props.shape || AzButtonShape.NONE,
    disabled: props.disabled || false
  });
  return null;
};
//# sourceMappingURL=AzNavRailScope.js.map