import React, { useEffect, useContext, useRef } from 'react';
import { AzNavItem, AzButtonShape, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps, AzNestedRailAlignment, HiddenMenuScope } from './types';

export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
  getDividerId: () => string;
  hasItem: (id: string) => boolean;
} | null>(null);

const useAzItem = (item: AzNavItem) => {
  const context = useContext(AzNavRailContext);
  const previousItem = useRef<AzNavItem | null>(null);

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
                   // Reloc props
                   JSON.stringify(prev.hiddenMenu) === JSON.stringify(item.hiddenMenu) &&
                   prev.onRelocate === item.onRelocate;

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

export const AzRailDivider: React.FC = () => {
    const context = useContext(AzNavRailContext);
    const id = useRef<string | null>(null);
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
    let hiddenMenuItems: any[] = [];
    if (props.hiddenMenu) {
        if (typeof props.hiddenMenu === 'function') {
            const scope: HiddenMenuScope = {
                listItem: (text, action) => {
                    if (typeof action === 'string') {
                        hiddenMenuItems.push({ id: `${props.id}_hidden_item_${hiddenMenuItems.length}`, text, route: action });
                    } else {
                        hiddenMenuItems.push({ id: `${props.id}_hidden_item_${hiddenMenuItems.length}`, text, onClick: action });
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
                            onValueChange = arg3;
                        }
                    } else if (typeof arg2 === 'function') {
                        onValueChange = arg2;
                    } else {
                        console.warn("inputItem requires an onValueChange function callback.");
                        onValueChange = () => {};
                    }
                    hiddenMenuItems.push({ id: `${props.id}_hidden_input_${hiddenMenuItems.length}`, text: '', isInput: true, hint, initialValue, onValueChange });
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

export const AzNestedRail: React.FC<AzNestedRailProps> = (props) => {
    const [nestedSettings, setNestedSettings] = React.useState<any>({});

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
        nestedRailSettings: nestedSettings,
    });

    return (
        <AzNavRailContext.Consumer>
            {(ctx) => {
                if (!ctx) return null;
                const isolatedContext = {
                    ...ctx,
                    updateSettings: (newSettings: any) => {
                        setNestedSettings((prev: any) => {
                            const merged = { ...prev, ...newSettings };
                            // Shallow check to avoid infinite loops
                            const keys = Object.keys(merged);
                            const changed = keys.some(k => prev[k] !== merged[k]);
                            return changed ? merged : prev;
                        });
                    }
                };
                return (
                    <AzNavRailContext.Provider value={isolatedContext}>
                        {props.children}
                    </AzNavRailContext.Provider>
                );
            }}
        </AzNavRailContext.Consumer>
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

export const AzHelpSubItem: React.FC<AzSubItemProps> = (props) => {
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
