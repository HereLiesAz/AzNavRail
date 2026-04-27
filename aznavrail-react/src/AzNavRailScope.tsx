import React, { useEffect, useContext, useRef, useId, useState } from 'react';
import { AzNavItem, AzButtonShape, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps, AzNestedRailAlignment, HiddenMenuScope, AzItemConfig } from './types';
import { AzNavRailDefaults } from './AzNavRailDefaults';

export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
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

    // Simple comparison to avoid spamming updates
    const prev = previousItem.current;
    const isSame = prev &&
                   prev.id === item.id &&
                   prev.text === item.text &&
                   prev.disabled === item.disabled &&
                   prev.isChecked === item.isChecked &&
                   prev.selectedOption === item.selectedOption &&
                   prev.menuText === item.menuText &&
                   prev.menuToggleOnText === item.menuToggleOnText &&
                   prev.menuToggleOffText === item.menuToggleOffText &&
                   prev.textColor === item.textColor &&
                   prev.fillColor === item.fillColor &&
                   // Compare arrays
                   JSON.stringify(prev.options) === JSON.stringify(item.options) &&
                   JSON.stringify(prev.menuOptions) === JSON.stringify(item.menuOptions) &&
                   prev.shape === item.shape &&
                   prev.color === item.color &&
                   prev.info === item.info &&
                   prev.isRelocItem === item.isRelocItem &&
                   prev.nestedRailItems === item.nestedRailItems &&
                   // Reloc props
                   (prev.hiddenMenu?.length === item.hiddenMenu?.length &&
                    (prev.hiddenMenu || []).every((hm, i) => hm.id === item.hiddenMenu![i].id && hm.text === item.hiddenMenu![i].text && hm.onClick === item.hiddenMenu![i].onClick));

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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
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
    shape: props.shape || AzButtonShape.CIRCLE,
    disabled: props.disabled || false,
    isHost: false,
    isSubItem: false,
    isExpanded: false,
    toggleOnText: '',
    toggleOffText: '',
  });
  return null;
};

export const AzDivider: React.FC = () => {
    const id = useId();
    useAzItem({
        id: `divider-${id}`,
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
        shape: props.shape || AzButtonShape.CIRCLE,
        disabled: props.disabled || false,
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
        shape: props.shape || AzButtonShape.CIRCLE,
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
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
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
    });
    return null;
};

export const AzRailRelocItem: React.FC<AzRailRelocItemProps> = (props) => {
    const hiddenMenuItems = React.useMemo(() => {
        let items: any[] = [];
        if (props.hiddenMenu) {
            if (typeof props.hiddenMenu === 'function') {
                const scope: HiddenMenuScope = {
                    listItem: (text, action) => {
                        if (typeof action === 'string') {
                            items.push({ id: `${props.id}_hidden_item_${items.length}`, text, route: action });
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
                };
                props.hiddenMenu(scope);
            } else {
                items = props.hiddenMenu.map((item, i) => ({
                    id: `${props.id}_hidden_item_${i}`,
                    text: item.text,
                    onClick: item.onClick
                }));
            }
        }
        return items;
    }, [props.hiddenMenu, props.id]);

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
        shape: props.shape,
        disabled: props.disabled || false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        hiddenMenu: hiddenMenuItems,
        forceHiddenMenuOpen: props.forceHiddenMenuOpen,
        onHiddenMenuDismiss: props.onHiddenMenuDismiss,
        nestedRailAlignment: props.nestedRailAlignment || AzNestedRailAlignment.VERTICAL,
    });
    return (
        <AzNavRailContext.Consumer>
            {(ctx) => (
                <AzNavRailContext.Provider value={ctx}>
                    {props.nestedContent}
                </AzNavRailContext.Provider>
            )}
        </AzNavRailContext.Consumer>
    );
};

export const AzSettings: React.FC<any> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzTheme: React.FC<any> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzConfig: React.FC<any> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzAdvanced: React.FC<any> = (props) => {
    const context = useContext(AzNavRailContext);
    const propsJson = JSON.stringify(props);
    useEffect(() => {
        if (context) {
            context.updateSettings(props);
        }
    }, [context, propsJson]);
    return null;
};

export const AzNestedRail: React.FC<AzNestedRailProps> = (props) => {
    const parentContext = useContext(AzNavRailContext);
    const [nestedItems, setNestedItems] = useState<AzNavItem[]>([]);

    const nestedRegister = (item: AzNavItem) => {
        setNestedItems((prev) => {
            const idx = prev.findIndex(i => i.id === item.id);
            if (idx >= 0) {
                const newItems = [...prev];
                newItems[idx] = item;
                return newItems;
            }
            return [...prev, item];
        });
    };

    const nestedUnregister = (id: string) => {
        setNestedItems((prev) => prev.filter(i => i.id !== id));
    };

    useAzItem({
        ...props,
        text: props.text ?? '',
        isRailItem: true,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: false,
        shape: props.shape,
        disabled: props.disabled || false,
        isHost: false,
        isSubItem: false,
        isExpanded: false,
        toggleOnText: '',
        toggleOffText: '',
        isNestedRail: true,
        nestedRailAlignment: props.alignment || AzNestedRailAlignment.VERTICAL,
        nestedRailItems: nestedItems,
    });

    const nestedContextValue = {
        register: nestedRegister,
        unregister: nestedUnregister,
        updateSettings: parentContext?.updateSettings || (() => {}),
    };

    return (
        <AzNavRailContext.Provider value={nestedContextValue}>
            {props.children}
        </AzNavRailContext.Provider>
    );
};

export const AzHelpRailItem: React.FC<AzNavItemProps> = (props) => {
    useAzItem({
        ...props,
        isRailItem: true,
        isHelpItem: true
    });
    return null;
};

export const AzHelpSubItem: React.FC<AzSubItemProps> = (props) => {
    // Validate hostId contextually via useAzItem or parent scope,
    // Note: hostId validation on the web/rn side is handled by AzNavRail directly linking items.
    // For parity we strictly enforce passing hostId.
    if (!props.hostId) {
        console.warn("AzHelpSubItem requires a valid hostId");
    }
    useAzItem({
        ...props,
        isRailItem: true,
        isHelpItem: true,
        isSubItem: true
    });
    return null;
};
