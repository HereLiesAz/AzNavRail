import React, { useEffect, useContext, useRef, useMemo } from 'react';
import { AzNavItem, AzButtonShape, AzNavItemProps, AzToggleProps, AzCyclerProps, AzHostItemProps, AzSubItemProps, AzSubToggleProps, AzSubCyclerProps, AzRailRelocItemProps, AzNestedRailProps, AzNestedRailAlignment, HiddenMenuScope, } from './types';

export const AzNavRailContext = React.createContext<{
  register: (item: AzNavItem) => void;
  unregister: (id: string) => void;
  updateSettings: (settings: any) => void;
} | null>(null);

const useAzItem = (rawItem: AzNavItem) => {
  const context = useContext(AzNavRailContext);

  // Create a stable reference based on content
  const itemDeps = [
    rawItem.id, rawItem.text, rawItem.disabled, rawItem.isChecked, rawItem.selectedOption,
    rawItem.menuText, rawItem.menuToggleOnText, rawItem.menuToggleOffText, rawItem.textColor, rawItem.fillColor,
    rawItem.shape, rawItem.color, rawItem.info, rawItem.isRelocItem, rawItem.isNestedRail, rawItem.keepNestedRailOpen,
    JSON.stringify(rawItem.options), JSON.stringify(rawItem.menuOptions), JSON.stringify(rawItem.hiddenMenu)
  ];

  const item = useMemo(() => rawItem, itemDeps);

  useEffect(() => {
    if (!context) return;
    context.register(item);
  }, [context, item]);

  useEffect(() => {
      if (context) {
          return () => context.unregister(item.id);
      }
      return undefined;
  }, [context, item.id]);
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
    // ID needed?
    const id = useRef(`divider-${Math.random()}`).current;
    useAzItem({
        id,
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
                        hiddenMenuItems.push({ id: `hidden_${hiddenMenuItems.length}`, text, route: action });
                    } else {
                        hiddenMenuItems.push({ id: `hidden_${hiddenMenuItems.length}`, text, onClick: action });
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
                    hiddenMenuItems.push({ id: `input_${hiddenMenuItems.length}`, text: '', isInput: true, hint, initialValue, onValueChange });
                }
            };
            props.hiddenMenu(scope);
        } else {
            hiddenMenuItems = props.hiddenMenu.map((item, i) => ({
                id: `hidden_${i}`,
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
    });
    return (
        <AzNavRailContext.Consumer>
            {(ctx) => (
                <AzNavRailContext.Provider value={ctx}>
                    {props.children}
                </AzNavRailContext.Provider>
            )}
        </AzNavRailContext.Consumer>
    );
};

export const AzHelpRailItem: React.FC<AzNavItemProps> = (props) => {
    useAzItem({
        ...props,
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        disabled: false,
        isHost: false,
        isExpanded: false,
        shape: props.shape || AzButtonShape.CIRCLE,
        toggleOnText: '',
        toggleOffText: '',
        isRailItem: true,
        isHelpItem: true
    } as AzNavItem);
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
        isToggle: false,
        isCycler: false,
        isDivider: false,
        collapseOnClick: true,
        disabled: false,
        isHost: false,
        isExpanded: false,
        shape: props.shape || AzButtonShape.CIRCLE,
        toggleOnText: '',
        toggleOffText: '',
        isRailItem: true,
        isHelpItem: true,
        isSubItem: true
    } as AzNavItem);
    return null;
};
