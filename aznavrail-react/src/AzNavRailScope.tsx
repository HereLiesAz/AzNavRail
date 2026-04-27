import React, { useEffect, useContext, useRef, useState, useMemo } from 'react';
import { AzNavItem, AzButtonShape, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps, AzNestedRailAlignment, HiddenMenuScope, AzItemConfig } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';

export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
  registerCallback: (id: string, type: 'onClick' | 'onRelocate' | 'onHiddenMenuClick' | 'onHiddenMenuValueChange', fn: Function, extraId?: string) => void;
  generateDividerId: () => string;
} | null>(null);

const useAzItem = (rawItem: AzNavItem) => {
  const context = useContext(AzNavRailContext);
  const previousItem = useRef<AzNavItem | null>(null);

  if (rawItem.isCycler) {
      if (rawItem.options && rawItem.selectedOption !== undefined && !rawItem.options.includes(rawItem.selectedOption)) {
          throw new Error(`Cycler configuration error for item='${rawItem.text}': selectedOption='${rawItem.selectedOption}' is not in options array.`);
      }
      if (rawItem.menuOptions && rawItem.options && rawItem.options.length !== rawItem.menuOptions.length) {
          throw new Error(`Cycler configuration mismatch for item='${rawItem.text}': optionsSize=${rawItem.options.length}, menuOptionsSize=${rawItem.menuOptions.length}`);
      }
  }

  const item = {
      ...rawItem,
      text: rawItem.text ?? '',
      screenTitle: rawItem.screenTitle === AzNavRailDefaults.NO_TITLE ? undefined : (rawItem.screenTitle ?? rawItem.text ?? '')
  };

  useEffect(() => {
    if (!context) return;

    if (item.onClick) context.registerCallback(item.id, 'onClick', item.onClick);
    if (item.onRelocate) context.registerCallback(item.id, 'onRelocate', item.onRelocate);

    if (item.hiddenMenu) {
        item.hiddenMenu.forEach(hItem => {
            if (hItem.onClick) context.registerCallback(item.id, 'onHiddenMenuClick', hItem.onClick, hItem.id);
            if (hItem.onValueChange) context.registerCallback(item.id, 'onHiddenMenuValueChange', hItem.onValueChange, hItem.id);
        });
    }

    // Strip functions before state registration
    const dataItem = { ...item };
    delete dataItem.onClick;
    delete dataItem.onRelocate;
    if (dataItem.hiddenMenu) {
        dataItem.hiddenMenu = dataItem.hiddenMenu.map(h => {
            const hCopy = { ...h };
            delete hCopy.onClick;
            delete hCopy.onValueChange;
            return hCopy;
        });
    }

    // Simple comparison to avoid spamming updates
    const prev = previousItem.current;
    const isSame = prev &&
                   prev.id === dataItem.id &&
                   prev.text === dataItem.text &&
                   prev.disabled === dataItem.disabled &&
                   prev.isChecked === dataItem.isChecked &&
                   prev.selectedOption === dataItem.selectedOption &&
                   prev.menuText === dataItem.menuText &&
                   prev.menuToggleOnText === dataItem.menuToggleOnText &&
                   prev.menuToggleOffText === dataItem.menuToggleOffText &&
                   prev.textColor === dataItem.textColor &&
                   prev.fillColor === dataItem.fillColor &&
                   // Compare arrays
                   JSON.stringify(prev.options) === JSON.stringify(dataItem.options) &&
                   JSON.stringify(prev.menuOptions) === JSON.stringify(dataItem.menuOptions) &&
                   prev.shape === dataItem.shape &&
                   prev.color === dataItem.color &&
                   prev.info === dataItem.info &&
                   prev.isRelocItem === dataItem.isRelocItem &&
                   // Reloc props
                   JSON.stringify(prev.hiddenMenu) === JSON.stringify(dataItem.hiddenMenu);

    if (!isSame) {
        context.register(dataItem);
        previousItem.current = dataItem;
    }

    // Cleanup only on unmount
    return () => {
      // We don't unregister on every update, only on unmount
      // But if ID changes (rare), we should unregister old ID.
    };
  }, [
    context,
    item.id,
    item.text,
    item.disabled,
    item.isChecked,
    item.selectedOption,
    item.menuText,
    item.menuToggleOnText,
    item.menuToggleOffText,
    item.textColor,
    item.fillColor,
    item.options,
    item.menuOptions,
    item.shape,
    item.color,
    item.info,
    item.isRelocItem,
    item.hiddenMenu,
    item.onClick,
    item.onRelocate,
    item.isExpanded,
    item.nestedRailAlignment,
    item.nestedRailItems,
    item.isNestedRail,
    item.isHost,
    item.hostId,
    item.isSubItem
  ]);

  useEffect(() => {
      if (context) {
          return () => context.unregister(item.id);
      }
      return undefined;
  }, [context, item.id]);

  return null;
};

// --- Component Wrappers ---

export const AzRailItem: React.FC<AzNavItemProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzMenuItem: React.FC<AzNavItemProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzRailToggle: React.FC<AzToggleProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
  });
  return null;
};

export const AzMenuToggle: React.FC<AzToggleProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: true,
    isCycler: false,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
  });
  return null;
};

export const AzRailCycler: React.FC<AzCyclerProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: true,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzMenuCycler: React.FC<AzCyclerProps> = (props) => {
  useAzItem({
    ...props,
    isRailItem: false,
    isToggle: false,
    isCycler: true,
    isDivider: false,
    collapseOnClick: true,
    shape: props.shape ?? AzButtonShape.CIRCLE,
    disabled: props.disabled ?? false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzDivider: React.FC = () => {
    const context = useContext(AzNavRailContext);
    const id = useRef(context?.generateDividerId() ?? `divider-${Math.random()}`).current;
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
        toggleOffText: '',
    });
    return null;
};

export const AzRailHostItem: React.FC<AzHostItemProps> = (props) => {
    useAzItem({
        ...props,
        isRailItem: true,
        isHost: true,
        isSubItem: false,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false, // Hosts expand/collapse, don't close rail
        shape: props.shape ?? AzButtonShape.CIRCLE,
        disabled: props.disabled ?? false,
        isExpanded: false, // Initial state, managed by parent usually
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuHostItem: React.FC<AzHostItemProps> = (props) => {
    useAzItem({
        ...props,
        isRailItem: false,
        isHost: true,
        isSubItem: false,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape ?? AzButtonShape.CIRCLE,
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailSubItem: React.FC<AzSubItemProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuSubItem: React.FC<AzSubItemProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailSubToggle: React.FC<AzSubToggleProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
    });
    return null;
};

export const AzMenuSubToggle: React.FC<AzSubToggleProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
    });
    return null;
};

export const AzRailSubCycler: React.FC<AzSubCyclerProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzMenuSubCycler: React.FC<AzSubCyclerProps> = (props) => {
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
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailRelocItem: React.FC<AzRailRelocItemProps & { keepNestedRailOpen?: boolean }> = (props) => {
    let hiddenMenuItems: any[] = [];
    if (props.hiddenMenu) {
        if (typeof props.hiddenMenu === 'function') {
            const scope: HiddenMenuScope = {
                parentId: props.id,
                listItem: (text, action) => {
                    if (typeof action === 'string') {
                        hiddenMenuItems.push({ id: `${props.id}_hidden_${hiddenMenuItems.length}`, text, route: action });
                    } else {
                        hiddenMenuItems.push({ id: `${props.id}_hidden_${hiddenMenuItems.length}`, text, onClick: action });
                    }
                },
                inputItem: (hint: string, arg2: any, arg3?: any) => {
                    let initialValue = '';
                    let onValueChange: (value: string) => void;

                    if (typeof arg2 === 'string') {
                        initialValue = arg2;
                        if (typeof arg3 !== 'function') {
                            console.warn("inputItem requires an onValueChange function callback.");
                            onValueChange = () => {};
                        } else {
                            items.push({ id: `${props.id}_hidden_item_${items.length}`, text, onClick: action });
                        }
                    },
                    inputItem: (hint: string, arg2: any, arg3?: any) => {
                        let initialValue = '';
                        let onValueChange: (value: string) => void;

                        if (typeof arg2 === 'string') {
                            initialValue = arg2;
                            if (typeof arg3 !== 'function') {
                                throw new Error("inputItem requires an onValueChange function callback.");
                            }
                            onValueChange = arg3;
                        } else if (typeof arg2 === 'function') {
                            onValueChange = arg2;
                        } else {
                            throw new Error("inputItem requires an onValueChange function callback.");
                        }
                        items.push({ id: `${props.id}_hidden_item_${items.length}`, text: '', isInput: true, hint, initialValue, onValueChange });
                    }
                    hiddenMenuItems.push({ id: `${props.id}_input_${hiddenMenuItems.length}`, text: '', isInput: true, hint, initialValue, onValueChange });
                }
            };
            props.hiddenMenu(scope);
        } else {
            hiddenMenuItems = props.hiddenMenu.map((item, i) => ({
                id: `${props.id}_hidden_${i}`,
                text: item.text,
                onClick: item.onClick
            }));
        }
        return items;
    }, [props.hiddenMenu, props.id]);

    const parentContext = useContext(AzNavRailContext);
    const [nestedItems, setNestedItems] = useState<AzNavItem[]>([]);

    const localContext = useMemo(() => {
        if (!parentContext) return null;
        return {
            ...parentContext,
            register: (item: AzNavItem) => {
                setNestedItems(prev => {
                    const idx = prev.findIndex(i => i.id === item.id);
                    if (idx >= 0) {
                        const newItems = [...prev];
                        newItems[idx] = item;
                        return newItems;
                    }
                    return [...prev, item];
                });
            },
            unregister: (id: string) => {
                setNestedItems(prev => prev.filter(i => i.id !== id));
            }
        };
    }, [parentContext]);

    useAzItem({
        ...props,
        text: props.text ?? '',
        isRailItem: true,
        isHost: false,
        isSubItem: true,
        isRelocItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape ?? AzButtonShape.NONE,
        disabled: props.disabled ?? false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        hiddenMenu: hiddenMenuItems,
        forceHiddenMenuOpen: props.forceHiddenMenuOpen,
        onHiddenMenuDismiss: props.onHiddenMenuDismiss,
        nestedRailAlignment: props.nestedRailAlignment ?? AzNestedRailAlignment.VERTICAL,
        isNestedRail: !!props.nestedContent,
        nestedRailItems: props.nestedContent ? nestedItems : undefined,
        keepNestedRailOpen: props.keepNestedRailOpen ?? false
    } as any);

    return (
        <AzNavRailContext.Provider value={localContext}>
            {props.nestedContent}
        </AzNavRailContext.Provider>
    );
};

const useShallowCompareSettings = (props: any) => {
    const context = useContext(AzNavRailContext);
    const prevProps = useRef<any>(null);
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

export const AzSettings: React.FC<any> = (props) => {
    useShallowCompareSettings(props);
    return null;
};

export const AzTheme: React.FC<any> = (props) => {
    useShallowCompareSettings(props);
    return null;
};

export const AzConfig: React.FC<any> = (props) => {
    useShallowCompareSettings(props);
    return null;
};

export const AzAdvanced: React.FC<any> = (props) => {
    useShallowCompareSettings(props);
    return null;
};

export const AzNestedRail: React.FC<AzNestedRailProps & { keepNestedRailOpen?: boolean }> = (props) => {
    const parentContext = useContext(AzNavRailContext);
    const [nestedItems, setNestedItems] = useState<AzNavItem[]>([]);

    const localContext = useMemo(() => {
        if (!parentContext) return null;
        return {
            ...parentContext,
            register: (item: AzNavItem) => {
                setNestedItems(prev => {
                    const idx = prev.findIndex(i => i.id === item.id);
                    if (idx >= 0) {
                        const newItems = [...prev];
                        newItems[idx] = item;
                        return newItems;
                    }
                    return [...prev, item];
                });
            },
            unregister: (id: string) => {
                setNestedItems(prev => prev.filter(i => i.id !== id));
            }
        };
    }, [parentContext]);

    useAzItem({
        ...props,
        text: props.text ?? '',
        isRailItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape ?? AzButtonShape.CIRCLE,
        disabled: props.disabled ?? false,
        isHost: false,
        isSubItem: false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        isNestedRail: true,
        nestedRailAlignment: props.alignment ?? AzNestedRailAlignment.VERTICAL,
        nestedRailItems: nestedItems,
        keepNestedRailOpen: props.keepNestedRailOpen ?? false
    } as any);

    return (
        <AzNavRailContext.Provider value={localContext}>
            {props.children}
        </AzNavRailContext.Provider>
    );
};

export const AzHelpRailItem: React.FC<AzNavItemProps> = (props) => {
    useAzItem({
        ...props,
        isRailItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        shape: props.shape ?? AzButtonShape.CIRCLE,
        disabled: props.disabled ?? false,
        isHost: false,
        isSubItem: false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        isHelpItem: true
    });
    return null;
};

export const AzHelpSubItem: React.FC<AzSubItemProps> = (props) => {
    const context = useContext(AzNavRailContext);

    useEffect(() => {
        if (context && props.hostId) {
            if (!context.hasItem(props.hostId)) {
                // To mirror Kotlin's IllegalArgumentException and prevent silent dropping
                console.error(`AzHelpSubItem error: Host ID '${props.hostId}' not found in registry.`);
            }
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
        shape: props.shape ?? AzButtonShape.NONE,
        disabled: props.disabled ?? false,
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
